package com.pocketstudio.app

import com.pocketstudio.app.recette.CheckResult
import com.pocketstudio.app.recette.CheckStatus
import com.pocketstudio.app.recette.ChecklistParser
import com.pocketstudio.app.studio.noteName
import com.pocketstudio.app.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class LogicTest {
    private val checklistJson = File("src/main/assets/recette.json").readText()

    @Test
    fun shippedChecklistParses() {
        val checklist = ChecklistParser.parse(checklistJson)
        assertTrue(checklist.items.isNotEmpty())
        assertEquals(checklist.items.size, checklist.items.map { it.id }.toSet().size)
    }

    @Test
    fun reportLinesShowStatusAndComment() {
        val checklist = ChecklistParser.parse(checklistJson)
        val first = checklist.items[0]
        val second = checklist.items[1]
        val lines = ChecklistParser.reportLines(
            checklist,
            mapOf(
                first.id to CheckResult(CheckStatus.Pass),
                second.id to CheckResult(CheckStatus.Fail, "ça grésille\nun peu"),
            ),
        )
        assertEquals("- ✅ ${first.title}", lines[0])
        assertEquals("- ❌ ${second.title} : ça grésille un peu", lines[1])
        assertTrue(lines[2].startsWith("- ⬜ "))
    }

    @Test
    fun changelogHasAtMostFiveLines() {
        val lines = File("src/main/assets/changelog.txt").readLines().filter { it.isNotBlank() }
        assertTrue("changelog must be 1..5 lines", lines.size in 1..5)
    }

    @Test
    fun noteNamesAreFrench() {
        assertEquals("La 4", noteName(440f))
        assertEquals("Do 4", noteName(261.63f))
        assertEquals("Do 5", noteName(523.25f))
        assertEquals("Si 3", noteName(246.94f))
        assertEquals("La 1", noteName(55f))
    }

    @Test
    fun releaseJsonParses() {
        val json = """
            {"tag_name":"v0.1.42","body":"Notes","assets":[
              {"name":"other.zip","browser_download_url":"https://x/other.zip"},
              {"name":"pocket-studio.apk","browser_download_url":"https://x/pocket-studio.apk"}]}
        """.trimIndent()
        val info = UpdateChecker.parse(json)
        assertNotNull(info)
        assertEquals(42, info!!.versionCode)
        assertEquals("0.1.42", info.versionName)
        assertEquals("https://x/pocket-studio.apk", info.apkUrl)
        assertNull(UpdateChecker.parse("""{"tag_name":"preview","assets":[]}"""))
    }
}
