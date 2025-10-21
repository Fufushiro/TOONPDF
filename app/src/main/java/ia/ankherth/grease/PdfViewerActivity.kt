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
import ia.ankherth.grease.databinding.ActivityPdfViewerBinding
import ia.ankherth.grease.util.CustomScrollHandle
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
        // Force light mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
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

        // Actualizar información de progreso inicial
        updateProgressInfo()
    }

    private fun setupPdfViewer() {
        pdfUri?.let { uri ->
            try {
                // Tomar permisos persistentes para el URI
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Si no se pueden tomar los permisos, intentar cargar de todas formas
                android.util.Log.w("PdfViewerActivity", "No se pudieron tomar permisos persistentes: ${e.message}")
            }

            // Configuración minimalista para máxima compatibilidad
            // Sin espaciado entre páginas para scroll continuo
            binding.pdfView.fromUri(uri)
                .defaultPage(currentPage)
                .onLoad(this)
                .onPageChange(this)
                .onTap(this)
                .scrollHandle(CustomScrollHandle(this)) // Usar CustomScrollHandle
                .spacing(0) // Sin espacios entre páginas
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .pageFling(true)
                .pageSnap(false) // Desactivar snap para scroll continuo
                .autoSpacing(false) // Desactivar espaciado automático
                .load()
        } ?: run {
            // Si no hay URI, mostrar error y cerrar
            android.widget.Toast.makeText(this, "Error: No se pudo cargar el PDF", android.widget.Toast.LENGTH_LONG).show()
            finish()
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

            // Animación elegante del color de la barra según el progreso
            animateProgressBarColor(progress)
        } else {
            binding.textCurrentPage.text = "0"
            binding.textTotalPages.text = "0"
            binding.textProgress.text = "0%"
            binding.progressBar.progress = 0
        }
    }

    private fun animateProgressBarColor(progress: Int) {
        val newDrawable = when {
            progress >= 90 -> R.drawable.progress_drawable_gradient_red
            progress >= 80 -> R.drawable.progress_drawable_gradient_orange
            else -> R.drawable.progress_drawable_rounded
        }

        // Solo cambiar si es diferente al actual
        if (binding.progressBar.tag != newDrawable) {
            val oldDrawable = binding.progressBar.progressDrawable
            val newProgressDrawable = androidx.core.content.ContextCompat.getDrawable(this, newDrawable)

            if (oldDrawable != null && newProgressDrawable != null) {
                // Crear transición suave entre drawables
                val transition = android.graphics.drawable.TransitionDrawable(
                    arrayOf(oldDrawable, newProgressDrawable)
                )
                binding.progressBar.progressDrawable = transition
                transition.startTransition(500) // 500ms de transición elegante

                // Actualizar el tag para rastrear el drawable actual
                binding.progressBar.tag = newDrawable
            } else {
                binding.progressBar.setProgressDrawable(newProgressDrawable)
                binding.progressBar.tag = newDrawable
            }

            // Animar también el texto del porcentaje con color
            val textColor = when {
                progress >= 90 -> android.graphics.Color.parseColor("#CC0000") // Rojo intenso
                progress >= 80 -> android.graphics.Color.parseColor("#FF6B35") // Naranja/rojo
                else -> androidx.core.content.ContextCompat.getColor(this, R.color.md_theme_light_primary)
            }

            // Animar el cambio de color del texto
            val currentColor = binding.textProgress.currentTextColor
            val colorAnimator = android.animation.ValueAnimator.ofObject(
                android.animation.ArgbEvaluator(),
                currentColor,
                textColor
            )
            colorAnimator.duration = 500
            colorAnimator.addUpdateListener { animator ->
                binding.textProgress.setTextColor(animator.animatedValue as Int)
            }
            colorAnimator.start()
        }
    }
}
