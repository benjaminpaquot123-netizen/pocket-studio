package com.pocketstudio.app.human

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.export.MyExports
import com.pocketstudio.app.export.ShareSongProgress
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.rememberSendProject
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Duration

/**
 * Round 5 (bench lot 3): app or bench? Without the bench (no HumanTester, no BenchEngine), the app's own classes:
 * - T5-ZIP: « Mes exports » after « Envoyer le projet » (.zip);
 * - T5-ALERTE: « Le morceau a changé depuis cet export » right after an export, nothing changed;
 * - T5-8MES: « Prêt à partager ? » exports the length chosen in Mix.
 *
 * Audit « banc crédible » A.13 (G1), a rule of method: a @KnownDefect guard goes through the code the team will fix. The
 * guards of T5-ZIP and of T5-ALERTE (« Partager ») wrote the app's wiring again (a ProjectsViewModel made here, the
 * Guide's card marked by the test): a fix in AppRoot or in ShareSongProgress would have left them « still there ». They
 * now compose and call the app's own: [rememberSendProject]'s action and [ShareSongProgress]. `scripts/bench/mutation-l1.sh`
 * replays the check (the app « fixed » in its wiring: both guards must say « no longer fails »).
 */
@RunWith(RobolectricTestRunner::class)
class Tour5AppProofTest {
    /** PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). */
    @get:Rule
    val pristine = BenchPristineRule()
    /**
     * What each proof leaves, read here once its @After has run (the outermost rule), not only by the bench test that
     * comes next: after the last proof of this class comes a test of the app, which the detector does not watch. Seen in a
     * full run of the suite (2026-11-10): the proof of T5-ZIP left the ViewModel factory with its Application. The data
     * folder is this test's own (the next test has another): the project the proof saved there is not a leak.
     */
    @get:Rule(order = -2)
    val leavesNothing = TestRule { base, description ->
        object : Statement() {
            override fun evaluate() {
                base.evaluate()
                val left = BenchPristine.leaks().filterNot { it.startsWith("projects/") }
                if (left.isNotEmpty()) throw AssertionError("${description.methodName} leaves to the test after it: " + left.joinToString("; "))
            }
        }
    }
    /** See [KnownDefect]: each proof runs (it was @Ignore'd, never run: external audit), inverted while the defect is there. */
    @get:Rule
    val knownDefects: TestRule = KnownDefectRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /**
     * Android's media store as far as the app uses it (insert, write, update, query by collection and RELATIVE_PATH):
     * Robolectric has none (its resolver answers a query with nothing), so « Mes exports » would be empty whatever the app did.
     */
    class FakeMediaStore : ContentProvider() {
        class Row(val id: Long, val collection: String, val values: ContentValues, val file: File)
        private val rows = ArrayList<Row>()
        private var next = 1L

        override fun onCreate() = true
        override fun getType(uri: Uri): String? = null

        @Synchronized override fun insert(uri: Uri, values: ContentValues?): Uri {
            val id = next++
            val v = ContentValues(values ?: ContentValues())
            v.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000 + id)
            rows += Row(id, uri.path!!, v, File(context!!.cacheDir, "fake-media-$id"))
            return ContentUris.withAppendedId(uri, id)
        }

