package com.pocketstudio.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every command id NativeEngine.kt sends (CMD_*) is the value of the same command in the C++
 * CommandType enum (Engine.h): both tables are kept by hand, a shift would make each command
 * do another one's job with no crash. CMD_CHANNEL_VOLUME ↔ SetChannelVolume, CMD_TRACK_808 ↔
 * SetTrack808Param (the « Set » and « Param » the Kotlin name may leave out).
 */
class CommandIdsTest {
    private fun cppCommands(): Map<String, Int> {
        val header = File("../engine/src/main/cpp/core/Engine.h").readText()
        val start = header.indexOf("enum class CommandType")
        val body = header.substring(header.indexOf('{', start) + 1, header.indexOf("};", start))
            .replace(Regex("//[^\n]*"), "")
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        val values = LinkedHashMap<String, Int>()
        var next = 0
        for (entry in body.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
            val name = entry.substringBefore('=').trim()
            if ('=' in entry) next = entry.substringAfter('=').trim().toInt()
            values[name.uppercase()] = next++
        }
        return values
    }

    private fun kotlinCommands(): Map<String, Int> {
        val source = File("../engine/src/main/java/com/pocketstudio/engine/NativeEngine.kt").readText()
        return Regex("const val CMD_([A-Z0-9_]+) = (\\d+)").findAll(source)
            .associate { it.groupValues[1] to it.groupValues[2].toInt() }
    }

    @Test
    fun everyKotlinCommandIdMatchesTheEngineEnum() {
        val cpp = cppCommands()
        val kotlin = kotlinCommands()
        assertTrue("CommandType not parsed: ${cpp.size}", cpp.size >= 90)
        assertTrue("CMD_* not parsed: ${kotlin.size}", kotlin.size >= 86)
        for ((name, id) in kotlin) {
            val flat = name.replace("_", "")
            val cppName = listOf(flat, "SET$flat", "SET${flat}PARAM").firstOrNull { it in cpp }
            assertTrue("CMD_$name has no CommandType counterpart", cppName != null)
            assertEquals("CMD_$name vs CommandType::$cppName", cpp.getValue(cppName!!), id)
        }
    }
}
