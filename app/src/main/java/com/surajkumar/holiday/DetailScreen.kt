package com.surajkumar.holiday

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.surajkumar.holiday.domain.model.Holiday
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    holiday: Holiday?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(holiday?.name ?: "Holiday") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (holiday == null) {
                Text("Holiday not found")
            } else {
                HolidayDetailContent(holiday)
            }
        }
    }
}

@Composable
private fun HolidayDetailContent(holiday: Holiday) {
    val days = remember(holiday) {
        ChronoUnit.DAYS.between(LocalDate.now(), holiday.date)
    }
    Text(holiday.name, style = MaterialTheme.typography.headlineMedium)
    Text(holiday.date.toString(), style = MaterialTheme.typography.bodyLarge)
    Text(
        text = urgencyLabel(days),
        style = MaterialTheme.typography.titleMedium,
        color = urgencyColor(days)
    )
}