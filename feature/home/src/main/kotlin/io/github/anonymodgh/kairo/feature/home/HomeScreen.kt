package io.github.anonymodgh.kairo.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.anonymodgh.kairo.core.ui.GlassOrb
import io.github.anonymodgh.kairo.core.ui.GlassSurface
import io.github.anonymodgh.kairo.core.ui.KairoColors
import io.github.anonymodgh.kairo.core.ui.KairoIcon
import io.github.anonymodgh.kairo.core.ui.KairoIconView
import io.github.anonymodgh.kairo.core.ui.KairoShapes

/**
 * Inicio: un panel de cristal sobre la pantalla que haya detrás, con la esfera en el centro y la
 * barra de controles abajo. La conversación por voz llega en la Fase 4; aquí el micro solo
 * cambia el estado visual.
 */
@Composable
fun HomeScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    var listening by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        GlassSurface(Modifier.fillMaxSize(), shape = KairoShapes.Panel, contentPadding = PaddingValues(20.dp)) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.weight(1f))
                GlassOrb(
                    level = 0f,
                    active = listening,
                    modifier = Modifier.fillMaxWidth(0.36f).aspectRatio(1f),
                )
                Text(
                    if (listening) "Escuchando…" else "Toca el micro para hablar",
                    style = MaterialTheme.typography.titleMedium,
                    color = KairoColors.TextPrimary,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (listening) "La conversación por voz llega en la Fase 4." else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KairoColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
                ControlBar(
                    listening = listening,
                    onMic = { listening = !listening },
                    onKeyboard = {},
                    onSettings = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun ControlBar(listening: Boolean, onMic: () -> Unit, onKeyboard: () -> Unit, onSettings: () -> Unit) {
    GlassSurface(
        Modifier.fillMaxWidth(0.84f).height(68.dp),
        shape = KairoShapes.Pill,
        fill = KairoColors.GlassFillStrong,
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(KairoIcon.Keyboard, "Escribir", 26.dp, onKeyboard)
            BarButton(
                KairoIcon.Mic,
                if (listening) "Dejar de escuchar" else "Hablar",
                32.dp,
                onMic,
                tint = if (listening) KairoColors.Accent else KairoColors.TextPrimary,
            )
            BarButton(KairoIcon.Settings, "Ajustes", 26.dp, onSettings)
        }
    }
}

@Composable
private fun BarButton(icon: KairoIcon, label: String, size: Dp, onClick: () -> Unit, tint: Color = KairoColors.TextPrimary) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        KairoIconView(icon, size = size, color = tint)
    }
}
