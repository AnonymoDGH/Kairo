package io.github.anonymodgh.kairo.core.voice

import io.github.anonymodgh.kairo.core.voice.engine.BenchReport
import io.github.anonymodgh.kairo.core.voice.engine.VoiceEngine
import java.io.File

/** Conversión de voz en streaming, 16 kHz mono (KAIRO_PLAN §4). */
interface VoiceConverter : AutoCloseable {
    fun loadVoice(vlvoice: ByteArray)
    fun push(pcm16k: FloatArray): FloatArray
    fun flush(): FloatArray
    fun reset()
}

enum class VoiceLabVariant(val key: String) { Quality("120ms"), LowLatency("40ms") }

class VoiceLabConverter(modelDir: File, variant: VoiceLabVariant = VoiceLabVariant.Quality, threads: Int = 1) : VoiceConverter {
    private val engine: VoiceEngine

    init {
        NativeRuntime.ensureLoaded()
        engine = VoiceEngine(modelDir.absolutePath, variant.key, threads.toUInt())
    }

    val algorithmicLatencyMs: Double get() = engine.algorithmicLatencyMs()

    override fun loadVoice(vlvoice: ByteArray) = engine.loadVoice(vlvoice)
    override fun push(pcm16k: FloatArray): FloatArray = engine.push(pcm16k.toLeBytes()).leBytesToFloats()
    override fun flush(): FloatArray = engine.flush().leBytesToFloats()
    override fun reset() = engine.reset()
    fun bench(seconds: Float): BenchReport = engine.bench(seconds)
    override fun close() = engine.close()
}
