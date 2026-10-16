package io.github.anonymodgh.kairo.feature.diagnostics.stt

import java.text.Normalizer
import java.util.Locale

/** Normalización de texto en español para comparar transcripciones. */
object SpanishText {
    private val units = listOf(
        "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
        "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete", "dieciocho", "diecinueve",
        "veinte", "veintiuno", "veintidós", "veintitrés", "veinticuatro", "veinticinco", "veintiséis",
        "veintisiete", "veintiocho", "veintinueve",
    )
    private val tens = listOf("", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa")
    private val hundreds = listOf(
        "", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
        "seiscientos", "setecientos", "ochocientos", "novecientos",
    )

    /** Número 0..9999 en palabras (con acentos; `normalize` los quita después). */
    fun numberToWords(n: Int): String {
        require(n in 0..9999) { "Fuera de rango: $n" }
        if (n < 30) return units[n]
        if (n < 100) return tens[n / 10] + if (n % 10 == 0) "" else " y ${units[n % 10]}"
        if (n == 100) return "cien"
        if (n < 1000) return hundreds[n / 100] + if (n % 100 == 0) "" else " ${numberToWords(n % 100)}"
        val thousands = if (n / 1000 == 1) "mil" else "${units[n / 1000]} mil"
        return thousands + if (n % 1000 == 0) "" else " ${numberToWords(n % 1000)}"
    }

    /** Minúsculas, sin puntuación, números a palabras y sin diacríticos (solo para comparar; la ñ se conserva). */
    fun normalize(s: String): List<String> {
        val lower = s.lowercase(Locale.forLanguageTag("es"))
        val spaced = StringBuilder()
        var i = 0
        while (i < lower.length) {
            val c = lower[i]
            if (c.isDigit()) {
                var j = i
                while (j < lower.length && lower[j].isDigit()) j++
                val digits = lower.substring(i, j)
                spaced.append(' ').append(if (digits.length <= 4) numberToWords(digits.toInt()) else digits).append(' ')
                i = j
            } else {
                spaced.append(if (c.isLetter() || c.isWhitespace() || c == '̃' || c == '́') c else ' ')
                i++
            }
        }
        val decomposed = Normalizer.normalize(spaced, Normalizer.Form.NFD)
        val out = StringBuilder()
        for (k in decomposed.indices) {
            val c = decomposed[k]
            val isMark = Character.getType(c) == Character.NON_SPACING_MARK.toInt()
            // El tilde de la ñ se conserva; el resto de marcas se descarta.
            if (!isMark || (c == '̃' && k > 0 && decomposed[k - 1] == 'n')) out.append(c)
        }
        // "1 kilo" (Whisper) frente a "un kilo": un/una/uno cuentan igual.
        return Normalizer.normalize(out, Normalizer.Form.NFC).split(Regex("\\s+")).filter { it.isNotEmpty() }
            .map { if (it == "una" || it == "uno") "un" else it }
    }
}

data class WerResult(val substitutions: Int, val deletions: Int, val insertions: Int, val referenceWords: Int) {
    val errors: Int get() = substitutions + deletions + insertions
    val wer: Double get() = if (referenceWords == 0) errors.toDouble() else errors.toDouble() / referenceWords
}

object Wer {
    /** WER a nivel de palabra: Levenshtein con retroceso para contar S/D/I. */
    fun compute(reference: String, hypothesis: String): WerResult {
        val ref = SpanishText.normalize(reference)
        val hyp = SpanishText.normalize(hypothesis)
        val d = Array(ref.size + 1) { IntArray(hyp.size + 1) }
        for (i in 0..ref.size) d[i][0] = i
        for (j in 0..hyp.size) d[0][j] = j
        for (i in 1..ref.size) for (j in 1..hyp.size) {
            val cost = if (ref[i - 1] == hyp[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j - 1] + cost, d[i - 1][j] + 1, d[i][j - 1] + 1)
        }
        var s = 0
        var del = 0
        var ins = 0
        var i = ref.size
        var j = hyp.size
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && ref[i - 1] == hyp[j - 1] && d[i][j] == d[i - 1][j - 1] -> { i--; j-- }
                i > 0 && j > 0 && d[i][j] == d[i - 1][j - 1] + 1 -> { s++; i--; j-- }
                i > 0 && d[i][j] == d[i - 1][j] + 1 -> { del++; i-- }
                else -> { ins++; j-- }
            }
        }
        return WerResult(s, del, ins, ref.size)
    }

    /** WER global: errores totales entre palabras de referencia totales. */
    fun aggregate(results: List<WerResult>): Double {
        val words = results.sumOf { it.referenceWords }
        val errors = results.sumOf { it.errors }
        return if (words == 0) errors.toDouble() else errors.toDouble() / words
    }
}
