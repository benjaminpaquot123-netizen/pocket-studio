package com.pocketstudio.app.midi

import com.pocketstudio.app.live.LiveFxUi
import com.pocketstudio.app.mixer.StripInfo
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.Buses
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.track

/** The screens a controller's knobs follow (phase 39): each has its own eight settings. */
enum class KnobScreen {
    Rythme, Acide, Synthe, Mixeur, Live;

    val label: String get() = when (this) {
        Rythme -> tr("Rythme")
        Acide -> tr("Acide")
        Synthe -> tr("Synthé")
        Mixeur -> tr("Mixeur")
        Live -> tr("Live")
    }

    companion object {
        /** The screen shown: the Live page over everything, else the tab ([tab] is its name); the other tabs get the mixer's knobs. */
        fun of(tab: String, live: Boolean): KnobScreen = when {
            live -> Live
            tab == "Rythme" -> Rythme
            tab == "Acide" -> Acide
            tab == "Synthe" -> Synthe
            else -> Mixeur
        }
    }
}

/**
 * A setting a knob moves: an automation [target] (the same numbers as MIDI learn), [live] for
 * the Live page's own 303 knobs (heard there, as its screen knobs are, not saved in the sound).
 */
data class KnobTarget(val target: AutoTarget, val live: Boolean = false) {
    /** One number, for the settings and the soft takeover. */
    val key: Long get() = (if (live) 1L shl 32 else 0L) or (target.pack().toLong() and 0xFFFFFFFFL)

    companion object {
        fun of(key: Long): KnobTarget {
            val p = (key and 0xFFFFFFFFL).toInt()
            return KnobTarget(AutoTarget(kind = (p shr 16) and 0xFF, ch = (p shr 12) and 0xF, slot = (p shr 8) and 0xF, param = p and 0xFF), live = key shr 32 != 0L)
        }
    }
}

/** A knob's setting and its name in the hint. */
data class KnobSlot(val target: KnobTarget, val label: String)

/** What the Live page's knobs are at now (not in the project: until Stop). */
data class LiveKnobs(val fx: LiveFxUi = LiveFxUi(), val returns: List<Float> = listOf(0.5f, 0.5f), val acid: Map<Int, Float> = emptyMap())

/**
 * The eight most useful knobs of each screen, the way its own knobs read: the 303 on Acide, the
 * selected track's synth (or Plaits, or its strip for a sampled sound) on Synthé, the first eight
 * faders on the Mixeur (and the tabs without knobs of their own), the big effects on Live, the
 * drums' strip on Rythme. [candidates] adds what « Changer » can offer.
 */
object KnobPages {
    private fun acid(param: Int, label: String, live: Boolean = false) = KnobSlot(KnobTarget(AutoTarget.acid(param), live), label)

    private fun strip(ch: Int): List<KnobSlot> = listOf(
        KnobSlot(KnobTarget(AutoTarget(AutoTarget.VOLUME, ch)), tr("Volume")),
        KnobSlot(KnobTarget(AutoTarget(AutoTarget.PAN, ch)), tr("Gauche / droite")),
        KnobSlot(KnobTarget(AutoTarget.macro(ch, 0)), tr("Couleur")),
        KnobSlot(KnobTarget(AutoTarget.macro(ch, 1)), tr("Saleté")),
        KnobSlot(KnobTarget(AutoTarget.macro(ch, 2)), tr("Mouvement")),
        KnobSlot(KnobTarget(AutoTarget.macro(ch, 3)), tr("Espace")),
        KnobSlot(KnobTarget(AutoTarget(AutoTarget.SEND, ch, 0)), tr("Envoi réverbe")),
        KnobSlot(KnobTarget(AutoTarget(AutoTarget.SEND, ch, 1)), tr("Envoi écho")),
    )

