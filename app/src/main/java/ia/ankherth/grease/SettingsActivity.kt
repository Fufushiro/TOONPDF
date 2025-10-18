package ia.ankherth.grease

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
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
        }
    }
}
