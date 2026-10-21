package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt

/** One insert slot: [type] in [EffectType] order (same numbers as the engine), 4 parameters 0..1. */
@Serializable
data class Insert(val type: Int = 0, val params: List<Float> = EffectType.NONE.defaults) {
    val effect: EffectType get() = EffectType.of(type)

    fun sanitized(): Insert {
        val t = EffectType.of(type)
        val p = (params + t.defaults.drop(params.size)).take(EffectType.PARAMS)
            .map { v -> v.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f }
        return Insert(t.ordinal, p)
    }

    companion object {
        fun of(type: EffectType) = Insert(type.ordinal, type.defaults)
    }
}

/** What a parameter means, for its label and the value shown next to the slider. */
data class Param(val label: String, val format: (Float) -> String)

/**
 * The insert effects, in the engine's InsertSlot::Type order. Ranges mirror Effects.cpp.
 * [blurb]: what it does, in a sentence; [crazy]: one of the fun ones (shown in their own group).
 */
enum class EffectType(val label: String, val params: List<Param>, val defaults: List<Float>, val blurb: String = "", val crazy: Boolean = false) {
    NONE("Aucun", emptyList(), listOf(0f, 0f, 0f, 0f)),
    EQ(
        "Égaliseur",
        listOf(Param("Graves", ::eqDb), Param("Médiums", ::eqDb), Param("Aigus", ::eqDb)),
        listOf(0.5f, 0.5f, 0.5f, 0f),
    ),
    COMPRESSOR(
        "Compresseur",
        listOf(
            Param("Seuil") { "${(-40 + 40 * it).roundToInt()} dB" },
            Param("Ratio") { num("%.1f:1", expMap(it, 1f, 20f)) },
            Param("Attaque") { ms(expMap(it, 0.1f, 80f)) },
            Param("Relâche") { ms(expMap(it, 20f, 800f)) },
        ),
        listOf(0.6f, 0.2f, 0.2f, 0.3f),
    ),
    SATURATION(
        "Saturation",
        listOf(Param("Drive", ::percent), Param("Tonalité") { hz(expMap(it, 1500f, 18000f)) }, Param("Mélange", ::percent), Param("Volume", ::percent)),
        listOf(0.3f, 0.7f, 1f, 0.7f),
    ),
    CHORUS(
        "Chorus",
        listOf(Param("Vitesse") { num("%.2f Hz", expMap(it, 0.05f, 5f)) }, Param("Profondeur", ::percent), Param("Retard", ::percent), Param("Mélange", ::percent)),
        listOf(0.3f, 0.5f, 0.4f, 0.5f),
    ),
    FILTER(
        "Filtre",
        listOf(
            Param("Coupure") { hz(expMap(it, 40f, 18000f)) },
            Param("Résonance", ::percent),
            Param("Type") { listOf(tr("Passe-bas"), tr("Passe-bande"), tr("Passe-haut"))[(it * 3).toInt().coerceIn(0, 2)] },
            Param("Mélange", ::percent),
        ),
        listOf(0.6f, 0.2f, 0f, 1f),
    ),
    PUMP(
        "Pompe",
        listOf(
            Param("Profondeur", ::percent),
            Param("Relâche") { ms(expMap(it, 50f, 600f)) },
            Param("Déclenche") { if (it < 0.5f) tr("Grosse caisse") else tr("Chaque temps") },
        ),
        listOf(0.7f, 0.4f, 0f, 0f),
    ),
    FLANGER(
        "Flanger",
        listOf(Param("Vitesse") { num("%.2f Hz", expMap(it, 0.05f, 5f)) }, Param("Profondeur", ::percent), Param("Feedback", ::percent), Param("Mélange", ::percent)),
        listOf(0.2f, 0.6f, 0.5f, 0.5f),
    ),
    PHASER(
        "Phaser",
        listOf(Param("Vitesse") { num("%.2f Hz", expMap(it, 0.05f, 5f)) }, Param("Profondeur", ::percent), Param("Feedback", ::percent), Param("Mélange", ::percent)),
        listOf(0.2f, 0.7f, 0.5f, 0.5f),
    ),
    GATE(
        "Noise gate",
        listOf(
            Param("Seuil") { "${(-80 + 80 * it).roundToInt()} dB" },
            Param("Attaque") { ms(expMap(it, 0.1f, 50f)) },
            Param("Relâche") { ms(expMap(it, 10f, 1000f)) },
            Param("Coupe", ::percent),
        ),
        listOf(0.4f, 0.2f, 0.4f, 1f),
    ),
    TREMOLO(
        "Trémolo",
        listOf(
            Param("Vitesse") { num("%.1f Hz", expMap(it, 0.1f, 20f)) },
            Param("Profondeur", ::percent),
            Param("Forme") { if (it < 0.33f) tr("Douce") else if (it < 0.66f) tr("Marquée") else tr("Carrée") },
            Param("Gauche/droite", ::percent),
        ),
        listOf(0.4f, 0.6f, 0f, 0f),
    ),
    WIDENER(
        "Élargisseur",
        listOf(
            Param("Largeur") { if (it < 0.01f) tr("Mono") else "${(it * 200).roundToInt()} %" },
            Param("Basses au centre") { if (it < 0.01f) tr("Non") else tr("sous %s", hz(expMap(it, 40f, 300f))) },
        ),
        listOf(0.75f, 0f, 0f, 0f),
    ),
    BITCRUSHER(
        "Bitcrusher",
        listOf(
            Param("Bits") { "${(16 - 14 * it).roundToInt()} bits" },
            Param("Échantillonnage") { "÷${1 + (31 * it * it).roundToInt()}" },
            Param("Mélange", ::percent),
            Param("Volume") { "${(200 * it * it).roundToInt()} %" },
        ),
        listOf(0.5f, 0.3f, 1f, 0.7f),
    ),
    PARAM_EQ(
        "EQ paramétrique",
        listOf(
            Param("Fréquence") { hz(expMap(it, 20f, 20000f)) },
            Param("Gain") { v -> val db = (v - 0.5f) * 36f; (if (db >= 0.05f) "+" else "") + num("%.1f dB", db) },
            Param("Largeur") { num("Q %.1f", expMap(it, 0.3f, 10f)) },
            Param("Coupe-bas") { if (it < 0.01f) tr("Non") else hz(expMap(it, 20f, 500f)) },
        ),
        listOf(0.5f, 0.5f, 0.4f, 0f),
    ),
    ROBOT(
        "Robot",
        listOf(
            Param("Hauteur de voix") { hz(expMap(it, 30f, 2000f)) },
            Param("Mélange", ::percent),
            Param("Tremblement", ::percent),
            Param("Vitesse") { num("%.1f Hz", expMap(it, 0.1f, 8f)) },
        ),
        listOf(0.45f, 1f, 0f, 0.3f),
        "Transforme n'importe quel son en voix de robot métallique (un modulateur en anneau).", crazy = true,
    ),
    WAH(
        "Wah-wah",
        listOf(
            Param("Sensibilité", ::percent),
            Param("Départ") { hz(expMap(it, 150f, 1200f)) },
            Param("Résonance", ::percent),
            Param("Mélange", ::percent),
        ),
        listOf(0.6f, 0.3f, 0.6f, 1f),
        "Plus tu joues fort, plus le son s'ouvre : « ouaou » façon guitare funk.", crazy = true,
    ),
    VINYL(
        "Vieux vinyle",
        listOf(Param("Pleurage", ::percent), Param("Craquements", ::percent), Param("Âge", ::percent), Param("Mélange", ::percent)),
        listOf(0.4f, 0.4f, 0.5f, 1f),
        "Un disque usé : il ondule, il craque, il sonne étouffé. Parfait pour du lofi.", crazy = true,
    ),
    RADIO(
        "Talkie-walkie",
        listOf(Param("Étroitesse", ::percent), Param("Grain", ::percent), Param("Grésillement", ::percent), Param("Mélange", ::percent)),
        listOf(0.5f, 0.4f, 0.3f, 1f),
        "Comme sorti d'une radio ou d'un téléphone : étroit, granuleux, avec un peu de friture.", crazy = true,
    ),
    CHOPPER(
        "Hachoir",
        listOf(
            Param("Motif") { listOf("x.x.", "xx.x", "x..x", "x.xx", ".x.x", "xxx.", "x...", "xx..")[(it * 8).toInt().coerceIn(0, 7)].replace('x', '■').replace('.', '□') },
            Param("Profondeur", ::percent),
            Param("Douceur") { ms(expMap(it, 1f, 30f)) },
            Param("Vitesse") { if (it < 0.5f) tr("Doubles-croches") else tr("Triples-croches") },
        ),
        listOf(0f, 1f, 0.2f, 0f),
        "Découpe le son en rythme, calé sur le tempo : le bégaiement des drops.", crazy = true,
    ),
    ;

