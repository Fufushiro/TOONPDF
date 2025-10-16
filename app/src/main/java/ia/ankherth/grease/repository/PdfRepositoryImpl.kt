package ia.ankherth.grease.repository

import android.content.Context
import androidx.lifecycle.LiveData
import ia.ankherth.grease.data.room.AppDatabase
import ia.ankherth.grease.data.room.PdfHistoryDao
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.data.preferences.UserPreferencesManager
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

    /** Preferencias: setters **/
    suspend fun setAppTheme(theme: String) = preferencesManager.setAppTheme(theme)
    suspend fun setUserName(name: String?) = preferencesManager.setUserName(name)
    suspend fun setUserAvatarUri(uri: String?) = preferencesManager.setUserAvatarUri(uri)
    suspend fun setStorageTreeUri(uri: String?) = preferencesManager.setStorageTreeUri(uri)

    /**
     * Agrega o actualiza un PDF en el historial
     * @param uri URI del PDF
     * @param fileName Nombre del archivo PDF
     * @param totalPages Número total de páginas
     * @param currentPage Página actual (por defecto 0)
     * @param filePath Ruta opcional del archivo para facilitar la reubicación
     */
    suspend fun addOrUpdatePdf(
        uri: String,
        fileName: String,
        totalPages: Int,
        currentPage: Int = 0,
        filePath: String? = null
    ) {
        val existingPdf = pdfDao.getPdfByUri(uri)

        if (existingPdf != null) {
            // Actualizar PDF existente
            val updatedPdf = existingPdf.copy(
                lastPageRead = currentPage,
                lastReadDate = System.currentTimeMillis(),
                fileName = fileName,
                totalPages = totalPages,
                filePath = filePath ?: existingPdf.filePath,
                isAccessible = true
            )
            pdfDao.update(updatedPdf)
        } else {
            // Agregar nuevo PDF
            val newPdf = PdfHistoryEntity(
                uri = uri,
                fileName = fileName,
                totalPages = totalPages,
                lastPageRead = currentPage,
                lastReadDate = System.currentTimeMillis(),
                filePath = filePath,
                isAccessible = true
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
     */
    suspend fun updateProgress(uri: String, pageNumber: Int) {
        pdfDao.updateProgress(uri, pageNumber, System.currentTimeMillis())
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
     * Obtiene el PDF más reciente del historial
     * @return La entidad del PDF más reciente o null si no hay historial
     */
    suspend fun getMostRecentPdf(): PdfHistoryEntity? {
        return pdfDao.getMostRecentPdf()
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
