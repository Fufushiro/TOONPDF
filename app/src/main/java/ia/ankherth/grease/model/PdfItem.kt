package ia.ankherth.grease.model
/**
 * Modelo de datos simplificado para mostrar PDFs en Home e Historial
 */
data class PdfItem(
    val id: Long,
    val title: String,
    val totalPages: Int,
    var lastReadPage: Int,
    var lastReadMillis: Long
) {
    val progressPercent: Int
        get() = if (totalPages <= 0) 0 else ((lastReadPage.toDouble() / totalPages) * 100).toInt()
}
