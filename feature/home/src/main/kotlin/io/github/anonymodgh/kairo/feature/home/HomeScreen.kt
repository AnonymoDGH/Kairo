package io.github.anonymodgh.kairo.feature.home

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.anonymodgh.kairo.core.ui.AuroraBackground
import io.github.anonymodgh.kairo.core.ui.GlassBackdrop
import io.github.anonymodgh.kairo.core.ui.GlassSurface
import io.github.anonymodgh.kairo.core.ui.KairoColors

@Composable
fun HomeScreen(versionName: String, modifier: Modifier = Modifier) {
    val refraction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    GlassBackdrop(
        background = { AuroraBackground(it) },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kairo", style = MaterialTheme.typography.displaySmall, color = KairoColors.TextPrimary)
                    Text(
                        "Tu asistente de voz, con tu voz.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KairoColors.TextSecondary,
                    )
                    Text(
                        buildString {
                            append("v$versionName · API ${Build.VERSION.SDK_INT} · ")
                            append(if (refraction) "cristal con refracción" else "cristal sin refracción")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = KairoColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}
