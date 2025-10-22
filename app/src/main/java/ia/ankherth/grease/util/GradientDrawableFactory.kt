package ia.ankherth.grease.util

import android.graphics.drawable.GradientDrawable
import android.graphics.Color

/**
 * Factory para crear gradientes elegantes basados en el color extraído del PDF.
 */
object GradientDrawableFactory {

    /**
     * Crea un gradiente diagonal elegante con transparencia sutil.
     *
     * @param baseColor Color base extraído del PDF
     * @param cornerRadius Radio de las esquinas en px
     * @return GradientDrawable configurado
     */
    fun createDiagonalGradient(baseColor: Int, cornerRadius: Float): GradientDrawable {
        // Crear versiones del color con diferentes saturaciones y luminosidades
        val startColor = PdfColorExtractor.saturateColor(baseColor, 0.2f)
        val endColor = PdfColorExtractor.lightenColor(baseColor, 0.3f)

        // Aplicar transparencia baja para mantener legibilidad
        val startColorWithAlpha = PdfColorExtractor.applyAlpha(startColor, 230) // ~90% opacidad
        val endColorWithAlpha = PdfColorExtractor.applyAlpha(endColor, 200) // ~78% opacidad

        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR, // Top-Left to Bottom-Right (diagonal)
            intArrayOf(startColorWithAlpha, endColorWithAlpha)
        ).apply {
            this.cornerRadius = cornerRadius
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
    }

    /**
     * Crea un gradiente más sutil para fondos secundarios.
     */
    fun createSubtleGradient(baseColor: Int, cornerRadius: Float): GradientDrawable {
        val lightColor = PdfColorExtractor.lightenColor(baseColor, 0.5f)
        val veryLightColor = PdfColorExtractor.lightenColor(baseColor, 0.7f)

        val startColorWithAlpha = PdfColorExtractor.applyAlpha(lightColor, 150)
        val endColorWithAlpha = PdfColorExtractor.applyAlpha(veryLightColor, 100)

        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(startColorWithAlpha, endColorWithAlpha)
        ).apply {
            this.cornerRadius = cornerRadius
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
    }

    /**
     * Crea un gradiente radial para efectos especiales.
     */
    fun createRadialGradient(baseColor: Int, radius: Float): GradientDrawable {
        val centerColor = PdfColorExtractor.lightenColor(baseColor, 0.4f)
        val edgeColor = PdfColorExtractor.darkenColor(baseColor, 0.2f)

        val centerColorWithAlpha = PdfColorExtractor.applyAlpha(centerColor, 220)
        val edgeColorWithAlpha = PdfColorExtractor.applyAlpha(edgeColor, 180)

        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(centerColorWithAlpha, edgeColorWithAlpha)
        ).apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = radius
        }
    }

    /**
     * Determina si debe usarse texto claro u oscuro sobre el gradiente.
     */
    fun shouldUseLightText(baseColor: Int): Boolean {
        return PdfColorExtractor.isDarkColor(baseColor)
    }

    /**
     * Obtiene el color de texto óptimo para usar sobre el gradiente.
     */
    fun getOptimalTextColor(baseColor: Int): Int {
        return PdfColorExtractor.getContrastColor(baseColor)
    }

    /**
     * Obtiene un color de acento complementario al color base.
     */
    fun getAccentColor(baseColor: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)

        // Rotar el matiz 30 grados para obtener un color complementario
        hsv[0] = (hsv[0] + 30) % 360
        hsv[1] = (hsv[1] * 1.2f).coerceIn(0f, 1f)

        return Color.HSVToColor(hsv)
    }
}

