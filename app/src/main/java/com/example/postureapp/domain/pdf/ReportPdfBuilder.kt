package com.example.postureapp.domain.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.example.postureapp.R
import com.example.postureapp.core.reports.FrontLevel
import com.example.postureapp.core.reports.ReportTileRenderer
import com.example.postureapp.core.reports.RightSegment
import com.example.postureapp.domain.analysis.front.FrontMetrics
import com.example.postureapp.domain.analysis.right.RightMetrics
import android.graphics.BitmapFactory
import com.example.postureapp.domain.analysis.front.ComputeFrontMetricsUseCase
import com.example.postureapp.domain.analysis.landmarks.LandmarkSet
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

            // Render panels at a decent native width, keeping aspect; draw time will scale down uniformly.
            val frontPanelMaxWidth = 800
            val rightPanelMaxWidth = 800
            val frontPanel = data.front?.let {
                val (w, h) = decodeBounds(it.imagePath)
                val targetWidth = if (w > 0) kotlin.math.min(w, frontPanelMaxWidth) else frontPanelMaxWidth
                val targetHeight = if (w > 0 && h > 0) (h.toFloat() / w * targetWidth).toInt().coerceAtLeast(1) else 520
                ReportTileRenderer.renderFrontPanel(it.imagePath, it.metrics, landmarks = it.landmarks, width = targetWidth, height = targetHeight)
            }
            val rightPanel = data.right?.let {
                val (w, h) = decodeBounds(it.imagePath)
                val targetWidth = if (w > 0) kotlin.math.min(w, rightPanelMaxWidth) else rightPanelMaxWidth
                val targetHeight = if (w > 0 && h > 0) (h.toFloat() / w * targetWidth).toInt().coerceAtLeast(1) else 520
                ReportTileRenderer.renderRightPanel(it.imagePath, it.metrics, landmarks = it.landmarks, width = targetWidth, height = targetHeight)
            }

            val frontTiles = data.front?.let { front ->
                listOf(
                    FrontLevel.EARS,
                    FrontLevel.SHOULDERS,
                    FrontLevel.ASIS,
                    FrontLevel.KNEES,
                    FrontLevel.FEET
                ).mapNotNull { level ->
                    ReportTileRenderer.renderFrontTile(level, front.imagePath, front.metrics, landmarks = front.landmarks, size = 200)
                        ?.let { bmp -> level to bmp }
                }
            }

//            val rightTiles = data.right?.let { right ->
//                listOf(
//                    RightSegment.CVA,
//                    RightSegment.KNEE,
//                    RightSegment.HIP,
//                    RightSegment.SHOULDER,
//                    RightSegment.EAR
//                ).mapNotNull { segment ->
//                    ReportTileRenderer.renderRightTile(segment, right.imagePath, right.metrics, landmarks = right.landmarks, size = 240)
//                        ?.let { bmp -> segment to bmp }
//                }
//            }

            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 1).create()).also { page ->
                page.canvas.drawColor(android.graphics.Color.WHITE)
                drawPageHeader(page, context.getString(R.string.pdf_report_header), data.createdAt)
                drawFrontOverview(page, frontPanel, data.front?.metrics)
                doc.finishPage(page)
            }

            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 2).create()).also { page ->
                page.canvas.drawColor(android.graphics.Color.WHITE)
                drawPageHeader(page, context.getString(R.string.page_front_details), data.createdAt)
                drawFrontTiles(page, frontTiles, data.front?.metrics)
                doc.finishPage(page)
            }

            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 3).create()).also { page ->
                page.canvas.drawColor(android.graphics.Color.WHITE)
                drawPageHeader(page, context.getString(R.string.page_right_overview), data.createdAt)
                drawRightOverview(page, rightPanel, data.right?.metrics)
                doc.finishPage(page)
            }

