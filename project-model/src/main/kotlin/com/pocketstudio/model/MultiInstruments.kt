package com.pocketstudio.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A downloadable multizone instrument (phase 38): the « instrument » of a pack built by
 * scripts/packs/instrument_pack.py from a free SFZ library (Salamander, Splendid, VSCO 2 CE).
 * Each zone is a recording of the pack (16-bit FLAC, 48 kHz) played over its keys and velocities
 * from its [MultiZone.root], in turn with the zones of the same key ([MultiZone.seq] / [MultiZone.seqLength],
 * the round robins), or when the key is let go ([MultiZone.release]). The engine plays them with
 * MultiSampler; [memoryBytes] is what the zones take there (16-bit).
 */
@Serializable
data class MultiInstrument(
    /** piano, strings, brass, woodwinds, harp: the family shown in the chooser ([MultiInstruments.CATEGORY_LABELS]). */
    val category: String = "",
    /** Seconds the sound takes to die away once the key is let go. */
    val release: Float = 0.5f,
    val attack: Float = 0.001f,
    /** 0..1: how much softer a soft note plays within its velocity layer. */
    val velTrack: Float = 0.5f,
    /** The calibration (middle C at velocity 100 about −20 dBFS RMS, set by the build script). */
    val gainDb: Float = 0f,
    /** dB per second held that a release sample loses. */
    val releaseDecay: Float = 0f,
    /** The library's synth sound a track plays while this instrument is not on the phone (the engine falls back to the synth). */
    val fallback: String = "",
    val memoryBytes: Long = 0,
    val zones: List<MultiZone> = emptyList(),
    /** The credit a CC BY recording asks for, shown under the instrument (empty: none needed). */
    val attribution: String = "",
) {
    fun sanitized(): MultiInstrument = copy(
        category = category.take(20),
        release = release.finiteOr(0.5f).coerceIn(0.02f, 10f),
        attack = attack.finiteOr(0.001f).coerceIn(0f, 2f),
        velTrack = velTrack.finiteOr(0.5f).coerceIn(0f, 1f),
        gainDb = gainDb.finiteOr(0f).coerceIn(-24f, 12f),
        releaseDecay = releaseDecay.finiteOr(0f).coerceIn(0f, 60f),
        fallback = fallback.take(40),
        memoryBytes = memoryBytes.coerceIn(0, MAX_MEMORY_BYTES),
        zones = zones.filter { it.isSafe }.take(MAX_ZONES).map { it.sanitized() },
        attribution = attribution.take(120),
    )

    /** What the engine takes for the whole instrument, in MultiSampler::Param order (Count, Release, Attack, VelTrack, Gain, ReleaseDecay). */
    fun engineParams(): List<Float> = listOf(
        zones.size.toFloat(), release, attack, velTrack, Math.pow(10.0, gainDb / 20.0).toFloat().coerceIn(0f, 4f), releaseDecay,
    )

    companion object {
        /** MultiSampler::kZones. */
        const val MAX_ZONES = 512
        /** No instrument may claim more than this on the phone (the biggest pianos take about 60 MB). */
        const val MAX_MEMORY_BYTES = 160_000_000L
    }
}

@Serializable
data class MultiZone(
    val file: String,
    val lokey: Int = 0,
    val hikey: Int = 127,
    val root: Float = 60f,
    val lovel: Int = 1,
    val hivel: Int = 127,
    /** Its turn among the round robins (1..[seqLength]); 0 = every time. */
    val seq: Int = 0,
    val seqLength: Int = 1,
    /** Played when the key is let go. */
    val release: Boolean = false,
    /** The sustain loop, in frames at 48 kHz ([loopEnd] <= [loopStart]: none). */
    val loopStart: Int = 0,
    val loopEnd: Int = 0,
) {
    val isSafe: Boolean get() = PackManifest.safeName(file) && file.endsWith(".flac")

    fun sanitized(): MultiZone = copy(
        lokey = lokey.coerceIn(0, 127), hikey = hikey.coerceIn(-1, 127), root = root.finiteOr(60f).coerceIn(0f, 127f),
        lovel = lovel.coerceIn(1, 127), hivel = hivel.coerceIn(1, 127), seq = seq.coerceIn(0, 16), seqLength = seqLength.coerceIn(1, 16),
        loopStart = loopStart.coerceAtLeast(0), loopEnd = loopEnd.coerceAtLeast(0),
    )

    /** What the engine takes for this zone, in MultiSampler::ZoneParam order. */
    fun engineValues(): List<Float> = listOf(
        lokey.toFloat(), hikey.toFloat(), lovel.toFloat(), hivel.toFloat(), root, 0f, seq.toFloat(), seqLength.toFloat(),
        if (release) 1f else 0f, loopStart.toFloat(), loopEnd.toFloat(),
    )
}

private fun Float.finiteOr(default: Float) = if (isFinite()) this else default

/**
 * An instrument track playing a downloaded multizone instrument (track engine 3): the pack it
 * comes from and its name. While the pack is not on the phone (another phone, deleted), the track
 * plays its synth sound, which is set to the instrument's fallback when it is chosen.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MultiPatch(
    val pack: String? = null,
    val name: String = "",
    /** The track's own level on top of the instrument's calibration, 0..2. */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val gain: Float = 1f,
) {
    fun sanitized(): MultiPatch = copy(
        pack = pack?.takeIf { PackManifest.safeName(it) },
        name = name.take(40),
        gain = (if (gain.isFinite()) gain else 1f).coerceIn(0f, 2f),
    )
}

