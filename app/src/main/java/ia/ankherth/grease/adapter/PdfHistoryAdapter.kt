package ia.ankherth.grease.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import ia.ankherth.grease.R
import ia.ankherth.grease.data.room.PdfHistoryEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador compacto para mostrar el historial de PDFs
 */
class PdfHistoryAdapter(
    private val onPdfClick: (PdfHistoryEntity) -> Unit,
    private val onDeleteClick: (PdfHistoryEntity) -> Unit,
    private val onRelocateClick: (PdfHistoryEntity) -> Unit,
    private val onLongPressDelete: ((PdfHistoryEntity) -> Unit)? = null
) : ListAdapter<PdfHistoryEntity, PdfHistoryAdapter.PdfViewHolder>(PdfDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_history, parent, false)
        return PdfViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PdfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Updated to use the correct view IDs from our new layout
        private val ivPdfPreview: ImageView = itemView.findViewById(R.id.ivPdfPreview)
        private val tvPdfTitle: TextView = itemView.findViewById(R.id.tvPdfTitle)
        private val tvPdfMeta: TextView = itemView.findViewById(R.id.tvPdfMeta)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)

        fun bind(pdf: PdfHistoryEntity) {
            tvPdfTitle.text = pdf.fileName

            // Cargar miniatura si existe
            pdf.thumbnailPath?.let { path ->
                ivPdfPreview.load(File(path)) {
                    crossfade(true)
                    placeholder(R.drawable.pdf_thumbnail_placeholder)
                    error(R.drawable.pdf_thumbnail_placeholder)
                }
            } ?: ivPdfPreview.setImageResource(R.drawable.pdf_thumbnail_placeholder)


            // Mostrar progreso y última lectura
            val progressText = if (pdf.totalPages > 0) {
                val percentage = ((pdf.lastPageRead.toFloat() / pdf.totalPages) * 100).toInt()
                progressBar.progress = percentage
                tvProgress.text = "$percentage%"
                "Página ${pdf.lastPageRead + 1} de ${pdf.totalPages} • ${getRelativeTime(pdf.lastReadDate)}"
            } else {
                progressBar.progress = 0
                tvProgress.text = "0%"
                "Página ${pdf.lastPageRead + 1} • ${getRelativeTime(pdf.lastReadDate)}"
            }
            tvPdfMeta.text = progressText

            // Click listeners
            itemView.setOnClickListener { onPdfClick(pdf) }

            // Long-press listener para eliminar del historial
            itemView.setOnLongClickListener {
                itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onLongPressDelete?.invoke(pdf) ?: onDeleteClick(pdf)
                true
            }
        }

        private fun getRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60_000 -> "Ahora"
                diff < 3600_000 -> "Hace ${diff / 60_000} min"
                diff < 86400_000 -> "Hace ${diff / 3600_000}h"
                diff < 7 * 86400_000 -> "Hace ${diff / 86400_000} días"
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class PdfDiffCallback : DiffUtil.ItemCallback<PdfHistoryEntity>() {
        override fun areItemsTheSame(oldItem: PdfHistoryEntity, newItem: PdfHistoryEntity): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: PdfHistoryEntity, newItem: PdfHistoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
