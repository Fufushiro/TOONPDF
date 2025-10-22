package ia.ankherth.grease.util

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import ia.ankherth.grease.R
import kotlinx.coroutines.launch
import java.io.File

/**
 * Clase helper para aplicar colores extraídos del PDF a la tarjeta de Material Design
 * con animaciones suaves y manejo de contraste automático.
 */
class PdfCardColorizer(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    companion object {
        private const val ANIMATION_DURATION = 400L
        private const val DEFAULT_CORNER_RADIUS = 16f // dp
    }

    /**
     * Aplica el color extraído del PDF a la tarjeta con gradiente y ajuste de texto.
     * Esta función es lifecycle-aware y se ejecuta en un hilo secundario.
     */
    fun applyPdfColorToCard(
        pdfFile: File,
        cardContentContainer: View,
        titleTextView: TextView,
        metaTextView: TextView,
        progressTextView: TextView,
        lastReadTextView: TextView,
        progressBar: ProgressBar
    ) {
        Log.d("PdfCardColorizer", "Starting color application for: ${pdfFile.name}")
        lifecycleScope.launch {
            // Extraer color en background thread
            when (val result = PdfColorExtractor.extractDominantColor(context, pdfFile)) {
                is PdfColorResult.Success -> {
                    val dominantColor = result.color
                    Log.d("PdfCardColorizer", "Successfully extracted color: ${Integer.toHexString(dominantColor)}")

                    // Aplicar en el hilo principal
                    applyColorToCardUI(
                        cardContentContainer = cardContentContainer,
                        color = dominantColor,
                        titleTextView = titleTextView,
                        metaTextView = metaTextView,
                        progressTextView = progressTextView,
                        lastReadTextView = lastReadTextView,
                        progressBar = progressBar
                    )
                }
                is PdfColorResult.Error -> {
                    Log.e("PdfCardColorizer", "Error extracting color: ${result.message}")
                    // Aplicar color por defecto en caso de error
                    applyDefaultColor(
                        cardContentContainer = cardContentContainer,
                        titleTextView = titleTextView,
                        metaTextView = metaTextView,
                        progressTextView = progressTextView,
                        lastReadTextView = lastReadTextView,
                        progressBar = progressBar
                    )
                }
            }
        }
    }

    /**
     * Aplica el color extraído a todos los elementos de la UI.
     */
    private fun applyColorToCardUI(
        cardContentContainer: View,
        color: Int,
        titleTextView: TextView,
        metaTextView: TextView,
        progressTextView: TextView,
        lastReadTextView: TextView,
        progressBar: ProgressBar
    ) {
        // Crear gradiente diagonal elegante
        val cornerRadiusPx = DEFAULT_CORNER_RADIUS * context.resources.displayMetrics.density
        val gradient = GradientDrawableFactory.createDiagonalGradient(color, cornerRadiusPx)

        // Aplicar gradiente al contenedor interno con animación suave
        animateBackgroundChange(cardContentContainer, gradient)

        // Determinar color de texto óptimo basado en contraste
        val textColor = GradientDrawableFactory.getOptimalTextColor(color)
        val secondaryTextColor = if (GradientDrawableFactory.shouldUseLightText(color)) {
            Color.argb(200, 255, 255, 255) // Blanco con 78% opacidad
        } else {
            Color.argb(180, 0, 0, 0) // Negro con 70% opacidad
        }

        // Aplicar colores de texto con animación
        animateTextColorChange(titleTextView, textColor)
        animateTextColorChange(metaTextView, secondaryTextColor)
        animateTextColorChange(lastReadTextView, secondaryTextColor)

        // Color de acento para el progreso
        val accentColor = GradientDrawableFactory.getAccentColor(color)
        animateTextColorChange(progressTextView, accentColor)

        // Aplicar color al ProgressBar
        applyProgressBarColor(progressBar, accentColor)
    }

    /**
     * Aplica colores por defecto en caso de error.
     */
    private fun applyDefaultColor(
        cardContentContainer: View,
        titleTextView: TextView,
        metaTextView: TextView,
        progressTextView: TextView,
        lastReadTextView: TextView,
        progressBar: ProgressBar
    ) {
        // Usar el color primario del tema como fallback
        val defaultColor = ContextCompat.getColor(context, R.color.default_card_color)

        applyColorToCardUI(
            cardContentContainer = cardContentContainer,
            color = defaultColor,
            titleTextView = titleTextView,
            metaTextView = metaTextView,
            progressTextView = progressTextView,
            lastReadTextView = lastReadTextView,
            progressBar = progressBar
        )
    }

    /**
     * Anima el cambio de fondo de la tarjeta.
     */
    private fun animateBackgroundChange(view: View, newBackground: android.graphics.drawable.Drawable) {
        // Aplicar directamente para evitar parpadeo
        view.background = newBackground

        // Animar fade in
        view.alpha = 0.7f
        view.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .start()
    }

    /**
     * Anima el cambio de color de texto.
     */
    private fun animateTextColorChange(textView: TextView, newColor: Int) {
        val currentColor = textView.currentTextColor

        ValueAnimator.ofArgb(currentColor, newColor).apply {
            duration = ANIMATION_DURATION
            addUpdateListener { animator ->
                textView.setTextColor(animator.animatedValue as Int)
            }
            start()
        }
    }

    /**
     * Aplica color al ProgressBar con el tinte apropiado.
     */
    private fun applyProgressBarColor(progressBar: ProgressBar, color: Int) {
        progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)

        // Color de fondo de la barra (más claro)
        val backgroundColor = PdfColorExtractor.lightenColor(color, 0.6f)
        val backgroundWithAlpha = PdfColorExtractor.applyAlpha(backgroundColor, 100)
        progressBar.progressBackgroundTintList = android.content.res.ColorStateList.valueOf(backgroundWithAlpha)
    }

    /**
     * Limpia la caché de colores.
     */
    fun clearCache() {
        PdfColorExtractor.clearCache(context)
    }
}


