package com.example.postureapp.domain.reports

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

object PdfThumbnailGenerator {
    fun renderFirstPage(pdfFile: File, outFile: File): Boolean {
        if (!pdfFile.exists()) return false
        return runCatching {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return false
                    renderer.openPage(0).use { page ->
                        val width = page.width
                        val height = page.height
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                        }
                    }
                }
            }
            outFile.exists()
        }.getOrDefault(false)
    }
}

