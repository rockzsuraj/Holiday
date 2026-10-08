package com.surajkumar.holiday

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.surajkumar.holiday.domain.model.Holiday
import java.time.LocalDate
import java.time.temporal.ChronoUnit


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    holidays: List<Holiday>,
    onHolidayClick: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Holiday App") }
            )
        }
    ) { innerPadding ->
        Column {
            HolidayList(
                holidays = holidays,
                onHolidayClick = onHolidayClick,
                modifier = Modifier
                    .padding(innerPadding)
            )
        }
    }
}

fun urgencyLabel(days: Long): String = when {
    days < 0 -> "Passed"
    days == 0L -> "Today"
    else -> "in $days days"
}

fun urgencyColor(days: Long): Color = when {
    days < 0 -> Color.Gray
    days == 0L -> Color.Red
    else -> Color.Unspecified
}

@Composable
fun HolidayList(
    holidays: List<Holiday>,
    onHolidayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "${holidays.size} Holidays",
            modifier = Modifier.padding(12.dp)
        )
        LazyColumn {
            items(holidays, key = { it.id }) { holiday ->
                HolidayRow(holiday = holiday, onClick = { onHolidayClick(holiday.id) })
            }
        }
    }
}

@Composable
private fun HolidayRow(holiday: Holiday, onClick: () -> Unit) {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), holiday.date)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(text = holiday.name, modifier = Modifier.padding(12.dp))
        Text(
            text = "${holiday.date} • ${urgencyLabel(days)}",
            modifier = Modifier.padding(12.dp),
            color = urgencyColor(days)
        )
    }
}