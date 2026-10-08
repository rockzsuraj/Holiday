package com.surajkumar.holiday

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.surajkumar.holiday.domain.model.Holiday

@Composable
fun RowResultList(
    rows: List<RowResult>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rows) { result ->
            when (result) {
                is RowResult.Valid -> ValidHolidayItem(holiday = result.holiday)
                is RowResult.Invalid -> InvalidRowItem(invalid = result)
            }
        }
    }
}

@Composable
fun ValidHolidayItem(holiday: Holiday) {

        Column(Modifier.padding(12.dp)) {
            Text(
                text = holiday.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = holiday.date.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }


@Composable
fun InvalidRowItem(invalid: RowResult.Invalid) {

        Column(Modifier.padding(12.dp)) {
            Text(
                text = "Line ${invalid.lineNumber}: ${invalid.reason}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Raw: \"${invalid.line}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
        }
}