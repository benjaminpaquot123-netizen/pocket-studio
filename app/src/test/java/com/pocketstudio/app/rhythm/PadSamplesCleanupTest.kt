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
}
