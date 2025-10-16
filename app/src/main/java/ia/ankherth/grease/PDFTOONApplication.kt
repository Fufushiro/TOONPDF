package ia.ankherth.grease

import android.app.Application
import ia.ankherth.grease.util.ChangelogManager

/**
 * Clase de aplicación personalizada que inicializa los componentes necesarios
 * al iniciar la aplicación, como el gestor de registro de cambios
 */
class PDFTOONApplication : Application() {

    private lateinit var changelogManager: ChangelogManager

    override fun onCreate() {
        super.onCreate()

        // Inicializar el gestor de registro de cambios
        changelogManager = ChangelogManager(this)
        changelogManager.initializeChangelog()
    }
}
