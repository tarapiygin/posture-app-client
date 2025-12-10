package com.example.postureapp.core.draw

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Rect
import com.example.postureapp.domain.landmarks.AnatomicalPoint
import com.example.postureapp.domain.landmarks.LandmarkSet
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.metrics.LevelAngle
import com.example.postureapp.domain.metrics.right.RightMetrics
import com.example.postureapp.ui.indicators.IndicatorBlue
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
    private val Yellow = Color(0xFFFFD60A)

    suspend fun renderFrontPanel(
        imagePath: String,
        metrics: FrontMetrics,
        landmarks: LandmarkSet? = null,
        width: Int = DEFAULT_SIZE,
        height: Int = (DEFAULT_SIZE * 1.4f).toInt()
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val canvasWidth = width
        val canvasHeight = height
        val bitmap = ImageBitmap(canvasWidth, canvasHeight)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(canvasWidth.toFloat(), canvasHeight.toFloat())
        ) {
            drawFrontImageWithOverlays(source.asImageBitmap(), metrics, landmarks, canvasWidth.toFloat(), canvasHeight.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    suspend fun renderRightPanel(
        imagePath: String,
        metrics: RightMetrics,
        landmarks: LandmarkSet? = null,
        width: Int = DEFAULT_SIZE,
        height: Int = (DEFAULT_SIZE * 1.4f).toInt()
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val bitmap = ImageBitmap(width, height)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat())
        ) {
            drawRightImageWithOverlays(source.asImageBitmap(), metrics, landmarks, width.toFloat(), height.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    suspend fun renderFrontTile(
        level: FrontLevel,
        imagePath: String,
        metrics: FrontMetrics,
        landmarks: LandmarkSet? = null,
        size: Int = DEFAULT_SIZE
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val bitmap = ImageBitmap(size, size)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(size.toFloat(), size.toFloat())
        ) {
            drawFrontTileContent(source.asImageBitmap(), metrics, landmarks, level, size.toFloat(), size.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    suspend fun renderRightTile(
        segment: RightSegment,
        imagePath: String,
        metrics: RightMetrics,
        landmarks: LandmarkSet? = null,
        size: Int = DEFAULT_SIZE
    ): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val bitmap = ImageBitmap(size, size)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = androidx.compose.ui.unit.Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = androidx.compose.ui.geometry.Size(size.toFloat(), size.toFloat())
        ) {
            drawRightTileContent(source.asImageBitmap(), metrics, landmarks, segment, size.toFloat(), size.toFloat())
        }
        source.recycle()
        bitmap.asAndroidBitmap()
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrontImageWithOverlays(
        source: ImageBitmap,
        metrics: FrontMetrics,
        landmarks: LandmarkSet?,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val refWidth = landmarks?.imageWidth?.toFloat() ?: source.width.toFloat()
        val refHeight = landmarks?.imageHeight?.toFloat() ?: source.height.toFloat()
        val drawnWidth = canvasHeight * (refWidth / refHeight)
        val leftOffset = (canvasWidth - drawnWidth) / 2f
        val scaleX = drawnWidth / refWidth
        val scaleY = canvasHeight / refHeight
        val toCanvas: (Offset) -> Offset = { offset ->
            Offset(leftOffset + offset.x * scaleX, offset.y * scaleY)
        }

        drawImage(
            image = source,
            dstSize = androidx.compose.ui.unit.IntSize(drawnWidth.toInt(), canvasHeight.toInt()),
            dstOffset = androidx.compose.ui.unit.IntOffset(leftOffset.toInt(), 0)
        )

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
            drawLevelSegment(start = left, end = right, color = Yellow, strokeWidth = 3.dp)
            level.bodyDeviationDeg?.let { dev ->
                drawRedAngleLabel(y = y, angleDeg = dev, geometry = body)
            }
            drawBlueTag(
                text = "${labelFor(level.name)}: ${formatDeg(level.deviationDeg)}",
                x = 12.dp.toPx(),
                y = y
            )
        }

        // draw markers on top of overlays to keep them visible
        drawLandmarks(landmarks, toCanvas)
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRightImageWithOverlays(
        source: ImageBitmap,
        metrics: RightMetrics,
        landmarks: LandmarkSet?,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val refWidth = landmarks?.imageWidth?.toFloat() ?: source.width.toFloat()
        val refHeight = landmarks?.imageHeight?.toFloat() ?: source.height.toFloat()
        val drawnWidth = canvasHeight * (refWidth / refHeight)
        val leftOffset = (canvasWidth - drawnWidth) / 2f
        val scaleX = drawnWidth / refWidth
        val scaleY = canvasHeight / refHeight
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
        // CVA label
        val cvaY = toCanvas(metrics.earPx).y
        drawBlueTag(
            text = "CVA: ${formatDeg(metrics.cvaDeg)}",
            x = 12.dp.toPx(),
            y = cvaY
        )

        metrics.segments.forEach { segment ->
            val anchor = toCanvas(segment.anchorPx)
            drawRedAngleLabel(
                y = anchor.y,
                angleDeg = segment.angleDeg,
                geometry = body
            )
        }

        // draw markers on top of overlays to keep them visible
        drawLandmarks(landmarks, toCanvas)
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrontTileContent(
        source: ImageBitmap,
        metrics: FrontMetrics,
        landmarks: LandmarkSet?,
        level: FrontLevel,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        // 1. Референсная система координат – та же, что у метрик и landmarks
        val refWidth = landmarks?.imageWidth?.toFloat() ?: source.width.toFloat()
        val refHeight = landmarks?.imageHeight?.toFloat() ?: source.height.toFloat()

        // 2. Уровень из метрик (для BODY пусть будет null – обработаем отдельно)
        val targetLevel: LevelAngle? = when (level) {
            FrontLevel.BODY -> null
            else -> metrics.findLevel(level)
        }

        // Если это не BODY и уровень не найден – нарисуем просто картинку, без разметки
        if (level != FrontLevel.BODY && targetLevel == null) {
            drawImage(
                image = source,
                dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
            )
            return
        }

        // 3. Центр и размер окна кропа в ref-координатах
        val (centerXRef, centerYRef, baseWidthRef) = if (level == FrontLevel.BODY) {
            // Для BODY: центр по всему изображению, база – вся ширина
            Triple(
                refWidth / 2f,
                refHeight / 2f,
                refWidth
            )
        } else {
            val t = targetLevel!!
            val cx = (t.left.x + t.right.x) / 2f
            val cy = (t.left.y + t.right.y) / 2f
            val bw = kotlin.math.abs(t.right.x - t.left.x).coerceAtLeast(1f)
            Triple(cx, cy, bw)
        }

        val boxSizeRef = (baseWidthRef * 3f).coerceAtLeast(80f)
        val halfRef = boxSizeRef / 2f

        val maxLeftRef = (refWidth - boxSizeRef).coerceAtLeast(0f)
        val maxTopRef = (refHeight - boxSizeRef).coerceAtLeast(0f)

        val cropLeftRef = (centerXRef - halfRef).coerceIn(0f, maxLeftRef)
        val cropTopRef = (centerYRef - halfRef).coerceIn(0f, maxTopRef)

        val cropWidthRef = boxSizeRef.coerceAtMost(refWidth - cropLeftRef)
        val cropHeightRef = boxSizeRef.coerceAtMost(refHeight - cropTopRef)

        // 4. Перевод ref-кропа в координаты битмапа source
        val sx = source.width.toFloat() / refWidth
        val sy = source.height.toFloat() / refHeight

        val srcLeft = cropLeftRef * sx
        val srcTop = cropTopRef * sy
        val srcWidth = (cropWidthRef * sx).coerceAtMost(source.width - srcLeft).toInt()
        val srcHeight = (cropHeightRef * sy).coerceAtMost(source.height - srcTop).toInt()
        val srcSize = androidx.compose.ui.unit.IntSize(srcWidth, srcHeight)

        // 5. Рисуем вырезанный фрагмент на весь тайл
        drawImage(
            image = source,
            srcOffset = androidx.compose.ui.unit.IntOffset(srcLeft.toInt(), srcTop.toInt()),
            srcSize = srcSize,
            dstSize = androidx.compose.ui.unit.IntSize(canvasWidth.toInt(), canvasHeight.toInt())
        )

        // 6. Проекция ref-координат (метрики и landmarks) в координаты тайла
        val toCanvas: (Offset) -> Offset = { ref ->
            val relX = (ref.x - cropLeftRef) / cropWidthRef
            val relY = (ref.y - cropTopRef) / cropHeightRef
            Offset(
                x = relX * canvasWidth,
                y = relY * canvasHeight
            )
        }

        // 7. BODY-тайл: допускаем красную геометрию корпуса (общий угол), это не «квадраты с углами для сегментов»
        if (level == FrontLevel.BODY) {
            val bodyGeom = computeBodyDeviationGeometry(
                base = toCanvas(metrics.bodyBase),
                jugular = toCanvas(metrics.jnPx)
            )

            // Вертикальная ось + линия отклонения + подпись угла тела – при желании можно тоже убрать
            drawBodyAxisLine(bodyGeom.base.x)
            drawBodyDeviationLine(bodyGeom)
            drawBodyAngleText(bodyGeom.base, metrics.bodyAngleDeg)

            // Лэндмарки в пределах кропа
            drawLandmarks(
                landmarks = landmarks,
                toCanvas = toCanvas,
                bounds = Rect(
                    left = cropLeftRef,
                    top = cropTopRef,
                    right = cropLeftRef + cropWidthRef,
                    bottom = cropTopRef + cropHeightRef
                ),
                filterPoints = null
            )
            return
        }

        // 8. Остальные уровни (уши, плечи, ASIS, колени, стопы):
        // только жёлтая линия уровня + горизонталь + точки; БЕЗ красных углов и квадратов

        val t = targetLevel!!
        val leftRef = t.left
        val rightRef = t.right

        val left = toCanvas(leftRef)
        val right = toCanvas(rightRef)
        val segmentY = (left.y + right.y) / 2f

        // горизонтальная направляющая
        drawLevelGuideLine(y = segmentY)
        // жёлтый сегмент
        drawLevelSegment(
            start = left,
            end = right,
            color = Yellow,
            strokeWidth = 3.dp
        )

        // Никаких drawRedAngleLabel / drawBlueTag для кроп-тайлов!
        // Просто рисуем маркеры соответствующих точек
        val relevantPoints = levelToPoints(level)
        drawLandmarks(
            landmarks = landmarks,
            toCanvas = toCanvas,
            bounds = Rect(
                left = cropLeftRef,
                top = cropTopRef,
                right = cropLeftRef + cropWidthRef,
                bottom = cropTopRef + cropHeightRef
            ),
            filterPoints = relevantPoints
        )
    }


    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRightTileContent(
        source: ImageBitmap,
        metrics: RightMetrics,
        landmarks: LandmarkSet?,
        segment: RightSegment,
        canvasWidth: Float,
        canvasHeight: Float
    ) {
        val refWidth = landmarks?.imageWidth?.toFloat() ?: source.width.toFloat()
        val refHeight = landmarks?.imageHeight?.toFloat() ?: source.height.toFloat()
        val drawnWidth = canvasHeight * (refWidth / refHeight)
        val leftOffset = (canvasWidth - drawnWidth) / 2f
        val scaleX = drawnWidth / refWidth
        val scaleY = canvasHeight / refHeight
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
                drawBlueTag(
                    text = "CVA: ${formatDeg(metrics.cvaDeg)}",
                    x = 12.dp.toPx(),
                    y = ear.y
                )
            }
            RightSegment.KNEE, RightSegment.HIP, RightSegment.SHOULDER, RightSegment.EAR -> {
                val target = metrics.segments.firstOrNull { it.name.equals(segment.name, true) } ?: return
                val anchor = toCanvas(target.anchorPx)
                drawBodyAxisLine(body.base.x)
                drawBodyDeviationLine(body)
                drawRedAngleLabel(y = anchor.y, angleDeg = target.angleDeg, geometry = body)
            }
        }

        // Markers after lines for better visibility - only show relevant landmarks for this segment
        val relevantPoints = segmentToPoints(segment)
        drawLandmarks(landmarks, toCanvas, filterPoints = relevantPoints)
    }

    private fun FrontMetrics.findLevel(level: FrontLevel): LevelAngle? = when (level) {
        FrontLevel.EARS -> levelAngles.firstOrNull { it.name.equals("Ears", true) }
        FrontLevel.SHOULDERS -> levelAngles.firstOrNull { it.name.equals("Shoulders", true) }
        FrontLevel.ASIS -> levelAngles.firstOrNull { it.name.equals("ASIS", true) }
        FrontLevel.KNEES -> levelAngles.firstOrNull { it.name.equals("Knees", true) }
        FrontLevel.FEET -> levelAngles.firstOrNull { it.name.equals("Feet", true) }
        else -> null
    }

    private fun levelToPoints(level: FrontLevel): Set<AnatomicalPoint> = when (level) {
        FrontLevel.BODY -> emptySet()
        FrontLevel.EARS -> setOf(AnatomicalPoint.LEFT_EAR, AnatomicalPoint.RIGHT_EAR)
        FrontLevel.SHOULDERS -> setOf(AnatomicalPoint.LEFT_SHOULDER, AnatomicalPoint.RIGHT_SHOULDER)
        FrontLevel.ASIS -> setOf(AnatomicalPoint.LEFT_HIP, AnatomicalPoint.RIGHT_HIP)
        FrontLevel.KNEES -> setOf(AnatomicalPoint.LEFT_KNEE, AnatomicalPoint.RIGHT_KNEE, AnatomicalPoint.TIBIAL_TUBEROSITY_LEFT, AnatomicalPoint.TIBIAL_TUBEROSITY_RIGHT)
        FrontLevel.FEET -> setOf(AnatomicalPoint.LEFT_ANKLE, AnatomicalPoint.RIGHT_ANKLE)
    }

    private fun segmentToPoints(segment: RightSegment): Set<AnatomicalPoint> = when (segment) {
        RightSegment.CVA -> setOf(AnatomicalPoint.RIGHT_EAR, AnatomicalPoint.RIGHT_C7)
        RightSegment.KNEE -> setOf(AnatomicalPoint.RIGHT_KNEE)
        RightSegment.HIP -> setOf(AnatomicalPoint.RIGHT_HIP)
        RightSegment.SHOULDER -> setOf(AnatomicalPoint.RIGHT_SHOULDER)
        RightSegment.EAR -> setOf(AnatomicalPoint.RIGHT_EAR)
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLandmarks(
        landmarks: LandmarkSet?,
        toCanvas: (Offset) -> Offset,
        bounds: Rect? = null,
        filterPoints: Set<AnatomicalPoint>? = null
    ) {
        if (landmarks == null) return
        if (landmarks.points.isEmpty()) return
        val strokeWidth = 2.dp.toPx()
        val radius = 5.dp.toPx()
        // Landmarks are stored in NORMALIZED coordinates (0..1)
        // Metrics use landmarks.imageWidth/Height as the coordinate system
        // So we convert normalized -> pixels in landmarks image coordinate system
        val imgW = landmarks.imageWidth.toFloat()
        val imgH = landmarks.imageHeight.toFloat()
        val pointsToRender = if (filterPoints != null) {
            landmarks.points.filter { it.point in filterPoints }
        } else {
            landmarks.points
        }
        pointsToRender.forEach { lm ->
            // Convert normalized coords to pixel coords (same system as metrics)
            val pixelX = lm.x * imgW
            val pixelY = lm.y * imgH
            if (!pixelX.isFinite() || !pixelY.isFinite()) return@forEach
            if (pixelX < 0 || pixelY < 0 || pixelX > imgW || pixelY > imgH) return@forEach
            if (bounds != null && (pixelX < bounds.left || pixelX > bounds.right || pixelY < bounds.top || pixelY > bounds.bottom)) {
                return@forEach
            }
            // Then project to canvas coords (toCanvas expects pixel coords)
            val center = toCanvas(Offset(pixelX, pixelY))
            // outer stroke for contrast
            drawCircle(color = Color.White, radius = radius + strokeWidth, center = center)
            // inner fill
            drawCircle(color = IndicatorBlue.copy(alpha = 0.95f), radius = radius, center = center)
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlueTag(
        text: String,
        x: Float,
        y: Float
    ) {
        val leftMarginPx = 12.dp.toPx()
        val paddingX = 6.dp.toPx()
        val paddingY = 4.dp.toPx()
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 12.dp.toPx()
            color = android.graphics.Color.BLACK
        }
        val textWidth = paint.measureText(text)
        val textHeight = paint.fontMetrics.let { it.descent - it.ascent }
        val rectWidth = textWidth + paddingX * 2
        val rectHeight = textHeight + paddingY * 2
        val top = y - rectHeight / 2f
        val rectLeft = x
        drawRect(
            color = androidx.compose.ui.graphics.Color.White,
            topLeft = Offset(rectLeft, top),
            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight)
        )
        drawRect(
            color = IndicatorBlue,
            topLeft = Offset(rectLeft, top),
            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        drawContext.canvas.nativeCanvas.drawText(
            text,
            rectLeft + paddingX,
            top + paddingY + textHeight * 0.8f,
            paint
        )
    }

    private fun labelFor(name: String): String = when (name) {
        "Ears" -> "Ears"
        "Shoulders" -> "Shoulders"
        "ASIS" -> "ASIS"
        "Knees" -> "Knees"
        "Feet" -> "Feet"
        else -> name
    }

    private fun formatDeg(value: Float): String = String.format("%.1f°", value)
}

