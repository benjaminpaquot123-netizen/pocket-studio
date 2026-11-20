package com.pocketstudio.app.session

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
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

/**
 * Tour 9, lot L12 (F101, seat t9-53): « La liste des parties défile d'où que parte mon doigt. » In the tutorial's « Ordina le
 * tue parti » (it, phone360 at 200 %, Mode simple), swipes from the screen's middle (270,520) moved nothing. There, the list
 * shows only from y 204 to about 510: the folded tutorial panel starts under it, and those swipes started on the panel. Every
 * swipe that starts on the list itself (a row, a row's button, the words and buttons under the rows) must scroll it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour9PartsListScrollsTest : HumanTester("t9-parts-scroll") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private val metrics get() = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics

    private fun tap(tag: String) {
        val at = reachTag(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 0.5")
        }
    }

    private fun tagged(regex: Regex): List<SemanticsNode> =
        compose.onAllNodes(SemanticsMatcher("tag ~ $regex") { n ->
            n.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.matches(regex) == true
        }, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false)

    /** The column the parts scroll in: the nearest parent of the first part row that scrolls vertically. */
    private fun column(): SemanticsNode {
        val row = tagged(Regex("part_\\d+")).firstOrNull()
        assertNotNull("no part row\n" + screenLines().joinToString("\n"), row)
        return generateSequence(row!!.parent) { it.parent }.firstOrNull { it.config.contains(SemanticsProperties.VerticalScrollAxisRange) }
            ?: error("the parts are in no scrolling column")
    }

    private fun offset(): Float = column().config[SemanticsProperties.VerticalScrollAxisRange].value()
    private fun maxOffset(): Float = column().config[SemanticsProperties.VerticalScrollAxisRange].maxValue()

    /** A 300 px swipe from ([x], [y]) away from the end the column is nearer to; the column must have moved. */
    private fun swipeScrolls(what: String, x: Float, y: Float, setting: String) {
        val before = offset()
        val up = before < maxOffset() / 2
        // The finger may leave the list on its way (a swipe from its top edge still has 300 px to go): only the screen bounds it.
        val toY = if (up) (y - 300f).coerceAtLeast(10f) else (y + 300f).coerceAtMost(metrics.heightPixels - 10f)
        act("glisse ${x.toInt()} ${y.toInt()} ${x.toInt()} ${toY.toInt()}")
        act("attends 1")
        val after = offset()
        assertTrue(
            "$setting: a swipe from $what (${x.toInt()},${y.toInt()} → ${toY.toInt()}) did not scroll the parts (offset $before → $after of ${maxOffset()})\n" +
                screenLines().joinToString("\n"),
            kotlin.math.abs(after - before) >= 20f,
        )
    }

    /** Swipes from a row's middle, a row's button and, down the screen's middle, every 50 px of the parts' card in view. */
    private fun everySwipeScrolls(setting: String) {
        assertTrue("$setting: the parts do not scroll at all here (max ${maxOffset()})\n" + screenLines().joinToString("\n"), maxOffset() > 150f)
        val view = BenchWindows.drawnBounds(column(), metrics)
        val shown = { n: SemanticsNode -> BenchWindows.drawnBounds(n, metrics).let { it.top >= view.top && it.bottom <= view.bottom && it.height > 0f } }
        // A row's middle (its name and play) and a row's button (its menu), each on a row shown whole.
        tagged(Regex("part_play_\\d+")).firstOrNull(shown)?.let { n ->
            val b = BenchWindows.drawnBounds(n, metrics)
            swipeScrolls("the middle of ${n.config[SemanticsProperties.TestTag]}", b.center.x, b.center.y, setting)
        }
        tagged(Regex("part_menu_\\d+")).firstOrNull(shown)?.let { n ->
            val b = BenchWindows.drawnBounds(n, metrics)
            swipeScrolls("the button ${n.config[SemanticsProperties.TestTag]}", b.center.x, b.center.y, setting)
        }
        // Down the middle of the screen, from the first row's top to the column's bottom: wherever the finger starts.
        val x = metrics.widthPixels / 2f
        var y = view.top + 25f
        while (y < view.bottom - 25f) {
            val card = tagged(Regex("part_\\d+")).map { BenchWindows.drawnBounds(it, metrics).top }.minOrNull() ?: view.top
            if (y >= card) swipeScrolls("the screen's middle", x, y, setting)
            y += 50f
        }
    }

    private fun intoTheSong(lang: Lang, device: String, font: Int) {
        begin(SeatOptions(lang = lang, device = device, font = font, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        for (t in listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now")) {
            if (reachTag(t) != null) tap(t)
        }
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
        enoughRows()
    }

    /** Rows of parts already made (no new part: the song's parts are limited), until the list scrolls well. */
    private fun enoughRows() {
        var added = 0
        while (maxOffset() < 600f && added < 8) { tap("add_part_0"); act("attends 0.5"); added++ }
    }

    /**
     * The seat's own place: the tutorial's « Ordina le tue parti » (chapter 9), its panel folded under the parts, the
     * parts added from « Aggiungi » as the seat did.
     */
    @Test fun italianSmallPhoneAt200InTheTutorial() {
        begin(SeatOptions(lang = Lang.IT, device = "phone360", font = 200, simple = true))
        amenee("tuto:phonk:9", java.io.File(java.nio.file.Files.createTempDirectory("t9-parts").toFile(), "journal.txt"))
        act("attends 1")
        if (centerOf("tuto_kit_later") != null) tap("tuto_kit_later")
        if (centerOf("tuto_do") != null) tap("tuto_do")
        waitFor("the parts' list") { centerOf("song_add") != null || centerOf("tab_simple_Morceau") != null }
        if (centerOf("song_add") == null) tap("tab_simple_Morceau")
        for (i in listOf(1, 0, 2, 1, 0, 2)) { tap("add_part_$i"); act("attends 0.5") }
        everySwipeScrolls("it phone360 200 % tutorial")
    }

    @Test fun frenchPhoneAt100() {
        intoTheSong(Lang.FR, "phone", 100)
        everySwipeScrolls("fr phone 100 %")
    }

    @Test fun italianSmallPhoneAt200() {
        intoTheSong(Lang.IT, "phone360", 200)
        everySwipeScrolls("it phone360 200 %")
    }
}
