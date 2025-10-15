package ia.ankherth.grease.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ia.ankherth.grease.data.PdfHistoryItem
import java.util.Date

class PdfRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("pdf_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _allPdfs = MutableLiveData<List<PdfHistoryItem>>()
    val allPdfs: LiveData<List<PdfHistoryItem>> = _allPdfs

    init {
        loadPdfs()
    }

    private fun loadPdfs() {
        val pdfsJson = sharedPreferences.getString("pdfs", "[]")
        val type = object : TypeToken<List<PdfHistoryItem>>() {}.type
        val pdfs: List<PdfHistoryItem> = gson.fromJson(pdfsJson, type) ?: emptyList()
        _allPdfs.value = pdfs.sortedByDescending { it.lastReadDate }
    }

    fun addOrUpdatePdf(uri: String, fileName: String, totalPages: Int, currentPage: Int = 0) {
        val currentPdfs = _allPdfs.value?.toMutableList() ?: mutableListOf()
        val existingIndex = currentPdfs.indexOfFirst { it.uri == uri }

        if (existingIndex >= 0) {
            // Update existing PDF
            val existing = currentPdfs[existingIndex]
            currentPdfs[existingIndex] = existing.copy(
                lastPageRead = currentPage,
                lastReadDate = Date(),
                totalPages = totalPages,
                fileName = fileName
            )
        } else {
            // Add new PDF
            val newPdf = PdfHistoryItem(
                uri = uri,
                fileName = fileName,
                totalPages = totalPages,
                lastPageRead = currentPage,
                lastReadDate = Date()
            )
            currentPdfs.add(0, newPdf)
        }

        savePdfs(currentPdfs)
    }

    fun updateProgress(uri: String, pageNumber: Int) {
        val currentPdfs = _allPdfs.value?.toMutableList() ?: mutableListOf()
        val existingIndex = currentPdfs.indexOfFirst { it.uri == uri }

        if (existingIndex >= 0) {
            val existing = currentPdfs[existingIndex]
            currentPdfs[existingIndex] = existing.copy(
                lastPageRead = pageNumber,
                lastReadDate = Date()
            )
            savePdfs(currentPdfs)
        }
    }

    fun deletePdf(pdf: PdfHistoryItem) {
        val currentPdfs = _allPdfs.value?.toMutableList() ?: mutableListOf()
        currentPdfs.removeAll { it.uri == pdf.uri }
        savePdfs(currentPdfs)
    }

    fun getMostRecentPdf(): PdfHistoryItem? {
        return _allPdfs.value?.firstOrNull()
    }

    private fun savePdfs(pdfs: List<PdfHistoryItem>) {
        val sortedPdfs = pdfs.sortedByDescending { it.lastReadDate }
        val pdfsJson = gson.toJson(sortedPdfs)
        sharedPreferences.edit().putString("pdfs", pdfsJson).apply()
        _allPdfs.value = sortedPdfs
    }
}
