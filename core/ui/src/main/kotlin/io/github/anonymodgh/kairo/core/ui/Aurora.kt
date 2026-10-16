package io.github.anonymodgh.kairo.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()

private class Blob(val color: Color, val cx: Float, val cy: Float, val ax: Float, val ay: Float, val radius: Float)

private val Blobs = listOf(
    Blob(KairoColors.Violet.copy(alpha = 0.42f), cx = 0.25f, cy = 0.28f, ax = 0.18f, ay = 0.10f, radius = 0.75f),
    Blob(KairoColors.Cyan.copy(alpha = 0.36f), cx = 0.78f, cy = 0.55f, ax = 0.14f, ay = 0.16f, radius = 0.65f),
    Blob(KairoColors.Magenta.copy(alpha = 0.38f), cx = 0.35f, cy = 0.85f, ax = 0.20f, ay = 0.08f, radius = 0.70f),
)

/** Ciclos de 20–30 s (KAIRO_PLAN §9). */
private val CycleMs = intArrayOf(20_000, 26_000, 30_000)

/**
 * Fondo aurora: manchas de color que se desplazan muy despacio sobre [KairoColors.Background].
 * Solo invalida la fase de dibujo, nunca recompone.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    val phases: List<State<Float>> = if (animated) {
        val transition = rememberInfiniteTransition(label = "aurora")
        CycleMs.map { ms ->
            transition.animateFloat(
                initialValue = 0f,
                targetValue = TWO_PI,
                animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Restart),
                label = "aurora-$ms",
            )
        }
    } else {
        remember { CycleMs.map { mutableFloatStateOf(1f) } }
    }
    Box(modifier.drawBehind { drawAurora(phases) }) { content() }
}

private fun DrawScope.drawAurora(phases: List<State<Float>>) {
    drawRect(KairoColors.Background)
    val w = size.width
    val h = size.height
    val base = maxOf(w, h)
    Blobs.forEachIndexed { i, blob ->
        val t = phases[i].value
        val center = Offset(
            x = (blob.cx + blob.ax * sin(t + i)) * w,
            y = (blob.cy + blob.ay * cos(t * 1.3f + i * 2)) * h,
        )
        val r = blob.radius * base * (0.92f + 0.08f * sin(t * 2))
        drawCircle(
            brush = Brush.radialGradient(
                0f to blob.color,
                0.45f to blob.color.copy(alpha = blob.color.alpha * 0.35f),
                1f to Color.Transparent,
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
    }
}
