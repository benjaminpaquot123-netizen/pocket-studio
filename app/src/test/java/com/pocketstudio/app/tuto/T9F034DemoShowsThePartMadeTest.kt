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
 * Tour 9, F034 (seat t9-08 a: zh-CN, phone, 200 %, Mode pro, the afro house course « Pro », chapter 8 « Construis tes
 * parties », gesture 24): « Montre-moi » on « crée l'Intro » put its finger on the « Parties » view switch, then « à toi » ;
 * the part it made, a button of « Ajouter : », stayed under the panel. The finger goes where the part made comes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w392dp-h852dp-port-xhdpi")
class T9F034DemoShowsThePartMadeTest : HumanTester("t9f034") {
    private fun screen() = screenLines().joinToString("\n")

    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screen())
        return act("touche ${at.first} ${at.second}")
    }

    private fun finger(): Pair<Float, Float>? = screenLines().firstOrNull { "doigt qui touche" in it }
        ?.substringAfterLast('@')?.trim()?.split(',')?.let { it[0].trim().toFloat() to it[1].trim().toFloat() }

    @Test
    fun theDemoOfAPartToMakePointsWhereThePartComes() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "200", "mode" to "pro")).first)
        tap(listOf("welcome_learn", "launch_learn").firstOrNull { reachTag(it) != null } ?: error("no way to the courses on:\n" + screen())); runCatching { tap("mode_card_pro") }; tap("tuto_style_afro_house")
        repeat(3) { act("attends 1") }
        tap("tuto_chapters"); tap("tuto_chapter_7")
        act("attends 1")
        val v = rhythm()!!
        val target = TutoTarget.of("afro_house")!!
        val steps = TutoCourse.of("afro_house")!!.chaptersFor(TutoLevel.Pro).flatMap { it.steps }
        // The song as the course has it when the chapter opens (the chapters' list jumps without making it).
        var made = v.project.value
        for (s in steps) {
            if (s.id == "make_intro") break
            if (!TutoChecks.done(s, made, target)) made = TutoChecks.show(s, made, target) ?: made
        }
        v.session.edit { made }
        act("attends 1")
        tap("tuto_show")
        // The demonstration watched to its end: where the finger is, and whether the part it makes is seen meanwhile.
        var at: Pair<Float, Float>? = null
        var seen = ""
        var shown = false
        for (i in 0 until 12) {
            val now = finger()
            if (now != null) {
                at = now; seen = screen()
                if (seen.lines().any { "Intro" in it && "bouton" in it }) { shown = true; break }
            } else if (at != null) break
            act("attends 1")
        }
        assertNotNull("no finger during « Montre-moi »:\n" + screen(), at)
        // « Ajouter : » is the title right above the buttons: the finger is under it, on them, not up on the view switch.
        val title = seen.lines().firstOrNull { "« Ajouter : »" in it }?.substringAfterLast(',')?.trim()?.toFloat()
        assertNotNull("« Ajouter : » not on the screen during the demonstration:\n$seen", title)
        assertTrue("the finger ($at) is above « Ajouter : » (y = $title), not on its buttons:\n$seen", at!!.second > title!!)
        assertTrue("the part made (« Intro ») is not shown during the demonstration:\n$seen", shown)
    }
}
