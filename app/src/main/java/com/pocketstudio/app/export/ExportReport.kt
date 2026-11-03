package com.pocketstudio.app.export

import com.pocketstudio.engine.ExportReport
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.i18n.tr
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lot E (« Master pro », §1 bis point 3): the pro readings of an export in words and numbers, and the
 * report written beside the file (« <nom>-rapport.txt »): integrated loudness, loudest 3 s and 400 ms,
 * loudness range (LRA), true peak (after the encoding when checked), PLR, stereo correlation and what
 * the mono fold keeps, the octave spectrum against the reference song when there is one.
 */
object ProReport {
    /** One line per reading, for the export card and the report. */
    fun lines(r: ExportReport?, decodedPeakDb: Double?): List<String> {
        if (r == null) return emptyList()
        val f = { v: Double -> LoudnessTargets.format(v) }
        val peak = decodedPeakDb ?: r.truePeakDb
        return listOfNotNull(
            r.lufs?.let { tr("Volume intégré : %s LUFS", f(it)) },
            r.maxShortTerm?.let { s -> r.maxMomentary?.let { m -> tr("Plus fort sur 3 s : %1\$s LUFS · sur 0,4 s : %2\$s LUFS", f(s), f(m)) } },
            r.lra?.let { tr("Dynamique (LRA) : %s LU", f(it)) },
            tr("Crête vraie : %s dBTP", f(peak)) + if (decodedPeakDb != null) " " + tr("(après compression)") else "",
            r.lufs?.let { tr("Écart crête / volume (PLR) : %s dB", f(peak - it)) },
            r.correlation?.let { c -> tr("Corrélation stéréo : %s", String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.2f", c).replace('-', '−')) },
            r.monoDrop?.let { tr("En mono : %s LU de moins", f(it.coerceAtLeast(0.0))) },
        )
    }

    /** « Mono : bon » / « attention » (more than 3 LU lost in mono, or a correlation under 0). */
    fun monoSafe(r: ExportReport): Boolean = (r.correlation ?: 1.0) >= 0.0 && (r.monoDrop ?: 0.0) <= 3.0

    /**
     * The report beside [audio] (its name with « -rapport.txt »): the file, its format, the readings,
     * the spectrum against [reference] (octave levels, dB, from [Bands]) when given. Returns the file.
     */
    fun write(audio: File, title: String, format: String, rate: Int, bits: Int, targetLufs: Float?, ceilingDb: Float,
              r: ExportReport?, decodedPeakDb: Double?, mix: DoubleArray?, reference: DoubleArray?, referenceName: String?): File {
        val out = File(audio.parentFile, audio.nameWithoutExtension + "-rapport.txt")
        val b = StringBuilder()
        b.appendLine(tr("Rapport d'export · %s", title))
        b.appendLine(tr("Fichier : %s", audio.name))
        b.appendLine(tr("Format : %1\$s · %2\$s Hz · %3\$s", format, rate.toString(), if (bits == 32) tr("32 bits flottants") else tr("%d bits", bits)))
        r?.let { b.appendLine(tr("Durée : %s s", String.format(java.util.Locale.ROOT, "%.2f", it.seconds))) }
        b.appendLine(tr("Cible : %s", targetLufs?.let { LoudnessTargets.formatTarget(it) + " LUFS" } ?: tr("aucune")) + " · " +
            tr("plafond %s dBTP", LoudnessTargets.format(ceilingDb.toDouble())))
        lines(r, decodedPeakDb).forEach { b.appendLine(it) }
        if (mix != null) {
            b.appendLine()
            b.appendLine(if (reference != null) tr("Spectre (octaves) contre « %s » :", referenceName ?: "") else tr("Spectre (octaves) :"))
            Bands.CENTERS.forEachIndexed { i, hz ->
                val name = if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz"
                val diff = reference?.let { " (" + signed(mix[i] - it[i]) + " dB)" } ?: ""
                b.appendLine("  $name : ${signed(mix[i])} dB$diff")
            }
        }
        out.writeText(b.toString())
        return out
    }

    private fun signed(v: Double): String = (if (v > 0) "+" else "") + LoudnessTargets.format(v)
}

/**
 * Octave bands from 31,5 Hz to 16 kHz: each band's level (dB) against the whole signal's, the mean of
 * both channels. RBJ band-passes (one octave wide), K-weighting left out: a plain spectrum, as the
 * reference's. Pure Kotlin: the same code reads the export and the reference song.
 */
class Bands(rate: Int) {
    private val filters = CENTERS.map { BandPass(rate, it.toDouble()) }
    private val energy = DoubleArray(CENTERS.size)
    private var total = 0.0

    fun add(interleaved: FloatArray, frames: Int, channels: Int) {
        for (f in 0 until frames) {
            var m = 0.0
            for (c in 0 until channels) m += interleaved[f * channels + c]
            m /= channels
            total += m * m
            for (k in filters.indices) {
                val y = filters[k].process(m)
                energy[k] += y * y
            }
        }
    }

    /** Each band's level in dB against the whole (null for silence). */
    fun levels(): DoubleArray? = if (total <= 0.0) null else DoubleArray(energy.size) { 10 * log10(maxOf(energy[it], 1e-20) / total) }

    private class BandPass(rate: Int, hz: Double) {
        private val b0: Double
        private val b2: Double
        private val a1: Double
        private val a2: Double
        private var x1 = 0.0
        private var x2 = 0.0
        private var y1 = 0.0
        private var y2 = 0.0

        init {
            val w = 2 * PI * minOf(hz, rate * 0.45) / rate
            val q = sqrt(2.0)  // one octave
            val alpha = sin(w) / (2 * q)
            val a0 = 1 + alpha
            b0 = alpha / a0
            b2 = -alpha / a0
            a1 = -2 * cos(w) / a0
            a2 = (1 - alpha) / a0
        }

        fun process(x: Double): Double {
            val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x
            y2 = y1
            y1 = y
            return y
        }
    }

    companion object {
        val CENTERS = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        /** The bands of a WAV (any format WavPcm reads); null when silent or unreadable. */
        fun of(wav: File): DoubleArray? = runCatching {
            WavPcm.open(wav).use { pcm ->
                val bands = Bands(pcm.sampleRate)
                val buf = FloatArray(4096 * pcm.channels)
                while (true) {
                    val n = pcm.readFloat(buf, 4096)
                    if (n == 0) break
                    bands.add(buf, n, pcm.channels)
                }
                bands.levels()
            }
        }.getOrNull()
    }
}
