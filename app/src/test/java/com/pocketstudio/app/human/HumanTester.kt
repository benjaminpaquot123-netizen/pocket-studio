package com.pocketstudio.app.human

import android.app.Application
import android.content.Intent
import android.media.MediaFormat
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowMediaCodec
import java.io.File
import java.time.Duration

/**
 * The « testeur humain » bench (owner, 2026-10-28: « un audit qui ressemble le plus possible à un
 * humain »): the real app (MainActivity, rebuilt when a project opens, as on a phone), freshly
 * installed in French, driven from outside by someone who only sees the screen. Each turn the
 * screen is saved as an image (screen-N.png) with what would be heard (sound-N.txt); the gesture
 * is read from cmd-N.txt in the screen's pixels and played as real touches, then the next screen.
 * A journal of what happened inside (journal.txt: the tutorial's step, its ticks) is for the
 * report only. Runs only with -Phuman.dir=<folder> (scripts/human-tester.sh), one seat a class.
 */
abstract class HumanTester(private val seat: String) {
    @get:Rule
    val compose = createEmptyComposeRule()
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    /** The examples as a person hears them: they last their length, then end. */
    private class Ears : GuideListenPlayer {
        var startedAt = -1L
        var frames = 0L
        var ended: (() -> Unit)? = null
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean {
            startedAt = SystemClock.uptimeMillis()
            frames = pcm.frames.toLong()
            this.ended = ended
            return true
        }
        override fun stop() { startedAt = -1L; ended = null }
        override fun heard(): Long = if (startedAt < 0) 0L else minOf(frames, (SystemClock.uptimeMillis() - startedAt) * OfflineExport.SAMPLE_RATE / 1000)
        val playing: Boolean get() = startedAt >= 0
        fun tick() {
            if (startedAt >= 0 && heard() >= frames) { val e = ended; stop(); e?.invoke() }
        }
    }

    private val ears = Ears()

    /** Copies what fits (as VideoExportTest): the shadow codec stands in for the phone's AAC encoder. */
    private val passThrough = ShadowMediaCodec.CodecConfig.Codec { input, output ->
        val n = minOf(input.remaining(), output.remaining())
        output.put(input.duplicate().also { it.limit(it.position() + n) })
        input.position(input.position() + n)
    }
    // Lot « tuto 4 » (É0): the play head moves as on a phone, and « Partager » exports.
    private val playhead = BenchPlayhead()
    private var lastVm: RhythmViewModel? = null
    /** Said once in the next sound-N.txt (what Android's share menu did). */
    private var note: String? = null

    @After
    fun end() {
        runCatching { scenario?.close() }
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        TutoTestHooks.player = null
        NativeEngine.sink = null
        ShadowMediaCodec.clearCodecs()
        LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
        com.pocketstudio.model.i18n.I18n.everyday = false
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
    }

