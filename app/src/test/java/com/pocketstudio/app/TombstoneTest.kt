package com.pocketstudio.app

import com.pocketstudio.app.report.CrashReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TombstoneTest {
    @Test
    fun keepsTheUsefulStringsOfABinaryTombstone() {
        val parts = listOf(
            "SIGABRT", "Abort message: 'terminating due to uncaught exception'", "libonnxruntime.so",
            "_ZN11onnxruntime8concurrency10ThreadPool", "/data/app/x/lib/arm64/libpocketstudio_separation.so", "xyz", "noise",
        )
        val bytes = parts.flatMap { listOf(0x0A.toByte(), it.length.toByte()) + it.toByteArray().toList() + listOf(0x00.toByte(), 0xFF.toByte()) }
        val text = CrashReporter.tombstoneStrings((bytes + bytes).toByteArray())
        val lines = text.lines()
        assertTrue("SIGABRT" in lines)
        assertTrue(lines.any { "Abort message" in it })  // a protobuf length byte may stick in front
        assertTrue("libonnxruntime.so" in lines)
        assertTrue(lines.any { "_ZN11onnxruntime" in it })
        assertTrue(lines.none { it == "noise" || it == "xyz" })
        assertEquals(lines.size, lines.toSet().size)  // no duplicates
    }
}
