package com.pocketstudio.bentest

import java.util.Locale
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin

/**
 * « Écoute au micro » (docs/PLAN-TEST-APK.md, T1b): what the phone's microphone hears, by third-octave band,
 * analysed on the phone as it comes (nothing recorded but the numbers). Same analysis on the Linux side
 * (scripts/bentest/empreinte.py): a Hann window of [Tiers.TAILLE] samples, half overlapped, each band the power of
 * the bins from fc·2^-1/6 (in) to fc·2^1/6 (out).
 */
object Tiers {
    const val TAILLE = 16384

    /** 50 Hz to 16 kHz: 1000 · 2^(n/3), n = -13 … 12. */
    val CENTRES: DoubleArray = DoubleArray(26) { 1000.0 * 2.0.pow((it - 13) / 3.0) }

    /** Their nominal names (« 50 », « 63 », … « 1000 », … « 16000 »). */
    val NOMS: List<String> = listOf(
        "50", "63", "80", "100", "125", "160", "200", "250", "315", "400", "500", "630", "800",
        "1000", "1250", "1600", "2000", "2500", "3150", "4000", "5000", "6300", "8000", "10000", "12500", "16000",
    )

    fun bas(k: Int) = CENTRES[k] * 2.0.pow(-1.0 / 6)
    fun haut(k: Int) = CENTRES[k] * 2.0.pow(1.0 / 6)

    /** dB of a power, -300 for nothing. */
    fun db(p: Double) = if (p > 1e-30) 10 * log10(p) else -300.0

    /** A list of dB (null: none) as the logbook keeps it: « -12.345,,3.2 ». */
    fun csv(v: List<Double?>) = v.joinToString(",") { d -> d?.let { String.format(Locale.ROOT, "%.3f", it) } ?: "" }

    fun lireCsv(s: String): List<Double?> = if (s.isEmpty()) emptyList() else s.split(',').map { it.toDoubleOrNull() }
}

/** Third-octave band powers of a stream of samples, frame by frame (on the robot's thread, never the audio one). */
class Bandes(val rate: Int = 48_000) {
    private val n = Tiers.TAILLE
    private val hop = n / 2
    private val buf = FloatArray(n)
    private var fill = 0
    private val win = DoubleArray(n) { 0.5 - 0.5 * cos(2 * PI * it / n) }
    private val re = DoubleArray(n)
    private val im = DoubleArray(n)
    private val sum = DoubleArray(Tiers.CENTRES.size)
    private val lo = IntArray(Tiers.CENTRES.size) { ceil(Tiers.bas(it) * n / rate).toInt() }
    private val hi = IntArray(Tiers.CENTRES.size) { ceil(Tiers.haut(it) * n / rate).toInt().coerceAtMost(n / 2) }
    private val fft = Fft(n)

    /** Frames analysed so far. */
    var trames: Int = 0
        private set

    fun ajouter(x: FloatArray, n: Int = x.size) {
        for (i in 0 until n) {
            buf[fill++] = x[i]
            if (fill == this.n) {
                analyser()
                System.arraycopy(buf, hop, buf, 0, this.n - hop)
                fill = this.n - hop
            }
        }
    }

    private fun analyser() {
        for (i in 0 until n) { re[i] = buf[i] * win[i]; im[i] = 0.0 }
        fft.run(re, im)
        for (b in sum.indices) {
            var s = 0.0
            for (k in lo[b] until hi[b]) s += re[k] * re[k] + im[k] * im[k]
            sum[b] += s
        }
        trames++
    }

    /** Each band's energy summed over the frames. */
    fun energies(): DoubleArray = sum.copyOf()

    /** Each band's mean power per frame (the room's noise). */
    fun puissances(): DoubleArray = DoubleArray(sum.size) { if (trames == 0) 0.0 else sum[it] / trames }
}

