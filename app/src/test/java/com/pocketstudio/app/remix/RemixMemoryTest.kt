package com.pocketstudio.app.remix

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemixMemoryTest {
    private val gb = 1_000_000_000L

    @Test
    fun aDeviceThatCanNeverRunItIsToldBeforeTheDownload() {
        // A 3 GB phone: even all of its memory is not enough for the model.
        val message = RemixMemory.problem(totalBytes = 2_900_000_000L, availableBytes = 1 * gb)
        assertNotNull(message)
        assertTrue(message!!, message.contains("n'a pas assez de mémoire"))
    }

    @Test
    fun aFourGigabyteTabletIsToldAtOnceItCannot() {
        // A 4 GB Galaxy Tab A11 reports about 3.7 GB: above the model's 3.1 GB, but it never has that
        // much free. It is told so plainly, not asked to close apps again and again.
        val message = RemixMemory.problem(totalBytes = 3_700_000_000L, availableBytes = 1_600_000_000L)
        assertNotNull(message)
        assertTrue(message!!, message.contains("n'a pas assez de mémoire"))
        assertTrue(message, !message.contains("Vérifier à nouveau"))
        // Even with (unlikely) plenty free at that moment; and the screen offers no « check again ».
        assertNotNull(RemixMemory.problem(totalBytes = 3_700_000_000L, availableBytes = 3_400_000_000L))
        assertTrue(RemixMemory.never(3_700_000_000L))
        assertTrue(!RemixMemory.never(7_600_000_000L))
    }

    @Test
    fun aBusyBigDeviceIsAskedToCloseApps() {
        // An 8 GB phone with 1.6 GB free: closing apps can free enough.
        val message = RemixMemory.problem(totalBytes = 7_600_000_000L, availableBytes = 1_600_000_000L)
        assertNotNull(message)
        assertTrue(message!!, message.contains("Vérifier à nouveau"))
    }

    @Test
    fun enoughFreeMemoryForAShortSongSaysNothing() {
        assertNull(RemixMemory.problem(totalBytes = 8 * gb, availableBytes = RemixMemory.needed(RemixMemory.SHORTEST_SECONDS)))
        // The service's own check, once the song's length is known, asks more for a longer song.
        assertTrue(RemixMemory.needed(180.0) > RemixMemory.needed(60.0))
        assertTrue(RemixMemory.needed(0.0) == RemixMemory.MODEL_BYTES)
    }
}
