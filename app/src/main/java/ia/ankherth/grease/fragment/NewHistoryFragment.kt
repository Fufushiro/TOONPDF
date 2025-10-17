package ia.ankherth.grease.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ia.ankherth.grease.PdfViewerActivity
import ia.ankherth.grease.R
import ia.ankherth.grease.adapter.PdfHistoryAdapter
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Fragment de historial que muestra todos los PDFs leídos
 */
class NewHistoryFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PdfHistoryAdapter
    private lateinit var emptyStateContainer: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        observeViewModel()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)

        adapter = PdfHistoryAdapter(
            onPdfClick = { pdf ->
                openPdfViewer(pdf)
            },
            onDeleteClick = { pdf ->
                showDeletePdfDialog(pdf)
            },
            onRelocateClick = { pdf ->
                // La funcionalidad de reubicar se maneja en MainActivity
                // Aquí podríamos mostrar un mensaje o dejar vacío
            },
            onLongPressDelete = { pdf ->
                showDeletePdfDialog(pdf)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NewHistoryFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.allPdfs.observe(viewLifecycleOwner) { pdfList ->
            if (pdfList.isNullOrEmpty()) {
                recyclerView.visibility = View.GONE
                emptyStateContainer.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyStateContainer.visibility = View.GONE
                adapter.submitList(pdfList)
            }
        }
    }

    private fun openPdfViewer(pdf: PdfHistoryEntity) {
        val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_PDF_URI, pdf.uri.toUri())
            putExtra(PdfViewerActivity.EXTRA_FILE_NAME, pdf.fileName)
            putExtra(PdfViewerActivity.EXTRA_CURRENT_PAGE, pdf.lastPageRead)
            putExtra(PdfViewerActivity.EXTRA_SCROLL_OFFSET, pdf.scrollOffset)
        }
        startActivity(intent)
    }

    private fun showDeletePdfDialog(pdf: PdfHistoryEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar PDF")
            .setMessage("¿Estás seguro de que quieres eliminar ${pdf.fileName} del historial?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deletePdf(pdf)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
