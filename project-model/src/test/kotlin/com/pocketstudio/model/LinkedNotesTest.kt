package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Audit final (lot notes): the assistant's Sub and Basse 2 follow the bass's notes when they are edited. */
class LinkedNotesTest {
    private val style = StarterWizard.style("jump_up")!!

    private fun build(): Project =
        StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() })

    private fun Project.notes(scene: Int, track: Int) = allScenes[scene].let { if (track == 0) it.synthNotes else it.trackNotes[track] ?: NoteClip() }.notes

    @Test
    fun theSubAndTheSecondBassFollowAnEditOfTheBassInEveryScene() {
        val p0 = build()
        val bass = p0.instrumentTracks.first { it.name == "Basse" }.id
        val sub = p0.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }.id
        val second = p0.instrumentTracks.first { it.name == StarterWizard.BASS_SWITCH_TRACK }.id
        assertEquals(setOf(LinkedNotes.Follower(sub, true), LinkedNotes.Follower(second, false)), LinkedNotes.followers(p0, bass).toSet())
        // The Drop shown, its bass edited as the piano roll does (a note added, the first one gone).
        val before = p0.switchScene(2).sanitized()
        val clip = before.track(bass).notes
        val edited = clip.copy(notes = clip.notes.drop(1) + Note(15 * NoteClip.TICKS_PER_STEP, 12, 43)).sanitized()
        val after = LinkedNotes.sync(before, before.withTrack(before.track(bass).copy(notes = edited)), bass).sanitized()
        for (scene in after.allScenes.indices) {
            val b = after.notes(scene, bass)
            assertEquals("scene $scene", StarterWizard.subNotes(b), after.notes(scene, sub))
            assertEquals("scene $scene", b, after.notes(scene, second))
        }
        assertNotEquals(p0.notes(2, sub), after.notes(2, sub))
        // Still linked: the next edit follows too.
        assertEquals(2, LinkedNotes.followers(after, bass).size)
    }

    @Test
    fun aSubChangedByHandKeepsItsOwnNotes() {
        val p0 = build()
        val bass = p0.instrumentTracks.first { it.name == "Basse" }.id
        val sub = p0.instrumentTracks.first { it.name == StarterWizard.SUB_TRACK }.id
        val p = p0.switchScene(2).sanitized()
        val own = p.withTrack(p.track(sub).copy(notes = p.track(sub).notes.copy(notes = listOf(Note(0, 24, 30))).sanitized())).sanitized()
        assertTrue(LinkedNotes.followers(own, bass).none { it.id == sub })
        val clip = own.track(bass).notes
        val after = LinkedNotes.sync(own, own.withTrack(own.track(bass).copy(notes = clip.copy(notes = clip.notes.drop(1)))), bass)
        assertEquals(listOf(Note(0, 24, 30)), after.track(sub).notes.notes)
        // Only the bass leads: an edit of the sub or of another track moves nothing else.
        for (t in p0.instrumentTracks.filter { it.id != bass }) assertTrue(t.name, LinkedNotes.followers(p0, t.id).isEmpty())
    }
}
