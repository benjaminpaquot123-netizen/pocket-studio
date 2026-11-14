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
    /**
     * The acid track's name, the same in every language (Benjamin, 2026-10-29: « Acid 303 », not a
     * brand name). It was « Basse acide » before: a name saved then is shown as this one.
     */
    const val ACID = "Acid 303"

    /** App names renamed since: the old French one → the one shown now. */
    private val RENAMED = mapOf("Basse acide" to ACID)

    /** Whole names. */
    private val EXACT: Set<String> by lazy {
        (
            listOf(
                "Nouveau projet", "Mon premier rythme", "Mon projet", "Mon morceau", "Mon son",
                "Synthé", "Basse", "Nappe", "Lead", "Batterie", ACID, "Remix", "Piste", "Sample",
                "Son importé", "Morceau 1 mesure", "Voix", SimpleMelody.NAME, StarterWizard.RISER_TRACK, StarterWizard.OLD_RISER_TRACK, StarterWizard.SUB_TRACK, StarterWizard.SUB_DROP_TRACK,
                StarterWizard.COUNTER_TRACK,
            ) + TrackKind.entries.map { it.trackName } + StarterWizard.CHORD_TRACK_NAMES + Demos.ALL.map { it.first } + StarterWizard.STYLES.map { it.name } +
                // The ready-to-play live pads and the party sounds put on a pad (Benjamin's list n° 12).
                LiveStarter.PADS.map { it.name } + PartySounds.ALL.map { it.name } +
                // Audit ultra round 2 (n° 17): a track named after the library sound it plays (« Basse sub »).
                SoundLibrary.SYNTH.map { it.name } + SoundLibrary.PLAITS.map { it.name } + MultiInstruments.KNOWN.map { it.name } +
                // Tour 6 lot 7 (T2): a pad named after its built-in voice (« Rumble », « Kick studio »: the tutorial's kit
                // keeps it), shown as the tutorial's step says it (럼블, ランブル).
                PadVoice.NAMES.filter { it.isNotBlank() }
            ).toSet()
    }

    /** Names with a number. */
    // « Audio %d »: an audio track's own name (audit ultra round 2, n° 20: « Audio 1 » in every language).
    private val NUMBERED = listOf("Piste %d", "Canal %d", "Morceau %d mesures", "Audio %d")

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
     * The kit's own voice on each of the 16 pads, in pad order: what a pad with no other sound on it shows (the app's
     * DrumKitInfo.names, translated when read). Not whole names of [EXACT]: a track or a file the user called « Rim »
     * is never translated; only [shownPad] reads them, for a pad.
     */
    val KIT_VOICES = listOf(
        "Kick", "Kick 2", "Caisse", "Caisse 2", "Clap", "Charley", "Charley ouvert", "Tom grave",
        "Tom médium", "Tom aigu", "Rim", "Cowbell", "Cymbale", "Shaker", "Conga", "Zap",
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
    val TEXTS: List<String> get() = EXACT.toList() + NUMBERED + TIMED + STEMS + KIT_PADS + KIT_VOICES + COPY + BEFORE_SESSION + com.pocketstudio.model.i18n.I18n.EVERYDAY.values

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
        RENAMED[name]?.let { return tr(it) }
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

    /**
     * The name pad [lane] shows (as the pad screen does): the [sample] put on it ([shownSample]: a file of the user's
     * stays as it is), else its studio voice [voice] (a [PadVoice] kind: « 808 », « Rumble »), else its kit voice
     * ([KIT_VOICES]), in the interface's language and with the Mode simple's words.
     */
    fun shownPad(lane: Int, sample: String?, voice: Int?): String =
        sample?.takeIf { it.isNotBlank() }?.let { shownSample(it) }
            ?: voice?.let { kind -> if (kind == PadVoice.BASS_808) "808" else PadVoice.NAMES.getOrNull(kind)?.takeIf { it.isNotBlank() }?.let { tr(it) } }
            ?: KIT_VOICES.getOrNull(lane)?.let { tr(it) } ?: "Pad ${lane + 1}"

    /** The French of a whole default name given in any language ("Bass", "Bas" → "Basse"), or null for any other name. */
    fun french(name: String): String? = if (name in EXACT) name else RENAMED[name] ?: fromTranslation[name]

    /**
     * Audit ultra round 3 (n° 26, seat C c: « Mélodie » became « FM piano », then « Ma mélodie » no longer found the
     * melody and came with another sound): names that say what a track does in the song. A new sound never renames them.
     */
    private val ROLES: Set<String> by lazy {
        setOf(
            TrackKind.Melody.trackName, TrackKind.Chords.trackName, TrackKind.Bass.trackName, TrackKind.Arp.trackName,
            "Lead", "Nappe", "Batterie", "Voix", SimpleMelody.NAME, StarterWizard.COUNTER_TRACK,
            StarterWizard.RISER_TRACK, StarterWizard.OLD_RISER_TRACK, StarterWizard.SUB_TRACK, StarterWizard.SUB_DROP_TRACK,
        )
    }

    /** [name] (in any language) says the track's role in the song (« Mélodie », « Basse »), not its sound. */
    fun isRole(name: String): Boolean = french(name)?.let { it in ROLES } == true

    /** True when [name] is one of the app's own names (in any language). */
    fun isDefault(name: String): Boolean =
        name in EXACT || name in RENAMED || name in fromTranslation || numbered.any { it.first.matches(name) } || timed.any { it.first.matches(name) }
}

/**
 * At most [max] chars of this name, never cutting a letter in two (relecture tour 1, mineur): an
 * emoji's surrogate pair, a letter and its combining sign (« é » typed as e + ´, Hindi's vowel signs)
 * stay whole or go together.
 */
fun String.takeWhole(max: Int): String {
    if (length <= max) return this
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(this)
    val end = it.preceding(max + 1).coerceAtLeast(0)
    return substring(0, end)
}
