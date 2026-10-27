package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr

/**
 * Names the app gives by itself: a project ("Nouveau projet", the starter beat, a style's or a
 * demo's name), a track ("Piste 3", "Basse", "Canal 10"), a pad's sound ("Morceau 4 mesures").
 * They are saved in French and shown in the interface's language, so a project reads the same
 * whatever the language it was made in. Names saved by older versions in English or Dutch
 * ("Track 2", "New project") are recognised too. Any other name was typed by the user (or comes
 * from a file) and is shown as it is, even when it happens to be a word the app translates: an
 * imported "Bass groove.wav" or "Basse live.wav" is never turned into "Basse groove" or "Bass live".
 */
object DefaultNames {
    /** Whole names. */
    private val EXACT: Set<String> by lazy {
        (
            listOf(
                "Nouveau projet", "Mon premier rythme", "Mon projet", "Mon morceau", "Mon son",
                "Synthé", "Basse", "Nappe", "Lead", "Batterie", "Basse acide", "Remix", "Piste", "Sample",
                "Son importé", "Morceau 1 mesure", "Voix", SimpleMelody.NAME, StarterWizard.RISER_TRACK, StarterWizard.OLD_RISER_TRACK, StarterWizard.SUB_TRACK, StarterWizard.SUB_DROP_TRACK,
                StarterWizard.COUNTER_TRACK,
            ) + TrackKind.entries.map { it.trackName } + StarterWizard.CHORD_TRACK_NAMES + Demos.ALL.map { it.first } + StarterWizard.STYLES.map { it.name }
            ).toSet()
    }

    /** Names with a number. */
    private val NUMBERED = listOf("Piste %d", "Canal %d", "Morceau %d mesures")

    /** Names with a time of day: a microphone take ("Prise 21:14"), a pad recorded at the mic ("Micro 21:14:05"). */
    private val TIMED = listOf("Prise %s", "Micro %s")

    /** The parts of a separated song, at the start of a pad cut from it ("Batterie · Da Funk"). */
    private val STEMS = listOf("Batterie", "Basse", "Autres", "Voix")

    /** A drum kit's 16 pads, in pad order, at the end of a pad's sound loaded from a kit ("Standard · Caisse"). */
    val KIT_PADS = listOf(
        "Kick", "Kick 2", "Caisse", "Caisse 2", "Clap", "Charley", "Charley ouvert", "Tom grave",
        "Tom médium", "Tom aigu", "Rim", "Cowbell", "Crash", "Maracas", "Conga", "Tambourin",
    )

    /**
     * Between the two halves of a pad's sound the app names ("Batterie · Da Funk", "Standard · Caisse"):
     * a file name never gets a part translated, only these app-made names do.
     */
    private const val JOIN = " · "

    private const val COPY = "%s (copie)"

    /** A copy of the version before the last session (ProjectCatalog.restoreOpening). */
    const val BEFORE_SESSION = "%s (avant la séance)"

    /** Every French text these names are made of (each one needs its translations). */
    val TEXTS: List<String> get() = EXACT.toList() + NUMBERED + TIMED + STEMS + KIT_PADS + COPY + BEFORE_SESSION + com.pocketstudio.model.i18n.I18n.EVERYDAY.values

    /** The pad's sound cut from the [stem] part (one of "Batterie", "Basse", "Autres", "Voix") of the separated song [song]. */
    fun stemPad(stem: String, song: String): String = "$stem$JOIN$song".take(40)

    /** The sound of pad [pad] (0..15) loaded from the drum kit [kit] (its preset name, as `tr` knows it). */
    fun kitPad(kit: String, pad: Int): String = "${kit.take(18)}$JOIN${KIT_PADS[pad]}"

    /** The translations (in every language) of the whole names, back to their French. */
    private val fromTranslation: Map<String, String> by lazy {
        val map = HashMap<String, String>()
        Lang.entries.filter { it != Lang.FR }.forEach { lang ->
            val table = I18n.table(lang)
            EXACT.forEach { fr -> table[fr]?.takeIf { it.isNotBlank() && it !in EXACT }?.let { map.putIfAbsent(it, fr) } }
        }
        map
    }

    /** One pattern per language for each numbered name, with the French template it stands for. */
    // At most 6 digits: a longer number typed by the user is not one of ours (and would not fit an Int).
    private val numbered: List<Pair<Regex, String>> by lazy { patterns(NUMBERED, """(\d{1,6})""") }
    private val timed: List<Pair<Regex, String>> by lazy { patterns(TIMED, """(\d{1,2}:\d{2}(?::\d{2})?)""") }
    private val copies: List<Pair<Regex, String>> by lazy { patterns(listOf(COPY, BEFORE_SESSION), "(.+)") }

    private fun patterns(templates: List<String>, arg: String): List<Pair<Regex, String>> =
        templates.flatMap { fr ->
            (listOf(fr) + Lang.entries.mapNotNull { I18n.table(it)[fr]?.takeIf { t -> t.isNotBlank() } }).distinct().map { t ->
                Regex(t.split(Regex("""%(\d\$)?[sd]""")).joinToString(arg) { Regex.escape(it) }) to fr
            }
        }

    private val trailingNumber = Regex("""^(.+) (\d+)$""")

    /** [name] in the interface's language when the app gave it, else as it is. */
    fun shown(name: String): String {
        if (name in EXACT) return tr(name)
        fromTranslation[name]?.let { return tr(it) }
        numbered.forEach { (re, fr) -> re.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()?.let { return tr(fr, it) } }
        timed.forEach { (re, fr) -> re.matchEntire(name)?.let { return tr(fr, it.groupValues[1]) } }
        copies.forEach { (re, fr) -> re.matchEntire(name)?.let { return tr(fr, shown(it.groupValues[1])) } }
        // A copy of a default name ("Basse 2"), or "Mon projet 3".
        trailingNumber.matchEntire(name)?.let { m ->
            val (base, n) = m.destructured
            val shownBase = shown(base)
            if (shownBase != base || isDefault(base)) return "$shownBase $n"
        }
        return name
    }

    /**
     * A pad's sound as shown: also "Batterie · <song>" for a part cut from a separated song and
     * "<kit> · Caisse" for a kit's sound. Only these exact French forms, the ones the app saves, are
     * translated: any other name (an imported file) stays as it is.
     */
    fun shownSample(name: String): String {
        val whole = shown(name)
        if (whole != name || isDefault(name)) return whole
        val cut = name.indexOf(JOIN)
        if (cut > 0) {
            val first = name.substring(0, cut)
            val rest = name.substring(cut + JOIN.length)
            if (first in STEMS && rest.isNotEmpty()) return tr(first) + JOIN + rest
            if (rest in KIT_PADS) return tr(first) + JOIN + tr(rest)
        }
        return name
    }

    /** The French of a whole default name given in any language ("Bass", "Bas" → "Basse"), or null for any other name. */
    fun french(name: String): String? = if (name in EXACT) name else fromTranslation[name]

    /** True when [name] is one of the app's own names (in any language). */
    fun isDefault(name: String): Boolean =
        name in EXACT || name in fromTranslation || numbered.any { it.first.matches(name) } || timed.any { it.first.matches(name) }
}
