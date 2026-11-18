package com.pocketstudio.model.tuto

import com.pocketstudio.model.NewTracks
import com.pocketstudio.model.Project
import com.pocketstudio.model.TrackKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 7, review of the lot « fable-repar »: « Montre-moi » is offered exactly where a touch of it shows something.
 * The panel's button ([TutoPlay.canShow]), the step's main button and the show itself ([TutoPlay.showMe]) read one
 * answer: a button drawn that does nothing when touched is the fault looked for.
 */
class Tour7TutoShowOfferedShowsTest {
    private fun inReach(s: TutoStep) = !(s.manual || s.listening || s.check == "shared")

    @Test
    fun theSubStepWithNoTrackLeftOffersNoShowThatDoesNothing() {
        var seen = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            val steps = TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }
            val step = steps.firstOrNull { it.check == "track:sub" } ?: continue
            var p = target.blank()
            for (s in steps.takeWhile { it != step }.filter(::inReach)) p = TutoPlay.play(s, p, target, level).project
            // A Sub of the user's own, not written yet, and every other track taken.
            p = TutoChecks.partTrack(p, "sub", target)?.first ?: continue
            while (true) p = NewTracks.add(p, TrackKind.Melody, target.style) ?: break
            if (TutoPlay.done(step, p, target, null)) continue
            seen++
            assertEquals("$style $level ${step.id}: « Montre-moi » offered, and what a touch of it shows", TutoPlay.showMe(step, p, target, level) != null, TutoPlay.canShow(step, p, target, level))
        }
        assertTrue("no Sub step met: the test checks nothing", seen > 0)
    }

    @Test
    fun theShowIsOfferedWhereItShowsAllAlongTheCourses() {
        var seen = 0
        for (style in TutoCourse.STYLES) for (level in TutoLevel.entries) {
            val target = TutoTarget.of(style)!!
            var p: Project = target.blank()
            for (s in TutoCourse.of(style)!!.chaptersFor(level).flatMap { it.steps }.filter(::inReach)) {
                p = TutoPlay.play(s, p, target, level) { q, _ ->
                    seen++
                    assertEquals("$style $level ${s.id}", TutoPlay.showMe(s, q, target, level) != null, TutoPlay.canShow(s, q, target, level))
                }.project
            }
        }
        assertTrue("nothing played: the test checks nothing", seen > 1000)
    }
}
