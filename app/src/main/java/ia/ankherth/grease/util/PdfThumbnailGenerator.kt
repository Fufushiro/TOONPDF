package ia.ankherth.grease.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfThumbnailGenerator {

    suspend fun generateThumbnail(context: Context, pdfUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return@withContext null
            val renderer = PdfRenderer(pfd)
            val page = renderer.openPage(0)

            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()

            val thumbnailFile = saveBitmapToCache(context, bitmap, pdfUri)
            thumbnailFile?.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap, pdfUri: Uri): File? {
        return try {
            val cacheDir = context.cacheDir
            val fileName = "thumb_${pdfUri.lastPathSegment?.replace(Regex("[^a-zA-Z0-9]"), "_")}.png"
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, it)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

