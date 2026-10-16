package io.github.anonymodgh.kairo.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

private val White = Color.White

/**
 * Esfera de cristal transparente con ondas internas, como una gota de agua.
 * [level] (0..1) es el nivel de voz: la esfera se hincha y las ondas se marcan más.
 * Solo invalida el dibujo, no recompone en cada frame.
 */
@Composable
fun GlassOrb(level: Float, active: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "orb")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "ripple")
    val breath by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "breath")
    val voice by animateFloatAsState(level.coerceIn(0f, 1f), KairoMotion.spring(), label = "voice")
    val energy by animateFloatAsState(if (active) 1f else 0.35f, KairoMotion.spring(), label = "energy")
    Canvas(modifier) {
        val s = 0.98f + 0.04f * breath + 0.10f * voice
        scale(s) { drawOrb({ ripple }, energy + voice * 0.8f) }
    }
}

private fun DrawScope.drawOrb(ripple: () -> Float, energy: Float) {
    val r = size.minDimension / 2f
    val c = center
    // Cuerpo: casi transparente en el centro y más blanco hacia el borde (fresnel).
    drawCircle(
        Brush.radialGradient(
            0f to White.copy(alpha = 0.05f),
            0.62f to White.copy(alpha = 0.10f),
            0.88f to White.copy(alpha = 0.26f),
            1f to White.copy(alpha = 0.55f),
            center = c,
            radius = r,
        ),
        r,
        c,
    )
    // Ondas concéntricas internas, centradas un poco por encima (la luz entra desde arriba).
    val rc = Offset(c.x, c.y - r * 0.08f)
    val phase = ripple()
    for (i in 0 until 4) {
        val p = (phase + i / 4f) % 1f
        val alpha = (1f - p) * p * 4f * 0.22f * energy.coerceIn(0f, 1.5f)
        val rr = r * (0.12f + 0.62f * p)
        drawCircle(White.copy(alpha = alpha * 0.35f), rr, rc, style = Stroke(width = r * 0.07f))
        drawCircle(White.copy(alpha = alpha * 0.8f), rr, rc, style = Stroke(width = r * 0.018f))
    }
    // Luz refractada que se acumula abajo, dentro de la esfera.
    drawCircle(
        Brush.radialGradient(
            0f to White.copy(alpha = 0.30f),
            1f to Color.Transparent,
            center = Offset(c.x, c.y + r * 0.72f),
            radius = r * 0.55f,
        ),
        r * 0.96f,
        c,
    )
    // Reflejo especular arriba a la izquierda.
    drawOval(
        Brush.radialGradient(
            0f to White.copy(alpha = 0.85f),
            0.5f to White.copy(alpha = 0.25f),
            1f to Color.Transparent,
            center = Offset(c.x - r * 0.38f, c.y - r * 0.46f),
            radius = r * 0.32f,
        ),
        topLeft = Offset(c.x - r * 0.70f, c.y - r * 0.72f),
        size = Size(r * 0.64f, r * 0.50f),
    )
    // Borde brillante.
    drawCircle(
        Brush.sweepGradient(
            0f to White.copy(alpha = 0.35f),
            0.25f to White.copy(alpha = 0.85f),
            0.5f to White.copy(alpha = 0.30f),
            0.75f to White.copy(alpha = 0.70f),
            1f to White.copy(alpha = 0.35f),
            center = c,
        ),
        r - r * 0.012f,
        c,
        style = Stroke(width = r * 0.025f),
    )
}
