package io.github.anonymodgh.kairo.core.voice

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import io.github.anonymodgh.kairo.core.voice.engine.resample
import java.io.File

const val VOICE_SAMPLE_RATE = 16_000

/** Síntesis de texto a voz; la salida ya está remuestreada a 16 kHz (KAIRO_PLAN §4). */
interface Tts : AutoCloseable {
    fun synthesize(text: String): FloatArray
}

/** Audio tal cual sale del modelo, para medir el RTF de Piper sin contar el remuestreo. */
class RawAudio(val samples: FloatArray, val sampleRate: Int) {
    val seconds: Double get() = samples.size.toDouble() / sampleRate
}

/**
 * Piper (VITS) vía sherpa-onnx. [modelDir] es el paquete descomprimido: `*.onnx`, `tokens.txt`
 * y `espeak-ng-data/`.
 */
class PiperTts(modelDir: File, threads: Int = 1, private val speed: Float = 1f) : Tts {
    private val tts: OfflineTts

    init {
        NativeRuntime.ensureLoaded()
        val model = modelDir.listFiles { f -> f.extension == "onnx" }?.firstOrNull()
            ?: error("No hay modelo .onnx en ${modelDir.name}")
        tts = OfflineTts(
            assetManager = null,
            config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = model.absolutePath,
                        tokens = File(modelDir, "tokens.txt").absolutePath,
                        dataDir = File(modelDir, "espeak-ng-data").absolutePath,
                    ),
                    numThreads = threads,
                    provider = "cpu",
                ),
            ),
        )
    }

    val sampleRate: Int get() = tts.sampleRate()

    fun synthesizeRaw(text: String): RawAudio {
        val audio = tts.generate(text, 0, speed)
        return RawAudio(audio.samples, audio.sampleRate)
    }

    override fun synthesize(text: String): FloatArray {
        val raw = synthesizeRaw(text)
        if (raw.sampleRate == VOICE_SAMPLE_RATE) return raw.samples
        return resample(raw.samples.toLeBytes(), raw.sampleRate.toUInt(), VOICE_SAMPLE_RATE.toUInt()).leBytesToFloats()
    }

    override fun close() = tts.release()
}
