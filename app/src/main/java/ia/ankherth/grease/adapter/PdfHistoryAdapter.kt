package ia.ankherth.grease.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    @Suppress("unused") private val onRelocateClick: (PdfHistoryEntity) -> Unit,
    @Suppress("unused") private val onLongPressDelete: ((PdfHistoryEntity) -> Unit)? = null
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

            // Long-press listener para mostrar menú de opciones
            itemView.setOnLongClickListener {
                itemView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                showOptionsBottomSheet(pdf)
                true
            }
        }

        private fun showOptionsBottomSheet(pdf: PdfHistoryEntity) {
            val bottomSheet = BottomSheetDialog(itemView.context)
            val view = LayoutInflater.from(itemView.context)
                .inflate(R.layout.bottom_sheet_pdf_options, null)

            bottomSheet.setContentView(view)

            // Opción: Ver información
            view.findViewById<View>(R.id.optionInfo).setOnClickListener {
                bottomSheet.dismiss()
                showInfoDialog(pdf)
            }

            // Opción: Eliminar
            view.findViewById<View>(R.id.optionDelete).setOnClickListener {
                bottomSheet.dismiss()
                showDeleteConfirmation(pdf)
            }

            bottomSheet.show()
        }

        private fun showInfoDialog(pdf: PdfHistoryEntity) {
            val dialog = MaterialAlertDialogBuilder(itemView.context)
                .setView(R.layout.dialog_pdf_info)
                .create()

            dialog.show()

            // Obtener las vistas del diálogo
            val tvFileName = dialog.findViewById<TextView>(R.id.tvFileName)
            val tvFileSize = dialog.findViewById<TextView>(R.id.tvFileSize)
            val tvFilePath = dialog.findViewById<TextView>(R.id.tvFilePath)
            val tvTotalPages = dialog.findViewById<TextView>(R.id.tvTotalPages)
            val tvProgress = dialog.findViewById<TextView>(R.id.tvProgress)
            val tvLastRead = dialog.findViewById<TextView>(R.id.tvLastRead)
            val btnClose = dialog.findViewById<View>(R.id.btnClose)

            // Llenar la información
            tvFileName?.text = pdf.fileName
            tvFileSize?.text = formatFileSize(pdf.fileSizeBytes)
            tvFilePath?.text = pdf.filePath ?: "Ubicación desconocida"
            tvTotalPages?.text = "${pdf.totalPages} páginas"

            val percentage = if (pdf.totalPages > 0) {
                ((pdf.lastPageRead.toFloat() / pdf.totalPages) * 100).toInt()
            } else 0
            tvProgress?.text = "Página ${pdf.lastPageRead + 1} de ${pdf.totalPages} ($percentage%)"
            tvLastRead?.text = getRelativeTime(pdf.lastReadDate)

            btnClose?.setOnClickListener {
                dialog.dismiss()
            }
        }

        private fun showDeleteConfirmation(pdf: PdfHistoryEntity) {
            MaterialAlertDialogBuilder(itemView.context)
                .setTitle("Eliminar del historial")
                .setMessage("¿Estás seguro de que quieres eliminar \"${pdf.fileName}\" del historial?\n\nEl archivo no será eliminado del dispositivo.")
                .setPositiveButton("Eliminar") { _, _ ->
                    onDeleteClick(pdf)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        private fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "Tamaño desconocido"

            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0

            return when {
                gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
                kb >= 1.0 -> String.format(Locale.getDefault(), "%.2f KB", kb)
                else -> "$bytes bytes"
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
