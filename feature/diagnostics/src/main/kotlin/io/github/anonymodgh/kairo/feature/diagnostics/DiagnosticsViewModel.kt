package io.github.anonymodgh.kairo.feature.diagnostics

import android.app.Application
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.anonymodgh.kairo.core.models.InstallState
import io.github.anonymodgh.kairo.core.models.ModelCatalog
import io.github.anonymodgh.kairo.core.models.ModelPackage
import io.github.anonymodgh.kairo.core.models.ModelStore
import io.github.anonymodgh.kairo.core.voice.VoiceLabVariant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

data class DiagnosticsUiState(
    val version: String = "",
    val device: List<Pair<String, String>> = emptyList(),
    val models: Map<String, InstallState> = emptyMap(),
    val queued: Set<String> = emptySet(),
    val running: BenchId? = null,
    val progress: String = "",
    val results: BenchResults = BenchResults(),
) {
    fun installed(pkg: ModelPackage) = models[pkg.id] == InstallState.Installed
    fun missing(id: BenchId) = requirements(id).filterNot(::installed)
    fun installedStt() = SttCandidate.entries.filter { installed(it.pkg) }
}

class DiagnosticsViewModel(private val app: Application) : AndroidViewModel(app) {
    private val store = ModelStore(File(app.filesDir, "models"))
    private val audioExecutor = Executors.newSingleThreadExecutor { r ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            r.run()
        }, "kairo-audio")
    }
    private val bench = Benchmarks(store, audioExecutor.asCoroutineDispatcher())
    private val downloads = mutableMapOf<String, Job>()
    private var phraseAudio: List<FloatArray>? = null
    private val sttByName = mutableMapOf<String, SttCandidate>()

    private val _state = MutableStateFlow(DiagnosticsUiState(version = appVersion(app), device = collectDeviceInfo(app)))
    val state: StateFlow<DiagnosticsUiState> = _state.asStateFlow()

    init {
        ModelCatalog.all.forEach { pkg ->
            viewModelScope.launch { store.state(pkg).collect { s -> _state.update { it.copy(models = it.models + (pkg.id to s)) } } }
        }
    }

    fun download(pkg: ModelPackage) {
        if (downloads[pkg.id]?.isActive == true) return
        _state.update { it.copy(queued = it.queued + pkg.id) }
        downloads[pkg.id] = viewModelScope.launch {
            try {
                store.install(pkg)
            } catch (_: CancellationException) {
            } catch (_: Exception) {
                // el error queda en InstallState.Failed
            } finally {
                _state.update { it.copy(queued = it.queued - pkg.id) }
            }
        }
    }

    fun cancel(pkg: ModelPackage) {
        downloads.remove(pkg.id)?.cancel()
    }

    fun delete(pkg: ModelPackage) {
        viewModelScope.launch {
            cancel(pkg)
            withContext(Dispatchers.IO) { store.delete(pkg) }
        }
    }

    fun report(): String {
        val s = _state.value
        return ReportBuilder.build(s.version, s.device, s.results)
    }

    fun run(id: BenchId) {
        val s = _state.value
        if (s.running != null || s.missing(id).isNotEmpty()) return
        _state.update { it.copy(running = id, progress = "", results = it.results.copy(errors = it.results.errors - id)) }
        viewModelScope.launch {
            try {
                execute(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val msg = "${e.javaClass.simpleName}: ${e.message.orEmpty()}"
                _state.update { it.copy(results = it.results.copy(errors = it.results.errors + (id to msg))) }
            } finally {
                _state.update { it.copy(running = null, progress = "") }
            }
        }
    }

    private fun results(f: (BenchResults) -> BenchResults) = _state.update { it.copy(results = f(it.results)) }

    private fun progress(text: String) = _state.update { it.copy(progress = text) }

    private suspend fun execute(id: BenchId) {
        when (id) {
            BenchId.VoiceLab1 -> voiceLab(id, "VoiceLab (Calidad) · 1 hilo", VoiceLabVariant.Quality, 1)
            BenchId.VoiceLab2 -> voiceLab(id, "VoiceLab (Calidad) · 2 hilos", VoiceLabVariant.Quality, 2)
            BenchId.VoiceLabLow -> voiceLab(id, "VoiceLab (40 ms) · 1 hilo", VoiceLabVariant.LowLatency, 1)
            BenchId.Piper -> {
                progress("Sintetizando 10 frases…")
                bench.piper().let { r -> results { it.copy(piper = r) } }
            }
            BenchId.Pipeline -> {
                progress("Piper y VoiceLab en paralelo…")
                bench.pipeline().let { r -> results { it.copy(pipeline = r) } }
            }
            BenchId.Stt -> stt()
            BenchId.Turn -> turn()
            BenchId.Fps -> {
                progress("Midiendo 5 s…")
                val hz = refreshRateHz(app)
                val r = fpsStats(captureFrames(5_000), hz)
                results { it.copy(fps = r) }
            }
        }
    }

    private suspend fun voiceLab(id: BenchId, label: String, variant: VoiceLabVariant, threads: Int) {
        progress("$label · 20 s de audio…")
        val r = bench.voiceLab(label, variant, threads)
        results { it.copy(voiceLab = it.voiceLab + (id to r)) }
    }

    private suspend fun phrases(): List<FloatArray> {
        phraseAudio?.let { return it }
        progress("Generando frases con Piper…")
        return bench.synthPhrases().also { phraseAudio = it }
    }

    private suspend fun stt() {
        val audio = phrases()
        val candidates = _state.value.installedStt()
        if (candidates.isEmpty()) error("No hay ningún modelo de STT instalado")
        results { it.copy(stt = emptyList()) }
        for (c in candidates) {
            progress("STT · ${c.pkg.title}…")
            val r = bench.stt(c, audio)
            sttByName[r.name] = c
            results { it.copy(stt = it.stt + r) }
        }
    }

    private suspend fun turn() {
        val audio = phrases()
        val candidates = _state.value.installedStt()
        if (candidates.isEmpty()) error("No hay ningún modelo de STT instalado")
        val winner = sttWinner(_state.value.results.stt)?.result?.name
        val candidate = sttByName[winner]?.takeIf { it in candidates } ?: candidates.first()
        progress("Turno completo · ${candidate.pkg.title}…")
        val r = bench.turn(candidate, audio.first())
        results { it.copy(turn = r) }
    }

    override fun onCleared() {
        audioExecutor.shutdown()
    }
}
