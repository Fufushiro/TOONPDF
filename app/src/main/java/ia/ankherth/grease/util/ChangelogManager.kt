package ia.ankherth.grease.util

import android.content.Context
import android.content.SharedPreferences
import ia.ankherth.grease.BuildConfig
import ia.ankherth.grease.data.room.ChangelogEntry
import ia.ankherth.grease.repository.ChangelogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gestor del registro de cambios (changelog)
 * Inicializa y mantiene el registro de cambios de la aplicación
 */
class ChangelogManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "changelog_prefs", Context.MODE_PRIVATE
    )
    private val repository = ChangelogRepository(context)

    /**
     * Inicializa el registro de cambios para la versión actual si es necesario
     * Se llama cuando se actualiza la aplicación a una nueva versión
     */
    fun initializeChangelog() {
        val lastVersionCode = sharedPreferences.getInt("last_version_code", -1)
        val currentVersionCode = BuildConfig.VERSION_CODE

        // Si es una nueva instalación o actualización, inicializar los cambios
        if (lastVersionCode < currentVersionCode) {
            CoroutineScope(Dispatchers.IO).launch {
                addChangelogEntries()
                sharedPreferences.edit()
                    .putInt("last_version_code", currentVersionCode)
                    .apply()
            }
        }
    }

    /**
     * Añade los cambios para la versión actual
     * Este método debe actualizarse para cada nueva versión
     */
    private suspend fun addChangelogEntries() {
        val currentVersion = BuildConfig.VERSION_NAME
        val currentVersionCode = BuildConfig.VERSION_CODE
        val currentTime = System.currentTimeMillis()

        // Fecha fija para mantener el changelog del 15 de octubre de 2025
        // para la versión 0.2.4 (octubre 15, 2025)
        val fixedDate = 1760582400000L // 15 de octubre de 2025 en milisegundos

        when (currentVersionCode) {
            8 -> { // Versión 4.5.1 - Nueva pantalla de inicio y compatibilidad Android 15
                val changes = listOf(
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = currentTime,
                        changeDescription = "Nueva pantalla de inicio rediseñada con tarjeta destacada de última lectura",
                        changeType = "FEATURE",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = currentTime,
                        changeDescription = "Saludo personalizado y sección de PDFs recientes en el inicio",
                        changeType = "FEATURE",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = currentTime,
                        changeDescription = "Compatibilidad con Android 15 y dispositivos con páginas de 16 KB",
                        changeType = "IMPROVEMENT",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = currentTime,
                        changeDescription = "Actualización a NDK r27 para soporte de Pixel 9 y ARM v9+",
                        changeType = "IMPROVEMENT",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = currentTime,
                        changeDescription = "Optimización de compilación (30-50% más rápido) y reducción del APK",
                        changeType = "IMPROVEMENT",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = currentTime,
                        changeDescription = "Diseño Material You con tarjetas redondeadas y degradados modernos",
                        changeType = "FEATURE",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = currentTime,
                        changeDescription = "Corrección de APIs deprecadas y build sin advertencias",
                        changeType = "BUGFIX",
                        isUserVisible = true
                    )
                )
                repository.addBulkChanges(changes)
            }
            3, 4 -> { // Versiones 0.1.3 y 0.2.4 (misma fecha, mismas entradas)
                val changes = listOf(
                    ChangelogEntry(
                        versionName = currentVersion, // Usa la versión actual (0.1.3 o 0.2.4)
                        versionCode = currentVersionCode.toInt(), // Usar toInt() para asegurar tipo correcto
                        changeDate = fixedDate, // Usa la fecha fija del 15 de octubre de 2025
                        changeDescription = "Persistencia mejorada de PDFs abiertos con historial completo",
                        changeType = "IMPROVEMENT",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = fixedDate,
                        changeDescription = "Implementado pull-to-refresh para actualizar el progreso de lectura",
                        changeType = "FEATURE",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = fixedDate,
                        changeDescription = "Permisos de almacenamiento persistentes con opción configurable",
                        changeType = "FEATURE",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = fixedDate,
                        changeDescription = "Manejo mejorado de errores para PDFs eliminados o movidos",
                        changeType = "BUGFIX",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = fixedDate,
                        changeDescription = "Implementado sistema interno de registro de cambios",
                        changeType = "FEATURE",
                        isUserVisible = true
                    ),
                    ChangelogEntry(
                        versionName = currentVersion,
                        versionCode = currentVersionCode.toInt(),
                        changeDate = fixedDate,
                        changeDescription = "Migración de SharedPreferences a Room Database para mayor robustez",
                        changeType = "IMPROVEMENT",
                        isUserVisible = false
                    )
                )
                repository.addBulkChanges(changes)
            }
            // Para futuras versiones, agregar nuevos casos aquí
            else -> {
                // Si no hay entradas específicas para esta versión, agregar una genérica
                repository.addChangelogEntry(
                    versionName = currentVersion,
                    versionCode = currentVersionCode.toInt(),
                    changeDescription = "Actualización a versión $currentVersion",
                    changeType = "IMPROVEMENT",
                    isUserVisible = true
                )
            }
        }
    }

    /**
     * Registra manualmente un nuevo cambio en la aplicación
     */
    fun logChange(description: String, type: String, isUserVisible: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.addChangelogEntry(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE.toInt(),
                changeDescription = description,
                changeType = type,
                isUserVisible = isUserVisible
            )
        }
    }
}
