package com.example.workoutracker.hevy

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.collections.get

class CsvReaderTest {

    private fun fields(text: String) = readCsv(text).map { it.second }

    @Test
    fun plainFields() {
        assertEquals(listOf(listOf("a", "b", "c")), fields("a,b,c"))
    }

    @Test
    fun quotedFieldKeepsItsComma() {
        // Like your workout titled "ham focused, forearms too".
        assertEquals(listOf(listOf("ham focused, forearms too", "x")), fields("\"ham focused, forearms too\",x"))
    }

    @Test
    fun doubledQuoteIsOneQuote() {
        assertEquals(listOf(listOf("say \"hi\"")), fields("\"say \"\"hi\"\"\""))
    }

    @Test
    fun emptyFieldsArePreserved() {
        // Hevy writes empty columns like this: ,,,
        assertEquals(listOf(listOf("a", "", "", "")), fields("a,,,"))
    }

    @Test
    fun lineBreakInsideQuotesIsPartOfTheField() {
        val records = readCsv("\"line one\nline two\",x\nnext,row")
        assertEquals(listOf("line one\nline two", "x"), records[0].second)
        assertEquals(listOf("next", "row"), records[1].second)
        assertEquals(3, records[1].first) // the second record starts on line 3
    }

    @Test
    fun windowsLineEndingsAndBomAndTrailingNewline() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), fields("﻿a,b\r\nc,d\r\n"))
    }

    @Test
    fun blankLinesAreSkipped() {
        assertEquals(listOf(listOf("a"), listOf("b")), fields("a\n\nb\n"))
    }
}