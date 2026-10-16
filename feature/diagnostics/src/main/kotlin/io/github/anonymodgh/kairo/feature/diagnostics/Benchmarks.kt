package io.github.anonymodgh.kairo.feature.diagnostics

import io.github.anonymodgh.kairo.core.listen.SherpaOfflineStt
import io.github.anonymodgh.kairo.core.listen.SherpaStreamingStt
import io.github.anonymodgh.kairo.core.listen.SpeechToText
import io.github.anonymodgh.kairo.core.models.ModelCatalog
import io.github.anonymodgh.kairo.core.models.ModelPackage
import io.github.anonymodgh.kairo.core.models.ModelStore
import io.github.anonymodgh.kairo.core.voice.PiperTts
import io.github.anonymodgh.kairo.core.voice.VOICE_SAMPLE_RATE
import io.github.anonymodgh.kairo.core.voice.VoiceLabConverter
import io.github.anonymodgh.kairo.core.voice.VoiceLabVariant
import io.github.anonymodgh.kairo.feature.diagnostics.stt.SttTestSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

enum class SttCandidate(val pkg: ModelPackage) {
    WhisperBase(ModelCatalog.WhisperBase),
    WhisperSmall(ModelCatalog.WhisperSmall),
    NemoCtc(ModelCatalog.NemoCtcEs),
    Kroko(ModelCatalog.KrokoEs),
    ;

    fun create(dir: File): SpeechToText = when (this) {
        WhisperBase -> SherpaOfflineStt.whisper(dir, "base")
        WhisperSmall -> SherpaOfflineStt.whisper(dir, "small")
        NemoCtc -> SherpaOfflineStt.nemoCtc(dir)
        Kroko -> SherpaStreamingStt(dir)
    }
}

/** Paquetes que necesita cada banco (para deshabilitar el botón y explicar por qué). */
fun requirements(id: BenchId): List<ModelPackage> = when (id) {
    BenchId.VoiceLab1, BenchId.VoiceLab2 -> listOf(ModelCatalog.VoiceLabQuality)
    BenchId.VoiceLabLow -> listOf(ModelCatalog.VoiceLabLowLatency)
    BenchId.Piper -> listOf(ModelCatalog.PiperEs)
    BenchId.Pipeline -> listOf(ModelCatalog.VoiceLabQuality, ModelCatalog.PiperEs)
    BenchId.Stt -> listOf(ModelCatalog.PiperEs)
    BenchId.Turn -> listOf(ModelCatalog.PiperEs, ModelCatalog.VoiceLabQuality)
    BenchId.Fps -> emptyList()
}

private const val BLOCK = 2560
private const val TURN_SENTENCE = "Claro, ahora mismo te lo explico con calma."

private inline fun <T> timed(block: () -> T): Pair<T, Double> {
    val t0 = System.nanoTime()
    val r = block()
    return r to (System.nanoTime() - t0) / 1e6
}

/** Bancos de §9.6. [audio] es el hilo con prioridad de audio; Piper y STT van en Default. */
class Benchmarks(private val store: ModelStore, private val audio: CoroutineDispatcher) {
    private fun dir(pkg: ModelPackage) = store.dir(pkg)

    suspend fun voiceLab(label: String, variant: VoiceLabVariant, threads: Int): VoiceLabResult = withContext(audio) {
        val pkg = if (variant == VoiceLabVariant.Quality) ModelCatalog.VoiceLabQuality else ModelCatalog.VoiceLabLowLatency
        VoiceLabConverter(dir(pkg), variant, threads).use { vl ->
            val b = vl.bench(20f)
            VoiceLabResult(label, threads, b.rtf, b.rtfP50, b.rtfP99, b.blockMsP50, b.blockMsP99, vl.algorithmicLatencyMs)
        }
    }

    suspend fun piper(): PiperResult = withContext(Dispatchers.Default) {
        PiperTts(dir(ModelCatalog.PiperEs)).use { tts ->
            tts.synthesizeRaw(SttTestSet.phrases.first())
            var ms = 0.0
            var seconds = 0.0
            for (p in SttTestSet.phrases) {
                val (raw, t) = timed { tts.synthesizeRaw(p) }
                ms += t
                seconds += raw.seconds
            }
            PiperResult(ms, seconds, SttTestSet.phrases.size)
        }
    }

