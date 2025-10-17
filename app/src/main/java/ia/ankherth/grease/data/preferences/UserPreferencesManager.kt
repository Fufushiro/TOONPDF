package ia.ankherth.grease.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gestor de preferencias de usuario utilizando DataStore
 * Permite almacenar y recuperar configuraciones persistentes como permisos de almacenamiento
 */
class UserPreferencesManager(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

        // Keys para las preferencias
        private val STORAGE_PERMISSION_GRANTED = booleanPreferencesKey("storage_permission_granted")
        private val LAST_OPENED_PDF_URI = stringPreferencesKey("last_opened_pdf_uri")
        private val APP_THEME = stringPreferencesKey("app_theme") // system | light | dark | amoled
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_AVATAR_URI = stringPreferencesKey("user_avatar_uri")
        private val STORAGE_TREE_URI = stringPreferencesKey("storage_tree_uri")
        private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    // Acceso a preferencias como Flows
    val storagePermissionGranted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[STORAGE_PERMISSION_GRANTED] ?: false
    }

    val lastOpenedPdfUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_OPENED_PDF_URI]
    }

    val appTheme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME] ?: "system"
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME]
    }

    val userAvatarUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_AVATAR_URI]
    }

    val storageTreeUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[STORAGE_TREE_URI]
    }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAPTICS_ENABLED] ?: true
    }

    // Métodos para actualizar preferencias
    suspend fun updateStoragePermissionStatus(granted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[STORAGE_PERMISSION_GRANTED] = granted
        }
    }

    suspend fun saveLastOpenedPdfUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[LAST_OPENED_PDF_URI] = uri
            } else {
                preferences.remove(LAST_OPENED_PDF_URI)
            }
        }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }

    suspend fun setUserName(name: String?) {
        context.dataStore.edit { preferences ->
            if (name.isNullOrBlank()) {
                preferences.remove(USER_NAME)
            } else {
                preferences[USER_NAME] = name.trim()
            }
        }
    }

    suspend fun setUserAvatarUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(USER_AVATAR_URI)
            } else {
                preferences[USER_AVATAR_URI] = uri
            }
        }
    }

    suspend fun setStorageTreeUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(STORAGE_TREE_URI)
            } else {
                preferences[STORAGE_TREE_URI] = uri
            }
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAPTICS_ENABLED] = enabled
        }
    }
}
