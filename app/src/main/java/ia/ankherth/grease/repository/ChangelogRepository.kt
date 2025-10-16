package ia.ankherth.grease.repository

import android.content.Context
import androidx.lifecycle.LiveData
import ia.ankherth.grease.data.room.AppDatabase
import ia.ankherth.grease.data.room.ChangelogDao
import ia.ankherth.grease.data.room.ChangelogEntry

/**
 * Repositorio para manejar el registro interno de cambios (changelog)
 * Permite registrar y consultar los cambios realizados en cada versión de la aplicación
 */
class ChangelogRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val changelogDao: ChangelogDao = database.changelogDao()

    // Obtener todos los cambios ordenados por fecha
    val allChanges: LiveData<List<ChangelogEntry>> = changelogDao.getAllChanges()

    // Obtener solo los cambios visibles para el usuario
    val userVisibleChanges: LiveData<List<ChangelogEntry>> = changelogDao.getUserVisibleChanges()

    // Obtener lista de versiones únicas
    val allVersions: LiveData<List<String>> = changelogDao.getAllVersions()

    /**
     * Agrega una nueva entrada al registro de cambios
     */
    suspend fun addChangelogEntry(
        versionName: String,
        versionCode: Int,
        changeDescription: String,
        changeType: String,
        isUserVisible: Boolean = true
    ) {
        val entry = ChangelogEntry(
            versionName = versionName,
            versionCode = versionCode,
            changeDescription = changeDescription,
            changeType = changeType,
            isUserVisible = isUserVisible,
            changeDate = System.currentTimeMillis()
        )
        changelogDao.insert(entry)
    }

    /**
     * Agrega múltiples entradas al registro de cambios
     */
    suspend fun addBulkChanges(entries: List<ChangelogEntry>) {
        changelogDao.insertAll(entries)
    }

    /**
     * Obtiene los cambios de una versión específica
     */
    fun getChangesByVersion(versionName: String): LiveData<List<ChangelogEntry>> {
        return changelogDao.getChangesByVersion(versionName)
    }
}
