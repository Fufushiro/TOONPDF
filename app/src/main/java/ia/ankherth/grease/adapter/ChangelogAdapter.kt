package ia.ankherth.grease.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ia.ankherth.grease.R
import ia.ankherth.grease.data.room.ChangelogEntry
import ia.ankherth.grease.databinding.ItemChangelogEntryBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador para mostrar las entradas del registro de cambios (changelog)
 * Muestra información sobre cada cambio, incluyendo tipo, versión y fecha
 */
class ChangelogAdapter : ListAdapter<ChangelogEntry, ChangelogAdapter.ChangelogViewHolder>(ChangelogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangelogViewHolder {
        val binding = ItemChangelogEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChangelogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChangelogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChangelogViewHolder(private val binding: ItemChangelogEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: ChangelogEntry) {
            binding.apply {
                textVersion.text = "v${entry.versionName}"
                textDate.text = formatDate(entry.changeDate)
                textChangeDescription.text = entry.changeDescription

                // Configurar icono según el tipo de cambio
                val iconRes = when (entry.changeType.uppercase()) {
                    "FEATURE" -> R.drawable.ic_feature
                    "BUGFIX" -> R.drawable.ic_bugfix
                    "IMPROVEMENT" -> R.drawable.ic_improvement
                    else -> R.drawable.ic_change
                }
                imageChangeType.setImageResource(iconRes)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    class ChangelogDiffCallback : DiffUtil.ItemCallback<ChangelogEntry>() {
        override fun areItemsTheSame(oldItem: ChangelogEntry, newItem: ChangelogEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChangelogEntry, newItem: ChangelogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
