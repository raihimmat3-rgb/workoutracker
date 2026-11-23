package com.example.workoutracker.coach

import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.CoachReviewEntity
import com.example.workoutracker.hevy.fingerprint
import com.example.workoutracker.hevy.toContent
import com.example.workoutracker.ui.weekStartOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.time.ZoneId

/** A coach problem, with a message ready to show the user. */
class CoachException(message: String) : Exception(message)

/**
 * Gathers the facts from the database, asks the server, and saves the answer.
 * The screen only talks to this, never to Retrofit or the DAOs directly.
 */
class CoachRepository(private val db: AppDatabase, private val api: CoachApi?) {

    private val workoutDao = db.workoutDao()
    private val coachDao = db.coachDao()

    fun observeSaved(workoutId: Long): Flow<CoachReviewEntity?> = coachDao.observe(workoutId)

    fun decode(saved: CoachReviewEntity): CoachReviewDto =
        coachJson.decodeFromString(CoachReviewDto.serializer(), saved.reviewJson)

    /** Asks for a fresh review of a finished workout and saves it. Throws CoachException with a friendly message. */
    suspend fun requestReview(workoutId: Long): CoachReviewDto {
        val coach = api ?: throw CoachException(
            "The coach isn't set up: add coach.baseUrl and coach.appKey to local.properties"
        )
        val details = workoutDao.observeDetails(workoutId).first()
            ?: throw CoachException("Workout not found")
        val start = details.workout.startedAt
        val zone = ZoneId.systemDefault()

        // "Last time" for each exercise, counted back from THIS workout's start.
        val previous = details.exercises.associate { item ->
            val exerciseId = item.exercise.id
            exerciseId to workoutDao.getPreviousSessionStart(exerciseId, start)?.let { prevStart ->
                PreviousData(prevStart, workoutDao.getDoneSetsBefore(exerciseId, start))
            }
        }
        val daysSinceLast = workoutDao.getLastFinishedStartBefore(start)?.let { daysBetween(it, start, zone) }
        val weekStart = weekStartOf(start, zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val thisWeek = workoutDao.countFinishedBetween(weekStart, start)

        val request = buildReviewRequest(details, previous, daysSinceLast, thisWeek, zone)
            ?: throw CoachException("Tick at least one set to get a review")

        val review = try {
            coach.review(request)
        } catch (e: HttpException) {
            throw CoachException(messageFor(e))
        } catch (e: IOException) {
            throw CoachException("Couldn't reach the coach server (${e.javaClass.simpleName}: ${e.message})")
        }

        coachDao.save(
            CoachReviewEntity(
                workoutId = workoutId,
                reviewJson = coachJson.encodeToString(CoachReviewDto.serializer(), review),
                workoutFingerprint = details.toContent().fingerprint(),
                createdAt = System.currentTimeMillis()
            )
        )
        return review
    }

    /** Turns the server's {"reason": ...} into something a person understands. */
    private fun messageFor(e: HttpException): String {
        val reason = try {
            e.response()?.errorBody()?.string()?.let { coachJson.decodeFromString(ApiErrorDto.serializer(), it).reason }
        } catch (_: Exception) {
            null
        }
        return when (reason) {
            "unauthorized" -> "The app key doesn't match the server's COACH_APP_KEY"
            "daily_limit" -> "Daily coach limit reached. Try again tomorrow"
            "gemini_quota" -> "The AI's free quota is used up for today. Try again tomorrow"
            "server_misconfigured" -> "The server has no COACH_APP_KEY set"
            "invalid_request" -> "The server couldn't read this workout (${e.code()})"
            else -> "The coach had a problem (${e.code()}). Try again in a moment"
        }
    }
}