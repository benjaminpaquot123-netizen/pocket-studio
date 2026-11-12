package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.log10

/**
 * « Le kick disparaît » (Benjamin, APK 233, his Acidcore; docs/REVIEW.md, corrections n° 6). The sound itself is
 * measured by the song bench's kick pass (scripts/song-bench.sh, ps_measure --punch: how much one kick hit lifts the
 * export, part by part, against the drums alone; report.py fails a part that buries its kick). Here, what the
 * assistant's songs must keep so that it does not come back: the cause found (a saturation that made a line louder
 * than its own level) and the fix of the one style the bench still flagged (the liquid's reese).
 */
class KickInTheMixTest {
    private fun song(style: StarterWizard.Style): Project {
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        return StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts)
    }

    private val songs by lazy { StarterWizard.STYLES.associateWith(::song) }

    /**
     * Acidcore's 303 saturation (drive 0.45) without « Volume constant » made the 303 10.7 LU louder: the master then
     * held it with the kick, a hit lifted the Drop by 0.2 dB (4.9 with the drums alone). Every saturation the assistant
     * puts on a line that is not the drums (their strip, the pads': the kick's own grit) keeps its level, whatever its
     * drive. The master's are left out: they come after the mix, the loudness target sets their level.
     */
    @Test
    fun noSaturationMakesALineLouderThanItsOwnLevel() {
        var seen = 0
        for ((style, p) in songs) {
            for ((ch, strip) in p.mixer.channels.withIndex()) {
                if (ch == 0 || ch == Mixer.MASTER || Mixer.isPad(ch)) continue
                for (insert in strip.inserts.filter { it.effect == EffectType.SATURATION }) {
                    seen++
                    assertTrue("${style.id}: the saturation on strip $ch (${insert.params}) must keep its level",
                        insert.params[EffectType.SAT_COMPENSATION] >= 0.5f)
                }
            }
        }
        assertTrue("the rule reads real saturations (acidcore's 303, the basses' soft clip…)", seen > 0)
    }

    private fun gainDb(t: InstrumentTrack): Float = when (t.engine) {
        0 -> SynthPatch.gainDb(t.synth.get(SynthPatch.GAIN))
        1 -> SynthPatch.gainDb(t.plaits.gain)
        else -> 20f * log10(t.sampled.gain)
    }

    private fun soundDb(s: StarterWizard.PartSound): Float = when {
        s.sampled != null -> 20f * log10(s.sampled.gain)
        s.plaits != null -> SynthPatch.gainDb(s.plaits.gain)
        else -> SynthPatch.gainDb(s.synth.get(SynthPatch.GAIN))
    }

    /** The bass's level over its sound's own: what the assistant's mix gives the style's bass. */
    private fun bassOverSound(id: String): Float {
        val style = StarterWizard.style(id)!!
        val p = songs.getValue(style)
        val sound = StarterWizard.soundsFor(Part.Bass, style).first()
        val others = setOf(StarterWizard.SUB_TRACK, StarterWizard.SUB_DROP_TRACK, StarterWizard.BASS_SWITCH_TRACK)
        val bass = p.track(p.bassTrackIds().first { id -> p.track(id).name !in others })
        return gainDb(bass) - soundDb(sound)
    }

    /**
     * The kick pass (2026-10-30): the liquid's reese filled the gaps of its breakbeat, a kick hit lifted its Drop by
     * 1.9 dB (4.7 with the drums alone, the floor is half of that). Its bass 3 dB further back: 2.3 dB. The same mix
     * otherwise as the drum & bass's.
     */
    @Test
    fun theLiquidsReeseSitsUnderItsKick() {
        assertEquals(-3f, WizardMix.BASS_TRIM_DB.getValue("liquid_dnb"), 0f)
        assertEquals(bassOverSound("dnb") - 3f, bassOverSound("liquid_dnb"), 0.05f)
        assertEquals(setOf("liquid_dnb"), WizardMix.BASS_TRIM_DB.keys)
    }
}
