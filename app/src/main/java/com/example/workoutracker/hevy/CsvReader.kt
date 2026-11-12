package com.example.workoutracker.hevy

/**
 * A small CSV reader (RFC 4180): fields separated by commas, optionally wrapped in double quotes.
 * Inside quotes, commas and line breaks are part of the field, and "" means one quote character.
 * Handles \n and \r\n line endings and an optional byte-order mark at the start.
 *
 * Splitting on commas isn't enough: your export has titles like "ham focused, forearms too".
 *
 * Returns one list of fields per record, each paired with the line number it started on.
 */
fun readCsv(text: String): List<Pair<Int, List<String>>> {
    val records = mutableListOf<Pair<Int, List<String>>>()
    val fields = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var line = 1
    var recordStartLine = 1
    var i = if (text.startsWith("﻿")) 1 else 0

    fun endField() {
        fields.add(field.toString())
        field.setLength(0)
    }

    fun endRecord() {
        endField()
        // Skip completely blank lines (often a trailing newline at the end of the file).
        if (!(fields.size == 1 && fields[0].isEmpty())) {
            records.add(recordStartLine to fields.toList())
        }
        fields.clear()
        recordStartLine = line
    }

    while (i < text.length) {
        val c = text[i]
        if (inQuotes) {
            when {
                c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = false
                else -> {
                    if (c == '\n') line++
                    field.append(c)
                }
            }
        } else {
            when (c) {
                '"' -> inQuotes = true
                ',' -> endField()
                '\r' -> Unit // the \n that follows ends the line
                '\n' -> { line++; endRecord() }
                else -> field.append(c)
            }
        }
        i++
    }
    // The last record, if the file doesn't end with a newline.
    if (field.isNotEmpty() || fields.isNotEmpty()) endRecord()
    return records
}