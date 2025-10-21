package ia.ankherth.grease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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

    private val openStorageTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.setStorageTreeUri(it.toString())
            } catch (_: SecurityException) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force light mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment(::onPickAvatar, ::onStorageAccess, ::onAbout))
            .commit()
    }

    private fun onPickAvatar() {
        pickImage.launch(arrayOf("image/*"))
    }

    private fun onStorageAccess() {
        openStorageTree.launch(null)
    }

    private fun onAbout() {
        startActivity(Intent(this, AboutActivity::class.java))
    }

    class SettingsFragment(
        private val onPickAvatar: () -> Unit,
        private val onStorageAccess: () -> Unit,
        private val onAbout: () -> Unit
    ) : PreferenceFragmentCompat() {
        private val viewModel: MainViewModel by lazy { (requireActivity() as SettingsActivity).viewModel }

        private var hapticsPref: SwitchPreferenceCompat? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            val namePref = findPreference<EditTextPreference>("pref_user_name")
            namePref?.setOnPreferenceChangeListener { _, newValue ->
                viewModel.setUserName(newValue?.toString())
                true
            }

            val avatarPref = findPreference<Preference>("pref_user_avatar")
            avatarPref?.setOnPreferenceClickListener {
                onPickAvatar()
                true
            }

            // Haptics preference
            hapticsPref = findPreference("pref_haptics")
            // Reflect initial value when available
            viewModel.hapticsEnabled.observe(this) { enabled ->
                hapticsPref?.isChecked = enabled == true
            }
            hapticsPref?.setOnPreferenceChangeListener { _, newValue ->
                viewModel.setHapticsEnabled((newValue as? Boolean) == true)
                true
            }

            // Language preference
            val languagePref = findPreference<ListPreference>("pref_language")
            languagePref?.setOnPreferenceChangeListener { _, newValue ->
                val lang = (newValue as? String).orEmpty()
                val locales = if (lang == "system" || lang.isBlank()) {
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    LocaleListCompat.forLanguageTags(lang)
                }
                AppCompatDelegate.setApplicationLocales(locales)
                // recreate to apply
                requireActivity().recreate()
                true
            }

            // Storage access preference
            val storagePref = findPreference<Preference>("pref_storage_access")
            storagePref?.setOnPreferenceClickListener {
                onStorageAccess()
                true
            }

            // About preference
            val aboutPref = findPreference<Preference>("pref_about")
            aboutPref?.setOnPreferenceClickListener {
                onAbout()
                true
            }
        }
    }
}
