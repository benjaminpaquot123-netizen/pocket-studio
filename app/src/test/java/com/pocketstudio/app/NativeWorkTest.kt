package com.pocketstudio.app

import com.pocketstudio.app.audio.finishOrRelease
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections

class NativeWorkTest {
    @Test
    fun aResultNobodyCanTakeAnyMoreIsReleased() = runBlocking {
        val released = Collections.synchronizedList(mutableListOf<String>())
        val started = CompletableDeferred<Unit>()
        var reached = false
        val job = launch(Dispatchers.Default) {
            finishOrRelease(Dispatchers.IO, { s: String -> released += s }) {
                started.complete(Unit)
                Thread.sleep(200)  // a long load, the user moves on meanwhile
                "stems"
            }
            reached = true
        }
        started.await()
        job.cancel()
        job.join()
        assertEquals(listOf("stems"), released.toList())
        assertTrue(!reached)
    }

    @Test
    fun aResultTakenIsNotReleased() = runBlocking {
        val released = mutableListOf<String>()
        val result = finishOrRelease(Dispatchers.IO, { s: String -> released += s }) { "stems" }
        assertEquals("stems", result)
        assertTrue(released.isEmpty())
    }
}
