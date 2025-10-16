package ia.ankherth.grease

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import ia.ankherth.grease.databinding.ActivityPdfViewerBinding
import ia.ankherth.grease.viewmodel.MainViewModel
import androidx.lifecycle.lifecycleScope
import ia.ankherth.grease.util.ThemeUtils
import kotlinx.coroutines.launch

/**
 * Actividad para visualizar PDFs con persistencia mejorada
 * Guarda automáticamente el progreso de lectura y utiliza Room para almacenamiento persistente
 */
class PdfViewerActivity : AppCompatActivity(), OnLoadCompleteListener, OnPageChangeListener, OnTapListener {

    private lateinit var binding: ActivityPdfViewerBinding
    private val viewModel: MainViewModel by viewModels()

    private var pdfUri: Uri? = null
    private var fileName: String = ""
    private var totalPages: Int = 0
    private var currentPage: Int = 0
    private var isFullscreen = false
    private var isControlsVisible = true
    private var filePath: String? = null

    companion object {
        const val EXTRA_PDF_URI = "pdf_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_CURRENT_PAGE = "current_page"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this, fullscreen = true)
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener datos del intent
        pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PDF_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PDF_URI)
        }
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Documento PDF"
        currentPage = intent.getIntExtra(EXTRA_CURRENT_PAGE, 0)

        // Intentar extraer la ruta del archivo para mayor robustez
        extractFilePath()

        setupUI()
        setupPdfViewer()
        setupClickListeners()
        setupBackPressHandler()
    }

    override fun onPause() {
        super.onPause()
        // Guardar progreso al pausar/salir de la actividad
        saveProgress()
    }

    private fun setupUI() {
        binding.toolbar.title = fileName

        // Preparar modo inmersivo
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun setupPdfViewer() {
        pdfUri?.let { uri: Uri ->
            binding.pdfView.fromUri(uri)
                .defaultPage(currentPage)
                .onLoad(this)
                .onPageChange(this)
                .onTap(this)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .enableAntialiasing(true)
                .spacing(10)
                .load()
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.buttonPreviousPage.setOnClickListener {
            if (currentPage > 0) {
                binding.pdfView.jumpTo(currentPage - 1)
            }
        }

        binding.buttonNextPage.setOnClickListener {
            if (currentPage < totalPages - 1) {
                binding.pdfView.jumpTo(currentPage + 1)
            }
        }

        binding.buttonFullscreen.setOnClickListener {
            toggleFullscreen()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    toggleFullscreen()
                } else {
                    saveProgress()
                    finish()
                }
            }
        })
    }

    override fun loadComplete(nbPages: Int) {
        totalPages = nbPages
        binding.textTotalPages.text = totalPages.toString()
        updateProgress()

        // Guardar o actualizar PDF en la base de datos
        pdfUri?.let { uri: Uri ->
            lifecycleScope.launch {
                viewModel.addOrUpdatePdf(
                    uri = uri.toString(),
                    fileName = fileName,
                    totalPages = totalPages,
                    currentPage = currentPage,
                    filePath = filePath
                )
            }
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        currentPage = page
        updateProgress()

        // No actualizamos la base de datos en cada cambio de página para evitar operaciones excesivas
        // El progreso se guardará al pausar/salir de la actividad
    }

    override fun onTap(e: android.view.MotionEvent?): Boolean {
        toggleControlsVisibility()
        return true
    }

    private fun updateProgress() {
        binding.textCurrentPage.text = (currentPage + 1).toString()
        binding.textTotalPages.text = totalPages.toString()

        val progress = if (totalPages > 0) {
            ((currentPage + 1).toFloat() / totalPages.toFloat() * 100).toInt()
        } else 0

        binding.progressBarReading.progress = progress
        binding.textProgressPercentage.text = "${progress}%"

        // Actualizar botones de navegación
        binding.buttonPreviousPage.isEnabled = currentPage > 0
        binding.buttonNextPage.isEnabled = currentPage < totalPages - 1
    }

    private fun toggleControlsVisibility() {
        if (isFullscreen) return // No mostrar controles en pantalla completa a menos que se solicite específicamente

        isControlsVisible = !isControlsVisible
        val visibility = if (isControlsVisible) View.VISIBLE else View.GONE

        binding.toolbar.visibility = visibility
        binding.bottomControls.visibility = visibility
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        if (isFullscreen) {
            // Entrar en modo pantalla completa
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            binding.toolbar.visibility = View.GONE
            binding.bottomControls.visibility = View.GONE
            isControlsVisible = false

            // Mantener la pantalla encendida durante la lectura
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            // Salir de modo pantalla completa
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

            binding.toolbar.visibility = View.VISIBLE
            binding.bottomControls.visibility = View.VISIBLE
            isControlsVisible = true

            // Permitir que la pantalla se apague normalmente
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun saveProgress() {
        pdfUri?.let { uri ->
            lifecycleScope.launch {
                viewModel.updateProgress(uri.toString(), currentPage)
                // También actualizamos la preferencia del último PDF abierto
                viewModel.addOrUpdatePdf(
                    uri = uri.toString(),
                    fileName = fileName,
                    totalPages = totalPages,
                    currentPage = currentPage,
                    filePath = filePath
                )
            }
        }
    }

    /**
     * Intenta extraer la ruta de archivo real para facilitar la reubicación si es necesario
     */
    private fun extractFilePath() {
        try {
            pdfUri?.let { uri ->
                if (uri.scheme == "file") {
                    filePath = uri.path
                } else if (uri.scheme == "content") {
                    contentResolver.openInputStream(uri)?.use { _ ->
                        val cursor = contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (displayNameIndex >= 0) {
                                    val displayName = it.getString(displayNameIndex)
                                    val docId = uri.toString()
                                    filePath = docId
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            filePath = null
        }
    }
}