        @Synchronized override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
            val r = rowOf(uri) ?: return 0
            values?.let { r.values.putAll(it) }
            return 1
        }

        @Synchronized override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
            if (rows.removeIf { it.id == ContentUris.parseId(uri) }) 1 else 0

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
            val r = synchronized(this) { rowOf(uri) } ?: throw java.io.FileNotFoundException(uri.toString())
            return ParcelFileDescriptor.open(r.file, ParcelFileDescriptor.parseMode(mode))
        }

        @Synchronized override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
            val cols = projection ?: arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME)
            val prefix = selectionArgs?.firstOrNull()?.removeSuffix("%")
            val out = MatrixCursor(cols)
            rows.filter { it.collection == uri.path && (prefix == null || (it.values.getAsString(MediaStore.MediaColumns.RELATIVE_PATH) ?: "").startsWith(prefix)) }
                .filter { (it.values.getAsInteger(MediaStore.MediaColumns.IS_PENDING) ?: 0) == 0 }
                .sortedByDescending { it.values.getAsLong(MediaStore.MediaColumns.DATE_ADDED) }
                .forEach { r ->
                    out.addRow(cols.map { c ->
                        when (c) {
                            MediaStore.MediaColumns._ID -> r.id
                            MediaStore.MediaColumns.SIZE -> r.file.length()
                            else -> r.values.get(c)
                        }
                    }.toTypedArray())
                }
            return out
        }

        private fun rowOf(uri: Uri): Row? = runCatching { ContentUris.parseId(uri) }.getOrNull()?.let { id -> rows.firstOrNull { it.id == id } }
    }

    /** A phone's engine as far as an export reads it: each render writes a short silent WAV and keeps its length in frames. */
    private class RenderEngine : CommandSink {
        private var next = 1L
        val renders = ArrayList<Long>()
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) = Unit
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        @Synchronized override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = next++
        override fun sampleFrames(handle: Long): Long = 48_000L
        override fun sampleRelease(handle: Long) = Unit
        @Synchronized override fun exportCreate(): Long = next++
        @Synchronized override fun exportRender(
            target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
            sampleRate: Int, bits: Int,
        ): ExportResult {
            renders += frames
            silentWav(File(path), 4_800)
            return ExportResult.Ok
        }
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit

        private fun silentWav(f: File, frames: Int) {
            val data = frames * 4
            val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray()).put("fmt ".toByteArray()).putInt(16)
                .putShort(1).putShort(2).putInt(48_000).putInt(48_000 * 4).putShort(4).putShort(16).put("data".toByteArray()).putInt(data)
            RandomAccessFile(f, "rw").use { it.setLength(0); it.write(h.array()); it.write(ByteArray(data)) }
        }
    }

    @After
    fun clear() {
        screen?.pause()?.stop()?.destroy()
        screen = null
        NativeEngine.sink = null
        CurrentProject.set(app, RhythmViewModel.PROJECT_ID)
    }

    /** The screen a piece of the app's own interface is composed on (A.13), closed after the test. */
    private var screen: ActivityController<ComponentActivity>? = null

    /** [content] composed on a screen that is shown (resumed), as the app's is when its wiring runs. */
    private fun compose(content: @androidx.compose.runtime.Composable () -> Unit) {
        // FileProvider keeps the folders of the first application it saw (a static cache; Robolectric gives each test its
        // own): without this the share menu opens for the first test of the run only (as BenchShareTest, BugReportViewModelTest).
        runCatching {
            androidx.core.content.FileProvider::class.java.declaredFields
                .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && Map::class.java.isAssignableFrom(it.type) }
                .forEach { f -> f.isAccessible = true; synchronized(f.get(null)) { (f.get(null) as MutableMap<*, *>).clear() } }
        }
        val made = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        screen = made
        made.get().setContent { content() }
        idle(100)
    }

    /** Android's share menus asked for so far (what each was to send), read from what the app started. */
    private val menus = ArrayList<Intent>()

    /** The share menu the app opened for a file of [type], or null: none yet. */
    private fun shareMenu(type: String): Intent? {
        while (true) menus += shadowOf(app).nextStartedActivity ?: break
        return menus.filter { it.action == Intent.ACTION_CHOOSER }
            .mapNotNull { @Suppress("DEPRECATION") it.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) }
            .firstOrNull { it.type == type }
    }

    private fun idle(ms: Long = 20) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun until(what: String, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + 30_000
        while (!ok()) {
            check(System.currentTimeMillis() < end) { what }
            idle(); Thread.sleep(5)
        }
    }

    private fun open(name: String): RhythmViewModel {
        val id = "tour5-${System.nanoTime()}"
        ProjectStore(File(app.filesDir, "projects")).save(id, Project(name = name))
        CurrentProject.set(app, id)
        val vm = RhythmViewModel(app)
        until("the project never opened") { vm.project.value.name == name }
        // The Guide follows the project from the start on a phone (AppRoot): created before the export, as there.
        vm.guide
        idle(200)
        return vm
    }

    /** T5-ZIP (seats C c 41, C d 68): « Envoyer le projet » (.zip), then « Mes exports » lists that .zip. */
    @KnownDefect("T5-ZIP", "« Mes exports » does not list the project just sent")
    @Test
    fun myExportsListsTheProjectZipJustSent() {
        Robolectric.setupContentProvider(FakeMediaStore::class.java, MediaStore.AUTHORITY)
        val vm = open("Envoi")
        // Control: an audio export's copy (the app's own call) is listed by the fake media store.
        val wav = File(app.cacheDir, "controle.wav").apply { writeBytes(ByteArray(64)) }
        assertTrue("the fake media store takes the app's copy", WavExporter.copyToMusic(app, wav))
        assertEquals(listOf("controle.wav"), MyExports.list(app).map { it.name })
        // « Envoyer le projet » by the app's own wiring (A.13): the action AppRoot gives « Prêt à partager ? », composed as
        // there and called as its button calls it: the open project saved, its .zip made, Android's share menu opened.
        var send: (() -> Unit)? = null
        compose { send = rememberSendProject(vm) }
        send!!()
        until("Android's share menu never opened for the project's .zip") { shareMenu("application/zip") != null }
        idle(200)
        @Suppress("DEPRECATION")
        val zip = shareMenu("application/zip")!!.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertTrue("what is sent is the project's .zip: $zip", zip?.lastPathSegment?.endsWith(".pocketstudio.zip") == true)
        val listed = MyExports.list(app).map { it.name }
        assertTrue("« Mes exports » does not list the project just sent (${zip?.lastPathSegment}): $listed", listed.any { it.endsWith(".zip") })
    }

    /** T5-ALERTE (seat C c 30): « Prêt à partager ? » › Partager, the share menu opened: no « a changé » warning, nothing changed. */
    @KnownDefect("T5-ALERTE", "once the share menu opened, nothing changed in the song")
    @Test
    fun noChangedWarningRightAfterSharing() {
        NativeEngine.sink = RenderEngine()
        val vm = open("Alerte")
        vm.shareSong(ExportFormat.Wav, title = "Alerte")
        until("the share never ended") { !vm.export.value.running && vm.export.value.files.isNotEmpty() }
        assertFalse("stale right after the export", vm.export.value.staleFor(vm.project.value))
        // The app's own share window (A.13), composed as AppRoot composes it wherever « Partager » is touched: it takes the
        // song that waits (a share made while the app was not shown waits for it), opens Android's share menu, and does
        // what the app does once that menu is open.
        compose { ShareSongProgress(vm, wav = true) }
        until("Android's share menu never opened for the song") { shareMenu("audio/wav") != null }
        idle(200)
        assertFalse("« Le morceau a changé depuis cet export » once the share menu opened, nothing changed in the song " +
            "(guide share.ready = ${vm.project.value.guide.statusOf("share.ready")})", vm.export.value.staleFor(vm.project.value))
    }

    /** T5-ALERTE (seat E c 61): Mix › Exporter (WAV), then MIDI: no « a changé » warning under the file just made. */
    @KnownDefect("T5-ALERTE", "right under the MIDI file just made")
    @Test
    fun noChangedWarningRightAfterAMixExport() {
        NativeEngine.sink = RenderEngine()
        val vm = open("Alerte mix")
        vm.exportWav()
        until("the WAV never ended") { !vm.export.value.running && vm.export.value.files.isNotEmpty() }
        idle(200)
        assertFalse("« a changé » right after Mix › Exporter (WAV), nothing changed", vm.export.value.staleFor(vm.project.value))
        // The song changes (the master lowered), then its MIDI is exported: the file just made is the song as it is now.
        vm.setMasterDb(vm.project.value.mixer.masterDb - 3f)
        idle(200)
        vm.exportMidi()
        until("the MIDI never came") { vm.export.value.files.singleOrNull()?.extension == "mid" }
        idle(200)
        assertFalse("« Le morceau a changé depuis cet export » right under the MIDI file just made", vm.export.value.staleFor(vm.project.value))
    }

    /** T5-8MES (seat C c 27): 16 bars chosen in Mix, « Prêt à partager ? » › Partager renders 16 bars. */
    @KnownDefect("T5-8MES", "« Prêt à partager ? » rendered 8.0 bars, Mix chose 16")
    @Test
    fun shareFollowsTheLengthChosenInMix() {
        val engine = RenderEngine()
        NativeEngine.sink = engine
        val vm = open("Longueur")
        assertTrue("a loop, not a song", vm.project.value.song.isEmpty())
        vm.setExportBars(16)
        assertEquals("Mix says « Exporter 16 mesures »", 16, vm.export.value.bars)
        vm.shareSong(ExportFormat.Wav, title = "Longueur")
        until("the share never ended") { !vm.export.value.running && vm.export.value.files.isNotEmpty() }
        val p = vm.project.value
        val rendered = engine.renders.single()
        val bars = rendered.toDouble() / OfflineExport.framesFor(1, p.tempoBpm, p.beatsPerBar)
        assertEquals("« Prêt à partager ? » rendered ${"%.1f".format(bars)} bars, Mix chose 16",
            OfflineExport.framesFor(16, p.tempoBpm, p.beatsPerBar), rendered)
    }
}

