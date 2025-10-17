package ia.ankherth.grease.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades para formatear tiempo
 */
object TimeUtils {

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatRelative(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - millis
        val minutes = diff / 60000
        return when {
            minutes < 1 -> "justo ahora"
            minutes < 60 -> "${minutes} min"
            minutes < 60 * 24 -> "${minutes / 60} h"
            minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} d"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))
        }
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
