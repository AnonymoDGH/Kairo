package io.github.anonymodgh.kairo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Panel de cristal líquido: relleno blanco translúcido, borde brillante en degradado y reflejo
 * especular arriba. El desenfoque lo pone la ventana (blur-behind del sistema), no el panel.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = KairoShapes.Card,
    fill: Color = KairoColors.GlassFill,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(fill)
            .drawWithContent {
                drawRect(
                    Brush.verticalGradient(
                        0f to KairoColors.GlassSpecular,
                        1f to Color.Transparent,
                        endY = 28.dp.toPx(),
                    ),
                )
                drawContent()
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(KairoColors.GlassBorderStart, KairoColors.GlassBorderEnd, KairoColors.GlassBorderStart.copy(alpha = 0.3f)),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}
