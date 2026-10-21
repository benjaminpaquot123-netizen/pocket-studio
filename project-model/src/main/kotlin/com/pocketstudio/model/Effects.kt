package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.percent
import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.Serializable
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * One insert slot: [type] in [EffectType] order (same numbers as the engine), 4 parameters 0..1.
 * [bypass]: kept with its settings but not heard (the engine gets an empty slot).
 */
@Serializable
data class Insert(val type: Int = 0, val params: List<Float> = EffectType.NONE.defaults, val bypass: Boolean = false) {
    val effect: EffectType get() = EffectType.of(type)

    /** The type the engine plays: none while bypassed. */
    val heardType: Int get() = if (bypass) 0 else EffectType.of(type).ordinal

    fun sanitized(): Insert {
        val t = EffectType.of(type)
        val p = (params + t.defaults.drop(params.size)).take(EffectType.PARAMS)
            .map { v -> v.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.5f }
        return Insert(t.ordinal, p, bypass && t != EffectType.NONE)
    }

    companion object {
        fun of(type: EffectType) = Insert(type.ordinal, type.defaults)
    }
}

/** What a parameter means, for its label and the value shown next to the slider. */
data class Param(val label: String, val format: (Float) -> String)

/** The families the effect list is shown in ("+ Ajouter un effet"). */
enum class EffectGroup(val label: String) {
    COLOUR("Distorsion & couleur"),
    MOTION("Filtres & mouvement"),
    SPACE("Espace"),
    DYNAMICS("Dynamique"),
    CREATIVE("Créatifs"),
}

/**
 * The insert effects, in the engine's InsertSlot::Type order. Ranges mirror Effects.cpp and
 * ExtraEffects.cpp. [blurb]: what it does, in a sentence; [group]: its family in the list.
 */
