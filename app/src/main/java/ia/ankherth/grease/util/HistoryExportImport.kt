package ia.ankherth.grease.util

import android.content.Context
import android.net.Uri
import ia.ankherth.grease.data.room.PdfHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Utilidad para exportar e importar el historial de lectura en formato JSON
 * Permite respaldar y restaurar el progreso de lectura del usuario
 */
object HistoryExportImport {

    /**
     * Exporta el historial completo a JSON
     */
    suspend fun exportHistory(
        context: Context,
        historyList: List<PdfHistoryEntity>,
        outputUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()

            historyList.forEach { pdf ->
                val jsonObject = JSONObject().apply {
                    put("uri", pdf.uri)
                    put("fileName", pdf.fileName)
                    put("totalPages", pdf.totalPages)
                    put("lastPageRead", pdf.lastPageRead)
                    put("scrollOffset", pdf.scrollOffset.toDouble())
                    put("lastReadDate", pdf.lastReadDate)
                    put("fileSizeBytes", pdf.fileSizeBytes)
                    put("filePath", pdf.filePath ?: "")
                    put("isAccessible", pdf.isAccessible)
                    put("isFavorite", pdf.isFavorite)
                }
                jsonArray.put(jsonObject)
            }

            val rootObject = JSONObject().apply {
                put("version", 1)
                put("exportDate", System.currentTimeMillis())
                put("totalEntries", historyList.size)
                put("history", jsonArray)
            }

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(rootObject.toString(2))
                }
            }

            Result.success("Historial exportado: ${historyList.size} entradas")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Importa el historial desde un archivo JSON
     */
    suspend fun importHistory(
        context: Context,
        inputUri: Uri
    ): Result<List<PdfHistoryEntity>> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return@withContext Result.failure(Exception("No se pudo leer el archivo"))

            val rootObject = JSONObject(jsonString)
            val historyArray = rootObject.getJSONArray("history")
            val historyList = mutableListOf<PdfHistoryEntity>()

            for (i in 0 until historyArray.length()) {
                val jsonObject = historyArray.getJSONObject(i)
                val pdf = PdfHistoryEntity(
                    uri = jsonObject.getString("uri"),
                    fileName = jsonObject.getString("fileName"),
                    totalPages = jsonObject.getInt("totalPages"),
                    lastPageRead = jsonObject.getInt("lastPageRead"),
                    scrollOffset = jsonObject.getDouble("scrollOffset").toFloat(),
                    lastReadDate = jsonObject.getLong("lastReadDate"),
                    fileSizeBytes = jsonObject.optLong("fileSizeBytes", 0L),
                    filePath = jsonObject.optString("filePath").takeIf { it.isNotEmpty() },
                    isAccessible = jsonObject.optBoolean("isAccessible", true),
                    isFavorite = jsonObject.optBoolean("isFavorite", false)
                )
                historyList.add(pdf)
            }

            Result.success(historyList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