    suspend fun pipeline(): PipelineResult = coroutineScope {
        val piper = PiperTts(dir(ModelCatalog.PiperEs))
        val vl = VoiceLabConverter(dir(ModelCatalog.VoiceLabQuality), VoiceLabVariant.Quality, 1)
        try {
            withContext(Dispatchers.Default) { piper.synthesize(SttTestSet.phrases.first()) }
            withContext(audio) {
                vl.push(FloatArray(BLOCK))
                vl.reset()
            }
            val queue = Channel<FloatArray>(2)
            var piperNs = 0L
            val t0 = System.nanoTime()
            val producer = launch(Dispatchers.Default) {
                for (p in SttTestSet.phrases) {
                    val s = System.nanoTime()
                    val pcm = piper.synthesize(p)
                    piperNs += System.nanoTime() - s
                    queue.send(pcm)
                }
                queue.close()
            }
            var vlNs = 0L
            var firstNs = -1L
            var samples = 0L
            withContext(audio) {
                fun step(f: () -> FloatArray) {
                    val s = System.nanoTime()
                    val out = f()
                    val e = System.nanoTime()
                    vlNs += e - s
                    if (firstNs < 0 && out.isNotEmpty()) firstNs = e - t0
                }
                for (pcm in queue) {
                    samples += pcm.size
                    var i = 0
                    while (i < pcm.size) {
                        val end = min(i + BLOCK, pcm.size)
                        step { vl.push(pcm.copyOfRange(i, end)) }
                        i = end
                    }
                    step { vl.flush() }
                }
            }
            val wallNs = System.nanoTime() - t0
            producer.join()
            PipelineResult(wallNs / 1e6, samples.toDouble() / VOICE_SAMPLE_RATE, piperNs / 1e6, vlNs / 1e6, firstNs / 1e6)
        } finally {
            vl.close()
            piper.close()
        }
    }

    /** Las 10 frases a 16 kHz con Piper; el llamador las cachea. */
    suspend fun synthPhrases(): List<FloatArray> = withContext(Dispatchers.Default) {
        PiperTts(dir(ModelCatalog.PiperEs)).use { tts -> SttTestSet.phrases.map { tts.synthesize(it) } }
    }

    suspend fun stt(candidate: SttCandidate, phrasesAudio: List<FloatArray>): SttResult = withContext(Dispatchers.Default) {
        candidate.create(dir(candidate.pkg)).use { stt ->
            stt.transcribe(phrasesAudio.first())
            val phrases = SttTestSet.phrases.mapIndexed { i, ref ->
                val (text, ms) = timed { stt.transcribe(phrasesAudio[i]) }
                SttPhrase(ref, text, ms, phrasesAudio[i].size.toDouble() / VOICE_SAMPLE_RATE)
            }
            SttResult(stt.name, phrases)
        }
    }

    suspend fun turn(candidate: SttCandidate, firstPhrase: FloatArray): TurnResult = coroutineScope {
        val stt = candidate.create(dir(candidate.pkg))
        val piper = PiperTts(dir(ModelCatalog.PiperEs))
        val vl = VoiceLabConverter(dir(ModelCatalog.VoiceLabQuality), VoiceLabVariant.Quality, 1)
        try {
            val (sttMs, piperMs, pcm) = withContext(Dispatchers.Default) {
                stt.transcribe(firstPhrase)
                piper.synthesize(TURN_SENTENCE)
                val (_, a) = timed { stt.transcribe(firstPhrase) }
                val (pcm, b) = timed { piper.synthesize(TURN_SENTENCE) }
                Triple(a, b, pcm)
            }
            val vlMs = withContext(audio) {
                vl.push(FloatArray(BLOCK))
                vl.reset()
                timed {
                    var i = 0
                    var got = false
                    while (!got && i < pcm.size) {
                        val end = min(i + BLOCK, pcm.size)
                        got = vl.push(pcm.copyOfRange(i, end)).isNotEmpty()
                        i = end
                    }
                    if (!got) vl.flush()
                }.second
            }
            TurnResult(stt.name, sttMs, piperMs, vlMs)
        } finally {
            vl.close()
            piper.close()
            stt.close()
        }
    }
}