enum class EffectType(val label: String, val params: List<Param>, val defaults: List<Float>, val blurb: String = "", val group: EffectGroup = EffectGroup.COLOUR) {
    NONE("Aucun", emptyList(), listOf(0f, 0f, 0f, 0f)),
    EQ(
        "Égaliseur",
        listOf(Param("Graves", ::eqDb), Param("Médiums", ::eqDb), Param("Aigus", ::eqDb)),
        listOf(0.5f, 0.5f, 0.5f, 0f),
        group = EffectGroup.COLOUR,
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
        group = EffectGroup.DYNAMICS,
    ),
    SATURATION(
        "Saturation",
        listOf(Param("Drive", ::percentOf), Param("Tonalité") { hz(expMap(it, 1500f, 18000f)) }, Param("Mélange", ::percentOf), Param("Volume", ::percentOf)),
        listOf(0.3f, 0.7f, 1f, 0.7f),
        group = EffectGroup.COLOUR,
    ),
    CHORUS(
        "Chorus",
        listOf(Param("Vitesse") { num("%.2f Hz", expMap(it, 0.05f, 5f)) }, Param("Profondeur", ::percentOf), Param("Retard", ::percentOf), Param("Mélange", ::percentOf)),
        listOf(0.3f, 0.5f, 0.4f, 0.5f),
        group = EffectGroup.MOTION,
    ),
    FILTER(
        "Filtre",
        listOf(
            Param("Coupure") { hz(expMap(it, 40f, 18000f)) },
            Param("Résonance", ::percentOf),
            Param("Type") { listOf(tr("Passe-bas"), tr("Passe-bande"), tr("Passe-haut"))[(it * 3).toInt().coerceIn(0, 2)] },
            Param("Mélange", ::percentOf),
        ),
        listOf(0.6f, 0.2f, 0f, 1f),
        group = EffectGroup.MOTION,
    ),
    PUMP(
        "Pompe",
        listOf(
            Param("Profondeur", ::percentOf),
            Param("Relâche") { ms(expMap(it, 50f, 600f)) },
            Param("Déclenche") { if (it < 0.5f) tr("Grosse caisse") else tr("Chaque temps") },
        ),
        listOf(0.7f, 0.4f, 0f, 0f),
        group = EffectGroup.DYNAMICS,
    ),
    FLANGER(
        "Flanger",
        listOf(Param("Vitesse") { num("%.2f Hz", expMap(it, 0.05f, 5f)) }, Param("Profondeur", ::percentOf), Param("Feedback", ::percentOf), Param("Mélange", ::percentOf)),
        listOf(0.2f, 0.6f, 0.5f, 0.5f),
        group = EffectGroup.MOTION,
    ),
    PHASER(
        "Phaser",
        listOf(Param("Vitesse") { num("%.2f Hz", expMap(it, 0.05f, 5f)) }, Param("Profondeur", ::percentOf), Param("Feedback", ::percentOf), Param("Mélange", ::percentOf)),
        listOf(0.2f, 0.7f, 0.5f, 0.5f),
        group = EffectGroup.MOTION,
    ),
    GATE(
        "Noise gate",
        listOf(
            Param("Seuil") { "${(-80 + 80 * it).roundToInt()} dB" },
            Param("Attaque") { ms(expMap(it, 0.1f, 50f)) },
            Param("Relâche") { ms(expMap(it, 10f, 1000f)) },
            Param("Coupe", ::percentOf),
        ),
        listOf(0.4f, 0.2f, 0.4f, 1f),
        group = EffectGroup.DYNAMICS,
    ),
    TREMOLO(
        "Trémolo",
        listOf(
            Param("Vitesse") { num("%.1f Hz", expMap(it, 0.1f, 20f)) },
            Param("Profondeur", ::percentOf),
            Param("Forme") { if (it < 0.33f) tr("Douce") else if (it < 0.66f) tr("Marquée") else tr("Carrée") },
            Param("Gauche/droite", ::percentOf),
        ),
        listOf(0.4f, 0.6f, 0f, 0f),
        group = EffectGroup.MOTION,
    ),
    WIDENER(
        "Élargisseur",
        listOf(
            Param("Largeur") { if (it < 0.01f) tr("Mono") else percent((it * 200).roundToInt()) },
            Param("Basses au centre") { if (it < 0.01f) tr("Non") else tr("sous %s", hz(expMap(it, 40f, 300f))) },
        ),
        listOf(0.75f, 0f, 0f, 0f),
        group = EffectGroup.SPACE,
    ),
    BITCRUSHER(
        "Bitcrusher",
        listOf(
            Param("Bits") { "${(16 - 14 * it).roundToInt()} bits" },
            Param("Échantillonnage") { "÷${1 + (31 * it * it).roundToInt()}" },
            Param("Mélange", ::percentOf),
            Param("Volume") { percent((200 * it * it).roundToInt()) },
        ),
        listOf(0.5f, 0.3f, 1f, 0.7f),
        group = EffectGroup.COLOUR,
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
        group = EffectGroup.COLOUR,
    ),
    ROBOT(
        "Robot",
        listOf(
            Param("Hauteur de voix") { hz(expMap(it, 30f, 2000f)) },
            Param("Mélange", ::percentOf),
            Param("Tremblement", ::percentOf),
            Param("Vitesse") { num("%.1f Hz", expMap(it, 0.1f, 8f)) },
        ),
        listOf(0.45f, 1f, 0f, 0.3f),
        "Transforme n'importe quel son en voix de robot métallique (un modulateur en anneau).", group = EffectGroup.CREATIVE,
    ),
    WAH(
        "Wah-wah",
        listOf(
            Param("Sensibilité", ::percentOf),
            Param("Départ") { hz(expMap(it, 150f, 1200f)) },
            Param("Résonance", ::percentOf),
            Param("Mélange", ::percentOf),
        ),
        listOf(0.6f, 0.3f, 0.6f, 1f),
        "Plus tu joues fort, plus le son s'ouvre : « ouaou » façon guitare funk.", group = EffectGroup.MOTION,
    ),
    VINYL(
        "Vieux vinyle",
        listOf(Param("Pleurage", ::percentOf), Param("Craquements", ::percentOf), Param("Âge", ::percentOf), Param("Mélange", ::percentOf)),
        listOf(0.4f, 0.4f, 0.5f, 1f),
        "Un disque usé : il ondule, il craque, il sonne étouffé. Parfait pour du lofi.", group = EffectGroup.COLOUR,
    ),
    RADIO(
        "Talkie-walkie",
        listOf(Param("Étroitesse", ::percentOf), Param("Grain", ::percentOf), Param("Grésillement", ::percentOf), Param("Mélange", ::percentOf)),
        listOf(0.5f, 0.4f, 0.3f, 1f),
        "Comme sorti d'une radio ou d'un téléphone : étroit, granuleux, avec un peu de friture.", group = EffectGroup.COLOUR,
    ),
    CHOPPER(
        "Hachoir",
        listOf(
            Param("Motif") { listOf("x.x.", "xx.x", "x..x", "x.xx", ".x.x", "xxx.", "x...", "xx..")[(it * 8).toInt().coerceIn(0, 7)].replace('x', '■').replace('.', '□') },
            Param("Profondeur", ::percentOf),
            Param("Douceur") { ms(expMap(it, 1f, 30f)) },
            Param("Vitesse") { if (it < 0.5f) tr("Doubles-croches") else tr("Triples-croches") },
        ),
        listOf(0f, 1f, 0.2f, 0f),
        "Découpe le son en rythme, calé sur le tempo : le bégaiement des drops.", group = EffectGroup.CREATIVE,
    ),
    DISTORTION(
        "Distorsion",
        listOf(
            Param("Drive", ::percentOf),
            Param("Tonalité") { hz(expMap(it, 800f, 18000f)) },
            Param("Type") { listOf(tr("Overdrive"), tr("Fuzz"), tr("Écrêtage dur"), tr("Repliement"))[(it * 4).toInt().coerceIn(0, 3)] },
            Param("Mélange", ::percentOf),
        ),
        listOf(0.45f, 0.6f, 0f, 1f),
        "Pousse le son dans le rouge : chaud (overdrive), baveux (fuzz), tranchant (écrêtage) ou métallique (repliement).",
        EffectGroup.COLOUR,
    ),
    ECHO(
        "Écho",
        listOf(Param("Temps", ::noteValue), Param("Répétitions", ::percentOf), Param("Tonalité") { hz(expMap(it, 800f, 16000f)) }, Param("Mélange", ::percentOf)),
        listOf(0.42f, 0.4f, 0.55f, 0.35f),
        "Le son se répète en rythme, calé sur le tempo (la croche pointée est le classique). Tonalité : des répétitions plus sombres ou plus claires.",
        EffectGroup.SPACE,
    ),
    PING_PONG(
        "Écho ping-pong",
        listOf(Param("Temps", ::noteValue), Param("Répétitions", ::percentOf), Param("Tonalité") { hz(expMap(it, 800f, 16000f)) }, Param("Mélange", ::percentOf)),
        listOf(0.25f, 0.45f, 0.55f, 0.35f),
        "Un écho qui rebondit de gauche à droite, en rythme : large et vivant.",
        EffectGroup.SPACE,
    ),
    REVERB(
        "Réverbe",
        listOf(Param("Taille", ::percentOf), Param("Amorti", ::percentOf), Param("Pré-délai") { ms(200 * it) }, Param("Mélange", ::percentOf)),
        listOf(0.6f, 0.5f, 0.1f, 0.3f),
        "Une pièce, une salle ou une cathédrale rien que pour ce son.",
        EffectGroup.SPACE,
    ),
    AUTO_PAN(
        "Auto-pan",
        listOf(
            Param("Vitesse") { listOf(tr("2 mesures"), tr("1 mesure"), "1/2", "1/4", "1/8", "1/16")[(it * 6).toInt().coerceIn(0, 5)] },
            Param("Profondeur", ::percentOf),
            Param("Forme") { if (it < 0.33f) tr("Douce") else if (it < 0.66f) tr("Marquée") else tr("Carrée") },
        ),
        listOf(0.58f, 0.7f, 0f, 0f),
        "Le son se promène de gauche à droite, en rythme avec le morceau.",
        EffectGroup.MOTION,
    ),
    OTT(
        "OTT multibande",
        listOf(Param("Profondeur", ::percentOf), Param("Vitesse", ::percentOf), Param("Remontée", ::percentOf), Param("Écrasement", ::percentOf)),
        listOf(0.6f, 0.5f, 0.7f, 0.7f),
        "Compresse graves, médiums et aigus séparément, vers le haut et vers le bas : le son énorme, serré et brillant de l'EDM.",
        EffectGroup.DYNAMICS,
    ),
    FREQ_SHIFT(
        "Décalage de fréquence",
        listOf(
            Param("Décalage") { v -> val f = shiftHz(v); (if (f >= 0.5f) "+" else if (f <= -0.5f) "−" else "") + hz(kotlin.math.abs(f)) },
            Param("Réinjection", ::percentOf),
            Param("Mélange", ::percentOf),
        ),
        listOf(0.6f, 0f, 0.5f, 0f),
        "Décale toutes les fréquences du même nombre de hertz : métallique, cloches désaccordées, ou un lent tourbillon avec quelques hertz.",
        EffectGroup.CREATIVE,
    ),
    TAPE_STOP(
        "Arrêt de bande",
        listOf(
            Param("Arrêt") { if (it >= 0.5f) tr("Arrêté") else tr("Joue") },
            Param("Durée de l'arrêt") { num("%.2f s", expMap(it, 0.1f, 2f)) },
            Param("Reprise") { ms(300 * it) },
        ),
        listOf(0f, 0.5f, 0.2f, 0f),
        "Le son ralentit jusqu'à s'arrêter, comme une cassette qu'on coupe. Pousse « Arrêt » au-delà du milieu (ou dessine-le en automation) pour le déclencher.",
        EffectGroup.CREATIVE,
    ),
    ;

