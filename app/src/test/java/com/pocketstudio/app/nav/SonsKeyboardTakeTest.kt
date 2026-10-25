package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.record.TakePhase
import com.pocketstudio.model.StarterWizard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Emulator journey (PR #48): on the assistant's jump up song, Sons › « Lead » › ● Enregistrer ma
 * mélodie, four keys of the Sons keyboard touched while the take records: the take kept nothing.
 * The same, in the whole app on Robolectric (no engine: the take's position is given).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp")
class SonsKeyboardTakeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag).performClick()
        compose.waitForIdle()
    }

    private val keyTag = SemanticsMatcher("a key") { n -> n.config.getOrNull(SemanticsProperties.TestTag)?.matches(Regex("key_\\d+")) == true }

    /** The jump up song as the assistant makes it (the kit's voices, the advised parts and mood). */
    private fun jumpUp() = StarterWizard.style("jump_up")!!.let { style ->
        val parts = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
        StarterWizard.build(style, style.lines.associate { it.role to null }, style.melody.mood, parts)
    }

    /** The jump up song open on Sons › « Lead », a take running (no engine: its position given). */
    private fun openLeadAndRecord(): com.pocketstudio.model.Project {
        val song = jumpUp()
        h.start { session.edit { song } }
        val p = h.vm.project.value
        val lead = p.tracks.first { it.name == "Lead" }
        tap("tab_Sons")
        tap("track_${lead.id}")
        assertEquals(lead.id, h.vm.selectedTrack.value)
        h.vm.playingForTest = true
        h.vm.recordPositionForTest = 2.0
        h.vm.playedClockForTest = 2.0
        tap("record_start")
        assertEquals(TakePhase.RECORDING, h.vm.noteTake.value?.phase)
        return p
    }

    /** Each key down then up, the take's position moving on (a sixteenth and a half per key). */
    private fun touch(keys: List<String>) {
        var at = 2.0
        for (key in keys) {
            compose.onNodeWithTag(key, useUnmergedTree = true).performTouchInput { down(center) }
            at += 1.5
            h.vm.recordPositionForTest = at
            h.vm.playedClockForTest = at
            compose.onNodeWithTag(key, useUnmergedTree = true).performTouchInput { up() }
            compose.waitForIdle()
            at += 1.0
            h.vm.recordPositionForTest = at
            h.vm.playedClockForTest = at
        }
    }

    private fun keyTags(): List<String> = compose.onAllNodes(keyTag, useUnmergedTree = true).fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.distinct()

    /**
     * The emulator's path: the first four keys of the Sons keyboard (C, D, E, F). The song is in F
     * phrygian with its scale locked (the assistant's mood): D and E are dark and silent, C and F
     * play and are kept. The take's count is said when it ends (■), not while it records.
     */
    @Test
    fun theFirstFourKeysKeepTheTwoInTheLockedScale() {
        val p = openLeadAndRecord()
        assertTrue("the assistant's scale is locked: ${p.harmony}", p.harmony.lock)
        val keys = keyTags().take(4)
        assertEquals(listOf("key_60", "key_62", "key_64", "key_65"), keys)
        touch(keys)
        assertEquals("counted at the end only", 0, h.vm.noteTake.value?.notes)
        tap("record_stop")
        val take = h.vm.noteTake.value
        assertEquals(TakePhase.DONE, take?.phase)
        val inScale = keys.map { it.removePrefix("key_").toInt() }.count { p.harmony.inScale(it) }
        assertEquals("the keys that sound are kept", inScale, take?.notes)
    }

    /** Keys of the scale: every one is kept, on the « Lead » track. */
    @Test
    fun keysOfTheScaleAreAllKept() {
        val p = openLeadAndRecord()
        val keys = keyTags().filter { p.harmony.inScale(it.removePrefix("key_").toInt()) }.take(4)
        touch(keys)
        tap("record_stop")
        assertEquals(4, h.vm.noteTake.value?.notes)
        compose.onNodeWithTag("take_summary").assertExists()
    }

    /** A take with nothing played: « Rien n'a été joué » under the button, no window. */
    @Test
    fun anEmptyTakeSaysNothingWasPlayed() {
        openLeadAndRecord()
        tap("record_stop")
        assertEquals(null, h.vm.noteTake.value)
        assertEquals(null, h.vm.notice.value)
        compose.onNodeWithTag("take_empty", useUnmergedTree = true).assertExists()
        // The next take takes the line away.
        tap("record_start")
        compose.onNodeWithTag("take_empty", useUnmergedTree = true).assertDoesNotExist()
    }
}
