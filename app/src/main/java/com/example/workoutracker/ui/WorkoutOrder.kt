package com.example.workoutracker.ui

/**
 * The index of the item to swap with when moving item [index] in a list of [size] items,
 * or null if it can't move that way (already first/last, or not in the list).
 */
fun neighbourIndex(index: Int, size: Int, up: Boolean): Int? {
    if (index !in 0 until size) return null
    val target = if (up) index - 1 else index + 1
    return if (target in 0 until size) target else null
}