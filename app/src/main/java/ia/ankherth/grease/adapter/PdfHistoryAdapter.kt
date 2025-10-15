package ia.ankherth.grease.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ia.ankherth.grease.data.PdfHistoryItem
import ia.ankherth.grease.databinding.ItemPdfHistoryBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class PdfHistoryAdapter(
    private val onPdfClick: (PdfHistoryItem) -> Unit,
    private val onDeleteClick: (PdfHistoryItem) -> Unit
) : ListAdapter<PdfHistoryItem, PdfHistoryAdapter.PdfViewHolder>(PdfDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val binding = ItemPdfHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PdfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PdfViewHolder(private val binding: ItemPdfHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pdf: PdfHistoryItem) {
            binding.apply {
                textFileName.text = pdf.fileName
                textPageInfo.text = "Página ${pdf.lastPageRead} / ${pdf.totalPages}"
                textLastRead.text = "Última lectura: ${getRelativeTime(pdf.lastReadDate)}"

                // Set progress
                val progress = pdf.progressPercentage.toInt()
                progressBar.progress = progress
                textProgress.text = "${progress}%"

                // Set click listeners
                root.setOnClickListener { onPdfClick(pdf) }
                buttonDelete.setOnClickListener { onDeleteClick(pdf) }
            }
        }

        private fun getRelativeTime(date: Date): String {
            val now = Date()
            val diffInMillis = now.time - date.time
            val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

            return when {
                diffInDays == 0L -> "Hoy"
                diffInDays == 1L -> "Ayer"
                diffInDays < 7 -> "hace ${diffInDays} días"
                diffInDays < 30 -> "hace ${diffInDays / 7} semanas"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
            }
        }
    }

    class PdfDiffCallback : DiffUtil.ItemCallback<PdfHistoryItem>() {
        override fun areItemsTheSame(oldItem: PdfHistoryItem, newItem: PdfHistoryItem): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: PdfHistoryItem, newItem: PdfHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
