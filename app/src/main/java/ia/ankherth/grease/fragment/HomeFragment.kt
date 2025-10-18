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
import androidx.cardview.widget.CardView
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
import java.util.Calendar

class HomeFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var pdfAdapter: PdfHistoryAdapter

    // Views
    private lateinit var textGreeting: TextView
    private lateinit var editTextSearch: EditText
    private lateinit var buttonClearSearch: ImageButton
    private lateinit var recyclerViewPdfs: RecyclerView
    private lateinit var cardContinueReading: CardView
    private lateinit var textContinueReadingTitle: TextView
    private lateinit var textContinueReadingProgress: TextView
    private lateinit var buttonContinueReading: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var layoutEmptyState: View
    private lateinit var imageUserAvatar: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
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
        textGreeting = view.findViewById(R.id.textGreeting)
        editTextSearch = view.findViewById(R.id.editTextSearch)
        buttonClearSearch = view.findViewById(R.id.buttonClearSearch)
        recyclerViewPdfs = view.findViewById(R.id.recyclerViewPdfs)
        cardContinueReading = view.findViewById(R.id.cardContinueReading)
        textContinueReadingTitle = view.findViewById(R.id.textContinueReadingTitle)
        textContinueReadingProgress = view.findViewById(R.id.textContinueReadingProgress)
        buttonContinueReading = view.findViewById(R.id.buttonContinueReading)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        imageUserAvatar = view.findViewById(R.id.imageUserAvatar)
    }

    private fun setupUI() {
        updateGreeting()

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

        buttonContinueReading.setOnClickListener {
            viewModel.getLastReadPdf()?.let { pdf ->
                openPdf(pdf)
            }
        }

        imageUserAvatar.setOnClickListener {
            // Handle avatar click if needed
        }
    }

    private fun setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPdfs()
        }
    }

    private fun observeData() {
        viewModel.allPdfs.observe(viewLifecycleOwner) { pdfs ->
            // Show only last 2 PDFs in home
            val recentPdfs = pdfs.take(2)
            pdfAdapter.submitList(recentPdfs)
            updateEmptyState(recentPdfs.isEmpty())
            swipeRefreshLayout.isRefreshing = false
        }

        viewModel.lastReadPdf.observe(viewLifecycleOwner) { pdf ->
            updateContinueReadingCard(pdf)
        }

        viewModel.userName.observe(viewLifecycleOwner) {
            updateGreeting()
        }

        viewModel.userAvatarUri.observe(viewLifecycleOwner) { uriString ->
            updateUserAvatar(uriString)
        }
    }

    private fun updateGreeting() {
        val userName = viewModel.userName.value ?: "Usuario"
        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 6..11 -> "Buenos días"
            in 12..17 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        textGreeting.text = "$greeting, $userName"
    }

    private fun updateUserAvatar(uriString: String?) {
        if (!uriString.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(uriString)
                imageUserAvatar.setImageURI(uri)
            } catch (e: Exception) {
                imageUserAvatar.setImageResource(R.drawable.ic_person)
            }
        } else {
            imageUserAvatar.setImageResource(R.drawable.ic_person)
        }
    }

    private fun updateContinueReadingCard(pdf: PdfHistoryEntity?) {
        if (pdf != null && pdf.lastPageRead > 0) {
            cardContinueReading.isVisible = true
            textContinueReadingTitle.text = pdf.fileName
            val progress = if (pdf.totalPages > 0) {
                val percentage = ((pdf.lastPageRead.toFloat() / pdf.totalPages) * 100).toInt()
                "Página ${pdf.lastPageRead + 1} / ${pdf.totalPages} • $percentage%"
            } else {
                "Página ${pdf.lastPageRead + 1}"
            }
            textContinueReadingProgress.text = progress
        } else {
            cardContinueReading.isVisible = false
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        layoutEmptyState.isVisible = isEmpty
        recyclerViewPdfs.isVisible = !isEmpty
    }

    private fun filterPdfs(query: String) {
        // HomeFragment shows only recent PDFs - filtering handled by observing allPdfs
        // Search functionality should be in LibraryFragment
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
