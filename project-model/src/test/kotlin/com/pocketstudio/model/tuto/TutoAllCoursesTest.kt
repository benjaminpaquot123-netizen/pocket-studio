package com.pocketstudio.model.tuto

import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.allScenes
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

    @Test
    fun showMeMakesTheWholeSongInFacile() = showMe(TutoLevel.Facile)

    @Test
    fun showMeMakesTheWholeSongInPro() = showMe(TutoLevel.Pro)

    private fun showMe(level: TutoLevel) {
        for (style in TutoCourse.STYLES) {
            val course = TutoCourse.of(style)!!
            val target = TutoTarget.of(style)!!
            val empty = target.blank()
            assertTrue("$style: the empty project has hits", empty.drums.lanes.all { it.hits.isEmpty() })
            val steps = course.chaptersFor(level).flatMap { it.steps }
            steps.forEach { assertFalse("$style/${it.id} is done in an empty project", TutoChecks.done(it, empty, target, TutoChecks.signature(it.check.substringAfter(':'), empty))) }
            var p = empty
            for (step in steps) {
                if (step.manual || step.listening || step.check.startsWith("changed:") || step.check == "shared") continue
                if (TutoChecks.done(step, p, target)) continue
                // What its main button needs first (a track, a part, « Copier »…), then « Montre-moi ».
                var guard = 0
                while (!TutoChecks.done(step, p, target) && guard++ < 12) {
                    val need = TutoChecks.need(step, p, target)
                    val next = when {
                        need is TutoNeed.Bars || need is TutoNeed.Track || need is TutoNeed.Part || need is TutoNeed.Scene -> TutoChecks.fulfil(need, step, p, target)?.first
                        else -> TutoChecks.show(step, p, target) ?: need?.let { TutoChecks.fulfil(it, step, p, target)?.first }
                    } ?: error("$style/${step.id}: nothing to do (need $need)")
                    p = next.sanitized()
                }
                assertTrue("$style/${step.id} is not done after Montre-moi", TutoChecks.done(step, p, target))
            }
            // The song laid out as the course's order says.
            course.chapters.flatMap { it.steps }.firstOrNull { it.check == "song:order" }?.let { order ->
                val wanted = order.song.split(',').map { it.substringBeforeLast(':').trim() }
                val have = p.song.map { p.allScenes[it.scene].label }
                assertEquals("$style: the song", wanted.size, have.size)
            }
        }
    }
}
