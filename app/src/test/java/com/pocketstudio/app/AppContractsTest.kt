package com.pocketstudio.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Audit stage 2, point 5 (« contrats interface ↔ moteur »), read from the code:
 *  - every engine command (Engine.h CommandType) is sent by at least one host test of the engine
 *    (engine/host-tests: its effect on the sound is checked there, e.g. contract_tests.inc);
 *  - every function of NativeEngine the interface could call is called by the app (a capability
 *    with no button is either wired or listed here with its reason);
 *  - every command id NativeEngine declares is used by one of its functions (no orphan id).
 */
class AppContractsTest {
    private val engineH = File("../engine/src/main/cpp/core/Engine.h").readText()
    private val nativeEngine = File("../engine/src/main/java/com/pocketstudio/engine/NativeEngine.kt").readText()

    private fun commandTypes(): List<String> {
        val start = engineH.indexOf("enum class CommandType")
        val body = engineH.substring(engineH.indexOf('{', start) + 1, engineH.indexOf("};", start))
            .replace(Regex("//[^\n]*"), "")
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        return body.split(',').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }
    }

    @Test
    fun everyEngineCommandIsCheckedByAHostTest() {
        val tests = File("../engine/host-tests").listFiles()!!.filter { it.isFile && it.extension in setOf("cpp", "inc", "h") }
            .joinToString("\n") { it.readText() }
        val commands = commandTypes()
        assertTrue("CommandType not parsed: ${commands.size}", commands.size >= 100)
        val untested = commands.filter { !Regex("CommandType::$it\\b").containsMatchIn(tests) }
        assertTrue("Engine commands no host test sends (their effect on the sound is unchecked): $untested", untested.isEmpty())
    }

    /** NativeEngine functions the app does not call, each with its reason (a new one fails until wired or listed). */
    private val notCalledByTheApp = mapOf(
        // A drum pad's velocity layers (Sampler::layerArg): built in phase 34, no kit uses them yet. Listed for
        // Benjamin in docs/lots/AUDIT-2.md (wire a « couches de vélocité » control, or drop the engine side).
        "setPadLayer" to "engine capability without a control (AUDIT-2, item for the main agent)",
        // The mixer channel of an instrument track: the model has its own (InstrumentTrack.mixerChannel).
        "trackChannel" to "kept as the reference the model's copy names",
    )

    @Test
    fun everyEngineFunctionIsUsedByTheApp() {
        val funs = Regex("""^\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:internal |public )?(?:external )?fun\s+(\w+)\s*\(""", RegexOption.MULTILINE)
            .findAll(nativeEngine).map { it.groupValues[1] }.toSet()
        val private = Regex("""^\s*(?:@\w+\s*)*private (?:external )?fun\s+(\w+)\s*\(""", RegexOption.MULTILINE)
            .findAll(nativeEngine).map { it.groupValues[1] }.toSet()
        assertTrue("NativeEngine functions not parsed: ${funs.size}", funs.size >= 150)
        val app = (File("src/main/java").walkTopDown() + File("../engine/src/main/java").walkTopDown())
            .filter { it.extension == "kt" && it.name != "NativeEngine.kt" }.joinToString("\n") { it.readText() }
        val unused = (funs - private).filter { f ->
            !Regex("\\b$f\\b").containsMatchIn(app) && Regex("\\b$f\\b").findAll(nativeEngine).count() <= 1
        }.toSet()
        val unexplained = unused - notCalledByTheApp.keys
        assertTrue("NativeEngine functions nothing calls (wire a control, or list them with a reason): $unexplained", unexplained.isEmpty())
        val stale = notCalledByTheApp.keys - unused
        assertTrue("Listed as unused but now called (remove them from the list): $stale", stale.isEmpty())
    }

    @Test
    fun everyCommandIdIsSentByAFunction() {
        val ids = Regex("""const val (CMD_[A-Z0-9_]+) = \d+""").findAll(nativeEngine).map { it.groupValues[1] }.toList()
        assertTrue("CMD_* not parsed: ${ids.size}", ids.size >= 86)
        val orphans = ids.filter { id -> Regex("\\b$id\\b").findAll(nativeEngine).count() < 2 }
        assertTrue("Command ids no function sends: $orphans", orphans.isEmpty())
    }
}
