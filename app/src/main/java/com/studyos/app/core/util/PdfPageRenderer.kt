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
) {
    fun recycle() {
        try {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        } catch (_: Exception) {}
    }
}

/**
 * Memory-hardened PDF Page Renderer with:
 * - RGB_565 color depth (50% less RAM than ARGB_8888)
 * - Dynamic downsampling to prevent OutOfMemoryError
 * - Explicit bitmap recycling and OOM protection fallback
 */
object PdfPageRenderer {

    suspend fun renderPdfPages(
        context: Context,
        uri: Uri,
        maxPages: Int = 50,
        targetWidth: Int = 1080
    ): List<RenderedPdfPage> = withContext(Dispatchers.IO) {
        val pages = mutableListOf<RenderedPdfPage>()
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        // Cap target width to 1080 to prevent excessive allocations on Ultra-HD tablets
        val safeWidth = targetWidth.coerceIn(360, 1080)

        try {
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
                try {
                    val page = renderer.openPage(i)
                    val aspectRatio = page.height.toFloat() / page.width.toFloat()
                    val renderWidth = safeWidth
                    val renderHeight = (renderWidth * aspectRatio).toInt().coerceAtLeast(300)

                    // Use RGB_565: 2 bytes per pixel instead of 4 bytes (ARGB_8888) -> 50% RAM savings
                    val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.RGB_565)
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
                } catch (oom: OutOfMemoryError) {
                    System.gc()
                    // If OOM occurs on large documents, stop rendering further pages to prevent app crash
                    break
                } catch (e: Exception) {
                    // Skip problematic page
                }
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

    suspend fun renderSinglePage(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): RenderedPdfPage? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val safeWidth = targetWidth.coerceIn(360, 1080)

        try {
            tempFile = File.createTempFile("pdf_single_render_", ".pdf", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (pageIndex < 0 || pageIndex >= totalPages) return@withContext null

            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val renderWidth = safeWidth
            val renderHeight = (renderWidth * aspectRatio).toInt().coerceAtLeast(300)

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            RenderedPdfPage(
                pageNumber = pageIndex + 1,
                totalPages = totalPages,
                bitmap = bitmap,
                width = renderWidth,
                height = renderHeight
            )
        } catch (oom: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) {
            null
        } finally {
            try {
                renderer?.close()
                pfd?.close()
                tempFile?.delete()
            } catch (_: Exception) {}
        }
    }

    fun recycleAll(pages: List<RenderedPdfPage>) {
        pages.forEach { it.recycle() }
    }
}
