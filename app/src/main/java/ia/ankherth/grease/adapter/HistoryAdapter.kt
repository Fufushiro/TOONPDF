package ia.ankherth.grease.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ia.ankherth.grease.R
import ia.ankherth.grease.model.PdfItem
import ia.ankherth.grease.util.TimeUtils

/**
 * Adapter para mostrar el historial de PDFs en un RecyclerView
 */
class HistoryAdapter(
    private val onItemClick: (PdfItem) -> Unit
) : ListAdapter<PdfItem, HistoryAdapter.PdfViewHolder>(PdfDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pdf_history, parent, false)
        return PdfViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PdfViewHolder(
        itemView: View,
        private val onItemClick: (PdfItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: CardView = itemView.findViewById(R.id.cardPdfItem)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvPdfTitle)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvPdfMeta)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)

        fun bind(pdf: PdfItem) {
            tvTitle.text = pdf.title
            tvMeta.text = "Página ${pdf.lastReadPage} de ${pdf.totalPages} • ${TimeUtils.formatRelative(pdf.lastReadMillis)}"
            progressBar.progress = pdf.progressPercent
            tvProgress.text = "${pdf.progressPercent}%"

            cardView.setOnClickListener {
                onItemClick(pdf)
            }
        }
    }

    private class PdfDiffCallback : DiffUtil.ItemCallback<PdfItem>() {
        override fun areItemsTheSame(oldItem: PdfItem, newItem: PdfItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PdfItem, newItem: PdfItem): Boolean {
            return oldItem == newItem
        }
    }
}