    /** Seconds of memory: the engine holds only a few of these (see [LONG_LIMITS]). */
    val long: Boolean get() = this == ECHO || this == PING_PONG || this == REVERB || this == TAPE_STOP

    companion object {
        const val PARAMS = 4
        fun of(type: Int) = entries.getOrElse(type) { NONE }

        /**
         * How many of each long effect can play at once in a project (the engine's LongFxPool):
         * the echoes (both kinds), the reverbs, the tape stops.
         */
        fun longLimit(type: EffectType): Int = when (type) {
            ECHO, PING_PONG -> 12
            REVERB -> 12
            TAPE_STOP -> 6
            else -> Int.MAX_VALUE
        }

        /** Whether [a] and [b] borrow from the same engine pool. */
        fun samePool(a: EffectType, b: EffectType) = a == b || (a in ECHOES && b in ECHOES)
        private val ECHOES = setOf(ECHO, PING_PONG)

        /** Frequency shift in Hz of the shift knob (the engine's FreqShifter::shiftHz). */
        fun shiftHz(v: Float): Float {
            val s = 2f * v.coerceIn(0f, 1f) - 1f
            return 2000f * s * kotlin.math.abs(s)
        }
    }
}

/** How many [type] effects (or others of its pool) the whole project uses, bypassed ones apart. */
fun Mixer.longFxCount(type: EffectType): Int =
    (channels.flatMap { it.inserts } + master).count { !it.bypass && EffectType.samePool(it.effect, type) }

