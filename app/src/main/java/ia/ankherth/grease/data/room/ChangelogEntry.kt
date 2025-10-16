package ia.ankherth.grease.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para el registro interno de cambios (changelog)
 * Permite mantener un historial de las modificaciones realizadas en cada versión de la aplicación
 */
@Entity(tableName = "changelog_entries")
data class ChangelogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val versionName: String,
    val versionCode: Int,
    val changeDate: Long = System.currentTimeMillis(),
    val changeDescription: String,
    val changeType: String, // "FEATURE", "BUGFIX", "IMPROVEMENT", etc.
    val isUserVisible: Boolean = true // Para filtrar cambios que no son relevantes para el usuario
)
