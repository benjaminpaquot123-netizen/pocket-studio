package com.pocketstudio.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.settings.KeyLayout
import com.pocketstudio.app.synth.ACCESSIBLE_TAP_MS
import com.pocketstudio.app.synth.PianoRollActions
import com.pocketstudio.app.synth.PlayKeys
import com.pocketstudio.app.synth.rollAccessibleActions
import com.pocketstudio.app.rhythm.stepState
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Hit
import com.pocketstudio.model.Note
import com.pocketstudio.ui.Help
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.ColorblindPalette
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Audit final (20, 22): what is seen, touched and said by TalkBack. */
@RunWith(RobolectricTestRunner::class)
class AccessibilityAuditTest {
    @get:Rule val compose = createComposeRule()

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun bordersReachThreeToOneOnThePanels() {
        for (p in listOf(Palettes.Studio, ColorblindPalette, Palettes.Dark)) {
            val ratio = contrast(p.outline, p.surface)
            assertTrue("outline ${p.outline} on ${p.surface}: $ratio", ratio >= 3f)
        }
    }

    @Test
    fun theSmallestTextsAreLegible() {
        var styles = emptyList<Pair<String, TextStyle>>()
        compose.setContent {
            PocketStudioTheme {
                val t = MaterialTheme.typography
                styles = listOf("bodySmall" to t.bodySmall, "labelMedium" to t.labelMedium, "labelSmall" to t.labelSmall)
            }
        }
        compose.waitForIdle()
        val sizes = styles.associate { (n, s) -> n to s.fontSize.value }
        assertTrue("$sizes", sizes.getValue("bodySmall") >= 13f && sizes.getValue("labelMedium") >= 13f && sizes.getValue("labelSmall") >= 12f)
        assertTrue("MonoStyle ${MonoStyle.fontSize}", MonoStyle.fontSize.value >= 14f)
    }

    @Test
    fun theHelpDotIsAFingersTarget() {
        compose.setContent { PocketStudioTheme { HelpDot(Help("Swing", "Le swing.")) } }
        val node = compose.onNodeWithTag("help_Swing").fetchSemanticsNode()
        val touch = node.touchBoundsInRoot
        val min = with(compose.density) { 47.5.dp.toPx() }
        assertTrue("the « ? » takes touches over ${touch.width} × ${touch.height} px", touch.width >= min && touch.height >= min)
    }

    @Test
    fun aKeyIsNamedAndPlaysFromTalkBack() {
        val heard = mutableListOf<String>()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme {
                PlayKeys(KeyLayout.TwoOctaves, 4, 2, Harmony(), false, { n, _ -> heard += "on $n" }, { n -> heard += "off $n" }, 150.dp)
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("key_60").assert(SemanticsMatcher("named Do 4") { it.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull() == "Do 4" })
        compose.onNodeWithTag("key_60").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf("on 60"), heard)
        compose.mainClock.advanceTimeBy(ACCESSIBLE_TAP_MS + 100)
        compose.waitForIdle()
        assertEquals(listOf("on 60", "off 60"), heard)
    }

    @Test
    fun aStepSaysWhatIsOnIt() {
        assertEquals("vide", stepState(null))
        assertEquals("coup, force 50 %", stepState(Hit(velocity = 0.5f)))
        assertEquals("coup, force 100 %, 3 répétitions", stepState(Hit(repeats = 3)))
    }

    @Test
    fun theNotesOfTheGridHaveTheirActions() {
        val removed = mutableListOf<Note>()
        val changed = mutableListOf<Pair<Note, Note>>()
        val note = Note(start = 48, length = 24, pitch = 60)
        val actions = rollAccessibleActions(listOf(note), false, PianoRollActions(remove = { removed += it }, change = { a, b -> changed += a to b }))
        assertEquals(3, actions.size)
        assertTrue(actions[0].label, actions[0].label.startsWith("Supprimer Do"))
        actions[0].action()
        actions[1].action()
        assertEquals(listOf(note), removed)
        assertEquals(61, changed.single().second.pitch)
    }
}
