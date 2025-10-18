package ia.ankherth.grease

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import android.view.HapticFeedbackConstants
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import ia.ankherth.grease.adapter.MainPagerAdapter
import ia.ankherth.grease.adapter.PdfHistoryAdapter
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
    private lateinit var viewPager: ViewPager2
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
    private lateinit var cardLastPdf: androidx.cardview.widget.CardView
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

    // Adapter
    private lateinit var pagerAdapter: MainPagerAdapter
    private lateinit var recentPdfsAdapter: PdfHistoryAdapter

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

        setupViewPager()
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
        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabAddPdf = findViewById(R.id.fabAddPdf)
        errorContainer = findViewById(R.id.errorContainer)
        textErrorMessage = findViewById(R.id.textErrorMessage)
        buttonDismissError = findViewById(R.id.buttonDismissError)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Inicializar vistas de las tarjetas en la pantalla principal
        mainContentScroll = findViewById(R.id.mainContentScroll)
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage)
        cardLastPdf = findViewById(R.id.cardLastPdf)
        tvPdfTitle = findViewById(R.id.tvPdfTitle)
        tvPdfMeta = findViewById(R.id.tvPdfMeta)
        tvLastRead = findViewById(R.id.tvLastRead)
        progressBar = findViewById(R.id.progressBar)
        tvProgress = findViewById(R.id.tvProgress)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        recyclerViewRecentPdfs = findViewById(R.id.recyclerViewRecentPdfs)
        ivPdfPreview = findViewById(R.id.ivPdfPreview)
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

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false // Disable swipe to prevent accidental navigation
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
                    // Mostrar tarjetas principales, ocultar ViewPager
                    mainContentScroll.visibility = View.VISIBLE
                    viewPager.visibility = View.GONE
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
        viewPager.visibility = View.GONE
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
            adapter = recentPdfsAdapter
            setHasFixedSize(true)
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
                // Mostrar feedback al usuario
                Snackbar.make(rootView, "Lista actualizada", Snackbar.LENGTH_SHORT).show()
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

                progressBar.progress = progressPercentage

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

                // Lista secundaria: siguientes 4 PDFs (para un total de 5 incluyendo el principal)
                recentPdfsAdapter.submitList(pdfs.drop(1).take(4))
                recyclerViewRecentPdfs.isVisible = pdfs.size > 1

            } else {
                // Estado vacío
                mainContentScroll.visibility = View.GONE
                emptyStateContainer.visibility = View.VISIBLE
            }
        }

        viewModel.userName.observe(this) { name ->
            tvWelcomeMessage.text = if (!name.isNullOrBlank()) {
                "Bienvenido, $name"
            } else {
                "Bienvenido de vuelta"
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
            R.id.action_storage_access -> {
                openStorageTree.launch(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
