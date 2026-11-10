package com.pocketstudio.app.nav

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.guide.GuideLevels
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Tour 5, « un mot par chose » (audit de prise en main § 5, « Volume »): « fort ou faible » is the only
 * word for a level in the Mode simple. No « dB » on the screens walked here: the first screen of the four tabs,
 * the ⋮ menu, the Mix (tip, drums open, Guide open) and the Batterie's track sheet; in texts and in what
 * TalkBack says (a fader's state, an action's name). Pro keeps its numbers. NOT walked, and still in dB
 * (docs/REVIEW.md): the effects box opened from Pad Live, the Guide's track sheet, the Guide's reference listening.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class SimpleNoDecibelTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun done() = h.finish()

    private fun idle(ms: Long = 300) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun tapIfThere(tag: String) {
        if (compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) tap(tag)
    }

    /** Every text, TalkBack name and state on the screen (all windows), as « scene: text ». */
    private fun said(scene: String): List<String> {
        val out = mutableListOf<String>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { out += "$scene: ${it.text}" }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { out += "$scene: $it" }
            n.config.getOrNull(SemanticsProperties.StateDescription)?.let { out += "$scene: $it" }
            n.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.OnClick)?.label?.let { out += "$scene: $it" }
            n.children.forEach(::walk)
        }
        compose.onAllNodes(androidx.compose.ui.test.isRoot(), useUnmergedTree = true).fetchSemanticsNodes().forEach(::walk)
        return out
    }

    private fun noDb(seen: List<String>) {
        val bad = seen.filter { it.contains("dB") }.distinct()
        assertTrue("« dB » in the Mode simple:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun noDecibelOnTheTabsAndTheMore() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        val seen = mutableListOf<String>()
        seen += said("Morceau")
        tap("tab_simple_Rythme"); seen += said("Rythme")
        tap("tab_simple_Clavier"); seen += said("Clavier")
        tap("tab_simple_Envoyer"); seen += said("Envoyer")
        tap("top_more"); seen += said("Plus (⋮)")
        noDb(seen)
    }

    /** Mix: the 303 pushed up (a tip shows), meters swinging, the drums open, the Guide open. */
    @Test
    fun noDecibelOnTheMix() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        val seen = mutableListOf<String>()
        tap("tab_simple_Mix")
        h.vm.setChannelVolume(1, GuideLevels.faderOf(GuideLevels.faderDb(h.vm.project.value.mixer.channels[1].volume) + 3f))
        val (m, l) = swingingMeters()
        h.vm.showMetersForTest(m, l)
        idle(800)
        seen += said("Mix")
        tap("mix_drums_expand"); idle(); seen += said("Mix, batterie ouverte")
        tap("guide_bar"); idle(); seen += said("Mix, Guide ouvert")
        noDb(seen)
    }

    /** A track's sheet (its volume slider, in Morceau). */
    @Test
    fun noDecibelOnATracksSheet() {
        h.start(simple = true) { session.edit { acidTechnoSong() } }
        for (key in listOf(com.pocketstudio.model.SongPart.DRUMS)) {
            tap("simple_volume_$key")
            idle()
            val seen = said("Fiche de piste")
            assertTrue("the sheet opened", compose.onAllNodes(androidx.compose.ui.test.hasTestTag("simple_track_volume"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
            noDb(seen)
        }
    }
}
