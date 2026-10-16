package io.github.anonymodgh.kairo.feature.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.PowerManager
import android.view.Choreographer
import android.view.Display
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private fun yesNo(b: Boolean) = if (b) "sí" else "no"

private val ThermalNames = listOf("ninguno", "ligero", "moderado", "severo", "crítico", "emergencia", "apagado")

fun refreshRateHz(context: Context): Double =
    context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate?.toDouble() ?: 60.0

fun appVersion(context: Context): String {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
    }
    return info.versionName ?: "?"
}

fun collectDeviceInfo(context: Context): List<Pair<String, String>> {
    val power = context.getSystemService(PowerManager::class.java)
    val mem = ActivityManager.MemoryInfo().also { context.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
    val headroom = power.getThermalHeadroom(10)
    return listOf(
        "Modelo" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "SoC" to Build.SOC_MODEL,
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Núcleos" to Runtime.getRuntime().availableProcessors().toString(),
        "RAM total" to "${mem.totalMem / (1024 * 1024)} MB",
        "Cancelación de eco" to yesNo(AcousticEchoCanceler.isAvailable()),
        "Supresor de ruido" to yesNo(NoiseSuppressor.isAvailable()),
        "Control de ganancia" to yesNo(AutomaticGainControl.isAvailable()),
        "Ahorro de energía" to yesNo(power.isPowerSaveMode),
        "Estado térmico" to (ThermalNames.getOrNull(power.currentThermalStatus) ?: power.currentThermalStatus.toString()),
        "Margen térmico (10 s)" to if (headroom.isNaN()) "no disponible" else fmt(headroom.toDouble()),
        "Rendimiento sostenido" to yesNo(power.isSustainedPerformanceModeSupported),
        "Pantalla" to "${fmt(refreshRateHz(context), 0)} Hz",
    )
}

/** Marcas de tiempo de los callbacks de Choreographer durante [durationMs]; llamar desde el hilo principal. */
suspend fun captureFrames(durationMs: Long): LongArray = suspendCancellableCoroutine { cont ->
    val choreographer = Choreographer.getInstance()
    val times = ArrayList<Long>()
    var startNs = 0L
    val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (startNs == 0L) startNs = frameTimeNanos
            times += frameTimeNanos
            if ((frameTimeNanos - startNs) / 1_000_000 >= durationMs) cont.resume(times.toLongArray())
            else choreographer.postFrameCallback(this)
        }
    }
    choreographer.postFrameCallback(callback)
    cont.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
}