/** An in-place radix-2 FFT of a fixed size, its tables made once. */
internal class Fft(private val n: Int) {
    private val cosT = DoubleArray(n / 2) { cos(2 * PI * it / n) }
    private val sinT = DoubleArray(n / 2) { -sin(2 * PI * it / n) }
    private val rev = IntArray(n).also { r ->
        val bits = Integer.numberOfTrailingZeros(n)
        for (i in 0 until n) r[i] = Integer.reverse(i) ushr (32 - bits)
    }

    fun run(re: DoubleArray, im: DoubleArray) {
        for (i in 0 until n) {
            val j = rev[i]
            if (j > i) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var start = 0
            while (start < n) {
                var k = 0
                for (j in start until start + half) {
                    val c = cosT[k]; val s = sinT[k]
                    val l = j + half
                    val tr = re[l] * c - im[l] * s
                    val ti = re[l] * s + im[l] * c
                    re[l] = re[j] - tr; im[l] = im[j] - ti
                    re[j] += tr; im[j] += ti
                    k += step
                }
                start += size
            }
            size *= 2
        }
    }
}

/** The speaker's curve: a sweep played by the engine, heard by the microphone, against the sweep itself. */
object Courbe {
    /** The middle of the curve, its 0 dB: the third-octaves from 500 Hz to 2 kHz. */
    private val MILIEU = 440.0..2250.0

    /** Heard over the room's noise by at least this much (power ratio 4, 6 dB), a band counts. */
    const val AU_DESSUS = 4.0

    /** The middle of the curve under this many dB over the room's noise: a noisy room (orange). */
    const val RSB_MIN_DB = 20.0

    /** An exponential sweep from [f1] to [f2] Hz, 50 ms faded in and out (the same energy in every third-octave). */
    fun balayage(rate: Int = 48_000, secondes: Double = 12.0, f1: Double = 40.0, f2: Double = 20_000.0, niveau: Float = 0.3f): FloatArray {
        val n = (secondes * rate).toInt()
        val l = secondes / ln(f2 / f1)
        val fade = (0.05 * rate).toInt()
        return FloatArray(n) { i ->
            val t = i.toDouble() / rate
            val g = when {
                i < fade -> 0.5 - 0.5 * cos(PI * i / fade)
                i >= n - fade -> 0.5 - 0.5 * cos(PI * (n - 1 - i) / fade)
                else -> 1.0
            }
            (niveau * g * sin(2 * PI * f1 * l * (exp(t / l) - 1))).toFloat()
        }
    }

    /**
     * [db]: dB per third-octave band, 0 dB = the mean of 500 Hz … 2 kHz; null: not swept or under the room's noise.
     * [rsb]: what the microphone heard over the room's noise, per band (dB; null: no noise measured).
     */
    data class Resultat(val db: List<Double?>, val rsb: List<Double?>)

    /** [reference], [micro]: energies of the sweep and of what the microphone heard over [tramesMicro] frames; [bruit]: the room's mean power per frame (null: none). */
    fun mesurer(reference: DoubleArray, micro: DoubleArray, tramesMicro: Int, bruit: DoubleArray?): Resultat {
        val refMax = reference.maxOrNull() ?: 0.0
        val micMax = micro.maxOrNull() ?: 0.0
        val lin = ArrayList<Double?>()
        val rsb = ArrayList<Double?>()
        for (b in Tiers.CENTRES.indices) {
            val noise = (bruit?.getOrNull(b) ?: 0.0) * tramesMicro
            val sig = micro[b] - noise
            rsb += if (bruit != null && noise > 0) Tiers.db(maxOf(sig, 0.0) / noise) else null
            val swept = reference[b] > refMax * 1e-6
            val heard = if (bruit != null && noise > 0) micro[b] >= AU_DESSUS * noise else sig > micMax * 1e-9
            lin += if (swept && heard && sig > 0) sig / reference[b] else null
        }
        val mid = Tiers.CENTRES.indices.filter { Tiers.CENTRES[it] in MILIEU }.mapNotNull { lin[it] }
        if (mid.isEmpty()) return Resultat(lin.map { null }, rsb)
        val ref = mid.average()
        return Resultat(lin.map { v -> v?.let { 10 * log10(it / ref) } }, rsb)
    }

