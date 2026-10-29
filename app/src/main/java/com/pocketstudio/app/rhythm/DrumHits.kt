package com.pocketstudio.app.rhythm

import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.Hit

/** What a drum hit needs sent beyond the hit command itself (shared by the grid and the assistant's preview). */
internal object DrumHits {
    /** The engine's ramp amount for a rising or falling roll (Engine.h kHitRampAmount: from / to half). */
    private const val RAMP_AMOUNT = 0.5f

    /**
     * A triplet ([Hit.span] > 1): its hits spread over its steps (SetDrumStepRoll, after the hit
     * command, which made it a roll in one step), with the hit's ramp. A hit in one step: nothing.
     */
    fun sendSpread(lane: Int, step: Int, hit: Hit) {
        if (hit.span <= 1) return
        val ramp = when {
            hit.ramp > 0 -> NativeEngine.ROLL_RAMP_UP
            hit.ramp < 0 -> NativeEngine.ROLL_RAMP_DOWN
            else -> NativeEngine.ROLL_RAMP_LEGACY
        }
        NativeEngine.setDrumStepRoll(lane, step, hit.repeats, hit.span, ramp, if (hit.ramp != 0) RAMP_AMOUNT else 0f)
    }
}
