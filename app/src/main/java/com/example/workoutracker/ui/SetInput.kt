package com.example.workoutracker.ui

import java.math.BigDecimal

private val weightPattern = Regex("""\d{0,4}(\.\d{0,2})?""")
private val repsPattern = Regex("""\d{0,3}""")

/**
 * Whether [text] is allowed in the weight box while typing: up to 4 digits, then optionally
 * a point and up to 2 decimals. Unfinished input like "" or "60." is allowed.
 */
fun isValidWeightInput(text: String): Boolean = weightPattern.matches(text)

/** "" or "." -> null (nothing entered yet), "60." -> 60.0, "22.5" -> 22.5 */
fun parseWeight(text: String): Double? = text.toDoubleOrNull()

/** Whether [text] is allowed in the reps box: up to 3 digits, or empty. */
fun isValidRepsInput(text: String): Boolean = repsPattern.matches(text)

/** "" -> null, "12" -> 12 */
fun parseReps(text: String): Int? = text.toIntOrNull()

/** 60.0 -> "60", 22.5 -> "22.5", null -> "". Avoids showing "60.0" in the box. */
fun formatWeight(kg: Double?): String = when {
    kg == null -> ""
    kg % 1.0 == 0.0 -> kg.toLong().toString()
    else -> BigDecimal.valueOf(kg).stripTrailingZeros().toPlainString()
}