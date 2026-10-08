package com.surajkumar.holiday

import com.surajkumar.holiday.domain.model.Holiday
import java.time.LocalDate

sealed class RowResult {
    data class Valid(val holiday: Holiday) : RowResult()
    data class Invalid(val lineNumber: Int, val line: String, val reason: String) : RowResult()
}

fun parseRow(rawDate: String?, rawName: String?, lineNumber: Int, line: String): RowResult {
    val dateText = rawDate?.trim().orEmpty()
    val name = rawName?.trim().orEmpty()

    if (dateText.isBlank()) {
        return RowResult.Invalid(lineNumber, line, "Missing date")
    }
    if (name.isBlank()) {
        return RowResult.Invalid(lineNumber, line, "Missing name")
    }

    return try {
        RowResult.Valid(Holiday(name = name, date = LocalDate.parse(dateText)))
    } catch (e: Exception) {
        RowResult.Invalid(lineNumber, line, reason = "Invalid date")
    }
}