package com.pocketstudio.app.ui

import android.view.MotionEvent
import kotlin.math.hypot

/**
 * The second touch of a double tap on a button that brings another screen (lot « tuto 5 », Lucas:
 * « Créer la piste Basse » touched twice, the Clavier opened by the first touch took the second one
 * as a note): when the screen changes right after a touch, a new touch at the same place shortly
 * after belongs to that button, not to what is under it now: it is dropped, from its press to its
 * release. Anything else (another place, later, a button that changes nothing) goes through.
 */
object DoubleTouchGuard {
    /** How long after the first touch a second one at its place is still part of it (ms). */
    const val WINDOW_MS = 1500L
    /** How long before the screen changes the touch that changed it may have started (ms). */
    const val CAUSE_MS = 1000L
    /** How near the first touch the second one falls (dp). */
    const val SLOP_DP = 24f

    private var downX = 0f
    private var downY = 0f
    private var downAt = Long.MIN_VALUE
    private var armedX = 0f
    private var armedY = 0f
    private var armedFrom = Long.MIN_VALUE
    private var armedUntil = Long.MIN_VALUE
    private var dropping = false

    /** The screen just changed ([now]: uptime ms): a touch that started it arms the guard at its place. */
    @Synchronized
    fun screenChanged(now: Long) {
        if (downAt == Long.MIN_VALUE || now < downAt || now - downAt > CAUSE_MS) return
        armedX = downX
        armedY = downY
        armedFrom = downAt
        armedUntil = downAt + WINDOW_MS
    }

    /** True: this event of the main window is the dropped second touch (or the rest of it). */
    @Synchronized
    fun drop(action: Int, x: Float, y: Float, time: Long, density: Float): Boolean {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                dropping = time in armedFrom..armedUntil && hypot(x - armedX, y - armedY) <= SLOP_DP * density
                if (dropping) {
                    // Once: a third touch there is the user's own.
                    armedUntil = Long.MIN_VALUE
                    return true
                }
                downX = x
                downY = y
                downAt = time
                return false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val was = dropping
                dropping = false
                return was
            }
            else -> return dropping
        }
    }

    /** Tests: as at the start of the app. */
    @Synchronized
    fun reset() {
        downAt = Long.MIN_VALUE
        armedFrom = Long.MIN_VALUE
        armedUntil = Long.MIN_VALUE
        dropping = false
    }
}
