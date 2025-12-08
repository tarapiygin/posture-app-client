package com.example.postureapp.core.graphics

import androidx.compose.ui.geometry.Offset
import kotlin.math.min

class ImageTransform(
    var scale: Float = 1f,
    var tx: Float = 0f,
    var ty: Float = 0f
) {
    var viewportWidth: Float = 1f
    var viewportHeight: Float = 1f
    var contentWidth: Float = 1f
    var contentHeight: Float = 1f

    fun updateBounds(
        viewportWidth: Float,
        viewportHeight: Float,
        contentWidth: Float,
        contentHeight: Float
    ) {
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        this.contentWidth = contentWidth
        this.contentHeight = contentHeight
        clampTranslation()
    }

    fun imageToScreen(image: Offset): Offset {
        val scaledWidth = contentWidth * scale
        val scaledHeight = contentHeight * scale
        val left = (viewportWidth - scaledWidth) / 2f + tx
        val top = (viewportHeight - scaledHeight) / 2f + ty
        return Offset(
            x = left + image.x * scaledWidth,
            y = top + image.y * scaledHeight
        )
    }

    fun screenToImage(screen: Offset): Offset {
        val scaledWidth = contentWidth * scale
        val scaledHeight = contentHeight * scale
        val left = (viewportWidth - scaledWidth) / 2f + tx
        val top = (viewportHeight - scaledHeight) / 2f + ty
        val x = if (scaledWidth == 0f) 0f else (screen.x - left) / scaledWidth
        val y = if (scaledHeight == 0f) 0f else (screen.y - top) / scaledHeight
        return Offset(
            x = x.coerceIn(0f, 1f),
            y = y.coerceIn(0f, 1f)
        )
    }

    fun clampTranslation(minVisibleFraction: Float = 0.05f) {
        val scaledWidth = contentWidth * scale
        val scaledHeight = contentHeight * scale
        tx = clampAxis(tx, viewportWidth, scaledWidth, minVisibleFraction)
        ty = clampAxis(ty, viewportHeight, scaledHeight, minVisibleFraction)
    }

    private fun clampAxis(
        offset: Float,
        viewportExtent: Float,
        contentExtent: Float,
        minVisibleFraction: Float
    ): Float {
        if (contentExtent <= 0f || viewportExtent <= 0f) return 0f
        val visibleMargin = min(viewportExtent * minVisibleFraction, contentExtent / 2f)
        val base = (viewportExtent - contentExtent) / 2f
        val minOffset = (visibleMargin - contentExtent) - base
        val maxOffset = (viewportExtent - visibleMargin) - base
        return offset.coerceIn(minOffset, maxOffset)
    }
}

