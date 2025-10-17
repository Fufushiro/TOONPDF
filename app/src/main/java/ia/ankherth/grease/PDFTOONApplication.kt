package ia.ankherth.grease

import android.app.Application
import android.util.Log
import ia.ankherth.grease.util.ChangelogManager
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Clase de aplicación personalizada que inicializa los componentes necesarios
 * al iniciar la aplicación, como el gestor de registro de cambios
 */
class PDFTOONApplication : Application() {

    private lateinit var changelogManager: ChangelogManager

    override fun onCreate() {
        super.onCreate()

        // Instalar un manejador global de excepciones para capturar crashes en producción/local
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                // Guardar en archivo interno para poder recuperar el stacktrace desde el dispositivo
                val crashFile = File(filesDir, "last_crash_log.txt")
                crashFile.writeText("Thread: ${thread.name}\n\n$stackTrace")

                Log.e("PDFTOONApplication", "Unhandled exception, written to ${crashFile.absolutePath}", throwable)
            } catch (e: Exception) {
                // Si falló escribir el archivo, al menos loguear
                Log.e("PDFTOONApplication", "Failed to write crash log", e)
            }

            // Rellanzar la excepción al sistema para que el proceso termine como de costumbre
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Inicializar el gestor de registro de cambios de forma segura
        try {
            changelogManager = ChangelogManager(this)
            changelogManager.initializeChangelog()
        } catch (e: Exception) {
            Log.e("PDFTOONApplication", "Failed to initialize ChangelogManager", e)
            // Guardar el error para diagnóstico
            try {
                val crashFile = File(filesDir, "last_crash_log.txt")
                crashFile.appendText("Failed to initialize ChangelogManager: ${e.stackTraceToString()}\n")
            } catch (_: Exception) { /* ignore */ }
        }
    }
}
