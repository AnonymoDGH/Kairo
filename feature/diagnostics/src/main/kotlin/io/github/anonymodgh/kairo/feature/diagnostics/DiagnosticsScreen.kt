package io.github.anonymodgh.kairo.feature.diagnostics

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import io.github.anonymodgh.kairo.core.ui.KairoShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.anonymodgh.kairo.core.models.InstallState
import io.github.anonymodgh.kairo.core.models.ModelCatalog
import io.github.anonymodgh.kairo.core.models.ModelPackage
import io.github.anonymodgh.kairo.core.ui.GlassSurface
import io.github.anonymodgh.kairo.core.ui.KairoIcon
import io.github.anonymodgh.kairo.core.ui.KairoIconView
import io.github.anonymodgh.kairo.core.ui.KairoColors

@Composable
fun DiagnosticsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: DiagnosticsViewModel = viewModel { DiagnosticsViewModel(app) }
    val state by vm.state.collectAsStateWithLifecycle()

    val view = LocalView.current
    val busy = state.running != null
    DisposableEffect(busy) {
        view.keepScreenOn = busy
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier.fillMaxSize().background(KairoColors.Scrim)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar(onBack, onCopy = { copyReport(context, vm.report()) })
            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Header("Modelos", "Recomendado con Wi-Fi") }
                ModelCatalog.all.forEach { pkg -> item(key = pkg.id) { ModelCard(pkg, state, vm) } }
                item { Header("Dispositivo") }
                item { DeviceCard(state.device) }
                item { Header("Bancos de prueba", "Audio sintético (Piper): el WER real con tu voz será algo peor") }
                benchItems(state, vm)
            }
        }
    }
}

private fun copyReport(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Informe Kairo", text))
    Toast.makeText(context, "Informe copiado", Toast.LENGTH_SHORT).show()
}

@Composable
private fun TopBar(onBack: () -> Unit, onCopy: () -> Unit) {
    GlassSurface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = KairoShapes.Pill,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack, modifier = Modifier.semantics { contentDescription = "Atrás" }) { KairoIconView(KairoIcon.Back, size = 22.dp) }
            Text("Diagnóstico", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = KairoColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onCopy) { Text("Copiar informe", color = KairoColors.Accent, maxLines = 1) }
        }
    }
}

@Composable
private fun Header(title: String, note: String? = null) {
    Column(Modifier.padding(top = 8.dp, start = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = KairoColors.TextPrimary)
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = KairoColors.TextSecondary) }
    }
}

@Composable
private fun FlatCard(content: @Composable () -> Unit) {
    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Mono(text: String, color: Color = KairoColors.TextPrimary) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = color)

@Composable
private fun Small(text: String, color: Color = KairoColors.TextSecondary) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)

private fun mb(bytes: Long) = "${bytes / 1_000_000} MB"

@Composable
private fun ModelCard(pkg: ModelPackage, ui: DiagnosticsUiState, vm: DiagnosticsViewModel) = FlatCard {
    val st = ui.models[pkg.id] ?: InstallState.NotInstalled
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(pkg.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = KairoColors.TextPrimary)
        Mono(mb(pkg.totalBytes), KairoColors.TextSecondary)
    }
    Small(pkg.license)
    when (st) {
        is InstallState.Downloading -> {
            LinearProgressIndicator({ if (st.total > 0) st.downloaded.toFloat() / st.total else 0f }, Modifier.fillMaxWidth(), color = KairoColors.Accent)
            Mono("${mb(st.downloaded)} / ${mb(st.total)}")
        }
        InstallState.Verifying -> Mono("Verificando…")
        InstallState.Extracting -> Mono("Descomprimiendo…")
        InstallState.Installed -> Mono("Instalado", KairoColors.Accent)
        is InstallState.Failed -> Text(st.message, style = MaterialTheme.typography.bodyMedium, color = KairoColors.Error)
        InstallState.NotInstalled -> if (pkg.id in ui.queued) Mono("En cola…")
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val inProgress = st is InstallState.Downloading || st == InstallState.Verifying || st == InstallState.Extracting ||
            (st == InstallState.NotInstalled && pkg.id in ui.queued)
        if (inProgress) OutlinedButton({ vm.cancel(pkg) }) { Text("Cancelar") }
        else if (st == InstallState.Installed) OutlinedButton({ vm.delete(pkg) }, enabled = ui.running == null) { Text("Borrar") }
        else PrimaryButton("Descargar") { vm.download(pkg) }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) = Button(
    onClick,
    enabled = enabled,
    colors = ButtonDefaults.buttonColors(containerColor = KairoColors.Accent, contentColor = KairoColors.OnAccent),
) { Text(text) }

@Composable
private fun DeviceCard(info: List<Pair<String, String>>) = FlatCard {
    info.forEach { (k, v) ->
        Row {
            Small(k)
            Text(v, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.labelMedium, color = KairoColors.TextPrimary, textAlign = TextAlign.End)
        }
    }
}

