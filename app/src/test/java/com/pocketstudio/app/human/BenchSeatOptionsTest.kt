package com.pocketstudio.app.human

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.AppTheme
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The bench's seat options (Audit ultra §3.1, lot P0-A): a seat in Arabic on a tablet at 130 % sees a
 * 600 dp wide screen, the font at 1.3, the interface in Arabic right to left, and its screen as text
 * (each line a role, words, a place) in Arabic.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchSeatOptionsTest : HumanTester("seat-options") {
    @Test
    fun anArabicSeatOnATabletAt130() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        val (o, warnings) = SeatOptions.parse(mapOf("lang" to "ar", "device" to "tablet8", "font" to "130", "theme" to "night"))
        assertTrue(warnings.toString(), warnings.isEmpty())
        begin(o)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val m = app.resources.displayMetrics
        assertEquals("the tablet's width in dp", 600f, m.widthPixels / m.density, 1f)
        assertEquals(1.3f, app.resources.configuration.fontScale, 0.001f)
        assertEquals("ar", app.resources.configuration.locales[0].language)
        assertEquals(Lang.AR, I18n.lang)
        assertEquals(AppTheme.Night, com.pocketstudio.app.settings.SettingsStore.state.value.theme)
        val lines = screenLines()
        assertTrue("an empty screen", lines.isNotEmpty())
        val arabic = Regex("[\\u0600-\\u06FF]")
        assertTrue("no Arabic on the screen:\n" + lines.joinToString("\n"), lines.any { arabic.containsMatchIn(it) })
        assertTrue("each line ends with its place:\n" + lines.joinToString("\n"), lines.filterNot { it.startsWith("—") || it.startsWith("  ") }.all { Regex("@ \\d+,\\d+$").containsMatchIn(it) })
        assertTrue(I18n.lang.rtl)
    }

    @Test
    fun aSeatThatDoesNotReadSeesBarsAndNoNames() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        begin(SeatOptions.parse(mapOf("lit" to "non")).first)
        val lines = screenLines()
        assertTrue(lines.joinToString("\n"), lines.any { "▭▭▭" in it })
        assertFalse("TalkBack's names are not seen:\n" + lines.joinToString("\n"), lines.any { "(nom «" in it })
    }
}

/** The options as start.txt writes them, and the thumb's reach (plain JUnit). */
class SeatOptionsParseTest {
    @Test
    fun everyOption() {
        val (o, warn) = SeatOptions.parse(
            mapOf(
                "lang" to "pt-BR", "device" to "pliable", "font" to "200", "mode" to "simple", "theme" to "daltonien",
                "gestes.max" to "60", "lit" to "non", "main" to "gauche", "amenee" to "remix", "timeout.minutes" to "5",
            ),
        )
        assertTrue(warn.toString(), warn.isEmpty())
        assertEquals(Lang.PT_BR, o.lang)
        assertEquals("foldable", o.device)
        assertEquals(2f, o.fontScale)
        assertEquals(true, o.simple)
        assertEquals(AppTheme.Colorblind, o.theme)
        assertEquals(60, o.maxGestures)
        assertFalse(o.reads)
        assertEquals(SeatOptions.Hand.LEFT, o.hand)
        assertEquals("remix", o.amenee)
        assertEquals("pt-rBR-w690dp-h829dp-port-hdpi", o.qualifiers)
    }

    @Test
    fun freshLeavesTheSettingsToThePerson() {
        val (o, warn) = SeatOptions.parse(mapOf("fresh" to "reel", "mode" to "pro", "lang" to "de"))
        assertTrue(o.fresh)
        assertEquals(null, o.simple)
        assertEquals(1, warn.size)
        assertEquals("de-w392dp-h852dp-port-hdpi", o.qualifiers)
    }

    @Test
    fun badValuesAreSaidNotFatal() {
        val (o, warn) = SeatOptions.parse(mapOf("lang" to "xx", "font" to "150", "device" to "watch", "zoom" to "2"))
        assertEquals(SeatOptions(), o)
        assertEquals(4, warn.size)
    }

    @Test
    fun theThumbReachesTheBottomNotTheFarTopCorner() {
        // The bench's phone: 588 x 1278 px at 1.5.
        val (w, h, d) = Triple(588, 1278, 1.5f)
        assertTrue(SeatOptions.reachable(SeatOptions.Hand.RIGHT, 300f, 1200f, w, h, d))
        assertTrue(SeatOptions.reachable(SeatOptions.Hand.RIGHT, 550f, 700f, w, h, d))
        assertFalse("the top left corner, right hand", SeatOptions.reachable(SeatOptions.Hand.RIGHT, 30f, 60f, w, h, d))
        assertTrue("the top left corner is far for the left hand too", !SeatOptions.reachable(SeatOptions.Hand.LEFT, 30f, 60f, w, h, d))
        assertFalse("the right edge's top, left hand", SeatOptions.reachable(SeatOptions.Hand.LEFT, 560f, 300f, w, h, d))
        assertTrue(SeatOptions.reachable(SeatOptions.Hand.BOTH, 30f, 60f, w, h, d))
        assertNotEquals(SeatOptions.reachable(SeatOptions.Hand.LEFT, 560f, 600f, w, h, d), SeatOptions.reachable(SeatOptions.Hand.RIGHT, 560f, 600f, w, h, d))
    }
}
