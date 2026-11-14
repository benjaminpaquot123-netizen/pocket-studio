package com.pocketstudio.bentest

import java.util.Locale

/** A step's light (docs/PLAN-TEST-APK.md, « Les feux »); [GRIS]: not conclusive (neither green nor red). */
enum class Feu(val mot: String) {
    VERT("vert"), ORANGE("orange"), ROUGE("rouge"), GRIS("non concluant");

    companion object {
        fun of(mot: String): Feu? = entries.firstOrNull { it.mot == mot }
        /** The worst of [feux] (red over orange over not conclusive over green); green when none. */
        fun pire(feux: Iterable<Feu>): Feu = feux.maxByOrNull { listOf(VERT, GRIS, ORANGE, ROUGE).indexOf(it) } ?: VERT
    }
}

/** The plan's thresholds (starting values, set again after the first full run). */
object Feux {
    fun decrochages(n: Int): Feu = when {
        n <= 0 -> Feu.VERT
        n <= 2 -> Feu.ORANGE
        else -> Feu.ROUGE
    }

    fun pireBloc(cpu: Float): Feu = when {
        cpu < 0.5f -> Feu.VERT
        cpu <= 0.8f -> Feu.ORANGE
        else -> Feu.ROUGE
    }

    fun latence(ms: Double): Feu = when {
        ms <= 50.0 -> Feu.VERT
        ms <= 70.0 -> Feu.ORANGE
        else -> Feu.ROUGE
    }

    fun imagesLentes(part: Double): Feu = when {
        part < 0.05 -> Feu.VERT
        part <= 0.15 -> Feu.ORANGE
        else -> Feu.ROUGE
    }

    /**
     * An export: read back ([relu]), its length within 0.1 s of the app's ([ecartS]), its true peak
     * under −1 dBTP, its time ([prisS]) at most twice the time planned ([prevuS]).
     */
    fun export(relu: Boolean, ecartS: Double?, creteDbtp: Double?, prisS: Double, prevuS: Double): Pair<Feu, String> {
        if (!relu) return Feu.ROUGE to "fichier illisible"
        val why = listOfNotNull(
            ecartS?.takeIf { it > 0.1 }?.let { "durée à ${fmt(it)} s de celle prévue" },
            creteDbtp?.takeIf { it >= -1.0 }?.let { "crête ${fmt(it)} dBTP" },
            prisS.takeIf { it > 2 * prevuS }?.let { "${fmt(it)} s, plus de 2 fois les ${fmt(prevuS)} s prévues" },
        )
        return (if (why.isEmpty()) Feu.VERT else Feu.ORANGE) to why.joinToString(", ")
    }

    /** The true worst block (render time over the callback's sound): red over 100 %, orange from 70 %. */
    fun blocVrai(w: Float): Feu = when {
        w > 1f -> Feu.ROUGE
        w >= 0.7f -> Feu.ORANGE
        else -> Feu.VERT
    }

    /** The longest wait between two callbacks, in callbacks: red over 2, orange over 1. */
    fun ecart(g: Float): Feu = when {
        g > 2f -> Feu.ROUGE
        g > 1f -> Feu.ORANGE
        else -> Feu.VERT
    }

    /**
     * A song played live: red for any dropout, a true worst block over 100 % or a gap over 2 blocks, a NaN; orange for a true
     * worst block from 70 % or a gap over 1 block, an alert. The smoothed load is information only. The reason says what is not green.
     */
    fun style(b: Ecoute.Bilan): Pair<Feu, String> {
        val parts = ArrayList<Pair<Feu, String>>()
        if (b.xruns > 0) parts += Feu.ROUGE to "${b.xruns} décrochage(s)"
        blocVrai(b.cpuWorst).takeIf { it != Feu.VERT }?.let { parts += it to "pire bloc vrai ${Ecoute.pct(b.cpuWorst)}" }
        ecart(b.gapWorst).takeIf { it != Feu.VERT }?.let { parts += it to "plus long écart ${fmt(b.gapWorst.toDouble()).replace('.', ',')} blocs" }
        if (b.nonFinite) parts += Feu.ROUGE to "valeur invalide (NaN)"
        if (b.alertes.isNotEmpty()) parts += Feu.ORANGE to b.alertes.joinToString(", ") { it.kind.lowercase(Locale.ROOT) + " à ${it.second} s" }
        return Feu.pire(parts.map { it.first }) to parts.joinToString(", ") { it.second }
    }

    fun fmt(v: Double) = String.format(Locale.ROOT, "%.1f", v)
}
