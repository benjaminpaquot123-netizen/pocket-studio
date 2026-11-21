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
import kotlin.math.hypot

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

    private fun far(a: Pair<Float, Float>, b: Pair<Float, Float>) = hypot(a.first - b.first, a.second - b.second)

    @Test
    fun theDemoOfAPartToMakePointsWhereThePartComes() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "200", "mode" to "pro")).first)
        tap("welcome_learn"); runCatching { tap("mode_card_pro") }; tap("tuto_style_afro_house")
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
        var at: Pair<Float, Float>? = null
        var seen = ""
        for (i in 0 until 8) {
            at = finger()
            if (at != null) { seen = screen(); break }
            act("attends 1")
        }
        assertNotNull("no finger during « Montre-moi »:\n" + screen(), at)
        val add = centerOf("song_add")
        assertNotNull("« Ajouter : » not on the screen during the demonstration:\n$seen", add)
        val switch = centerOf("structure_switch")
        assertTrue(
            "the finger ($at) is on the view switch ($switch), not on « Ajouter : » ($add):\n$seen",
            switch == null || far(at!!, add!!) < far(at!!, switch),
        )
        assertTrue("the part made (« Intro ») is not shown during the demonstration:\n$seen", seen.lines().any { "Intro" in it && "bouton" in it })
    }
}