    private val ACID: List<KnobSlot> get() = listOf(
        acid(0, tr("Coupure")), acid(1, tr("Résonance")), acid(2, tr("Enveloppe")), acid(3, tr("Déclin")),
        acid(4, tr("Accent")), acid(7, tr("Saturation")), acid(5, tr("Accordage")), acid(8, tr("Volume")),
    )

    private fun synth(track: Int, param: Int) = KnobSlot(KnobTarget(AutoTarget.synth(track, param)), com.pocketstudio.app.synth.synthKnobName(param))

    private val SYNTH_FIRST = listOf(
        SynthPatch.CUTOFF, SynthPatch.RESONANCE, SynthPatch.FILTER_ENV, SynthPatch.A_ATTACK, SynthPatch.A_RELEASE,
        SynthPatch.OSC2_DETUNE, SynthPatch.WIDTH, SynthPatch.LEVEL,
    )

    private fun plaits(track: Int): List<KnobSlot> {
        val names = listOf(tr("Harmoniques"), tr("Timbre"), tr("Morph"), tr("Déclin"), tr("Couleur"), tr("Relâche"), tr("Volume"))
        return names.mapIndexed { i, n -> KnobSlot(KnobTarget(AutoTarget.plaits(track, i + 1)), n) }
    }

    private val LIVE: List<KnobSlot> get() = listOf(
        KnobSlot(KnobTarget(AutoTarget.live(AutoTarget.LIVE_FILTER)), tr("Filtre DJ")),
        KnobSlot(KnobTarget(AutoTarget.live(AutoTarget.LIVE_RESONANCE)), tr("Résonance")),
        KnobSlot(KnobTarget(AutoTarget.live(AutoTarget.LIVE_BUILD_UP)), tr("Montée")),
        KnobSlot(KnobTarget(AutoTarget.live(AutoTarget.LIVE_REVERB_RETURN)), tr("Réverbe")),
        KnobSlot(KnobTarget(AutoTarget.live(AutoTarget.LIVE_ECHO_RETURN)), tr("Écho")),
        acid(0, tr("303 : coupure"), live = true),
        acid(1, tr("303 : résonance"), live = true),
        acid(2, tr("303 : enveloppe"), live = true),
    )

    /** The app's eight for [screen] ([track]: the selected instrument track; [strips]: the mixer's, in order). */
    fun slots(screen: KnobScreen, p: Project, track: Int, strips: List<StripInfo>): List<KnobSlot> = when (screen) {
        KnobScreen.Acide -> ACID
        KnobScreen.Live -> LIVE
        KnobScreen.Rythme -> strip(0)
        KnobScreen.Mixeur -> faders(strips)
        KnobScreen.Synthe -> when (p.track(track).engine) {
            0 -> if (p.track(track).instrument == null) SYNTH_FIRST.map { synth(track, it) } else strip(InstrumentTrack.mixerChannel(track))
            1 -> plaits(track) + KnobSlot(KnobTarget(AutoTarget(AutoTarget.VOLUME, InstrumentTrack.mixerChannel(track))), tr("Volume de la piste"))
            else -> strip(InstrumentTrack.mixerChannel(track))
        }
    }.take(SLOTS)

    /** The first eight strips' faders, named after their tracks. */
    private fun faders(strips: List<StripInfo>): List<KnobSlot> =
        strips.filter { it.ch != Mixer_MASTER && !com.pocketstudio.model.Mixer.isPad(it.ch) }.take(SLOTS)
            .map { KnobSlot(KnobTarget(AutoTarget(AutoTarget.VOLUME, it.ch)), com.pocketstudio.model.DefaultNames.shown(it.name)) }

    private const val Mixer_MASTER = com.pocketstudio.model.Mixer.MASTER

