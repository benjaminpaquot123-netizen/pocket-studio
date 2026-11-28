package com.pocketstudio.app.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasTestTag
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
import java.io.File

/**
 * Tour 9, L10 N1 (seat t9-27, tablet8, id): « Sur tablette, à 100, 130 et 200 %, chaque onglet se lit en entier. » The
 * Structure's two tabs (Timeline / Parts) were read « coupé » by the seat. Both views, once a part exists, each tab drawn
 * whole (at least 90 % of its width and height, the bench's own « coupé » rule).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour9TabsWholeTest : HumanTester("t9-tabs-whole") {
    @Before fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun tap(tag: String) {
        val at = reachTag(tag)
        assertNotNull("nothing to touch: $tag\n" + screenLines().joinToString("\n"), at)
        act("touche ${at!!.first} ${at.second}"); act("attends 2")
    }

    private fun tabsWhole(font: Int, view: String) {
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        for (tag in listOf("structure_timeline", "structure_parts")) {
            val n = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()
            assertNotNull("$font % $view: no $tag\n" + screenLines().joinToString("\n"), n)
            val d = BenchWindows.drawnBounds(n!!, m)
            assertTrue(
                "$font % $view: $tag drawn ${d.width} x ${d.height} of ${n.size} (${n.config[SemanticsProperties.TestTag]})\n" + screenLines().joinToString("\n"),
                d.width >= 0.9f * n.size.width && d.height >= 0.9f * n.size.height,
            )
        }
    }

    private fun tablet(font: Int) {
        val a = "tuto:gabber:7"
        begin(SeatOptions(lang = Lang.ID, device = "tablet8", font = font, simple = false, amenee = a))
        amenee(a, File.createTempFile("amenee", ".txt"))
        act("attends 2")
        tap("tab_Structure")
        tabsWhole(font, "parts, no part yet")
        val add = centerOfText("+ A", substring = true)
        assertNotNull("$font %: no « + A »\n" + screenLines().joinToString("\n"), add)
        act("touche ${add!!.first} ${add.second}"); act("attends 2")
        if (centerOf("structure_timeline") != null) tabsWhole(font, "after a part")
        tap("structure_parts")
        tabsWhole(font, "parts")
        tap("structure_timeline")
        tabsWhole(font, "timeline")
    }

    @Test fun tablet100() = tablet(100)
    @Test fun tablet130() = tablet(130)
    @Test fun tablet200() = tablet(200)
}
