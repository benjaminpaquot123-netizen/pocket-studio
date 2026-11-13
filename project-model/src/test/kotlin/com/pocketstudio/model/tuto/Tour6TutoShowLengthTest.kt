package com.pocketstudio.model.tuto

import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.allScenes
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, lot 2, fresh review (moyen, T8): a notes step cut in two (its first half with a `length`) had « Montre-moi » lay the
 * whole line, the second half's notes too, so the next step was done before it was read. The first half's demo lays only its
 * own bars, in every course.
 */
class Tour6TutoShowLengthTest {
    @Test
    fun theFirstHalfsDemoLaysOnlyItsBars() {
        val bad = mutableListOf<String>()
        var seen = 0
        for (style in TutoCourse.STYLES) {
            val target = TutoTarget.of(style) ?: continue
            for (s in TutoCourse.of(style)!!.chapters.flatMap { it.steps }) {
                if (s.length <= 0 || !s.check.startsWith("notes:") || s.check == "notes:acid") continue
                val shown = TutoChecks.show(s, target.blank(), target) ?: continue
                seen++
                val notes = shown.allScenes.flatMap { sc -> listOfNotNull(sc.synthNotes) + sc.trackNotes.values }.flatMap { it.notes } + shown.tracks.flatMap { it.notes.notes }
                val past = notes.count { it.start >= s.length * NoteClip.TICKS_PER_STEP }
                if (past > 0) bad += "$style/${s.id}: $past notes past step ${s.length}"
            }
        }
        assertTrue("no cut step seen", seen > 0)
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }
}