    @Test
    fun drive() {
        val root = System.getProperty("ps.human.dir")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("only with -Phuman.dir", root != null)
        val dir = File(root, seat)
        assumeTrue("no seat $seat", File(dir, "start.txt").isFile)
        val start = File(dir, "start.txt").readLines().mapNotNull { l -> l.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap()
        freshInstall()
        TutoTestHooks.player = ears
        // The phone's AAC encoder (« Partager » in M4A): the shadow codec copies the sound through.
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        scenario = ActivityScenario.launch(MainActivity::class.java)
        val journal = File(dir, "journal.txt")
        settle(1500)
        var n = 1
        val timeoutMs = (start["timeout.minutes"]?.toLongOrNull() ?: 20L) * 60_000
        while (true) {
            show(dir, n)
            journal.appendText("#$n ${inside()}\n")
            val cmdFile = File(dir, "cmd-$n.txt")
            val waitEnd = System.currentTimeMillis() + timeoutMs
            while (!cmdFile.isFile) {
                if (System.currentTimeMillis() > waitEnd) { journal.appendText("#$n no gesture for too long: end\n"); return }
                Thread.sleep(100)
            }
            val cmd = cmdFile.readText().trim()
            journal.appendText("#$n > $cmd\n")
            if (cmd == "fin") return
            try {
                play(cmd)
            } catch (e: Throwable) {
                journal.appendText("#$n CRASH ${e.javaClass.name}: ${e.message?.take(400)}\n${e.stackTrace.take(12).joinToString("\n") { "    at $it" }}\n")
                File(dir, "sound-${n + 1}.txt").writeText("⚠ L'appli s'est arrêtée (plantage).")
                runCatching { show(dir, n + 1) }
                return
            }
            n++
        }
    }

    /** The phone as just installed, in French (as GuideJourneyTest.freshInstall). */
    private fun freshInstall() {
        for (d in listOf("projects", "projects-trash", "stems")) File(app.filesDir, d).deleteRecursively()
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { app.getSharedPreferences(it.name.removeSuffix(".xml"), 0).edit().clear().commit() }
        app.getSharedPreferences("settings", 0).edit().putString("language", "fr").commit()
        SettingsStore.forgetForTests()
        LaunchRule.askAtStartForTest = true
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        // The app's ViewModels with this test's application (the default factory keeps the first one).
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance").apply { isAccessible = true }.set(null, null)
    }

    /** The open project's ViewModel (fetched each time: opening a project rebuilds the activity). */
    private fun vm(): RhythmViewModel? {
        var v: RhythmViewModel? = null
        runCatching { scenario?.onActivity { v = ViewModelProvider(it)[RhythmViewModel::class.java] } }
        // An example of 8 s to hear (Robolectric has no engine to render it).
        v?.let { if (it.guideRenderForTest == null) it.guideRenderForTest = { _, _ -> FloatArray(OfflineExport.SAMPLE_RATE * 8) { k -> 0.3f * kotlin.math.sin(k * 0.05f) } to FloatArray(2) } }
        // The built-in sounds as the phone's engine renders them (Robolectric has none: « Ce son n'a pas pu être lu »).
        runCatching {
            scenario?.onActivity { a ->
                val lib = ViewModelProvider(a)[com.pocketstudio.app.library.SoundLibraryViewModel::class.java]
                if (lib.kitCache.isEmpty()) for (voice in 0 until 64) {
                    lib.kitCache[voice] = com.pocketstudio.app.audio.Pcm(FloatArray(2 * 9600) { k -> 0.5f * kotlin.math.sin(k * 0.01f) * kotlin.math.exp(-k / 4000f) }, 2, OfflineExport.SAMPLE_RATE)
                }
            }
        }
        lastVm = v
        return v
    }

    /**
     * The bench's engine stands in for a touch on « Partager » or « Écouter » of « Prêt à partager ? »,
     * then while that export runs or that song plays, and only then: the screens that read the
     * engine's meters or lit pads need the native library, which Robolectric has not. Any other
     * gesture (one that may open such a screen) first stops the song it played and lets it go.
     */
    private fun engineBefore(kind: String, x: Float, y: Float) {
        if (kind == "touche" && (touches("share_go", x, y) || touches("share_listen", x, y))) {
            NativeEngine.sink = NativeEngine.sink ?: BenchEngine()
            return
        }
        if (kind == "attends" || NativeEngine.sink == null) return
        val v = lastVm
        if (v?.export?.value?.running == true) return
        if (v != null && songPlaying(v)) v.toggleSong()
        settleLooper()
        NativeEngine.sink = null
    }

    private fun engineDone() {
        val v = lastVm
        if (NativeEngine.sink != null && v?.export?.value?.running != true && (v == null || !songPlaying(v))) NativeEngine.sink = null
    }

    private fun songPlaying(v: RhythmViewModel) = v.transport.value.playing && v.sessionUi.value.songIndex >= 0

    private fun settleLooper() = shadowOf(Looper.getMainLooper()).idle()

    /** The node tagged [tag] is under the finger at ([x], [y]) (screen pixels, any window). */
    private fun touches(tag: String, x: Float, y: Float): Boolean = runCatching {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any { n ->
            val at = n.positionOnScreen
            x >= at.x && x <= at.x + n.size.width && y >= at.y && y <= at.y + n.size.height
        }
    }.getOrDefault(false)

    /** Android's share menu, as a phone shows it once the song is ready: the apps to send it with. */
    private fun shareMenu() {
        val started = shadowOf(app).peekNextStartedActivity() ?: return
        if (started.action != Intent.ACTION_CHOOSER) return
        shadowOf(app).nextStartedActivity
        @Suppress("DEPRECATION")
        val stream = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let { send ->
            send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM) ?: send.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.firstOrNull()
        }
        val file = stream?.lastPathSegment?.substringAfterLast('/') ?: "?"
        val apps = arrayOf("WhatsApp", "Messages", "Gmail", "Drive", "Enregistrer dans Fichiers")
        scenario?.onActivity { a ->
            android.app.AlertDialog.Builder(a)
                .setTitle("Partager « $file »")
                .setItems(apps) { d, which ->
                    note = "Ton morceau « $file » part avec ${apps[which]} (ici rien ne part vraiment : c'est la fin du partage)."
                    d.dismiss()
                }
                .setOnCancelListener { note = "Menu de partage fermé : rien n'a été envoyé." }
                .show()
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** One gesture: « touche X Y », « appui-long X Y », « glisse X1 Y1 X2 Y2 », « ecris … », « retour », « attends S ». */
    private fun play(cmd: String) {
        val words = cmd.split(Regex("\\s+"))
        fun f(i: Int) = words.getOrNull(i)?.replace(',', '.')?.toFloatOrNull() ?: error("bad gesture: $cmd")
        val kind = words[0].lowercase()
        engineBefore(kind, words.getOrNull(1)?.replace(',', '.')?.toFloatOrNull() ?: -1f, words.getOrNull(2)?.replace(',', '.')?.toFloatOrNull() ?: -1f)
        when (kind) {
            "touche" -> { val d = SystemClock.uptimeMillis(); touch(MotionEvent.ACTION_DOWN, f(1), f(2), d); pass(80); touch(MotionEvent.ACTION_UP, f(1), f(2), d); settle(900) }
            "appui-long" -> { val d = SystemClock.uptimeMillis(); touch(MotionEvent.ACTION_DOWN, f(1), f(2), d); pass(900); touch(MotionEvent.ACTION_UP, f(1), f(2), d); settle(900) }
            "glisse" -> {
                val d = SystemClock.uptimeMillis()
                val (x1, y1, x2, y2) = listOf(f(1), f(2), f(3), f(4))
                touch(MotionEvent.ACTION_DOWN, x1, y1, d)
                for (i in 1..12) { pass(30); touch(MotionEvent.ACTION_MOVE, x1 + (x2 - x1) * i / 12, y1 + (y2 - y1) * i / 12, d) }
                touch(MotionEvent.ACTION_UP, x2, y2, d)
                settle(900)
            }
            "ecris" -> { compose.onNode(isFocused()).performTextInput(cmd.substringAfter(' ')); settle(500) }
            "retour" -> { androidx.test.espresso.Espresso.pressBackUnconditionally(); settle(900) }
            "attends" -> settle(((words.getOrNull(1)?.replace(',', '.')?.toFloatOrNull() ?: 2f) * 1000).toLong().coerceIn(100, 30_000))
            else -> error("unknown gesture: $cmd")
        }
    }

    /** A finger on the glass at ([x], [y]) in the screen's pixels: the window there gets it. */
    private fun touch(action: Int, x: Float, y: Float, downAt: Long) {
        val e = MotionEvent.obtain(downAt, SystemClock.uptimeMillis(), action, x, y, 0)
        e.source = InputDevice.SOURCE_TOUCHSCREEN
        InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(e, true)
        e.recycle()
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** [ms] of time go by, for the app's clocks and its animations. */
    private fun pass(ms: Long) {
        var left = ms
        while (left > 0) {
            val d = minOf(left, 50L)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(d))
            runCatching { compose.mainClock.advanceTimeBy(d) }
            ears.tick()
            lastVm?.let { playhead.tick(it, SystemClock.uptimeMillis()) }
            left -= d
        }
    }

    /** Time goes by, then the screen is let settle (the work off the main thread too, a rebuilt activity). */
    private fun settle(ms: Long) {
        pass(ms)
        repeat(4) {
            Thread.sleep(40)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            runCatching { compose.waitForIdle() }
        }
        vm()
        engineDone()
        shareMenu()
    }

    private fun show(dir: File, n: Int) {
        val shot = File(dir, "screen-$n.png")
        captureScreenRoboImage(shot.path, RoborazziOptions(taskType = RoborazziTaskType.Record, captureType = RoborazziOptions.CaptureType.Screenshot()))
        val sound = File(dir, "sound-$n.txt")
        if (!sound.isFile) sound.writeText(heard())
        File(dir, "turn.tmp").writeText("$n")
        File(dir, "turn.tmp").renameTo(File(dir, "turn"))
    }

    /** What a person would hear now (the pads' own sounds are not heard here). */
    private fun heard(): String = buildList {
        if (ears.playing) add("l'exemple du tuto joue (encore ${(ears.frames - ears.heard()) / OfflineExport.SAMPLE_RATE + 1} s)")
        if (vm()?.transport?.value?.playing == true) add("la musique du projet joue en boucle")
    }.let { if (it.isEmpty()) "Son : rien ne joue." else "Son : " + it.joinToString(" ; ") + "." }
        .let { s -> note?.let { n -> note = null; "$s\n$n" } ?: s }

    /** For the report, never shown to the tester: where the tutorial is. */
    private fun inside(): String = runCatching {
        val v = vm() ?: return@runCatching "no activity"
        val p = PrefsTutoStore(app).load(v.projectId)
        if (p == null) "project=${v.projectId} no tuto"
        else "project=${v.projectId} tuto=${p.style}/${p.level} active=${p.active} step=${p.current} done=${p.done.size} playing=${v.transport.value.playing}"
    }.getOrElse { "inside? ${it.message}" }

    companion object {
        /** A phone like the owner's (Honor), at 1.5 px a dp: images small enough to read quickly. */
        const val PHONE = "w392dp-h852dp-hdpi"
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanTesterA : HumanTester("a")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanTesterB : HumanTester("b")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanTesterC : HumanTester("c")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanTesterD : HumanTester("d")
