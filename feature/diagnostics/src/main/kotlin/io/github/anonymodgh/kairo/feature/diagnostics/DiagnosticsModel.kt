package io.github.anonymodgh.kairo.feature.diagnostics

import io.github.anonymodgh.kairo.feature.diagnostics.stt.Wer
import io.github.anonymodgh.kairo.feature.diagnostics.stt.WerResult
import java.util.Locale
import kotlin.math.ceil

/** Lógica pura de Diagnóstico (sin Android): criterios de KAIRO_PLAN §6 y §10, resultados e informe. */

enum class BenchId { VoiceLab1, VoiceLab2, VoiceLabLow, Piper, Pipeline, Stt, Turn, Fps }

enum class Verdict(val icon: String) { Ok("✅"), Warn("⚠️"), Fail("❌") }

object Criteria {
    const val VOICELAB_OK = 0.6
    const val VOICELAB_WARN = 0.9
    const val PIPER_RTF = 0.3
    const val PIPELINE_RTF = 0.8
    const val STT_MS_4S = 700.0
    const val STT_WER = 0.15
    const val STT_FALLBACK_MS_4S = 1200.0
    const val REFERENCE_PHRASE_S = 4.0
}

fun voiceLabVerdict(rtfP99: Double): Verdict = when {
    rtfP99 < Criteria.VOICELAB_OK -> Verdict.Ok
    rtfP99 < Criteria.VOICELAB_WARN -> Verdict.Warn
    else -> Verdict.Fail
}

fun rtfVerdict(rtf: Double, limit: Double): Verdict = if (rtf < limit) Verdict.Ok else Verdict.Fail

/** Tiempo medido normalizado a una frase de 4 s: suma(ms) / suma(audio s) * 4. */
fun normalizeTo4s(totalMs: Double, totalAudioSeconds: Double): Double =
    if (totalAudioSeconds <= 0.0) 0.0 else totalMs / totalAudioSeconds * Criteria.REFERENCE_PHRASE_S

