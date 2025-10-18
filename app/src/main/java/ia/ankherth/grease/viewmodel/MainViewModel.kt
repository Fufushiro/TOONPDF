package ia.ankherth.grease.viewmodel

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.repository.PdfRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel principal actualizado para manejar la lógica de negocio de la aplicación
 * Proporciona la comunicación entre los repositorios y la UI
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepositoryImpl(application)

    // LiveData para los PDFs del historial
    val allPdfs: LiveData<List<PdfHistoryEntity>> = repository.allPdfs

    // LiveData para el estado del permiso de almacenamiento
    val storagePermissionGranted = repository.storagePermissionGranted.asLiveData()

    // Preferencias de usuario
    val appTheme = repository.appTheme.asLiveData()
    val userName = repository.userName.asLiveData()
    val userAvatarUri = repository.userAvatarUri.asLiveData()
    val storageTreeUri = repository.storageTreeUri.asLiveData()
    val hapticsEnabled = repository.hapticsEnabled.asLiveData()

    // LiveData para indicar que se está realizando una operación
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para manejar errores
    private val _errorEvent = MutableLiveData<ErrorEvent?>()
    val errorEvent: LiveData<ErrorEvent?> = _errorEvent

    /** Preferencias setters **/
    fun setAppTheme(theme: String) { viewModelScope.launch { repository.setAppTheme(theme) } }
    fun setUserName(name: String?) { viewModelScope.launch { repository.setUserName(name) } }
    fun setUserAvatarUri(uri: String?) { viewModelScope.launch { repository.setUserAvatarUri(uri) } }
    fun setStorageTreeUri(uri: String?) { viewModelScope.launch { repository.setStorageTreeUri(uri) } }
    fun setHapticsEnabled(enabled: Boolean) { viewModelScope.launch { repository.setHapticsEnabled(enabled) } }

    /**
     * Actualiza el avatar del usuario
     */
    fun updateUserAvatar(uri: String) {
        viewModelScope.launch {
            try {
                // Tomar permisos persistentes para el URI
                val context = getApplication<Application>()
                val avatarUri = uri.toUri()
                context.contentResolver.takePersistableUriPermission(
                    avatarUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                repository.setUserAvatarUri(uri)
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent("Error al guardar el avatar: ${e.message}")
            }
        }
    }

    /**
     * Agrega o actualiza un PDF en el historial
     */
    fun addOrUpdatePdf(uri: String, fileName: String, totalPages: Int, currentPage: Int = 0, filePath: String? = null, scrollOffset: Float = 0f, fileSizeBytes: Long = 0L) {
        viewModelScope.launch {
            try {
                repository.addOrUpdatePdf(uri, fileName, totalPages, currentPage, filePath, scrollOffset, fileSizeBytes)
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent(
                    message = "Error al guardar el PDF en el historial: ${e.message}",
                    exception = e
                )
            }
        }
    }

    /**
     * Agrega o actualiza un PDF en el historial
     */
    fun addPdfToHistory(uri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Extraer información del URI y agregar al historial
                val fileName = extractRealFileNameFromUri(uri)
                val fileSize = getFileSizeFromUri(uri)
                val filePath = extractFilePathFromUri(uri)

                repository.addOrUpdatePdf(
                    uri = uri.toString(),
                    fileName = fileName,
                    totalPages = 0, // Se actualizará cuando se abra el PDF
                    currentPage = 0,
                    filePath = filePath,
                    scrollOffset = 0f,
                    fileSizeBytes = fileSize
                )
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent("Error al agregar PDF: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Obtiene el tamaño del archivo desde el URI
     */
    private fun getFileSizeFromUri(uri: Uri): Long {
        return try {
            val context = getApplication<Application>()
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        cursor.getLong(sizeIndex)
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Extrae la ruta del archivo desde el URI
     */
    private fun extractFilePathFromUri(uri: Uri): String {
        return try {
            val context = getApplication<Application>()
            when (uri.scheme) {
                "content" -> {
                    // Intentar obtener la ruta real del archivo
                    val pathSegments = uri.pathSegments
                    if (pathSegments.isNotEmpty()) {
                        // Para URIs de DocumentProvider, el último segmento suele tener información de la ruta
                        val lastSegment = pathSegments.last()
                        if (lastSegment.contains(":")) {
                            val parts = lastSegment.split(":")
                            if (parts.size > 1) {
                                "/storage/emulated/0/${parts[1]}"
                            } else {
                                uri.toString()
                            }
                        } else {
                            uri.toString()
                        }
                    } else {
                        uri.toString()
                    }
                }
                "file" -> uri.path ?: uri.toString()
                else -> uri.toString()
            }
        } catch (e: Exception) {
            uri.toString()
        }
    }

    /**
     * Extrae el nombre real del archivo desde el URI usando ContentResolver
     */
    private fun extractRealFileNameFromUri(uri: Uri): String {
        return try {
            val context = getApplication<Application>()
            when (uri.scheme) {
                "content" -> {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (displayNameIndex >= 0) {
                                cursor.getString(displayNameIndex) ?: getDefaultFileName(uri)
                            } else {
                                getDefaultFileName(uri)
                            }
                        } else {
                            getDefaultFileName(uri)
                        }
                    } ?: getDefaultFileName(uri)
                }
                "file" -> {
                    uri.lastPathSegment ?: "documento.pdf"
                }
                else -> getDefaultFileName(uri)
            }
        } catch (e: Exception) {
            getDefaultFileName(uri)
        }
    }

    private fun getDefaultFileName(uri: Uri): String {
        val lastSegment = uri.lastPathSegment ?: "documento"
        return if (lastSegment.contains(".pdf", ignoreCase = true)) {
            lastSegment
        } else {
            "$lastSegment.pdf"
        }
    }

    /**
     * Actualiza el progreso de lectura de un PDF
     */
    fun updateProgress(uri: String, pageNumber: Int, scrollOffset: Float = 0f) {
        viewModelScope.launch {
            repository.updateProgress(uri, pageNumber, scrollOffset)
        }
    }

    /**
     * Actualiza la URI de un PDF en el historial
     */
    @Suppress("UNUSED_PARAMETER")
    fun updatePdfUri(oldUri: String, newUri: String) {
        viewModelScope.launch {
            try {
                // Simular actualización - implementar según el repository actual
                _errorEvent.value = ErrorEvent("Funcionalidad de reubicación no implementada aún")
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent("Error al actualizar PDF: ${e.message}")
            }
        }
    }

    /**
     * Elimina un PDF del historial
     */
    fun deletePdf(pdf: PdfHistoryEntity) {
        viewModelScope.launch {
            try {
                repository.deletePdf(pdf)
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent("Error al eliminar PDF: ${e.message}")
            }
        }
    }

    /**
     * Limpia todo el historial
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                repository.clearAllHistory()
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent("Error al limpiar historial: ${e.message}")
            }
        }
    }

    /**
     * Obtiene el PDF más reciente
     */
    suspend fun getMostRecentPdf(): PdfHistoryEntity? {
        return repository.getMostRecentPdf()
    }

    /**
     * Alterna el estado de favorito de un PDF
     */
    fun toggleFavorite(uri: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateFavorite(uri, isFavorite)
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent("Error al actualizar favorito: ${e.message}")
            }
        }
    }

    /**
     * Exporta el historial a JSON
     */
    suspend fun exportHistory(outputUri: Uri): Result<String> {
        return repository.exportHistory(outputUri)
    }

    /**
     * Importa el historial desde JSON
     */
    suspend fun importHistory(inputUri: Uri): Result<Int> {
        return repository.importHistory(inputUri)
    }

    /**
     * Obtiene el PDF más reciente accesible del historial
     */
    fun getMostRecentPdf(callback: (PdfHistoryEntity?) -> Unit) {
        // Usar el valor actual del LiveData
        val currentPdfs = allPdfs.value ?: emptyList()
        val mostRecentAccessible = currentPdfs.firstOrNull { it.isAccessible }
        callback(mostRecentAccessible)
    }

    /**
     * Obtiene la entidad del último PDF abierto o el más reciente como fallback
     */
    suspend fun getLastOpenedPdf(): PdfHistoryEntity? {
        val lastUri = repository.lastOpenedPdfUri.first()
        return if (!lastUri.isNullOrBlank()) {
            repository.getPdfByUri(lastUri)
        } else {
            repository.getMostRecentPdf()
        }
    }

    /**
     * Actualiza el estado de accesibilidad de un PDF
     */
    fun updatePdfAccessibility(uri: String, isAccessible: Boolean) {
        viewModelScope.launch {
            repository.updateAccessibility(uri, isAccessible)
        }
    }

    /**
     * Actualiza el estado del permiso de almacenamiento
     */
    fun updateStoragePermissionStatus(granted: Boolean) {
        viewModelScope.launch {
            repository.updateStoragePermissionStatus(granted)
        }
    }

    /**
     * Busca PDFs por nombre
     */
    fun searchPdfsByName(query: String): LiveData<List<PdfHistoryEntity>> {
        return repository.searchPdfsByName(query)
    }

    /**
     * Verifica y actualiza la accesibilidad de todos los PDFs en el historial
     */
    fun refreshPdfAccessibility() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Esta función se implementará en la actividad principal para verificar si los archivos existen
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Obtiene el último PDF abierto de las preferencias
     */
    suspend fun getLastOpenedPdfUri(): String? {
        return repository.lastOpenedPdfUri.first()
    }

    /**
     * Obtiene el último PDF leído
     */
    fun getLastReadPdf(): PdfHistoryEntity? {
        return allPdfs.value?.maxByOrNull { it.lastReadDate }
    }

    /**
     * LiveData para el último PDF leído
     */
    val lastReadPdf: LiveData<PdfHistoryEntity?> = MutableLiveData<PdfHistoryEntity?>().apply {
        allPdfs.observeForever { pdfs ->
            value = pdfs?.maxByOrNull { it.lastReadDate }
        }
    }

    /**
     * Refresca la lista de PDFs (forzar recarga desde la base de datos)
     */
    fun refreshPdfs() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // El LiveData ya está observando cambios en la base de datos
                // automáticamente a través del Flow, no necesitamos hacer nada especial
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent("Error al actualizar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpia el evento de error después de manejarlo
     */
    fun clearErrorEvent() {
        _errorEvent.value = null
    }

    /**
     * Clase para representar un evento de error
     */
    data class ErrorEvent(
        val message: String,
        val exception: Exception? = null
    )
}
