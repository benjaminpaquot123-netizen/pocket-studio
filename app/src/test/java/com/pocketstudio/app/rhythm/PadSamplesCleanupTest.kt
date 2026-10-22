package com.pocketstudio.app.rhythm

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PadSamplesCleanupTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun theCleanupSparesWaitingTakesAndFilesBeingWritten() {
        val dir = tmp.newFolder("samples")
        listOf("old.wav", "take.wav", "next.wav.tmp").forEach { File(dir, it).writeBytes(ByteArray(8)) }
        // Nothing the project uses yet; "take.wav" is named by a waiting take, "next.wav.tmp" is being written.
        PadSamples(dir).loadAll(emptySet(), cleanUp = true, keep = setOf("take.wav"))
        assertEquals(setOf("take.wav", "next.wav.tmp"), dir.list()!!.toSet())
    }

    @Test
    fun aWriteTheAppNeverFinishedIsCleanedAnHourLater() {
        val dir = tmp.newFolder("samples")
        val now = System.currentTimeMillis()
        File(dir, "fresh.wav.tmp").apply { writeBytes(ByteArray(8)); setLastModified(now - 60_000) }
        File(dir, "stale.wav.tmp").apply { writeBytes(ByteArray(8)); setLastModified(now - 2 * PadSamples.STALE_TMP_MS) }
        PadSamples(dir).loadAll(emptySet(), cleanUp = true, now = now)
        assertEquals(setOf("fresh.wav.tmp"), dir.list()!!.toSet())
    }

    @Test
    fun aFailedWriteLeavesNoHalfFile() {
        val dir = tmp.newFolder("takes")
        // Fewer samples than asked for: the write fails halfway, as on a full phone.
        val failed = runCatching { com.pocketstudio.app.audio.FloatWav.write(File(dir, "take.wav"), FloatArray(10), 0, 100_000, 1, 48_000) }
        org.junit.Assert.assertTrue(failed.isFailure)
        assertEquals(emptySet<String>(), dir.list()!!.toSet())
    }
}
