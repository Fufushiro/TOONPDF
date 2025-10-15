package ia.ankherth.grease.data

import java.util.Date

data class PdfHistoryItem(
    val uri: String,
    val fileName: String,
    val totalPages: Int,
    val lastPageRead: Int = 0,
    val lastReadDate: Date = Date(),
    val fileSizeBytes: Long = 0L
) {
    val progressPercentage: Float
        get() = if (totalPages > 0) (lastPageRead.toFloat() / totalPages.toFloat()) * 100f else 0f
}
