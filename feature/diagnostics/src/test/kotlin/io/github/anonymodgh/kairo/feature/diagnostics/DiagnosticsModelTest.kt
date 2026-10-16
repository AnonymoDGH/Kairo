package io.github.anonymodgh.kairo.feature.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsModelTest {
    private fun stt(name: String, ms: Double, hyp: String, ref: String = "uno dos tres cuatro") =
        SttResult(name, listOf(SttPhrase(ref, hyp, ms, 4.0)))

    @Test fun voiceLabVerdictThresholds() {
        assertEquals(Verdict.Ok, voiceLabVerdict(0.59))
        assertEquals(Verdict.Warn, voiceLabVerdict(0.6))
        assertEquals(Verdict.Warn, voiceLabVerdict(0.89))
        assertEquals(Verdict.Fail, voiceLabVerdict(0.9))
    }

    @Test fun rtfVerdictIsStrict() {
        assertEquals(Verdict.Ok, rtfVerdict(0.29, Criteria.PIPER_RTF))
        assertEquals(Verdict.Fail, rtfVerdict(0.3, Criteria.PIPER_RTF))
        assertEquals(Verdict.Ok, rtfVerdict(0.79, Criteria.PIPELINE_RTF))
    }

    @Test fun normalizesTo4Seconds() {
        assertEquals(500.0, normalizeTo4s(1000.0, 8.0), 1e-9)
        assertEquals(0.0, normalizeTo4s(1000.0, 0.0), 0.0)
        val r = SttResult("x", listOf(SttPhrase("a", "a", 300.0, 2.0), SttPhrase("a", "a", 900.0, 6.0)))
        assertEquals(600.0, r.ms4s, 1e-9)
        assertEquals(600.0, r.meanMs, 1e-9)
        assertEquals(900.0, r.maxMs, 1e-9)
    }

    @Test fun percentileNearestRank() {
        val v = (1..100).map { it.toDouble() }
        assertEquals(50.0, percentile(v, 50.0), 0.0)
        assertEquals(99.0, percentile(v, 99.0), 0.0)
        assertEquals(0.0, percentile(emptyList(), 50.0), 0.0)
    }

    @Test fun winnerPrefersBothCriteriaWithLowestWer() {
        val fastBad = stt("rapido", 500.0, "uno dos tres")
        val fastGood = stt("bueno", 600.0, "uno dos tres cuatro")
        val slowBest = stt("lento", 1000.0, "uno dos tres cuatro")
        val w = sttWinner(listOf(fastBad, slowBest, fastGood))!!
        assertEquals("bueno", w.result.name)
        assertTrue(w.meetsBoth)
    }

    @Test fun winnerFallsBackToLowestWerUnder1200() {
        val a = stt("a", 1000.0, "uno dos tres")
        val b = stt("b", 1100.0, "uno dos tres cuatro")
        val c = stt("c", 2000.0, "uno dos tres cuatro")
        val w = sttWinner(listOf(a, b, c))!!
        assertEquals("b", w.result.name)
        assertEquals(false, w.meetsBoth)
        assertNull(sttWinner(listOf(c)))
        assertNull(sttWinner(emptyList()))
    }

    @Test fun pipelineLoadsAreRelativeToAudio() {
        val p = PipelineResult(wallMs = 8000.0, audioSeconds = 10.0, piperBusyMs = 2000.0, voiceLabBusyMs = 6000.0, firstAudioMs = 400.0)
        assertEquals(0.8, p.rtf, 1e-9)
        assertEquals(Verdict.Fail, p.verdict)
        assertEquals(0.2, p.piperLoad, 1e-9)
        assertEquals(0.6, p.voiceLabLoad, 1e-9)
    }

    @Test fun fpsStatsCountsJank() {
        val ms = 1_000_000L
        // 60 Hz: 16,7 ms; un salto de 40 ms (> 25 ms) entre 5 intervalos
        val t = longArrayOf(0, 16 * ms, 32 * ms, 72 * ms, 88 * ms, 104 * ms)
        val r = fpsStats(t, 60.0)!!
        assertEquals(20.0, r.jankPct, 1e-9)
        assertEquals(5 / 0.104, r.avgFps, 1e-6)
        assertEquals(40.0, r.p99FrameMs, 1e-9)
        assertNull(fpsStats(longArrayOf(0), 60.0))
    }

    @Test fun hintSuggestsTwoThreadsOnlyWhenOneThread() {
        val r = VoiceLabResult("x", 1, 0.5, 0.5, 0.7, 10.0, 20.0, 305.0)
        assertEquals("prueba 2 hilos", r.hint)
        assertEquals("justo, anótalo", r.copy(threads = 2).hint)
    }

    private val device = listOf("Modelo" to "Xiaomi Redmi Note 14", "SoC" to "MT6789")

    @Test fun reportHasHeaderAndEverySection() {
        val full = BenchResults(
            voiceLab = mapOf(BenchId.VoiceLab1 to VoiceLabResult("VoiceLab (Calidad) · 1 hilo", 1, 0.5, 0.5, 0.55, 40.0, 60.0, 305.0)),
            piper = PiperResult(2000.0, 20.0, 10),
            pipeline = PipelineResult(9000.0, 20.0, 3000.0, 6000.0, 700.0),
            stt = listOf(stt("Whisper base int8", 600.0, "uno dos tres cuatro")),
            turn = TurnResult("Whisper base int8", 600.0, 300.0, 400.0),
            fps = FpsResult(300, 59.5, 2.0, 20.0, 60.0),
        )
        val text = ReportBuilder.build("0.1.0", device, full)
        assertTrue(text.startsWith("Kairo 0.1.0 · informe de diagnóstico"))
        listOf("## Dispositivo", "## VoiceLab", "## Piper", "## VoiceLab + Piper", "## STT", "## Turno sin LLM", "## Fluidez de la UI")
            .forEach { assertTrue(it, text.contains(it)) }
        assertTrue(text.contains("Redmi Note 14"))
        assertTrue(text.contains("REF: uno dos tres cuatro"))
        assertTrue(text.contains("Ganador: Whisper base int8"))
        assertTrue(text.contains("= 1300 ms"))
        assertTrue(!text.contains(ReportBuilder.NOT_RUN))
    }

    @Test fun reportMarksMissingAndErrors() {
        val r = BenchResults(errors = mapOf(BenchId.Piper to "UnsatisfiedLinkError: boom"))
        val text = ReportBuilder.build("0.1.0", device, r)
        assertNotNull(text)
        assertTrue(text.contains("ERROR (Piper): UnsatisfiedLinkError: boom"))
        assertTrue(text.contains(ReportBuilder.NOT_RUN))
    }
}
