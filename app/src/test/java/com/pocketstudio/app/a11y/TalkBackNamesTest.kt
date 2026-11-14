package com.pocketstudio.app.a11y

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.padlive.PadLiveActions
import com.pocketstudio.app.padlive.PadLiveScreen
import com.pocketstudio.app.padlive.PadLiveUi
import com.pocketstudio.app.record.NoteTakeUi
import com.pocketstudio.app.record.RecordBar
import com.pocketstudio.app.record.RecordBarActions
import com.pocketstudio.app.record.RecordBarUi
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.app.record.TakeSource
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.ScaledBottomSheet
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
 * Audit ultra round 3, n° 37 and 7 (seat B b, TalkBack; seats D a, D c, F a, F c): checked without the bench, whose
 * `lit=non` seat neither hears TalkBack's names nor reads small texts, on what the app itself gives TalkBack: every
 * control of the Mode simple's main screens has a spoken name (its words or its description; an action's label is
 * only a hint said later, never a name), the sheets' handle too, Pad Live's 16 corners « Réglages du pad », and a
 * take says what it keeps while it records. [com.pocketstudio.app.audit.ScreenScanTest] keeps the same rule on
 * every screen of the screenshot tests.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class TalkBackNamesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    /** The controls on screen that TalkBack would read as « bouton » alone: where, and what kind. */
    private fun unnamed(where: String): List<String> {
        compose.waitForIdle()
        return compose.onAllNodes(LayoutScan.CONTROL, useUnmergedTree = false).fetchSemanticsNodes(atLeastOneRootRequired = false)
            .filter { !LayoutScan.hidden(it) && LayoutScan.controlName(it).isBlank() }
            .map { "$where: ${LayoutScan.controlKind(it)} ${LayoutScan.tagOf(it)} @${it.boundsInRoot}" }
    }

    private fun open(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.waitForIdle()
    }

    @Test
    fun everyControlOfTheSimpleModesScreensHasASpokenName() {
        h.start(simple = true)
        val found = mutableListOf<String>()
        found += unnamed("Mon morceau")
        for (tab in listOf("Rythme", "Clavier", "Mix", "Envoyer", "Morceau")) {
            open("tab_simple_$tab")
            found += unnamed(tab)
        }
        assertTrue("Controls TalkBack reads as « bouton » alone:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Test
    fun aSheetsHandleHasAName() {
        compose.setContent { PocketStudioTheme { ScaledBottomSheet(onDismissRequest = {}) { Text("Son de : Mélodie") } } }
        val found = unnamed("sheet")
        assertTrue("A sheet's controls without a name (Material's handle in its tooltip, seats B b, E a, C a, E d, F d):\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun padLivesCornersSayWhatTheyOpen() {
        val pads = PadLive.placed(
            LivePads(), 0,
            LiveStarter.PADS.mapIndexed { i, p -> LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain) },
        )
        val ui = PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4)
        compose.setContent { PocketStudioTheme { PadLiveScreen(ui, PadLiveActions(), clockOf = { 0 }, still = true) } }
        val found = unnamed("Pad Live")
        assertTrue("Pad Live's controls without a name (the 16 corners, seats D a, D c, F a, F c):\n" + found.joinToString("\n"), found.isEmpty())
        val corner = compose.onNodeWithTag("padlive_mode_0").fetchSemanticsNode()
        assertEquals("Réglages du pad, pad A1", corner.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString())
    }

    @Test
    fun aTakeSaysWhatItKeepsWhileItRecords() {
        val take = NoteTakeUi(TakeSource.KEYS, TakePhase.RECORDING, notes = 3)
        compose.setContent { PocketStudioTheme { RecordBar(RecordBarUi(take, "Enregistrer ma mélodie"), RecordBarActions()) } }
        val kept = compose.onNodeWithTag("record_kept").fetchSemanticsNode()
        assertEquals("Ta prise : 3 notes", kept.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text })
        // Said by TalkBack as it changes, with no touch (seat B b: « rien ne me confirme qu'elles sont enregistrées »).
        assertEquals(androidx.compose.ui.semantics.LiveRegionMode.Polite, kept.config.getOrNull(SemanticsProperties.LiveRegion))
    }
}