/**
 * A pool of long effects past its limit here (the engine would leave the last ones silent): its
 * type, else null. With [before], only a pool that has more of them than in [before] (an edit
 * that adds one: refused; one that leaves things as they were or better: allowed).
 */
fun Mixer.longFxOverLimit(before: Mixer? = null): EffectType? = LONG_POOLS.firstOrNull { t ->
    val n = longFxCount(t)
    n > EffectType.longLimit(t) && (before == null || n > before.longFxCount(t))
}

private val LONG_POOLS = listOf(EffectType.ECHO, EffectType.REVERB, EffectType.TAPE_STOP)

/** The warning when a long effect would go past its limit ([longFxOverLimit]), the same everywhere. */
fun longFxLimitNotice(type: EffectType): String = tr(
    "Le morceau a déjà %1\$d effets « %2\$s » ou de la même famille : c'est le maximum. Retires-en un ailleurs, ou utilise la réverbe et l'écho communs.",
    EffectType.longLimit(type), tr(type.label),
)

/** The two send buses: 0 = reverb, 1 = tempo delay (same order as the engine). */
object Buses {
    const val COUNT = 2
    val NAMES = listOf("Réverbe", "Écho")
    val PARAMS: List<List<Param>> = listOf(
        listOf(Param("Taille", ::percentOf), Param("Amorti", ::percentOf), Param("Largeur", ::percentOf), Param("Pré-délai") { ms(200 * it) }),
        listOf(
            Param("Temps") { listOf("1/16", "1/8", "3/16", "1/4", "3/8", "1/2")[(it * 6).toInt().coerceIn(0, 5)] },
            Param("Feedback", ::percentOf),
            Param("Ping-pong", ::percentOf),
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
private fun percentOf(v: Float) = percent((v * 100).roundToInt())
private fun shiftHz(v: Float) = EffectType.shiftHz(v)
/** The echo's note values (the engine's 1, 2, 3, 4, 6, 8 sixteenths). */
private fun noteValue(v: Float) = listOf("1/16", "1/8", tr("1/8 pointée"), "1/4", tr("1/4 pointée"), "1/2")[(v * 6).toInt().coerceIn(0, 5)]
private fun ms(v: Float) = if (v < 10) num("%.1f ms", v) else "${v.roundToInt()} ms"
private fun hz(v: Float) = if (v >= 1000) num("%.1f kHz", v / 1000) else "${v.roundToInt()} Hz"

/**
 * Where a new effect goes in [inserts]: after the last one in use (effects run in slot order: the
 * newest last), else the first empty slot; null when all are taken.
 */
fun freeInsertSlot(inserts: List<Insert>): Int? {
    val last = inserts.indexOfLast { it.effect != EffectType.NONE }
    return (last + 1 until inserts.size).firstOrNull { inserts[it].effect == EffectType.NONE }
        ?: inserts.indices.firstOrNull { inserts[it].effect == EffectType.NONE }
}

/** The inserts of strip [ch] ([Mixer.MASTER]: the master's). */
fun Project.insertsOf(ch: Int): List<Insert> = if (ch == Mixer.MASTER) mixer.master else mixer.channels.getOrNull(ch)?.inserts.orEmpty()

/**
 * Slots [a] and [b] of strip [ch] swapped (an effect moved up or down the chain); their automation
 * lanes and learned MIDI knobs follow them.
 */
fun Project.withInsertsSwapped(ch: Int, a: Int, b: Int): Project {
    val list = insertsOf(ch)
    if (a !in list.indices || b !in list.indices || a == b) return this
    val swapped = list.toMutableList().also { it[a] = list[b]; it[b] = list[a] }
    val mixed = if (ch == Mixer.MASTER) mixer.copy(master = swapped) else mixer.copy(
        channels = mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(inserts = swapped) else c },
    )
    fun AutoTarget.swapped() =
        if (kind != AutoTarget.INSERT_PARAM || this.ch != ch || (slot != a && slot != b)) this else copy(slot = if (slot == a) b else a)
    val lanes = automation.map { l -> l.target.swapped().let { t -> if (t == l.target) l else l.copy(target = t) } }
    // A MIDI knob learned on an effect's setting keeps moving that effect's setting.
    val midi = midiMap.map { m -> m.target.swapped().let { t -> if (t == m.target) m else m.copy(target = t) } }
    return copy(mixer = mixed, automation = lanes, midiMap = midi)
}
