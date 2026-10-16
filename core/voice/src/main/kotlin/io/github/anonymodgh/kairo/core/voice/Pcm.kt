package io.github.anonymodgh.kairo.core.voice

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** PCM `f32` ↔ bytes little-endian, el formato que cruza la frontera con Rust. */
fun FloatArray.toLeBytes(): ByteArray {
    val buf = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
    buf.asFloatBuffer().put(this)
    return buf.array()
}

fun ByteArray.leBytesToFloats(): FloatArray {
    val out = FloatArray(size / 4)
    ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(out)
    return out
}
