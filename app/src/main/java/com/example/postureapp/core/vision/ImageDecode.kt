package com.example.postureapp.core.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun decodeBitmap(path: String, maxSide: Int = 2048): Bitmap = withContext(Dispatchers.IO) {
    val file = File(path)
    require(file.exists()) { "Bitmap source not found at $path" }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(file)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            decoder.setTargetSampleSize(
                calculateSampleSize(info.size.width, info.size.height, maxSide)
            )
        }
    } else {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, boundsOptions)

        val decodeOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, maxSide)
        }

        BitmapFactory.decodeFile(path, decodeOptions)
            ?: throw IllegalArgumentException("Cannot decode bitmap at $path")
    }
}

private fun calculateSampleSize(width: Int, height: Int, maxSide: Int): Int {
    if (width <= 0 || height <= 0) return 1
    val largestSide = maxOf(width, height)
    if (largestSide <= maxSide) return 1
    val ratio = largestSide.toFloat() / maxSide.toFloat()
    var sampleSize = 1
    while (sampleSize * 2 <= ratio) {
        sampleSize *= 2
    }
    return sampleSize.coerceAtLeast(1)
}

class ImageDecoderFacade @Inject constructor() {
    suspend fun decode(path: String, maxSide: Int = 2048): Bitmap = decodeBitmap(path, maxSide)
}













