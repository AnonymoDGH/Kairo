package io.github.anonymodgh.kairo.core.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Tokens de diseño (KAIRO_PLAN §9). */
object KairoColors {
    val Background = Color(0xFF07070B)
    val Violet = Color(0xFF7C5CFF)
    val Cyan = Color(0xFF22D3EE)
    val Magenta = Color(0xFFF472B6)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA1A1AA)
    val Error = Color(0xFFF59E0B)

    val Listening = Cyan
    val Thinking = Violet

    val GlassFill = Color.White.copy(alpha = 0.08f)
    val GlassBorderStart = Color.White.copy(alpha = 0.28f)
    val GlassBorderEnd = Color.White.copy(alpha = 0.04f)
    val GlassSpecular = Color.White.copy(alpha = 0.12f)
}

object KairoShapes {
    val Card = RoundedCornerShape(24.dp)
    val Dock = RoundedCornerShape(28.dp)
    val Chip = RoundedCornerShape(999.dp)
}

object KairoMotion {
    const val SHORT_MS = 220
    const val LONG_MS = 320
    fun <T> spring() = spring<T>(dampingRatio = 0.8f, stiffness = 300f)
}
