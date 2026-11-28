package com.pocketstudio.app.synth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.UiScaled
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, F134: « + Piste » n'est jamais coupé. Seats (ko 200 %, hi 100 %, three tracks) saw the button cut at the
 * screen's right edge. Scrolled to the row's end, it shows whole, its words inside it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class TrackAddWholeTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun french() { I18n.lang = Lang.FR }

    private fun node(tag: String): SemanticsNode? =
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull()

    private data class Case(val lang: Lang, val scale: Float, val selected: Int, val simple: Boolean)

    @Test
    fun plusPisteShowsWholeAtTheRowsEnd() {
        val cases = listOf(Lang.KO, Lang.HI, Lang.FR, Lang.DE, Lang.AR).flatMap { l -> listOf(1f, 2f).flatMap { s -> listOf(0, 2).flatMap { sel -> listOf(true, false).map { Case(l, s, sel, it) } } } }
        var case by mutableStateOf(cases.first())
        val tracks = listOf(InstrumentTrack(0, "Mélodie"), InstrumentTrack(1, "Basse"), InstrumentTrack(2, "Nappe"))
        compose.setContent {
            key(case) {
                I18n.lang = case.lang
                UiScaled(case.scale) {
                    PocketStudioTheme {
                        // As the Sons space shows it: the editors' chips first (the drums, the 303, the voice, the remix).
                        com.pocketstudio.app.ui.SonsHeader(
                            com.pocketstudio.app.ui.AppTab.Synthe,
                            TracksUi(tracks, case.selected, changeSound = { }, effects = { }, addKind = { _, _ -> }, simple = case.simple),
                            go = { }, audioTracks = 0, simple = case.simple,
                        )
                    }
                }
            }
        }
        val faults = mutableListOf<String>()
        cases.forEach { c ->
            compose.runOnIdle { case = c }
            compose.waitForIdle()
            compose.onNodeWithTag("track_bar").performSemanticsAction(SemanticsActions.ScrollBy) { it(100_000f, 0f) }
            compose.waitForIdle()
            val root = compose.onAllNodesWithTag("track_bar", useUnmergedTree = true).fetchSemanticsNodes().first().let { it.boundsInRoot }
            val add = node("track_add")!!
            val shown = add.boundsInRoot
            val whole = add.size
            val bar = node("track_bar")!!.boundsInRoot
            if (shown.width < whole.width - 1f || shown.right > bar.right + 1f || shown.left < bar.left - 1f) {
                faults += "$c: + Piste shown ${shown.width} of ${whole.width} px ($shown in $bar, root $root)"
            }
        }
        assertTrue(faults.joinToString("\n"), faults.isEmpty())
    }
}
