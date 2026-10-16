package io.github.anonymodgh.kairo.core.ui

import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tokens de diseño: cristal líquido monocromo estilo iOS 26 sobre la pantalla que haya detrás
 * (la ventana de Kairo es translúcida y desenfoca lo de debajo). Un solo acento.
 */
object KairoColors {
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color.White.copy(alpha = 0.66f)
    val TextTertiary = Color.White.copy(alpha = 0.42f)
    val Accent = Color(0xFF0A84FF)
    val Error = Color(0xFFFF9F0A)
    val OnAccent = Color.White

    /** Velo bajo los paneles para que el texto se lea sobre cualquier fondo. */
    val Scrim = Color.Black.copy(alpha = 0.28f)
    /** Fondo cuando el sistema no permite desenfocar lo de detrás. */
    val FallbackBackground = Color(0xFF3A3B3E)

    val GlassFill = Color.White.copy(alpha = 0.10f)
    val GlassFillStrong = Color.White.copy(alpha = 0.16f)
    val GlassBorderStart = Color.White.copy(alpha = 0.55f)
    val GlassBorderEnd = Color.White.copy(alpha = 0.14f)
    val GlassSpecular = Color.White.copy(alpha = 0.18f)
}

object KairoShapes {
    val Panel = RoundedCornerShape(36.dp)
    val Card = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(999.dp)
}

object KairoMotion {
    const val SHORT_MS = 220
    const val LONG_MS = 320
    fun <T> spring() = spring<T>(dampingRatio = 0.8f, stiffness = 300f)
}
