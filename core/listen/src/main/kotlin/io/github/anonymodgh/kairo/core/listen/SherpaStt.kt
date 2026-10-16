package io.github.anonymodgh.kairo.core.listen

import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineNemoEncDecCtcModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import io.github.anonymodgh.kairo.core.voice.NativeRuntime
import java.io.File

private fun File.need(name: String): String =
    File(this, name).also { require(it.exists()) { "Falta $name en ${this.name}" } }.absolutePath

/** Reconocedor por frases completas (Whisper, NeMo CTC). */
class SherpaOfflineStt private constructor(
    override val name: String,
    config: OfflineRecognizerConfig,
) : SpeechToText {
    private val recognizer: OfflineRecognizer

    init {
        NativeRuntime.ensureLoaded()
        recognizer = OfflineRecognizer(assetManager = null, config = config)
    }

    override val supportsPartials = false

    override fun transcribe(pcm16k: FloatArray): String {
        val stream = recognizer.createStream()
        try {
            stream.acceptWaveform(pcm16k, LISTEN_SAMPLE_RATE)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    override fun close() = recognizer.release()

    companion object {
        /** `prefix` = "base" o "small": archivos `<prefix>-encoder.int8.onnx`, etc. */
        fun whisper(dir: File, prefix: String, threads: Int = 2) = SherpaOfflineStt(
            name = "Whisper $prefix int8",
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = LISTEN_SAMPLE_RATE, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = dir.need("$prefix-encoder.int8.onnx"),
                        decoder = dir.need("$prefix-decoder.int8.onnx"),
                        language = "es",
                        task = "transcribe",
                    ),
                    tokens = dir.need("$prefix-tokens.txt"),
                    numThreads = threads,
                    provider = "cpu",
                ),
            ),
        )

        fun nemoCtc(dir: File, threads: Int = 2) = SherpaOfflineStt(
            name = "NeMo FastConformer CTC es",
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = LISTEN_SAMPLE_RATE, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    nemo = OfflineNemoEncDecCtcModelConfig(model = dir.need("model.int8.onnx")),
                    tokens = dir.need("tokens.txt"),
                    numThreads = threads,
                    provider = "cpu",
                ),
            ),
        )
    }
}

/** Reconocedor en streaming (zipformer transducer): da parciales mientras hablas. */
class SherpaStreamingStt(dir: File, threads: Int = 2) : SpeechToText {
    private val recognizer: OnlineRecognizer

    init {
        NativeRuntime.ensureLoaded()
        recognizer = OnlineRecognizer(
            assetManager = null,
            config = OnlineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = LISTEN_SAMPLE_RATE, featureDim = 80),
                modelConfig = OnlineModelConfig(
                    transducer = OnlineTransducerModelConfig(
                        encoder = dir.need("encoder.onnx"),
                        decoder = dir.need("decoder.onnx"),
                        joiner = dir.need("joiner.onnx"),
                    ),
                    tokens = dir.need("tokens.txt"),
                    numThreads = threads,
                    provider = "cpu",
                ),
                endpointConfig = EndpointConfig(),
                enableEndpoint = false,
            ),
        )
    }

    override val name = "Zipformer streaming es (Kroko)"
    override val supportsPartials = true

    override fun transcribe(pcm16k: FloatArray): String {
        val stream = recognizer.createStream("")
        try {
            stream.acceptWaveform(pcm16k, LISTEN_SAMPLE_RATE)
            // Cola de 2 s de silencio: con menos, el modelo se come las últimas palabras (medido en host).
            stream.acceptWaveform(FloatArray(LISTEN_SAMPLE_RATE * 2), LISTEN_SAMPLE_RATE)
            stream.inputFinished()
            while (recognizer.isReady(stream)) recognizer.decode(stream)
            return recognizer.getResult(stream).text.trim()
        } finally {
            stream.release()
        }
    }

    override fun close() = recognizer.release()
}
