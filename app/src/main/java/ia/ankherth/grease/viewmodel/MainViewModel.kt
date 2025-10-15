package ia.ankherth.grease.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ia.ankherth.grease.data.PdfHistoryItem
import ia.ankherth.grease.repository.PdfRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository = PdfRepository(application)
    val allPdfs: LiveData<List<PdfHistoryItem>> = repository.allPdfs

    fun addOrUpdatePdf(uri: String, fileName: String, totalPages: Int, currentPage: Int = 0) {
        viewModelScope.launch {
            repository.addOrUpdatePdf(uri, fileName, totalPages, currentPage)
        }
    }

    fun updateProgress(uri: String, pageNumber: Int) {
        viewModelScope.launch {
            repository.updateProgress(uri, pageNumber)
        }
    }

    fun deletePdf(pdf: PdfHistoryItem) {
        viewModelScope.launch {
            repository.deletePdf(pdf)
        }
    }

    fun getMostRecentPdf(callback: (PdfHistoryItem?) -> Unit) {
        viewModelScope.launch {
            val recentPdf = repository.getMostRecentPdf()
            callback(recentPdf)
        }
    }
}
