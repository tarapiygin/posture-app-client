package com.example.postureapp.ui.indicators

import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.postureapp.core.draw.drawDottedLine
import com.example.postureapp.core.draw.drawDottedVertical
import com.example.postureapp.core.draw.drawLevelGuide
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ----------------------------------------------------------------------
// Цвета индикаторов (можешь позже перенести в общую тему приложения)
// ----------------------------------------------------------------------

val IndicatorBlue: Color = Color(0xFF89B4F8)   // голубые линии уровней и рамки
val IndicatorRed: Color = Color(0xFFFF3B30)    // красные линии/углы
val IndicatorGreen: Color = Color(0xFF2ECC71)  // зелёная вертикальная ось

// ----------------------------------------------------------------------
// Вспомогательные функции
// ----------------------------------------------------------------------

private fun formatDeg(value: Float): String {
    val v = if (value < 0.1f) 0f else value
    return String.format("%.1f\u00B0", v)
}

private fun length(v: Offset): Float = sqrt(v.x * v.x + v.y * v.y)

private operator fun Offset.div(scalar: Float): Offset = Offset(x / scalar, y / scalar)

private operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)

// ----------------------------------------------------------------------
// Голубой квадрат с текстом и углом (Composable)
// ----------------------------------------------------------------------

/**
 * Голубой прямоугольник с текстом вида "Shoulders: 3.2°".
 * Аналог BlueSquareTextWidget.
 */
@Composable
fun BlueLevelAngleLabel(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = IndicatorBlue,
    backgroundColor: Color = Color.White,
    textColor: Color = Color(0xFF2B2B2B)
) {
    Surface(
        color = backgroundColor,
        shape = RectangleShape,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                color = textColor
            )
        }
    }
}

// ----------------------------------------------------------------------
// Геометрия отклонения тела и красная линия
// ----------------------------------------------------------------------

/**
 * Геометрия отклонения тела:
 * - base    — точка основания тела (ось тела);
 * - jugular — точка яремной выемки;
 * - dir     — нормализованный вектор направления от base к jugular;
 * - extended — точка пересечения продолжения этой линии с верхней границей кадра.
 */
data class BodyDeviationGeometry(
    val base: Offset,
    val jugular: Offset,
    val dir: Offset,
    val extended: Offset
)

/**
 * Вычисляет геометрию красной линии отклонения тела по двум точкам: base и jugular.
 * Линия продолжается вверх до пересечения с верхней границей (y = 0).
 */
fun computeBodyDeviationGeometry(
    base: Offset,
    jugular: Offset
): BodyDeviationGeometry {
    val deviationVector = Offset(
        x = jugular.x - base.x,
        y = jugular.y - base.y
    )
    val deviationLength = length(deviationVector).takeIf { it > 0.0001f } ?: 1f
    val dir = deviationVector / deviationLength

    // фактор для пересечения с верхней границей (y = 0)
    val factor = if (dir.y == 0f) 1f else base.y / -dir.y

    val extended = Offset(
        x = base.x + dir.x * factor,
        y = base.y + dir.y * factor
    )

    return BodyDeviationGeometry(
        base = base,
        jugular = jugular,
        dir = dir,
        extended = extended
    )
}

/**
 * Красная прерывистая линия тела от base до extended.
 * Использует уже рассчитанную BodyDeviationGeometry.
 */
fun DrawScope.drawBodyDeviationLine(
    geometry: BodyDeviationGeometry,
    color: Color = IndicatorRed,
    strokeWidth: Dp = 3.dp
) {
    drawDottedLine(
        start = geometry.base,
        end = geometry.extended,
        color = color,
        strokeWidth = strokeWidth
    )
}

// ----------------------------------------------------------------------
// Зелёная вертикальная ось тела
// ----------------------------------------------------------------------

/**
 * Зелёная пунктирная вертикальная ось тела через заданную X-координату.
 */
