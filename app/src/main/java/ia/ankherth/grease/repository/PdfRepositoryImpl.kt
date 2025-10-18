package ia.ankherth.grease.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import ia.ankherth.grease.data.room.AppDatabase
import ia.ankherth.grease.data.room.PdfHistoryDao
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.data.preferences.UserPreferencesManager
import ia.ankherth.grease.util.HistoryExportImport
import ia.ankherth.grease.util.PdfThumbnailGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repositorio actualizado para manejar la persistencia de datos de PDFs y preferencias de usuario
 * Utiliza Room Database para almacenamiento robusto y DataStore para preferencias de usuario
 */
class PdfRepositoryImpl(private val context: Context) {

    // Acceso a la base de datos y DAO
    private val database = AppDatabase.getDatabase(context)
    private val pdfDao: PdfHistoryDao = database.pdfHistoryDao()

    // Gestor de preferencias de usuario
    private val preferencesManager = UserPreferencesManager(context)

    // Obtener todos los PDFs del historial ordenados por fecha de lectura
    val allPdfs: LiveData<List<PdfHistoryEntity>> = pdfDao.getAllPdfs()

    // Flujos de preferencias
    val storagePermissionGranted: Flow<Boolean> = preferencesManager.storagePermissionGranted
    val lastOpenedPdfUri: Flow<String?> = preferencesManager.lastOpenedPdfUri
    val appTheme: Flow<String> = preferencesManager.appTheme
    val userName: Flow<String?> = preferencesManager.userName
    val userAvatarUri: Flow<String?> = preferencesManager.userAvatarUri
    val storageTreeUri: Flow<String?> = preferencesManager.storageTreeUri
    val hapticsEnabled: Flow<Boolean> = preferencesManager.hapticsEnabled

    /** Preferencias: setters **/
    suspend fun setAppTheme(theme: String) = preferencesManager.setAppTheme(theme)
    suspend fun setUserName(name: String?) = preferencesManager.setUserName(name)
    suspend fun setUserAvatarUri(uri: String?) = preferencesManager.setUserAvatarUri(uri)
    suspend fun setStorageTreeUri(uri: String?) = preferencesManager.setStorageTreeUri(uri)
    suspend fun setHapticsEnabled(enabled: Boolean) = preferencesManager.setHapticsEnabled(enabled)

    /**
     * Agrega o actualiza un PDF en el historial
     * @param uri URI del archivo PDF
     * @param fileName Nombre del archivo
     * @param totalPages Número total de páginas
     * @param currentPage Página actual (por defecto 0)
     * @param filePath Ruta opcional del archivo para facilitar la reubicación
     * @param scrollOffset Desplazamiento de scroll para reanudar lectura
     * @param fileSizeBytes Tamaño del archivo en bytes
     */
    suspend fun addOrUpdatePdf(
        uri: String,
        fileName: String,
        totalPages: Int,
        currentPage: Int = 0,
        filePath: String? = null,
        scrollOffset: Float = 0f,
        fileSizeBytes: Long = 0L
    ) {
        val existingPdf = pdfDao.getPdfByUri(uri)
        val thumbnailPath = PdfThumbnailGenerator.generateThumbnail(context, uri.toUri())

        if (existingPdf != null) {
            // Actualizar PDF existente
            val updatedPdf = existingPdf.copy(
                lastPageRead = currentPage,
                scrollOffset = scrollOffset,
                lastReadDate = System.currentTimeMillis(),
                fileName = fileName,
                totalPages = totalPages,
                filePath = filePath ?: existingPdf.filePath,
                thumbnailPath = thumbnailPath ?: existingPdf.thumbnailPath,
                isAccessible = true,
                fileSizeBytes = if (fileSizeBytes > 0L) fileSizeBytes else existingPdf.fileSizeBytes
            )
            pdfDao.update(updatedPdf)
        } else {
            // Agregar nuevo PDF
            val newPdf = PdfHistoryEntity(
                uri = uri,
                fileName = fileName,
                totalPages = totalPages,
                lastPageRead = currentPage,
                scrollOffset = scrollOffset,
                lastReadDate = System.currentTimeMillis(),
                filePath = filePath,
                thumbnailPath = thumbnailPath,
                isAccessible = true,
                fileSizeBytes = fileSizeBytes
            )
            pdfDao.insert(newPdf)
        }

        // Guardar como último PDF abierto en las preferencias
        preferencesManager.saveLastOpenedPdfUri(uri)
    }

