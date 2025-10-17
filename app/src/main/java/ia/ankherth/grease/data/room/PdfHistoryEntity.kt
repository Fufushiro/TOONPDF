package ia.ankherth.grease.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar historial de PDFs de forma persistente
 * Permite guardar la información completa de un PDF incluyendo su URI, progreso de lectura
 * y fecha de última apertura
 */
@Entity(tableName = "pdf_history")
data class PdfHistoryEntity(
    @PrimaryKey
    val uri: String,
    val fileName: String,
    val totalPages: Int,
    val lastPageRead: Int = 0,
    val scrollOffset: Float = 0f, // Posición exacta de scroll (0-1)
    val lastReadDate: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    // Campos adicionales para mejorar la persistencia
    val filePath: String? = null, // Para facilitar la reubicación
    val thumbnailPath: String? = null, // Ruta a la miniatura generada
    val isAccessible: Boolean = true, // Indica si el archivo es accesible
    val isFavorite: Boolean = false // Marcador/favorito
) {
    val progressPercentage: Float
        get() = if (totalPages > 0) (lastPageRead.toFloat() / totalPages.toFloat()) * 100f else 0f
}
