package ia.ankherth.grease

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import ia.ankherth.grease.databinding.ActivityPdfViewerBinding
import ia.ankherth.grease.util.ThemeUtils
import ia.ankherth.grease.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class PdfViewerActivity : AppCompatActivity(), OnLoadCompleteListener, OnPageChangeListener, OnTapListener {

    private lateinit var binding: ActivityPdfViewerBinding
    private val viewModel: MainViewModel by viewModels()

    private var pdfUri: Uri? = null
    private var fileName: String = ""
    private var totalPages: Int = 0
    private var currentPage: Int = 0
    private var currentScrollOffset: Float = 0f
    private var filePath: String? = null

    private var isUiVisible = true
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideControls() }

    companion object {
        const val EXTRA_PDF_URI = "pdf_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_CURRENT_PAGE = "current_page"
        const val EXTRA_SCROLL_OFFSET = "scroll_offset"
        private const val AUTO_HIDE_DELAY_MILLIS = 2500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this, fullscreen = true)
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bloqueo de rotación a vertical por defecto
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PDF_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PDF_URI)
        }
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Documento"
        currentPage = intent.getIntExtra(EXTRA_CURRENT_PAGE, 0)

        extractFilePath()
        setupUI()
        setupPdfViewer()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        delayedHide(AUTO_HIDE_DELAY_MILLIS)
    }

    override fun onPause() {
        super.onPause()
        saveProgress()
        hideHandler.removeCallbacks(hideRunnable)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.toolbar.title = fileName.substringBeforeLast('.')

        // Escuchar toques en la vista del PDF para reiniciar el temporizador de ocultación
        binding.pdfView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            false // No consumir el evento
        }

        // Actualizar información de progreso inicial
        updateProgressInfo()
    }

    private fun setupPdfViewer() {
        pdfUri?.let { uri ->
            binding.pdfView.fromUri(uri)
                .defaultPage(currentPage)
                .onLoad(this)
                .onPageChange(this)
                .onTap(this)
                .scrollHandle(DefaultScrollHandle(this)) // Añade un scroll handle
                .spacing(10) // Espacio entre páginas
                .enableSwipe(true) // Habilitar swipe para navegar
                .swipeHorizontal(false) // Scroll vertical
                .enableDoubletap(true) // Habilitar zoom con doble tap
                .enableAntialiasing(true) // Mejor calidad de renderizado
                .pageFling(true) // Animación suave al cambiar página
                .pageSnap(true) // Ajustar a página completa
                .autoSpacing(true) // Espaciado automático entre páginas
                .nightMode(false) // Modo normal
                .enableAnnotationRendering(true) // Renderizar anotaciones
                .password(null) // Sin contraseña
                .scrollHandle(DefaultScrollHandle(this))
                .load()
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        // El resto de los listeners de botones de página se eliminan
    }

    override fun loadComplete(nbPages: Int) {
        totalPages = nbPages
        updateProgressInfo()
        pdfUri?.let { uri ->
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
        updateProgressInfo()
        // El progreso se guarda al salir
    }

    override fun onTap(e: MotionEvent?): Boolean {
        toggleControls()
        return true
    }

    private fun toggleControls() {
        if (isUiVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun hideControls() {
        isUiVisible = false
        binding.toolbar.animate().alpha(0f).setDuration(300).withEndAction {
            binding.toolbar.visibility = View.GONE
        }
        binding.bottomControls.animate().alpha(0f).setDuration(300).withEndAction {
            binding.bottomControls.visibility = View.GONE
        }

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun showControls() {
        isUiVisible = true
        binding.toolbar.visibility = View.VISIBLE
        binding.toolbar.animate().alpha(1f).setDuration(300)
        binding.bottomControls.visibility = View.VISIBLE
        binding.bottomControls.animate().alpha(1f).setDuration(300)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        delayedHide(AUTO_HIDE_DELAY_MILLIS)
    }

    private fun delayedHide(delayMillis: Long) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis)
    }

    private fun saveProgress() {
        pdfUri?.let { uri ->
            lifecycleScope.launch {
                viewModel.updateProgress(uri.toString(), currentPage)
            }
        }
    }

    private fun extractFilePath() {
        try {
            pdfUri?.let { uri ->
                if (uri.scheme == "file") {
                    filePath = uri.path
                }
                // La extracción para 'content' URI se mantiene igual
            }
        } catch (e: Exception) {
            filePath = null
        }
    }

    private fun updateProgressInfo() {
        if (totalPages > 0) {
            val displayPage = currentPage + 1 // Las páginas empiezan en 0, mostrar desde 1
            binding.textCurrentPage.text = displayPage.toString()
            binding.textTotalPages.text = totalPages.toString()

            val progress = ((displayPage.toFloat() / totalPages.toFloat()) * 100).toInt()
            binding.textProgress.text = "$progress%"
            binding.progressBar.progress = progress
        } else {
            binding.textCurrentPage.text = "0"
            binding.textTotalPages.text = "0"
            binding.textProgress.text = "0%"
            binding.progressBar.progress = 0
        }
    }
}
