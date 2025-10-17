package ia.ankherth.grease.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ia.ankherth.grease.model.PdfItem

/**
 * ViewModel para gestionar los datos de PDFs en Home e Historial
 */
class PdfViewModel : ViewModel() {

    private val _pdfList = MutableLiveData<List<PdfItem>>()
    val pdfList: LiveData<List<PdfItem>> = _pdfList

    private val _lastRead = MutableLiveData<PdfItem?>()
    val lastRead: LiveData<PdfItem?> = _lastRead

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        val currentTime = System.currentTimeMillis()
        _pdfList.value = listOf(
            PdfItem(
                1,
                "Introducción a Diseño UI.pdf",
                50,
                40,
                currentTime - 3_600_000 // 1 hora
            ),
            PdfItem(
                2,
                "UIUX_Basics_Manual.pdf",
                30,
                27,
                currentTime - 86_400_000 // 1 día
            ),
            PdfItem(
                3,
                "Motion_Design_Primer.pdf",
                120,
                12,
                currentTime - 172_800_000 // 2 días
            ),
            PdfItem(
                4,
                "Android_Development_Guide.pdf",
                200,
                150,
                currentTime - 259_200_000 // 3 días
            ),
            PdfItem(
                5,
                "Material_Design_Guidelines.pdf",
                80,
                60,
                currentTime - 432_000_000 // 5 días
            )
        )
        _lastRead.value = _pdfList.value?.firstOrNull()
    }

    fun refreshData() {
        loadSampleData()
    }
}

