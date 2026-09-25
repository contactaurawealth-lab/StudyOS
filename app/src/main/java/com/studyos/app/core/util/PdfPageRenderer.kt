package com.studyos.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class RenderedPdfPage(
    val pageNumber: Int,
    val totalPages: Int,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int
)

object PdfPageRenderer {

    suspend fun renderPdfPages(
        context: Context,
        uri: Uri,
        maxPages: Int = 100,
        targetWidth: Int = 1080
    ): List<RenderedPdfPage> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<RenderedPdfPage>()
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            // Write stream to temp cache file to ensure a seekable ParcelFileDescriptor
            tempFile = File.createTempFile("pdf_page_render_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            val pagesToRender = minOf(totalPages, maxPages)

            for (i in 0 until pagesToRender) {
                val page = renderer.openPage(i)
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val renderWidth = targetWidth.coerceAtLeast(300)
                val renderHeight = (renderWidth * aspectRatio).toInt().coerceAtLeast(400)

                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                pages.add(
                    RenderedPdfPage(
                        pageNumber = i + 1,
                        totalPages = totalPages,
                        bitmap = bitmap,
                        width = renderWidth,
                        height = renderHeight
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
                pfd?.close()
                tempFile?.delete()
            } catch (_: Exception) {}
        }

        pages
    }
}
