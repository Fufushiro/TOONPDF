package ia.ankherth.grease

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ia.ankherth.grease.adapter.ChangelogAdapter
import ia.ankherth.grease.databinding.ActivityChangelogBinding
import ia.ankherth.grease.viewmodel.ChangelogViewModel

/**
 * Actividad para mostrar el registro interno de cambios (changelog)
 * Permite visualizar todos los cambios realizados en la aplicación por versión
 */
class ChangelogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangelogBinding
    private val viewModel: ChangelogViewModel by viewModels()
    private lateinit var adapter: ChangelogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangelogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupCurrentVersion()
        observeChanges()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChangelogAdapter()
        binding.recyclerViewChangelog.apply {
            layoutManager = LinearLayoutManager(this@ChangelogActivity)
            adapter = this@ChangelogActivity.adapter
        }
    }

    private fun setupCurrentVersion() {
        // Mostrar la versión actual de la aplicación usando packageManager como alternativa
        val versionName = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "0.2.4" // Versión por defecto si no se puede obtener
        }
        binding.textCurrentVersion.text = "v$versionName"
    }

    private fun observeChanges() {
        // Observar cambios del registro y actualizar la UI
        viewModel.userVisibleChanges.observe(this) { changes ->
            if (changes.isEmpty()) {
                binding.recyclerViewChangelog.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            } else {
                binding.recyclerViewChangelog.visibility = View.VISIBLE
                binding.emptyState.visibility = View.GONE
                adapter.submitList(changes)
            }
        }

        // Inicializar el registro si está vacío
        viewModel.initializeChangelogIfNeeded()
    }
}
