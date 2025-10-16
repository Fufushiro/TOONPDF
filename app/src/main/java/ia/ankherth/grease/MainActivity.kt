package ia.ankherth.grease

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import ia.ankherth.grease.adapter.PdfHistoryAdapter
import ia.ankherth.grease.data.room.PdfHistoryEntity
import ia.ankherth.grease.util.ThemeUtils
import ia.ankherth.grease.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.core.widget.NestedScrollView
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Actividad principal mejorada con implementaciones para:
 * - Persistencia robusta de historial de PDFs
 * - Acceso a almacenamiento mediante SAF con permisos persistentes
 * - Pull-to-refresh para actualización en tiempo real
 * - Personalización de nombre y foto de inicio
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var allPdfs: List<PdfHistoryEntity> = emptyList()
    private lateinit var pdfAdapter: PdfHistoryAdapter

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var textGreeting: TextView
    private lateinit var editTextSearch: EditText
    private lateinit var buttonClearSearch: ImageButton
    private lateinit var recyclerViewPdfs: RecyclerView
    private lateinit var fabAddPdf: FloatingActionButton
    private lateinit var cardContinueReading: CardView
    private lateinit var textContinueReadingTitle: TextView
    private lateinit var textContinueReadingProgress: TextView
    private lateinit var buttonContinueReading: Button
    private lateinit var buttonDismissError: Button
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var layoutEmptyState: View
    private lateinit var errorContainer: View
    private lateinit var textErrorMessage: TextView
    private lateinit var imageUserAvatar: ImageView

    // New UI
    private lateinit var rootView: View

    // SAF registrars
    private val openPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleNewPdfSelection(it) }
    }

    private val relocatePdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handlePdfRelocation(it, pendingRelocationPdf) }
        pendingRelocationPdf = null
    }

    private val pickAvatar = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
            viewModel.setUserAvatarUri(it.toString())
        }
    }

    // Almacenar temporalmente el PDF que está pendiente de reubicación
    private var pendingRelocationPdf: PdfHistoryEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before inflating UI (persisted)
        applyPersistedNightMode()
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar referencias a las vistas
        initViews()
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // Insets: add top padding for status bar
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, status, v.paddingRight, v.paddingBottom)
            insets
        }

        // Update status bar icon color based on current mode
        updateStatusBarIconColor(isDarkMode(this))

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        setupRefreshLayout()
        observeData()
        setupErrorHandling()

        // Manejar intent si la app fue abierta con un PDF
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW) {
                // Intent de otra app: intentar abrir y, si se desea, el usuario puede agregarlo a la biblioteca
                handleNewPdfSelection(uri)
            }
        }
    }

    private fun applyPersistedNightMode() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isDark = prefs.getBoolean("pref_dark", false)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            if (isDark) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun isDarkMode(context: Context): Boolean {
        // Prefer boolean flag if set, fallback to ThemeUtils string
        val prefs = context.getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.contains("pref_dark")) return prefs.getBoolean("pref_dark", false)
        return ThemeUtils.readThemePref(context) in listOf("dark", "amoled")
    }

    private fun updateStatusBarIconColor(isDark: Boolean) {
        // Use WindowInsetsControllerCompat to avoid deprecated systemUiVisibility flags
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDark
    }

    private fun initViews() {
        rootView = findViewById(R.id.rootView)
        toolbar = findViewById(R.id.toolbar)
        textGreeting = findViewById(R.id.textGreeting)
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonClearSearch = findViewById(R.id.buttonClearSearch)
        recyclerViewPdfs = findViewById(R.id.recyclerViewPdfs)
        fabAddPdf = findViewById(R.id.fabAddPdf)
        buttonContinueReading = findViewById(R.id.buttonContinueReading)
        cardContinueReading = findViewById(R.id.cardContinueReading)
        textContinueReadingTitle = findViewById(R.id.textContinueReadingTitle)
        textContinueReadingProgress = findViewById(R.id.textContinueReadingProgress)
        buttonDismissError = findViewById(R.id.buttonDismissError)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        errorContainer = findViewById(R.id.errorContainer)
        textErrorMessage = findViewById(R.id.textErrorMessage)
        imageUserAvatar = findViewById(R.id.imageUserAvatar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.action_changelog -> { startActivity(Intent(this, ChangelogActivity::class.java)); true }
            R.id.action_about -> { showAboutDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPdfAccessibility()
        updateStatusBarIconColor(isDarkMode(this))
    }

    private fun setupUI() {
        // Establecer saludo basado en la hora del día + nombre personalizado
        viewModel.userName.observe(this) { name ->
            textGreeting.text = composeGreeting(name)
        }

        // Configurar funcionalidad de búsqueda
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterPdfs(s.toString())
                buttonClearSearch.isVisible = !s.isNullOrEmpty()
            }
        })

        // Cargar avatar persistido
        viewModel.userAvatarUri.observe(this) { avatarUri ->
            if (!avatarUri.isNullOrBlank()) {
                imageUserAvatar.setImageURI(Uri.parse(avatarUri))
            } else {
                imageUserAvatar.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun setupRecyclerView() {
        pdfAdapter = PdfHistoryAdapter(
            onPdfClick = { pdf -> openPdf(pdf) },
            onDeleteClick = { pdf ->
                AlertDialog.Builder(this)
                    .setTitle("Eliminar del historial")
                    .setMessage("¿Estás seguro de que deseas eliminar \"${pdf.fileName}\" del historial?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.deletePdf(pdf)
                        Toast.makeText(this, "PDF eliminado del historial", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            },
            onRelocateClick = { pdf -> startPdfRelocation(pdf) }
        )

        recyclerViewPdfs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = pdfAdapter
        }
    }

    private fun setupClickListeners() {
        buttonClearSearch.setOnClickListener {
            editTextSearch.text.clear(); buttonClearSearch.visibility = View.GONE
        }
        fabAddPdf.setOnClickListener { openPdfPicker() }
        buttonContinueReading.setOnClickListener {
            viewModel.getMostRecentPdf { recentPdf: PdfHistoryEntity? -> recentPdf?.let { pdf -> openPdf(pdf) } }
        }
        buttonDismissError.setOnClickListener { hideErrorMessage(); viewModel.clearErrorEvent() }
        imageUserAvatar.setOnClickListener { pickAvatar.launch(arrayOf("image/*")) }
        toolbar.setNavigationOnClickListener {
            findViewById<NestedScrollView>(R.id.nestedScroll)?.smoothScrollTo(0, 0)
        }
    }

    private fun observeData() {
        viewModel.allPdfs.observe(this) { pdfs: List<PdfHistoryEntity> ->
            allPdfs = pdfs
            updateUI(pdfs)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.errorEvent.observe(this) { errorEvent ->
            errorEvent?.let {
                showErrorMessage(it.message)
            } ?: hideErrorMessage()
        }
    }

    private fun setupErrorHandling() {
        errorContainer.visibility = View.GONE
    }

    private fun setupRefreshLayout() {
        // Configurar pull-to-refresh
        swipeRefreshLayout.setOnRefreshListener { refreshPdfAccessibility() }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.purple_500,
            R.color.teal_200,
            R.color.purple_700
        )
    }

    private fun updateUI(pdfs: List<PdfHistoryEntity>) {
        if (pdfs.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            recyclerViewPdfs.visibility = View.GONE
            cardContinueReading.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerViewPdfs.visibility = View.VISIBLE

            // Mostrar PDF más reciente en sección "Continuar leyendo"
            val mostRecent = pdfs.firstOrNull()
            mostRecent?.let { pdf ->
                cardContinueReading.visibility = View.VISIBLE
                textContinueReadingTitle.text = pdf.fileName
                textContinueReadingProgress.text =
                    "Página ${pdf.lastPageRead + 1} / ${pdf.totalPages} • ${pdf.progressPercentage.toInt()}%"
            }

            pdfAdapter.submitList(pdfs)
        }
    }

    private fun filterPdfs(query: String) {
        if (query.isBlank()) {
            pdfAdapter.submitList(allPdfs)
        } else {
            val filteredList = allPdfs.filter { pdf ->
                pdf.fileName.contains(query, ignoreCase = true)
            }
            pdfAdapter.submitList(filteredList)
        }
    }

    private fun openPdfPicker() {
        openPdf.launch(arrayOf("application/pdf"))
    }

    private fun handleNewPdfSelection(uri: Uri) {
        try {
            // Intentar persistir permiso de lectura para acceso futuro
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* El proveedor puede no soportar persistencia */ }

            val fileName = getFileNameFromUri(uri) ?: "Documento PDF"
            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                putExtra(PdfViewerActivity.EXTRA_PDF_URI, uri)
                putExtra(PdfViewerActivity.EXTRA_FILE_NAME, fileName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            showErrorMessage("Error al abrir el PDF: ${e.message}")
        }
    }

    private fun openPdf(pdf: PdfHistoryEntity) {
        try {
            val uri = Uri.parse(pdf.uri)

            // Verificar si el archivo todavía existe
            if (checkIfFileExists(uri)) {
                val intent = Intent(this, PdfViewerActivity::class.java).apply {
                    putExtra(PdfViewerActivity.EXTRA_PDF_URI, uri)
                    putExtra(PdfViewerActivity.EXTRA_FILE_NAME, pdf.fileName)
                    putExtra(PdfViewerActivity.EXTRA_CURRENT_PAGE, pdf.lastPageRead)
                }
                startActivity(intent)

                if (!pdf.isAccessible) {
                    viewModel.updatePdfAccessibility(pdf.uri, true)
                }
            } else {
                showFileNotFoundDialog(pdf)
            }
        } catch (e: Exception) {
            showErrorMessage("Error al abrir el PDF: ${e.message}")
        }
    }

    private fun showFileNotFoundDialog(pdf: PdfHistoryEntity) {
        AlertDialog.Builder(this)
            .setTitle("Archivo no encontrado")
            .setMessage("El archivo \"${pdf.fileName}\" ya no existe o no es accesible. ¿Qué deseas hacer?")
            .setPositiveButton("Reubicar") { _, _ -> startPdfRelocation(pdf) }
            .setNegativeButton("Eliminar del historial") { _, _ -> viewModel.deletePdf(pdf) }
            .setNeutralButton("Cancelar", null)
            .show()
    }

    private fun startPdfRelocation(pdf: PdfHistoryEntity) {
        pendingRelocationPdf = pdf
        relocatePdf.launch(arrayOf("application/pdf"))
    }

    private fun handlePdfRelocation(newUri: Uri, oldPdf: PdfHistoryEntity?) {
        if (oldPdf == null) return
        try {
            val fileName = getFileNameFromUri(newUri) ?: oldPdf.fileName
            // Persistir permiso para el nuevo documento
            try {
                contentResolver.takePersistableUriPermission(newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}

            viewModel.addOrUpdatePdf(
                uri = newUri.toString(),
                fileName = fileName,
                totalPages = oldPdf.totalPages,
                currentPage = oldPdf.lastPageRead
            )
            viewModel.deletePdf(oldPdf)
            Toast.makeText(this, "PDF reubicado correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            showErrorMessage("Error al reubicar el PDF: ${e.message}")
        }
    }

    private fun refreshPdfAccessibility() {
        lifecycleScope.launch {
            swipeRefreshLayout.isRefreshing = true
            try {
                allPdfs.forEach { pdf ->
                    val uri = Uri.parse(pdf.uri)
                    val isAccessible = checkIfFileExists(uri)
                    if (pdf.isAccessible != isAccessible) {
                        viewModel.updatePdfAccessibility(pdf.uri, isAccessible)
                    }
                }
            } catch (e: Exception) {
                showErrorMessage("Error al actualizar el estado de los PDFs: ${e.message}")
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) it.getString(nameIndex) else null
                    } else null
                }
            }
            "file" -> Uri.parse(uri.toString()).lastPathSegment
            else -> null
        }
    }

    private fun checkIfFileExists(uri: Uri): Boolean {
        return try {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
        } catch (_: Exception) { false }
    }

    private fun composeGreeting(name: String?): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
        return if (!name.isNullOrBlank()) "$greeting, $name" else greeting
    }

    private fun showErrorMessage(message: String) {
        textErrorMessage.text = message
        errorContainer.visibility = View.VISIBLE
    }

    private fun hideErrorMessage() {
        errorContainer.visibility = View.GONE
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("PDFTOON")
            .setMessage("Lector de PDFs rápido y sencillo.\nVersión: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            .setPositiveButton("OK", null)
            .show()
    }
}
