package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, chantier 4 (Benjamin's answer 2B, 2026-11-09): « deux boutons ▶ ; on ne sait pas ce que chacun fait ». Both
 * stay, each named: « ▶ cette partie » at the top plays the part open, « ▶ le morceau » at the thumb plays the whole song
 * (at the top too where the thumb's bar is not). Read as a person (and TalkBack) reads them, touched as a finger does,
 * in Simple and in Pro; the words whole on a small phone at 130 % in German, on a tablet.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
open class TwoPlaysNamedTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    internal val h = AppHarness(compose)

    protected open val uiScale = 1f
    protected open val lang = Lang.FR
    /** Where the song's ▶ is: the thumb's bar, or the top bar (no thumb's bar: lying down). */
    protected open val songTag = "thumb_play"

    @After
    fun back() = h.finish()

    /** What the screen shows inside [tag] (its words) and what TalkBack says of it. */
    private fun read(tag: String): Pair<String, String> {
        val n = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull() ?: return "(nothing tagged $tag)" to ""
        val words = n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
        val talk = n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ").orEmpty()
        return words to talk
    }

    private fun theyAreNamed(simple: Boolean) {
        h.start(simple = simple, scale = uiScale, lang = lang)
        val (top, topTalk) = read("top_play")
        val (song, songTalk) = read(songTag)
        assertEquals("the top ▶'s word", tr("cette partie"), top)
        assertEquals("the top ▶ for TalkBack", tr("Jouer cette partie"), topTalk)
        assertEquals("the song's ▶'s word", tr("le morceau"), song)
        assertEquals("the song's ▶ for TalkBack", tr("Jouer le morceau"), songTalk)
    }

    @Test
    fun theTwoPlaysAreNamedInSimple() = theyAreNamed(simple = true)

    @Test
    fun theTwoPlaysAreNamedInPro() = theyAreNamed(simple = false)

    /** Never cut (« cette pa… », « der So… »): the words of both bars whole, and inside the screen. */
    @Test
    fun theWordsAreWhole() {
        h.start(simple = false, scale = uiScale, lang = lang)
        val words = setOf(tr("cette partie"), tr("le morceau"))
        val density = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        val cut = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT)).filter { it.what in words }
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val out = listOf("top_play", songTag).mapNotNull { tag ->
            val b = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()?.boundsInRoot ?: return@mapNotNull "$tag: not on the screen"
            if (b.left < screen.left - 0.5f || b.right > screen.right + 0.5f || b.width < 1f) "$tag at $b, the screen $screen" else null
        }
        val shown = listOf("top_play", songTag).map { read(it).first }
        assertTrue("$lang ui $uiScale: words ${shown}\ncut: $cut\n$out", cut.isEmpty() && out.isEmpty() && shown == listOf(tr("cette partie"), tr("le morceau")))
    }
}

/** A small phone at Taille 130 %, in German (long words, the thumb's bar back since G6). */
@Config(qualifiers = "w360dp-h780dp-xxhdpi")
class TwoPlaysNamedGermanSmallPhoneTest : TwoPlaysNamedTest() {
    override val uiScale = 1.3f
    override val lang = Lang.DE
}

/** The Tab A11 upright at 130 %: the thumb's bar has « ▶ le morceau ». */
@Config(qualifiers = "sw600dp-w600dp-h934dp-port-tvdpi")
class TwoPlaysNamedTabletTest : TwoPlaysNamedTest() {
    override val uiScale = 1.3f
}

/** A tablet lying down: no thumb's bar, both named ▶ in the top bar. */
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")
class TwoPlaysNamedTabletLyingTest : TwoPlaysNamedTest() {
    override val songTag = "top_song"
}
