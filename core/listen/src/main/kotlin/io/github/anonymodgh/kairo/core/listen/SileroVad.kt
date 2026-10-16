package io.github.anonymodgh.kairo.core.listen

import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import io.github.anonymodgh.kairo.core.voice.NativeRuntime
import java.io.File

/** Silero VAD vía sherpa-onnx. Fin de habla tras [minSilenceSeconds] de silencio (KAIRO_PLAN §6). */
class SileroVad(model: File, minSilenceSeconds: Float = 0.5f, threshold: Float = 0.5f) : AutoCloseable {
    private val vad: Vad

    init {
        NativeRuntime.ensureLoaded()
        vad = Vad(
            assetManager = null,
            config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = model.absolutePath,
                    threshold = threshold,
                    minSilenceDuration = minSilenceSeconds,
                    minSpeechDuration = 0.25f,
                    windowSize = WINDOW,
                ),
                sampleRate = LISTEN_SAMPLE_RATE,
                numThreads = 1,
                provider = "cpu",
            ),
        )
    }

    /** Segmentos de voz (inicio en muestras, audio) encontrados en un clip completo. */
    fun segments(pcm16k: FloatArray): List<Pair<Int, FloatArray>> {
        vad.reset()
        var i = 0
        while (i + WINDOW <= pcm16k.size) {
            vad.acceptWaveform(pcm16k.copyOfRange(i, i + WINDOW))
            i += WINDOW
        }
        vad.flush()
        val out = mutableListOf<Pair<Int, FloatArray>>()
        while (!vad.empty()) {
            val s = vad.front()
            out += s.start to s.samples
            vad.pop()
        }
        return out
    }

    override fun close() = vad.release()

    companion object {
        const val WINDOW = 512
    }
}
