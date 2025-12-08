package com.example.postureapp.ui.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun MagnifierOverlay(
    source: Bitmap,
    centerImageSpace: Offset,
    imageToScreen: (Offset) -> Offset,
    zoom: Float = 3f,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(source) { source.asImageBitmap() }
    Surface(
        modifier = modifier.size(120.dp),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val anchorOrigin = imageToScreen(Offset.Zero)
            val anchorX = imageToScreen(Offset(1f, 0f))
            val anchorY = imageToScreen(Offset(0f, 1f))
            val screenWidth = abs(anchorX.x - anchorOrigin.x).coerceAtLeast(1f)
            val screenHeight = abs(anchorY.y - anchorOrigin.y).coerceAtLeast(1f)
            val pxPerScreenX = source.width / screenWidth
            val pxPerScreenY = source.height / screenHeight
            val srcWidth = (size.width * pxPerScreenX / zoom).roundToInt().coerceIn(4, source.width)
            val srcHeight = (size.height * pxPerScreenY / zoom).roundToInt().coerceIn(4, source.height)
            val centerX = (centerImageSpace.x.coerceIn(0f, 1f) * source.width).roundToInt()
            val centerY = (centerImageSpace.y.coerceIn(0f, 1f) * source.height).roundToInt()
            val left = centerX - srcWidth / 2
            val top = centerY - srcHeight / 2
            val srcOffsetX = left.coerceIn(0, max(source.width - srcWidth, 0))
            val srcOffsetY = top.coerceIn(0, max(source.height - srcHeight, 0))

            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset(srcOffsetX, srcOffsetY),
                srcSize = IntSize(srcWidth, srcHeight),
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            )

            val stroke = 2.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            drawLine(
                color = Color.Red,
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = stroke
            )
            drawLine(
                color = Color.Red,
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = stroke
            )
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(width = stroke),
                size = size
            )
        }
    }
}

