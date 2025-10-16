package ia.ankherth.grease.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.databinding.ItemPdfHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador mejorado para mostrar el historial de PDFs
 * Incluye funcionalidad para gestionar PDFs inaccesibles y opciones para reubicación
 */
class PdfHistoryAdapter(
    private val onPdfClick: (PdfHistoryEntity) -> Unit,
    private val onDeleteClick: (PdfHistoryEntity) -> Unit,
    private val onRelocateClick: (PdfHistoryEntity) -> Unit
) : ListAdapter<PdfHistoryEntity, PdfHistoryAdapter.PdfViewHolder>(PdfDiffCallback()) {

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

        fun bind(pdf: PdfHistoryEntity) {
            binding.apply {
                // Configurar información básica del PDF
                textFileName.text = pdf.fileName
                textPageInfo.text = "Página ${pdf.lastPageRead + 1} / ${pdf.totalPages}"
                textLastRead.text = "Última lectura: ${getRelativeTime(pdf.lastReadDate)}"

                // Configurar barra de progreso
                val progress = pdf.progressPercentage.toInt()
                progressBar.progress = progress
                textProgress.text = "${progress}%"

                // Mostrar indicador de accesibilidad si el archivo no es accesible
                if (!pdf.isAccessible) {
                    textInaccessible.visibility = View.VISIBLE
                    buttonRelocate.visibility = View.VISIBLE
                    root.alpha = 0.7f
                } else {
                    textInaccessible.visibility = View.GONE
                    buttonRelocate.visibility = View.GONE
                    root.alpha = 1.0f
                }

                // Configurar listeners de clics
                root.setOnClickListener { onPdfClick(pdf) }
                buttonDelete.setOnClickListener { onDeleteClick(pdf) }
                buttonRelocate.setOnClickListener { onRelocateClick(pdf) }
            }
        }

        private fun getRelativeTime(timestamp: Long): String {
            val date = Date(timestamp)
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

    class PdfDiffCallback : DiffUtil.ItemCallback<PdfHistoryEntity>() {
        override fun areItemsTheSame(oldItem: PdfHistoryEntity, newItem: PdfHistoryEntity): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: PdfHistoryEntity, newItem: PdfHistoryEntity): Boolean {
            return oldItem == newItem
        }
    }
}
