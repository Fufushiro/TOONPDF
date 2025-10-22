package ia.ankherth.grease.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Utilidad para extraer el color dominante de un PDF y aplicarlo de forma elegante.
 * Incluye caché de colores y manejo de errores robusto.
 */
object PdfColorExtractor {

    private const val CACHE_FILE_NAME = "pdf_color_cache.txt"
    private const val DEFAULT_COLOR = 0xFF6200EE.toInt() // Material Purple
    private const val SAMPLE_WIDTH = 400 // Tamaño reducido para mejor rendimiento

    // Caché en memoria para evitar lecturas frecuentes
    private val colorCache = mutableMapOf<String, Int>()

    /**
     * Extrae el color dominante de la primera página del PDF.
     * Se ejecuta en un hilo secundario y cachea el resultado.
     */
    suspend fun extractDominantColor(
        context: Context,
        pdfFile: File
    ): PdfColorResult = withContext(Dispatchers.IO) {
        try {
            Log.d("PdfColorExtractor", "Extracting color from: ${pdfFile.name}")

            // Verificar caché en memoria primero
            val cacheKey = "${pdfFile.absolutePath}:${pdfFile.lastModified()}"
            colorCache[cacheKey]?.let {
                Log.d("PdfColorExtractor", "Color found in memory cache: ${Integer.toHexString(it)}")
                return@withContext PdfColorResult.Success(it)
            }

            // Verificar caché en disco
            val cachedColor = loadColorFromDiskCache(context, cacheKey)
            if (cachedColor != null) {
                Log.d("PdfColorExtractor", "Color found in disk cache: ${Integer.toHexString(cachedColor)}")
                colorCache[cacheKey] = cachedColor
                return@withContext PdfColorResult.Success(cachedColor)
            }

            // Extraer color del PDF
            if (!pdfFile.exists() || !pdfFile.canRead()) {
                Log.e("PdfColorExtractor", "PDF file not accessible: ${pdfFile.absolutePath}")
                return@withContext PdfColorResult.Error("PDF file not accessible")
            }

            Log.d("PdfColorExtractor", "Opening PDF renderer...")
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )

            fileDescriptor.use { fd ->
                val pdfRenderer = PdfRenderer(fd)

                pdfRenderer.use { renderer ->
                    if (renderer.pageCount == 0) {
                        return@withContext PdfColorResult.Error("PDF has no pages")
                    }

                    val page = renderer.openPage(0)

                    page.use { p ->
                        // Calcular dimensiones manteniendo aspect ratio
                        val aspectRatio = p.width.toFloat() / p.height.toFloat()
                        val width = SAMPLE_WIDTH
                        val height = (width / aspectRatio).toInt()

                        // Renderizar la página a un bitmap reducido
                        val bitmap = Bitmap.createBitmap(
                            width,
                            height,
                            Bitmap.Config.ARGB_8888
                        )

                        p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        // Extraer color dominante usando Palette
                        val palette = Palette.from(bitmap).generate()

                        // Priorizar colores vibrantes, luego oscuros, luego cualquiera
                        val dominantColor = palette.vibrantSwatch?.rgb
                            ?: palette.darkVibrantSwatch?.rgb
                            ?: palette.lightVibrantSwatch?.rgb
                            ?: palette.mutedSwatch?.rgb
                            ?: palette.darkMutedSwatch?.rgb
                            ?: palette.lightMutedSwatch?.rgb
                            ?: palette.dominantSwatch?.rgb
                            ?: DEFAULT_COLOR

                        Log.d("PdfColorExtractor", "Extracted color: ${Integer.toHexString(dominantColor)}")
                        Log.d("PdfColorExtractor", "Vibrant: ${palette.vibrantSwatch?.rgb?.let { Integer.toHexString(it) }}")
                        Log.d("PdfColorExtractor", "DarkVibrant: ${palette.darkVibrantSwatch?.rgb?.let { Integer.toHexString(it) }}")
                        Log.d("PdfColorExtractor", "LightVibrant: ${palette.lightVibrantSwatch?.rgb?.let { Integer.toHexString(it) }}")

                        bitmap.recycle()

                        // Guardar en caché
                        colorCache[cacheKey] = dominantColor
                        saveColorToDiskCache(context, cacheKey, dominantColor)

                        return@withContext PdfColorResult.Success(dominantColor)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PdfColorExtractor", "Error extracting color: ${e.message}", e)
            return@withContext PdfColorResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Carga un color del caché en disco.
     */
    private fun loadColorFromDiskCache(context: Context, key: String): Int? {
        return try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            if (!cacheFile.exists()) return null

            cacheFile.readLines().forEach { line ->
                val parts = line.split("|")
                if (parts.size == 2 && parts[0] == key) {
                    return parts[1].toIntOrNull()
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Guarda un color en el caché en disco.
     */
    private fun saveColorToDiskCache(context: Context, key: String, color: Int) {
        try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            val existingLines = if (cacheFile.exists()) {
                cacheFile.readLines().filter { !it.startsWith("$key|") }
            } else {
                emptyList()
            }

            // Limitar tamaño del caché a 100 entradas
            val linesToKeep = if (existingLines.size >= 100) {
                existingLines.takeLast(99)
            } else {
                existingLines
            }

            cacheFile.writeText(
                (linesToKeep + "$key|$color").joinToString("\n")
            )
        } catch (_: Exception) {
            // Fallar silenciosamente si no se puede escribir en caché
        }
    }

    /**
     * Limpia la caché en memoria y disco.
     */
    fun clearCache(context: Context) {
        colorCache.clear()
        try {
            File(context.cacheDir, CACHE_FILE_NAME).delete()
        } catch (_: Exception) {
            // Ignorar
        }
    }

    /**
     * Genera un color de texto óptimo (blanco o negro) basado en el contraste.
     */
    fun getContrastColor(backgroundColor: Int): Int {
        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        return if (luminance > 0.5) Color.BLACK else Color.WHITE
    }

    /**
     * Calcula si un color es suficientemente oscuro.
     */
    fun isDarkColor(color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    /**
     * Genera una versión más clara del color con el factor especificado.
     */
    fun lightenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = min(hsv[2] * (1 + factor), 1f)
        return Color.HSVToColor(hsv)
    }

    /**
     * Genera una versión más saturada del color.
     */
    fun saturateColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = min(hsv[1] * (1 + factor), 1f)
        return Color.HSVToColor(hsv)
    }

    /**
     * Genera una versión más oscura del color.
     */
    fun darkenColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = max(hsv[2] * (1 - factor), 0f)
        return Color.HSVToColor(hsv)
    }

    /**
     * Aplica transparencia a un color.
     */
    fun applyAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}

/**
 * Resultado de la extracción de color.
 */
sealed class PdfColorResult {
    data class Success(val color: Int) : PdfColorResult()
    data class Error(val message: String) : PdfColorResult()
}

