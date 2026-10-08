package com.surajkumar.holiday

import com.surajkumar.holiday.domain.model.Holiday
import java.time.LocalDate

fun parseLine(line: String): Holiday? {
    val parts = line.split(",", limit = 2)
    if (parts.size < 2) {
        return null
    }
    val dateText = parts[0].trim()
    val name = parts[1].trim()
    return try {
        Holiday(name = name, date = LocalDate.parse(dateText))
    } catch (e: Exception) {
        null
    }
}

fun parseCsv(text: String): List<Holiday> {
    return text
        .lines()
        .drop(1)
        .mapNotNull {
            parseLine(it)
        }
}