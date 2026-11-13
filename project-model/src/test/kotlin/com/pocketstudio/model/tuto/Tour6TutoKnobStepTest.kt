package com.pocketstudio.model.tuto

import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.track
import com.pocketstudio.model.withTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Tour 6, control seat (tuto Pro, techno, chapter 7, « lead_knobs »): « monte la Coupure et la Résonance d'au moins
 * 15 chacune » was refused for a move of +15 read on the screen; only +20 ticked it. The screen shows a knob as a
 * rounded percent ((value × 100), rounded), so a user who moves the number by 15 can leave a raw delta a little under
 * 0.15; the old strict raw test `v ≥ was + 0.15` then failed although the screen said +15. The step must count what
 * the instruction and the screen say: the rounded percent gone up by at least 15.
 */
class Tour6TutoKnobStepTest {
    private val target = TutoTarget.of("techno")!!
    private val step = TutoCourse.of("techno")!!.chapters.flatMap { it.steps }.first { it.id == "lead_knobs" }
    private fun pct(x: Float) = (x * 100f).roundToInt()

    @Test
    fun theStepNamesTheKnobsCutoffAndResonance() {
        assertEquals("changed:synth:lead:CUTOFF+RESONANCE", step.check)
        assertTrue("the instruction says « au moins 15 »", "au moins 15" in step.text)
    }

    @Test
    fun fifteenOnTheScreenTicksTheStepEvenWhenTheRawDeltaIsJustUnderThePoint15() {
        // The project as it is by chapter 7: the lead track written (« lead_do » makes it), the step « lead_knobs » next.
        var base = target.blank()
        for (s in TutoCourse.of("techno")!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }) {
            if (s.id == "lead_knobs") break
            if (!TutoChecks.done(s, base, target)) base = TutoChecks.show(s, base, target) ?: base
        }
        val leadId = TutoChecks.trackFor(base, "lead")!!
        // A plain subtractive-synth track, so the knob the screen shows is its raw value (no engine mapping): the
        // knobs where they were when the step began show a round « 40 ».
        val started = base.track(leadId).copy(engine = 0, instrument = null, synth = SynthPatch()
            .with(SynthPatch.CUTOFF, 0.404f).with(SynthPatch.RESONANCE, 0.404f))
        val p0 = base.withTrack(started)
        val was = TutoChecks.knobValue(p0.track(leadId), SynthPatch.CUTOFF)!!
        val baseline = TutoChecks.signature("synth:lead", p0)

        // The user moves each knob to « 55 » on the screen: +15 read, but a raw delta a touch under 0.15.
        val raised = p0.track(leadId).copy(synth = p0.track(leadId).synth
            .with(SynthPatch.CUTOFF, 0.546f).with(SynthPatch.RESONANCE, 0.546f))
        val p1 = p0.withTrack(raised)
        val now = TutoChecks.knobValue(p1.track(leadId), SynthPatch.CUTOFF)!!

        // The case the seat hit: the screen says +15, the raw delta is below 0.15 (so the old strict test refused it).
        assertEquals("the screen shows +15 (was ${pct(was)}, now ${pct(now)})", 15, pct(now) - pct(was))
        assertTrue("the raw delta is just under 0.15: ${now - was}", now - was < 0.15f)

        assertTrue("+15 on the screen ticks « monte d'au moins 15 »", TutoChecks.done(step, p1, target, baseline))
    }
}