//            doc.startPage(PdfDocument.PageInfo.Builder(a4Width, a4Height, 4).create()).also { page ->
//                page.canvas.drawColor(android.graphics.Color.WHITE)
//                drawPageHeader(page, context.getString(R.string.page_right_details), data.createdAt)
//                drawRightTiles(page, rightTiles, data.right?.metrics)
//                doc.finishPage(page)
//            }

            FileOutputStream(outFile).use { out ->
                doc.writeTo(out)
            }
            doc.close()
            outFile
        }
    }

    suspend fun renderFrontPanel(
        imagePath: String,
        landmarksFinal: LandmarkSet
    ) = ReportTileRenderer.renderFrontPanel(imagePath, computeFrontMetrics(landmarksFinal))

    suspend fun renderRightPanel(
        imagePath: String,
        landmarksFinal: LandmarkSet
    ) = ReportTileRenderer.renderRightPanel(imagePath, computeRightMetrics(landmarksFinal))

    suspend fun renderFrontTile(level: FrontLevel, imagePath: String, landmarksFinal: LandmarkSet) =
        ReportTileRenderer.renderFrontTile(level, imagePath, computeFrontMetrics(landmarksFinal))

//    suspend fun renderRightTile(segment: RightSegment, imagePath: String, landmarksFinal: com.example.postureapp.domain.landmarks.LandmarkSet) =
//        ReportTileRenderer.renderRightTile(segment, imagePath, computeRightMetrics(landmarksFinal))

    private fun computeFrontMetrics(set: LandmarkSet): FrontMetrics {
        return ComputeFrontMetricsUseCase().invoke(set)
            ?: throw IllegalArgumentException("Missing front metrics")
    }

    private fun computeRightMetrics(set: LandmarkSet): RightMetrics {
        return com.example.postureapp.domain.analysis.right.ComputeRightMetricsUseCase().invoke(set)
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
        val sectionPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
        }
        canvas.drawText(context.getString(R.string.page_front_overview), 32f, 110f, sectionPaint)

        val imageLeft = 32f
        val imageTop = 130f
        val textLeft = 330f
        val textTop = imageTop
        val termsWidth = canvas.width.toFloat() - textLeft - 24f

        val rowsCount = 6 // ears, shoulders, ASIS, knees, feet, body deviation
        val tableHeight = computeFrontTableHeight(rowsCount)
        val termsHeight = measureFrontTermsHeight(termsWidth)
        val bottomMargin = 40f

        val availableHeight = canvas.height.toFloat() - bottomMargin - imageTop - termsHeight - 12f - tableHeight
        val maxImageHeight = availableHeight.coerceAtLeast(120f)

        var imageBottom = imageTop
        if (panel != null) {
            val scale = kotlin.math.min(maxImageHeight / panel.height.toFloat(), 1f)
            val targetWidth = (panel.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (panel.height * scale).toInt().coerceAtLeast(1)
            val destRect = android.graphics.Rect(
                imageLeft.toInt(),
                imageTop.toInt(),
                imageLeft.toInt() + targetWidth,
                imageTop.toInt() + targetHeight
            )
            canvas.drawBitmap(panel, null, destRect, null)
            imageBottom = destRect.bottom.toFloat()
        }

        drawFrontTerms(canvas, textLeft, textTop, termsWidth)

        val tableTop = imageBottom + 12f
        drawFrontTable(canvas, imageLeft, tableTop, metrics, canvas.width.toFloat())
    }

    private fun drawFrontTiles(
        page: PdfDocument.Page,
        tiles: List<Pair<FrontLevel, android.graphics.Bitmap>>?,
        metrics: FrontMetrics?
    ) {
        val canvas = page.canvas
        if (tiles.isNullOrEmpty()) {
            drawNotProvided(canvas, 32f, 140f)
            return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val labelPaint = Paint().apply { textSize = 12f; isAntiAlias = true }
        val columns = 2
        val tileWidth = 200
        val gap = 12
        tiles.forEachIndexed { index, pair ->
            val row = index / columns
            val col = index % columns
            val x = 32f + col * (tileWidth + gap)
            val y = 120f + row * (tileWidth + gap + 18)
            val bmp = pair.second
            val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, tileWidth, tileWidth, true)
            canvas.drawBitmap(scaled, x, y, paint)
            val value = frontValue(pair.first, metrics)
            val valueText = value?.let { String.format("%.1f°", it) } ?: context.getString(R.string.not_available_dash)
            canvas.drawText("${titleFor(pair.first)}: $valueText", x, y + tileWidth + 14, labelPaint)
        }
    }

    private fun drawRightOverview(
        page: PdfDocument.Page,
        panel: android.graphics.Bitmap?,
        metrics: RightMetrics?
    ) {
        val canvas = page.canvas
        val sectionPaint = Paint().apply {
            textSize = 14f
            isAntiAlias = true
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
        }
        canvas.drawText(context.getString(R.string.page_right_overview), 32f, 110f, sectionPaint)

        if (panel != null) {
            val left = 32
            val top = 130
            val targetWidth = 280
            val targetHeight = (panel.height.toFloat() / panel.width.toFloat() * targetWidth).toInt().coerceAtLeast(1)
            val dest = android.graphics.Rect(left, top, left + targetWidth, top + targetHeight)
            canvas.drawBitmap(panel, null, dest, null)
        }

        val tableLeft = 330f
        drawRightTableStyled(canvas, tableLeft, 150f, metrics)
    }

    private fun drawRightTiles(
        page: PdfDocument.Page,
        tiles: List<Pair<RightSegment, android.graphics.Bitmap>>?,
        metrics: RightMetrics?
    ) {
        val canvas = page.canvas
        if (tiles.isNullOrEmpty()) {
            drawNotProvided(canvas, 32f, 140f)
            return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val labelPaint = Paint().apply { textSize = 12f; isAntiAlias = true }
        val columns = 2
        val gap = 16
        val labelHeight = 18
        tiles.forEachIndexed { index, pair ->
            val row = index / columns
            val col = index % columns
            val bmp = pair.second
            val tileW = bmp.width.toFloat()
            val tileH = bmp.height.toFloat()
            val x = 32f + col * (tileW + gap)
            val y = 120f + row * (tileH + gap + labelHeight)
            canvas.drawBitmap(bmp, x, y, paint)
            val value = rightValue(pair.first, metrics)
            val valueText = value?.let { String.format("%.1f°", it) } ?: context.getString(R.string.not_available_dash)
            canvas.drawText("${titleFor(pair.first)}: $valueText", x, y + tileH + 14, labelPaint)
        }
    }

    private fun drawFrontTable(canvas: android.graphics.Canvas, left: Float, top: Float, metrics: FrontMetrics?, canvasWidth: Float) {
        // Medical-style color palette
        val colorHeaderBg = android.graphics.Color.parseColor("#E3EDF7")      // Soft blue header
        val colorHeaderText = android.graphics.Color.parseColor("#3D5A80")    // Dark blue-gray
        val colorEvenRowBg = android.graphics.Color.parseColor("#F8FAFB")     // Very light gray
        val colorOddRowBg = android.graphics.Color.WHITE
        val colorGridLine = android.graphics.Color.parseColor("#D1DBE5")      // Light blue-gray grid
        val colorNormalValue = android.graphics.Color.parseColor("#4A5568")   // Neutral gray
        val colorAlertValue = android.graphics.Color.parseColor("#C53030")    // Medical red for deviations
        val deviationThreshold = 3.0f  // frontal tilt threshold
        val bodyDeviationThreshold = 4.0f // lateral/body deviation threshold
        val availableWidth = canvasWidth - left - 24f

        // Paints
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
            color = colorHeaderText
        }
        val segmentPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = ResourcesCompat.getFont(context, R.font.inter_medium)
            color = colorNormalValue
        }
        val valuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
        }
        val gridPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = colorGridLine
        }
        val fillPaint = Paint().apply { isAntiAlias = true }

        // Table dimensions
        // Measure columns based on content, then scale if needed
        val padding = 6f
        val headerSegmentWidth = headerPaint.measureText(context.getString(R.string.table_header_segment))
        val headerFrontalWidth = headerPaint.measureText(context.getString(R.string.table_header_frontal_tilt))
        val headerLateralWidth = headerPaint.measureText(context.getString(R.string.table_header_lateral_shift))

        data class RowData(val segment: String, val frontalTilt: Float?, val lateralShift: Float?)
        val rowHeight = 20f
        val headerHeight = 22f

        val rows = listOf(
            RowData(context.getString(R.string.lbl_ears), 
                metrics?.levelAngles?.firstOrNull { it.name == "Ears" }?.deviationDeg,
                metrics?.levelAngles?.firstOrNull { it.name == "Ears" }?.bodyDeviationDeg),
            RowData(context.getString(R.string.lbl_shoulders),
                metrics?.levelAngles?.firstOrNull { it.name == "Shoulders" }?.deviationDeg,
                metrics?.levelAngles?.firstOrNull { it.name == "Shoulders" }?.bodyDeviationDeg),
            RowData(context.getString(R.string.lbl_asis),
                metrics?.levelAngles?.firstOrNull { it.name == "ASIS" }?.deviationDeg,
                metrics?.levelAngles?.firstOrNull { it.name == "ASIS" }?.bodyDeviationDeg),
            RowData(context.getString(R.string.lbl_knees),
                metrics?.levelAngles?.firstOrNull { it.name == "Knees" }?.deviationDeg,
                metrics?.levelAngles?.firstOrNull { it.name == "Knees" }?.bodyDeviationDeg),
            RowData(context.getString(R.string.lbl_feet),
                metrics?.levelAngles?.firstOrNull { it.name == "Feet" }?.deviationDeg,
                metrics?.levelAngles?.firstOrNull { it.name == "Feet" }?.bodyDeviationDeg),
            RowData(context.getString(R.string.metric_body_dev),
                null,
                metrics?.bodyAngleDeg)
        )

        val maxSegmentWidth = (rows.maxOfOrNull { segmentPaint.measureText(it.segment) } ?: 0f).coerceAtLeast(headerSegmentWidth)
        val maxFrontalWidth = (rows.maxOfOrNull { valuePaint.measureText(it.frontalTilt?.let { v -> String.format("%.1f°", v) } ?: context.getString(R.string.not_available_dash)) } ?: 0f)
            .coerceAtLeast(headerFrontalWidth)
        val maxLateralWidth = (rows.maxOfOrNull { valuePaint.measureText(it.lateralShift?.let { v -> String.format("%.1f°", v) } ?: context.getString(R.string.not_available_dash)) } ?: 0f)
            .coerceAtLeast(headerLateralWidth)

        var colSegment = maxSegmentWidth + padding * 2
        var colFrontal = maxFrontalWidth + padding * 2
        var colLateral = maxLateralWidth + padding * 2

        val tableWidthUnscaled = colSegment + colFrontal + colLateral
        if (tableWidthUnscaled > availableWidth) {
            val scale = availableWidth / tableWidthUnscaled
            colSegment *= scale
            colFrontal *= scale
            colLateral *= scale
        }
        val tableWidth = colSegment + colFrontal + colLateral

        var y = top

        // Draw header row background
        fillPaint.color = colorHeaderBg
        canvas.drawRect(left, y, left + tableWidth, y + headerHeight, fillPaint)

        // Draw header text
        canvas.drawText(context.getString(R.string.table_header_segment), left + padding, y + 15f, headerPaint)
        canvas.drawText(context.getString(R.string.table_header_frontal_tilt), left + colSegment + padding, y + 15f, headerPaint)
        canvas.drawText(context.getString(R.string.table_header_lateral_shift), left + colSegment + colFrontal + padding, y + 15f, headerPaint)

        y += headerHeight

        // Draw data rows
        rows.forEachIndexed { index, row ->
            val rowTop = y
            val rowBottom = y + rowHeight

            // Zebra striping
            fillPaint.color = if (index % 2 == 0) colorOddRowBg else colorEvenRowBg
            canvas.drawRect(left, rowTop, left + tableWidth, rowBottom, fillPaint)

            // Segment name with wrapping if needed
            val segMaxWidth = colSegment - padding * 2
            val segLines = wrapText(row.segment, segmentPaint, segMaxWidth)
            var segY = rowTop + 14f
            segLines.forEach { line ->
                canvas.drawText(line, left + padding, segY, segmentPaint)
                segY += 12f
            }

            // Frontal tilt value
            val frontalText = row.frontalTilt?.let { String.format("%.1f°", it) } ?: context.getString(R.string.not_available_dash)
            valuePaint.color = if (row.frontalTilt != null && kotlin.math.abs(row.frontalTilt) > deviationThreshold) colorAlertValue else colorNormalValue
            valuePaint.typeface = if (row.frontalTilt != null && kotlin.math.abs(row.frontalTilt) > deviationThreshold)
                ResourcesCompat.getFont(context, R.font.inter_semibold) else ResourcesCompat.getFont(context, R.font.inter_regular)
            canvas.drawText(frontalText, left + colSegment + padding, rowTop + 14f, valuePaint)

            // Lateral shift value
            val lateralText = row.lateralShift?.let { String.format("%.1f°", it) } ?: context.getString(R.string.not_available_dash)
            valuePaint.color = if (row.lateralShift != null && kotlin.math.abs(row.lateralShift) > bodyDeviationThreshold) colorAlertValue else colorNormalValue
            valuePaint.typeface = if (row.lateralShift != null && kotlin.math.abs(row.lateralShift) > bodyDeviationThreshold)
                ResourcesCompat.getFont(context, R.font.inter_semibold) else ResourcesCompat.getFont(context, R.font.inter_regular)
            canvas.drawText(lateralText, left + colSegment + colFrontal + padding, rowTop + 14f, valuePaint)

            // Row bottom border
            canvas.drawLine(left, rowBottom, left + tableWidth, rowBottom, gridPaint)

            y = rowBottom
        }

        // Draw vertical grid lines
        canvas.drawLine(left, top, left, y, gridPaint)
        canvas.drawLine(left + colSegment, top, left + colSegment, y, gridPaint)
        canvas.drawLine(left + colSegment + colFrontal, top, left + colSegment + colFrontal, y, gridPaint)
        canvas.drawLine(left + tableWidth, top, left + tableWidth, y, gridPaint)

        // Draw outer border (slightly thicker)
        gridPaint.strokeWidth = 1f
        canvas.drawRect(left, top, left + tableWidth, y, gridPaint)

        // Header bottom border after we know final y
        gridPaint.strokeWidth = 0.5f
        canvas.drawLine(left, top + headerHeight, left + tableWidth, top + headerHeight, gridPaint)
    }

    private fun drawFrontTerms(canvas: android.graphics.Canvas, left: Float, top: Float, maxWidth: Float) {
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
            color = android.graphics.Color.parseColor("#3D5A80")
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
            color = android.graphics.Color.parseColor("#4A5568")
        }
        val lineHeight = 14f
        var y = top
        canvas.drawText(context.getString(R.string.terms_title), left, y, titlePaint)
        y += lineHeight + 4f

        fun drawWrapped(text: String) {
            wrapText(text, bodyPaint, maxWidth).forEach { line ->
                canvas.drawText(line, left, y, bodyPaint)
                y += lineHeight
            }
            y += 2f
        }

        drawWrapped(context.getString(R.string.terms_frontal_tilt))
        drawWrapped(context.getString(R.string.terms_lateral_shift))
        drawWrapped(context.getString(R.string.terms_body_deviation))
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = candidate
            } else {
                lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    private fun measureFrontTermsHeight(maxWidth: Float): Float {
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
        }
        val lineHeight = 14f
        var height = 0f
        height += lineHeight // title
        height += 4f
        fun addLines(text: String) {
            val lines = wrapText(text, bodyPaint, maxWidth)
            height += lines.size * lineHeight
            height += 2f
        }
        addLines(context.getString(R.string.terms_frontal_tilt))
        addLines(context.getString(R.string.terms_lateral_shift))
        addLines(context.getString(R.string.terms_body_deviation))
        return height
    }

    private fun computeFrontTableHeight(rowsCount: Int): Float {
        val headerHeight = 22f
        val rowHeight = 20f
        return headerHeight + rowsCount * rowHeight
    }

    private fun drawRightTableStyled(canvas: android.graphics.Canvas, left: Float, top: Float, metrics: RightMetrics?) {
        val colorHeaderBg = android.graphics.Color.parseColor("#E3EDF7")
        val colorHeaderText = android.graphics.Color.parseColor("#3D5A80")
        val colorEvenRowBg = android.graphics.Color.parseColor("#F8FAFB")
        val colorOddRowBg = android.graphics.Color.WHITE
        val colorGridLine = android.graphics.Color.parseColor("#D1DBE5")
        val colorNormalValue = android.graphics.Color.parseColor("#4A5568")
        val colorAlertValue = android.graphics.Color.parseColor("#C53030")
        val deviationThreshold = 3.0f
        val bodyDeviationThreshold = 4.0f

        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 9f
            typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
            color = colorHeaderText
        }
        val segmentPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = ResourcesCompat.getFont(context, R.font.inter_medium)
            color = colorNormalValue
        }
        val valuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = ResourcesCompat.getFont(context, R.font.inter_regular)
        }
        val gridPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = colorGridLine
        }
        val fillPaint = Paint().apply { isAntiAlias = true }

        val padding = 6f
        val headerSegmentWidth = headerPaint.measureText(context.getString(R.string.table_header_segment))
        val headerDevWidth = headerPaint.measureText(context.getString(R.string.table_header_deviation))

        data class Row(val segment: String, val value: Float?, val isBody: Boolean = false)
        val rows = listOf(
            Row(context.getString(R.string.label_cva), metrics?.cvaDeg),
            Row(context.getString(R.string.metric_knee_dev), metrics?.segments?.firstOrNull { it.name == "Knee" }?.angleDeg),
            Row(context.getString(R.string.metric_hip_dev), metrics?.segments?.firstOrNull { it.name == "Hip" }?.angleDeg),
            Row(context.getString(R.string.metric_shoulder_dev), metrics?.segments?.firstOrNull { it.name == "Shoulder" }?.angleDeg),
            Row(context.getString(R.string.metric_ear_dev), metrics?.segments?.firstOrNull { it.name == "Ear" }?.angleDeg),
            Row(context.getString(R.string.metric_body_dev), metrics?.bodyAngleDeg, isBody = true)
        )

        val rowHeight = 20f
        val headerHeight = 22f

        val maxSegmentWidth = (rows.maxOfOrNull { segmentPaint.measureText(it.segment) } ?: 0f).coerceAtLeast(headerSegmentWidth)
        val maxValueWidth = (rows.maxOfOrNull { valuePaint.measureText(it.value?.let { v -> String.format("%.1f°", v) } ?: context.getString(R.string.not_available_dash)) } ?: 0f)
            .coerceAtLeast(headerDevWidth)

        val colSegment = maxSegmentWidth + padding * 2
        val colValue = maxValueWidth + padding * 2
        val tableWidth = colSegment + colValue

        var y = top

        // Header
        fillPaint.color = colorHeaderBg
        canvas.drawRect(left, y, left + tableWidth, y + headerHeight, fillPaint)
        canvas.drawText(context.getString(R.string.table_header_segment), left + padding, y + 15f, headerPaint)
        canvas.drawText(context.getString(R.string.table_header_deviation), left + colSegment + padding, y + 15f, headerPaint)
        y += headerHeight

        rows.forEachIndexed { index, row ->
            val rowTop = y
            val rowBottom = y + rowHeight
            fillPaint.color = if (index % 2 == 0) colorOddRowBg else colorEvenRowBg
            canvas.drawRect(left, rowTop, left + tableWidth, rowBottom, fillPaint)

            canvas.drawText(row.segment, left + padding, rowTop + 14f, segmentPaint)

            val valueText = row.value?.let { String.format("%.1f°", it) } ?: context.getString(R.string.not_available_dash)
            val isCva = row.segment == context.getString(R.string.label_cva)
            val threshold = when {
                row.isBody -> bodyDeviationThreshold
                isCva -> 48f // CVA alert if less than 48°
                else -> deviationThreshold
            }
            val isAlert = row.value != null && (
                if (isCva) row.value < threshold else kotlin.math.abs(row.value) > threshold
            )
            valuePaint.color = if (isAlert) colorAlertValue else colorNormalValue
            valuePaint.typeface = if (isAlert) ResourcesCompat.getFont(context, R.font.inter_semibold) else ResourcesCompat.getFont(context, R.font.inter_regular)
            canvas.drawText(valueText, left + colSegment + padding, rowTop + 14f, valuePaint)

            canvas.drawLine(left, rowBottom, left + tableWidth, rowBottom, gridPaint)
            y = rowBottom
        }

        // Grid
        canvas.drawLine(left, top, left, y, gridPaint)
        canvas.drawLine(left + colSegment, top, left + colSegment, y, gridPaint)
        canvas.drawLine(left + tableWidth, top, left + tableWidth, y, gridPaint)
        gridPaint.strokeWidth = 1f
        canvas.drawRect(left, top, left + tableWidth, y, gridPaint)
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
        RightSegment.CVA -> context.getString(R.string.label_cva)
        RightSegment.KNEE -> context.getString(R.string.metric_knee_dev)
        RightSegment.HIP -> context.getString(R.string.metric_hip_dev)
        RightSegment.SHOULDER -> context.getString(R.string.metric_shoulder_dev)
        RightSegment.EAR -> context.getString(R.string.metric_ear_dev)
    }

    private fun frontValue(level: FrontLevel, metrics: FrontMetrics?): Float? = when (level) {
        FrontLevel.BODY -> metrics?.bodyAngleDeg
        FrontLevel.EARS -> metrics?.levelAngles?.firstOrNull { it.name == "Ears" }?.deviationDeg
        FrontLevel.SHOULDERS -> metrics?.levelAngles?.firstOrNull { it.name == "Shoulders" }?.deviationDeg
        FrontLevel.ASIS -> metrics?.levelAngles?.firstOrNull { it.name == "ASIS" }?.deviationDeg
        FrontLevel.KNEES -> metrics?.levelAngles?.firstOrNull { it.name == "Knees" }?.deviationDeg
        FrontLevel.FEET -> metrics?.levelAngles?.firstOrNull { it.name == "Feet" }?.deviationDeg
    }

    private fun rightValue(segment: RightSegment, metrics: RightMetrics?): Float? = when (segment) {
        RightSegment.CVA -> metrics?.cvaDeg
        RightSegment.KNEE -> metrics?.segments?.firstOrNull { it.name == "Knee" }?.angleDeg
        RightSegment.HIP -> metrics?.segments?.firstOrNull { it.name == "Hip" }?.angleDeg
        RightSegment.SHOULDER -> metrics?.segments?.firstOrNull { it.name == "Shoulder" }?.angleDeg
        RightSegment.EAR -> metrics?.segments?.firstOrNull { it.name == "Ear" }?.angleDeg
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timestamp))
    }

    private fun decodeBounds(path: String): Pair<Int, Int> {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, opts)
            (opts.outWidth.takeIf { it > 0 } ?: 0) to (opts.outHeight.takeIf { it > 0 } ?: 0)
        } catch (_: Exception) {
            0 to 0
        }
    }
}

