package io.github.anonymodgh.kairo.core.voice

import io.github.anonymodgh.kairo.core.voice.engine.initOnnxRuntime

/**
 * Carga una sola vez el ONNX Runtime que trae sherpa-onnx y lo comparte con el motor Rust
 * (`ort` con `load-dynamic`). Así hay una única `libonnxruntime.so` en el APK (KAIRO_PLAN §5.3).
 */
object NativeRuntime {
    @Volatile private var loaded = false

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return
        System.loadLibrary("onnxruntime")
        System.loadLibrary("kairo_voice")
        initOnnxRuntime("libonnxruntime.so")
        loaded = true
    }
}
