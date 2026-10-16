package io.github.anonymodgh.kairo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val LocalGlassSource = staticCompositionLocalOf<HazeState?> { null }

/**
 * Capa de fondo que las [GlassSurface] desenfocan y refractan. El fondo se graba una vez y lo
 * reutilizan todas las superficies (presupuesto de GPU, KAIRO_PLAN §9).
 */
@Composable
fun GlassBackdrop(
    background: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberHazeState()
    Box(modifier) {
        background(Modifier.matchParentSize().hazeSource(state))
        CompositionLocalProvider(LocalGlassSource provides state) { content() }
    }
}

/**
 * Tarjeta de cristal líquido: desenfoque del fondo, relleno blanco 8 %, borde degradado y brillo
 * especular. En API 33+ Haze añade refracción con un shader AGSL; en 31–32 cae a solo desenfoque.
 * Con [blur] = false es un cristal "plano" (sin desenfoque) para listas: el presupuesto de GPU es
 * de 2 superficies desenfocadas visibles a la vez.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = KairoShapes.Card,
    blurRadius: Dp = 28.dp,
    refraction: Boolean = true,
    blur: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val source = LocalGlassSource.current
    val style = remember(shape, blurRadius, refraction) {
        GlassStyle {
            shape(shape)
            optics(
                refractionStrength = if (refraction) 0.6f else 0f,
                refractionDisplacement = if (refraction) 12.dp else 0.dp,
                blurRadius = blurRadius,
            )
            // Kairo siempre es oscuro: fijamos la respuesta "oscura" de Haze aunque el sistema esté en claro.
            tint(KairoColors.GlassFill)
            specularIntensity(0.38f)
            edgeShadow(Color.Black.copy(alpha = 0.32f))
            ambientResponse(0.08f)
            contrast(0.08f)
            whitePoint(-0.22f)
            chromaMultiplier(1.1f)
        }
    }
    val glass = when {
        blur && source != null -> Modifier.hazeGlass(HazeInput.Sources(source), style)
        else -> Modifier.background(KairoColors.GlassFillFlat)
    }
    Box(
        modifier
            .clip(shape)
            .then(glass)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        0f to KairoColors.GlassSpecular,
                        1f to Color.Transparent,
                        endY = 18.dp.toPx(),
                    ),
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(KairoColors.GlassBorderStart, KairoColors.GlassBorderEnd),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}
