package ia.ankherth.grease.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ia.ankherth.grease.PdfViewerActivity
import ia.ankherth.grease.R
import ia.ankherth.grease.adapter.PdfHistoryAdapter
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class PdfsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var pdfAdapter: PdfHistoryAdapter
    private var allPdfs: List<PdfHistoryEntity> = emptyList()

    // Views
    private lateinit var editTextSearch: EditText
    private lateinit var buttonClearSearch: ImageButton
    private lateinit var recyclerViewPdfs: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var layoutEmptyState: View
    private lateinit var textPdfCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdfs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupUI()
        setupRecyclerView()
        setupClickListeners()
        setupRefreshLayout()
        observeData()
    }

    private fun initViews(view: View) {
        editTextSearch = view.findViewById(R.id.editTextSearch)
        buttonClearSearch = view.findViewById(R.id.buttonClearSearch)
        recyclerViewPdfs = view.findViewById(R.id.recyclerViewPdfs)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        textPdfCount = view.findViewById(R.id.textPdfCount)
    }

    private fun setupUI() {
        // Setup search functionality
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                buttonClearSearch.isVisible = query.isNotEmpty()
                filterPdfs(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupRecyclerView() {
        pdfAdapter = PdfHistoryAdapter(
            onPdfClick = { pdf -> openPdf(pdf) },
            onDeleteClick = { pdf ->
                // Handle delete action - show confirmation dialog
                showDeleteConfirmationDialog(pdf)
            },
            onRelocateClick = { pdf ->
                // Handle relocate action
                showRelocateDialog(pdf)
            }
        )

        recyclerViewPdfs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pdfAdapter
        }
    }

    private fun setupClickListeners() {
        buttonClearSearch.setOnClickListener {
            editTextSearch.text.clear()
        }
    }

    private fun setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPdfs()
        }
    }

    private fun observeData() {
        viewModel.allPdfs.observe(viewLifecycleOwner) { pdfs ->
            allPdfs = pdfs
            pdfAdapter.submitList(pdfs)
            updateEmptyState(pdfs.isEmpty())
            updatePdfCount(pdfs.size)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun updatePdfCount(count: Int) {
        textPdfCount.text = when (count) {
            0 -> "No hay PDFs"
            1 -> "1 PDF"
            else -> "$count PDFs"
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        layoutEmptyState.isVisible = isEmpty
        recyclerViewPdfs.isVisible = !isEmpty
    }

    private fun filterPdfs(query: String) {
        if (query.isEmpty()) {
            pdfAdapter.submitList(allPdfs)
        } else {
            val filteredPdfs = allPdfs.filter { pdf ->
                pdf.fileName.contains(query, ignoreCase = true)
            }
            pdfAdapter.submitList(filteredPdfs)
        }
    }

    private fun openPdf(pdf: PdfHistoryEntity) {
        lifecycleScope.launch {
            try {
                val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
                    putExtra(PdfViewerActivity.EXTRA_PDF_URI, Uri.parse(pdf.uri))
                    putExtra(PdfViewerActivity.EXTRA_FILE_NAME, pdf.fileName)
                    putExtra(PdfViewerActivity.EXTRA_CURRENT_PAGE, pdf.lastPageRead)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Handle error - PDF not found
                showPdfNotFoundDialog(pdf)
            }
        }
    }

    private fun showPdfOptionsDialog(pdf: PdfHistoryEntity) {
        // Implementation for PDF options dialog
    }

    private fun showPdfNotFoundDialog(pdf: PdfHistoryEntity) {
        // Implementation for PDF not found dialog
    }

    private fun showDeleteConfirmationDialog(pdf: PdfHistoryEntity) {
        // Implementation for delete confirmation dialog
    }

    private fun showRelocateDialog(pdf: PdfHistoryEntity) {
        // Implementation for relocate dialog
    }
}