    companion object {
        const val PARAMS = 4
        fun of(type: Int) = entries.getOrElse(type) { NONE }
    }
}

/** The two send buses: 0 = reverb, 1 = tempo delay (same order as the engine). */
object Buses {
    const val COUNT = 2
    val NAMES = listOf("Réverbe", "Écho")
    val PARAMS: List<List<Param>> = listOf(
        listOf(Param("Taille", ::percent), Param("Amorti", ::percent), Param("Largeur", ::percent), Param("Pré-délai") { ms(200 * it) }),
        listOf(
            Param("Temps") { listOf("1/16", "1/8", "3/16", "1/4", "3/8", "1/2")[(it * 6).toInt().coerceIn(0, 5)] },
            Param("Feedback", ::percent),
            Param("Ping-pong", ::percent),
            Param("Tonalité") { hz(expMap(it, 800f, 16000f)) },
        ),
    )
    val DEFAULTS: List<List<Float>> = listOf(listOf(0.6f, 0.5f, 1f, 0.1f), listOf(0.33f, 0.4f, 1f, 0.6f))

    fun sanitized(buses: List<List<Float>>): List<List<Float>> = List(COUNT) { b ->
        val given = buses.getOrNull(b).orEmpty()
        (given + DEFAULTS[b].drop(given.size)).take(EffectType.PARAMS).map { v -> v.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f }
    }
}

/** A number in the interface language's format (1,5 in French, 1.5 in English). */
private fun num(format: String, vararg args: Any?) = String.format(I18n.lang.locale, format, *args)

private fun expMap(v: Float, lo: Float, hi: Float) = lo * (hi / lo).pow(v.coerceIn(0f, 1f))
private fun eqDb(v: Float): String {
    val db = (v - 0.5f) * 24f
    return (if (db >= 0.05f) "+" else "") + num("%.1f dB", db)
}
private fun percent(v: Float) = "${(v * 100).roundToInt()} %"
private fun ms(v: Float) = if (v < 10) num("%.1f ms", v) else "${v.roundToInt()} ms"
private fun hz(v: Float) = if (v >= 1000) num("%.1f kHz", v / 1000) else "${v.roundToInt()} Hz"
