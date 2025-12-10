package com.example.postureapp.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.postureapp.R
import com.example.postureapp.core.draw.FrontLevel
import com.example.postureapp.core.draw.ReportTileRenderer
import com.example.postureapp.core.draw.RightSegment
import com.example.postureapp.domain.metrics.FrontMetrics
import com.example.postureapp.domain.metrics.right.RightMetrics
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportPdfBuilder @Inject constructor(
    @ApplicationContext private val context: Context
    ) {

    suspend fun build(
        data: ReportRenderData,
        outFile: File
    ): Result<File> = withContext(Dispatchers.Default) {
        runCatching {
            outFile.parentFile?.mkdirs()
            val doc = PdfDocument()
            val a4Width = 595
            val a4Height = 842

            val frontPanel = data.front?.let {
                ReportTileRenderer.renderFrontPanel(it.imagePath, it.metrics, width = 360, height = 520)
            }
            val rightPanel = data.right?.let {
                ReportTileRenderer.renderRightPanel(it.imagePath, it.metrics, width = 360, height = 520)
            }

            val frontTiles = data.front?.let { front ->
                listOf(
                    FrontLevel.BODY,
                    FrontLevel.EARS,
                    FrontLevel.SHOULDERS,
                    FrontLevel.ASIS,
                    FrontLevel.KNEES,
                    FrontLevel.FEET
                ).mapNotNull { level ->
                    ReportTileRenderer.renderFrontTile(level, front.imagePath, front.metrics, size = 240)
                        ?.let { bmp -> level to bmp }
                }
            }

            val rightTiles = data.right?.let { right ->
                listOf(
                    RightSegment.CVA,
                    RightSegment.KNEE,
                    RightSegment.HIP,
                    RightSegment.SHOULDER,
                    RightSegment.EAR
                ).mapNotNull { segment ->
                    ReportTileRenderer.renderRightTile(segment, right.imagePath, right.metrics, size = 240)
                        ?.let { bmp -> segment to bmp }
                }
            }

            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()).also { page ->
                drawPageHeader(page, "POSTURE REPORT — Quick Analysis", data.createdAt)
                drawFrontOverview(page, frontPanel, data.front?.metrics)
                doc.finishPage(page)
            }

            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()).also { page ->
                drawPageHeader(page, context.getString(R.string.page_front_details), data.createdAt)
                drawFrontTiles(page, frontTiles)
                doc.finishPage(page)
            }

            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 3).create()).also { page ->
                drawPageHeader(page, context.getString(R.string.page_right_overview), data.createdAt)
                drawRightOverview(page, rightPanel, data.right?.metrics)
                doc.finishPage(page)
            }

            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 4).create()).also { page ->
                drawPageHeader(page, context.getString(R.string.page_right_details), data.createdAt)
                drawRightTiles(page, rightTiles)
                doc.finishPage(page)
            }

            FileOutputStream(outFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()
            outFile
        }
    }

    suspend fun renderFrontPanel(
        imagePath: String,
        landmarksFinal: com.example.postureapp.domain.landmarks.LandmarkSet
    ) = ReportTileRenderer.renderFrontPanel(imagePath, computeFrontMetrics(landmarksFinal))

    suspend fun renderRightPanel(
        imagePath: String,
        landmarksFinal: com.example.postureapp.domain.landmarks.LandmarkSet
    ) = ReportTileRenderer.renderRightPanel(imagePath, computeRightMetrics(landmarksFinal))

    suspend fun renderFrontTile(level: FrontLevel, imagePath: String, landmarksFinal: com.example.postureapp.domain.landmarks.LandmarkSet) =
        ReportTileRenderer.renderFrontTile(level, imagePath, computeFrontMetrics(landmarksFinal))

    suspend fun renderRightTile(segment: RightSegment, imagePath: String, landmarksFinal: com.example.postureapp.domain.landmarks.LandmarkSet) =
        ReportTileRenderer.renderRightTile(segment, imagePath, computeRightMetrics(landmarksFinal))

    private fun computeFrontMetrics(set: com.example.postureapp.domain.landmarks.LandmarkSet): FrontMetrics {
        return com.example.postureapp.domain.metrics.ComputeFrontMetricsUseCase().invoke(set)
            ?: throw IllegalArgumentException("Missing front metrics")
    }

    private fun computeRightMetrics(set: com.example.postureapp.domain.landmarks.LandmarkSet): RightMetrics {
        return com.example.postureapp.domain.metrics.right.ComputeRightMetricsUseCase().invoke(set)
            ?: throw IllegalArgumentException("Missing right metrics")
    }

    private fun drawPageHeader(page: PdfDocument.Page, title: String, timestamp: Long) {
        val canvas = page.canvas
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 18f
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
        }
        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
        }
        canvas.drawText(title, 32f, 48f, headerPaint)
        canvas.drawText(formatDate(timestamp), 32f, 68f, subtitlePaint)
    }

    private fun drawFrontOverview(
        page: PdfDocument.Page,
        panel: android.graphics.Bitmap?,
        metrics: FrontMetrics?
    ) {
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sectionPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
        }
        canvas.drawText(context.getString(R.string.page_front_overview), 32f, 110f, sectionPaint)

        if (panel != null) {
            val left = 32f
            val top = 130f
            val targetWidth = 280
            val targetHeight = (panel.height * (targetWidth.toFloat() / panel.width)).toInt()
            val scaled = android.graphics.Bitmap.createScaledBitmap(panel, targetWidth, targetHeight, true)
            canvas.drawBitmap(scaled, left, top, paint)
        }

        val tableLeft = 330f
        drawFrontTable(canvas, tableLeft, 150f, metrics)
    }

    private fun drawFrontTiles(
        page: PdfDocument.Page,
        tiles: List<Pair<FrontLevel, android.graphics.Bitmap>>?
    ) {
        val canvas = page.canvas
        if (tiles.isNullOrEmpty()) {
            drawNotProvided(canvas, 32f, 140f)
            return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val labelPaint = Paint().apply { textSize = 12f; isAntiAlias = true }
        val columns = 2
        val tileWidth = 240
        val gap = 16
        tiles.forEachIndexed { index, pair ->
            val row = index / columns
            val col = index % columns
            val x = 32f + col * (tileWidth + gap)
            val y = 120f + row * (tileWidth + gap + 18)
            val bmp = pair.second
            val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, tileWidth, tileWidth, true)
            canvas.drawBitmap(scaled, x, y, paint)
            canvas.drawText(titleFor(pair.first), x, y + tileWidth + 14, labelPaint)
        }
    }

    private fun drawRightOverview(
        page: PdfDocument.Page,
        panel: android.graphics.Bitmap?,
        metrics: RightMetrics?
    ) {
        val canvas = page.canvas
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val sectionPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
        }
        canvas.drawText(context.getString(R.string.page_right_overview), 32f, 110f, sectionPaint)

        if (panel != null) {
            val left = 32f
            val top = 130f
            val targetWidth = 280
            val targetHeight = (panel.height * (targetWidth.toFloat() / panel.width)).toInt()
            val scaled = android.graphics.Bitmap.createScaledBitmap(panel, targetWidth, targetHeight, true)
            canvas.drawBitmap(scaled, left, top, paint)
        }

        val tableLeft = 330f
        drawRightTable(canvas, tableLeft, 150f, metrics)
    }

    private fun drawRightTiles(
        page: PdfDocument.Page,
        tiles: List<Pair<RightSegment, android.graphics.Bitmap>>?
    ) {
        val canvas = page.canvas
        if (tiles.isNullOrEmpty()) {
            drawNotProvided(canvas, 32f, 140f)
            return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val labelPaint = Paint().apply { textSize = 12f; isAntiAlias = true }
        val columns = 2
        val tileWidth = 240
        val gap = 16
        tiles.forEachIndexed { index, pair ->
            val row = index / columns
            val col = index % columns
            val x = 32f + col * (tileWidth + gap)
            val y = 120f + row * (tileWidth + gap + 18)
            val bmp = pair.second
            val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, tileWidth, tileWidth, true)
            canvas.drawBitmap(scaled, x, y, paint)
            canvas.drawText(titleFor(pair.first), x, y + tileWidth + 14, labelPaint)
        }
    }

    private fun drawFrontTable(canvas: android.graphics.Canvas, left: Float, top: Float, metrics: FrontMetrics?) {
        val headerPaint = Paint().apply { isAntiAlias = true; textSize = 12f; typeface = ResourcesCompat.getFont(context, R.font.inter_semibold) }
        val valuePaint = Paint().apply { isAntiAlias = true; textSize = 12f }
        val rows = listOf(
            "Body deviation" to metrics?.bodyAngleDeg,
            "Ears" to metrics?.levelAngles?.firstOrNull { it.name == "Ears" }?.deviationDeg,
            "Shoulders" to metrics?.levelAngles?.firstOrNull { it.name == "Shoulders" }?.deviationDeg,
            "ASIS" to metrics?.levelAngles?.firstOrNull { it.name == "ASIS" }?.deviationDeg,
            "Knees" to metrics?.levelAngles?.firstOrNull { it.name == "Knees" }?.deviationDeg,
            "Feet" to metrics?.levelAngles?.firstOrNull { it.name == "Feet" }?.deviationDeg
        )
        canvas.drawText(context.getString(R.string.page_front_overview), left, top, headerPaint)
        var y = top + 20f
        rows.forEach { (title, value) ->
            canvas.drawText(title, left, y, valuePaint)
            canvas.drawText(formatValue(value), left + 180f, y, valuePaint)
            y += 20f
        }
    }

    private fun drawRightTable(canvas: android.graphics.Canvas, left: Float, top: Float, metrics: RightMetrics?) {
        val headerPaint = Paint().apply { isAntiAlias = true; textSize = 12f; typeface = ResourcesCompat.getFont(context, R.font.inter_semibold) }
        val valuePaint = Paint().apply { isAntiAlias = true; textSize = 12f }
        val rows = listOf(
            context.getString(R.string.metric_body_dev) to metrics?.bodyAngleDeg,
            "CVA" to metrics?.cvaDeg,
            context.getString(R.string.metric_knee_dev) to metrics?.segments?.firstOrNull { it.name == "Knee" }?.angleDeg,
            context.getString(R.string.metric_hip_dev) to metrics?.segments?.firstOrNull { it.name == "Hip" }?.angleDeg,
            context.getString(R.string.metric_shoulder_dev) to metrics?.segments?.firstOrNull { it.name == "Shoulder" }?.angleDeg,
            context.getString(R.string.metric_ear_dev) to metrics?.segments?.firstOrNull { it.name == "Ear" }?.angleDeg
        )
        canvas.drawText(context.getString(R.string.page_right_overview), left, top, headerPaint)
        var y = top + 20f
        rows.forEach { (title, value) ->
            canvas.drawText(title, left, y, valuePaint)
            canvas.drawText(formatValue(value), left + 180f, y, valuePaint)
            y += 20f
        }
    }

    private fun drawNotProvided(canvas: android.graphics.Canvas, left: Float, top: Float) {
        val paint = Paint().apply { textSize = 14f; isAntiAlias = true }
        canvas.drawText(context.getString(R.string.not_provided), left, top, paint)
    }

    private fun formatValue(value: Float?): String =
        value?.let { String.format("%.1f°", it) } ?: context.getString(R.string.not_available_dash)

    private fun titleFor(level: FrontLevel): String = when (level) {
        FrontLevel.BODY -> context.getString(R.string.metric_body_dev)
        FrontLevel.EARS -> context.getString(R.string.lbl_ears)
        FrontLevel.SHOULDERS -> context.getString(R.string.lbl_shoulders)
        FrontLevel.ASIS -> context.getString(R.string.lbl_asis)
        FrontLevel.KNEES -> context.getString(R.string.lbl_knees)
        FrontLevel.FEET -> context.getString(R.string.lbl_feet)
    }

    private fun titleFor(segment: RightSegment): String = when (segment) {
        RightSegment.CVA -> "CVA"
        RightSegment.KNEE -> context.getString(R.string.metric_knee_dev)
        RightSegment.HIP -> context.getString(R.string.metric_hip_dev)
        RightSegment.SHOULDER -> context.getString(R.string.metric_shoulder_dev)
        RightSegment.EAR -> context.getString(R.string.metric_ear_dev)
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timestamp))
    }
}

