package com.pocketstudio.model.guide

import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * A parcours: the 30 to 40 cards of a style, in order (docs/PLAN-GUIDE.md §3.5), read from
 * `guides/<style>.json` (resources of this module, written by scripts/guides/make_guides.py from the
 * style recipes; data, not code). Every text is French, the key of its translation ([guideText]).
 */
@Serializable
data class GuideParcours(
    val format: Int = FORMAT,
    val style: String = "",
    val version: Int = 0,
    val cards: List<GuideCard> = emptyList(),
    /**
     * Why each effect is on a track, by role then by effect ([com.pocketstudio.model.EffectType] name);
     * the role "*" for every role (French keys): the track sheet's « pourquoi ».
     */
    val whys: Map<String, Map<String, String>> = emptyMap(),
) {
    fun card(id: String): GuideCard? = cards.firstOrNull { it.id == id }

    /** Why [effect] is on a track of [role] (a French key), empty when the parcours does not say. */
    fun why(role: String, effect: com.pocketstudio.model.EffectType): String =
        whys[role]?.get(effect.name) ?: whys["*"]?.get(effect.name) ?: ""

    /** The cards of the Mode simple (stages 1, 4, 6 and 7), or all of them. */
    fun cardsFor(simple: Boolean): List<GuideCard> = if (simple) cards.filter { it.simple } else cards

    companion object {
        const val FORMAT = 1
        /** The parcours of a song made by hand (no style). */
        const val GENERIC = "generic"
    }
}

/**
 * One decision, 1 to 3 minutes. [stage] 0..7 ([GuideStages]); [target] the role it is about
 * ([GuideRoles]: "bass", "kick"…, "song" for the whole song); [kind] [DECIDE] (proposals to apply),
 * [REVIEW] (what the assistant put, to hear and keep, adjust or remove), [CHECK] (a check to do by
 * ear). [applicable] and [done]: detectors ([GuideFacts.holds]: "has:bass", "!peaks_differ",
 * "by_hand|changed:lanes" for either…), every one must hold; none in [done]: only a decision ticks it. [proposals]: 2 or 3 options;
 * [elsewhere]: « ailleurs ? » (reuse a pattern, copy a chain, keep a sound). [lexicon]: the word of
 * « Les mots de la musique » ([com.pocketstudio.model.learn.SoundGlossary]) that explains it.
 * [whenText], [howMuch], [howOften]: the track sheet's columns « Quand », « Combien », « Combien de fois ».
 */
@Serializable
data class GuideCard(
    val id: String,
    val stage: Int = 0,
    val target: String = GuideRoles.SONG,
    val kind: String = DECIDE,
    val simple: Boolean = false,
    val title: String = "",
    val why: String = "",
    val simpleTitle: String? = null,
    val simpleWhy: String? = null,
    @SerialName("when") val whenText: String? = null,
    val howMuch: String? = null,
    val howOften: String? = null,
    val applicable: List<String> = emptyList(),
    val done: List<String> = emptyList(),
    val proposals: List<GuideProposal> = emptyList(),
    val elsewhere: List<GuideProposal> = emptyList(),
    val lexicon: String = "",
) {
    /** Its title in the interface's language, in everyday words in the Mode simple when it has some. */
    fun titleFor(simple: Boolean): String = guideText(if (simple) simpleTitle ?: title else title)

    fun whyFor(simple: Boolean): String = guideText(if (simple) simpleWhy ?: why else why)

    companion object {
        const val DECIDE = "decide"
        const val REVIEW = "review"
        const val CHECK = "check"
    }
}

/** An option of a card: what it is called, why, its starting value, and what it does ([action]). */
@Serializable
data class GuideProposal(
    val id: String,
    val label: String = "",
    val why: String = "",
    val value: String? = null,
    val action: GuideAction = GuideAction(),
)

/**
 * What applying an option does (see [GuideActions.apply]). [type]:
 * - [KEEP]: nothing changes (the card is decided);
 * - [OPEN]: nothing changes here, the screen [spot] opens (« Voir le réglage »);
 * - [INSERT]: [effect] (an [com.pocketstudio.model.EffectType] name) with [params] on the target's
 *   strip: the first one of that type gets them, else it goes in a free slot;
 * - [NUDGE]: knob [param] of the target's [effect] moved by [delta] (none: [params] inserted);
 * - [CHAIN]: several [inserts] (each an [INSERT]);
 * - [BYPASS]: the target's [effect] heard ([value] 0) or not (1), kept with its settings;
 * - [SEND]: send [bus] to [value], or moved by [delta];
 * - [VOLUME]: the fader moved by [delta] dB; [PAN]: the pan to [value];
 * - [TARGET]: the master's loudness target [name] ([com.pocketstudio.model.LoudnessTarget]);
 * - [DUCK]: the Musique bus ducks under the kick, its lows by [value];
 * - [LOW_CUT]: a low cut on every track that has none, at its role's height; [PANS]: each track at
 *   its role's place; [RETURNS]: the reverb and echo returns cut under 200 Hz and over 10 kHz;
 * - [MUTE]: the target out of the [half] ("first") of the [occurrence]-th [section] ("peak");
 * - [COPY_CHAIN]: the target's inserts and sends copied to role [to];
 * - [UNSOLO]: every solo off; [REFUSE]: nothing changes, the card is refused for good (« C'est voulu »);
 *   [LATER]: nothing changes, the card is put off (« Plus tard »).
 * [target]: a role, empty for the card's own.
 */
