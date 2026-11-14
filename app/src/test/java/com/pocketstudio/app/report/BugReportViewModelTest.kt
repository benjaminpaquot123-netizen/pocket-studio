package com.pocketstudio.app.report

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

/**
 * « Signaler un bug » without the screens: the photo picker's limit, what « Partager » leaves out
 * and cleans up, report.md in French whatever the interface, and the crash handler's chain.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BugReportViewModelTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private lateinit var vm: BugReportViewModel

    @Before
    fun fresh() {
        Journal.clear()
        HealthMonitor.reset()
        vm = BugReportViewModel(app)
        // FileProvider keeps the folders of the first application it saw (a static cache), and
        // Robolectric gives each test its own: forget them.
        androidx.core.content.FileProvider::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && Map::class.java.isAssignableFrom(it.type) }
            .forEach { f -> f.isAccessible = true; (f.get(null) as MutableMap<*, *>).clear() }
    }

    @After
    fun back() {
        SettingsStore.update(app) { it.copy(language = Lang.FR, englishNotes = false) }
        Journal.clear()
        HealthMonitor.reset()
    }

    private fun waitFor(what: String, ok: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (!ok()) {
            check(System.currentTimeMillis() < until) { "timed out waiting for $what: ${vm.state.value}, errors ${Journal.errors.snapshot()}" }
            Thread.sleep(10)
            shadowOf(Looper.getMainLooper()).idle()
        }
    }

    private fun screen(color: Int = Color.CYAN): Bitmap = Bitmap.createBitmap(120, 260, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun png(): ByteArray = ByteArrayOutputStream().also { screen(Color.MAGENTA).compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    private val summary = ProjectSummary("Nuit d'été", "acid-techno", 128.0, "Drop · mes. 14/60", false, 6, 60, 5, -1.5f, "AUTO", masterLane = false)

    private fun share(): File {
        val exports = File(app.cacheDir, "exports")
        exports.deleteRecursively()
        vm.share()
        waitFor("the zip") { exports.listFiles { f -> f.name.endsWith(".zip") }?.isNotEmpty() == true && !vm.state.value.busy }
        return exports.listFiles { f -> f.name.endsWith(".zip") }!!.single()
    }

    @Test
    fun sevenPicturesChosenKeepFiveAndSaySo() {
        val resolver = shadowOf(app.contentResolver)
        val uris = (1..7).map { Uri.parse("content://com.example.photos/picked/$it") }
        uris.forEach { resolver.registerInputStream(it, ByteArrayInputStream(png())) }
        vm.picked(uris)
        waitFor("five pictures") { vm.state.value.shots.size == 5 }
        assertEquals(BugReportViewModel.MAX_ADDED, vm.state.value.shots.size)
        assertTrue(vm.state.value.shots.all { it.source == BugShot.Source.PICKED && it.file.length() > 0 && it.thumb != null })
        assertEquals("5 images au plus : les autres ne sont pas ajoutées.", vm.state.value.message)
        // Full: one more is refused with the same word, nothing added.
        vm.dismissMessage()
        vm.picked(listOf(uris.first()))
        assertEquals(5, vm.state.value.shots.size)
        assertNotNull(vm.state.value.message)
    }

    @Test
    fun theProjectFileGoesOnlyWhenTickedAndThePicturesAreCleanedAfterSharing() {
        vm.project = { ReportProject(summary) { "{\"tracks\":[]}" } }
        vm.opened(screen())
        waitFor("the screen behind") { vm.state.value.autoShot != null }
        vm.captured(screen(Color.YELLOW))
        waitFor("the capture taken") { vm.state.value.shots.size == 1 }
        val files = listOf(vm.state.value.autoShot!!.file, vm.state.value.shots.single().file)
        assertTrue(files.all { it.isFile })
        assertFalse(vm.state.value.attachProject)

        val zip = share()
        ZipFile(zip).use { z ->
            val names = z.entries().toList().map { it.name }
            assertEquals(listOf("report.md", "journal.json", "screenshot-1.png", "screenshot-2.png"), names)
            // Unticked: no project file, but its name and settings are in the report (the sheet says so).
            val md = z.getInputStream(z.getEntry("report.md")).readBytes().toString(Charsets.UTF_8)
            assertTrue(md.contains("« Nuit d'été »"))
            assertFalse(md.contains("project.json"))
        }
        // Shared: the pictures are gone from the cache, the next report starts afresh.
        assertTrue("left in the cache: $files", files.none { it.exists() })
        assertEquals(BugReportState(), vm.state.value)
    }

    @Test
    fun theReportStaysFrenchWithTheAppInDutch() {
        SettingsStore.update(app) { it.copy(language = Lang.NL, englishNotes = true) }
        vm.setText("Geen geluid na de refrein")
        val md = vm.reportText()
        listOf("# Rapport de bug : BenStudio", "## Ce qui s'est passé", "Geen geluid na de refrein", "## Appli et téléphone", "- Langue : nl", "## Journal").forEach {
            assertTrue("missing « $it »:\n$md", md.contains(it))
        }
        // The sheet itself speaks Dutch.
        assertTrue(bugSummary(vm.state.value, "Nuit d'été").any { it.startsWith("Je tekst") })
    }

    @Test
    fun theCrashHandlerWritesTheJournalThenCallsThePreviousOne() {
        val original = Thread.getDefaultUncaughtExceptionHandler()
        var passedOn: Throwable? = null
        try {
            Thread.setDefaultUncaughtExceptionHandler { _, t -> passedOn = t }
            CrashReporter.install(app)
            Journal.action("▶ Lecture")
            val boom = IllegalStateException("boum dans /storage/emulated/0/Music/Ma chanson.wav")
            Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), boom)
            // Android's own handler (which ends the process) still runs.
            assertEquals(boom, passedOn)
            assertTrue(File(app.filesDir, "last_crash.txt").readText().contains("IllegalStateException"))
            val journal = File(app.filesDir, JournalFiles.CRASH).readText()
            assertTrue(journal.contains("▶ Lecture"))
            assertTrue(journal.contains("IllegalStateException: boum"))
            // The path (a song's name) does not leave the phone.
            assertFalse(journal.contains("Ma chanson"))
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original)
            File(app.filesDir, "last_crash.txt").delete()
            File(app.filesDir, JournalFiles.CRASH).delete()
        }
    }
}
