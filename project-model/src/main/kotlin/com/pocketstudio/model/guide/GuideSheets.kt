package com.pocketstudio.model.guide

import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.Buses
import com.pocketstudio.model.CurveShape
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Project
import com.pocketstudio.model.matchingFx
import com.pocketstudio.model.shapeIn
import com.pocketstudio.model.track

/**
 * A track's sheet (docs/PLAN-GUIDE.md §3.3), in its five columns: [sound] (« Son »), [plays] and
 * [parts] (« Quand »), [chain] and [sends] (« Quel effet et combien »), [often] (« Combien de
 * fois »), [elsewhere] (« Ailleurs ? »); then its [history] and the [facts] about it. Texts are
 * French keys ([guideText] shows them) unless said otherwise.
 */
data class GuideSheet(
    val track: GuideTrack,
    val sound: SheetSound,
    val plays: List<SheetSection>,
    /** One box per song part: it plays there. */
    val parts: List<Boolean>,
    /** What the style advises for it (its stage 1 card's why), when the parcours has one. */
    val advice: String?,
    val chain: List<SheetEffect>,
    /** Its sends to the shared reverb and echo, 0..1. */
    val sends: List<Float>,
    val often: SheetOften,
    val elsewhere: List<GuideProposal>,
    val history: List<GuideChange>,
    val facts: List<GuideFact>,
    /** The cards about it, in order. */
    val cards: List<String>,
    /** G3: [history] as lines, one per group (an option, a gesture), newest first. */
    val lines: List<GuideHistoryLine> = emptyList(),
)

/** « Son »: what plays ([label], already in the interface's language), its layers' names (an instrument track's: [com.pocketstudio.model.Layers.shownTitle]). */
data class SheetSound(val label: String, val layers: List<String> = emptyList())

/** « Quand »: a section of the song and the bars the track plays in it. */
data class SheetSection(val label: String, val kind: GuideSection.Kind, val start: Int, val bars: Int, val playedBars: Int)

/**
 * An effect of the chain: its [type], [why] (a French key), its first setting ([valueLabel], French,
 * and [value], shown), whether it is bypassed and whether it plays in every section.
 */
data class SheetEffect(
    val slot: Int,
    val type: EffectType,
    val why: String,
    val valueLabel: String,
    val value: String,
    val bypassed: Boolean,
    val everywhere: Boolean,
)

/**
 * « Combien de fois »: the ready-made effect the chain is ([preset], a French name, or null), whether
 * it plays once for the whole song, and the curves that move the track, section by section.
 */
data class SheetOften(val preset: String?, val onceForSong: Boolean, val lanes: List<SheetLane>)

/** A curve on the track: what it moves ([what], French: « Volume », the effect's name…), its shape, and the sections where it moves. */
data class SheetLane(val what: String, val shape: CurveShape.Kind, val sections: List<String>)

object GuideSheets {
    fun of(p: Project, facts: GuideFacts, parcours: GuideParcours, simple: Boolean): List<GuideSheet> {
        val song = facts.song
        return song.tracks.filter { song.plays(it.key) || it.key.startsWith("inst:") }.map { t -> sheet(p, facts, parcours, t, simple) }
    }

