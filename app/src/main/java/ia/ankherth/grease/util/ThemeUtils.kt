package ia.ankherth.grease.util

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import ia.ankherth.grease.R

object ThemeUtils {
    private const val PREFS = "ui_prefs"
    private const val KEY_THEME = "app_theme_pref" // system | light | dark | amoled

    fun saveThemePref(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme).apply()
    }

    fun readThemePref(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "system") ?: "system"
    }

    fun applyTheme(activity: Activity, fullscreen: Boolean = false) {
        when (readThemePref(activity)) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "amoled" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                val style = if (fullscreen) R.style.Theme_PDFTOON_Amoled_Fullscreen else R.style.Theme_PDFTOON_Amoled_NoActionBar
                activity.setTheme(style)
            }
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

