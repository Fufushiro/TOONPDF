package ia.ankherth.grease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ia.ankherth.grease.adapter.PdfHistoryAdapter
import ia.ankherth.grease.data.PdfHistoryItem
import ia.ankherth.grease.databinding.ActivityMainBinding
import ia.ankherth.grease.viewmodel.MainViewModel
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var pdfAdapter: PdfHistoryAdapter
    private var allPdfs: List<PdfHistoryItem> = emptyList()

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleNewPdfSelection(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeData()

        // Handle intent if app was opened with a PDF
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW) {
                handleNewPdfSelection(uri)
            }
        }
    }

    private fun setupUI() {
        // Set greeting based on time of day
        val greeting = getGreetingMessage()
        binding.textGreeting.text = greeting

        // Setup search functionality
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPdfs(s.toString())
            }
        })
    }

    private fun setupRecyclerView() {
        pdfAdapter = PdfHistoryAdapter(
            onPdfClick = { pdf -> openPdf(pdf) },
            onDeleteClick = { pdf -> deletePdf(pdf) }
        )

        binding.recyclerViewPdfs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = pdfAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPdf.setOnClickListener {
            openPdfPicker()
        }

        binding.buttonContinueReading.setOnClickListener {
            viewModel.getMostRecentPdf { recentPdf: PdfHistoryItem? ->
                recentPdf?.let { pdf -> openPdf(pdf) }
            }
        }
    }

    private fun observeData() {
        viewModel.allPdfs.observe(this) { pdfs: List<PdfHistoryItem> ->
            allPdfs = pdfs
            updateUI(pdfs)
        }
    }

    private fun updateUI(pdfs: List<PdfHistoryItem>) {
        if (pdfs.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewPdfs.visibility = View.GONE
            binding.cardContinueReading.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewPdfs.visibility = View.VISIBLE

            // Show most recent PDF in "Continue Reading" section
            val mostRecent = pdfs.firstOrNull()
            mostRecent?.let { pdf ->
                binding.cardContinueReading.visibility = View.VISIBLE
                binding.textContinueReadingTitle.text = pdf.fileName
                binding.textContinueReadingProgress.text =
                    "Página ${pdf.lastPageRead + 1} / ${pdf.totalPages} • ${pdf.progressPercentage.toInt()}%"
            }

            pdfAdapter.submitList(pdfs)
        }
    }

    private fun filterPdfs(query: String) {
        if (query.isBlank()) {
            pdfAdapter.submitList(allPdfs)
        } else {
            val filteredList = allPdfs.filter { pdf ->
                pdf.fileName.contains(query, ignoreCase = true)
            }
            pdfAdapter.submitList(filteredList)
        }
    }

    private fun openPdfPicker() {
        getContent.launch("application/pdf")
    }

    private fun handleNewPdfSelection(uri: Uri) {
        try {
            // Get file name from URI
            val fileName = getFileNameFromUri(uri) ?: "Documento PDF"

            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                putExtra(PdfViewerActivity.EXTRA_PDF_URI, uri)
                putExtra(PdfViewerActivity.EXTRA_FILE_NAME, fileName)
                putExtra(PdfViewerActivity.EXTRA_CURRENT_PAGE, 0)
            }
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPdf(pdf: PdfHistoryItem) {
        try {
            val uri = Uri.parse(pdf.uri)

            // Check if file still exists
            if (checkIfFileExists(uri)) {
                val intent = Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra(PdfViewerActivity.EXTRA_PDF_URI, uri)
                    putExtra(PdfViewerActivity.EXTRA_FILE_NAME, pdf.fileName)
                    putExtra(PdfViewerActivity.EXTRA_CURRENT_PAGE, pdf.lastPageRead)
                }
                startActivity(intent)
            } else {
                // File no longer exists, show message and remove from database
                Toast.makeText(this, "El archivo ya no existe en esta ubicación", Toast.LENGTH_LONG).show()
                viewModel.deletePdf(pdf)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir el PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun deletePdf(pdf: PdfHistoryItem) {
        viewModel.deletePdf(pdf)
        Toast.makeText(this, "PDF eliminado del historial", Toast.LENGTH_SHORT).show()
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex >= 0) {
                            it.getString(displayNameIndex)
                        } else null
                    } else null
                }
            }
            "file" -> {
                uri.lastPathSegment
            }
            else -> null
        }
    }

    private fun checkIfFileExists(uri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun getGreetingMessage(): String {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (currentHour) {
            in 0..11 -> "Buenos días, Usuario"
            in 12..17 -> "Buenas tardes, Usuario"
            else -> "Buenas noches, Usuario"
        }
    }
}