fun DrawScope.drawBodyAxisLine(
    baseX: Float,
    color: Color = IndicatorGreen,
    strokeWidth: Dp = 3.dp
) {
    drawDottedVertical(
        x = baseX,
        color = color,
        strokeWidth = strokeWidth
    )
}

// ----------------------------------------------------------------------
// Голубые горизонтальные линии уровней
// ----------------------------------------------------------------------

/**
 * Голубая горизонтальная линия уровня на заданной Y-координате.
 */
fun DrawScope.drawLevelGuideLine(
    y: Float,
    color: Color = IndicatorBlue,
    strokeWidth: Dp = 2.dp
) {
    drawLevelGuide(
        y = y,
        color = color,
        strokeWidth = strokeWidth
    )
}

// ----------------------------------------------------------------------
// Красный квадрат с углом + коннектор до красной линии тела
// ----------------------------------------------------------------------

/**
 * Красный прямоугольник с числом угла и пунктирным коннектором
 * до красной линии тела на уровне данной Y-координаты.
 *
 * @param y         Y-координата уровня (средняя точка уровня).
 * @param angleDeg  Значение отклонения уровня (в градусах).
 * @param geometry  Геометрия красной линии тела (base, dir и т.д.).
 */
fun DrawScope.drawRedAngleLabel(
    y: Float,
    angleDeg: Float,
    geometry: BodyDeviationGeometry,
    borderColor: Color = IndicatorRed,
    backgroundColor: Color = Color.White
) {
    val levelLabelWidth = 44.dp.toPx()
    val levelLabelHeight = 28.dp.toPx()
    val levelLabelRightMargin = 16.dp.toPx()
    val levelLabelTopShift = 12.dp.toPx()
    val levelLabelBorder = 1.5.dp.toPx()

    val labelLeft = size.width - levelLabelRightMargin - levelLabelWidth
    val labelTop = y - levelLabelHeight - levelLabelTopShift
    val labelRectSize = Size(levelLabelWidth, levelLabelHeight)

    // фон
    drawRect(
        color = backgroundColor,
        topLeft = Offset(labelLeft, labelTop),
        size = labelRectSize
    )

    // рамка
    drawRect(
        color = borderColor,
        topLeft = Offset(labelLeft, labelTop),
        size = labelRectSize,
        style = Stroke(width = levelLabelBorder)
    )

    // текст
    val textPaint = android.graphics.Paint().apply {
        color = Color(0xFF2B2B2B).toArgb()
        textSize = 12.sp.toPx()
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    drawContext.canvas.nativeCanvas.drawText(
        formatDeg(angleDeg),
        labelLeft + 8.dp.toPx(),
        labelTop + levelLabelHeight / 2f + textPaint.textSize / 3f,
        textPaint
    )

    // пересечение уровня y с красной линией тела
    val dir = geometry.dir
    val base = geometry.base

    val intersection = if (abs(dir.y) < 1e-4f) {
        base
    } else {
        val t = (y - base.y) / dir.y
        Offset(
            x = base.x + dir.x * t,
            y = base.y + dir.y * t
        )
    }

    // пунктирный коннектор от нижнего края квадрата до красной линии
    drawDottedLine(
        start = Offset(labelLeft, labelTop + levelLabelHeight),
        end = intersection,
        color = borderColor,
        strokeWidth = 2.dp
    )
}

// ----------------------------------------------------------------------
// Подпись общего угла тела (красный текст рядом с основанием)
// ----------------------------------------------------------------------

/**
 * Отрисовка общего угла тела (красный текст около точки base).
 */
fun DrawScope.drawBodyAngleText(
    base: Offset,
    angleDeg: Float,
    color: Color = IndicatorRed
) {
    val text = "${angleDeg.roundToInt()}\u00B0"

    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = 14.sp.toPx()
        isAntiAlias = true
        typeface = Typeface.create(
            Typeface.DEFAULT,
            Typeface.BOLD
        )
    }

    drawContext.canvas.nativeCanvas.drawText(
        text,
        base.x + 12.dp.toPx(),
        base.y - 12.dp.toPx(),
        paint
    )
}
