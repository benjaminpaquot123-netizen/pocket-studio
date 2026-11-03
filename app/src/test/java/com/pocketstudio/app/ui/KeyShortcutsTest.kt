package com.pocketstudio.app.ui

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.nav.AppHarness
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot D (tablet and keyboard): a physical keyboard's shortcuts. Space = ▶/■, Ctrl+Z = Annuler,
 * Ctrl+Maj+Z and Ctrl+Y = Rétablir, through the activity's own path (the screen first, then the
 * shortcut), on the whole app; a text being typed keeps its space and its Ctrl+Z.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-hdpi")  // a tablet lying down, its keyboard plugged in
class KeyShortcutsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun key(code: Int, action: Int = KeyEvent.ACTION_DOWN, meta: Int = 0, repeat: Int = 0) =
        KeyEvent(0L, 0L, action, code, repeat, meta)

    private val ctrl = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
    private val shift = KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON

    /** A key down then up as MainActivity passes them: the window first, then the shortcut. */
    private fun press(code: Int, meta: Int = 0): Boolean {
        val window = compose.activity.window
        var taken = false
        compose.runOnUiThread {
            taken = KeyShortcuts.dispatch(key(code, meta = meta)) { window.superDispatchKeyEvent(it) }
            KeyShortcuts.dispatch(key(code, KeyEvent.ACTION_UP, meta)) { window.superDispatchKeyEvent(it) }
        }
        compose.waitForIdle()
        return taken
    }

    @Test
    fun eachKeyHasOneShortcut() {
        assertEquals(Shortcut.Play, KeyShortcuts.of(KeyEvent.KEYCODE_SPACE, ctrl = false, shift = false, alt = false))
        assertEquals(Shortcut.Undo, KeyShortcuts.of(KeyEvent.KEYCODE_Z, ctrl = true, shift = false, alt = false))
        assertEquals(Shortcut.Redo, KeyShortcuts.of(KeyEvent.KEYCODE_Z, ctrl = true, shift = true, alt = false))
        assertEquals(Shortcut.Redo, KeyShortcuts.of(KeyEvent.KEYCODE_Y, ctrl = true, shift = false, alt = false))
        // A Z alone is typed, a held space repeats nothing, Alt is the system's.
        assertNull(KeyShortcuts.of(KeyEvent.KEYCODE_Z, ctrl = false, shift = false, alt = false))
        assertNull(KeyShortcuts.of(KeyEvent.KEYCODE_SPACE, ctrl = false, shift = false, alt = false, repeat = 1))
        assertNull(KeyShortcuts.of(KeyEvent.KEYCODE_Z, ctrl = true, shift = false, alt = true))
        assertNull(KeyShortcuts.of(KeyEvent.KEYCODE_SPACE, ctrl = true, shift = false, alt = false))
        // The Meta key (a Chromebook's search key) counts as Ctrl.
        assertEquals(Shortcut.Undo, KeyShortcuts.of(key(KeyEvent.KEYCODE_Z, meta = KeyEvent.META_META_ON)))
        assertNull(KeyShortcuts.of(key(KeyEvent.KEYCODE_SPACE, KeyEvent.ACTION_UP)))
    }

    @Test
    fun spacePlaysAndStopsAndCtrlZUndoesAndRedoesInTheWholeApp() {
        h.start()
        assertTrue("AppRoot puts its shortcuts in place", KeyShortcuts.installed)
        val vm = h.vm
        assertFalse(vm.transport.value.playing)
        assertTrue(press(KeyEvent.KEYCODE_SPACE))
        assertTrue("space = ▶", vm.transport.value.playing)
        assertTrue(press(KeyEvent.KEYCODE_SPACE))
        assertFalse("space again = ■", vm.transport.value.playing)
        // A held space (its repeats) does not start and stop again and again.
        compose.runOnUiThread {
            KeyShortcuts.dispatch(key(KeyEvent.KEYCODE_SPACE)) { compose.activity.window.superDispatchKeyEvent(it) }
            repeat(5) { r -> KeyShortcuts.dispatch(key(KeyEvent.KEYCODE_SPACE, repeat = r + 1)) { compose.activity.window.superDispatchKeyEvent(it) } }
            KeyShortcuts.dispatch(key(KeyEvent.KEYCODE_SPACE, KeyEvent.ACTION_UP)) { compose.activity.window.superDispatchKeyEvent(it) }
        }
        compose.waitForIdle()
        assertTrue(vm.transport.value.playing)
        press(KeyEvent.KEYCODE_SPACE)
        assertFalse(vm.transport.value.playing)
        // An edit, undone by Ctrl+Z and back with Ctrl+Maj+Z, then again with Ctrl+Y.
        val tempo = vm.project.value.tempoBpm
        compose.runOnUiThread { vm.setTempo(tempo + 7) }
        assertEquals(tempo + 7, vm.project.value.tempoBpm, 1e-9)
        assertTrue(press(KeyEvent.KEYCODE_Z, ctrl))
        assertEquals("Ctrl+Z = Annuler", tempo, vm.project.value.tempoBpm, 1e-9)
        assertTrue(press(KeyEvent.KEYCODE_Z, ctrl or shift))
        assertEquals("Ctrl+Maj+Z = Rétablir", tempo + 7, vm.project.value.tempoBpm, 1e-9)
        press(KeyEvent.KEYCODE_Z, ctrl)
        assertTrue(press(KeyEvent.KEYCODE_Y, ctrl))
        assertEquals("Ctrl+Y = Rétablir", tempo + 7, vm.project.value.tempoBpm, 1e-9)
        // Another key goes on to the system (nothing taken).
        assertFalse(press(KeyEvent.KEYCODE_Q))
    }

    @Test
    fun aTextBeingTypedKeepsItsSpace() {
        var played = 0
        var text by mutableStateOf("")
        var typing by mutableStateOf(true)
        compose.setContent {
            KeyShortcuts.Handler(play = { played++ }, undo = {}, redo = {})
            Column { if (typing) TextField(text, { text = it }, Modifier.testTag("name")) }
        }
        compose.onNodeWithTag("name").performClick()
        compose.waitForIdle()
        assertTrue("the text field takes the space", press(KeyEvent.KEYCODE_SPACE))
        assertEquals(0, played)
        assertEquals(" ", text)
        // Typing done (the field closed, nothing focused): the space is ▶ again.
        typing = false
        compose.waitForIdle()
        assertTrue(press(KeyEvent.KEYCODE_SPACE))
        assertEquals("typed: '$text'", 1, played)
    }
}
