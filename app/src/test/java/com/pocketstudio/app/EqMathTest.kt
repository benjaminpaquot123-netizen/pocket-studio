package com.pocketstudio.app

import com.pocketstudio.app.mixer.EqMath
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln

class EqMathTest {
    @Test
    fun theCurveMatchesTheEngine() {
        // Same settings as the engine test: +12 dB at 1 kHz, flat at 100 Hz; the low cut drops 50 Hz.
        val f = (ln(1000.0 / 20.0) / ln(1000.0)).toFloat()
        val bell = Insert(EffectType.PARAM_EQ.ordinal, listOf(f, 0.5f + 12f / 36f, 0.4f, 0f))
        assertEquals(12.0, EqMath.responseDb(bell, 1000.0), 0.3)
        assertEquals(0.0, EqMath.responseDb(bell, 100.0), 0.5)
        assertTrue(EqMath.responseDb(bell.copy(params = bell.params.take(3) + 1f), 50.0) < -15)
        // The 3-band EQ flat at the middle.
        assertEquals(0.0, EqMath.responseDb(Insert.of(EffectType.EQ), 3000.0), 0.01)
    }
}
