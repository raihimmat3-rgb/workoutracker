package com.example.workoutracker.hevy

/**
 * What the app knows about one workout it previously imported from Hevy.
 *  - [hevyKey]: its start time in Hevy, used to match it in the file. Kept separately from the
 *    workout's own start time, so changing the date in this app doesn't break the match.
 *  - [hevyFingerprint]: Hevy's version at the last sync.
 *  - [syncedFingerprint]: this app's version at the last sync (after any choice you made).
 *  - [currentFingerprint]: this app's version now.
 */
data class LocalSyncState(
    val id: Long,
    val title: String,
    val startedAt: Long,
    val hevyKey: Long,
    val hevyFingerprint: String?,
    val syncedFingerprint: String?,
    val currentFingerprint: String
)

/** Why a workout needs you to choose. */
enum class ConflictKind {
    /** You edited it here; Hevy's copy is unchanged since the last sync. */
    EditedHere,
    /** You edited it here, AND it changed in Hevy. */
    EditedInBoth,
    /** You edited it here, but it's been deleted in Hevy. */
    DeletedInHevy
}

/** A workout to ask about: "Keep my edits" or go with Hevy (its version, or deleting it). */
data class SyncConflict(
    val localId: Long,
    val title: String,
    val startedAt: Long,
    val kind: ConflictKind,
    val incoming: HevyWorkout? // Hevy's version; null when it was deleted in Hevy
)

data class SyncUpdate(val localId: Long, val incoming: HevyWorkout)

/** The full result of comparing the file with the app. Nothing is changed until it's applied. */
data class SyncPlan(
    val toAdd: List<HevyWorkout>,
    val toUpdate: List<SyncUpdate>,
    val toDelete: List<Long>,
    val conflicts: List<SyncConflict>,
    /** Skipped: a workout made in this app already starts at that exact minute. */
    val clashes: List<HevyWorkout>,
    val unchanged: Int
) {
    val hasChanges: Boolean
        get() = toAdd.isNotEmpty() || toUpdate.isNotEmpty() || toDelete.isNotEmpty() || conflicts.isNotEmpty()
}

/**
 * Compares a full Hevy export ([file]) with the workouts previously imported from Hevy ([imported]).
 * [allStartTimes] is the start time of EVERY workout in the app, so a new Hevy workout can't
 * collide with one you logged here (start times are unique in the database).
 * Workouts made in this app are never in [imported], so they're never changed.
 *
 * For each workout in both, two questions decide what happens:
 *   did Hevy's copy change since the last sync?  did your copy change since the last sync?
 *     no  / no   -> unchanged
 *     yes / no   -> update from Hevy (you hadn't touched it)
 *     no  / yes  -> ask (EditedHere)
 *     yes / yes  -> ask (EditedInBoth)
 * Imported workouts missing from the file were deleted in Hevy: deleted here too,
 * unless you'd edited them, in which case ask (DeletedInHevy).
 */
fun planSync(
    file: List<HevyWorkout>,
    imported: List<LocalSyncState>,
    allStartTimes: Set<Long>
): SyncPlan {
    val byKey = imported.associateBy { it.hevyKey }
    val inFile = mutableSetOf<Long>()
    val toAdd = mutableListOf<HevyWorkout>()
    val toUpdate = mutableListOf<SyncUpdate>()
    val conflicts = mutableListOf<SyncConflict>()
    val clashes = mutableListOf<HevyWorkout>()
    var unchanged = 0

    for (workout in file) {
        // Two Hevy workouts starting in the same minute can't both be stored: keep the first.
        if (!inFile.add(workout.startedAt)) {
            clashes += workout
            continue
        }
        val local = byKey[workout.startedAt]
        if (local == null) {
            if (workout.startedAt in allStartTimes) clashes += workout else toAdd += workout
            continue
        }
        val hevyChanged = workout.toContent().fingerprint() != local.hevyFingerprint
        val localChanged = local.currentFingerprint != local.syncedFingerprint
        when {
            !hevyChanged && !localChanged -> unchanged++
            hevyChanged && !localChanged -> toUpdate += SyncUpdate(local.id, workout)
            else -> conflicts += SyncConflict(
                localId = local.id,
                title = local.title,
                startedAt = local.startedAt,
                kind = if (hevyChanged) ConflictKind.EditedInBoth else ConflictKind.EditedHere,
                incoming = workout
            )
        }
    }

    val toDelete = mutableListOf<Long>()
    for (local in imported) {
        if (local.hevyKey in inFile) continue
        if (local.currentFingerprint != local.syncedFingerprint) {
            conflicts += SyncConflict(local.id, local.title, local.startedAt, ConflictKind.DeletedInHevy, null)
        } else {
            toDelete += local.id
        }
    }

    return SyncPlan(toAdd, toUpdate, toDelete, conflicts, clashes, unchanged)
}