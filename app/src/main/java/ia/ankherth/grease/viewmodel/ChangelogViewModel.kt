package ia.ankherth.grease.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import ia.ankherth.grease.BuildConfig
import ia.ankherth.grease.data.room.ChangelogEntry
import ia.ankherth.grease.repository.ChangelogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar el sistema de changelog interno
 * Proporciona acceso al registro de cambios de la aplicación
 */
class ChangelogViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChangelogRepository(application)

    // LiveData para todos los cambios
    val allChanges: LiveData<List<ChangelogEntry>> = repository.allChanges

    // LiveData para cambios visibles por el usuario
    val userVisibleChanges: LiveData<List<ChangelogEntry>> = repository.userVisibleChanges

    // Lista de versiones únicas
    val allVersions: LiveData<List<String>> = repository.allVersions

    /**
     * Registra un nuevo cambio en la aplicación
     */
    fun addChangelogEntry(
        description: String,
        changeType: String,
        isUserVisible: Boolean = true
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.addChangelogEntry(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE.toInt(), // Usar toInt() para asegurar tipo correcto
                changeDescription = description,
                changeType = changeType,
                isUserVisible = isUserVisible
            )
        }
    }

    /**
     * Obtiene los cambios de una versión específica
     */
    fun getChangesByVersion(versionName: String): LiveData<List<ChangelogEntry>> {
        return repository.getChangesByVersion(versionName)
    }

    /**
     * Registra cambios iniciales para la versión actual si el changelog está vacío
     * Útil para mostrar un historial inicial de cambios en la primera ejecución después de actualizar
     */
    fun initializeChangelogIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            // Aquí se puede agregar código para verificar si hay cambios en la versión actual
            // y agregar entradas iniciales si es necesario
        }
    }
}
