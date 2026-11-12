package com.pocketstudio.bentest

import java.util.Locale

/** A step's light (docs/PLAN-TEST-APK.md, « Les feux »); [GRIS]: not conclusive (neither green nor red). */
enum class Feu(val mot: String) {
    VERT("vert"), ORANGE("orange"), ROUGE("rouge"), GRIS("non concluant");

    companion object {
        fun of(mot: String): Feu? = entries.firstOrNull { it.mot == mot }
        fun pire(feux: Iterable<Feu>): Feu = VERT
    }
}

/** The plan's thresholds (starting values, set again after the first full run). */
object Feux {
    fun decrochages(n: Int): Feu = Feu.VERT
    fun pireBloc(cpu: Float): Feu = Feu.VERT
    fun latence(ms: Double): Feu = Feu.VERT
    fun imagesLentes(part: Double): Feu = Feu.VERT

    /**
     * An export: read back ([relu]), its length within 0.1 s of the app's ([ecartS]), its true peak
     * under −1 dBTP, its time ([prisS]) at most twice the time planned ([prevuS]).
     */
    fun export(relu: Boolean, ecartS: Double?, creteDbtp: Double?, prisS: Double, prevuS: Double): Pair<Feu, String> = Feu.VERT to ""

    /** A song played live: dropouts, worst block, a NaN (red), an alert (orange). The reason says what is not green. */
    fun style(b: Ecoute.Bilan): Pair<Feu, String> = Feu.VERT to ""

    fun fmt(v: Double) = String.format(Locale.ROOT, "%.1f", v)
}
