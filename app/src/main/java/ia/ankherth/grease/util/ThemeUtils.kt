package ia.ankherth.grease.util

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import ia.ankherth.grease.R

object ThemeUtils {
    private const val PREFS = "app_prefs"
    private const val KEY_THEME = "pref_app_theme" // system | light | dark | amoled

    fun saveThemePref(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()

        // Aplicar el tema inmediatamente al AppCompatDelegate
        applyNightMode(theme)
    }

    fun readThemePref(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "system") ?: "system"
    }

    private fun applyNightMode(theme: String) {
        when (theme) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark", "amoled" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun applyTheme(activity: Activity, fullscreen: Boolean = false) {
        val theme = readThemePref(activity)

        // Primero aplicar el modo día/noche al sistema
        applyNightMode(theme)

        // Luego aplicar el tema específico de la actividad
        when (theme) {
            "light" -> {
                val style = if (fullscreen) R.style.Theme_PDFTOON_Light_Fullscreen else R.style.Theme_PDFTOON_Light_NoActionBar
                activity.setTheme(style)
            }
            "dark" -> {
                val style = if (fullscreen) R.style.Theme_PDFTOON_Dark_Fullscreen else R.style.Theme_PDFTOON_Dark_NoActionBar
                activity.setTheme(style)
            }
            "amoled" -> {
                val style = if (fullscreen) R.style.Theme_PDFTOON_Amoled_Fullscreen else R.style.Theme_PDFTOON_Amoled_NoActionBar
                activity.setTheme(style)
            }
            else -> {
                // Tema del sistema - se aplica automáticamente con MODE_NIGHT_FOLLOW_SYSTEM
            }
        }
    }
}
