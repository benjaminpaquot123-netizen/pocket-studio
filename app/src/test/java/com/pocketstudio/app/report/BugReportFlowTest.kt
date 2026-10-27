package com.pocketstudio.app.report

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import com.pocketstudio.app.nav.AppHarness
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.zip.ZipFile

/**
 * « Signaler un bug » in the whole app: its three ways in (Plus, the Mode simple's ⋯, a long press
 * on « ? »), the screen captured behind it, « Prendre une capture », « Partager » (a zip and
 * Android's share menu) and the banner after a crash.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class BugReportFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)
    private lateinit var bug: BugReportViewModel

    @After
    fun back() {
        h.finish()
        Journal.clear()
        HealthMonitor.reset()
    }

    private fun start(simple: Boolean = false, launchChoice: Boolean = false) {
        Journal.clear()
        // Made with this test's application (the default factory would keep the first test's one).
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = BugReportViewModel(h.app) as T
        }
        bug = ViewModelProvider(compose.activity, factory)[BugReportViewModel::class.java]
        h.start(simple = simple, launchChoice = launchChoice)
    }

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun exists(tag: String) = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.TestTag, tag)).fetchSemanticsNodes().isNotEmpty()

    private fun waitFor(what: String, timeout: Long = 10_000, ok: () -> Boolean) {
        val until = System.currentTimeMillis() + timeout
        while (!ok()) {
            check(System.currentTimeMillis() < until) { "timed out waiting for $what: ${if (::bug.isInitialized) bug.state.value else null}, errors ${Journal.errors.snapshot()}" }
            Thread.sleep(20)
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
        }
    }

    private val addedThumb = SemanticsMatcher("an added picture") { n ->
        n.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.let { it.startsWith("bug_thumb_") && it != "bug_thumb_auto" } == true
    }

    private fun openFromPlus() {
        tap("tab_Plus")
        tap("plus_bug")
        waitFor("the sheet") { exists("bug_sheet") }
        waitFor("the screen behind, captured") { exists("bug_thumb_auto") }
    }

    @Test
    fun plusOpensTheSheetWithTheScreenBehindAttached() {
        start()
        openFromPlus()
        compose.onNodeWithTag("bug_attach_auto").assertIsOn()
        val auto = bug.state.value.autoShot
        assertNotNull(auto)
        assertTrue("the capture is a real picture", auto!!.file.length() > 1000)
        // Where it was opened from is in the journal.
        assertTrue(Journal.events.snapshot().any { it.text == "Signaler un bug (Plus)" })
        assertTrue(Journal.events.snapshot().any { it.kind == JournalEvent.Kind.NAV && it.text.startsWith("Plus › Outils") })
        // Its ✕ turns it off.
        tap("bug_remove_auto")
        compose.onNodeWithTag("bug_attach_auto").assertIsOff()
        assertFalse(exists("bug_thumb_auto"))
        tap("bug_close")
        assertFalse(exists("bug_sheet"))
    }

    @Test
    fun aLongPressOnTheHelpButtonOpensIt() {
        start()
        compose.onNodeWithTag("help_labels_button").performTouchInput { longClick() }
        compose.waitForIdle()
        waitFor("the sheet") { exists("bug_sheet") }
        // A touch still labels the screen.
        tap("bug_close")
        tap("help_labels_button")
        assertFalse(exists("bug_sheet"))
    }

    @Test
    fun theHelpLabelsOfferIt() {
        start()
        tap("help_labels_button")
        assertTrue(exists("help_labels"))
        tap("help_labels_bug")
        waitFor("the sheet") { exists("bug_sheet") }
        // The labels go first, so the screen captured behind is the plain one.
        assertFalse(exists("help_labels"))
        assertTrue(Journal.events.snapshot().any { it.text == "Signaler un bug (étiquettes « ? »)" })
    }

    @Test
    fun thePreviewShowsTheReportAsItWillLeave() {
        start()
        openFromPlus()
        compose.onNodeWithTag("bug_text").performTextInput("Pas de son au casque")
        assertFalse(exists("bug_preview_text"))
        tap("bug_preview_toggle")
        val shown = compose.onNodeWithTag("bug_preview_text").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
        listOf("# Rapport de bug : BenStudio", "Pas de son au casque", "- Sortie du son :", "## Journal", "screenshot-1.png").forEach {
            assertTrue("missing « $it »:\n$shown", shown.contains(it))
        }
        tap("bug_preview_toggle")
        assertFalse(exists("bug_preview_text"))
    }

    @Test
    fun theSimpleModesMenuHasIt() {
        start(simple = true)
        tap("top_more")
        tap("more_bug")
        waitFor("the sheet") { exists("bug_sheet") }
    }

    @Test
    fun takeACaptureHidesTheSheetThenBringsItBackWithThePicture() {
        start()
        openFromPlus()
        tap("bug_take")
        assertFalse(exists("bug_sheet"))
        assertTrue(exists("bug_capture_chip"))
        // Elsewhere in the app, under the floating chip.
        tap("tab_Mix")
        tap("bug_capture_now")
        waitFor("the sheet back") { exists("bug_sheet") }
        waitFor("the picture taken") { compose.onAllNodes(addedThumb).fetchSemanticsNodes().size == 1 }
        assertFalse(exists("bug_capture_chip"))
        assertEquals(BugShot.Source.TAKEN, bug.state.value.shots.single().source)
        assertEquals(listOf("screenshot-1.png", "screenshot-2.png"), bug.zipShots().map { it.second })
    }

    @Test
    fun shareWritesTheZipAndOpensAndroidsShareMenu() {
        start()
        openFromPlus()
        compose.onNodeWithTag("bug_text").performTextInput("Le son coupe au refrain")
        tap("bug_attach_project")
        tap("bug_share")
        val shadow = shadowOf(h.app)
        waitFor("the share menu") { shadow.peekNextStartedActivity() != null }
        val chooser = shadow.nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("application/zip", send.type)
        @Suppress("DEPRECATION")
        val uri = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
        assertEquals("com.pocketstudio.files", uri.authority)
        assertTrue(send.getStringExtra(Intent.EXTRA_TEXT)!!.contains("Le son coupe au refrain"))
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val zip = File(h.app.cacheDir, "exports").listFiles { f -> f.name.startsWith("rapport-pocket-studio-") }!!.single()
        ZipFile(zip).use { z ->
            val names = z.entries().toList().map { it.name }
            assertEquals(listOf("report.md", "journal.json", "screenshot-1.png", "project.json"), names)
            val md = z.getInputStream(z.getEntry("report.md")).readBytes().toString(Charsets.UTF_8)
            assertTrue(md.contains("Le son coupe au refrain"))
            assertTrue(md.contains("screenshot-1.png : capture de l'écran derrière la fiche"))
            assertTrue(md.contains("« Mon projet") || md.contains("Nom : «"))
            val project = z.getInputStream(z.getEntry("project.json")).readBytes().toString(Charsets.UTF_8)
            assertTrue(project.contains("\"tracks\""))
            org.json.JSONObject(z.getInputStream(z.getEntry("journal.json")).readBytes().toString(Charsets.UTF_8)).getJSONArray("events")
        }
        // Sent: the sheet closes and the next report starts afresh.
        waitFor("the sheet closed") { !exists("bug_sheet") }
        assertEquals("", bug.state.value.text)
    }

    @Test
    fun copyPutsTheReportInTheClipboard() {
        start()
        openFromPlus()
        compose.onNodeWithTag("bug_text").performTextInput("Ça grésille")
        tap("bug_copy")
        val clip = h.app.getSystemService(android.content.ClipboardManager::class.java).primaryClip!!
        assertTrue(clip.getItemAt(0).text.toString().contains("Ça grésille"))
        assertTrue(exists("bug_message"))
    }

    /** « Que veux-tu faire ? » after a crash: shown at once, and never again on top of the work. */
    @Test
    fun aCrashBannerHoldsNothingBackAndNothingPopsUpLater() {
        File(h.app.filesDir, "last_crash.txt").writeText("Version 0.22 · 2026-10-24 · thread main\njava.lang.IllegalStateException: boum")
        start(launchChoice = true)
        // The choice shows at once, the banner waits under the bar.
        compose.onNodeWithTag("launch").assertIsDisplayed()
        assertTrue(exists("crash_banner"))
        tap("launch_continue")
        assertFalse(exists("launch"))
        compose.onNodeWithTag("crash_banner").assertIsDisplayed()
        // « Envoyer »: the report, and never « Que veux-tu faire ? » again on top of the work.
        tap("crash_send")
        assertFalse(exists("crash_banner"))
        waitFor("the sheet") { exists("bug_sheet") }
        assertFalse(exists("launch"))
        tap("bug_close")
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        assertFalse(exists("launch"))
        assertFalse(exists("welcome"))
        compose.onNodeWithTag("timeline_screen").assertIsDisplayed()
    }

    @Test
    fun afterACrashABannerOffersTheReport() {
        File(h.app.filesDir, "last_crash.txt").writeText("Version 0.22 · 2026-10-24 · thread main\njava.lang.IllegalStateException: boum")
        start()
        assertTrue(exists("crash_banner"))
        tap("crash_send")
        waitFor("the sheet") { exists("bug_sheet") }
        assertTrue(exists("bug_crash"))
        // The crash's own capture would only show the screen after the restart: off.
        compose.onNodeWithTag("bug_attach_auto").assertIsOff()
        assertFalse(exists("crash_banner"))
        assertFalse(File(h.app.filesDir, "last_crash.txt").exists())
        assertTrue(BugReportText.markdown(bug.input()).contains("IllegalStateException: boum"))
    }
}