@Serializable
data class GuideAction(
    val type: String = KEEP,
    val target: String = "",
    val spot: String = "",
    val effect: String = "",
    val params: List<Float> = emptyList(),
    val param: Int = 0,
    val delta: Float? = null,
    val value: Float? = null,
    val bus: Int = 0,
    val name: String = "",
    val to: String = "",
    val section: String = "",
    val occurrence: Int = 0,
    val half: String = "",
    val inserts: List<GuideAction> = emptyList(),
) {
    /** It changes the project (not [KEEP] nor [OPEN]). */
    val edits: Boolean get() = type != KEEP && type != OPEN && type != REFUSE && type != LATER

    companion object {
        const val KEEP = "keep"
        const val OPEN = "open"
        const val INSERT = "insert"
        const val NUDGE = "nudge"
        const val CHAIN = "chain"
        const val BYPASS = "bypass"
        const val SEND = "send"
        const val VOLUME = "volume"
        const val PAN = "pan"
        const val TARGET = "target"
        const val DUCK = "duck"
        const val LOW_CUT = "lowCut"
        const val PANS = "pans"
        const val RETURNS = "returns"
        const val MUTE = "mute"
        const val COPY_CHAIN = "copyChain"
        const val UNSOLO = "unsolo"
        const val REFUSE = "refuse"
        const val LATER = "later"
        val TYPES = setOf(KEEP, OPEN, INSERT, NUDGE, CHAIN, BYPASS, SEND, VOLUME, PAN, TARGET, DUCK, LOW_CUT, PANS, RETURNS, MUTE, COPY_CHAIN, UNSOLO, REFUSE, LATER)
    }
}

/** The seven stages of a parcours (plus the frame, 0), their names in the plan and the summary. */
object GuideStages {
    const val COUNT = 8

    /** Each stage's title in the plan (French, shown through [guideText]). */
    val TITLES = listOf(
        "Le cadre", "Les sons et les motifs", "Les couches", "Les effets de chaque piste",
        "L'arrangement", "Le mouvement", "Les niveaux et le master", "Exporter et partager",
    )

    /** Each stage in a few words, inside a sentence (« Fait : les sons, les effets »). */
    val SHORT = listOf("le cadre", "les sons", "les couches", "les effets", "l'arrangement", "le mouvement", "les niveaux", "l'export")

    /** The stages of the Mode simple; the others are under « Aller plus loin ». */
    val SIMPLE = setOf(1, 4, 6, 7)

    fun title(stage: Int): String = guideText(TITLES.getOrElse(stage) { TITLES.last() })
    fun short(stage: Int): String = guideText(SHORT.getOrElse(stage) { SHORT.last() })
}

/** The parcours on disk, read once each. */
object GuideLibrary {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val cache = ConcurrentHashMap<String, GuideParcours>()

    /** The parcours of style [id] (null when there is none: an unknown style). */
    fun forStyle(id: String): GuideParcours? {
        cache[id]?.let { return it }
        if (!id.matches(SAFE)) return null
        val stream = GuideLibrary::class.java.classLoader?.getResourceAsStream("guides/$id.json") ?: return null
        val p = stream.bufferedReader(Charsets.UTF_8).use { json.decodeFromString(GuideParcours.serializer(), it.readText()) }
            .takeIf { it.format <= GuideParcours.FORMAT } ?: return null
        cache[id] = p
        return p
    }

    /** The card of serious fact [factId] ("alert.<id>" in `guides/alerts.json`), or null. */
    fun alert(factId: String): GuideCard? = forStyle(ALERTS)?.card("alert.$factId")

    /** The parcours of [project]: its style's, the generic one for a song made by hand. */
    fun forProject(project: Project): GuideParcours =
        project.style?.takeIf { it != ALERTS }?.let(::forStyle) ?: forStyle(GuideParcours.GENERIC) ?: GuideParcours(style = GuideParcours.GENERIC)

    private val SAFE = Regex("[a-z0-9_]{1,40}")

    /** The file of the alerts' cards (a parcours of its own, never followed). */
    const val ALERTS = "alerts"
}

/** A text of the Guide's data (a card, a stage) in the interface's language. */
fun guideText(fr: String): String = tr(fr)
