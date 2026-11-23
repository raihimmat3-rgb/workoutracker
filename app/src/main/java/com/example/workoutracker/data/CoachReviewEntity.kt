package com.example.workoutracker.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * The last coach review for a workout, saved so reopening the workout doesn't spend another
 * of your few free AI requests. Added in database version 5.
 *
 * [workoutFingerprint] is the workout's fingerprint when it was reviewed (the same fingerprint
 * Hevy sync uses). If the workout is edited afterwards, the fingerprints differ and the app
 * offers to refresh the review.
 */
@Entity(
    tableName = "coach_review",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE // deleting the workout deletes its review
        )
    ]
)
data class CoachReviewEntity(
    @PrimaryKey val workoutId: Long,
    val reviewJson: String,
    val workoutFingerprint: String,
    val createdAt: Long
)

@Dao
interface CoachDao {
    /** Insert, or replace the existing review for that workout. */
    @Upsert
    suspend fun save(review: CoachReviewEntity)

    @Query("SELECT * FROM coach_review WHERE workoutId = :workoutId")
    fun observe(workoutId: Long): Flow<CoachReviewEntity?>
}