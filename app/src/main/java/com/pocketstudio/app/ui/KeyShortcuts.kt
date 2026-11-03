package com.pocketstudio.app.ui

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

/** What a key of a physical keyboard (a tablet's, a Chromebook's) does in the whole app. */
enum class Shortcut { Play, Undo, Redo }

/**
 * Lot D (tablet and keyboard, USAGE-TABLETTE-SMARTPHONE §5 point 2): space = ▶/■, Ctrl+Z = Annuler,
 * Ctrl+Maj+Z (or Ctrl+Y) = Rétablir, on every screen. A key the screen wants for itself (a text
 * being typed, a focused button) is the screen's: the shortcut is only what nothing else took
 * ([dispatch], from MainActivity).
 */
object KeyShortcuts {
    /** The shortcut of a key down ([keyCode] with its modifiers), null: none. Held keys repeat nothing. */
    fun of(keyCode: Int, ctrl: Boolean, shift: Boolean, alt: Boolean, repeat: Int = 0): Shortcut? {
        if (repeat > 0 || alt) return null
        return when {
            keyCode == KeyEvent.KEYCODE_SPACE && !ctrl && !shift -> Shortcut.Play
            keyCode == KeyEvent.KEYCODE_Z && ctrl -> if (shift) Shortcut.Redo else Shortcut.Undo
            keyCode == KeyEvent.KEYCODE_Y && ctrl && !shift -> Shortcut.Redo
            else -> null
        }
    }

    fun of(event: KeyEvent): Shortcut? =
        if (event.action != KeyEvent.ACTION_DOWN) null
        else of(event.keyCode, event.isCtrlPressed || event.isMetaPressed, event.isShiftPressed, event.isAltPressed, event.repeatCount)

    /** What the app does with a shortcut (set by [Handler] while the app is shown), on the main thread. */
    @Volatile private var handler: ((Shortcut) -> Unit)? = null

    /**
     * A key from the activity: the screen first ([screen]: the window's own dispatch); what it
     * left becomes a shortcut. The key up of a shortcut is taken too (nothing half-handled).
     */
    fun dispatch(event: KeyEvent, screen: (KeyEvent) -> Boolean): Boolean {
        if (screen(event)) return true
        val h = handler ?: return false
        if (event.action == KeyEvent.ACTION_UP) {
            return of(event.keyCode, event.isCtrlPressed || event.isMetaPressed, event.isShiftPressed, event.isAltPressed) != null
        }
        val shortcut = of(event) ?: return event.repeatCount > 0 && of(event.keyCode, event.isCtrlPressed || event.isMetaPressed, event.isShiftPressed, event.isAltPressed) != null
        h(shortcut)
        return true
    }

    /** Tests: the shortcut handler in place (AppRoot shown). */
    internal val installed: Boolean get() = handler != null

    @Composable
    internal fun Handler(play: () -> Unit, undo: () -> Unit, redo: () -> Unit) {
        val actions = rememberUpdatedState(Triple(play, undo, redo))
        DisposableEffect(Unit) {
            val h: (Shortcut) -> Unit = { s ->
                val (p, u, r) = actions.value
                when (s) {
                    Shortcut.Play -> p()
                    Shortcut.Undo -> u()
                    Shortcut.Redo -> r()
                }
            }
            handler = h
            onDispose { if (handler === h) handler = null }
        }
    }
}
