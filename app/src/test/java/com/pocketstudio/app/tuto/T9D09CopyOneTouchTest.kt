package com.pocketstudio.app.tuto

import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, D09 (seat t9-09 a: fr, phone 360, 100 %, Mode simple, the afrobeats course « Facile », chapter 5 « La basse »,
 * gestures 72 to 74): « Réussi » touched, then « Copier sur les mesures suivantes » touched: nothing changed; the same
 * touch again copied the bar and ended the chapter. One touch copies the bar and moves the step on.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class T9D09CopyOneTouchTest : HumanTester("t9d09") {
    private fun screen() = screenLines().joinToString("\n")

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screen())
        return act("touche ${at.first} ${at.second}")
    }

    @Test
    fun oneTouchOnCopyCopiesTheBassBarAndEndsTheStep() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone360", "font" to "100", "mode" to "simple")).first)
        tap("welcome_learn"); runCatching { tap("mode_card_simple") }; tap("tuto_style_afrobeats")
        repeat(3) { act("attends 1") }
        tap("tuto_chapters"); tap("tuto_chapter_4")
        act("attends 1")
        val v = rhythm()!!
        val target = TutoTarget.of("afrobeats")!!
        val steps = TutoCourse.of("afrobeats")!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
        // The song as the course has it when the bass chapter opens (the chapters' list jumps without making it).
        var made = v.project.value
        for (s in steps) {
            if (s.id == "bass_hear") break
            if (!TutoChecks.done(s, made, target)) made = TutoChecks.show(s, made, target) ?: made
        }
        v.session.edit { made }
        act("attends 1")
        // « Écoute la basse » passed, « Créer la piste Basse » touched: the seat's gestures 64 and 65.
        tap("tuto_next")
        act("attends 1")
        tap("tuto_need")
        act("attends 1")
        // The first bar written as the person wrote it (the five dotted notes, gestures 66 to 71).
        val doStep = steps.first { it.id == "bass_do" }
        val before = v.project.value
        val shown = TutoChecks.show(doStep, before, target) ?: error("no show for bass_do")
        v.session.edit { shown }
        act("attends 1")
        assertTrue("bass_do not done:\n" + screen(), TutoChecks.done(doStep, v.project.value, target))
        val next = listOf("tuto_next_folded", "tuto_next").firstOrNull { centerOf(it) != null } ?: error("no next on:\n" + screen())
        tap(next)
        act("attends 1")
        assertNotNull("« Copier » not offered:\n" + screen(), centerOf("tuto_need"))
        val was = v.project.value
        tap("tuto_need")
        act("attends 1")
        assertTrue("one touch on « Copier » copied nothing:\n" + screen(), v.project.value != was)
        assertTrue("one touch on « Copier » did not end the step:\n" + screen(), TutoChecks.done(steps.first { it.id == "bass_2" }, v.project.value, target))
        assertNotNull("no « Bravo, chapitre fini ! » after one touch:\n" + screen(), centerOf("tuto_next_big") ?: centerOf("tuto_chapter_end_folded"))
    }
}
