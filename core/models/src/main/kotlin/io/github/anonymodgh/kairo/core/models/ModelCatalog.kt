package io.github.anonymodgh.kairo.core.models

object ModelCatalog {
    private const val VOICELAB = "https://github.com/AnonymoDGH/voicelab/releases/download/v0.2.0/"
    private const val SHERPA = "https://github.com/k2-fsa/sherpa-onnx/releases/download/"
    private const val HF = "https://huggingface.co/csukuangfj/"

    private fun hf(repo: String) = "$HF$repo/resolve/main/"

    private fun file(base: String, name: String, sha256: String, bytes: Long) =
        ModelFile(url = base + name, path = name, sha256 = sha256, bytes = bytes)

    private val voiceLabManifest = file(
        VOICELAB, "manifest.json",
        "03fafff1db38d32d198911aea0d0c184a2ac1c398cf46b2f74dba52e38d0ee9a", 3521,
    )
    private val voiceLabVocoder = file(
        VOICELAB, "vocos.onnx",
        "9726e5bc7ad97d7eb9e920626a41e33b2fcc99f3d3ba8102224bf7f4f9596e9d", 33204686,
    )

    val VoiceLabQuality = ModelPackage(
        id = "voicelab-120ms",
        title = "Voz VoiceLab (Calidad)",
        description = "Conversión de voz de mayor calidad, con 120 ms de bloque.",
        license = "Apache-2.0 (VoiceLab, MeanVC2)",
        files = listOf(
            voiceLabManifest,
            file(VOICELAB, "asr_120ms.onnx", "0c45f940506eacc483aafb73b1754ec5f075cfdb65a9110b75da1b5e04ac46d5", 75781495),
            file(VOICELAB, "dit_120ms.onnx", "22ce1df5b86dae8e0d8a69c259d5460c61eb08cd4d2a0b05442f19d529a88d74", 50809912),
            file(VOICELAB, "gtm_120ms.onnx", "d823134655ed55cbf79b32c9768d8962f46199fdea517062dae2fae7b10f3201", 17441890),
            voiceLabVocoder,
        ),
    )

    val VoiceLabLowLatency = ModelPackage(
        id = "voicelab-40ms",
        title = "Voz VoiceLab (baja latencia)",
        description = "Conversión de voz con 40 ms de bloque. Autónomo: no requiere el paquete de Calidad.",
        license = "Apache-2.0 (VoiceLab, MeanVC2)",
        files = listOf(
            voiceLabManifest,
            file(VOICELAB, "asr_40ms.onnx", "ba13d073df8f2592de246beb04367756d028c8ba65c23cf8439a5a1d33b42ac1", 75781495),
            file(VOICELAB, "dit_40ms.onnx", "eee28854d2b3b236f57c7d1656cd4a0398cda2c3ddb89c0266f1b4a51f6dcd54", 50643128),
            file(VOICELAB, "gtm_40ms.onnx", "fc015df1e40bfd8ca30dd155d17f3a1f951c32e8bcdaba732933d8f50bf6e28f", 17441890),
            voiceLabVocoder,
        ),
    )

    val PiperEs = ModelPackage(
        id = "piper-es_ES-davefx",
        title = "Voz base Piper (es_ES davefx)",
        description = "Voz sintética en español para la conversación.",
        license = "CC0 (dataset NabuCasa) · Piper MIT",
        files = listOf(
            ModelFile(
                url = SHERPA + "tts-models/vits-piper-es_ES-davefx-medium.tar.bz2",
                path = "",
                sha256 = "a3f6beb54a9cb893279f72978a22f807a4d9fc9c7848157b524d5cc7b7f58b22",
                bytes = 67184952,
                archive = ArchiveType.TarBz2,
                stripComponents = 1,
            ),
        ),
    )

