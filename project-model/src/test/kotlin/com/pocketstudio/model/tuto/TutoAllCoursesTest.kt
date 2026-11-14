package com.pocketstudio.model.tuto

import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every course file of `tutos/` (a new style is a new file, tested here without a line of code):
 * readable, every field known ([TutoValidation]), and played from the tutorial's empty project
 * with « Montre-moi » alone, step after step, in Facile and in Pro, up to the whole song.
 */
class TutoAllCoursesTest {
    private val files = File("src/main/resources/tutos").listFiles { f -> f.extension == "json" }.orEmpty().sortedBy { it.name }

    @Test
    fun everyFileIsACourseOfTheAssistantsStyles() {
        assertTrue("no course found", files.isNotEmpty())
        val ids = files.map { it.nameWithoutExtension }
        // TutoCourse.STYLES: the files present, in the assistant's order; the others say « Bientôt ».
        assertEquals(StarterWizard.STYLES.map { it.id }.filter { it in ids }, TutoCourse.STYLES)
        assertTrue("jump_up" in TutoCourse.STYLES)
        // A style without its file has no course (every style may have one).
        StarterWizard.STYLES.map { it.id }.firstOrNull { it !in ids }?.let { assertEquals(null, TutoCourse.of(it)) }
    }

    @Test
    fun everyCourseIsValid() {
        val all = files.flatMap { f ->
            val course = runCatching { TutoCourse.parse(f.readText()) }.getOrElse { return@flatMap listOf("${f.name}: unreadable: ${it.message}") }
            TutoValidation.problems(course, f.name).map { "${f.name}: $it" }
        }
        assertTrue(all.joinToString("\n"), all.isEmpty())
    }

