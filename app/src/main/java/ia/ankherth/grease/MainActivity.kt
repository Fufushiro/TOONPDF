package ia.ankherth.grease

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import android.view.HapticFeedbackConstants
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import ia.ankherth.grease.adapter.PdfHistoryAdapter
import kotlinx.coroutines.Dispatchers
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.util.ThemeUtils
import ia.ankherth.grease.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

/**
 * Actividad principal con sistema de navegación por tabs:
 * - HOME: Muestra los 2 últimos PDFs y funcionalidades principales
 * - HISTORIAL: Muestra todo el historial de PDFs
 * - PDF (centro): Abre el último PDF en la última página
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Views principales
    private lateinit var toolbar: Toolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAddPdf: FloatingActionButton
    private lateinit var errorContainer: MaterialCardView
    private lateinit var textErrorMessage: TextView
    private lateinit var buttonDismissError: Button
    private lateinit var rootView: androidx.coordinatorlayout.widget.CoordinatorLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Vistas de las tarjetas en la pantalla principal
    private lateinit var mainContentScroll: ScrollView
    private lateinit var tvWelcomeMessage: TextView
    private lateinit var cardLastPdf: MaterialCardView
    private lateinit var cardLastPdfContent: View
    private lateinit var tvPdfTitle: TextView
    private lateinit var tvPdfMeta: TextView
    private lateinit var tvLastRead: TextView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var emptyStateContainer: View
    private lateinit var recyclerViewRecentPdfs: androidx.recyclerview.widget.RecyclerView
    private lateinit var ivPdfPreview: android.widget.ImageView
    private lateinit var ivUserAvatar: android.widget.ImageView
    private lateinit var cardAvatar: MaterialCardView

    // Animator references (to avoid duplicates and allow dynamic updates)
    private var progressAnimator: ValueAnimator? = null
    private var shakeAnimator: ObjectAnimator? = null

    // Adapter
    private lateinit var recentPdfsAdapter: PdfHistoryAdapter

    // PDF Card Colorizer para aplicar colores dinámicos
    private lateinit var pdfCardColorizer: ia.ankherth.grease.util.PdfCardColorizer

    // SAF registrars
    private val openPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleNewPdfSelection(it) }
    }

    @Suppress("unused")
    private val relocatePdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handlePdfRelocation(it, pendingRelocationPdf) }
        pendingRelocationPdf = null
    }

    // Almacenar temporalmente el PDF que está pendiente de reubicación
    private var pendingRelocationPdf: PdfHistoryEntity? = null

    // Launcher para seleccionar avatar
    private val pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleAvatarSelection(it) }
    }

    // Launcher para acceso a carpeta (Storage Access Framework)
    private val openStorageTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.setStorageTreeUri(uri.toString())
                showSuccessMessage()
            } catch (_: SecurityException) {
                showErrorMessage("No se pudo obtener acceso a la carpeta")
            }
        }
    }

    // Preferencias
    private var hapticsEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before inflating UI (persisted)
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        // Inicializar referencias a las vistas
        initViews()
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // Configure proper edge-to-edge insets handling
        setupEdgeToEdgeInsets()

        // Update status bar icon color based on current mode
        updateStatusBarIconColor(isDarkMode(this))

        setupBottomNavigation()
        setupClickListeners()
        observeData()
        setupErrorHandling()

        // Manejar intent si la app fue abierta con un PDF
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW) {
                handleNewPdfSelection(uri)
            }
        }
    }

    private fun initViews() {
        rootView = findViewById(R.id.rootView)
        toolbar = findViewById(R.id.toolbar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabAddPdf = findViewById(R.id.fabAddPdf)
        textErrorMessage = findViewById(R.id.textErrorMessage)
        buttonDismissError = findViewById(R.id.buttonDismissError)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Inicializar vistas de las tarjetas en la pantalla principal
        mainContentScroll = findViewById(R.id.mainContentScroll)
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage)
        cardLastPdf = findViewById(R.id.cardLastPdf)
        cardLastPdfContent = findViewById(R.id.cardLastPdfContent)
        tvPdfTitle = findViewById(R.id.tvPdfTitle)
        tvPdfMeta = findViewById(R.id.tvPdfMeta)
        tvLastRead = findViewById(R.id.tvLastRead)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        recyclerViewRecentPdfs = findViewById(R.id.recyclerViewRecentPdfs)
        ivPdfPreview = findViewById(R.id.ivPdfPreview)
        // Inicializar el colorizer para aplicar colores dinámicos del PDF
        pdfCardColorizer = ia.ankherth.grease.util.PdfCardColorizer(this, lifecycleScope)

        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        cardAvatar = findViewById(R.id.cardAvatar)

        // Configurar RecyclerView de PDFs recientes (solo 3)
        setupRecentPdfsRecyclerView()

        // Configurar SwipeRefreshLayout
        setupSwipeRefresh()
    }

    @Suppress("DEPRECATION")
    private fun enableEdgeToEdge() {
        // Enable edge-to-edge for API 21+ using WindowCompat (modern approach)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set transparent system bars (deprecated but necessary for API 29-34 compatibility)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply top inset to the root view to avoid overlap with status bar
            view.setPadding(
                view.paddingLeft,
                insets.top,
                view.paddingRight,
                view.paddingBottom
            )

            windowInsets
        }
    }

    private fun performHaptic() {
        if (hapticsEnabled) {
            rootView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    performHaptic()
                    // Mostrar tarjetas principales
                    mainContentScroll.visibility = View.VISIBLE
                    updateToolbarTitle("HOME")
                    true
                }
                R.id.navigation_pdf_center -> {
                    performHaptic()
                    // Acción directa: abrir último PDF o mostrar prompt
                    openLastPdfOrPrompt()
                    // Volver a marcar Home para mantener navegación por pestañas
                    bottomNavigation.selectedItemId = R.id.navigation_home
                    true
                }
                else -> false
            }
        }

        // Set initial state - mostrar tarjetas principales
        mainContentScroll.visibility = View.VISIBLE
        bottomNavigation.selectedItemId = R.id.navigation_home
        updateToolbarTitle("HOME")
    }

    private fun updateToolbarTitle(title: String) {
        // Fade out, update, then fade in
        toolbar.animate().cancel()
        toolbar.alpha = 1f
        toolbar.animate().alpha(0f).setDuration(80).withEndAction {
            toolbar.title = title
            toolbar.animate().alpha(1f).setDuration(120).start()
        }.start()
    }

    private fun setupClickListeners() {
        fabAddPdf.setOnClickListener {
            performHaptic()
            openPdfPicker()
        }

        // Añadir listener al avatar para cambiar foto
        cardAvatar.setOnClickListener {
            performHaptic()
            pickAvatarLauncher.launch(arrayOf("image/*"))
        }
    }

    private fun setupRecentPdfsRecyclerView() {
        // Configurar adapter para PDFs recientes
        recentPdfsAdapter = PdfHistoryAdapter(
            onPdfClick = { pdf ->
                openPdfViewer(pdf)
            },
            onDeleteClick = { pdf ->
                // Mostrar diálogo de confirmación para eliminar
                showDeletePdfDialog(pdf)
            },
            onRelocateClick = { pdf ->
                // Reubicar PDF
                pendingRelocationPdf = pdf
                relocatePdf.launch(arrayOf("application/pdf"))
            },
            onLongPressDelete = { pdf ->
                // Mostrar diálogo de confirmación para eliminar al mantener presionado
                performHaptic()
                showDeletePdfDialog(pdf)

            }
        )

        recyclerViewRecentPdfs.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
            // Asegurar que el LayoutManager permita auto-measure para que
            // el RecyclerView con wrap_content dentro de un ScrollView mida su altura.
            (layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.isAutoMeasureEnabled = true
            adapter = recentPdfsAdapter
            // No usar setHasFixedSize(true) cuando el RecyclerView está dentro de un ScrollView
            // porque impide que el RecyclerView mida su altura correctamente; usar false para
            // permitir auto-measure y que expanda su contenido.
            setHasFixedSize(false)
            // Desactivar nested scrolling porque el RecyclerView está dentro de un ScrollView
            isNestedScrollingEnabled = false
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            // Recargar los datos cuando el usuario hace pull-to-refresh
            lifecycleScope.launch {
                // Simular una pequeña espera para mejor UX
                kotlinx.coroutines.delay(300)
                // Forzar recarga de los PDFs del ViewModel
                viewModel.refreshPdfs()
                // Detener la animación de refresh
                swipeRefreshLayout.isRefreshing = false
                // Mostrar feedback al usuario en la parte superior
                Snackbar.make(toolbar, "Lista actualizada", Snackbar.LENGTH_SHORT)
                    .setAnchorView(toolbar)
                    .show()
            }
        }

        // Personalizar colores del SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeResources(
            R.color.md_theme_light_primary,
            R.color.md_theme_light_secondary,
            R.color.md_theme_light_tertiary
        )
    }

    private fun observeData() {
        viewModel.allPdfs.observe(this) { pdfs ->
            if (pdfs.isNotEmpty()) {
                mainContentScroll.visibility = View.VISIBLE
                emptyStateContainer.visibility = View.GONE

                // Tarjeta principal: último PDF leído
                val mostRecent = pdfs.first()
                tvPdfTitle.text = mostRecent.fileName

                // Evitar división por cero y valores inválidos al calcular porcentaje
                val totalPages = mostRecent.totalPages.coerceAtLeast(0)
                val lastReadPage = mostRecent.lastPageRead.coerceAtLeast(0)
                val progressPercentage = if (totalPages > 0) {
                    ((lastReadPage.toFloat() / totalPages) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                // Actualizar el progreso con animación y cambio de color
                updateProgressBarWithAnimation(progressPercentage)

                tvProgress.text = if (totalPages > 0) {
                    getString(R.string.pdf_progress_percentage, progressPercentage)
                } else {
                    // Mostrar 0% si no se conoce el total de páginas
                    getString(R.string.pdf_progress_percentage, 0)
                }

                tvPdfMeta.text = if (totalPages > 0) {
                    getString(R.string.pdf_page_meta, lastReadPage + 1, totalPages)
                } else {
                    // Si no hay total conocido, sólo mostrar la página actual (1-indexed)
                    getString(R.string.pdf_page_meta_unknown, lastReadPage + 1)
                }

                tvLastRead.text = getString(R.string.pdf_last_read, getRelativeTime(mostRecent.lastReadDate))

                // Aplicar colores dinámicos extraídos del PDF
                applyPdfDynamicColors(mostRecent)

                mostRecent.thumbnailPath?.let { path ->
                    ivPdfPreview.load(File(path)) {
                        crossfade(true)
                        placeholder(R.drawable.pdf_thumbnail_placeholder)
                        error(R.drawable.pdf_thumbnail_placeholder)
                    }
                } ?: ivPdfPreview.setImageResource(R.drawable.pdf_thumbnail_placeholder)

                cardLastPdf.setOnClickListener { openPdfViewer(mostRecent) }

                // Agregar long-press para eliminar del historial
                cardLastPdf.setOnLongClickListener {
                    performHaptic()
                    showDeletePdfDialog(mostRecent)
                    true
                }

                // Lista secundaria: siguientes 6 PDFs (para un total de 7 incluyendo el principal)
                recentPdfsAdapter.submitList(pdfs.drop(1).take(6))
                recyclerViewRecentPdfs.isVisible = pdfs.size > 1

            } else {
                // Estado vacío
                mainContentScroll.visibility = View.GONE
                emptyStateContainer.visibility = View.VISIBLE
            }
        }

        viewModel.userName.observe(this) { name ->
            tvWelcomeMessage.text = if (!name.isNullOrBlank()) {
                getString(R.string.welcome_user, name)
            } else {
                getString(R.string.welcome_back)
            }
        }

        viewModel.errorEvent.observe(this) { error ->
            error?.let {
                showErrorMessage(it.message)
                viewModel.clearErrorEvent()
            }
        }

        viewModel.userAvatarUri.observe(this) { uri ->
            // Cargar URI del avatar del usuario
            if (!uri.isNullOrBlank()) {
                ivUserAvatar.load(Uri.parse(uri)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_default_avatar)
                    error(R.drawable.ic_default_avatar)
                }
            } else {
                ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
            }
        }
    }

    private fun setupErrorHandling() {
        buttonDismissError.setOnClickListener {
            errorContainer.visibility = View.GONE
        }
    }

    private fun showErrorMessage(message: String) {
        textErrorMessage.text = message
        errorContainer.visibility = View.VISIBLE
    }

    private fun hideErrorMessage() {
        errorContainer.visibility = View.GONE
    }

    private fun showSuccessMessage() {
        Snackbar.make(rootView, "Acceso a carpeta concedido correctamente", Snackbar.LENGTH_SHORT).show()
    }

    private fun openPdfPicker() {
        openPdf.launch(arrayOf("application/pdf"))
    }

    private fun handleNewPdfSelection(uri: Uri) {
        lifecycleScope.launch {
            viewModel.addPdfToHistory(uri)
            // La UI se actualizará a través del observer
        }
    }

    private fun handlePdfRelocation(uri: Uri, pdfToRelocate: PdfHistoryEntity?) {
        if (pdfToRelocate == null) return
        lifecycleScope.launch {
            viewModel.updatePdfUri(pdfToRelocate.uri, uri.toString())
        }
    }

    private fun handleAvatarSelection(uri: Uri) {
        lifecycleScope.launch {
            viewModel.updateUserAvatar(uri.toString())
        }
    }

    private fun openPdfViewer(pdf: PdfHistoryEntity) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_PDF_URI, pdf.uri.toUri())
            putExtra(PdfViewerActivity.EXTRA_FILE_NAME, pdf.fileName)
            putExtra(PdfViewerActivity.EXTRA_CURRENT_PAGE, pdf.lastPageRead)
            putExtra(PdfViewerActivity.EXTRA_SCROLL_OFFSET, pdf.scrollOffset)
        }
        startActivity(intent)
    }

    /**
     * Aplica colores dinámicos extraídos del PDF a la tarjeta de forma elegante.
     * Esta función es lifecycle-aware y se ejecuta en un hilo secundario.
     */
    private fun applyPdfDynamicColors(pdf: PdfHistoryEntity) {
        android.util.Log.d("MainActivity", "=== applyPdfDynamicColors START ===")
        android.util.Log.d("MainActivity", "PDF fileName: ${pdf.fileName}")
        android.util.Log.d("MainActivity", "PDF filePath: ${pdf.filePath}")
        android.util.Log.d("MainActivity", "PDF uri: ${pdf.uri}")

        lifecycleScope.launch(Dispatchers.Main) {
            android.util.Log.d("MainActivity", "Inside coroutine scope")
            try {
                // Intentar obtener el archivo desde el filePath
                val pdfFile = if (!pdf.filePath.isNullOrBlank()) {
                    android.util.Log.d("MainActivity", "Using filePath: ${pdf.filePath}")
                    File(pdf.filePath)
                } else {
                    // Fallback: intentar obtener desde URI usando ContentResolver
                    android.util.Log.d("MainActivity", "filePath is null/blank, using URI: ${pdf.uri}")
                    getPdfFileFromUri(pdf.uri)
                }

                android.util.Log.d("MainActivity", "pdfFile: $pdfFile")
                android.util.Log.d("MainActivity", "pdfFile exists: ${pdfFile?.exists()}")
                android.util.Log.d("MainActivity", "pdfFile canRead: ${pdfFile?.canRead()}")

                if (pdfFile != null && pdfFile.exists() && pdfFile.canRead()) {
                    android.util.Log.d("MainActivity", "✓ PDF file is accessible: ${pdfFile.absolutePath}")
                    // Aplicar colores dinámicos usando el colorizer
                    pdfCardColorizer.applyPdfColorToCard(
                        pdfFile = pdfFile,
                        cardContentContainer = cardLastPdfContent,
                        titleTextView = tvPdfTitle,
                        metaTextView = tvPdfMeta,
                        progressTextView = tvProgress,
                        lastReadTextView = tvLastRead,
                        progressBar = progressBar
                    )
                } else {
                    android.util.Log.e("MainActivity", "✗ PDF file NOT accessible!")
                    android.util.Log.e("MainActivity", "  - pdfFile path: ${pdfFile?.absolutePath}")
                    android.util.Log.e("MainActivity", "  - exists: ${pdfFile?.exists()}")
                    android.util.Log.e("MainActivity", "  - canRead: ${pdfFile?.canRead()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "✗ Exception in applyPdfDynamicColors: ${e.message}", e)
                e.printStackTrace()
            }
            android.util.Log.d("MainActivity", "=== applyPdfDynamicColors END ===")
        }
    }

    /**
     * Obtiene el archivo PDF desde el URI usando ContentResolver.
     * Retorna null si no se puede acceder al archivo.
     */
    private fun getPdfFileFromUri(uriString: String): File? {
        return try {
            val uri = uriString.toUri()
            // Crear archivo temporal en caché para procesamiento
            val tempFile = File(cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")

            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                tempFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun openLastPdfOrPrompt() {
        lifecycleScope.launch {
            val lastPdf = viewModel.getLastOpenedPdf()
            if (lastPdf != null) {
                openPdfViewer(lastPdf)
            } else {
                showErrorMessage("No hay PDFs recientes para abrir.")
            }
        }
    }

    private fun showDeletePdfDialog(pdf: PdfHistoryEntity) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar PDF")
            .setMessage("¿Estás seguro de que quieres eliminar ${pdf.fileName} del historial?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deletePdf(pdf)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "ahora"
            diff < 3600_000 -> "hace ${diff / 60_000} min"
            diff < 86400_000 -> "hace ${diff / 3600_000}h"
            diff < 7 * 86400_000 -> "hace ${diff / 86400_000} días"
            else -> {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestamp))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_changelog -> {
                startActivity(Intent(this, ChangelogActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Update the ProgressBar with a shake animation and smooth color grading.
    // - If the animation crosses the 80% threshold it runs in two phases: [start..80] (normal) and [80..target] (accelerated).
    // - Shake starts when reaching 80% and its speed increases as progress approaches 100%.
    // - Color smoothly interpolates between the primary color and red in the 85..100 range.
    private fun updateProgressBarWithAnimation(progress: Int) {
        // Cancelar animadores previos
        progressAnimator?.cancel()
        progressAnimator = null

        val startProgress = progressBar.progress
        if (startProgress == progress) {
            // Ajustar shake si corresponde
            if (progress >= 80) ensureShakeForProgress(progress) else stopShake()
            return
        }

        // Helper para aplicar color según valor actual
        fun applyColorForValue(value: Int) {
            val color = when {
                value < 85 -> ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_primary)
                value >= 100 -> Color.parseColor("#D32F2F")
                else -> {
                    val greenColor = ContextCompat.getColor(this@MainActivity, R.color.md_theme_light_primary)
                    val redColor = Color.parseColor("#D32F2F")
                    val factor = (value - 85) / 15f
                    @Suppress("DEPRECATION")
                    ArgbEvaluator().evaluate(factor, greenColor, redColor) as Int
                }
            }

            val progressDrawable = progressBar.progressDrawable as? LayerDrawable
            progressDrawable?.let { layerDrawable ->
                val progressLayer = layerDrawable.findDrawableByLayerId(android.R.id.progress)
                if (progressLayer is ClipDrawable) {
                    progressLayer.drawable?.setTint(color)
                } else {
                    progressLayer?.setTint(color)
                }
            }
        }

        // Si la animación cruza 80% y necesitamos dividir en fases
        if (startProgress < 80 && progress >= 80) {
            // Fase 1: start -> 80 (ritmo normal)
            val diff1 = 80 - startProgress
            val duration1 = (diff1 * 25).coerceAtLeast(220).toLong()

            val anim1 = ValueAnimator.ofInt(startProgress, 80).apply {
                duration = duration1
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener { a ->
                    val cur = a.animatedValue as Int
                    progressBar.progress = cur
                    applyColorForValue(cur)
                }
            }

            // Fase 2: 80 -> progress (acelerada; más progreso -> más rápida)
            val diff2 = progress - 80
            val duration2 = ((diff2 * 12).coerceAtLeast(160)).toLong()
            val anim2 = ValueAnimator.ofInt(80, progress).apply {
                duration = duration2
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { a ->
                    val cur = a.animatedValue as Int
                    progressBar.progress = cur
                    applyColorForValue(cur)
                    // Mientras avanzamos por encima de 80, actualizar la velocidad del temblor
                    ensureShakeForProgress(cur)
                }
            }

            // Enlazar animadores secuencialmente
            anim1.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Arrancar anim2 y guardar referencia
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
                    // Si el objetivo final es menor que 80 (no debería aquí), detener temblor
                    if (progress < 80) stopShake()
                }

                override fun onAnimationCancel(animation: Animator) {
                    progressAnimator = null
                }
            })

            // Guardar referencia y arrancar la primera fase
            progressAnimator = anim1
            anim1.start()
            return
        }

        // Si empezamos ya >=80 o no cruzamos 80, animar en una sola fase
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
                progressBar.progress = cur
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

    // Asegura que el ObjectAnimator de "shake" exista y su duración se ajuste según el progreso (80..100)
    private fun ensureShakeForProgress(current: Int) {
        // Mapear current [80..100] a shake duration [900ms .. 180ms] inversamente proporcional
        val clamped = current.coerceIn(80, 100)
        val mappedDuration = ((100 - clamped) * 36 + 180).toLong().coerceAtLeast(120L)

        if (shakeAnimator == null) {
            shakeAnimator = ObjectAnimator.ofFloat(progressBar, "translationX", -6f, 6f).apply {
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
        progressBar.translationX = 0f
    }

    override fun onDestroy() {
        progressAnimator?.cancel()
        progressAnimator = null
        shakeAnimator?.cancel()
        shakeAnimator = null
        super.onDestroy()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun isDarkMode(context: Context): Boolean {
        // Siempre retornar false ya que solo usamos tema claro
        return false
    }

    private fun updateStatusBarIconColor(isDark: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDark
    }
}
