package ia.ankherth.grease.util

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import ia.ankherth.grease.R

object ThemeUtils {
    private const val PREFS = "app_prefs"
    private const val KEY_THEME = "pref_app_theme"

    fun saveThemePref(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, "light")
            .apply()

        // Forzar siempre tema claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    fun readThemePref(context: Context): String {
        // Siempre retornar tema claro
        return "light"
    }

    fun applyTheme(activity: Activity, fullscreen: Boolean = false) {
        // Forzar siempre modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Aplicar tema claro
        val style = if (fullscreen) R.style.Theme_PDFTOON_Light_Fullscreen else R.style.Theme_PDFTOON_Light_NoActionBar
        activity.setTheme(style)
    }
}
