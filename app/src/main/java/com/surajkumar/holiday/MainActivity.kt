package com.surajkumar.holiday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.surajkumar.holiday.domain.model.Holiday
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val holidays = listOf(
                        Holiday(name = "New Year", date = LocalDate.of(2026, 1, 1)),
                        Holiday(name = "Republic Day", date = LocalDate.of(2026, 1, 26)),
                        Holiday(name = "Holi", date = LocalDate.of(2026, 3, 3)),
                        Holiday(name = "Gudi Padava (Floater)", date = LocalDate.of(2026, 3, 19)),
                        Holiday(name = "Good Friday (Floater)", date = LocalDate.of(2026, 4, 3)),
                        Holiday(name = "Labour Day", date = LocalDate.of(2026, 5, 1)),
                        Holiday(name = "Eid-Ul-Adha (Floater)", date = LocalDate.of(2026, 5, 27)),
                        Holiday(name = "Ganesh Chaturthi", date = LocalDate.of(2026, 9, 14)),
                        Holiday(name = "Gandhi Jayanti", date = LocalDate.of(2026, 10, 2)),
                        Holiday(name = "Dussehra (Floater)", date = LocalDate.of(2026, 10, 20)),
                        Holiday(name = "Diwali", date = LocalDate.of(2026, 11, 9)),
                        Holiday(name = "Christmas", date = LocalDate.of(2026, 12, 25)),
                    )
                    HolidayList(holidays)
                }
            }
        }
    }
}

fun urgencyLabel(days: Long): String = when {
    days < 0 -> "Passed"
    days == 0L -> "Today"
    else -> "in $days days"
}

@Composable
fun HolidayList(holidays: List<Holiday>) {
    val sortedHolidays = holidays.sortedBy { it.date }
    Column {
        Text(
            text = "${sortedHolidays.size} Holidays",
            modifier = Modifier.padding(12.dp)
        )
        LazyColumn {
            items(sortedHolidays) { holiday ->
                val days = ChronoUnit.DAYS.between(LocalDate.now(), holiday.date)
                val urgency = urgencyLabel(days)
                val color = urgencyColor(days)
                Column {
                    Text(text = holiday.name, modifier = Modifier.padding(12.dp))
                    Text(
                        text = "${holiday.date} • $urgency",
                        modifier = Modifier.padding(12.dp),
                        color = color
                    )
                }
            }
        }
    }
}

fun urgencyColor(days: Long): Color = when {
    days < 0 -> Color.Gray
    days == 0L -> Color.Red
    else -> Color.Unspecified
}