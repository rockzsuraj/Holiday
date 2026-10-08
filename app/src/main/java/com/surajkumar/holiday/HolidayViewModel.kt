package com.surajkumar.holiday

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.surajkumar.holiday.domain.model.Holiday
import java.time.LocalDate

class HolidayViewModel : ViewModel() {
    val holidays = mutableStateListOf(
        Holiday(id = 1, name = "New Year", date = LocalDate.of(2026, 1, 1)),
        Holiday(id = 2, name = "Republic Day", date = LocalDate.of(2026, 1, 26)),
        Holiday(id = 3, name = "Holi", date = LocalDate.of(2026, 3, 3)),
        Holiday(id = 4, name = "Gudi Padava (Floater)", date = LocalDate.of(2026, 3, 19)),
        Holiday(id = 5, name = "Good Friday (Floater)", date = LocalDate.of(2026, 4, 3)),
        Holiday(id = 6, name = "Labour Day", date = LocalDate.of(2026, 5, 1)),
        Holiday(id = 7, name = "Eid-Ul-Adha (Floater)", date = LocalDate.of(2026, 5, 27)),
        Holiday(id = 8, name = "Ganesh Chaturthi", date = LocalDate.of(2026, 9, 14)),
        Holiday(id = 9, name = "Gandhi Jayanti", date = LocalDate.of(2026, 10, 2)),
        Holiday(id = 10, name = "Dussehra (Floater)", date = LocalDate.of(2026, 10, 20)),
        Holiday(id = 11, name = "Diwali", date = LocalDate.of(2026, 11, 9)),
        Holiday(id = 12, name = "Christmas", date = LocalDate.of(2026, 12, 25))
    )
    val sortedHolidays: List<Holiday> by derivedStateOf {
        holidays.sortedBy { it.date }
    }

    fun getHoliday(id: Long): Holiday? = holidays.firstOrNull { it.id == id }
}