package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Lot 2 (audit 35, amateur 4 and the tablet): Réglages › Taille de l'interface 130 % reached the
 * main window only. Each Dialog, ModalBottomSheet, menu and popup opens a window of its own,
 * which read the device's density: « Que veux-tu faire ? », the pad sheet, « Prêt à partager ? »,
 * the export… stayed at 100 %, smaller than the screen behind them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-xxhdpi")
class ScaledWindowsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    /** The height of node [tag] in the device's dp (the window's px over the device's density). */
    private fun heightDp(tag: String): Float {
        val b = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        return b.height / compose.activity.resources.displayMetrics.density
    }

    @Test
    fun thePadSheetIsDrawnAtTheInterfaceSize() {
        h.start(scale = 1.3f)
        compose.onNodeWithTag("tab_Sons").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pad_0").performTouchInput { longClick() }
        compose.waitForIdle()
        // « Tous les sons… » is at least 56 dp high: 72.8 device dp at 130 %.
        val tall = heightDp("pad_sound_more")
        assertTrue("« Tous les sons… » is $tall dp high", tall >= 56f * 1.3f - 0.5f)
    }

    @Test
    fun theLaunchChoiceIsDrawnAtTheInterfaceSize() {
        h.start(scale = 1.3f, launchChoice = true)
        h.awaitTag("launch_mode_simple")
        val tall = heightDp("launch_mode_simple")
        assertTrue("« Simple » is $tall dp high", tall >= 48f * 1.3f - 0.5f)
    }

    @Test
    fun atHundredPercentTheSheetIsAsBefore() {
        h.start(scale = 1f)
        compose.onNodeWithTag("tab_Sons").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pad_0").performTouchInput { longClick() }
        compose.waitForIdle()
        assertEquals(56f, heightDp("pad_sound_more"), 0.5f)
    }

    /**
     * Every window goes through the scaled wrappers (ui-components' Windows.kt): no Material or
     * platform window is opened straight from the app's or the components' code.
     */
    @Test
    fun noWindowIsOpenedOutsideTheScaledWrappers() {
        val raw = Regex("""(?<![A-Za-z.])(androidx\.compose\.material3\.|androidx\.compose\.ui\.window\.)?(ModalBottomSheet|AlertDialog|BasicAlertDialog|Dialog|DropdownMenu|Popup)\(""")
        // A window imported under another name (« import …ModalBottomSheet as Sheet ») is caught too.
        val aliased = Regex("""^\s*import\s+androidx\.compose\.(material3|ui\.window)\.(ModalBottomSheet|AlertDialog|BasicAlertDialog|Dialog|DropdownMenu|Popup)\s+as\s+""")
        val roots = listOf(File("src/main/java"), File("../ui-components/src/main/java"))
        roots.forEach { assertTrue("${it.absolutePath} is not a folder: the scan would see nothing", it.isDirectory) }
        // The one file allowed to open them: ui-components' own wrappers.
        val wrappers = File("../ui-components/src/main/java/com/pocketstudio/ui/Windows.kt").canonicalFile
        assertTrue(wrappers.isFile)
        val files = roots.flatMap { root -> root.walkTopDown().filter { it.extension == "kt" && it.canonicalFile != wrappers }.toList() }
        assertTrue("only ${files.size} sources scanned", files.size >= 150)
        val found = files.flatMap { f ->
            f.readLines().withIndex()
                .filter { (_, line) -> aliased.containsMatchIn(line) || (!line.trimStart().startsWith("//") && !line.trimStart().startsWith("*") && raw.containsMatchIn(line)) }
                .map { (i, line) -> "${f.name}:${i + 1}: ${line.trim().take(80)}" }
        }
        assertTrue("windows opened without the interface size:\n" + found.joinToString("\n"), found.isEmpty())
    }
}