private fun LazyListScope.benchItems(ui: DiagnosticsUiState, vm: DiagnosticsViewModel) {
    item {
        BenchCard(BenchId.VoiceLab1, "VoiceLab (Calidad) · 1 hilo", "20 s de audio sintético. Criterio: RTF p99 < 0,6.", ui, vm) { VoiceLabLines(ui.results.voiceLab[BenchId.VoiceLab1]) }
    }
    item {
        BenchCard(BenchId.VoiceLab2, "VoiceLab (Calidad) · 2 hilos", "Para cuando 1 hilo da entre 0,6 y 0,9.", ui, vm) { VoiceLabLines(ui.results.voiceLab[BenchId.VoiceLab2]) }
    }
    if (ui.installed(ModelCatalog.VoiceLabLowLatency)) item {
        BenchCard(BenchId.VoiceLabLow, "VoiceLab (40 ms) · 1 hilo", "Variante de baja latencia.", ui, vm) { VoiceLabLines(ui.results.voiceLab[BenchId.VoiceLabLow]) }
    }
    item {
        BenchCard(BenchId.Piper, "Piper", "10 frases tras un calentamiento. Criterio: RTF < 0,3.", ui, vm) {
            ui.results.piper?.let { Mono("${it.verdict.icon} RTF ${fmt(it.rtf)} · ${fmt(it.audioSeconds, 1)} s de audio en ${fmt(it.synthMs, 0)} ms") }
        }
    }
    item {
        BenchCard(BenchId.Pipeline, "VoiceLab + Piper a la vez", "Como en la conversación real. Criterio: RTF combinado < 0,8.", ui, vm) {
            ui.results.pipeline?.let {
                Mono("${it.verdict.icon} RTF combinado ${fmt(it.rtf)} · ${fmt(it.wallMs, 0)} ms")
                Mono("Piper ${fmt(it.piperLoad)} x audio · VoiceLab ${fmt(it.voiceLabLoad)} x audio")
                Mono("Primer audio: ${fmt(it.firstAudioMs, 0)} ms")
            }
        }
    }
    item {
        BenchCard(BenchId.Stt, "STT", "Cada modelo instalado, 10 frases. Criterio: 4 s en ≤ 700 ms y WER ≤ 15 %. Audio sintético (Piper): el WER real con tu voz será algo peor.", ui, vm, extraNeed = ui.installedStt().isEmpty()) {
            ui.results.stt.forEach { SttResultView(it) }
            sttWinner(ui.results.stt)?.let {
                Mono("Ganador: ${it.result.name}" + if (it.meetsBoth) "" else " (no cumple ambos criterios)", KairoColors.Accent)
            }
        }
    }
    item {
        BenchCard(BenchId.Turn, "Latencia de un turno (sin LLM)", "STT + Piper + VoiceLab hasta el primer audio. No incluye el primer token del LLM (300–800 ms).", ui, vm, extraNeed = ui.installedStt().isEmpty()) {
            ui.results.turn?.let {
                Mono("STT ${fmt(it.sttMs, 0)} ms (${it.sttName})")
                Mono("Piper ${fmt(it.piperMs, 0)} ms")
                Mono("VoiceLab ${fmt(it.voiceLabMs, 0)} ms")
                Mono("Total ${fmt(it.totalMs, 0)} ms", KairoColors.Accent)
            }
        }
    }
    item {
        BenchCard(BenchId.Fps, "Fluidez de la UI (fps)", "5 s con el fondo aurora animado.", ui, vm) {
            ui.results.fps?.let {
                Mono("${fmt(it.avgFps, 1)} fps a ${fmt(it.refreshHz, 0)} Hz · jank ${fmt(it.jankPct, 1)} %")
                Mono("Fotograma p99 ${fmt(it.p99FrameMs, 1)} ms")
            }
        }
    }
}

@Composable
private fun VoiceLabLines(r: VoiceLabResult?) {
    r ?: return
    Mono("${r.verdict.icon} RTF p50 ${fmt(r.rtfP50)} · p99 ${fmt(r.rtfP99)} ${r.hint}".trimEnd())
    Mono("ms/bloque p50 ${fmt(r.blockMsP50, 1)} · p99 ${fmt(r.blockMsP99, 1)}")
    Mono("Latencia algorítmica ${fmt(r.latencyMs, 0)} ms")
}

@Composable
private fun SttResultView(s: SttResult) {
    var open by remember { mutableStateOf(false) }
    Mono("${if (s.meetsBoth) "✅ " else ""}${s.name}")
    Mono("media ${fmt(s.meanMs, 0)} ms · 4 s ${fmt(s.ms4s, 0)} ms · máx ${fmt(s.maxMs, 0)} ms · WER ${fmt(s.wer * 100, 1)} %", KairoColors.TextSecondary)
    TextButton({ open = !open }) { Text(if (open) "Ocultar frases" else "Ver frases", color = KairoColors.Accent) }
    if (open) s.phrases.forEachIndexed { i, p ->
        Small("${i + 1}. ${p.reference}")
        Mono(p.hypothesis.ifBlank { "(vacío)" })
    }
}

@Composable
private fun BenchCard(
    id: BenchId,
    title: String,
    description: String,
    ui: DiagnosticsUiState,
    vm: DiagnosticsViewModel,
    extraNeed: Boolean = false,
    result: @Composable () -> Unit,
) = FlatCard {
    val missing = ui.missing(id).map { it.title } + if (extraNeed) listOf("algún modelo de STT") else emptyList()
    Text(title, style = MaterialTheme.typography.titleMedium, color = KairoColors.TextPrimary)
    Small(description)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton("Ejecutar", enabled = ui.running == null && missing.isEmpty()) { vm.run(id) }
        if (ui.running == id) {
            CircularProgressIndicator(Modifier.size(20.dp), color = KairoColors.Accent, strokeWidth = 2.dp)
            Mono(ui.progress, KairoColors.TextSecondary)
        }
    }
    if (missing.isNotEmpty()) Text("Falta descargar: ${missing.joinToString()}", style = MaterialTheme.typography.bodyMedium, color = KairoColors.Error)
    ui.results.errors[id]?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = KairoColors.Error) }
    result()
}
