package io.github.anonymodgh.kairo.feature.diagnostics.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WerTest {
    @Test fun identicalIsZero() {
        val r = Wer.compute("hola mundo", "hola mundo")
        assertEquals(0.0, r.wer, 0.0)
        assertEquals(2, r.referenceWords)
    }

    @Test fun oneSubstitution() {
        val r = Wer.compute("el gato duerme hoy", "el perro duerme hoy")
        assertEquals(WerResult(1, 0, 0, 4), r)
        assertEquals(0.25, r.wer, 1e-9)
    }

    @Test fun oneDeletion() {
        assertEquals(WerResult(0, 1, 0, 4), Wer.compute("el gato duerme hoy", "el gato hoy"))
    }

    @Test fun oneInsertion() {
        assertEquals(WerResult(0, 0, 1, 3), Wer.compute("el gato duerme", "el gato duerme mucho"))
    }

    @Test fun punctuationCaseAndAccentsIgnored() {
        val r = Wer.compute("¿Qué tal, Andrés? ¡Bien!", "que tal andres bien")
        assertEquals(0.0, r.wer, 0.0)
    }

    @Test fun enyeIsKept() {
        assertEquals(listOf("nino"), SpanishText.normalize("niño").map { it.replace('ñ', 'n') })
        assertTrue(Wer.compute("niño", "nino").wer > 0.0)
    }

    @Test fun digitsEqualWords() {
        assertEquals(0.0, Wer.compute("tengo 25 años", "tengo veinticinco años").wer, 0.0)
    }

    @Test fun numberWords() {
        val expected = mapOf(
            0 to "cero", 15 to "quince", 16 to "dieciseis", 21 to "veintiuno", 30 to "treinta",
            99 to "noventa y nueve", 100 to "cien", 101 to "ciento uno", 115 to "ciento quince",
            500 to "quinientos", 999 to "novecientos noventa y nueve", 1000 to "mil",
            1990 to "mil novecientos noventa", 2026 to "dos mil veintiseis",
        )
        for ((n, words) in expected) {
            assertEquals("n=$n", SpanishText.normalize(words), SpanishText.normalize(n.toString()))
        }
    }

    @Test fun aggregateIsTotalErrorsOverTotalWords() {
        val a = WerResult(1, 0, 0, 4)
        val b = WerResult(0, 1, 1, 6)
        assertEquals(3.0 / 10, Wer.aggregate(listOf(a, b)), 1e-9)
    }

    @Test fun testSetPhrasesAreUsable() {
        assertEquals(10, SttTestSet.phrases.size)
        SttTestSet.phrases.forEach { assertTrue(it, SpanishText.normalize(it).size >= 6) }
    }

    @Test
    fun articleNumberEquivalence() {
        assertEquals(0.0, Wer.compute("un kilo de naranjas", "1 kilo de naranjas").wer, 0.0)
        assertEquals("ciento uno", SpanishText.numberToWords(101))
        assertEquals("veintiuno", SpanishText.numberToWords(21))
    }
}
