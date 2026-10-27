package com.example.workoutracker.data

/**
 * The 57 exercises found in your Hevy export, with a muscle group I assigned.
 * Please check them: the ones marked "guess" are custom names I had to interpret.
 * null = I couldn't tell, so it's left blank.
 *
 * TEMPORARY dev helper, like the sample-data button. Week 4's CSV import will create
 * any missing exercises itself, and matches by name, so loading this first causes no duplicates.
 */
val hevyExercises: List<Pair<String, String?>> = listOf(
    "Butterfly (Pec Deck)" to MuscleGroup.CHEST,
    "Seated Incline Curl (Dumbbell)" to MuscleGroup.BICEPS,
    "Lat Pulldown (Cable)" to MuscleGroup.LATS,
    "Bench Press (Barbell)" to MuscleGroup.CHEST,
    "Lying Leg Curl (Machine)" to MuscleGroup.HAMSTRINGS,
    "Leg Extension (Machine)" to MuscleGroup.QUADS,
    "Hip Abduction (Machine)" to MuscleGroup.ABDUCTORS,
    "Single Arm Triceps Pushdown (Cable)" to MuscleGroup.TRICEPS,
    "Hack Squat (Machine)" to MuscleGroup.QUADS,
    "Hip Adduction (Machine)" to MuscleGroup.ADDUCTORS,
    "Triceps Pushdown" to MuscleGroup.TRICEPS,
    "Preacher Curl (Machine)" to MuscleGroup.BICEPS,
    "lat machine seated row" to MuscleGroup.LATS,
    "kelso" to MuscleGroup.U_BACK,
    "Single Arm Lateral Raise (Cable)" to MuscleGroup.SHOULDERS,
    "Incline Bench Press (Dumbbell)" to MuscleGroup.CHEST,
    "Hanging Leg Raise" to MuscleGroup.ABS,
    "Reverse Fly Single Arm (Cable)" to MuscleGroup.SHOULDERS,
    "Back Extension (Weighted Hyperextension)" to MuscleGroup.HAMSTRINGS,
    "Romanian Deadlift (Barbell)" to MuscleGroup.HAMSTRINGS,
    "unilateral pec dec" to MuscleGroup.CHEST,
    "Seated Row (Machine)" to MuscleGroup.U_BACK,
    "cable pullover" to MuscleGroup.LATS,
    "diddy extensions" to MuscleGroup.TRICEPS,
    "Bent Over Row (Barbell)" to MuscleGroup.U_BACK,
    "Calf Press (Machine)" to MuscleGroup.CALVES,
    "Skullcrusher (Barbell)" to MuscleGroup.TRICEPS,
    "Single Leg Extensions" to MuscleGroup.QUADS,
    "Calf Extension (Machine)" to MuscleGroup.CALVES,
    "Shoulder Press (Machine Plates)" to MuscleGroup.SHOULDERS,
    "Iso-Lateral Chest Press (Machine)" to MuscleGroup.CHEST,
    "noodle crunch" to MuscleGroup.ABS,
    "sam sulek curls" to MuscleGroup.BICEPS,
    "Hammer Curl (Cable)" to MuscleGroup.BICEPS,
    "Preacher Curl (Barbell)" to MuscleGroup.BICEPS,
    "Squat (Barbell)" to MuscleGroup.QUADS,
    "Hammer Curl (Dumbbell)" to MuscleGroup.BICEPS,
    "Bicep Curl (Cable)" to MuscleGroup.BICEPS,
    "unilateral forearm curl" to MuscleGroup.FOREARMS,
    "Lateral Raise (Dumbbell)" to MuscleGroup.SHOULDERS,
    "Lat Pulldown - Close Grip (Cable)" to MuscleGroup.LATS,
    "Bicep Curl (Dumbbell)" to MuscleGroup.BICEPS,
    "cable single arm forearm curl" to MuscleGroup.FOREARMS,
    "Shoulder Press (Dumbbell)" to MuscleGroup.SHOULDERS,
    "Chest Press (Machine)" to MuscleGroup.CHEST,
    "Incline Bench Press (Smith Machine)" to MuscleGroup.CHEST,
    "Leg Press (Machine)" to MuscleGroup.QUADS,
    "Seated Calf Raise" to MuscleGroup.CALVES,
    "Crunch (Machine)" to MuscleGroup.ABS,
    "single leg calf press" to MuscleGroup.CALVES,
    "Seated Leg Curl (Machine)" to MuscleGroup.HAMSTRINGS,
    "Pull Up (Weighted)" to MuscleGroup.LATS,
    "Pendulum Squat (Machine)" to MuscleGroup.QUADS,
    "single arm pushdown" to MuscleGroup.TRICEPS,
    "Chest Supported Incline Row (Dumbbell)" to MuscleGroup.LATS,
    "Overhead Triceps Extension (Cable)" to MuscleGroup.TRICEPS,
    "Triceps Kickback (Cable)" to MuscleGroup.TRICEPS
)

/** Inserts the exercises above. Names that already exist are skipped, never overwritten. */
suspend fun seedHevyExercises(dao: ExerciseDao) {
    hevyExercises.forEach { (name, group) ->
        dao.insert(ExerciseEntity(name = name, muscleGroup = group))
    }
}