    fun feu(r: Resultat): Pair<Feu, String> {
        val mid = Tiers.CENTRES.indices.filter { Tiers.CENTRES[it] in MILIEU }
        if (mid.count { r.db.getOrNull(it) != null } < 3) return Feu.ROUGE to "rien entendu au micro (le son est-il coupé ?)"
        val rsb = mid.mapNotNull { r.rsb.getOrNull(it) }.sorted()
        if (rsb.isNotEmpty()) {
            val median = rsb[rsb.size / 2]
            if (median < RSB_MIN_DB) return Feu.ORANGE to "pièce bruyante : le milieu de la courbe n'est qu'à ${fmt(median)} dB au-dessus du bruit"
        }
        val heard = r.db.indices.filter { r.db[it] != null }
        return Feu.VERT to if (heard.isEmpty()) "" else "entendue de ${Tiers.NOMS[heard.first()]} à ${Tiers.NOMS[heard.last()]} Hz"
    }

    fun fmt(v: Double) = String.format(Locale.ROOT, "%.1f", v).replace('.', ',')
}

/** A song's fingerprint at the microphone: its energy per octave band. */
object Empreinte {
    val OCTAVES = listOf(63, 125, 250, 500, 1000, 2000, 4000, 8000)

    /** Each octave's middle third-octave in [Tiers.CENTRES]. */
    private val MILIEUX = OCTAVES.map { o -> Tiers.NOMS.indexOf(if (o == 63) "63" else o.toString()) }

    /** dB per octave, 0 dB = the mean of 500 Hz … 4 kHz; null: under the room's noise. */
    fun octaves(micro: DoubleArray, tramesMicro: Int, bruit: DoubleArray?): List<Double?> {
        val micMax = micro.maxOrNull() ?: 0.0
        val lin = MILIEUX.map { m ->
            val e = (m - 1..m + 1).sumOf { micro[it] }
            val noise = (m - 1..m + 1).sumOf { bruit?.getOrNull(it) ?: 0.0 } * tramesMicro
            val sig = e - noise
            val heard = if (bruit != null && noise > 0) e >= Courbe.AU_DESSUS * noise else sig > micMax * 1e-9
            if (heard && sig > 0) sig else null
        }
        val mid = OCTAVES.indices.filter { OCTAVES[it] in 500..4000 }.mapNotNull { lin[it] }
        if (mid.isEmpty()) return lin.map { null }
        val ref = mid.average()
        return lin.map { v -> v?.let { 10 * log10(it / ref) } }
    }

    /** Each third-octave's power per frame heard over the room (dB re full scale of the FFT; null: under the noise). */
    fun tiers(micro: DoubleArray, tramesMicro: Int, bruit: DoubleArray?): List<Double?> = micro.indices.map { b ->
        val noise = (bruit?.getOrNull(b) ?: 0.0) * tramesMicro
        val sig = micro[b] - noise
        if (tramesMicro > 0 && sig > 0 && (noise <= 0 || micro[b] >= Courbe.AU_DESSUS * noise)) Tiers.db(sig / tramesMicro) else null
    }

    fun feu(octaves: List<Double?>): Pair<Feu, String> {
        val mid = OCTAVES.indices.filter { OCTAVES[it] in 500..4000 }
        if (mid.none { octaves.getOrNull(it) != null }) return Feu.ROUGE to "rien entendu au micro"
        val heard = octaves.count { it != null }
        if (heard < 5) return Feu.ORANGE to "seulement $heard octaves sur 8 au-dessus du bruit de la pièce"
        return Feu.VERT to ""
    }
}