    /** Everything « Changer » offers on [screen]: its eight first, then the rest of what the screen shows. */
    fun candidates(screen: KnobScreen, p: Project, track: Int, strips: List<StripInfo>): List<KnobSlot> {
        val extra = when (screen) {
            KnobScreen.Acide -> strip(AutoTarget.ACID_CHANNEL)
            KnobScreen.Synthe -> if (p.track(track).engine == 0 && p.track(track).instrument == null) {
                (com.pocketstudio.app.synth.SYNTH_LABELS.map { it.first } + com.pocketstudio.app.synth.S4_LABELS.map { it.first }).map { synth(track, it) } +
                    strip(InstrumentTrack.mixerChannel(track))
            } else strip(InstrumentTrack.mixerChannel(track))
            KnobScreen.Live -> ACID.take(5).map { it.copy(target = it.target.copy(live = true), label = tr("303 : %s", it.label)) }
            KnobScreen.Rythme -> faders(strips)
            KnobScreen.Mixeur -> strips.filter { it.ch != Mixer_MASTER && !com.pocketstudio.model.Mixer.isPad(it.ch) }.flatMap { s ->
                val n = com.pocketstudio.model.DefaultNames.shown(s.name)
                listOf(
                    KnobSlot(KnobTarget(AutoTarget(AutoTarget.PAN, s.ch)), tr("%s : gauche / droite", n)),
                    KnobSlot(KnobTarget(AutoTarget(AutoTarget.SEND, s.ch, 0)), tr("%s : envoi réverbe", n)),
                )
            }
        }
        return (slots(screen, p, track, strips) + extra).distinctBy { it.target }
    }

    /**
     * The knobs as they play on [screen]: the app's eight, each one replaced by the setting the
     * user chose for it ([overrides]: knob → [KnobTarget.key]), named from [candidates].
     */
    fun resolved(screen: KnobScreen, p: Project, track: Int, strips: List<StripInfo>, overrides: Map<Int, Long>): List<KnobSlot> {
        val base = slots(screen, p, track, strips)
        if (overrides.isEmpty()) return base
        val all = candidates(screen, p, track, strips)
        return List(maxOf(base.size, (overrides.keys.maxOrNull() ?: -1) + 1).coerceAtMost(SLOTS)) { i ->
            overrides[i]?.let { key -> all.firstOrNull { it.target.key == key } } ?: base.getOrNull(i) ?: KnobSlot(KnobTarget(AutoTarget(AutoTarget.VOLUME, 0)), tr("Volume"))
        }
    }

    /** Where [t]'s setting is now, 0..1, as its knob on screen shows it. */
    fun valueOf(t: KnobTarget, p: Project, live: LiveKnobs): Float {
        val a = t.target
        val ch = p.mixer.channels.getOrNull(a.ch)
        return when (a.kind) {
            AutoTarget.ACID -> if (t.live) live.acid[a.param] ?: p.acid.sound.values().getOrElse(a.param) { 0f } else p.acid.sound.values().getOrElse(a.param) { 0f }
            AutoTarget.SYNTH -> a.track?.let { p.track(it).synth.get(a.param) } ?: 0f
            AutoTarget.PLAITS -> a.track?.let { p.track(it).plaits.engineValues().getOrElse(a.param) { 0f } } ?: 0f
            AutoTarget.VOLUME -> ch?.volume ?: 0f
            AutoTarget.PAN -> ((ch?.pan ?: 0f) + 1f) / 2f
            AutoTarget.SEND -> ch?.sends?.getOrNull(a.slot.coerceIn(0, Buses.COUNT - 1)) ?: 0f
            AutoTarget.MACRO -> ch?.macros?.getOrNull(a.param) ?: 0f
            AutoTarget.LIVE -> when (a.param) {
                AutoTarget.LIVE_REVERB_RETURN -> live.returns.getOrElse(0) { 0.5f }
                AutoTarget.LIVE_ECHO_RETURN -> live.returns.getOrElse(1) { 0.5f }
                else -> live.fx.value(a.param, AutoTarget.liveRest(a.param))
            }
            else -> 0f
        }.coerceIn(0f, 1f)
    }

    const val SLOTS = com.pocketstudio.model.midi.KnobLayout.SLOTS
}
