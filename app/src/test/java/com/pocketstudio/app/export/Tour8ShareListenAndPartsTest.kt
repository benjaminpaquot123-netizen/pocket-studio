package com.pocketstudio.app.export

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.human.BenchWindows
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
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
 * Tour 8, lot R (seat 3a, tour 7, hi, phone360 à 130 %, Simple):
 * - #64: « en hi, phone360 à 130 %, Mode Simple : sur Partager, le bouton « ▶ सुनो » montre son mot en entier. » (an
 *   empty outline was seen beside the clock);
 * - #65: « en hi, phone360 à 130 %, Mode Simple : sur Partager, le nombre de parties dit est celui des parties du
 *   morceau (A à T = 20), pas le nombre de cases de la liste. » (« 27 पार्ट » for 20 parts: a part played twice counted twice).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour8ShareListenAndPartsTest : HumanTester("t8-share") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

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

    /** A style song, its first part played once more at the end (a part twice in the list), then « Partager ». */
    private fun share(lang: Lang, device: String, font: Int) {
        begin(SeatOptions(lang = lang, device = device, font = font, simple = true))
        waitFor("the home's choices") { centerOf("welcome_guided") != null || centerOf("launch_new") != null }
        if (centerOf("welcome_guided") != null) tap("welcome_guided") else { tap("launch_new"); tap("starter_wizard") }
        if (centerOf("mode_card_simple") != null) tap("mode_card_simple")
        for (t in listOf("wizard_family_Techno", "wizard_style_techno", "wizard_style_choose_techno", "wizard_create_now")) {
            if (centerOf("tab_simple_Morceau") != null) break
            if (centerOf(t) != null) tap(t)
        }
        waitFor("the song's tabs") { centerOf("tab_simple_Morceau") != null }
        tap("tab_simple_Morceau")
        centerOf("guide_later")?.let { act("touche ${it.first} ${it.second}") }
        tap("add_part_0")
        act("attends 1")
        tap("thumb_share")
        waitFor("the share sheet") { centerOf("share_listen") != null }
        act("attends 1")
    }

    private fun listenShowsItsWord(lang: Lang, device: String, font: Int) {
        share(lang, device, font)
        val m = ApplicationProvider.getApplicationContext<android.app.Application>().resources.displayMetrics
        val word = tr("Écouter")
        val label = compose.onAllNodes(androidx.compose.ui.test.hasText(word, substring = true).and(androidx.compose.ui.test.hasAnyAncestor(hasTestTag("share_listen"))), useUnmergedTree = true)
            .fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()
        assertNotNull("$lang $device $font %: no « $word » in « ▶ »\n" + screenLines().joinToString("\n"), label)
        val b = BenchWindows.drawnBounds(label!!, m)
        val layout = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        label.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult)?.action?.invoke(layout)
        val cut = layout.firstOrNull()?.let { it.hasVisualOverflow || it.isLineEllipsized(it.lineCount - 1) } ?: false
        assertTrue(
            "$lang $device $font %: « ▶ $word » not shown whole (drawn ${b.width}×${b.height} of ${label.size.width}×${label.size.height}, cut $cut)\n" + screenLines().joinToString("\n"),
            shownWhole(label) && b.width >= label.size.width - 1f && b.height >= label.size.height - 1f && !cut,
        )
    }

    private fun partsSaidAreTheSongs(lang: Lang, device: String, font: Int) {
        share(lang, device, font)
        val song = project()!!.song
        val distinct = song.map { it.scene }.distinct().size
        assertTrue("the song plays a part twice: ${song.map { it.scene }}", distinct < song.size)
        val said = trCount(distinct, "1 partie", "%d parties")
        val wrong = trCount(song.size, "1 partie", "%d parties")
        val lines = screenLines()
        assertTrue("$lang $device $font %: « $said » not said (${song.size} rows, $distinct parts)\n" + lines.joinToString("\n"), lines.any { said in it })
        assertTrue("$lang $device $font %: « $wrong » said, a part counted twice\n" + lines.joinToString("\n"), lines.none { wrong in it })
    }

    @Test fun listenHindiSmallPhoneAt130() = listenShowsItsWord(Lang.HI, "phone360", 130)
    @Test fun listenFrenchSmallPhoneAt200() = listenShowsItsWord(Lang.FR, "phone360", 200)
    @Test fun listenGermanSmallPhoneAt130() = listenShowsItsWord(Lang.DE, "phone360", 130)
    @Test fun partsHindiSmallPhoneAt130() = partsSaidAreTheSongs(Lang.HI, "phone360", 130)
    @Test fun partsFrenchPhoneAt100() = partsSaidAreTheSongs(Lang.FR, "phone", 100)
}
