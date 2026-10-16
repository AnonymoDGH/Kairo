package io.github.anonymodgh.kairo.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Iconos de trazo fino dibujados a mano (estilo SF Symbols), en una caja de 24×24. */
enum class KairoIcon { Mic, Keyboard, Settings, Back }

@Composable
fun KairoIconView(icon: KairoIcon, modifier: Modifier = Modifier, size: Dp = 26.dp, color: Color = KairoColors.TextPrimary) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 24f
        val stroke = Stroke(width = 1.6f * u, cap = StrokeCap.Round)
        when (icon) {
            KairoIcon.Mic -> mic(u, stroke, color)
            KairoIcon.Keyboard -> keyboard(u, stroke, color)
            KairoIcon.Settings -> gear(u, stroke, color)
            KairoIcon.Back -> back(u, stroke, color)
        }
    }
}

private fun DrawScope.mic(u: Float, s: Stroke, c: Color) {
    drawRoundRect(c, Offset(9f * u, 2.5f * u), Size(6f * u, 11.5f * u), CornerRadius(3f * u), style = s)
    drawArc(c, 0f, 180f, false, Offset(5.5f * u, 6.5f * u), Size(13f * u, 11f * u), style = s)
    drawLine(c, Offset(12f * u, 17.5f * u), Offset(12f * u, 21.5f * u), s.width, StrokeCap.Round)
    drawLine(c, Offset(8.5f * u, 21.5f * u), Offset(15.5f * u, 21.5f * u), s.width, StrokeCap.Round)
}

private fun DrawScope.keyboard(u: Float, s: Stroke, c: Color) {
    drawRoundRect(c, Offset(2.5f * u, 5.5f * u), Size(19f * u, 13f * u), CornerRadius(2.5f * u), style = s)
    for (row in 0..1) for (col in 0..4) {
        drawCircle(c, 0.9f * u, Offset((6f + col * 3f) * u, (9f + row * 3f) * u))
    }
    drawLine(c, Offset(8f * u, 15.3f * u), Offset(16f * u, 15.3f * u), s.width, StrokeCap.Round)
}

private fun DrawScope.gear(u: Float, s: Stroke, c: Color) {
    val center = Offset(12f * u, 12f * u)
    for (i in 0 until 8) {
        val a = i * PI / 4
        val inner = Offset(center.x + cos(a).toFloat() * 7.2f * u, center.y + sin(a).toFloat() * 7.2f * u)
        val outer = Offset(center.x + cos(a).toFloat() * 9.6f * u, center.y + sin(a).toFloat() * 9.6f * u)
        drawLine(c, inner, outer, 2.6f * u, StrokeCap.Round)
    }
    drawCircle(c, 7.2f * u, center, style = s)
    drawCircle(c, 3f * u, center, style = s)
}

private fun DrawScope.back(u: Float, s: Stroke, c: Color) {
    drawLine(c, Offset(15f * u, 4.5f * u), Offset(7.5f * u, 12f * u), s.width, StrokeCap.Round)
    drawLine(c, Offset(7.5f * u, 12f * u), Offset(15f * u, 19.5f * u), s.width, StrokeCap.Round)
}
