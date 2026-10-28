package com.pocketstudio.app.human

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.media.MediaFormat
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.pocketstudio.app.projects.AppViewModels
import com.pocketstudio.app.remix.RemixViewModel
import com.pocketstudio.app.remix.SeparationJob
import com.pocketstudio.app.remix.SeparationService
import com.pocketstudio.app.remix.SeparationState
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.separation.Demucs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
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
 *
 * The Remix (owner, 2026-10-28: seats E and F take the voice of a song into their jump up): the
 * phone's file picker gives « Ma chanson préférée.wav » ([BenchSong]) when the Remix asks for a
 * song, the separation service runs for real with a stand-in for the model ([BenchSeparation]),
 * and the bench's engine ([BenchEngine]) holds the stems while the Remix page is on screen.
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

    // The Remix (2026-10-28): one engine for the seat (the stems it holds outlive a screen), the
    // separation's stand-in, the service the app started, the bench's own polls of the engine.
    private val engine = BenchEngine()
    private var separation: BenchSeparation? = null
    private var service: ServiceController<SeparationService>? = null
    private var servicesSeen = 0
    private var serviceStarts = 0
    private var pollers: List<Job> = emptyList()
    private var remixVm: RemixViewModel? = null
    private var song: File? = null

    @After
    fun end() {
        stopPollers()
        runCatching { scenario?.close() }
        runCatching { service?.destroy() }
        separation?.close()
        SeparationJob.set(SeparationState.Idle)
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
        begin()
        val journal = File(dir, "journal.txt")
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
                problems.forEach { journal.appendText("#$n SCREEN ERROR $it\n") }
                problems.clear()
            } catch (e: Throwable) {
                journal.appendText("#$n CRASH ${e.javaClass.name}: ${e.message?.take(400)}\n${e.stackTrace.take(12).joinToString("\n") { "    at $it" }}\n")
                File(dir, "sound-${n + 1}.txt").writeText("⚠ L'appli s'est arrêtée (plantage).")
                runCatching { show(dir, n + 1) }
                return
            }
            n++
        }
    }

    /** The app opened on a phone as just installed, with the bench's stand-ins in place. */
    protected fun begin() {
        freshInstall()
        TutoTestHooks.player = ears
        // The phone's AAC encoder (« Partager » in M4A): the shadow codec copies the sound through.
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        // The Remix: a phone like the owner's (8 GB, enough free), the model from a local server.
        shadowOf(app.getSystemService(ActivityManager::class.java)).setMemoryInfo(
            ActivityManager.MemoryInfo().apply { totalMem = 8_000_000_000L; availMem = 4_200_000_000L },
        )
        SeparationJob.set(SeparationState.Idle)
        separation = BenchSeparation(app, remixTimeScale)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        settle(1500)
    }

    /** How long the model's download and the separation take (1: as the testers see them). */
    protected open val remixTimeScale: Double = 1.0

    /** The phone as just installed, in French (as GuideJourneyTest.freshInstall). */
    private fun freshInstall() {
        for (d in listOf("projects", "projects-trash", "stems", "models", "separation")) File(app.filesDir, d).deleteRecursively()
        File(app.filesDir, "stems-pinned.txt").delete()
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { app.getSharedPreferences(it.name.removeSuffix(".xml"), 0).edit().clear().commit() }
        app.getSharedPreferences("settings", 0).edit().putString("language", "fr").commit()
        SettingsStore.forgetForTests()
        LaunchRule.askAtStartForTest = true
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        // Notifications allowed, as a person answers the question (the bench cannot show it).
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
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
            NativeEngine.sink = NativeEngine.sink ?: engine
            return
        }
        if (kind == "attends" || NativeEngine.sink == null) return
        // The Remix page's own controls keep it: none of them opens another screen.
        if ((kind == "touche" || kind == "appui-long" || kind == "glisse") && remixPage() && touchesRemixControl(x, y)) return
        val v = lastVm
        if (v?.export?.value?.running == true) return
        if (v != null && songPlaying(v)) v.toggleSong()
        // The loop played with the bench's engine (▶ on the Remix page) goes on without it: stopped
        // and started again, so that the screen's polls of the engine start again without it too.
        val replay = v != null && v.transport.value.playing
        if (replay) {
            v!!.togglePlay()
            pass(100)
            runCatching { compose.waitForIdle() }
        }
        stopPollers()
        settleLooper()
        NativeEngine.sink = null
        if (replay) {
            v!!.togglePlay()
            pass(100)
        }
    }

    private fun engineDone() {
        val v = lastVm
        // The Remix page on screen: the engine holds the stems (« Écouter », « Jouer avec le projet », « → Pad »).
        if (remixPage()) {
            NativeEngine.sink = NativeEngine.sink ?: engine
            startPollers()
            return
        }
        if (NativeEngine.sink != null && v?.export?.value?.running != true && (v == null || !songPlaying(v))) {
            stopPollers()
            NativeEngine.sink = null
        }
    }

    /** The Remix page is on screen (not only composed beside it). */
    private fun remixPage(): Boolean = runCatching {
        val width = app.resources.displayMetrics.widthPixels
        compose.onAllNodesWithTag("remix_start").fetchSemanticsNodes().any { it.positionOnScreen.x in 0f..(width - 1f) }
    }.getOrDefault(false)

    /** The Remix page's buttons that use the engine, by their tags' start. */
    private val remixControls = listOf("use_", "listen_", "stem_", "remix_play", "remix_bar_next", "remix_bar_prev", "waveform_", "remix_follow_tempo", "remix_clear")

    private fun touchesRemixControl(x: Float, y: Float): Boolean = runCatching {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)).fetchSemanticsNodes().any { n ->
            val tag = n.config[SemanticsProperties.TestTag]
            // The part of it on screen (a row scrolled away under the tabs is not under the finger).
            remixControls.any { tag.startsWith(it) } && n.boundsInWindow.contains(androidx.compose.ui.geometry.Offset(x, y))
        }
    }.getOrDefault(false)

    /**
     * Where the stems are, read from the bench's engine as the page's own polls do on a phone: those
     * start with the page, before the bench could set its engine, and found none.
     */
    private fun startPollers() {
        if (pollers.isNotEmpty() && pollers.all { it.isActive }) return
        stopPollers()
        val remix = runCatching { ViewModelProvider(AppViewModels.of(app), ViewModelProvider.AndroidViewModelFactory.getInstance(app))[RemixViewModel::class.java] }.getOrNull()
        remixVm = remix
        val rhythm = lastVm
        pollers = listOfNotNull(
            remix?.let { r -> r.viewModelScope.launch { r.pollPreview() } },
            rhythm?.let { r -> r.viewModelScope.launch { r.pollStemPosition() } },
        )
    }

    private fun stopPollers() {
        pollers.forEach { it.cancel() }
        pollers = emptyList()
    }

    /**
     * The phone's file picker: asked for a sound on the Remix page, it gives the song of the test
     * ([BenchSong], made the first time); anywhere else, the bench has nothing to offer and says so.
     */
    private fun picker() {
        var answered = false
        scenario?.onActivity { a ->
            val asked = shadowOf(a).peekNextStartedActivityForResult() ?: return@onActivity
            val intent = asked.intent
            if (intent.action != Intent.ACTION_OPEN_DOCUMENT && intent.action != Intent.ACTION_GET_CONTENT) return@onActivity
            shadowOf(a).nextStartedActivityForResult
            if (shadowOf(app).peekNextStartedActivity()?.filterEquals(intent) == true) shadowOf(app).nextStartedActivity
            val types = listOfNotNull(intent.type) + intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES).orEmpty()
            if (types.any { it.startsWith("audio/") } && remixPage()) {
                val file = song ?: File(app.cacheDir, "Musique/${BenchSong.FILE_NAME}").also { if (!it.isFile) BenchSong.writeWav(it) }
                song = file
                val uri = android.net.Uri.Builder().scheme("content").authority("com.android.externalstorage.documents")
                    .appendPath("Musique").appendPath(BenchSong.FILE_NAME).build()
                shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { file.inputStream() }
                shadowOf(a).receiveResult(intent, Activity.RESULT_OK, Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                say(
                    "Le sélecteur de fichiers du téléphone s'est ouvert ; tu y as choisi « ${BenchSong.FILE_NAME} » " +
                        "(dossier Musique, 0:${BenchSong.SECONDS}, ${"%.1f".format(file.length() / 1e6).replace('.', ',')} Mo) : " +
                        "ta chanson préférée, avec une batterie, une basse, une voix qui chante et une nappe.",
                )
            } else {
                shadowOf(a).receiveResult(intent, Activity.RESULT_CANCELED, null)
                say("Le sélecteur de fichiers s'est ouvert, mais sur ce banc il ne propose une chanson que pour le Remix : rien n'a été choisi.")
            }
            answered = true
        }
        if (answered) settle(900)
    }

    /**
     * The services the app started: the separation (and its model's download, from the bench's
     * server) runs with its stand-in, as a phone would. The generation model's download (gigabytes
     * from the Internet) does not: as before, nothing happens.
     */
    private fun services() {
        val all = shadowOf(app).allStartedServices
        while (servicesSeen < all.size) {
            val intent = all[servicesSeen++]
            if (intent.component?.className != SeparationService::class.java.name || intent.action !in SEPARATION_ACTIONS) continue
            val running = service
            service = if (running != null && (SeparationJob.state.value.busy || intent.action == SEPARATION_ACTIONS.last())) {
                running.withIntent(intent).startCommand(0, ++serviceStarts)
            } else {
                runCatching { running?.destroy() }
                Robolectric.buildService(SeparationService::class.java, intent).create().startCommand(0, ++serviceStarts)
            }
        }
    }

    private fun say(text: String) {
        note = note?.let { "$it\n$text" } ?: text
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
        val kind = words[0].lowercase()
        engineBefore(kind, words.getOrNull(1)?.replace(',', '.')?.toFloatOrNull() ?: -1f, words.getOrNull(2)?.replace(',', '.')?.toFloatOrNull() ?: -1f)
        val padsBefore = stemPads()
        try {
            gesture(kind, words, cmd)
        } finally {
            padNotes(padsBefore)
        }
    }

    private fun gesture(kind: String, words: List<String>, cmd: String) {
        fun f(i: Int) = words.getOrNull(i)?.replace(',', '.')?.toFloatOrNull() ?: error("bad gesture: $cmd")
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
            // A failure in the screen's work stops its updates (a frozen screen): kept for the report.
            runCatching { compose.waitForIdle() }.onFailure { e -> problems += "${e.javaClass.name}: ${e.message?.take(300)}\n${e.stackTrace.take(8).joinToString("\n") { "    at $it" }}" }
        }
        vm()
        picker()
        services()
        engineDone()
        shareMenu()
        toasts()
    }

    /** A Toast is on the phone's screen for a few seconds but in no screenshot: the bench says what it reads (lot « remix 1 »). */
    private fun toasts() {
        val shown = org.robolectric.shadows.ShadowToast.shownToastCount()
        if (shown <= toastsSeen) return
        org.robolectric.shadows.ShadowToast.getTextOfLatestToast()?.let { say("Un petit message s'affiche quelques secondes en bas de l'écran : « $it ».") }
        toastsSeen = shown
    }

    private var toastsSeen = 0

    private fun show(dir: File, n: Int) {
        val shot = File(dir, "screen-$n.png")
        captureScreenRoboImage(shot.path, RoborazziOptions(taskType = RoborazziTaskType.Record, captureType = RoborazziOptions.CaptureType.Screenshot()))
        val sound = File(dir, "sound-$n.txt")
        if (!sound.isFile) sound.writeText(heard())
        File(dir, "turn.tmp").writeText("$n")
        File(dir, "turn.tmp").renameTo(File(dir, "turn"))
    }

    /** What a person would hear now (the pads' own sounds are not heard here, but the Remix's parts are). */
    protected fun heard(): String = buildList {
        if (ears.playing) add("l'exemple du tuto joue (encore ${(ears.frames - ears.heard()) / OfflineExport.SAMPLE_RATE + 1} s)")
        val v = vm()
        if (v?.transport?.value?.playing == true) {
            add("la musique du projet joue en boucle")
            remixHeard(v)?.let(::add)
            stemPads().filter { it.value.second }.forEach { (pad, s) -> add("le pad ${pad + 1} (« ${s.first} ») sonne dans la boucle") }
        }
        previewHeard()?.let(::add)
    }.let { if (it.isEmpty()) "Son : rien ne joue." else "Son : " + it.joinToString(" ; ") + "." }
        .let { s -> note?.let { n -> note = null; "$s\n$n" } ?: s }

    /** The song of the Remix along the project: which of its parts are heard. */
    private fun remixHeard(v: RhythmViewModel): String? {
        val r = v.project.value.remix
        if (r.setId == null) return null
        if (v.stemOverview.value == null) return "la chanson « ${r.name} » du Remix ne s'entend pas encore (ses pistes se chargent)"
        val solo = r.stems.any { it.solo }
        val heard = Demucs.STEMS.indices.filter { i -> val m = r.stems[i]; (if (solo) m.solo else !m.mute) && m.volume > 0.01f }
        val from = if (r.startBar > 0) ", depuis sa mesure ${r.startBar + 1}" else ""
        // Where the song is: known while the bench's engine plays it (the Remix page on screen).
        val voice = if (VOCALS in heard && NativeEngine.sink != null) voiceNow(r.name, v.stemPosition.value) else ""
        return when {
            heard.isEmpty() -> "la chanson « ${r.name} » est dans le projet, mais toutes ses parties sont coupées"
            heard.size == Demucs.STEMS.size -> "avec la chanson « ${r.name} » entière$from$voice"
            else -> "avec ${parts(heard)} de « ${r.name} »$from" + (if (solo) " (solo)" else "") + voice
        }
    }

    /** For the song of the test, whether its voice sings at [frame] (48 kHz; -1: not known). */
    private fun voiceNow(song: String, frame: Long): String {
        if (frame < 0 || song != BenchSong.FILE_NAME.substringBeforeLast('.')) return ""
        val t = frame.toDouble() / OfflineExport.SAMPLE_RATE
        return if (BenchSong.phrases.any { (a, b) -> t >= a && t < b }) " — la voix chante en ce moment" else " — la voix ne chante pas à cet instant"
    }

    /** « Écouter » on the Remix page: the song's parts that are on. */
    private fun previewHeard(): String? {
        val r = remixVm ?: return null
        val p = r.preview.value ?: return null
        if (!p.playing) return null
        val set = r.sets.value.firstOrNull { it.id == p.setId } ?: return null
        val on = p.on.indices.filter { p.on[it] }
        val what = if (on.isEmpty()) "aucune partie (toutes éteintes)" else parts(on)
        val pos = p.position.coerceAtLeast(0) / OfflineExport.SAMPLE_RATE
        val total = p.frames / OfflineExport.SAMPLE_RATE
        val voice = if (VOCALS in on) voiceNow(set.name, p.position) else ""
        return "le Remix fait écouter « ${set.name} » : $what (%d:%02d / %d:%02d)".format(pos / 60, pos % 60, total / 60, total % 60) + voice
    }

    /** « la voix seule », « la batterie et la basse »… */
    private fun parts(stems: List<Int>): String {
        val names = stems.map { mapOf("drums" to "la batterie", "bass" to "la basse", "other" to "les autres sons", "vocals" to "la voix").getValue(Demucs.STEMS[it]) }
        return if (names.size == 1) names[0] + if (Demucs.STEMS[stems[0]] == "other") " seuls" else " seule"
        else names.dropLast(1).joinToString(", ") + " et " + names.last()
    }

    /** Pads holding a part of a separated song: pad → (its sound's name, it plays in the loop). */
    private fun stemPads(): Map<Int, Pair<String, Boolean>> {
        val p = lastVm?.project?.value ?: return emptyMap()
        val stem = Regex("^(${STEM_NAMES.joinToString("|")}) · ")
        return p.drums.lanes.withIndex().mapNotNull { (i, lane) ->
            lane.sample?.name?.takeIf { stem.containsMatchIn(it) }?.let { i to (it to lane.hits.isNotEmpty()) }
        }.toMap()
    }

    /** A part of the song put on a pad by the gesture: the phone plays it once, as the pad's new sound. */
    private fun padNotes(before: Map<Int, Pair<String, Boolean>>) {
        stemPads().forEach { (pad, s) ->
            if (before[pad]?.first != s.first) say("Le pad ${pad + 1} reçoit « ${s.first} » et le joue une fois : on entend cette partie de la chanson, seule.")
        }
        val live = lastVm?.project?.value?.live?.clips.orEmpty()
        live.forEachIndexed { i, clip ->
            if (clip != null && clip.name !in liveSeen && STEM_NAMES.any { clip.name.startsWith("$it · ") }) {
                say("Le pad live ${i + 1} reçoit « ${clip.name} » (${clip.beats / 4} mesure${if (clip.beats > 4) "s" else ""}) : il jouera quand tu le lanceras (Live › Pads).")
            }
        }
        liveSeen = live.filterNotNull().map { it.name }.toSet()
    }

    private var liveSeen: Set<String> = emptySet()

    // --- For the bench's own tests (HumanRemixJourneyTest): what a person sees and does ---------

    /** One gesture as the tester types it (« touche X Y »…), then what is heard. */
    protected fun act(cmd: String): String {
        play(cmd)
        return heard()
    }

    /** The middle of the node tagged [tag] on the screen (the first one), or null when there is none. */
    protected fun centerOf(tag: String): Pair<Float, Float>? = runCatching {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().firstOrNull()?.let { n ->
            n.positionOnScreen.x + n.size.width / 2f to n.positionOnScreen.y + n.size.height / 2f
        }
    }.getOrNull()

    /** The node showing [text] (the first one), its middle, or null. */
    protected fun centerOfText(text: String, substring: Boolean = false): Pair<Float, Float>? = runCatching {
        compose.onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).fetchSemanticsNodes().firstOrNull()?.let { n ->
            n.positionOnScreen.x + n.size.width / 2f to n.positionOnScreen.y + n.size.height / 2f
        }
    }.getOrNull()

    protected fun project() = lastVm?.project?.value
    protected fun rhythm() = lastVm
    /** Failures of the screen's own work since the start (a frozen screen), for the report. */
    protected val problems = mutableListOf<String>()
    protected val screenHeight: Int get() = app.resources.displayMetrics.heightPixels
    protected val separationRuns: Int get() = separation?.separated ?: 0
    protected fun shot(file: File) = captureScreenRoboImage(file.path, RoborazziOptions(taskType = RoborazziTaskType.Record, captureType = RoborazziOptions.CaptureType.Screenshot()))

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

        /** A stem's name on a pad (DefaultNames.stemPad, kept in French whatever the language). */
        private val STEM_NAMES = listOf("Batterie", "Basse", "Autres", "Voix")
        private val VOCALS = Demucs.STEMS.indexOf("vocals")
        /** SeparationService's own actions: separate, download its model, cancel (last). */
        private val SEPARATION_ACTIONS = listOf("com.pocketstudio.SEPARATE", "com.pocketstudio.DOWNLOAD_MODEL", "com.pocketstudio.CANCEL_SEPARATION")
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

// The Remix's testers (2026-10-28): a song's voice into their jump up.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanTesterE : HumanTester("e")

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanTesterF : HumanTester("f")
