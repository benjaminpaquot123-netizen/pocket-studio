package com.pocketstudio.bentest

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.NativeEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy

/** The phone's own speaker for « Écoute au micro »: where the song is, read from the engine (the Linux bench starts its render there). */
@RunWith(RobolectricTestRunner::class)
class HautParleurReelTest {
    @After
    fun noEngine() {
        NativeEngine.sink = null
    }

    /** An engine that has played [steps] sixteenths since ▶ (every other call answers its type's nothing). */
    private fun engine(steps: Int): CommandSink = Proxy.newProxyInstance(CommandSink::class.java.classLoader, arrayOf(CommandSink::class.java)) { _, m, _ ->
        when {
            m.name == "absoluteStep" -> steps
            m.returnType == Int::class.javaPrimitiveType -> 0
            m.returnType == Long::class.javaPrimitiveType -> 0L
            m.returnType == Double::class.javaPrimitiveType -> 0.0
            m.returnType == Float::class.javaPrimitiveType -> 0f
            m.returnType == Boolean::class.javaPrimitiveType -> false
            else -> null
        }
    } as CommandSink

    @Test
    fun theStepsPlayedAreTheEngines() {
        NativeEngine.sink = engine(32)
        assertEquals(32, HautParleurReel(ApplicationProvider.getApplicationContext()).pasJoues())
        NativeEngine.sink = engine(-1)
        assertEquals("stopped: unknown", -1, HautParleurReel(ApplicationProvider.getApplicationContext()).pasJoues())
    }
}