/**
 * External audit of the bench: a proof that the app has a defect runs every time (self-test, CI), as a guard. The test
 * states the right behaviour; while the app still fails it with exactly [message] in its assertion, the guard is green
 * (« still there »); when it passes (fixed: move the verdict to « corrigé » and remove this annotation) or fails for any
 * other reason (a precondition, a crash), the guard is red.
 *
 * [id] is the « garde_id » of the line(s) of docs/audits/ultra/verdicts.tsv that cite the test (the line's item has another
 * name: T5-ZIP is carried by T5-Cd-68 and T5-Cc-41a); scripts/bench/verifie-chantier.py refuses an id no such line carries.
 * The self-test runs every class that holds one, in `human/` or not (scripts/bench/selftest-attendus.py).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class KnownDefect(val id: String, val message: String)

class KnownDefectRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        val known = description.getAnnotation(KnownDefect::class.java) ?: return base
        return object : Statement() {
            override fun evaluate() {
                val failure = runCatching { base.evaluate() }.exceptionOrNull()
                if (failure == null) throw AssertionError("${known.id} no longer fails: fixed? Set the verdicts.tsv line(s) whose garde_id is ${known.id} to « corrigé » (scripts/bench/marque-corrige.sh) and remove @KnownDefect.")
                if (failure !is AssertionError || known.message !in (failure.message ?: "")) {
                    throw AssertionError("${known.id}: failed for another reason than the known defect (« ${known.message} »)", failure)
                }
                println("${known.id}: known defect still there (${failure.message?.take(160)})")
            }
        }
    }
}
