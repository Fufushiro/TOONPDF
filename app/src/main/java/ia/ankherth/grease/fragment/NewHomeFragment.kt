package ia.ankherth.grease.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ia.ankherth.grease.R
import ia.ankherth.grease.util.TimeUtils
import ia.ankherth.grease.viewmodel.PdfViewModel

/**
 * Fragment de inicio que muestra la tarjeta destacada con la última lectura
 */
class NewHomeFragment : Fragment() {

    private val viewModel: PdfViewModel by activityViewModels()

    private lateinit var emptyStateContainer: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        observeViewModel()
    }

    private fun initViews(view: View) {
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)

        // Get the included card view
        val cardLastPdf = view.findViewById<View>(R.id.cardLastPdf)

        cardLastPdf.setOnClickListener {
            viewModel.lastRead.value?.let { pdf ->
                Toast.makeText(
                    requireContext(),
                    "Abriendo ${pdf.title}...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.lastRead.observe(viewLifecycleOwner) { lastPdf ->
            val cardLastPdf = view?.findViewById<View>(R.id.cardLastPdf)

            if (lastPdf != null && cardLastPdf != null) {
                cardLastPdf.visibility = View.VISIBLE
                emptyStateContainer.visibility = View.GONE

                // Access views from the included layout
                val tvPdfTitle = cardLastPdf.findViewById<TextView>(R.id.tvPdfTitle)
                val tvPdfMeta = cardLastPdf.findViewById<TextView>(R.id.tvPdfMeta)
                val tvLastRead = cardLastPdf.findViewById<TextView>(R.id.tvLastRead)
                val progressBar = cardLastPdf.findViewById<ProgressBar>(R.id.progressBar)
                val tvProgress = cardLastPdf.findViewById<TextView>(R.id.tvProgress)

                tvPdfTitle.text = lastPdf.title
                tvPdfMeta.text = "Página ${lastPdf.lastReadPage} de ${lastPdf.totalPages}"
                tvLastRead.text = "Leído ${TimeUtils.formatRelative(lastPdf.lastReadMillis)}"
                progressBar.progress = lastPdf.progressPercent
                tvProgress.text = "${lastPdf.progressPercent}%"
            } else {
                cardLastPdf?.visibility = View.GONE
                emptyStateContainer.visibility = View.VISIBLE
            }
        }
    }
}