    /**
     * Actualiza el progreso de lectura de un PDF
     * @param uri URI del PDF
     * @param pageNumber Número de página actual
     * @param scrollOffset Desplazamiento de scroll actual
     */
    suspend fun updateProgress(uri: String, pageNumber: Int, scrollOffset: Float = 0f) {
        pdfDao.updateProgress(uri, pageNumber, scrollOffset, System.currentTimeMillis())
    }

    /**
     * Elimina un PDF del historial
     * @param pdf Entidad del PDF a eliminar
     */
    suspend fun deletePdf(pdf: PdfHistoryEntity) {
        pdfDao.delete(pdf)

        // Si era el último PDF abierto, limpiar la referencia
        if (preferencesManager.lastOpenedPdfUri.first() == pdf.uri) {
            preferencesManager.saveLastOpenedPdfUri(null)
        }
    }

    /**
     * Limpia todo el historial
     */
    suspend fun clearAllHistory() {
        pdfDao.clearAllHistory()
        preferencesManager.saveLastOpenedPdfUri(null)
    }

    /**
     * Actualiza el estado de favorito de un PDF
     * @param uri URI del PDF
     * @param isFavorite Indica si el PDF es favorito
     */
    suspend fun updateFavorite(uri: String, isFavorite: Boolean) {
        pdfDao.updateFavorite(uri, isFavorite)
    }

    /**
     * Obtiene los PDFs favoritos
     * @return LiveData con la lista de PDFs favoritos
     */
    fun getFavoritePdfs(): LiveData<List<PdfHistoryEntity>> {
        return pdfDao.getFavoritePdfs()
    }

    /**
     * Exporta el historial a JSON
     * @param outputUri URI de salida para el archivo JSON
     * @return Resultado de la operación de exportación
     */
    suspend fun exportHistory(outputUri: Uri): Result<String> {
        val historyList = pdfDao.getAllPdfsList()
        return HistoryExportImport.exportHistory(context, historyList, outputUri)
    }

    /**
     * Importa el historial desde JSON
     * @param inputUri URI de entrada del archivo JSON
     * @return Resultado de la operación de importación
     */
    suspend fun importHistory(inputUri: Uri): Result<Int> {
        val result = HistoryExportImport.importHistory(context, inputUri)

        return if (result.isSuccess) {
            val historyList = result.getOrNull() ?: emptyList()
            historyList.forEach { pdf ->
                pdfDao.insert(pdf)
            }
            Result.success(historyList.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error desconocido"))
        }
    }

    /**
     * Obtiene el PDF más reciente del historial
     * @return La entidad del PDF más reciente o null si no hay historial
     */
    suspend fun getMostRecentPdf(): PdfHistoryEntity? {
        return pdfDao.getMostRecentPdf()
    }

    suspend fun getPdfByUri(uri: String): PdfHistoryEntity? {
        return pdfDao.getPdfByUri(uri)
    }

    /**
     * Actualiza el estado de accesibilidad de un PDF
     * @param uri URI del PDF
     * @param isAccessible Indica si el archivo es accesible
     */
    suspend fun updateAccessibility(uri: String, isAccessible: Boolean) {
        pdfDao.updateAccessibility(uri, isAccessible)
    }

    /**
     * Actualiza el estado del permiso de almacenamiento (bandera histórica)
     * @param granted Indica si el permiso fue concedido
     */
    suspend fun updateStoragePermissionStatus(granted: Boolean) {
        preferencesManager.updateStoragePermissionStatus(granted)
    }

    /**
     * Busca PDFs por nombre
     * @param query Texto para buscar en los nombres de archivo
     * @return LiveData con la lista de PDFs que coinciden con la búsqueda
     */
    fun searchPdfsByName(query: String): LiveData<List<PdfHistoryEntity>> {
        return pdfDao.searchPdfsByName(query)
    }
}