    private fun sheet(p: Project, facts: GuideFacts, parcours: GuideParcours, t: GuideTrack, simple: Boolean): GuideSheet {
        val song = facts.song
        val strip = p.mixer.channels.getOrNull(t.channel)
        val inserts = strip?.inserts.orEmpty()
        val chain = inserts.mapIndexedNotNull { slot, ins ->
            if (ins.effect == EffectType.NONE) return@mapIndexedNotNull null
            val param = ins.effect.params.firstOrNull()
            SheetEffect(
                slot, ins.effect, parcours.why(t.role, ins.effect), param?.label.orEmpty(), param?.format?.invoke(ins.params.getOrElse(0) { 0f }).orEmpty(),
                ins.bypass, ins.scenes == null,
            )
        }
        val sections = song.sections.map { s -> SheetSection(s.label, s.kind, s.start, s.bars, (s.start until s.end).count { song.sounds(t.key, it) }) }
        val parts = p.song.indices.map { i -> (0 until song.partBars[i]).any { song.sounds(t.key, song.partStarts[i] + it) } }
        val lanes = p.automation.filter { it.target.ch == t.channel && it.target.kind != AutoTarget.BUS_PARAM && it.target.kind != AutoTarget.LIVE }.mapNotNull { lane ->
            val shape = lane.shapeIn(p, null) ?: return@mapNotNull null
            if (shape.kind == CurveShape.Kind.EMPTY) return@mapNotNull null
            val moving = song.sections.filter { s -> (lane.shapeIn(p, s.scene)?.kind ?: CurveShape.Kind.STEADY) != CurveShape.Kind.STEADY }.map { it.label }.distinct()
            SheetLane(laneWhat(p, lane.target), shape.kind, moving)
        }
        val cards = parcours.cardsFor(simple).filter { it.target == t.role }
        val advice = cards.firstOrNull { it.stage == 1 }?.why
        val elsewhere = cards.flatMap { it.elsewhere }.distinctBy { it.id }
        val layers = t.key.removePrefix("inst:").toIntOrNull()?.takeIf { t.key.startsWith("inst:") }?.let { p.track(it).layers.map(com.pocketstudio.model.Layers::shownTitle) }.orEmpty()
        val history = p.guide.historyOf(t.key)
        return GuideSheet(
            t, SheetSound(soundOf(p, t), layers), sections, parts, advice, chain, strip?.sends.orEmpty(),
            SheetOften(strip?.matchingFx()?.takeIf { it.id != "none" }?.name, inserts.all { it.effect == EffectType.NONE || it.scenes == null }, lanes),
            elsewhere, history, facts.list.filter { t.key in it.tracks }, cards.map { it.id },
            GuideHistory.lines(history) { id -> parcours.card(id)?.titleFor(simple) },
        )
    }

    private fun soundOf(p: Project, t: GuideTrack): String = when {
        // A library sound by its name (« Basse ronde »), else what kind of sound it is.
        t.key.startsWith("inst:") -> soundName(p.track(t.key.removePrefix("inst:").toInt()))
        t.key.startsWith("pad:") -> t.key.removePrefix("pad:").toInt().let { lane ->
            p.drums.lanes.getOrNull(lane)?.sample?.name?.let(com.pocketstudio.model.DefaultNames::shownSample) ?: com.pocketstudio.model.DefaultNames.shown(t.name)
        }
        t.key == GuideTrack.ACID -> "TB-303"
        else -> com.pocketstudio.model.DefaultNames.shown(t.name)
    }

    /** Instrument track [t]'s sound by its library name (« Basse growl »), else what kind of sound it is; in the interface's language. */
    fun soundName(t: com.pocketstudio.model.InstrumentTrack): String {
        val name = when {
            t.instrument != null -> null
            t.engine == com.pocketstudio.model.InstrumentTrack.ENGINE_SYNTH -> com.pocketstudio.model.SoundLibrary.synthNameOf(t.synth)
            t.engine == com.pocketstudio.model.InstrumentTrack.ENGINE_PLAITS ->
                com.pocketstudio.model.SoundLibrary.PLAITS.firstOrNull { it.sound == t.plaits.copy(level = it.sound.level, gain = it.sound.gain) }?.name
            else -> null
        }
        return name?.let { libraryName -> com.pocketstudio.model.i18n.tr(libraryName) } ?: t.soundLabel
    }

    /** What a lane moves, as a French key. */
    private fun laneWhat(p: Project, t: AutoTarget): String = when (t.kind) {
        AutoTarget.VOLUME -> "Volume"
        AutoTarget.PAN -> "Pan"
        AutoTarget.SEND -> Buses.NAMES.getOrElse(t.slot) { "Réverbe" }
        AutoTarget.INSERT_PARAM -> p.mixer.channels.getOrNull(t.ch)?.inserts?.getOrNull(t.slot)?.effect?.label ?: "Effet"
        else -> "Son"
    }
}