/** The multizone instruments the app knows of, and what they are for. */
object MultiInstruments {
    /** The packs' family in the catalogue (scripts/packs/instrument_pack.py). */
    const val FAMILY = "Instruments"

    /** The chooser's names of the categories, in this order (saved in French, translated at display). */
    val CATEGORY_LABELS = linkedMapOf(
        "piano" to "Pianos",
        "strings" to "Cordes",
        "brass" to "Cuivres",
        "woodwinds" to "Bois",
        "harp" to "Harpe",
    )

    /**
     * One instrument of the catalogue: its pack, its name, its category, and the library sound
     * that stands in for it while it is not downloaded.
     */
    data class Entry(val pack: String, val name: String, val category: String, val fallback: String, val description: String)

    const val PIANO_SALAMANDER = "instrument_piano_salamander"
    const val PIANO_SPLENDID = "instrument_piano_splendid"
    const val STRINGS = "instrument_cordes"
    const val PIZZICATO = "instrument_cordes_pizz"
    const val BRASS = "instrument_cuivres"
    const val WOODWINDS = "instrument_bois"
    const val HARP = "instrument_harpe"

    /** The same as scripts/packs/instruments.json (names, fallbacks), in the chooser's order. */
    val KNOWN = listOf(
        Entry(PIANO_SALAMANDER, "Piano à queue", "piano", "Piano électrique", "Un vrai piano à queue, enregistré note par note"),
        Entry(PIANO_SPLENDID, "Piano de concert", "piano", "Piano électrique", "Un piano de concert, plus doux et plus rond"),
        Entry(STRINGS, "Cordes", "strings", "Cordes", "Violons, altos et violoncelles tenus"),
        Entry(PIZZICATO, "Cordes pizzicato", "strings", "Pluck", "Les cordes pincées, courtes"),
        Entry(BRASS, "Cuivres", "brass", "Nappe chaude", "Cors et trompettes"),
        Entry(WOODWINDS, "Bois", "woodwinds", "Lead", "Basson, clarinette et flûte"),
        Entry(HARP, "Harpe", "harp", "Pluck", "Une harpe de concert"),
    )

    fun entry(pack: String?): Entry? = KNOWN.firstOrNull { it.pack == pack }

    /** True for a pack of the catalogue that is an instrument (its own section, not a pack of sounds). */
    fun isInstrument(entry: PackEntry): Boolean = entry.family == FAMILY || entry.id.startsWith("instrument_")

    /**
     * The instrument the assistant gives [part] in [style] when it is on the phone (lo-fi, boom bap,
     * pop, ballads): the song is made the same way without it (its usual synth sound), it never
     * waits for a download.
     */
    fun forStyle(style: String, part: StarterWizard.Part): String? = STYLE_PARTS[style]?.get(part)

    private val STYLE_PARTS: Map<String, Map<StarterWizard.Part, String>> = mapOf(
        "lofi" to mapOf(StarterWizard.Part.Pad to PIANO_SALAMANDER),
        "boom_bap" to mapOf(StarterWizard.Part.Pad to STRINGS),
        "pop" to mapOf(StarterWizard.Part.Pad to PIANO_SALAMANDER),
        "liquid_dnb" to mapOf(StarterWizard.Part.Pad to PIANO_SPLENDID),
        "amapiano" to mapOf(StarterWizard.Part.Pad to PIANO_SPLENDID),
    )

    /** An instrument of [category] fits [part]: no orchestra under the bass, no woodwinds as chords. */
    fun suits(category: String, part: StarterWizard.Part): Boolean = when (part) {
        StarterWizard.Part.Bass -> false
        StarterWizard.Part.Pad -> category in setOf("piano", "strings", "brass", "harp")
        StarterWizard.Part.Lead -> category in setOf("piano", "woodwinds", "brass", "harp", "strings")
    }

    /**
     * The track's synth sound for an instrument (its fallback, a synth preset: the engine plays the
     * track's synth while the instrument is not there), or the plain synth when the library has none of that name.
     */
    fun fallbackSynth(fallback: String): SynthPatch = SoundLibrary.SYNTH.firstOrNull { it.name == fallback }?.sound ?: SynthPatch()

    /**
     * The library sound for [manifest]'s instrument as the assistant's parts take one: the
     * instrument itself (track engine 3), its fallback sound underneath. Null when the pack has no instrument.
     */
    fun partSound(manifest: PackManifest): StarterWizard.PartSound? {
        val inst = manifest.instrument ?: return null
        return StarterWizard.PartSound(
            manifest.name,
            synth = fallbackSynth(inst.fallback.ifBlank { entry(manifest.id)?.fallback.orEmpty() }),
            multi = MultiPatch(manifest.id, manifest.name),
            chord = inst.category != "woodwinds",
            family = inst.category,
        )
    }
}

/** The packs the project's tracks play as multizone instruments (to load). */
val Project.multiPacks: Set<String> get() = instrumentTracks.filter { it.engine == InstrumentTrack.ENGINE_MULTI }.mapNotNull { it.multi.pack }.toSet()
