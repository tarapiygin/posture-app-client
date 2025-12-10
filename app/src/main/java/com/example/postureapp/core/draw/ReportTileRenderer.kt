package com.example.postureapp.core.draw

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.postureapp.ui.indicators.IndicatorBlue
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.metrics.LevelAngle
import com.example.postureapp.domain.metrics.right.RightMetrics
import com.example.postureapp.ui.indicators.BodyDeviationGeometry
import com.example.postureapp.ui.indicators.computeBodyDeviationGeometry
import com.example.postureapp.ui.indicators.drawBodyAngleText
import com.example.postureapp.ui.indicators.drawBodyAxisLine
import com.example.postureapp.ui.indicators.drawBodyDeviationLine
import com.example.postureapp.ui.indicators.drawLevelGuideLine
import com.example.postureapp.ui.indicators.drawRedAngleLabel
import com.example.postureapp.core.draw.drawLevelSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class FrontLevel { BODY, EARS, SHOULDERS, ASIS, KNEES, FEET }
enum class RightSegment { CVA, KNEE, HIP, SHOULDER, EAR }

object ReportTileRenderer {

    private const val DEFAULT_SIZE = 600

    suspend fun renderFrontPanel(
        imagePath: String,
        metrics: FrontMetrics,
        width: Int = DEFAULT_SIZE,
        height: Int = (DEFAULT_SIZE * 1.4f).toInt()
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val canvasWidth = width
        val canvasHeight = height
        val bitmap = ImageBitmap(canvasWidth, canvasHeight)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            size = androidx.compose.ui.geometry.Size(canvasWidth.toFloat(), canvasHeight.toFloat())
        ) {
            drawFrontImageWithOverlays(source.asImageBitmap(), metrics, canvasWidth.toFloat(), canvasHeight.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    suspend fun renderRightPanel(
        imagePath: String,
        metrics: RightMetrics,
        width: Int = DEFAULT_SIZE,
        height: Int = (DEFAULT_SIZE * 1.4f).toInt()
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val bitmap = ImageBitmap(width, height)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
        ) {
            drawRightImageWithOverlays(source.asImageBitmap(), metrics, width.toFloat(), height.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    suspend fun renderFrontTile(
        level: FrontLevel,
        imagePath: String,
        metrics: FrontMetrics,
        size: Int = DEFAULT_SIZE
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val bitmap = ImageBitmap(size, size)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            size = androidx.compose.ui.geometry.Size(size.toFloat(), size.toFloat())
        ) {
            drawFrontTileContent(source.asImageBitmap(), metrics, level, size.toFloat(), size.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    suspend fun renderRightTile(
        segment: RightSegment,
        imagePath: String,
        metrics: RightMetrics,
        size: Int = DEFAULT_SIZE
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val bitmap = ImageBitmap(size, size)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            size = androidx.compose.ui.geometry.Size(size.toFloat(), size.toFloat())
        ) {
            drawRightTileContent(source.asImageBitmap(), metrics, segment, size.toFloat(), size.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrontImageWithOverlays(
        source: ImageBitmap,
        metrics: FrontMetrics,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val drawnWidth = canvasHeight * (source.width.toFloat() / source.height.toFloat())
        val leftOffset = (canvasWidth - drawnWidth) / 2f
        val scaleX = drawnWidth / source.width.toFloat()
        val scaleY = canvasHeight / source.height.toFloat()

        drawImage(
            image = source,
            dstSize = androidx.compose.ui.unit.IntSize(drawnWidth.toInt(), canvasHeight.toInt()),
            dstOffset = androidx.compose.ui.unit.IntOffset(leftOffset.toInt(), 0)
        )

        val toCanvas: (Offset) -> Offset = { offset ->
            Offset(leftOffset + offset.x * scaleX, offset.y * scaleY)
        }

        val body = computeBodyDeviationGeometry(
            base = toCanvas(metrics.bodyBase),
            jugular = toCanvas(metrics.jnPx)
        )

        drawBodyAxisLine(body.base.x)
        drawBodyDeviationLine(body)
        drawBodyAngleText(body.base, metrics.bodyAngleDeg)

        metrics.levelAngles.forEach { level ->
            val left = toCanvas(level.left)
            val right = toCanvas(level.right)
            val y = (left.y + right.y) / 2f
            drawLevelGuideLine(y = y)
            drawLevelSegment(start = left, end = right, color = IndicatorBlue, strokeWidth = 3.dp)
            level.bodyDeviationDeg?.let { dev ->
                drawRedAngleLabel(y = y, angleDeg = dev, geometry = body)
            }
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRightImageWithOverlays(
        source: ImageBitmap,
        metrics: RightMetrics,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val drawnWidth = canvasHeight * (source.width.toFloat() / source.height.toFloat())
        val leftOffset = (canvasWidth - drawnWidth) / 2f
        val scaleX = drawnWidth / source.width.toFloat()
        val scaleY = canvasHeight / source.height.toFloat()

        drawImage(
            image = source,
            dstSize = androidx.compose.ui.unit.IntSize(drawnWidth.toInt(), canvasHeight.toInt()),
            dstOffset = androidx.compose.ui.unit.IntOffset(leftOffset.toInt(), 0)
        )

        val toCanvas: (Offset) -> Offset = { offset ->
            Offset(leftOffset + offset.x * scaleX, offset.y * scaleY)
        }

        val body = computeBodyDeviationGeometry(
            base = toCanvas(metrics.greenBase),
            jugular = toCanvas(metrics.earPx)
        )

        drawBodyAxisLine(body.base.x)
        drawBodyDeviationLine(body)
        drawBodyAngleText(body.base, metrics.bodyAngleDeg)
        drawLevelGuideLine(y = toCanvas(metrics.earPx).y)

        metrics.segments.forEach { segment ->
            val anchor = toCanvas(segment.anchorPx)
            drawRedAngleLabel(
                y = anchor.y,
                angleDeg = segment.angleDeg,
                geometry = body
            )
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrontTileContent(
        source: ImageBitmap,
        metrics: FrontMetrics,
        level: FrontLevel,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val drawnWidth = canvasHeight * (source.width.toFloat() / source.height.toFloat())
        val leftOffset = (canvasWidth - drawnWidth) / 2f
        val scaleX = drawnWidth / source.width.toFloat()
        val scaleY = canvasHeight / source.height.toFloat()
        drawImage(
            image = source,
            dstSize = androidx.compose.ui.unit.IntSize(drawnWidth.toInt(), canvasHeight.toInt()),
            dstOffset = androidx.compose.ui.unit.IntOffset(leftOffset.toInt(), 0)
        )

        val toCanvas: (Offset) -> Offset = { offset ->
            Offset(leftOffset + offset.x * scaleX, offset.y * scaleY)
        }

        val bodyGeom: BodyDeviationGeometry? = when (level) {
            FrontLevel.BODY -> computeBodyDeviationGeometry(
                base = toCanvas(metrics.bodyBase),
                jugular = toCanvas(metrics.jnPx)
            )
            else -> null
        }

        if (level == FrontLevel.BODY && bodyGeom != null) {
            drawBodyAxisLine(bodyGeom.base.x)
            drawBodyDeviationLine(bodyGeom)
            drawBodyAngleText(bodyGeom.base, metrics.bodyAngleDeg)
        } else {
            val target = metrics.findLevel(level) ?: return
            val left = toCanvas(target.left)
            val right = toCanvas(target.right)
            val y = (left.y + right.y) / 2f
            drawLevelGuideLine(y)
            drawLevelSegment(start = left, end = right, color = IndicatorBlue, strokeWidth = 3.dp)
            val geom = computeBodyDeviationGeometry(
                base = toCanvas(metrics.bodyBase),
                jugular = toCanvas(metrics.jnPx)
            )
            target.bodyDeviationDeg?.let { dev ->
                drawRedAngleLabel(y = y, angleDeg = dev, geometry = geom)
            }
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRightTileContent(
        source: ImageBitmap,
        metrics: RightMetrics,
        segment: RightSegment,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val drawnWidth = canvasHeight * (source.width.toFloat() / source.height.toFloat())
        val leftOffset = (canvasWidth - drawnWidth) / 2f
        val scaleX = drawnWidth / source.width.toFloat()
        val scaleY = canvasHeight / source.height.toFloat()
        drawImage(
            image = source,
            dstSize = androidx.compose.ui.unit.IntSize(drawnWidth.toInt(), canvasHeight.toInt()),
            dstOffset = androidx.compose.ui.unit.IntOffset(leftOffset.toInt(), 0)
        )

        val toCanvas: (Offset) -> Offset = { offset ->
            Offset(leftOffset + offset.x * scaleX, offset.y * scaleY)
        }

        val body = computeBodyDeviationGeometry(
            base = toCanvas(metrics.greenBase),
            jugular = toCanvas(metrics.earPx)
        )

        when (segment) {
            RightSegment.CVA -> {
                val ear = toCanvas(metrics.earPx)
                val c7 = toCanvas(metrics.c7Px)
                drawLevelGuideLine(y = ear.y)
                drawRedAngleLabel(y = ear.y, angleDeg = metrics.cvaDeg, geometry = body)
                drawBodyAxisLine(body.base.x)
                drawBodyDeviationLine(body)
                drawBodyAngleText(body.base, metrics.bodyAngleDeg)
                drawRedAngleLabel(y = c7.y, angleDeg = metrics.bodyAngleDeg, geometry = body)
            }
            RightSegment.KNEE, RightSegment.HIP, RightSegment.SHOULDER, RightSegment.EAR -> {
                val target = metrics.segments.firstOrNull { it.name.equals(segment.name, true) } ?: return
                val anchor = toCanvas(target.anchorPx)
                drawBodyAxisLine(body.base.x)
                drawBodyDeviationLine(body)
                drawRedAngleLabel(y = anchor.y, angleDeg = target.angleDeg, geometry = body)
            }
        }
    }

    private fun FrontMetrics.findLevel(level: FrontLevel): LevelAngle? = when (level) {
        FrontLevel.EARS -> levelAngles.firstOrNull { it.name.equals("Ears", true) }
        FrontLevel.SHOULDERS -> levelAngles.firstOrNull { it.name.equals("Shoulders", true) }
        FrontLevel.ASIS -> levelAngles.firstOrNull { it.name.equals("ASIS", true) }
        FrontLevel.KNEES -> levelAngles.firstOrNull { it.name.equals("Knees", true) }
        FrontLevel.FEET -> levelAngles.firstOrNull { it.name.equals("Feet", true) }
        else -> null
    }
}

