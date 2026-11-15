package com.example.workoutracker.hevy

import androidx.room.withTransaction
import com.example.workoutracker.data.AppDatabase
import com.example.workoutracker.data.ExerciseEntity

/** Your answer for one conflict. */
enum class Resolution { KeepMine, UseHevy }

/** A plan plus what's needed to apply it ("keep mine" saves your version's fingerprint). */
data class PreparedSync(
    val plan: SyncPlan,
    val currentFingerprints: Map<Long, String>,
    /** The app's Hevy workouts by id, so the preview can name the ones it will delete. */
    val imported: Map<Long, LocalSyncState> = emptyMap()
)

/** What applying did, for the summary screen. */
data class SyncResult(
    val added: Int,
    val updated: Int,
    val deleted: Int,
    val keptYours: Int,
    /** Conflicts you didn't answer: left exactly as they were, and asked about again next sync. */
    val undecided: Int,
    /** Couldn't be stored, e.g. a workout made here already starts at that exact minute. */
    val skipped: Int
)

/**
 * Hevy sync, in two steps so you can preview before anything changes:
 *  1. [prepare]: read the app's Hevy workouts and compare them with the file (no writes).
 *  2. [apply]: carry out the plan and your conflict choices, all in ONE transaction:
 *     if anything fails half-way, nothing at all is changed.
 */
class HevySync(private val db: AppDatabase) {

    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()

    suspend fun prepare(file: List<HevyWorkout>): PreparedSync {
        val imported = workoutDao.getImportedWithDetails()
        val current = imported.associate { it.workout.id to it.toContent().fingerprint() }
        val states = imported.map { d ->
            LocalSyncState(
                id = d.workout.id,
                title = d.workout.title,
                startedAt = d.workout.startedAt,
                hevyKey = d.workout.hevyKey!!, // the query only returns rows that have one
                hevyFingerprint = d.workout.hevyFingerprint,
                syncedFingerprint = d.workout.syncedFingerprint,
                currentFingerprint = current.getValue(d.workout.id)
            )
        }
        val plan = planSync(file, states, workoutDao.getAllStartTimes().toSet())
        return PreparedSync(plan, current, states.associateBy { it.id })
    }

    suspend fun apply(prepared: PreparedSync, resolutions: Map<Long, Resolution>): SyncResult =
        db.withTransaction {
            val plan = prepared.plan
            var added = 0
            var updated = 0
            var deleted = 0
            var kept = 0
            var undecided = 0
            var skipped = plan.clashes.size

            // Exercise name -> library id. Names in the library ignore case, so the key is lower case.
            val exerciseIds = mutableMapOf<String, Long>()
            suspend fun exerciseIdFor(name: String): Long =
                exerciseIds.getOrPut(name.trim().lowercase()) {
                    val trimmed = name.trim()
                    val inserted = exerciseDao.insert(ExerciseEntity(name = trimmed))
                    if (inserted != -1L) inserted else exerciseDao.findByName(trimmed)!!.id
                }

            /** Writes Hevy's exercises and sets into a workout that has none. */
            suspend fun writeExercises(workoutId: Long, incoming: HevyWorkout) {
                incoming.exercises.forEachIndexed { position, exercise ->
                    val weId = workoutDao.insertWorkoutExercise(
                        exercise.toWorkoutExerciseEntity(workoutId, exerciseIdFor(exercise.title), position)
                    )
                    workoutDao.insertSets(exercise.sets.mapIndexed { i, set -> set.toSetEntity(weId, i) })
                }
            }

            /** Replaces a workout's contents with Hevy's version, keeping its id. */
            suspend fun replaceWithHevy(localId: Long, incoming: HevyWorkout): Boolean {
                val existing = workoutDao.getById(localId) ?: return false
                val fingerprint = incoming.toContent().fingerprint()
                // Go back to Hevy's start time, unless another workout has taken that minute.
                val start = if (incoming.startedAt == existing.startedAt ||
                    !workoutDao.existsByStartedAt(incoming.startedAt)
                ) incoming.startedAt else existing.startedAt
                workoutDao.updateWorkout(
                    existing.copy(
                        title = incoming.title,
                        startedAt = start,
                        endedAt = incoming.endedAt,
                        notes = incoming.description,
                        source = "hevy",
                        hevyKey = incoming.startedAt,
                        hevyFingerprint = fingerprint,
                        syncedFingerprint = fingerprint
                    )
                )
                workoutDao.deleteExercisesInWorkout(localId) // sets go too (cascade)
                writeExercises(localId, incoming)
                return true
            }

            for (incoming in plan.toAdd) {
                val id = workoutDao.insertWorkout(incoming.toNewWorkoutEntity(incoming.toContent().fingerprint()))
                if (id == -1L) { skipped++; continue } // start time taken after all
                writeExercises(id, incoming)
                added++
            }

            for (update in plan.toUpdate) {
                if (replaceWithHevy(update.localId, update.incoming)) updated++ else skipped++
            }

            plan.toDelete.forEach { workoutDao.deleteWorkout(it); deleted++ }

            for (conflict in plan.conflicts) {
                when (resolutions[conflict.localId]) {
                    null -> undecided++
                    Resolution.UseHevy -> {
                        val incoming = conflict.incoming
                        if (incoming == null) {
                            workoutDao.deleteWorkout(conflict.localId)  // deleted in Hevy: delete here too
                            deleted++
                        } else if (replaceWithHevy(conflict.localId, incoming)) updated++ else skipped++
                    }
                    Resolution.KeepMine -> {
                        val incoming = conflict.incoming
                        if (incoming == null) {
                            workoutDao.detachFromHevy(conflict.localId) // gone from Hevy: now just yours
                        } else {
                            // Your version becomes the baseline, so this edit isn't asked about again;
                            // Hevy's current version is recorded so only a NEW Hevy change brings it back.
                            workoutDao.updateSyncFingerprints(
                                conflict.localId,
                                hevy = incoming.toContent().fingerprint(),
                                synced = prepared.currentFingerprints.getValue(conflict.localId)
                            )
                        }
                        kept++
                    }
                }
            }

            SyncResult(added, updated, deleted, kept, undecided, skipped)
        }
}