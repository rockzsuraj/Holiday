package com.surajkumar.holiday.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.ai.edge.aicore.GenerativeModel as GeminiNanoModel
import com.google.ai.edge.aicore.generationConfig as aicoreGenerationConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.surajkumar.holiday.RowResult
import com.surajkumar.holiday.detectFileType
import com.surajkumar.holiday.parseRow
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

enum class AiEngineMode {
    CLOUD_FIREBASE_AI,
    ON_DEVICE_GEMINI_NANO,
    LOCAL_CODE_PARSER
}

class HolidayAiExtractor {

    /**
     * Firebase AI Logic SDK (Cloud Integration via Gemini Developer Suite)
     * Automatically iterates through multiple models (gemini-3.8-flash -> gemini-3.5-flash-lite -> gemini-1.5-flash)
     */
    suspend fun extractHolidaysWithFirebaseAI(
        context: Context,
        apiKey: String,
        fileBytes: ByteArray,
        mimeType: String
    ): List<RowResult> {
        if (FirebaseApp.getApps(context).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId("com.surajkumar.holiday")
                .setProjectId("holiday-reminder-d92b4")
                .build()
            FirebaseApp.initializeApp(context, options)
        }

        val modelsToTry = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-1.5-flash")

        for (modelName in modelsToTry) {
            try {
                val generativeModel = Firebase.ai(
                    backend = GenerativeBackend.googleAI()
                ).generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    }
                )

                val inputContent = content {
                    inlineData(fileBytes, mimeType)
                    text(
                        """
                        Extract all public holidays or scheduled calendar events mentioned in this document.
                        Return a valid JSON array of objects.
                        Each object must contain:
                        - "date": String formatted strictly as YYYY-MM-DD
                        - "name": Name/title of the holiday or event
                        
                        Do not include conversational text or markdown code blocks like ```json.
                        """.trimIndent()
                    )
                }

                val response = generativeModel.generateContent(inputContent)
                Log.d("HolidayAiExtractor", "🟢 SUCCESS: Extracted using Firebase AI SDK (model: $modelName)")
                val jsonString = response.text ?: "[]"
                return parseHolidaysJson(jsonString)
            } catch (e: Exception) {
                Log.w("HolidayAiExtractor", "⚠️ WARNING: Firebase AI SDK model $modelName failed (${e.localizedMessage}), trying next model...", e)
            }
        }

        // If all Firebase AI SDK models fail, fallback to Direct HTTP
        Log.w("HolidayAiExtractor", "⚠️ All Firebase AI SDK models failed, falling back to Direct HTTP...")
        return extractWithDirectHttp(apiKey, fileBytes, mimeType)
    }

    /**
     * Direct HTTP Fallback for cloud endpoints
     */
    private fun extractWithDirectHttp(
        apiKey: String,
        fileBytes: ByteArray,
        mimeType: String
    ): List<RowResult> {
        val base64Data = Base64.encodeToString(fileBytes, Base64.NO_WRAP)
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", mimeType)
                                put("data", base64Data)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", "Extract all holidays mentioned in this document. Return a raw valid JSON array of objects with keys \"date\" (YYYY-MM-DD) and \"name\".")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }.toString()

        val modelsToTry = listOf("gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-1.5-flash")
        var lastError = ""

        for (modelName in modelsToTry) {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                conn.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                val responseText = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                }

                if (responseCode == 200) {
                    Log.d("HolidayAiExtractor", "🟢 SUCCESS: Extracted using Direct HTTP Fallback (model: $modelName)")
                    val responseObj = JSONObject(responseText)
                    val candidates = responseObj.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val contentObj = firstCandidate?.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val extractedText = parts?.optJSONObject(0)?.optString("text", "[]") ?: "[]"

                    return parseHolidaysJson(extractedText)
                } else {
                    lastError = "$modelName ($responseCode): $responseText"
                }
            } catch (e: Exception) {
                lastError = "$modelName error: ${e.localizedMessage ?: e.message}"
            }
        }

        throw Exception(lastError)
    }

    suspend fun extractHolidaysWithGeminiNano(
        context: Context,
        fileBytes: ByteArray
    ): List<RowResult> {
        return extractHolidaysWithGeminiNano(context, String(fileBytes, Charsets.UTF_8))
    }

    suspend fun extractHolidaysWithGeminiNano(
        context: Context,
        fileText: String
    ): List<RowResult> {
        return try {
            val generativeModel = GeminiNanoModel(aicoreGenerationConfig {
                this.context = context.applicationContext
            })

            val fullPrompt = """
                You are a strict data extraction tool. Analyze the provided text and convert it into a JSON array.
                Do not include conversational text or markdown formatting like ```json. Output ONLY valid raw JSON.
                Target Schema:
                [
                  {"date": "YYYY-MM-DD", "name": "Event or Holiday Name"}
            	]

                TEXT TO PARSE:
                $fileText

                OUTPUT JSON:
            """.trimIndent()

            val response = generativeModel.generateContent(fullPrompt)
            val jsonString = response.text ?: "[]"
            parseHolidaysJson(jsonString)
        } catch (e: Exception) {
            throw Exception("On-device Gemini Nano (AICore) service failed to bind (AICore is supported only on compatible physical devices like Pixel 9). Error: ${e.localizedMessage}")
        }
    }

    private fun parseHolidaysJson(jsonString: String): List<RowResult> {
        val rowResults = mutableListOf<RowResult>()
        try {
            val cleanedJson = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanedJson)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val date = item.optString("date", "").trim()
                val name = item.optString("name", "").trim()
                val lineNumber = i + 1
                val rawLine = "date: $date, name: $name"

                val rowResult = parseRow(
                    rawDate = date,
                    rawName = name,
                    lineNumber = lineNumber,
                    line = rawLine
                )
                rowResults.add(rowResult)
            }
        } catch (e: Exception) {
            rowResults.add(RowResult.Invalid(0, jsonString.take(120), "Malformed structural content: ${e.localizedMessage}"))
        }
        return rowResults
    }

    fun extractTextFromFile(context: Context, uri: Uri, fileName: String): String {
        val fileType = detectFileType(fileName)
        return when (fileType) {
            "csv", "txt" -> {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            "xlsx" -> {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    val sheet = workbook.getSheetAt(0)
                    val sb = StringBuilder()
                    for (row in sheet) {
                        val cells = row.map { cell -> cell.toString().trim() }.joinToString(", ")
                        sb.append(cells).append("\n")
                    }
                    sb.toString()
                }.orEmpty()
            }
            else -> {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
        }
    }
}
