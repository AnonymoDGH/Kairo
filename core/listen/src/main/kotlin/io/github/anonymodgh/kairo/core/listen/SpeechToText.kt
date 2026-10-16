package io.github.anonymodgh.kairo.core.listen

/** Reconocimiento de voz local, 16 kHz mono (KAIRO_PLAN §6). Una implementación por candidato. */
interface SpeechToText : AutoCloseable {
    val name: String
    val supportsPartials: Boolean
    fun transcribe(pcm16k: FloatArray): String
}

const val LISTEN_SAMPLE_RATE = 16_000
