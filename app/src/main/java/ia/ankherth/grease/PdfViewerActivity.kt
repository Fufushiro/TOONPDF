package ia.ankherth.grease
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private var progressAnimator: ValueAnimator? = null
    private var shakeAnimator: ObjectAnimator? = null
    companion object {
        const val EXTRA_PDF_URI = "pdf_uri"
        const val EXTRA_FILE_NAME = "file_name"
        const val EXTRA_CURRENT_PAGE = "current_page"
        const val EXTRA_SCROLL_OFFSET = "scroll_offset"
        private const val AUTO_HIDE_DELAY_MILLIS = 2500L
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        ThemeUtils.applyTheme(this, fullscreen = true)
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        progressAnimator?.cancel()
        progressAnimator = null
        stopShake()
    }
    override fun onDestroy() {
        progressAnimator?.cancel()
        progressAnimator = null
        shakeAnimator?.cancel()
        shakeAnimator = null
        super.onDestroy()
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.toolbar.title = fileName.substringBeforeLast('.')
        updateProgressInfo()
    }
    private fun setupPdfViewer() {
        pdfUri?.let { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                android.util.Log.w("PdfViewerActivity", "No se pudieron tomar permisos persistentes: ${e.message}")
            }
            binding.pdfView.fromUri(uri)
                .defaultPage(currentPage)
                .onLoad(this)
                .onPageChange(this)
                .onTap(this)
                .scrollHandle(CustomScrollHandle(this))
                .spacing(0)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .pageFling(true)
                .pageSnap(false)
                .autoSpacing(false)
                .load()
        } ?: run {
            android.widget.Toast.makeText(this, "Error: No se pudo cargar el PDF", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }
    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
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
            }
        } catch (e: Exception) {
            filePath = null
        }
    }
    private fun updateProgressInfo() {
        if (totalPages > 0) {
            val displayPage = currentPage + 1
            binding.textCurrentPage.text = displayPage.toString()
            binding.textTotalPages.text = totalPages.toString()
            val progress = ((displayPage.toFloat() / totalPages.toFloat()) * 100).toInt()
            binding.textProgress.text = "$progress%"
            updateProgressBarWithAnimation(progress)
        } else {
            binding.textCurrentPage.text = "0"
            binding.textTotalPages.text = "0"
            binding.textProgress.text = "0%"
            binding.progressBar.progress = 0
        }
    }
    private fun updateProgressBarWithAnimation(progress: Int) {
        progressAnimator?.cancel()
        progressAnimator = null
        val startProgress = binding.progressBar.progress
        if (startProgress == progress) {
            if (progress >= 80) ensureShakeForProgress(progress) else stopShake()
            return
        }
        fun applyColorForValue(value: Int) {
            val color = when {
                value < 85 -> ContextCompat.getColor(this@PdfViewerActivity, R.color.md_theme_light_primary)
                value >= 100 -> Color.parseColor("#D32F2F")
                else -> {
                    val greenColor = ContextCompat.getColor(this@PdfViewerActivity, R.color.md_theme_light_primary)
                    val redColor = Color.parseColor("#D32F2F")
                    val factor = (value - 85) / 15f
                    @Suppress("DEPRECATION")
                    ArgbEvaluator().evaluate(factor, greenColor, redColor) as Int
                }
            }
            val progressDrawable = binding.progressBar.progressDrawable as? LayerDrawable
            progressDrawable?.let { layerDrawable ->
                val progressLayer = layerDrawable.findDrawableByLayerId(android.R.id.progress)
                if (progressLayer is ClipDrawable) {
                    progressLayer.drawable?.setTint(color)
                } else {
                    progressLayer?.setTint(color)
                }
            }
            binding.textProgress.setTextColor(color)
        }
        if (startProgress < 80 && progress >= 80) {
            val diff1 = 80 - startProgress
            val duration1 = (diff1 * 25).coerceAtLeast(220).toLong()
            val anim1 = ValueAnimator.ofInt(startProgress, 80).apply {
                duration = duration1
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener { a ->
                    val cur = a.animatedValue as Int
                    binding.progressBar.progress = cur
                    applyColorForValue(cur)
                }
            }
            val diff2 = progress - 80
            val duration2 = ((diff2 * 12).coerceAtLeast(160)).toLong()
            val anim2 = ValueAnimator.ofInt(80, progress).apply {
                duration = duration2
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { a ->
                    val cur = a.animatedValue as Int
                    binding.progressBar.progress = cur
                    applyColorForValue(cur)
                    ensureShakeForProgress(cur)
                }
            }
            anim1.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    progressAnimator = anim2
                    anim2.start()
                }
                override fun onAnimationCancel(animation: Animator) {
                    progressAnimator = null
                }
            })
            anim2.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    progressAnimator = null
                    if (progress < 80) stopShake()
                }
                override fun onAnimationCancel(animation: Animator) {
                    progressAnimator = null
                }
            })
            progressAnimator = anim1
            anim1.start()
            return
        }
        val baseDiff = kotlin.math.abs(progress - startProgress)
        val baseDuration = when {
            startProgress >= 80 -> (baseDiff * 12).coerceAtLeast(160)
            else -> (baseDiff * 20).coerceAtLeast(220)
        }
        progressAnimator = ValueAnimator.ofInt(startProgress, progress).apply {
            duration = baseDuration.toLong()
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { a ->
                val cur = a.animatedValue as Int
                binding.progressBar.progress = cur
                applyColorForValue(cur)
                if (cur >= 80) ensureShakeForProgress(cur) else stopShake()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    progressAnimator = null
                }
                override fun onAnimationCancel(animation: Animator) {
                    progressAnimator = null
                }
            })
            start()
        }
    }
    private fun ensureShakeForProgress(current: Int) {
        val clamped = current.coerceIn(80, 100)
        val mappedDuration = ((100 - clamped) * 36 + 180).toLong().coerceAtLeast(120L)
        if (shakeAnimator == null) {
            shakeAnimator = ObjectAnimator.ofFloat(binding.progressBar, "translationX", -6f, 6f).apply {
                duration = mappedDuration
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
        } else {
            if (shakeAnimator?.duration != mappedDuration) {
                shakeAnimator?.duration = mappedDuration
            }
            if (!shakeAnimator!!.isStarted) shakeAnimator!!.start()
        }
    }
    private fun stopShake() {
        shakeAnimator?.cancel()
        shakeAnimator = null
        binding.progressBar.translationX = 0f
    }
}
