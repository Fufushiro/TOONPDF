package ia.ankherth.grease.viewmodel

import android.app.Application
import android.net.Uri
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

    /**
     * Agrega o actualiza un PDF en el historial
     */
    fun addOrUpdatePdf(uri: String, fileName: String, totalPages: Int, currentPage: Int = 0, filePath: String? = null) {
        viewModelScope.launch {
            try {
                repository.addOrUpdatePdf(uri, fileName, totalPages, currentPage, filePath)
            } catch (e: Exception) {
                _errorEvent.value = ErrorEvent(
                    message = "Error al guardar el PDF en el historial: ${e.message}",
                    exception = e
                )
            }
        }
    }

    /**
     * Actualiza el progreso de lectura de un PDF
     */
    fun updateProgress(uri: String, pageNumber: Int) {
        viewModelScope.launch {
            repository.updateProgress(uri, pageNumber)
        }
    }

    /**
     * Elimina un PDF del historial
     */
    fun deletePdf(pdf: PdfHistoryEntity) {
        viewModelScope.launch {
            repository.deletePdf(pdf)
        }
    }

    /**
     * Obtiene el PDF más reciente del historial
     */
    fun getMostRecentPdf(callback: (PdfHistoryEntity?) -> Unit) {
        viewModelScope.launch {
            val recentPdf = repository.getMostRecentPdf()
            callback(recentPdf)
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