/** Percentil por rango más cercano; [p] en 0..100. */
fun percentile(values: List<Double>, p: Double): Double {
    if (values.isEmpty()) return 0.0
    val sorted = values.sorted()
    val rank = ceil(p / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
    return sorted[rank - 1]
}

fun fmt(v: Double, decimals: Int = 2): String = String.format(Locale.ROOT, "%.${decimals}f", v)

data class VoiceLabResult(
    val label: String,
    val threads: Int,
    val rtf: Double,
    val rtfP50: Double,
    val rtfP99: Double,
    val blockMsP50: Double,
    val blockMsP99: Double,
    val latencyMs: Double,
) {
    val verdict: Verdict get() = voiceLabVerdict(rtfP99)
    val hint: String
        get() = when {
            verdict == Verdict.Warn && threads == 1 -> "prueba 2 hilos"
            verdict == Verdict.Warn -> "justo, anótalo"
            verdict == Verdict.Fail -> "no llega a tiempo real"
            else -> ""
        }
}

data class PiperResult(val synthMs: Double, val audioSeconds: Double, val phrases: Int) {
    val rtf: Double get() = if (audioSeconds <= 0.0) 0.0 else synthMs / 1000.0 / audioSeconds
    val verdict: Verdict get() = rtfVerdict(rtf, Criteria.PIPER_RTF)
}

data class PipelineResult(
    val wallMs: Double,
    val audioSeconds: Double,
    val piperBusyMs: Double,
    val voiceLabBusyMs: Double,
    val firstAudioMs: Double,
) {
    private fun per(ms: Double) = if (audioSeconds <= 0.0) 0.0 else ms / 1000.0 / audioSeconds
    val rtf: Double get() = per(wallMs)
    val piperLoad: Double get() = per(piperBusyMs)
    val voiceLabLoad: Double get() = per(voiceLabBusyMs)
    val verdict: Verdict get() = rtfVerdict(rtf, Criteria.PIPELINE_RTF)
}

data class SttPhrase(val reference: String, val hypothesis: String, val ms: Double, val audioSeconds: Double) {
    val wer: WerResult get() = Wer.compute(reference, hypothesis)
}

data class SttResult(val name: String, val phrases: List<SttPhrase>) {
    val meanMs: Double get() = if (phrases.isEmpty()) 0.0 else phrases.sumOf { it.ms } / phrases.size
    val maxMs: Double get() = phrases.maxOfOrNull { it.ms } ?: 0.0
    val ms4s: Double get() = normalizeTo4s(phrases.sumOf { it.ms }, phrases.sumOf { it.audioSeconds })
    val wer: Double get() = Wer.aggregate(phrases.map { it.wer })
    val meetsBoth: Boolean get() = ms4s <= Criteria.STT_MS_4S && wer <= Criteria.STT_WER
}

data class SttWinner(val result: SttResult, val meetsBoth: Boolean)

/** Regla de §6: gana quien cumple ambos criterios (menor WER); si ninguno, menor WER con ≤ 1,2 s. */
fun sttWinner(results: List<SttResult>): SttWinner? {
    results.filter { it.meetsBoth }.minByOrNull { it.wer }?.let { return SttWinner(it, true) }
    return results.filter { it.ms4s <= Criteria.STT_FALLBACK_MS_4S }.minByOrNull { it.wer }?.let { SttWinner(it, false) }
}

data class TurnResult(val sttName: String, val sttMs: Double, val piperMs: Double, val voiceLabMs: Double) {
    val totalMs: Double get() = sttMs + piperMs + voiceLabMs
}

data class FpsResult(val frames: Int, val avgFps: Double, val jankPct: Double, val p99FrameMs: Double, val refreshHz: Double)

/** [frameNanos]: marcas de tiempo de los callbacks de Choreographer; jank = intervalo > 1,5 × el de refresco. */
fun fpsStats(frameNanos: LongArray, refreshHz: Double): FpsResult? {
    if (frameNanos.size < 2 || refreshHz <= 0.0) return null
    val intervalsMs = (1 until frameNanos.size).map { (frameNanos[it] - frameNanos[it - 1]) / 1e6 }
    val totalS = (frameNanos.last() - frameNanos.first()) / 1e9
    if (totalS <= 0.0) return null
    val limit = 1.5 * 1000.0 / refreshHz
    return FpsResult(
        frames = frameNanos.size,
        avgFps = intervalsMs.size / totalS,
        jankPct = intervalsMs.count { it > limit } * 100.0 / intervalsMs.size,
        p99FrameMs = percentile(intervalsMs, 99.0),
        refreshHz = refreshHz,
    )
}

data class BenchResults(
    val voiceLab: Map<BenchId, VoiceLabResult> = emptyMap(),
    val piper: PiperResult? = null,
    val pipeline: PipelineResult? = null,
    val stt: List<SttResult> = emptyList(),
    val turn: TurnResult? = null,
    val fps: FpsResult? = null,
    val errors: Map<BenchId, String> = emptyMap(),
)

object ReportBuilder {
    const val NOT_RUN = "(no ejecutado)"

    fun build(version: String, device: List<Pair<String, String>>, r: BenchResults): String = buildString {
        appendLine("Kairo $version · informe de diagnóstico")
        appendLine()
        appendLine("## Dispositivo")
        device.forEach { (k, v) -> appendLine("$k: $v") }

        section("## VoiceLab (criterio: RTF p99 < 0,6)", r, BenchId.VoiceLab1, BenchId.VoiceLab2, BenchId.VoiceLabLow) {
            if (r.voiceLab.isEmpty()) appendLine(NOT_RUN)
            r.voiceLab.values.forEach { v ->
                appendLine("${v.label}: ${v.verdict.icon} ${v.hint}".trimEnd())
                appendLine("  RTF media ${fmt(v.rtf)} · p50 ${fmt(v.rtfP50)} · p99 ${fmt(v.rtfP99)}")
                appendLine("  ms/bloque p50 ${fmt(v.blockMsP50, 1)} · p99 ${fmt(v.blockMsP99, 1)} · latencia algorítmica ${fmt(v.latencyMs, 0)} ms")
            }
        }

        section("## Piper (criterio: RTF < 0,3)", r, BenchId.Piper) {
            r.piper?.let {
                appendLine("${it.verdict.icon} RTF ${fmt(it.rtf)} (${it.phrases} frases, ${fmt(it.audioSeconds, 1)} s de audio, ${fmt(it.synthMs, 0)} ms)")
            } ?: appendLine(NOT_RUN)
        }

        section("## VoiceLab + Piper a la vez (criterio: RTF combinado < 0,8)", r, BenchId.Pipeline) {
            r.pipeline?.let {
                appendLine("${it.verdict.icon} RTF combinado ${fmt(it.rtf)} (${fmt(it.wallMs, 0)} ms para ${fmt(it.audioSeconds, 1)} s de audio)")
                appendLine("  Piper ocupado ${fmt(it.piperBusyMs, 0)} ms (${fmt(it.piperLoad)} x audio) · VoiceLab ${fmt(it.voiceLabBusyMs, 0)} ms (${fmt(it.voiceLabLoad)} x audio)")
                appendLine("  Primer audio tras ${fmt(it.firstAudioMs, 0)} ms")
            } ?: appendLine(NOT_RUN)
        }

        section("## STT (criterio: frase de 4 s ≤ 700 ms y WER ≤ 15 %; audio sintético de Piper)", r, BenchId.Stt) {
            if (r.stt.isEmpty()) appendLine(NOT_RUN)
            r.stt.forEach { s ->
                val ok = if (s.meetsBoth) " ✅" else ""
                appendLine("${s.name}$ok: media ${fmt(s.meanMs, 0)} ms · 4 s ${fmt(s.ms4s, 0)} ms · máx ${fmt(s.maxMs, 0)} ms · WER ${fmt(s.wer * 100, 1)} %")
            }
            val winner = sttWinner(r.stt)
            if (winner != null) {
                appendLine("Ganador: ${winner.result.name}" + if (winner.meetsBoth) " (cumple ambos criterios)" else " (menor WER con ≤ 1,2 s; no cumple ambos)")
            } else if (r.stt.isNotEmpty()) {
                appendLine("Ganador: ninguno (todos tardan más de 1,2 s)")
            }
            r.stt.forEach { s ->
                appendLine()
                appendLine("Frases · ${s.name}")
                s.phrases.forEachIndexed { i, p ->
                    appendLine("${i + 1}. REF: ${p.reference}")
                    appendLine("   HIP: ${p.hypothesis} [${fmt(p.ms, 0)} ms, ${fmt(p.audioSeconds, 1)} s, WER ${fmt(p.wer.wer * 100, 0)} %]")
                }
            }
        }

        section("## Turno sin LLM (el primer token del LLM, 300–800 ms, no está incluido)", r, BenchId.Turn) {
            r.turn?.let {
                appendLine("STT (${it.sttName}) ${fmt(it.sttMs, 0)} ms + Piper ${fmt(it.piperMs, 0)} ms + VoiceLab ${fmt(it.voiceLabMs, 0)} ms = ${fmt(it.totalMs, 0)} ms")
            } ?: appendLine(NOT_RUN)
        }

        section("## Fluidez de la UI", r, BenchId.Fps) {
            r.fps?.let {
                appendLine("${fmt(it.avgFps, 1)} fps de media a ${fmt(it.refreshHz, 0)} Hz · jank ${fmt(it.jankPct, 1)} % · fotograma p99 ${fmt(it.p99FrameMs, 1)} ms (${it.frames} fotogramas)")
            } ?: appendLine(NOT_RUN)
        }
    }.trimEnd()

    private fun StringBuilder.section(title: String, r: BenchResults, vararg ids: BenchId, body: StringBuilder.() -> Unit) {
        appendLine()
        appendLine(title)
        ids.forEach { id -> r.errors[id]?.let { appendLine("ERROR ($id): $it") } }
        body()
    }
}