    @Test
    fun everyExampleCanBeMade() {
        assertTrue("no course to play", TutoCourse.STYLES.size >= MIN_COURSES)
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style)!!
            val target = TutoTarget.of(style)!!
            for (s in course.chapters.flatMap { it.steps }) if (s.listen.isNotEmpty()) {
                val ex = TutoChecks.example(s, target)
                assertNotNull("$style/${s.id}: no example for ${s.listen}", ex)
                val bricks = ex!!.after.allScenes.flatMap { TutoChecks.bricks(it) }
                assertTrue("$style/${s.id}: an example with nothing in it (${s.listen})", bricks.isNotEmpty())
            }
        }
    }

    /**
     * Review (MOYEN 6): « Copier » that follows « I V vi IV » took pop's bass from Mi1 down to Si0
     * (31 Hz, nothing on a phone). Moved to the nearest chord note, it never goes under Mi1: an octave up.
     */
    @Test
    fun copierNeverTakesTheBassUnderMi1() {
        val key = com.pocketstudio.model.Harmony(root = 4, scale = com.pocketstudio.model.Scale.Major)
        val bar = 16 * com.pocketstudio.model.NoteClip.TICKS_PER_STEP
        val phrase = listOf(com.pocketstudio.model.Note(0, bar / 2, TutoTarget.BASS_FLOOR))
        val clip = com.pocketstudio.model.NoteClip(steps = 64)
        val free = TutoTarget.fitted(clip, phrase, 16, listOf(0, 4, 5, 3), key)
        val floored = TutoTarget.fitted(clip, phrase, 16, listOf(0, 4, 5, 3), key, TutoTarget.BASS_FLOOR)
        assertTrue("the rule tested nothing: the line never went under Mi1", free.notes.any { it.pitch < TutoTarget.BASS_FLOOR })
        assertTrue(floored.notes.all { it.pitch >= TutoTarget.BASS_FLOOR })
        // The same chord notes, an octave up where they went under it.
        free.notes.zip(floored.notes).forEach { (a, b) -> assertEquals(Math.floorMod(a.pitch, 12), Math.floorMod(b.pitch, 12)) }
        // A course's own bass under Mi1 (trap's 808 on Ré♭1) sets its own floor: what it writes, « Copier » may write.
        assertEquals(25, TutoTarget.of("trap")!!.floorOf("bass"))
        assertEquals(TutoTarget.BASS_FLOOR, TutoTarget.of("pop")!!.floorOf("bass"))
        assertEquals(null, TutoTarget.of("pop")!!.floorOf("pad"))
    }

    @Test
    fun showMeMakesTheWholeSongInFacile() = showMe(TutoLevel.Facile)

    @Test
    fun showMeMakesTheWholeSongInPro() = showMe(TutoLevel.Pro)

    /**
     * Every course played with « Montre-moi » alone, each step in turn, up to the whole song; then
     * that song checked (review 2026-10-29): its parts in the course's order and lengths, the parts
     * holding what their step keeps, no note out of the key, no bass note under Mi1.
     *
     * Steps « Montre-moi » cannot do, left out here (each is played in the app by TutoJourneyTest):
     * « C'est fait » ([TutoStep.manual]: nothing in the project to check), the examples to hear
     * ([TutoStep.listening]: done once heard) and the share ("shared": the app marks it when the
     * song goes out). "changed:…" steps are done here from their baseline, as the app does.
     */
    private fun showMe(level: TutoLevel) {
        assertTrue("no course to play", TutoCourse.STYLES.size >= MIN_COURSES)
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style)!!
            val target = TutoTarget.of(style)!!
            val empty = target.blank()
            assertTrue("$style: the empty project has hits", empty.drums.lanes.all { it.hits.isEmpty() })
            val steps = course.chaptersFor(level).flatMap { it.steps }
            steps.forEach { assertFalse("$style/${it.id} is done in an empty project", TutoChecks.done(it, empty, target, TutoChecks.signature(it.check.substringAfter(':'), empty))) }
            var p = empty
            val skipped = steps.filter { it.manual || it.listening || it.check == "shared" }
            // Most of a course is made in the project: a course of « C'est fait » and examples alone checks nothing.
            assertTrue("$style $level: ${skipped.size} of ${steps.size} steps out of reach of « Montre-moi »", steps.size - skipped.size >= steps.size / 2)
            for (step in steps) {
                if (step in skipped) continue
                // A "changed:…" step: what it watches when it begins (the app keeps the same).
                val baseline = if (step.check.startsWith("changed:")) TutoChecks.signature(step.check.substringAfter(':'), p) else null
                if (TutoChecks.done(step, p, target, baseline)) continue
                // What its main button needs first (a track, a part, « Copier »…), then « Montre-moi ».
                var guard = 0
                while (!TutoChecks.done(step, p, target, baseline) && guard++ < 12) {
                    val need = TutoChecks.need(step, p, target)
                    val next = when {
                        need is TutoNeed.Bars || need is TutoNeed.Track || need is TutoNeed.Part || need is TutoNeed.Scene -> TutoChecks.fulfil(need, step, p, target)?.first
                        else -> TutoChecks.show(step, p, target) ?: need?.let { TutoChecks.fulfil(it, step, p, target)?.first }
                    } ?: error("$style/${step.id}: nothing to do (need $need)")
                    p = next.sanitized()
                }
                assertTrue("$style/${step.id} is not done after Montre-moi", TutoChecks.done(step, p, target, baseline))
            }
            songChecked(style, level, steps, p, target)
        }
    }

    /** [p], the song [steps] made: as the course lays it out, each part as its step makes it, in key, the bass where a phone plays it. */
    private fun songChecked(style: String, level: TutoLevel, steps: List<TutoStep>, p: Project, target: TutoTarget) {
        // The order: the course's parts one after the other (its « Fin » may be the Intro played again), each as long as it says.
        steps.firstOrNull { it.check == "song:order" }?.let { order ->
            val wanted = order.song.split(',').map { it.substringBeforeLast(':').trim() to it.substringAfterLast(':').trim().toInt() }
            val runs = TutoChecks.runs(p)
            val have = runs.map { p.allScenes[it.scene].label }
            assertEquals("$style $level: the song's parts", wanted.size, have.size)
            wanted.zip(have).forEach { (w, h) ->
                val same = h.equals(w.first, ignoreCase = true) || h == Scene.shownLabel(w.first) ||
                    (w.first in TutoTarget.ENDINGS && (h == "Intro" || h in TutoTarget.ENDINGS))
                assertTrue("$style $level: the song plays $have, not ${wanted.map { it.first }}", same)
            }
            // Its lengths: the course's once « Donne les durées » is done, else each part as « Ajouter » brings it (8 bars).
            val sized = steps.any { it.check == "song:phrases" }
            assertEquals("$style $level: the parts' lengths", if (sized) wanted.map { it.second } else List(wanted.size) { 8 }, runs.map { it.bars })
        }
        // Each part holds what its step keeps: the kick where it keeps it, none in a « nokick » part, the bass where it keeps it.
        val kick = TutoChecks.lanesOf("kick", target)
        val bass = TutoChecks.trackFor(p, "bass")
        for (step in steps.filter { it.check.startsWith("part:") }) {
            val label = step.check.removePrefix("part:").substringBeforeLast(':')
            val modes = step.check.substringAfterLast(':').split('+')
            val i = p.allScenes.indexOfFirst { it.label == label && !it.isEmpty }
            assertTrue("$style $level: no part $label", i >= 0)
            val q = p.switchScene(i)
            val keep = step.keep.split(',').map { it.trim() }.toSet()
            val kicks = kick.any { q.drums.lanes[it].hits.isNotEmpty() }
            if ("nokick" in modes) assertFalse("$style $level: a kick in the $label", kicks)
            if ("*" in keep || "kick" in keep) assertTrue("$style $level: no kick in the $label", kicks)
            if (bass != null && ("*" in keep || "bass" in keep) && step.transpose == 0) assertTrue("$style $level: no bass in the $label", q.track(bass).notes.notes.isNotEmpty())
        }
        // No note out of the key, in any part, on any track.
        val scale = target.harmony.scale.intervals.toSet()
        if (target.harmony.scale != com.pocketstudio.model.Scale.Off) p.allScenes.indices.forEach { i ->
            val q = p.switchScene(i)
            q.instrumentTracks.forEach { t ->
                val out = t.notes.notes.filter { Math.floorMod(it.pitch - target.harmony.root, 12) !in scale }
                assertTrue("$style $level: ${out.size} notes out of the key on ${t.name} in ${q.allScenes[i].label}", out.isEmpty())
            }
        }
        // Review (MOYEN 6): no bass note under Mi1 (41 Hz), the course's line as « Copier » moves it. An 808 written
        // lower by its course (trap's Ré♭1, drill's Mi♭1) keeps it: its dirt and its click carry it on a phone.
        bass?.takeUnless { target.bass808 }?.let { b ->
            p.allScenes.indices.forEach { i ->
                val low = p.switchScene(i).track(b).notes.notes.minOfOrNull { it.pitch }
                assertTrue("$style $level: a bass note at $low in ${p.allScenes[i].label}", low == null || low >= TutoTarget.BASS_FLOOR)
            }
        }
    }

    private companion object {
        /** The courses of `tutos/` (37 on 2026-10-29): a test looping over none passes for nothing. */
        const val MIN_COURSES = 37
    }
}
