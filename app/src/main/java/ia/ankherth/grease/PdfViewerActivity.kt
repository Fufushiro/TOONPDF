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

class PdfViewerActivity : AppCompatActivity(), OnLoadCompleteListener, OnPageChangeListener, OnTapListener {

    private lateinit var binding: ActivityPdfViewerBinding
    private val viewModel: MainViewModel by viewModels()

    private var pdfUri: Uri? = null
    private var fileName: String = ""
    private var totalPages: Int = 0
    private var currentPage: Int = 0
    private var isFullscreen = false
    private var isControlsVisible = true

    companion object {
        const val EXTRA_PDF_URI = "pdf_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_CURRENT_PAGE = "current_page"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        pdfUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PDF_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PDF_URI)
        }
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Documento PDF"
        currentPage = intent.getIntExtra(EXTRA_CURRENT_PAGE, 0)

        setupUI()
        setupPdfViewer()
        setupClickListeners()
        setupBackPressHandler()
    }

    private fun setupUI() {
        binding.toolbar.title = fileName

        // Enable immersive mode preparation
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
                    finish()
                }
            }
        })
    }

    override fun loadComplete(nbPages: Int) {
        totalPages = nbPages
        binding.textTotalPages.text = totalPages.toString()
        updateProgress()

        // Save or update PDF in database
        pdfUri?.let { uri: Uri ->
            viewModel.addOrUpdatePdf(
                uri = uri.toString(),
                fileName = fileName,
                totalPages = totalPages,
                currentPage = currentPage
            )
        }
    }

    override fun onPageChanged(page: Int, pageCount: Int) {
        currentPage = page
        updateProgress()

        // Update progress in database
        pdfUri?.let { uri: Uri ->
            viewModel.updateProgress(uri.toString(), currentPage)
        }
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

        // Update navigation buttons
        binding.buttonPreviousPage.isEnabled = currentPage > 0
        binding.buttonNextPage.isEnabled = currentPage < totalPages - 1
    }

    private fun toggleControlsVisibility() {
        if (isFullscreen) return // Don't show controls in fullscreen unless specifically requested

        isControlsVisible = !isControlsVisible
        val visibility = if (isControlsVisible) View.VISIBLE else View.GONE

        binding.toolbar.visibility = visibility
        binding.bottomControls.visibility = visibility
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)

        if (isFullscreen) {
            // Enter fullscreen mode
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            binding.toolbar.visibility = View.GONE
            binding.bottomControls.visibility = View.GONE
            isControlsVisible = false

            // Keep screen on during reading
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            // Exit fullscreen mode
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

            binding.toolbar.visibility = View.VISIBLE
            binding.bottomControls.visibility = View.VISIBLE
            isControlsVisible = true

            // Allow screen to turn off normally
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to save final progress
        pdfUri?.let { uri: Uri ->
            viewModel.updateProgress(uri.toString(), currentPage)
        }
    }
}
