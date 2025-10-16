package ia.ankherth.grease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import ia.ankherth.grease.util.ThemeUtils
import ia.ankherth.grease.viewmodel.MainViewModel

class SettingsActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                // Persistir permiso de lectura para el avatar
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { }
            viewModel.setUserAvatarUri(it.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment(::onPickAvatar))
            .commit()
    }

    private fun onPickAvatar() {
        pickImage.launch(arrayOf("image/*"))
    }

    class SettingsFragment(private val onPickAvatar: () -> Unit) : PreferenceFragmentCompat() {
        private val viewModel: MainViewModel by lazy { (requireActivity() as SettingsActivity).viewModel }

        private var storagePref: SwitchPreferenceCompat? = null

        // Launcher para seleccionar carpeta y conceder permisos persistentes
        private val openStorageTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    viewModel.setStorageTreeUri(uri.toString())
                } catch (e: SecurityException) {
                    // Ignorar: el proveedor puede no permitir persistencia
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            val namePref = findPreference<EditTextPreference>("pref_user_name")
            namePref?.setOnPreferenceChangeListener { _, newValue ->
                viewModel.setUserName(newValue?.toString())
                true
            }

            val themePref = findPreference<ListPreference>("pref_app_theme")
            themePref?.setOnPreferenceChangeListener { _, newValue ->
                val theme = newValue?.toString() ?: "system"
                viewModel.setAppTheme(theme)
                ThemeUtils.saveThemePref(requireContext(), theme)
                requireActivity().recreate()
                true
            }

            val avatarPref = findPreference<Preference>("pref_user_avatar")
            avatarPref?.setOnPreferenceClickListener {
                onPickAvatar()
                true
            }

            // Toggle de acceso a almacenamiento
            storagePref = findPreference("pref_storage_access")
            // Estado inicial
            storagePref?.isChecked = !viewModel.storageTreeUri.value.isNullOrBlank()

            storagePref?.setOnPreferenceChangeListener { _, newValue ->
                val enable = newValue as? Boolean ?: false
                if (enable) {
                    // Lanzar selector de carpeta; el check se actualizará cuando el usuario elija
                    openStorageTree.launch(null)
                    false
                } else {
                    // Revocar permiso persistente, si existe
                    val tree = viewModel.storageTreeUri.value
                    if (!tree.isNullOrBlank()) {
                        try {
                            val uri = Uri.parse(tree)
                            requireContext().contentResolver.releasePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (_: Exception) { }
                        viewModel.setStorageTreeUri(null)
                    }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            // Observar cambios y reflejarlos en el switch
            viewModel.storageTreeUri.observe(viewLifecycleOwner) { tree ->
                storagePref?.isChecked = !tree.isNullOrBlank()
            }
        }
    }
}
