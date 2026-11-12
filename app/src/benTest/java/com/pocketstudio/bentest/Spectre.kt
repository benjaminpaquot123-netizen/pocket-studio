package com.pocketstudio.bentest

/**
 * « Écoute au micro » (docs/PLAN-TEST-APK.md, T1b): what the phone's microphone hears, by third-octave band,
 * analysed on the phone as it comes (nothing recorded but the numbers). Same analysis on the Linux side
 * (scripts/bentest/empreinte.py): a Hann window of [Tiers.TAILLE] samples, half overlapped, each band the power of
 * the bins from fc·2^-1/6 (in) to fc·2^1/6 (out).
 */
object Tiers {
    const val TAILLE = 16384

    /** 50 Hz to 16 kHz: 1000 · 2^(n/3), n = -13 … 12. */
    val CENTRES: DoubleArray = DoubleArray(0)

    /** Their nominal names (« 50 », « 63 », … « 1000 », … « 16000 »). */
    val NOMS: List<String> = emptyList()
}

/** Third-octave band powers of a stream of samples, frame by frame. */
class Bandes(val rate: Int = 48_000) {
    /** Frames analysed so far. */
    var trames: Int = 0
        private set

    fun ajouter(x: FloatArray, n: Int = x.size) = Unit

    /** Each band's energy summed over the frames. */
    fun energies(): DoubleArray = DoubleArray(Tiers.CENTRES.size)

    /** Each band's mean power per frame (the room's noise). */
    fun puissances(): DoubleArray = DoubleArray(Tiers.CENTRES.size)
}

/** The speaker's curve: a sweep played by the engine, heard by the microphone, against the sweep itself. */
object Courbe {
    fun balayage(rate: Int = 48_000, secondes: Double = 12.0, f1: Double = 40.0, f2: Double = 20_000.0, niveau: Float = 0.3f): FloatArray = FloatArray(0)

    /**
     * [db]: dB per third-octave band, 0 dB = the mean of 500 Hz … 2 kHz; null: not swept or under the room's noise.
     * [rsb]: what the microphone heard over the room's noise, per band (dB; null: no noise measured).
     */
    data class Resultat(val db: List<Double?>, val rsb: List<Double?>)

    /** [reference], [micro]: energies of the sweep and of what the microphone heard over [tramesMicro] frames; [bruit]: the room's mean power per frame (null: none). */
    fun mesurer(reference: DoubleArray, micro: DoubleArray, tramesMicro: Int, bruit: DoubleArray?): Resultat = Resultat(emptyList(), emptyList())

    fun feu(r: Resultat): Pair<Feu, String> = Feu.GRIS to ""
}

/** A song's fingerprint at the microphone: its energy per octave band. */
object Empreinte {
    val OCTAVES = listOf(63, 125, 250, 500, 1000, 2000, 4000, 8000)

    /** dB per octave, 0 dB = the mean of 500 Hz … 4 kHz; null: under the room's noise. */
    fun octaves(micro: DoubleArray, tramesMicro: Int, bruit: DoubleArray?): List<Double?> = emptyList()

    fun feu(octaves: List<Double?>): Pair<Feu, String> = Feu.GRIS to ""
}