    val SileroVad = ModelPackage(
        id = "silero-vad",
        title = "Detector de voz (Silero VAD)",
        description = "Detecta cuándo hablas.",
        license = "MIT",
        files = listOf(
            file(SHERPA + "asr-models/", "silero_vad.onnx", "9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6", 643854),
        ),
    )

    private const val WHISPER_TOKENS = "b34b360dbb493e781e479794586d661700670d65564001f23024971d1f2fa126"

    val WhisperBase = ModelPackage(
        id = "stt-whisper-base",
        title = "Whisper base (int8)",
        description = "Reconocimiento de voz offline, rápido y ligero.",
        license = "MIT (OpenAI Whisper)",
        files = hf("sherpa-onnx-whisper-base").let { b ->
            listOf(
                file(b, "base-encoder.int8.onnx", "0b8fb1304b6109976038efff5ace81720e00386f3ff6b54ee8c75291ca0a1e11", 29120534),
                file(b, "base-decoder.int8.onnx", "9759d217388a01b3a4c7c15533201067b48ae819c4daafc8624e64b9409dc02d", 130672026),
                file(b, "base-tokens.txt", WHISPER_TOKENS, 816730),
            )
        },
    )

    val WhisperSmall = ModelPackage(
        id = "stt-whisper-small",
        title = "Whisper small (int8)",
        description = "Reconocimiento de voz offline, más preciso y más pesado.",
        license = "MIT (OpenAI Whisper)",
        files = hf("sherpa-onnx-whisper-small").let { b ->
            listOf(
                file(b, "small-encoder.int8.onnx", "4cbe7b22fa9026b843b60a68640c747de05bafb1a11b57edc0e66c232d9f33a9", 112442483),
                file(b, "small-decoder.int8.onnx", "acad50b5c782696e91b55914cc5ab4f756f1532f76e22aa6fc615f39fb69a8ee", 262226114),
                file(b, "small-tokens.txt", WHISPER_TOKENS, 816730),
            )
        },
    )

    val NemoCtcEs = ModelPackage(
        id = "stt-nemo-ctc-es",
        title = "NeMo FastConformer CTC español (int8)",
        description = "Reconocimiento de voz offline entrenado para español.",
        license = "CC-BY-4.0 (NVIDIA NeMo)",
        files = hf("sherpa-onnx-nemo-fast-conformer-ctc-es-1424-int8").let { b ->
            listOf(
                file(b, "model.int8.onnx", "9539b206ba7cb46231e24eb1f1d7269370bfd45209c549d70e2bfd0e9f3b021a", 131652445),
                file(b, "tokens.txt", "6191b4853e3654f053c42f6fd184ca53b402987aefdd6ff6baa68034d803ee85", 10871),
            )
        },
    )

    val KrokoEs = ModelPackage(
        id = "stt-kroko-es",
        title = "Zipformer streaming español (Kroko)",
        description = "Reconocimiento de voz en streaming, con texto mientras hablas.",
        license = "CC-BY-SA (modelos comunitarios de Kroko/Banafo)",
        files = hf("sherpa-onnx-streaming-zipformer-es-kroko-2025-08-06").let { b ->
            listOf(
                file(b, "encoder.onnx", "2d9f5ef87d1a5257f8a6687e21501c56f3aa2fcbfcfab9364dcc4ce4e06ae81b", 154878102),
                file(b, "decoder.onnx", "d4ce176b94b25f7acc88717bc3f704fcf5d6e131aaac2e0cabab3885541181ee", 617488),
                file(b, "joiner.onnx", "dae35df88d676e320fcdb99217328e66dcf722bf11b0f2459e14ddb5b982ded5", 336817),
                file(b, "tokens.txt", "1be5e0a58e05d06d327df4c6b7b5e4f8aba01da6981eb016fcaceafc6a56680f", 6385),
            )
        },
    )

    val all = listOf(
        VoiceLabQuality, VoiceLabLowLatency, PiperEs, SileroVad,
        WhisperBase, WhisperSmall, NemoCtcEs, KrokoEs,
    )
}
