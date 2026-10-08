package com.surajkumar.holiday

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.surajkumar.holiday.ai.AiEngineMode
import com.surajkumar.holiday.ai.HolidayAiExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Row as PoiRow
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

@Composable
fun CsvPicker() {
    val apiKey = BuildConfig.GEMINI_API_KEY
    var engineMode by remember { mutableStateOf(AiEngineMode.CLOUD_FIREBASE_AI) }
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Pick any file to extract holidays") }
    var rows by remember { mutableStateOf<List<RowResult>>(emptyList()) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiExtractor = remember { HolidayAiExtractor() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(context, uri) ?: "unknown_file"
            val mimeType = getMimeType(context, uri, fileName)

            when (engineMode) {
                AiEngineMode.CLOUD_FIREBASE_AI -> {
                    if (apiKey.isBlank()) {
                        statusText = "GEMINI_API_KEY is missing in local.properties!"
                        return@rememberLauncherForActivityResult
                    }

                    scope.launch(Dispatchers.IO) {
                        isLoading = true
                        withContext(Dispatchers.Main) {
                            statusText = "Processing $fileName with Firebase AI Logic (Cloud)..."
                        }
                        try {
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            if (bytes != null) {
                                val parsedRows = aiExtractor.extractHolidaysWithFirebaseAI(
                                    context = context,
                                    apiKey = apiKey.trim(),
                                    fileBytes = bytes,
                                    mimeType = mimeType
                                )
                                withContext(Dispatchers.Main) {
                                    rows = parsedRows
                                    statusText = "Cloud AI Extraction completed for $fileName"
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    statusText = "Could not read bytes from $fileName"
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                statusText = "Firebase AI Error: ${e.localizedMessage ?: e.message}"
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                }

                AiEngineMode.ON_DEVICE_GEMINI_NANO -> {
                    scope.launch(Dispatchers.IO) {
                        isLoading = true
                        withContext(Dispatchers.Main) {
                            statusText = "Parsing $fileName & running on-device Gemini Nano (AICore)..."
                        }
                        try {
                            val extractedText = aiExtractor.extractTextFromFile(context, uri, fileName)
                            val parsedRows = aiExtractor.extractHolidaysWithGeminiNano(context, extractedText)
                            withContext(Dispatchers.Main) {
                                rows = parsedRows
                                statusText = "On-Device Gemini Nano Extraction completed for $fileName"
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                statusText = "On-Device AI Error: ${e.localizedMessage ?: e.message}"
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                            }
                        }
                    }
                }

                AiEngineMode.LOCAL_CODE_PARSER -> {
                    val fileType = detectFileType(fileName)
                    when (fileType) {
                        "csv" -> {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                val text = inputStream.bufferedReader().use { it.readText() }
                                rows = parseCsvRow(text)
                                statusText = "Parsed CSV locally"
                            }
                        }
                        "xlsx" -> {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                rows = readWorkSheet(inputStream)
                                statusText = "Parsed Excel locally"
                            }
                        }
                        else -> {
                            statusText = "Unsupported file type for local code parser. Choose Cloud or On-Device AI!"
                        }
                    }
                }
            }
        }
    }

    val validRows = rows.filterIsInstance<RowResult.Valid>().size
    val invalidRows = rows.filterIsInstance<RowResult.Invalid>().size

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Extraction Engine:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Column(Modifier.selectableGroup()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (engineMode == AiEngineMode.CLOUD_FIREBASE_AI),
                        onClick = { engineMode = AiEngineMode.CLOUD_FIREBASE_AI }
                    )
                    Text(
                        text = "Cloud: Firebase AI Logic SDK",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (engineMode == AiEngineMode.ON_DEVICE_GEMINI_NANO),
                        onClick = { engineMode = AiEngineMode.ON_DEVICE_GEMINI_NANO }
                    )
                    Text(
                        text = "On-Device: Google AI Edge / Gemini Nano (AICore)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = (engineMode == AiEngineMode.LOCAL_CODE_PARSER),
                        onClick = { engineMode = AiEngineMode.LOCAL_CODE_PARSER }
                    )
                    Text(
                        text = "Local Code Parser (CSV/XLSX)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { launcher.launch("*/*") },
                enabled = !isLoading
            ) {
                Text("Pick File (PDF, Image, Doc, CSV, etc.)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (rows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$validRows valid, $invalidRows invalid",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rows.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No holidays extracted yet.")
                }
            } else {
                RowResultList(rows = rows, modifier = Modifier.weight(1f))
            }
        }
    }
}

fun getMimeType(context: Context, uri: Uri, fileName: String?): String {
    val contentMime = context.contentResolver.getType(uri)
    if (!contentMime.isNullOrBlank() && contentMime != "application/octet-stream") {
        return contentMime
    }
    return when {
        fileName?.endsWith(".pdf", ignoreCase = true) == true -> "application/pdf"
        fileName?.endsWith(".csv", ignoreCase = true) == true -> "text/csv"
        fileName?.endsWith(".txt", ignoreCase = true) == true -> "text/plain"
        fileName?.endsWith(".png", ignoreCase = true) == true -> "image/png"
        fileName?.endsWith(".jpg", ignoreCase = true) == true -> "image/jpeg"
        fileName?.endsWith(".jpeg", ignoreCase = true) == true -> "image/jpeg"
        fileName?.endsWith(".webp", ignoreCase = true) == true -> "image/webp"
        fileName?.endsWith(".docx", ignoreCase = true) == true -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        fileName?.endsWith(".xlsx", ignoreCase = true) == true -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> "text/plain"
    }
}

fun Map<String, String?>.getCaseInsensitive(key: String): String? {
    return entries.firstOrNull { it.key.equalsIgnoreCase(key) }?.value
}

private fun String.equalsIgnoreCase(other: String): Boolean {
    return this.equals(other, ignoreCase = true)
}

fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

fun detectFileType(fileName: String): String {
    return when {
        fileName.endsWith(".csv", ignoreCase = true) -> "csv"
        fileName.endsWith(".txt", ignoreCase = true) -> "txt"
        fileName.endsWith(".pdf", ignoreCase = true) -> "pdf"
        fileName.endsWith(".doc", ignoreCase = true) -> "doc"
        fileName.endsWith(".docx", ignoreCase = true) -> "docx"
        fileName.endsWith(".xlsx", ignoreCase = true) -> "xlsx"
        else -> "unknown"
    }
}

fun parseCsvRow(text: String?): List<RowResult> {
    text?.let {
        val allHeaders: List<Map<String, String>> = csvReader().readAllWithHeader(text)
        val parsedRows = allHeaders.mapIndexed { index, header ->
            val dateText = header.getCaseInsensitive("date")
            val name = header.getCaseInsensitive("name")
            val lineNumber = index + 2
            val rawLine = "date: $dateText, name: $name"
            parseRow(
                rawDate = dateText, rawName = name, lineNumber = lineNumber, line = rawLine
            )
        }
        return parsedRows
    }
    return emptyList()
}

fun readWorkSheet(input: InputStream): List<RowResult> {
    val workbook = WorkbookFactory.create(input)
    val sheet = workbook.getSheetAt(0)
    val row = sheet.getRow(0)
    val headerIndex = buildHeaderIndex(row)
    val dateColumnIndex = headerIndex["date"]
    val nameColumnIndex = headerIndex["name"]
    val rowResults = mutableListOf<RowResult>()

    for (rowIndex in 1..sheet.lastRowNum) {
        val row = sheet.getRow(rowIndex) ?: continue
        val lineNumber = row.rowNum + 1
        val dateText = dateColumnIndex?.let { row.getCell(it)?.toString() } ?: ""
        val name = nameColumnIndex?.let { row.getCell(it)?.toString() } ?: ""
        val rawLine = "date: $dateText, name: $name"
        val result = parseRow(dateText, name, lineNumber, rawLine)
        rowResults.add(result)
    }
    return rowResults
}

fun buildHeaderIndex(headerRow: PoiRow): Map<String, Int> {
    val index = mutableMapOf<String, Int>()
    for (cell in headerRow) {
        val columnName = cell.toString().trim().lowercase()
        index[columnName] = cell.columnIndex
    }
    return index
}