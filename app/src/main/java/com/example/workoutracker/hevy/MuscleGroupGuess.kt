package com.example.workoutracker.hevy

import com.example.workoutracker.data.MuscleGroup
import java.util.Locale

/**
 * Picks a muscle group for an exercise name, so exercises created by a Hevy sync aren't blank.
 * First the exact names from your own export (your list from HevyExerciseSeed.kt), then
 * keyword rules for anything new. Returns null when it can't tell; you can set it on the
 * exercise's screen.
 */
fun guessMuscleGroup(name: String): String? {
    val n = name.trim().lowercase(Locale.ROOT)
    knownExercises[n]?.let { return it }
    // Order matters: the first rule with a matching keyword wins, so the more specific rules
    // come first ("leg curl" before "curl", "reverse fly" before "fly", "row" before "chest").
    return keywordRules.firstOrNull { (_, words) -> words.any { it in n } }?.first
}

/** Your exercises, by lower-case name, with the groups you chose. */
private val knownExercises: Map<String, String> = mapOf(
    "butterfly (pec deck)" to MuscleGroup.CHEST,
    "seated incline curl (dumbbell)" to MuscleGroup.BICEPS,
    "lat pulldown (cable)" to MuscleGroup.LATS,
    "bench press (barbell)" to MuscleGroup.CHEST,
    "lying leg curl (machine)" to MuscleGroup.HAMSTRINGS,
    "leg extension (machine)" to MuscleGroup.QUADS,
    "hip abduction (machine)" to MuscleGroup.ABDUCTORS,
    "single arm triceps pushdown (cable)" to MuscleGroup.TRICEPS,
    "hack squat (machine)" to MuscleGroup.QUADS,
    "hip adduction (machine)" to MuscleGroup.ADDUCTORS,
    "triceps pushdown" to MuscleGroup.TRICEPS,
    "preacher curl (machine)" to MuscleGroup.BICEPS,
    "lat machine seated row" to MuscleGroup.LATS,
    "kelso" to MuscleGroup.U_BACK,
    "single arm lateral raise (cable)" to MuscleGroup.SHOULDERS,
    "incline bench press (dumbbell)" to MuscleGroup.CHEST,
    "hanging leg raise" to MuscleGroup.ABS,
    "reverse fly single arm (cable)" to MuscleGroup.SHOULDERS,
    "back extension (weighted hyperextension)" to MuscleGroup.HAMSTRINGS,
    "romanian deadlift (barbell)" to MuscleGroup.HAMSTRINGS,
    "unilateral pec dec" to MuscleGroup.CHEST,
    "seated row (machine)" to MuscleGroup.U_BACK,
    "cable pullover" to MuscleGroup.LATS,
    "diddy extensions" to MuscleGroup.TRICEPS,
    "bent over row (barbell)" to MuscleGroup.U_BACK,
    "calf press (machine)" to MuscleGroup.CALVES,
    "skullcrusher (barbell)" to MuscleGroup.TRICEPS,
    "single leg extensions" to MuscleGroup.QUADS,
    "calf extension (machine)" to MuscleGroup.CALVES,
    "shoulder press (machine plates)" to MuscleGroup.SHOULDERS,
    "iso-lateral chest press (machine)" to MuscleGroup.CHEST,
    "noodle crunch" to MuscleGroup.ABS,
    "sam sulek curls" to MuscleGroup.BICEPS,
    "hammer curl (cable)" to MuscleGroup.BICEPS,
    "preacher curl (barbell)" to MuscleGroup.BICEPS,
    "squat (barbell)" to MuscleGroup.QUADS,
    "hammer curl (dumbbell)" to MuscleGroup.BICEPS,
    "bicep curl (cable)" to MuscleGroup.BICEPS,
    "unilateral forearm curl" to MuscleGroup.FOREARMS,
    "lateral raise (dumbbell)" to MuscleGroup.SHOULDERS,
    "lat pulldown - close grip (cable)" to MuscleGroup.LATS,
    "bicep curl (dumbbell)" to MuscleGroup.BICEPS,
    "cable single arm forearm curl" to MuscleGroup.FOREARMS,
    "shoulder press (dumbbell)" to MuscleGroup.SHOULDERS,
    "chest press (machine)" to MuscleGroup.CHEST,
    "incline bench press (smith machine)" to MuscleGroup.CHEST,
    "leg press (machine)" to MuscleGroup.QUADS,
    "seated calf raise" to MuscleGroup.CALVES,
    "crunch (machine)" to MuscleGroup.ABS,
    "single leg calf press" to MuscleGroup.CALVES,
    "seated leg curl (machine)" to MuscleGroup.HAMSTRINGS,
    "pull up (weighted)" to MuscleGroup.LATS,
    "pendulum squat (machine)" to MuscleGroup.QUADS,
    "single arm pushdown" to MuscleGroup.TRICEPS,
    "chest supported incline row (dumbbell)" to MuscleGroup.LATS,
    "overhead triceps extension (cable)" to MuscleGroup.TRICEPS,
    "triceps kickback (cable)" to MuscleGroup.TRICEPS,
)

private val keywordRules: List<Pair<String, List<String>>> = listOf(
    MuscleGroup.CALVES to listOf("calf", "calves"),
    MuscleGroup.HAMSTRINGS to listOf("leg curl", "hamstring", "romanian deadlift", "rdl",
        "stiff leg", "good morning", "back extension", "hyperextension"),
    MuscleGroup.ADDUCTORS to listOf("adduction", "adductor"),
    MuscleGroup.ABDUCTORS to listOf("abduction", "abductor"),
    MuscleGroup.QUADS to listOf("leg extension", "leg press", "squat", "lunge", "step up", "step-up"),
    MuscleGroup.FOREARMS to listOf("forearm", "wrist"),
    MuscleGroup.SHOULDERS to listOf("reverse fly", "rear delt", "lateral raise", "shoulder press",
        "overhead press", "face pull", "arnold", "upright row"),
    MuscleGroup.TRICEPS to listOf("tricep", "pushdown", "skullcrusher", "kickback", "dip"),
    MuscleGroup.ABS to listOf("crunch", "leg raise", "plank", "sit up", "sit-up", "russian twist", "ab wheel"),
    MuscleGroup.LATS to listOf("pulldown", "pull up", "pull-up", "chin up", "chin-up", "pullover"),
    MuscleGroup.U_BACK to listOf("row", "shrug"),
    MuscleGroup.CHEST to listOf("bench", "chest", "fly", "pec", "push up", "push-up"),
    MuscleGroup.BICEPS to listOf("curl")
)