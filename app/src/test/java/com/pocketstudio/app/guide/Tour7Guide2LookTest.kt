package com.pocketstudio.app.guide

import androidx.compose.ui.test.hasTestTag
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Tour 7, lot guide2: the screens it touched, to look at (app/screenshots/tour7/guide2_*.png), on a 360 dp phone in
 * Simple and Pro, at 100 % and 130 %: the Guide's sound card (D2), Plus over the Guide's bar and Mix (D3).
 * Each screen is also checked: the Guide's open card in sight in its panel, the console in Mix (Pro), the Plus page.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour7Guide2LookTest : HumanTester("t7-guide2-look") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun has(tag: String) =
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    private fun tap(tag: String): Boolean {
        val at = centerOf(tag) ?: return false
        act("touche ${at.first} ${at.second}")
        return true
    }

    private fun waitFor(done: () -> Boolean): Boolean {
        val until = System.currentTimeMillis() + 30_000
        while (!done()) {
            if (System.currentTimeMillis() > until) return false
            act("attends 0.5")
        }
        return true
    }

    private fun song(simple: Boolean, font: Int) {
        begin(SeatOptions(lang = Lang.FR, device = "phone360", font = font, simple = simple))
        waitFor { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        tap(if (simple) "mode_card_simple" else "mode_card_pro")
        listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now").forEach { tap(it) }
        assertTrue("the Guide never came\n" + screenLines().joinToString("\n"), waitFor { centerOf("guide_bar") != null })
    }

    /** [tag] drawn with its middle on the screen (not off it, not under its bottom edge). */
    private fun inSight(tag: String) {
        val at = centerOf(tag)
        assertNotNull("no $tag\n" + screenLines().joinToString("\n"), at)
        assertTrue("$tag off the screen at $at\n" + screenLines().joinToString("\n"), at!!.first >= 0f && at.second in 0f..screenHeight.toFloat())
    }

    private fun shot(name: String) = shot(File("screenshots/tour7/guide2_$name.png"))

    private fun look(simple: Boolean, font: Int) {
        val mode = if (simple) "simple" else "pro"
        song(simple, font)
        if (!has("guide_panel")) tap("guide_bar")
        waitFor { has("guide_option_drop") || has("guide_card_start.sound") }
        if (has("guide_option_drop")) tap("guide_option_drop")
        waitFor { has("guide_card_start.sound") }
        act("attends 1")
        // The Guide's panel is open under its bar. The sound card when the Guide moved on to it; else (most runs, as
        // the captures show) its « Écouter » card still waits: whichever is there must be in sight.
        assertTrue("no Guide panel\n" + screenLines().joinToString("\n"), has("guide_panel"))
        inSight("guide_panel")
        val bar = centerOf("guide_bar")
        assertNotNull("no Guide bar", bar)
        assertTrue("the Guide's panel is not under its bar", centerOf("guide_panel")!!.second > bar!!.second)
        listOf("guide_card_start.sound", "guide_option_drop").firstOrNull { has(it) }?.let { inSight(it) }
        shot("guide_${mode}_$font")
        if (has("guide_panel")) tap("guide_bar")
        act("attends 0.5")
        // Mix is a tab of Pro only (Mode simple has no mixer of its own).
        if (simple) assertTrue("a Mix tab in Mode simple", centerOf("tab_Mix") == null) else {
            assertTrue("no Mix tab", tap("tab_Mix"))
            act("attends 1.5")
            inSight("console")
            shot("mix_${mode}_$font")
        }
        assertTrue("no way to Plus", tap("thumb_more") || tap("tab_Plus"))
        act("attends 1.5")
        assertTrue("no Plus page\n" + screenLines().joinToString("\n"), has("plus"))
        shot("plus_${mode}_$font")
    }

    @Test fun simple100() = look(true, 100)
    @Test fun simple130() = look(true, 130)
    @Test fun pro100() = look(false, 100)
    @Test fun pro130() = look(false, 130)
}
