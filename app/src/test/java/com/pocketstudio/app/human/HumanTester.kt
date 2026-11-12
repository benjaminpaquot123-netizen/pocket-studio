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
import androidx.compose.ui.semantics.getOrNull
import com.pocketstudio.model.allScenes
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performScrollTo
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
 *
 * Audit ultra tour 0: Android's player takes the app's float previews as a phone's does ([BenchAudioTrack]).
 */
@Config(shadows = [BenchAudioTrack::class])
abstract class HumanTester(private val seat: String) {
    @get:Rule
    val compose = createEmptyComposeRule()
    /**
     * PLAN-RESTE 4.5: the state inherited from the test before must be pristine (else: red, naming that test). A real seat
     * ([drive], `-Phuman.dir`) is not made red: the state is put back and the leak written in its journal (review of banc45).
     */
    @get:Rule
    val pristine = BenchPristineRule { d -> seatJournalFor(d) }
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /** The seat's journal when the bench runs this seat for a lot (the [drive] test with its folder); null for the bench's own tests. */
    private fun seatJournalFor(d: org.junit.runner.Description): File? {
        if (d.methodName != "drive") return null
        val root = System.getProperty("ps.human.dir")?.takeIf { it.isNotBlank() }?.let(::File) ?: return null
        val dir = File(root, seat)
        return if (File(dir, "start.txt").isFile) File(dir, "journal.txt") else null
    }
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
    /** The bench's engine (its own tests). */
    internal val benchEngine: BenchEngine get() = engine
    private var separation: BenchSeparation? = null
    private var service: ServiceController<SeparationService>? = null
    private var servicesSeen = 0
    private var serviceStarts = 0
    private var pollers: List<Job> = emptyList()
    private var remixVm: RemixViewModel? = null
    private var song: File? = null

    @After
    fun end() {
        // PLAN-RESTE 4.5 (found by BenchPristineRule, run 1): a long render of this test (60 s in BenchTour1Test) ran on into the
        // tests after it, on a coroutine worker; cancelled first, before the screen closes.
        engine.cancelAllRenders()
        stopPollers()
        runCatching { scenario?.close() }
        // PLAN-RESTE 4.4: a download still running when the test ends (the stuck stand-in's) fails once the stand-in's
        // server closes (« connexion coupée »), on the service's own thread: let it end before the state is put back.
        separation?.close()
        val until = System.currentTimeMillis() + 5_000
        while (SeparationJob.state.value.busy && System.currentTimeMillis() < until) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idle()
        }
        backgroundQuiet(2_000)
        runCatching { service?.destroy() }
        // Round 6, lot ci2 (CI 43279184052, Zone3JourneysTest#redoAfterUndoInTheModeSimple red): the rest above ignores a worker
        // waiting on a socket (as « attends » must), so the app's own network work (the assistant's packs' catalogue from GitHub)
        // ran on past a short test and was the next one's inherited state, or not, by the CI's network speed. Waited for here,
        // the main looper running, only while every busy app worker is on the network, [NETWORK_END_CAP_MS] at most; other work
        // and what is left past the cap stay the detector's to name (BenchNetworkEndTest).
        appNetworkWorkEnded(NETWORK_END_CAP_MS)
        SeparationJob.set(SeparationState.Idle)
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        TutoTestHooks.player = null
        NativeEngine.sink = null
        ShadowMediaCodec.clearCodecs()
        BenchMic.unplug()
        // PLAN-RESTE 4.5 and the review of banc45: everything the detector checks is put back by its own list (the launch rule,
        // the settings, i18n, the tuto hooks, the intro, the hints, the spot checks, the prepared project, the ViewModel
        // factory that kept this test's Application, the touch guard's last touch, a take still being written, the files).
        BenchPristine.restore()
    }

    /** The seat's folder while the bench runs a seat (null in the bench's own tests). */
    private var seatDir: File? = null
    /** The turn being played (the journal's « #N »). */
    private var turnNow = 0
    private lateinit var options: SeatOptions
    private var watchdog: BenchWatchdog? = null
    private val layoutSeen = HashSet<String>()

    @Test
    fun drive() {
        val root = System.getProperty("ps.human.dir")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("only with -Phuman.dir", root != null)
        val dir = File(root, seat)
        assumeTrue("no seat $seat", File(dir, "start.txt").isFile)
        val (o, warnings) = SeatOptions.read(File(dir, "start.txt"))
        val journal = File(dir, "journal.txt")
        seatDir = dir
        journal.appendText("#0 SIEGE ${o.describe()}\n")
        warnings.forEach { journal.appendText("#0 OPTION ? $it\n") }
        val dog = BenchWatchdog(Thread.currentThread()).start()
        watchdog = dog
        try {
            begin(o)
            firstScreenLine?.let { journal.appendText("#0 PREMIER ECRAN $it\n") }
            o.amenee?.let { amenee(it, journal) }
            stalls(journal, 0)
            seatLoop(dir, o, journal)
        } finally {
            dog.stop()
            stalls(journal, turnNow)
            runCatching { SeatState.save(app, lastVm, dir, shared) }.onFailure { journal.appendText("#$turnNow ETAT ? ${it.message}\n") }
        }
    }

    /**
     * A long work under way that the screen shows (round 1, lot banc): the app's own long works (a separation, an export,
     * the video, a sound or a pack being made, the tutorial's kit downloading) or a spinning ring on screen. Not any
     * progress bar: the tutorial's course bar and the sliders are ones too, and stay.
     */
    internal fun longWorkShown(): Boolean {
        val v = lastVm
        if (SeparationJob.state.value.busy || v?.export?.value?.running == true || engineWorkRunning(v) || shownOnScreen("tuto_kit_progress")) return true
        return runCatching {
            val m = app.resources.displayMetrics
            compose.onAllNodes(SemanticsMatcher("a spinning ring") { n ->
                n.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo) == androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate
            }).fetchSemanticsNodes(atLeastOneRootRequired = false).any { n ->
                val at = BenchWindows.drawnPosition(n, m)
                n.size.width > 0 && at.x < m.widthPixels && at.y < m.heightPixels && at.x + n.size.width > 0 && at.y + n.size.height > 0
            }
        }.getOrDefault(false)
    }

    /** The turns of a seat: a screen (image, text, sound), a gesture, until the end. */
    internal fun seatLoop(dir: File, o: SeatOptions, journal: File) {
        var n = 1
        var gestures = 0
        var same = 0
        var lastPrint: String? = null
        var lastSong: Int? = null
        var afterWait = false
        val source = o.replayOf
        // PLAN-RESTE 7.5 (rejeu lisible): the seat the replay was made from (its screens, its finger's journal), and what
        // the replay saw differ from it: said under a verdict that is not « oracle tenu ».
        val origin = o.replayOrigin ?: source
        val originFinger: Map<Int, String> by lazy {
            origin?.let { File(it, "journal.txt") }?.takeIf { it.isFile }?.readLines().orEmpty()
                .mapNotNull { l -> Regex("^#(\\d+) DOIGT (.*)$").find(l)?.groupValues?.let { it[1].toInt() to it[2] } }
                .groupBy({ it.first }, { it.second }).mapValues { it.value.first() }
        }
        var lastDiff: String? = null
        val moved = mutableListOf<String>()
        val timeoutMs = o.timeoutMinutes * 60_000
        var benchExceptions = 0
        fun end(why: String) {
            journal.appendText("#$n FIN : $why\n")
            File(dir, "fin.txt").writeText(why)
        }
        fun replayVerdict(head: String) {
            val shown = origin?.listFiles()?.any { it.name.matches(Regex("screen-\\d+\\.txt")) } == true
            val lines = listOf(
                lastDiff?.let { "dernier écran différent de l'origine : $it" }
                    ?: if (shown) "aucun écran différent de l'origine" else "pas d'écran-texte à l'origine (${origin?.path ?: "-"}) : écrans non comparés",
            ) + moved
            File(dir, "rejeu.txt").writeText(head + lines.joinToString("") { "\n  $it" })
            end("rejeu : $head")
        }
        while (true) {
            turnNow = n
            val snap = capture(dir, n)
            val notes = mutableListOf<String>()
            // The screen's fingerprint: what is seen and the sound line (a pad that starts is a change).
            val heardNow = heard()
            File(dir, "sound-$n.txt").let { if (!it.isFile) it.writeText(heardNow) }
            val print = fingerprint(File(dir, "screen-$n.txt"), File(dir, "sound-$n.txt"))
            val song = songDigest()
            // « Rien ne change » (the screens after « attends » or « regarde » neither count nor reset it). Round 1 (lot banc):
            // nor while a long work shows its progress (a separation, an export, a pack being made): the screen waits
            // for it, the tester is not going round in circles; the count starts again once it is over.
            if (longWorkShown()) {
                same = 0
            } else if (!afterWait) {
                // Tour 6, E1 (seat D b #70): notes touched into the piano roll are drawn, not written in the screen's text
                // (lot « réalisme » R1): the song that changed at the gesture is a change too, whatever the screen's text.
                same = if (print == lastPrint && song == lastSong) same + 1 else 1
                if (same == 3) notes += "Rien ne change à l'écran depuis 3 gestes."
            }
            lastPrint = print
            lastSong = song
            val sound = publish(dir, n, notes)
            journal.appendText("#$n ROUTE ${snap.route} | MODE ${if (SettingsStore.state.value.simpleMode) "simple" else "pro"} | EMPREINTE $print | ${inside()}\n")
            journal.appendText("#$n SON ${sound.lineSequence().first()}\n")
            // Audit method v2: the bench's own stalls, apart from the app's (seat-metrics counts them as « banc »).
            if (benchStalls > stallsSaid) { journal.appendText("#$n BANC repos non venu en $IDLE_CAP_S s (${benchStalls - stallsSaid} fois)\n"); stallsSaid = benchStalls }
            layout(journal, n, snap)
            if (source != null) {
                // Audit method v2: a replay test of a fixed defect plays the gestures without comparing screens (the fix
                // changes them), then reads the last screen and sound against its oracle.
                val oracle = o.replayExpect != null || o.replayAbsent != null
                val wasText = File(origin, "screen-$n.txt").takeIf { it.isFile }
                val was = wasText?.let { fingerprint(it, File(origin, "sound-$n.txt")) }
                if (wasText != null && was != print) {
                    fun seen(d: File) = File(d, "screen-$n.txt").readLines().drop(1) +
                        File(d, "sound-$n.txt").takeIf { it.isFile }?.readLines().orEmpty().filterNot { it.startsWith("Rien ne change") }
                    lastDiff = "écran $n, empreinte $print (origine $was), première ligne différente : " +
                        (ScreenText.firstDifference(seen(origin!!), seen(dir)) ?: "aucune (le son ?)")
                }
                if (!oracle && was != print) {
                    replayVerdict("divergé au pas ${n - 1}" + if (was == null) " (pas d'écran-texte $n à l'origine)" else ""); return
                }
                if (n > o.replayN) {
                    val seen = File(dir, "screen-$n.txt").takeIf { it.isFile }?.readText().orEmpty() + "\n" + sound
                    val verdict = when {
                        !oracle -> "reproduit"
                        o.replayExpect != null && !seen.contains(o.replayExpect) -> "oracle non tenu : « ${o.replayExpect} » absent"
                        o.replayAbsent != null && seen.contains(o.replayAbsent) -> "oracle non tenu : « ${o.replayAbsent} » encore là"
                        else -> "oracle tenu"
                    }
                    if (verdict == "oracle tenu" || verdict == "reproduit") { File(dir, "rejeu.txt").writeText(verdict); end("rejeu : $verdict") } else replayVerdict(verdict)
                    return
                }
            }
            if (same >= 5) { journal.appendText("#$n RIEN NE CHANGE (5 écrans identiques)\n"); end("rien ne change (5 écrans identiques)"); return }
            if (same == 3) journal.appendText("#$n RIEN NE CHANGE (3 écrans identiques)\n")
            val cmdFile = File(dir, "cmd-$n.txt")
            if (source != null) {
                val from = File(source, "cmd-$n.txt")
                if (!from.isFile) { replayVerdict("divergé au pas $n (pas de geste $n à l'origine)"); return }
                cmdFile.writeText(from.readText())
            }
            val waitEnd = System.currentTimeMillis() + timeoutMs
            while (!cmdFile.isFile) {
                if (System.currentTimeMillis() > waitEnd) { journal.appendText("#$n no gesture for too long: end\n"); end("abandon : plus de geste"); return }
                Thread.sleep(100)
            }
            var cmd = cmdFile.readText().trim()
            // Socle du banc, point 3: « touche <étiquette> » (and appui-long, double-touche) aims at the node of that test
            // tag, brought into view and touched in its middle: a change of layout no longer breaks a replay. The gesture
            // then goes on as the pixels it resolved to (thumb's reach, finger's journal, the engine's lending).
            val aimed = cmd.split(Regex("\\s+"))
            fun number(w: String) = w.replace(',', '.').toFloatOrNull()
            // Tour 5 (rejeux6): « glisse <étiquette> DX DY » swipes from the middle of the tagged node by (DX, DY) pixels: a scroll or a
            // handle's drag that still needs the control where it is, not where it was.
            val swipeByTag = aimed.size == 4 && aimed[0].lowercase() == "glisse" && number(aimed[1]) == null && number(aimed[2]) != null && number(aimed[3]) != null
            if (swipeByTag || (aimed.size == 2 && aimed[0].lowercase() in TAG_GESTURES && number(aimed[1]) == null)) {
                val tag = aimed[1]
                val reached = runCatching { reachTag(tag) }.getOrNull()
                // A thumb touches a wide control where it can reach it, not necessarily in its middle: when the middle is out of
                // reach, the nearest reachable point of the node (a tag touch must not be refused where a finger would not be).
                if (reached == null) {
                    journal.appendText("#$n > $cmd\n#$n BANC étiquette introuvable : $tag\n")
                    if (source != null) {
                        originFinger[n]?.let { moved += "pas $n : à l'origine sous le doigt : $it" }
                        replayVerdict("divergé au pas $n (étiquette « $tag » introuvable)"); return
                    }
                    say("Banc : rien ne porte l'étiquette « $tag » sur cet écran : rien n'a été fait.")
                    n++
                    continue
                }
                // Review of rejeux6 (grave 2): the touch lands on a point of the element a finger can see (on the screen, not cut by
                // the list that scrolls it), within the thumb's reach, where that element is the one under the finger (not a
                // popup over it): when there is none, the touch is refused, as a finger could not touch that element there.
                val at = reachableNear(tag, reached, o.hand)
                if (at == null) {
                    journal.appendText("#$n > $cmd\n#$n REFUS « $tag » : aucun point visible, à portée du pouce (${o.hand.word}) et sous le doigt\n")
                    say("Ce bouton est caché, hors de l'écran ou trop loin de ton pouce (tu tiens l'appareil ${o.hand.word}) : rien ne s'est passé.")
                    n++
                    continue
                }
                journal.appendText("#$n ETIQUETTE $tag : ${at.first.toInt()} ${at.second.toInt()}\n")
                cmd = if (swipeByTag) "glisse ${at.first.toInt()} ${at.second.toInt()} ${(at.first + number(aimed[2])!!).toInt()} ${(at.second + number(aimed[3])!!).toInt()}"
                else "${aimed[0]} ${at.first.toInt()} ${at.second.toInt()}"
            }
            val words = cmd.split(Regex("\\s+"))
            val kind = words[0].lowercase()
            fun num(i: Int) = words.getOrNull(i)?.replace(',', '.')?.toFloatOrNull()
            if (kind != "attends" && kind != "fin" && kind != "regarde") {
                // The budget: this gesture is not played once it is spent (an image looked at counts half).
                val max = o.maxGestures
                val images = File(source ?: dir, "images.txt").takeIf { it.isFile }?.readLines()?.count { it.isNotBlank() } ?: 0
                if (max != null && gestures + images / 2.0 >= max) { end("abandon : budget ($max gestes, $gestures faits, $images images)"); return }
                gestures++
            }
            journal.appendText("#$n > $cmd\n")
            if (cmd == "fin") { end("fin"); return }
            afterWait = kind == "attends" || kind == "regarde"
            // External audit: a gesture written wrong (« tape 10 20 4 ») threw inside the app's turn and read as a crash of
            // the app; it is the bench's: said, not played, not counted.
            val malformed = when (kind) {
                "touche", "appui-long", "double-touche" -> num(1) == null || num(2) == null
                "glisse" -> (1..4).any { num(it) == null }
                // Plan 4.1: « tape X Y N tempo », the open challenge's pulse as the interval.
                "tape" -> (1..3).any { num(it) == null } || (num(4) == null && words.getOrNull(4)?.lowercase() != "tempo")
                "ecris" -> words.size < 2
                "retour", "attends", "regarde" -> false
                else -> true
            }
            if (malformed) {
                journal.appendText("#$n BANC geste mal écrit : $cmd\n")
                say("Banc : geste mal écrit (« $cmd ») : rien n'a été fait. Les gestes : touche X Y (ou touche ÉTIQUETTE), appui-long X Y, double-touche X Y, glisse X1 Y1 X2 Y2, tape X Y N MS (ou tape X Y N tempo : la pulsation du défi), ecris TEXTE, retour, attends S.")
                if (kind != "attends" && kind != "regarde") gestures--
                n++
                continue
            }
            val points = when (kind) {
                "touche", "appui-long", "double-touche", "tape" -> listOfNotNull(num(1)?.let { x -> num(2)?.let { x to it } })
                "glisse" -> listOfNotNull(num(1)?.let { x -> num(2)?.let { x to it } }, num(3)?.let { x -> num(4)?.let { x to it } })
                else -> emptyList()
            }
            val m = app.resources.displayMetrics
            if (points.any { (x, y) -> !SeatOptions.reachable(o.hand, x, y, m.widthPixels, m.heightPixels, m.density) }) {
                journal.appendText("#$n REFUS hors de portée du pouce (${o.hand.word})\n")
                say("Ce point est trop loin de ton pouce (tu tiens l'appareil ${o.hand.word}) : rien ne s'est passé.")
                n++
                continue
            }
            points.firstOrNull()?.let { (x, y) ->
                val under = screenText().under(x, y)
                journal.appendText("#$n DOIGT $under\n")
                if (source != null) originFinger[n]?.let { was -> fingerGone(was, under)?.let { moved += "pas $n : $it"; journal.appendText("#$n REJEU $it\n") } }
                Regex("(?:^|dans:|désactivé )(help_[A-Za-z0-9_]*)").find(under)?.let { journal.appendText("#$n AIDE ${it.groupValues[1]}\n") }
            }
            if (kind == "retour") journal.appendText("#$n RETOUR\n")
            try {
                val dialogsBefore = dialogSeconds.size
                play(cmd)
                dialogSeconds.drop(dialogsBefore).forEach { sec ->
                    journal.appendText("#$n DIALOGUE ${"%.1f".format(java.util.Locale.ROOT, sec)} s\n")
                    if (sec > DIALOG_SLOW_S) {
                        journal.appendText("#$n BANC dialogue lent (${"%.0f".format(java.util.Locale.ROOT, sec)} s > $DIALOG_SLOW_S s)\n")
                        say("Banc : ce geste dans la fenêtre à champ de texte a pris ${"%.0f".format(java.util.Locale.ROOT, sec)} s réelles au banc ; un téléphone répond tout de suite. Note-le dans « incidents-banc » avec banc?: temps.")
                    }
                }
                problems.forEach { journal.appendText("#$n SCREEN ERROR $it\n") }
                problems.clear()
            } catch (e: Throwable) {
                // PLAN-RESTE 5.10 (control 11): sorted by its frames, never by its class. Thrown by the test's own code
                // (Compose's test API, Robolectric, the bench): « BANC », the seat goes on; 4 of them end it « banc ».
                if (!thrownByTheApp(e)) {
                    benchExceptions++
                    journal.appendText("#$n BANC exception du banc (pas de l'appli) : ${e.javaClass.name}: ${e.message?.take(300)}\n${e.stackTrace.take(8).joinToString("\n") { "    at $it" }}\n")
                    say("Banc : le banc a eu un souci pendant ce geste (${e.javaClass.simpleName}) ; ce n'est pas l'appli. Note-le dans « incidents-banc » avec banc?: banc.")
                    // Review of « banc6b » (M3): what the gesture saw before the bench's exception is written, as for any gesture.
                    problems.forEach { journal.appendText("#$n SCREEN ERROR $it\n") }
                    problems.clear()
                    stalls(journal, n)
                    n++
                    if (benchExceptions >= BENCH_EXCEPTIONS_MAX) { end("banc : $benchExceptions exceptions du banc"); return }
                    continue
                }
                journal.appendText("#$n CRASH ${e.javaClass.name}: ${e.message?.take(400)}\n${e.stackTrace.take(12).joinToString("\n") { "    at $it" }}\n")
                File(dir, "sound-${n + 1}.txt").writeText("⚠ L'appli s'est arrêtée (plantage).")
                runCatching { capture(dir, n + 1); publish(dir, n + 1, emptyList()) }
                n++
                end("plantage")
                return
            }
            stalls(journal, n)
            n++
        }
    }

    /**
     * PLAN-RESTE 7.5: the button the origin's finger was on ([was], its `#n DOIGT` line) when it is no longer under the finger
     * ([now], `under(x, y)` at the replay), in words: « bouton « Suivant » (wizard_next) absent, sous le doigt : … », or
     * « déplacé (maintenant en x,y) » when it is still on the screen elsewhere. Null when it is still there, or when nothing was
     * aimed at (« rien »). A button without a test tag (« #12 », a view) is known by its words.
     */
    private fun fingerGone(was: String, now: String): String? {
        fun key(u: String) = u.removePrefix("désactivé ").substringBefore(" «").trim()
        val k = key(was)
        if (k.isEmpty() || k.startsWith("rien")) return null
        val words = Regex("« (.*) »").find(was)?.groupValues?.get(1)
        val tagged = !k.startsWith("#") && k != "vue"
        if (tagged && key(now) == k) return null
        if (!tagged && (words == null || now.contains("« $words »"))) return null
        val tag = k.removePrefix("dans:")
        val named = if (!tagged) "bouton « $words »" else if (words != null) "bouton « $words » ($tag)" else "bouton ($tag)"
        val at = if (tagged) centerOf(tag) else centerOfText(words!!)
        return named + (at?.let { " déplacé (maintenant en ${it.first.toInt()},${it.second.toInt()})" } ?: " absent") + ", sous le doigt : $now"
    }

    /**
     * A screen's fingerprint: its text (the title line aside) and what is heard (the bench's own note aside), clock times and
     * percentages left out. The same for a seat and its replay (scripts/human-replay.sh).
     */
    private fun fingerprint(text: File, sound: File): String =
        ScreenText.fingerprintOf(text.readLines().drop(1) + sound.takeIf { it.isFile }?.readLines().orEmpty().filterNot { it.startsWith("Rien ne change") })

    /** The open project as it would be saved, digested (null: none open): what a gesture changed of the song. */
    private fun songDigest(): Int? = runCatching { vm()?.project?.value?.let { com.pocketstudio.model.ProjectJson.encode(it).hashCode() } }.getOrNull()

    /** The stalls the watchdog saw since the last turn, into the journal. */
    private fun stalls(journal: File, n: Int) {
        val dog = watchdog ?: return
        while (true) { val s = dog.stalls.poll() ?: break; journal.appendText("#$n GEL ${s.removePrefix("gel ")}\n") }
    }

    /** LayoutScan on the screen as it is (the findings not seen earlier in the seat). */
    private fun layout(journal: File, n: Int, snap: ScreenText.Snapshot) {
        watchdog?.paused = true
        try {
            val found = runCatching { com.pocketstudio.app.audit.LayoutScan.scan(compose, app.resources.displayMetrics.density) }
            found.onFailure { journal.appendText("#$n LAYOUT ? ${it.javaClass.simpleName}: ${it.message?.take(200)}\n") }
            found.getOrNull()?.filter { shownFinding(it, snap) }?.forEach { f -> if (layoutSeen.add(f.toString())) journal.appendText("#$n LAYOUT $f\n") }
        } finally {
            watchdog?.paused = false
        }
    }

    /**
     * A finding about what a person sees: LayoutScan reads every node composed, also a page covered by
     * another (the project under « Que veux-tu faire ? ») or an activity left behind.
     */
    private fun shownFinding(f: com.pocketstudio.app.audit.LayoutScan.Finding, snap: ScreenText.Snapshot): Boolean {
        if (f.kind == com.pocketstudio.app.audit.LayoutScan.Kind.TARGET || f.kind == com.pocketstudio.app.audit.LayoutScan.Kind.LABEL) return true
        if (f.what !in snap.texts) return false
        if (f.kind != com.pocketstudio.app.audit.LayoutScan.Kind.OVERLAP) return true
        if ("over the touchable «  »" in f.detail) return false
        val other = Regex("^over « (.*) » \\(").find(f.detail)?.groupValues?.get(1) ?: return true
        return other in snap.texts
    }

    internal fun screenText(): ScreenText = ScreenText(
        compose, app.resources.displayMetrics, reads = !::options.isInitialized || options.reads, rtl = com.pocketstudio.model.i18n.I18n.lang.rtl,
    )

    /** What a person who only sees the screen has of it now, as text (the bench's own tests). */
    internal fun screenLines(): List<String> = screenText().read().lines

    /** The screen's route, as the journal says it (the bench's own tests). */
    internal fun screenRoute(): String = screenText().read().route

    /** The app opened on a phone as just installed, with the bench's stand-ins in place. */
    protected fun begin() = begin(null)

    /**
     * The same on the seat's phone ([o]: its screen, locale, font size, settings; null: the bench's
     * phone in French, as the bench's own tests always had it).
     */
    internal fun begin(o: SeatOptions?) {
        options = o ?: SeatOptions()
        if (o != null) {
            org.robolectric.RuntimeEnvironment.setQualifiers(o.qualifiers)
            org.robolectric.RuntimeEnvironment.setFontScale(o.fontScale)
        }
        freshInstall(options)
        TutoTestHooks.player = ears
        // The phone's AAC encoder (« Partager » in M4A): the shadow codec copies the sound through.
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_AUDIO_AAC, ShadowMediaCodec.CodecConfig(16 * 1024, 16 * 1024, passThrough))
        // Round 1 (lot banc): the phone's video encoder too (« Créer la vidéo », 720 x 1280 pictures in YUV 4:2:0).
        ShadowMediaCodec.addEncoder(MediaFormat.MIMETYPE_VIDEO_AVC, ShadowMediaCodec.CodecConfig(720 * 1280 * 3 / 2, 720 * 1280 * 3 / 2, passThrough))
        // The Remix: a phone like the owner's (8 GB, enough free), the model from a local server.
        shadowOf(app.getSystemService(ActivityManager::class.java)).setMemoryInfo(
            ActivityManager.MemoryInfo().apply { totalMem = 8_000_000_000L; availMem = 4_200_000_000L },
        )
        SeparationJob.set(SeparationState.Idle)
        separation = BenchSeparation(app, remixTimeScale)
        // Round 1 (lot banc): a microphone that hears someone sing ([BenchMic]).
        BenchMic.plugIn()
        // Audit method v2: the party sounds a phone decodes (Pad Live's ready set, the library's « Fête »).
        BenchPartySounds(app).register()
        // Audit method v2: the engine is there before the app opens, as a phone's (see ENGINE_ALWAYS).
        if (ENGINE_ALWAYS) NativeEngine.sink = engine
        val launched = System.currentTimeMillis()
        val clock = SystemClock.uptimeMillis()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        settle(1500)
        firstScreen(launched, clock)
    }

    /**
     * PLAN-RESTE 4.6, the first screen as measured: real seconds since the launch, the app's clock, seconds since the JVM
     * started (the bench's own start-up, what a tester waits before screen 1: F a 120 s, F c 300 s at round 5), the bench's
     * workers busy, the screen blank (nothing to touch), the bench's wait for it. Written as « #0 PREMIER ECRAN » by [drive].
     */
    internal var firstScreenLine: String? = null
        private set

    /**
     * PLAN-RESTE 4.6 (seats D b, F a, F c: a first screen almost empty): a blank first screen (« Ouverture du projet… »)
     * is waited for, [FIRST_SCREEN_CAP_MS] at most, only while the bench's own workers run (a loaded machine: the bench's
     * cause, proven by the threads). With them at rest it is shown as it is: what the app shows, « à trancher » (tel-20).
     */
    private fun firstScreen(launched: Long, clock: Long) {
        val busy = coroutineWorkersBusy()
        val from = System.currentTimeMillis()
        if (blankScreen() && busy) {
            while (blankScreen() && coroutineWorkersBusy() && System.currentTimeMillis() - from < FIRST_SCREEN_CAP_MS) {
                Thread.sleep(50)
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
                runCatching { compose.mainClock.advanceTimeBy(50) }
            }
            settle(200)
        }
        val waited = System.currentTimeMillis() - from
        fun s(ms: Long) = "%.1f".format(java.util.Locale.ROOT, ms / 1000.0)
        firstScreenLine = "reel_s=${s(System.currentTimeMillis() - launched)} simule_s=${s(SystemClock.uptimeMillis() - clock)} " +
            "jvm_s=${s(java.lang.management.ManagementFactory.getRuntimeMXBean().uptime)} travail=${if (busy) "oui" else "non"} " +
            "vide=${if (blankScreen()) "oui" else "non"} attente_s=${s(if (busy) waited else 0)}"
    }

    /**
     * The app's own screen is not there yet: its opening (« Ouverture du projet… ») is shown, or nothing can be touched.
     * The « Mode retours » bubble floats over everything, the opening too: it does not make a screen.
     */
    private fun blankScreen(): Boolean = shownOnScreen("project_opening") || runCatching {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.OnClick))
            .fetchSemanticsNodes(atLeastOneRootRequired = false).none { it.size.width > 0 && it.size.height > 0 }
    }.getOrDefault(true)

    /** How long the model's download and the separation take (1: as the testers see them). */
    protected open val remixTimeScale: Double = 1.0

    /**
     * The phone as just installed (as GuideJourneyTest.freshInstall), in the seat's language with its
     * settings and the permissions granted; with `fresh=reel` nothing at all: the app takes the
     * phone's language and asks for each permission (the bench shows the system's question).
     */
    private fun freshInstall(o: SeatOptions) {
        for (d in listOf("projects", "projects-trash", "stems", "models", "separation")) File(app.filesDir, d).deleteRecursively()
        File(app.filesDir, "stems-pinned.txt").delete()
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { app.getSharedPreferences(it.name.removeSuffix(".xml"), 0).edit().clear().commit() }
        if (!o.fresh) {
            app.getSharedPreferences("settings", 0).edit().apply {
                putString("language", o.lang.code)
                if (o.lang != com.pocketstudio.model.i18n.Lang.FR) putBoolean("englishNotes", !o.lang.solfege)
                // Audit ultra tour 0 (seat c, mode=pro): the app's first opening sets Mode simple until its guide was
                // seen; a person who chose Pro has gone past it.
                o.simple?.let { putBoolean("simpleMode", it); if (!it) putBoolean("guideSeen", true) }
                o.theme?.let { putString("theme", it.name) }
            }.commit()
        }
        SettingsStore.forgetForTests()
        LaunchRule.askAtStartForTest = true
        if (o.fresh) {
            shadowOf(app).denyPermissions(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
            // Notifications allowed, as a person answers the question (the bench cannot show it).
            shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        }
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
        // A.16 (G2): no hook on the pads' touches any more: the app sends its pads to the bench's engine, which plays them.
        if (seatPads() && v != null && v !== padsHooked) {
            padsHooked = v
            v.padLive.played.sourceForTest = BenchResample(v)
            v.padLive.makerForTest = BenchPackMaker(engine)
        }
        return v
    }

    private var padsHooked: RhythmViewModel? = null

    /** Audit ultra tour 0 (seat a): the downloaded packs' FLAC sounds are read as on a phone ([BenchFlac]). */
    private val flac by lazy { BenchFlac(app, com.pocketstudio.app.library.PackStore.dir(app)) }

    /** The bench's own tests: Pad Live's « Enregistrer ce que je joue » and its packs as on a seat (the pads play anyway). */
    internal var padsAsOnASeat = false
    private fun seatPads() = seatDir != null || padsAsOnASeat

    /**
     * The Live pads' lights, as a phone's pollLiveStates sets them from its engine about 30 times a second (Robolectric:
     * that poll stops at once without the native engine). Audit « banc crédible » A.16 (F3, G2): read from the bench's
     * engine, which played what the app sent it (ToggleClip, LaunchClipRow, « Tout couper » and ■ as toggleClip(-1)):
     * 1 waiting for its line, 2 playing. The bench used to write them itself from hooks on the app's touches.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun lightPads(v: RhythmViewModel) {
        if (NativeEngine.sink !== engine) return
        val states = IntArray(NativeEngine.LIVE_PADS).also(NativeEngine::clipStatesNow)
        val f = RhythmViewModel::class.java.getDeclaredField("_liveStates").apply { isAccessible = true }
        val flow = f.get(v) as kotlinx.coroutines.flow.MutableStateFlow<List<Int>>
        val now = flow.value.indices.map { states.getOrElse(it) { 0 } }
        if (now != flow.value) flow.value = now
    }

    /**
     * What the Live pads play, heard on the sound line (A.16): the hits that sounded since the last line, the loops that
     * play, the pads that wait for their line (the next beat for a hit, at most the next bar for a loop or a row), all as
     * the bench's engine plays them (see [BenchClips]).
     */
    private fun livePadsHeard(v: RhythmViewModel): String? {
        if (NativeEngine.sink !== engine) return null
        val pads = v.project.value.live
        val states = IntArray(NativeEngine.LIVE_PADS).also(NativeEngine::clipStatesNow)
        // Tour 6, E12 (seat C b #58): each pad with its number of hits (« tape … 4 » on one pad was heard « une fois »).
        val played = engine.clips.hitsPlayed().groupingBy { it }.eachCount()
        val hits = played.keys.toList()
        fun times(pad: Int) = played[pad]?.let { if (it == 1) "une fois" else "$it fois" }.orEmpty()
        val loops = states.indices.filter { states[it] == 2 && pads.at(it)?.oneShot == false }
        val waiting = states.indices.filter { states[it] == 1 }
        fun names(list: List<Int>) = list.sorted().joinToString(", ") { "${it + 1}" }
        return listOfNotNull(
            hits.takeIf { it.isNotEmpty() }?.let { h ->
                if (h.size == 1) "le pad live ${h[0] + 1} (« ${pads.at(h[0])?.name ?: "?"} ») a joué ${times(h[0])}"
                else if (h.all { played[it] == 1 }) "les pads live ${names(h)} ont joué une fois"
                else "les pads live ont joué : " + h.sorted().joinToString(", ") { "le ${it + 1} ${times(it)}" }
            },
            loops.takeIf { it.isNotEmpty() }?.let { if (it.size == 1) "le pad live ${it[0] + 1} joue en boucle" else "les pads live ${names(it)} jouent en boucle" },
            waiting.takeIf { it.isNotEmpty() }?.let {
                if (it.size == 1) "le pad live ${it[0] + 1} attend sa ligne (le prochain temps, ou la prochaine mesure)"
                else "les pads live ${names(it)} attendent leur ligne (le prochain temps, ou la prochaine mesure)"
            },
        ).joinToString(" ; ").ifEmpty { null }
    }

    /**
     * The phone's engine renders the ready-to-play set (loops of the style, party sounds) when Pad
     * Live opens; Robolectric has none, so the bench puts the same pads there with a short tone
     * each (a stand-in engine only while they are stored), as [HumanTester] stands in for the kit.
     */
    protected fun standInForTheStarter() {
        val vm = rhythm() ?: error("no project")
        if (vm.project.value.live.at(4) != null) return
        // Round 2 (zone 1, n° 2): also during a seat (the engine the bench kept stays once the set is made).
        val before = com.pocketstudio.engine.NativeEngine.sink
        com.pocketstudio.engine.NativeEngine.sink = before ?: BenchEngine()
        try {
            // Audit method v2 (engine always there): the app starts its own set as Pad Live opens, rendered by a phone's
            // engine; the bench's cannot render it (it waits forever). That job is cancelled, the bench's stand-in made.
            runCatching {
                val f = RhythmViewModel::class.java.getDeclaredField("liveStarterJob").apply { isAccessible = true }
                (f.get(vm) as? Job)?.cancel()
            }
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            if (vm.project.value.live.at(4) != null) return
            vm.fillLiveStarter { _ ->
                com.pocketstudio.model.LiveStarter.PADS.indices.associateWith { i ->
                    val pad = com.pocketstudio.model.LiveStarter.PADS[i]
                    val frames = if (pad.oneShot) 9_600 else 48_000
                    com.pocketstudio.app.audio.Pcm(FloatArray(frames * 2) { k -> 0.3f * kotlin.math.sin(k * (0.01f + i * 0.002f)) * kotlin.math.exp(-(k / 2) / 12_000f) }, 2, 48_000) to
                        (if (pad.oneShot) 1 else pad.beats)
                }
            }
            val until = System.currentTimeMillis() + 30_000
            while (vm.project.value.live.at(4) == null && System.currentTimeMillis() < until) {
                shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
                Thread.sleep(10)
            }
        } finally {
            com.pocketstudio.engine.NativeEngine.sink = before
        }
    }

    /**
     * Amenées (Audit ultra §3.1): the app brought to a mission's start by script before the tester gets
     * the seat (the gestures are the bench's, not counted, said in the journal as « #0 AMENEE »).
     * Audit ultra round 1 (lot B seat c, lot E seat c): the bench's gestures are not the tester's (no thumb rule:
     * [play] never applies it), each way in is the welcome's (first opening, Mode simple) or « Que veux-tu faire ? »'s
     * (Mode Pro: its guide seen), and the target is scrolled into view where it is (BenchAmeneesProfilesTest).
     */
    internal fun amenee(name: String, journal: File) {
        journal.appendText("#0 AMENEE $name\n")
        fun tap(tag: String) {
            val at = reachTag(tag) ?: error("amenée $name : « $tag » introuvable")
            journal.appendText("#0 AMENEE > touche ${at.first.toInt()} ${at.second.toInt()} ($tag)\n")
            play("touche ${at.first} ${at.second}")
        }
        /** The first of [tags] the screen has (the welcome's, then « Que veux-tu faire ? »'s), touched. */
        fun tapFirst(vararg tags: String): String {
            val tag = tags.firstOrNull { centerOf(it) != null } ?: error("amenée $name : ${tags.joinToString(" ou ") { "« $it »" }} introuvable")
            tap(tag)
            return tag
        }
        fun waitFor(what: String, done: () -> Boolean) {
            val until = System.currentTimeMillis() + 120_000
            while (!done()) {
                if (System.currentTimeMillis() > until) error("amenée $name : $what n'est jamais venu")
                play("attends 0.5")
            }
        }
        fun chooseMode() {
            if (centerOf("mode_card_simple") != null) tap(if (options.simple == false) "mode_card_pro" else "mode_card_simple")
        }
        when (name) {
            AMENEE_SONG -> {
                if (tapFirst("welcome_empty", "launch_new") == "launch_new") {
                    // « Nouveau morceau » of « Que veux-tu faire ? »: its page, then « Je pars de zéro ».
                    waitFor("« Je pars de zéro »") { centerOf("start_empty") != null }
                    tap("start_empty")
                }
                waitFor("le choix du mode") { centerOf("mode_card_simple") != null || projectShown() }
                chooseMode()
                waitFor("le projet") { projectShown() }
            }
            AMENEE_REMIX -> {
                tapFirst("welcome_remix", "launch_remix")
                waitFor("le Remix") { centerOf("remix_pick") != null || centerOf("mode_card_simple") != null }
                if (centerOf("mode_card_simple") != null) tap(if (options.simple == true) "mode_card_simple" else "mode_card_pro")
                waitFor("la page du Remix") { centerOf("remix_pick") != null }
            }
            AMENEE_PADLIVE -> {
                tap("launch_padlive")
                waitFor("Pad Live") { centerOf("padlive") != null }
                standInForTheStarter()
                waitFor("les pads prêts") { centerOf("padlive_busy") == null && project()?.live?.at(4) != null }
            }
        }
        journal.appendText("#0 AMENEE prête (${screenText().read().route})\n")
    }

    /** The project's tabs are on screen (the welcome and the mode's choice gone). */
    private fun tabsShown(): Boolean = runCatching {
        compose.onAllNodes(SemanticsMatcher("a tab") { n -> n.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("tab_") })
            .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
    }.getOrDefault(false) && centerOf("welcome") == null

    /** The project itself, with nothing over it (« Que veux-tu faire ? » is drawn over the studio, its tabs under it). */
    private fun projectShown(): Boolean = tabsShown() && centerOf("mode_card_simple") == null && centerOf("launch_page") == null && centerOf("new_project") == null

    /**
     * Brings the node tagged [tag] into the screen's middle part, then its middle. Round 1 (lot E seat c): the swipes
     * went on the screen's left margin, outside the welcome's column centred on a tablet: nothing scrolled, and
     * « Pad Live » was touched below the screen. The bench scrolls its container to it (as a swipe on it would),
     * then swipes on the target's own column if it is still at an edge.
     */
    internal fun reachTag(tag: String): Pair<Float, Float>? {
        val first = centerOf(tag) ?: return null
        if (tag.startsWith("vue_")) return first
        // PLAN-RESTE R.6 (re-reads of banc-r1): an element the screen shows whole is touched where it is, as a finger does; the
        // swipes below (up to 8 « glisse » + « attends 1 », 22 to 26 s of the bench's clock) are for one cut or off the screen.
        if (shownWhole(tag)) return first
        if (!inMiddle(first)) {
            runCatching { taggedNodes(tag)[0].performScrollTo() }
            play("attends 0.5")
        }
        repeat(8) {
            val at = centerOf(tag) ?: return null
            if (inMiddle(at)) return at
            val x = at.first.coerceIn(10f, app.resources.displayMetrics.widthPixels - 10f)
            val dy = (at.second - screenHeight / 2f).coerceIn(-500f, 500f)
            play("glisse $x ${screenHeight / 2f + dy / 2} $x ${screenHeight / 2f - dy / 2}")
            play("attends 1")
        }
        return centerOf(tag)
    }

    private fun inMiddle(at: Pair<Float, Float>) = at.second in 0.2f * screenHeight..0.8f * screenHeight

    /** The first node tagged [tag] is drawn whole on the screen (see [shownWhole]). */
    private fun shownWhole(tag: String): Boolean =
        runCatching { taggedNodes(tag).fetchSemanticsNodes().firstOrNull()?.let(::shownWhole) }.getOrNull() == true

    /** [node] is drawn whole on the screen (not cut by a parent that scrolls, not off an edge): a finger can touch it where it is. */
    internal fun shownWhole(node: androidx.compose.ui.semantics.SemanticsNode): Boolean = runCatching {
        val m = app.resources.displayMetrics
        val b = BenchWindows.drawnBounds(node, m)
        node.size.width > 0 && node.size.height > 0 &&
            b.width >= node.size.width * 0.9f && b.height >= node.size.height * 0.9f &&
            b.left >= 0f && b.top >= 0f && b.right <= m.widthPixels && b.bottom <= m.heightPixels
    }.getOrDefault(false)

    /**
     * The bench's engine stands in for a touch on « Partager » or « Écouter » of « Prêt à partager ? »,
     * then while that export runs or that song plays, and only then: the screens that read the
     * engine's meters or lit pads need the native library, which Robolectric has not. Any other
     * gesture (one that may open such a screen) first stops the song it played and lets it go.
     */
    private fun engineBefore(kind: String, x: Float, y: Float) {
        // Audit method v2 (META-AUDIT change 2): the engine is there all the time, as a phone's. Lending it touch by touch, from
        // lists of tags kept by hand, made most of the bench's false alarms in rounds 0 to 4 (each tag forgotten = « nothing plays »).
        if (ENGINE_ALWAYS) {
            NativeEngine.sink = engine
            return
        }
        // Audit ultra round 4 (seats A b, C b, C d, D a, D d: « Rien n'a été joué pendant la prise »): a keyboard take is written
        // at the engine's play position (Tour4AppProofTest); the bench never gave its engine to one, and let it go during it
        // (stopped, then ▶ again without it: the stop ended the take at once). The engine comes before the touch that starts it.
        if (kind == "touche" && noteTakeTouched(x, y)) {
            engineForANoteTake(lastVm)
            return
        }
        if (kind == "touche" && (touches("share_go", x, y) || touches("share_listen", x, y) || playsTheSong(x, y) || needsTheEngine(x, y))) {
            NativeEngine.sink = NativeEngine.sink ?: engine
            return
        }
        // Round 1 (lot banc): « Enregistrer » of an audio track (or ● in its microphone mode) opens the engine's microphone:
        // the bench's engine has one ([BenchEngine.micStart]); it counts in with the transport, as long as the app asks with ▶ (A.9).
        if (kind == "touche" && audioTakeTouched(x, y)) {
            NativeEngine.sink = NativeEngine.sink ?: engine
            return
        }
        if (kind == "attends" || NativeEngine.sink == null) return
        // Audit ultra tour 0 (seat a): the assistant's « Écouter » are rendered by the engine, as on a phone.
        if (previewsWanted()) return
        // The Remix page's own controls keep it: none of them opens another screen.
        if ((kind == "touche" || kind == "appui-long" || kind == "glisse") && stemPages() && touchesRemixControl(x, y)) return
        val v = lastVm
        if (v?.export?.value?.running == true) return
        // Audit ultra tour 0 (seat b): a stretch on its way to a pad or a Live cell, a voice being cut into
        // phrases, a sound being prepared go on with the engine (a phone's never goes away under them).
        if (engineWorkRunning(v)) return
        // Round 1 (zone 3, n° 27; seats D b, C a): the song playing went back to its part's loop (« Intro » in a loop) at
        // any other gesture, a sound changed for instance: the bench stopped it, let its engine go and played ▶ again,
        // the loop the app plays without one. A phone's song goes on: the engine stays while the song plays (the
        // screens' own reads of the engine go through it, the meters are the native engine's only).
        if (v != null && songPlaying(v)) return
        letTheEngineGo(v)
    }

    /**
     * The bench lets its engine go. What plays with it goes on without it (step 5b: the song played from the
     * Remix page, the loop of its ▶; since round 0, the song of ▶ and of the guide's « Écouter » anywhere): stopped,
     * the stop COMPOSED, then the engine let go and ▶ again (the loop the app plays with no engine). The screen's
     * polls of the engine stop with the transport and start again without it: the position under the top bar
     * ([RhythmViewModel.pollPosition], keyed on the transport playing) looks for an engine once, as it starts.
     * Audit ultra round 1 (lot A, seats a and d): the song was stopped and played again in the same frame, so that
     * poll never stopped; its next read went to the native library Robolectric has not (UnsatisfiedLinkError),
     * which ended the screen's updates: every touch after it (tabs, « Retour », ■) left the screen as it was.
     * A phone's engine never goes: nothing like it there (PlayThenTabsTest).
     */
    private fun letTheEngineGo(v: RhythmViewModel?) {
        val playing = v != null && v.transport.value.playing
        if (playing) {
            if (songPlaying(v!!)) v.toggleSong() else v.togglePlay()
            pass(100)
            runCatching { idleScreen() }
        }
        stopPollers()
        settleLooper()
        NativeEngine.sink = null
        if (playing) {
            v!!.togglePlay()
            pass(100)
        }
    }

    /** « ● Enregistrer » of an editor (« Enregistrer ma mélodie ») or the top bar's ● in its notes mode: a take of notes. */
    private fun noteTakeTouched(x: Float, y: Float): Boolean =
        touches("record_start", x, y) || (touches("top_record", x, y) && !topRecordIsTheMicrophones())

    /**
     * The bench's engine under a take of notes, as a phone's always is. Music already playing without it (the loop ▶ plays
     * once the bench let its engine go): stopped, the engine given, ▶ again, so that its play position runs from that ▶
     * (an engine joining a transport it never started would say « stopped », and every key would be dropped).
     */
    private fun engineForANoteTake(v: RhythmViewModel?) {
        if (NativeEngine.sink != null) return
        val playing = v != null && v.transport.value.playing
        if (playing && !v!!.recording.value) {
            v.togglePlay()
            pass(100)
            runCatching { idleScreen() }
        }
        NativeEngine.sink = engine
        if (playing && !v!!.transport.value.playing) {
            v.togglePlay()
            pass(100)
        }
    }

    private fun engineDone() {
        // The project the gesture may have opened (its activity is a new one): its ViewModel's work counts.
        val v = vm() ?: lastVm
        // The Remix page (its cut screen, its stem board) on screen: the engine holds the stems (« Écouter »,
        // « Jouer avec le projet », « → Pad », « → Live »).
        if (stemPages()) {
            val was = NativeEngine.sink
            NativeEngine.sink = was ?: engine
            startPollers()
            // Flaky fix (audit stage 1, HumanRemixJourneyTest « the waveforms never came », 1 run in 3): a song put
            // in the project while the bench had let its engine go (« Mettre dans mon projet » is not the Remix
            // page) was never loaded: the view model only loads stems for an engine that is there. The bench's
            // engine comes back as a phone's engine (re)starts: the view model syncs it, the song's stems with it.
            // (Whenever it is still missing, not only as the engine comes back: the song may enter the project a
            // touch after the engine did. Once a song: a load under way is left to finish.)
            val song = v?.project?.value?.remix?.setId
            // Audit ultra tour 0: and again each time the engine comes back to a page without the song's stems (the
            // stem board needs them; a phone's engine never let them go).
            if (v != null && song != null && (song != syncedFor || was == null) && v.stemOverview.value == null && !v.remixLoading.value) {
                syncedFor = song
                v.syncEngine()
            }
            return
        }
        if (ENGINE_ALWAYS) {
            NativeEngine.sink = engine
            if (v != null && padLiveAwaitsItsSet(v)) standInForTheStarter()
            return
        }
        // Audit ultra round 1: a loop that plays with it (a take written while it plays) goes on without it as in
        // [engineBefore]: the stop composed first, so that no poll of the screen reads the engine once gone.
        if (NativeEngine.sink != null && v?.export?.value?.running != true && (v == null || !songPlaying(v)) && !engineWorkRunning(v) && !previewsWanted()) {
            letTheEngineGo(v)
        }
        // Audit ultra tour 0 (seat c): a Pad Live take (the performance, « Enregistrer ce que je joue ») is written
        // with the engine (its pads bounced to a track, the take put on a pad): the bench's engine comes for it and
        // stays until it is written, before the gesture that ends it. (Seat a) The assistant on screen: its
        // « Écouter » are rendered by the engine, which is there before the touch.
        // Round 1 (lot banc): a sound recorded at the microphone for a pad (« Enregistrer ma voix », a live pad) becomes the
        // engine's sample when it ends (NativeSample.create): the bench's engine comes while it records.
        if (NativeEngine.sink == null && (takeUnderWay(v) || previewsWanted() || padTakeRecording(v))) NativeEngine.sink = engine
        // Audit ultra round 2 (zone 1, n° 2, seat A c): Pad Live opened by the tester's own touch (not an amenée) showed 16
        // empty pads: a phone makes its ready-to-play set with the engine as it opens (PadLiveReadyEveryWayTest), the
        // bench's had gone. The bench makes it as for the amenée.
        if (v != null && padLiveAwaitsItsSet(v)) standInForTheStarter()
    }

    /** Pad Live on screen, its « Live » project still waiting for its ready-to-play set (nothing making it). */
    private fun padLiveAwaitsItsSet(v: RhythmViewModel): Boolean =
        shownOnScreen("padlive") && v.project.value.padLive.home && com.pocketstudio.model.LiveStarter.wanted(v.project.value) && !v.liveStarting.value

    /**
     * Audit ultra tour 0 (seat a): the guide's « Écouter » and ▶ play the song from a part on a phone (« Montée
     * mes. 25 »), through the engine; without it the app only moved its « from » and the loop of the first part
     * played. The bench's engine comes for the touch and stays while the song plays ([engineDone]).
     */
    private fun playsTheSong(x: Float, y: Float): Boolean {
        // Audit ultra round 3 (seats A b, C a): a part touched (« Touche une partie pour écouter le morceau à partir d'elle »),
        // « Jouer depuis… › Tout le morceau, du début » or a part, « ▶ Depuis ma prise »: the app plays the song only with an
        // engine (Tour3AppProofTest); the bench's had gone, nothing played and ▶ then played the part's loop alone.
        if (SONG_STARTS.any { touchesStart(it, x, y) }) return true
        if (lastVm?.project?.value?.song.isNullOrEmpty()) return false
        // Tour 5, chantier 4: « ▶ le morceau » (at the thumb, or at the top with no thumb's bar); the top « ▶ cette partie » loops a part.
        return touches("thumb_play", x, y) || touches("top_song", x, y) || touchesStart("guide_option_", x, y)
    }

    /**
     * Audit ultra round 1 (lot banc): touches that a phone's engine answers and the bench's had not: « Exporter » of the
     * Mix tab (export_*, the app's render returns at once with no engine: 3 seats saw it do nothing), « Créer la vidéo »
     * (« Le moteur audio n'est pas prêt »), « Écouter » of a performance kept (the song from its part), « Montre-moi »
     * (each gesture's sound is the engine's pad or note).
     */
    private fun needsTheEngine(x: Float, y: Float): Boolean =
        ENGINE_TAGS.any { touches(it, x, y) }

    /** The assistant on screen, or a sound of the library being made to be heard. */
    private fun previewsWanted(): Boolean = shownOnScreen("wizard") || library()?.preparing?.value != null ||
        // Audit ultra round 2 (zone 1, n° 1, seats A c and B d): the sound library open: its « Poser » makes the pad's sound
        // with the engine (NativeSample.create, after the sound is read): the bench let its engine go before the touch, the
        // sound went to the native library Robolectric has not (UnsatisfiedLinkError), the pad kept its sound. A phone's
        // engine never goes (PlaceFromLibraryTest).
        shownOnScreen("library")

    /** The sound library's ViewModel (the assistant's and the library's « Écouter »). */
    private fun library(): com.pocketstudio.app.library.SoundLibraryViewModel? {
        var lib: com.pocketstudio.app.library.SoundLibraryViewModel? = null
        runCatching { scenario?.onActivity { lib = ViewModelProvider(it)[com.pocketstudio.app.library.SoundLibraryViewModel::class.java] } }
        return lib
    }

    /** What the library plays (« Écouter » of the assistant or of a sound), as the ear would name it. */
    private fun libraryHeard(): String? {
        val ref = library()?.playing?.value ?: return null
        return when (ref) {
            is com.pocketstudio.app.library.SoundRef.StyleDemo -> "l'assistant fait écouter un extrait du style « ${com.pocketstudio.model.StarterWizard.style(ref.styleId)?.name ?: ref.styleId} »"
            is com.pocketstudio.app.library.SoundRef.MoodDemo -> "l'assistant fait écouter les accords de l'ambiance « ${ref.mood.label} » sur le rythme du style"
            else -> "un son de la bibliothèque joue (écoute)"
        }
    }

    /** The song whose stems the bench last asked the view model to load (see [engineDone]). */
    private var syncedFor: String? = null

    /**
     * Work of the app's that needs the engine until it ends (audit ultra tour 0, seat b: « Voix vers le pad… »
     * forever, the bench had let its engine go under it): the stem board's (a stretch to a pad, a track, a Live
     * cell; phrases), a pad's sound being made.
     */
    internal fun engineWorkRunning(v: RhythmViewModel? = lastVm): Boolean {
        if (v == null) return false
        return v.stemBoard.state.value.busy != null || v.sampleTask.value is com.pocketstudio.app.rhythm.SampleTask.Busy || takeUnderWay(v) ||
            v.padLive.busy.value != null || liveFillRunning(v) || v.liveStarting.value ||
            // Round 1 (lot banc): the video being made, « Montre-moi » under way (its finger on screen).
            v.videoBusy || shownOnScreen("tuto_finger") ||
            // A take of an audio track recording or being written (the bench's microphone is its engine's).
            v.audioRecording.value != null || v.takeSaving.value || padTakeRecording(v) ||
            // Audit ultra round 4: a take of notes (keyboard, pads, 303) records at the engine's position until it ends.
            v.recording.value
    }

    /**
     * Audit ultra tour 0 (seat a): a song made by the assistant gets its live pads made with the engine once it is
     * open (`fillLivePads`, its loops rendered then stored): the bench's engine, there when it started, stays
     * until it ends (a phone's never goes away under it).
     */
    private fun liveFillRunning(v: RhythmViewModel): Boolean = runCatching {
        val f = RhythmViewModel::class.java.getDeclaredField("liveFill").apply { isAccessible = true }
        (f.get(v) as? Job)?.isActive == true
    }.getOrDefault(false)

    private fun padTakeRecording(v: RhythmViewModel?): Boolean = v?.sampleTask?.value is com.pocketstudio.app.rhythm.SampleTask.Recording

    /** A Pad Live take armed, recording, being written, or recorded and waiting for its place ([engineDone]). */
    private fun takeUnderWay(v: RhythmViewModel?): Boolean {
        if (v == null) return false
        val take = v.take.value
        val performance = take !is com.pocketstudio.app.live.TakeUi.Ready && take !is com.pocketstudio.app.live.TakeUi.Done
        return performance || v.padLive.played.ui.value !is com.pocketstudio.app.padlive.PlayedTakeUi.Ready
    }

    /** The Remix page, its cut screen or its stem board on screen: the stems' screens. */
    protected fun stemPages(): Boolean = remixPage() || shownOnScreen("board") || shownOnScreen("cut_screen")

    private fun shownOnScreen(tag: String): Boolean = runCatching {
        val width = app.resources.displayMetrics.widthPixels
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any { it.positionOnScreen.x in 0f..(width - 1f) }
    }.getOrDefault(false)

    /** The Remix page is on screen (not only composed beside it). */
    protected fun remixPage(): Boolean = runCatching {
        val width = app.resources.displayMetrics.widthPixels
        compose.onAllNodesWithTag("remix_start").fetchSemanticsNodes().any { it.positionOnScreen.x in 0f..(width - 1f) }
    }.getOrDefault(false)

    /** The Remix page's buttons that use the engine, by their tags' start. */
    // Round 3 (zone 1, n° 4): « → Pad » asks which pad first (« Sur quel pad ? », pad_pick_*): its answer cuts the stem with the engine.
    private val remixControls = listOf("use_", "import_", "listen_", "stem_", "remix_play", "remix_bar_next", "remix_bar_prev", "waveform_", "remix_follow_tempo", "remix_clear", "remix_open_", "board_", "cut_", "pad_pick_")

    private fun touchesRemixControl(x: Float, y: Float): Boolean = runCatching {
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag)).fetchSemanticsNodes().any { n ->
            val tag = n.config[SemanticsProperties.TestTag]
            // The part of it on screen (a row scrolled away under the tabs is not under the finger).
            // The import screen (step 5c) is a dialog: its window is not at the screen's corner.
            val bounds = if (tag.startsWith("import_") || tag.startsWith("pad_pick_")) {
                androidx.compose.ui.geometry.Rect(BenchWindows.drawnPosition(n, app.resources.displayMetrics), androidx.compose.ui.geometry.Size(n.size.width.toFloat(), n.size.height.toFloat()))
            } else {
                n.boundsInWindow
            }
            remixControls.any { tag.startsWith(it) } && bounds.contains(androidx.compose.ui.geometry.Offset(x, y))
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
            } else if ((types.contains("application/zip") && projectImportShown()) || (types.contains("audio/midi") && !types.contains("application/zip"))) {
                // Banc fiable 3.3 (P1; round 5 C d, the import of a .zip or a .mid out of reach): the bench gives a file
                // made here from the project open now (never one of Benjamin's), as a friend would send it.
                val zip = types.contains("application/zip")
                val name = if (zip) "Le-morceau-de-Sam.pocketstudio.zip" else "Le-morceau-de-Sam.mid"
                val file = File(app.cacheDir, "Téléchargements/$name").also { it.parentFile?.mkdirs() }
                val made = runCatching {
                    if (zip) {
                        val catalog = com.pocketstudio.model.ProjectCatalog(File(app.filesDir, "projects"), File(app.filesDir, "projects-trash"))
                        file.outputStream().use { catalog.exportZip(com.pocketstudio.app.projects.CurrentProject.get(app), it) }
                    } else {
                        file.writeBytes(com.pocketstudio.model.MidiFile.export(lastVm?.project?.value ?: error("no project")))
                    }
                }.isSuccess && file.length() > 0
                if (made) {
                    val uri = android.net.Uri.Builder().scheme("content").authority("com.android.providers.downloads.documents")
                        .appendPath("Téléchargements").appendPath(name).build()
                    shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { file.inputStream() }
                    shadowOf(a).receiveResult(intent, Activity.RESULT_OK, Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                    say("Le sélecteur de fichiers du téléphone s'est ouvert ; tu y as choisi « $name » (dossier Téléchargements, ${"%.1f".format(file.length() / 1e3).replace('.', ',')} Ko), envoyé par un ami.")
                } else {
                    shadowOf(a).receiveResult(intent, Activity.RESULT_CANCELED, null)
                    say("Le sélecteur de fichiers s'est ouvert, mais le banc n'a pas pu fabriquer de fichier à proposer : rien n'a été choisi (banc).")
                }
            } else {
                shadowOf(a).receiveResult(intent, Activity.RESULT_CANCELED, null)
                say("Le sélecteur de fichiers s'est ouvert, mais sur ce banc il ne propose une chanson que pour le Remix, et un projet (.zip) ou un MIDI (.mid) quand l'appli les demande : rien n'a été choisi.")
            }
            answered = true
        }
        if (answered) settle(900)
    }

    /**
     * Review of 3.3: the sound packs' import asks the same types as a project's (.zip); a project given there made a false
     * « Aucun son dans ce .zip ». The bench gives a project only where a project is imported (Mes projets, a new project).
     */
    private fun projectImportShown(): Boolean = shownOnScreen("starter_import") || shownOnScreen("project_new")

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
            val at = BenchWindows.drawnPosition(n, app.resources.displayMetrics)
            x >= at.x && x <= at.x + n.size.width && y >= at.y && y <= at.y + n.size.height
        }
    }.getOrDefault(false)

    /** A node whose tag starts with [prefix] is under the finger at ([x], [y]). */
    private fun touchesStart(prefix: String, x: Float, y: Float): Boolean = runCatching {
        compose.onAllNodes(SemanticsMatcher("tag $prefix…") { n -> n.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith(prefix) })
            .fetchSemanticsNodes(atLeastOneRootRequired = false).any { n ->
                val at = BenchWindows.drawnPosition(n, app.resources.displayMetrics)
                x >= at.x && x <= at.x + n.size.width && y >= at.y && y <= at.y + n.size.height
            }
    }.getOrDefault(false)

    /** Android's share menu, as a phone shows it once the song is ready: the apps to send it with. */
    private fun shareMenu() {
        val started = shadowOf(app).peekNextStartedActivity() ?: return
        if (started.action != Intent.ACTION_CHOOSER) return
        shadowOf(app).nextStartedActivity
        @Suppress("DEPRECATION")
        // Audit ultra round 3 (zone 2, « Partager les 2 fichiers »): every file sent (ACTION_SEND_MULTIPLE), not the first only.
        val streams = started.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)?.let { send ->
            send.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
                ?: runCatching { listOfNotNull(send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)) }.getOrDefault(emptyList())
        }.orEmpty()
        val names = streams.mapNotNull { it.lastPathSegment?.substringAfterLast('/') }
        names.forEach(::keepShared)
        val file = names.joinToString(", ").ifEmpty { "?" }
        // Audit ultra round 1 (n° 18, seats A c, C a, D b, E b, B b): a phone's share menu speaks the phone's language
        // (the seat's, as the app): its title is the one the app gives (translated), its apps by their names, never French words.
        @Suppress("DEPRECATION")
        val title = started.getCharSequenceExtra(Intent.EXTRA_TITLE)?.toString() ?: com.pocketstudio.model.i18n.tr("Partager le morceau")
        val apps = arrayOf("WhatsApp", "Messages", "Gmail", "Drive", "Files")
        scenario?.onActivity { a ->
            android.app.AlertDialog.Builder(a)
                .setTitle("$title · $file")
                .setItems(apps) { d, which ->
                    note = shareNote(names.map { n -> n to (sharedSize(n) ?: -1L) }, apps[which])
                    d.dismiss()
                }
                .setOnCancelListener { note = "Menu de partage fermé : rien n'a été envoyé." }
                .show()
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** One gesture: « touche X Y », « appui-long X Y », « glisse X1 Y1 X2 Y2 », « ecris … », « retour », « attends S ». */

    /**
     * PLAN-RESTE 5.10: an exception is the app's when the first frame of com.pocketstudio in it (or in its causes) is outside
     * the bench (`com.pocketstudio.app.human`): the app's code threw, even under a test call. Else the test's code threw it.
     * Review of « banc6b » (grave): every link of the chain is read, a bench frame that rethrows the app's exception as
     * its cause never hides the app's crash.
     */
    internal fun thrownByTheApp(e: Throwable): Boolean {
        val seen = HashSet<Throwable>()
        var t: Throwable? = e
        while (t != null && seen.add(t)) {
            val first = t.stackTrace.firstOrNull { it.className.startsWith("com.pocketstudio.") }
            if (first != null && !first.className.startsWith("com.pocketstudio.app.human.")) return true
            t = t.cause
        }
        return false
    }

    /** PLAN-RESTE 5.3 (control 4): the real seconds each gesture made in a window with a text field took (« DIALOGUE N s »). */
    internal val dialogSeconds = mutableListOf<Double>()

    /** For the bench's own tests (PLAN-RESTE 5.10): runs at the start of each gesture, to provoke an exception there. */
    internal var beforeGesture: ((String) -> Unit)? = null

    private fun play(cmd: String) {
        beforeGesture?.invoke(cmd)
        followTextDialogs()
        // PLAN-RESTE 5.3 (control 4): a gesture made in (or closing) a window with a text field is timed in real seconds.
        val inDialog = textDialog
        val at = System.nanoTime()
        try {
            playGesture(cmd)
        } finally {
            if (inDialog || textDialog) dialogSeconds += (System.nanoTime() - at) / 1e9
        }
    }

    private fun playGesture(cmd: String) {
        followTextDialogs()
        val words = cmd.split(Regex("\\s+"))
        val kind = words[0].lowercase()
        flac.register()
        engineBefore(kind, words.getOrNull(1)?.replace(',', '.')?.toFloatOrNull() ?: -1f, words.getOrNull(2)?.replace(',', '.')?.toFloatOrNull() ?: -1f)
        val padsBefore = stemPads()
        val x = words.getOrNull(1)?.replace(',', '.')?.toFloatOrNull() ?: -1f
        val y = words.getOrNull(2)?.replace(',', '.')?.toFloatOrNull() ?: -1f
        val micTake = kind == "touche" && audioTakeTouched(x, y)
        try {
            gesture(kind, words, cmd)
        } finally {
            padNotes(padsBefore)
            // Nothing started though the bench's engine was there (a refusal of the app's own): said, never silent.
            if (micTake && lastVm?.audioRecording?.value == null && !NativeEngine.isAvailable) {
                say("Sur ce banc, le micro des pistes audio ne s'est pas ouvert : sur un téléphone, le décompte puis la prise commenceraient ici.")
            }
        }
    }

    /**
     * Round 1 (lot banc): « Enregistrer » of an audio track, the top bar's ● in its microphone mode, or « Le micro » of its
     * choice (round 1, n° 30). The engine opens
     * the microphone for these (NativeEngine.micStart): the bench gives its engine, whose microphone hears [BenchMic]'s voice.
     */
    private fun audioTakeTouched(x: Float, y: Float): Boolean =
        touchesStart("audio_record_", x, y) || touches("record_choice_mic", x, y) ||
            (touches("top_record", x, y) && topRecordIsTheMicrophones())

    /**
     * Audit ultra round 3 (zone 4, n° 23, seats E c, F a): the top ● in its microphone mode. Its name is its icon's
     * description, which [ScreenText.under] does not read (« micro » was never found there): the bench kept no engine
     * and the app said « Le moteur audio n'a pas démarré » where a phone records (the card's « Enregistrer » worked).
     */
    private fun topRecordIsTheMicrophones(): Boolean = runCatching {
        val mic = com.pocketstudio.model.i18n.tr("Enregistrer le micro sur la piste audio")
        compose.onAllNodesWithTag("top_record").fetchSemanticsNodes().any { n ->
            n.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription).orEmpty().any { it == mic }
        }
    }.getOrDefault(false)

    private fun gesture(kind: String, words: List<String>, cmd: String) {
        fun f(i: Int) = words.getOrNull(i)?.replace(',', '.')?.toFloatOrNull() ?: error("bad gesture: $cmd")
        when (kind) {
            "touche" -> { val d = SystemClock.uptimeMillis(); touch(MotionEvent.ACTION_DOWN, f(1), f(2), d); pass(80); touch(MotionEvent.ACTION_UP, f(1), f(2), d); settle(900) }
            // Round 1 integration: two touches at one place as a finger does them (a « touche » settles ~1 s, more while
            // the screen animates: two of them in a row were no double tap, past [com.pocketstudio.app.ui.DoubleTouchGuard]'s window).
            "double-touche" -> {
                for (i in 0..1) {
                    val d = SystemClock.uptimeMillis()
                    touch(MotionEvent.ACTION_DOWN, f(1), f(2), d); pass(80); touch(MotionEvent.ACTION_UP, f(1), f(2), d)
                    if (i == 0) pass(150)
                }
                settle(900)
            }
            "appui-long" -> { val d = SystemClock.uptimeMillis(); touch(MotionEvent.ACTION_DOWN, f(1), f(2), d); pass(900); touch(MotionEvent.ACTION_UP, f(1), f(2), d); settle(900) }
            // Banc fiable 3.3 (P3, the challenges: round 5 C a #32, no star in 3 tries): « tape X Y N INTERVALLE_MS », N touches
            // of 60 ms at one place, one every INTERVALLE_MS of the bench's clock (the app's and the engine's), as a finger
            // keeping time does; one gesture. N 1 to 32, interval 100 to 4000 ms.
            // Plan 4.1 (P3, the testers' 0 or 1 star). « tempo » as the interval is the open challenge's pulse (the project's
            // tempo kept in 80 to 120 BPM: cause d, a fixed 500 ms was 33 % too fast at 90 BPM). A « tape » given while the
            // example plays taps right away, as a person who goes on with it does (those touches are not counted by the app:
            // T5-DEFI-ECOUTE stays visible to the seats). Review of 4.1: a hand, not a metronome: each touch lands up to
            // ±[TAPE_JITTER_MS] off its place, drawn from a generator seeded by the seat and the gesture's number (the same
            // seat replays the same hand).
            "tape" -> {
                val n = f(3).toInt().coerceIn(1, 32)
                val pulse = challengePulse()
                val byPulse = words.getOrNull(4)?.lowercase() == "tempo"
                if (byPulse && pulse == null) {
                    say("Banc : « tape … tempo » : aucun défi ouvert, rien n'a été tapé (écris l'intervalle en ms).")
                    return
                }
                val every = (if (byPulse) Math.round(pulse!!.beatMs) else f(4).toLong()).coerceIn(100, 4_000)
                val hand = java.util.Random(seat.hashCode().toLong() * 1_000_003L + tapes++)
                // Each touch on its own place of the clock (start + i × interval, plus the hand's error). `pass(every - 60)`
                // after a touch let the frames' own time add up: 500 ms asked gave touches ~590 ms apart (2 of 4 at 120 BPM,
                // the bench's own 1 star), so the time is passed in small steps up to each touch's place.
                val start = SystemClock.uptimeMillis() + TAPE_JITTER_MS
                for (i in 0 until n) {
                    passUntil(start + i * every + hand.nextInt(2 * TAPE_JITTER_MS.toInt() + 1) - TAPE_JITTER_MS)
                    val d = SystemClock.uptimeMillis()
                    touch(MotionEvent.ACTION_DOWN, f(1), f(2), d); passUntil(d + 60); touch(MotionEvent.ACTION_UP, f(1), f(2), d)
                }
                settle(900)
            }
            "glisse" -> {
                val d = SystemClock.uptimeMillis()
                val (x1, y1, x2, y2) = listOf(f(1), f(2), f(3), f(4))
                touch(MotionEvent.ACTION_DOWN, x1, y1, d)
                for (i in 1..12) { pass(30); touch(MotionEvent.ACTION_MOVE, x1 + (x2 - x1) * i / 12, y1 + (y2 - y1) * i / 12, d) }
                touch(MotionEvent.ACTION_UP, x2, y2, d)
                settle(900)
            }
            // Round 1 (lot D, seat d): typing with no field focused threw the test API's AssertionError,
            // which the bench told the tester was an app crash. On a phone the keys go nowhere: say so.
            "ecris" -> {
                val text = cmd.substringAfter(' ')
                val typed = runCatching { if (textDialog) typeInDialog(text) else compose.onNode(isFocused()).performTextInput(text) }
                // T5-Dc-4 / Ac-61 / Db-16 (round 5): said as a phone would show it, so the tester never takes it for the app's.
                if (typed.isFailure) say("Rien ne s'écrit : aucun champ n'a le curseur (un téléphone n'afficherait pas de clavier) : touche d'abord le champ, puis écris.")
                settle(500)
            }
            "retour" -> { androidx.test.espresso.Espresso.pressBackUnconditionally(); settle(900) }
            "attends" -> {
                settle(((words.getOrNull(1)?.replace(',', '.')?.toFloatOrNull() ?: 2f) * 1000).toLong().coerceIn(100, 30_000))
                slowWorkDone()
            }
            // Only looking (scripts/human-act.py answers « regarde » itself; written as a gesture, nothing is touched).
            "regarde" -> settle(100)
            else -> error("unknown gesture: $cmd")
        }
    }

    /**
     * A finger on the glass at ([x], [y]) in the screen's pixels: the window there gets it. The
     * screen (screen-N.png) draws each window where a phone puts it (a dialog centred), while
     * Robolectric takes every window's touches as if it sat at its own (0, 0): the touch is moved by
     * that gap, for the window a phone would give it to (lot « tuto 5 »: « Quitter », « Passer »,
     * « Annuler » of a centred dialog fell beside it). The gesture keeps its window's gap to the end.
     */
    private fun touch(action: Int, x: Float, y: Float, downAt: Long) {
        if (action == MotionEvent.ACTION_DOWN) gestureShift = BenchWindows.shiftAt(x, y, app.resources.displayMetrics)
        val (dx, dy) = gestureShift
        sendTouch(action, x - dx, y - dy, downAt)
    }

    /** The gap of the window the current gesture went to (see [touch]). */
    private var gestureShift = 0f to 0f

    private fun sendTouch(action: Int, x: Float, y: Float, downAt: Long) {
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
            lastVm?.let {
                playhead.tick(it, SystemClock.uptimeMillis())
                lightPads(it)
                engine.tempo = it.project.value.tempoBpm
                engine.beatsPerBar = it.project.value.beatsPerBar
            }
            left -= d
        }
    }

    /**
     * The app's network work at the end of a test (lot ci2): waited for, the main looper running, while at least one app worker
     * is busy and every busy one waits on a socket ([onTheNetwork]), [capMs] at most. Work of the app off the network is not
     * waited for: the detector (BenchPristine) still names it, as before.
     */
    private fun appNetworkWorkEnded(capMs: Long) {
        val until = System.currentTimeMillis() + capMs
        while (System.currentTimeMillis() < until) {
            val busy = appWorkers()
            if (busy.isEmpty() || !busy.all(::onTheNetwork)) return
            Thread.sleep(20)
            runCatching { shadowOf(Looper.getMainLooper()).idle() }
        }
    }

    /** The coroutine workers running the app's code (a frame of com.pocketstudio), as BenchPristine.appThreads names them. */
    private fun appWorkers(): List<Thread> {
        var root = Thread.currentThread().threadGroup
        while (root.parent != null) root = root.parent
        val threads = arrayOfNulls<Thread>(root.activeCount() + 16)
        val n = root.enumerate(threads, true)
        return (0 until n).mapNotNull { threads[it] }.filter { t ->
            t.name.startsWith("DefaultDispatcher-worker") && t !== Thread.currentThread() && t.stackTrace.any { "pocketstudio" in it.className }
        }
    }

    /** [t] is in a network call (socket, HTTP or TLS frames anywhere in its stack: an HTTPS read is deep below the app's frame). */
    private fun onTheNetwork(t: Thread): Boolean = t.stackTrace.any { f ->
        f.className.startsWith("sun.nio.ch.") || f.className.startsWith("java.net.") || f.className.startsWith("sun.net.") ||
            f.className.startsWith("sun.security.ssl.") || f.className.startsWith("javax.net.ssl.")
    }

    /**
     * Flaky fix (audit stage 1): « attends 2 » passes 2 s of the main thread's clock at once, but the work the
     * app runs off the main thread (a project created, a pack read, on Dispatchers.IO / Default) needs real
     * time, and a loaded machine (the whole suite) gave it less than the 160 ms below: the screen was read
     * before it came (HumanLucasBugsTest « still in the assistant », « nothing to touch: Aide et premiers pas »).
     * Waits, in real time, until no coroutine worker is running and the main thread has caught up, as a
     * phone's seconds would have let it. The cap only stops a worker that never rests (it is not a timeout).
     */
    private fun backgroundQuiet(capMs: Long = 5_000) {
        val until = System.currentTimeMillis() + capMs
        var quiet = 0
        while (quiet < 2 && System.currentTimeMillis() < until) {
            Thread.sleep(10)
            shadowOf(Looper.getMainLooper()).idle()
            if (coroutineWorkersBusy()) quiet = 0 else quiet++
        }
    }

    /**
     * Banc fiable 1.4 (round 5, seat C b: « attends 30, 60, 120, 300, 500 », 13 min and 6 gestures, the model never came):
     * the bench's stand-ins for slow work (the model's download, the separation, BenchSeparation) sleep in REAL time, but
     * « attends N » only moved the simulated clock. Now « attends » pumps the main thread in real time while that work runs,
     * [SLOW_WORK_CAP_MS] at most; past it, a « BANC » line says the work is still going (not the app's fault).
     * PLAN-RESTE 4.4: and only while something moves. A thread asleep in a stuck stand-in, or the separation's flag alone
     * (the app says it works, nothing runs), held a tester's « attends 2 » for 120 s. Once neither the stand-in's heartbeat
     * ([BenchSeparation.beats]) nor a busy worker (off the network) has moved for [STILL_MS], the hand goes back.
     */
    /** The threads that held the last « attends » (name, how long, where), for a failure's message; empty: none. */
    internal var waitHeldBy: String = ""

    private fun slowWorkDone() {
        val until = System.currentTimeMillis() + SLOW_WORK_CAP_MS
        var waited = false
        var beats = separation?.beats ?: 0L
        var movedAt = System.currentTimeMillis()
        // Lot « verdicts »: who held the wait (a busy worker), how often it was seen, and where it was the last time.
        val heldBy = LinkedHashMap<String, Pair<Int, List<StackTraceElement>>>()
        val started = System.currentTimeMillis()
        // The separation's own flag too: between two of its steps (or before its service ran) no thread is in the stand-in.
        while (slowBenchWorkBusy() || SeparationJob.state.value.busy) {
            val now = System.currentTimeMillis()
            val b = separation?.beats ?: 0L
            val busy = busyWorker()
            busy?.let { t -> heldBy[t.name] = ((heldBy[t.name]?.first ?: 0) + 1) to t.stackTrace.take(12) }
            if (b != beats) heldBy["battement du remplaçant (BenchSeparation)"] = ((heldBy["battement du remplaçant (BenchSeparation)"]?.first ?: 0) + 1) to emptyList()
            if (b != beats || busy != null) { beats = b; movedAt = now }
            if (now - movedAt >= STILL_MS) {
                // A stand-in that stopped moving is the bench's; the flag alone is what the app shows (a phone shows it too).
                if (slowBenchWorkBusy()) say("Banc : le remplaçant du banc (téléchargement ou séparation) n'avance plus depuis ${STILL_MS / 1000} s : l'attente s'arrête là. Note-le dans « incidents-banc » avec banc?: temps.")
                break
            }
            if (now >= until) {
                say("Banc : le travail en cours (téléchargement ou séparation) n'est pas fini après ${SLOW_WORK_CAP_MS / 1000} s d'attente réelle ; attends encore. Si cela dure, note-le dans « incidents-banc » avec banc?: temps : ce peut être le banc ou l'appli.")
                heldWait(heldBy, started)
                return
            }
            waited = true
            Thread.sleep(50)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            runCatching { compose.mainClock.advanceTimeBy(50) }
            // What a phone does meanwhile: the file picker answered, a service started (the separation after the download).
            picker()
            services()
        }
        heldWait(heldBy, started)
        if (waited) settle(200)
    }

    /** [waitHeldBy] for this « attends », printed too (the test's output): each thread that held it, seen N times (50 ms apart), its last stack. */
    private fun heldWait(heldBy: Map<String, Pair<Int, List<StackTraceElement>>>, started: Long) {
        waitHeldBy = heldBy.entries.joinToString("\n") { (name, seen) ->
            "fil $name occupé (vu ${seen.first} fois sur ${System.currentTimeMillis() - started} ms) :\n" + seen.second.joinToString("\n") { "    at $it" }
        }
        if (waitHeldBy.isNotEmpty()) println("BANC attente retenue par :\n$waitHeldBy")
    }

    /** A thread runs (or sleeps in) the bench's slow stand-ins: BenchSeparation's server, decoding, separating, analysing. */
    private fun slowBenchWorkBusy(): Boolean {
        var root = Thread.currentThread().threadGroup
        while (root.parent != null) root = root.parent
        val threads = arrayOfNulls<Thread>(root.activeCount() + 16)
        val n = root.enumerate(threads, true)
        return (0 until n).any { i ->
            val t = threads[i] ?: return@any false
            t !== Thread.currentThread() && t.stackTrace.any { f -> f.className.startsWith(BenchSeparation::class.java.name) }
        }
    }

    /** A coroutine worker thread (Dispatchers.Default and IO share them) is running, not parked waiting for work. */
    private fun coroutineWorkersBusy(): Boolean = busyWorker() != null

    /** The first coroutine worker found running ([coroutineWorkersBusy]), or null. */
    private fun busyWorker(): Thread? {
        var root = Thread.currentThread().threadGroup
        while (root.parent != null) root = root.parent
        val threads = arrayOfNulls<Thread>(root.activeCount() + 16)
        val n = root.enumerate(threads, true)
        return (0 until n).firstNotNullOfOrNull { i ->
            threads[i]?.takeIf { t -> t.name.startsWith("DefaultDispatcher-worker") && t.state == Thread.State.RUNNABLE && !waitsOnTheNetwork(t) }
        }
    }

    /** A worker reading a socket (the model's download from the bench's server) waits for the network, as on a phone: not work to wait for. */
    private fun waitsOnTheNetwork(t: Thread): Boolean = t.stackTrace.take(12).any { f ->
        f.className == "sun.nio.ch.NioSocketImpl" || f.className == "sun.nio.ch.SocketDispatcher" || f.className == "sun.nio.ch.Net" ||
            f.className.startsWith("java.net.Socket") || f.className.startsWith("sun.net.www.")
    }

    /**
     * Audit ultra round 3 (n° 16, seats B a and E c, « gel en tapant un nom »; Zone3DialogDiagTest): under Robolectric a Compose
     * dialog holding a text field never comes back to rest, without one line of the app (a phone's does). The bench waited for
     * that rest (each look at the screen does by default) and froze in every rename window. While such a window is open, the
     * looks at the screen do not wait for rest ([followTextDialogs]) and the screen is let settle by frames, a bounded wait.
     */
    private var textDialog = false
    private var suppressSaid = false
    /** A text field was seen in the dialog open now (until no dialog is open). */
    private var fieldDialog = false

    /** How many times Compose's rest did not come within [IDLE_CAP_S] seconds (journal: « BANC »). */
    internal var benchStalls = 0
    private var stallsSaid = 0

    private fun followTextDialogs() {
        // Audit method v2 (round 4, « Renommer » › ✕ « Effacer », 240 s): the field's window is read again and again while
        // the dialog lives; one look that misses it (the window being redrawn) made the bench wait for a rest that never
        // comes (its cursor blinks), 60 s a look. A dialog that held a field holds it until it closes.
        val dialog = BenchWindows.dialogOpen()
        if (!dialog) fieldDialog = false
        if (BenchWindows.textFieldInADialog()) fieldDialog = true
        textDialog = (dialog && fieldDialog) || BenchWindows.focusedTextField()
        // Audit method v2: a screen that did not rest within IDLE_CAP_S is read as it is too (no wait), until it rests again.
        val noWait = textDialog || restless
        runCatching {
            val env = compose.javaClass.getDeclaredField("environment").apply { isAccessible = true }.get(compose)
            // The environment is a subclass (Robolectric's): the field is its parent's. Looked up the class chain; until
            // 2026-11-06 it was looked up on the subclass only, failed in silence, and no look was ever left unwaited.
            val holder = generateSequence<Class<*>>(env.javaClass) { it.superclass }.first { c -> c.declaredFields.any { it.name == "testOwner" } }
            val owner = holder.getDeclaredField("testOwner").apply { isAccessible = true }.get(env)
            owner.javaClass.getMethod("setImplicitWaitSuppressed", Boolean::class.javaPrimitiveType).apply { isAccessible = true }.invoke(owner, noWait)
        }.onFailure { e -> if (!suppressSaid) { suppressSaid = true; println("BENCH implicit waits not suppressed: $e (${compose.javaClass.name})") } }
    }

    /** The last wait for Compose's rest gave up (see [idleScreen]): the screen's looks do not wait for it. */
    private var restless = false

    /** [block] with Compose's rest awaited [IDLE_CAP_S] seconds at most (Espresso's master policy), then 60 s again. */
    private fun <T> idleCapped(block: () -> T): T {
        androidx.test.espresso.IdlingPolicies.setMasterPolicyTimeout(IDLE_CAP_S, java.util.concurrent.TimeUnit.SECONDS)
        try {
            return block()
        } finally {
            androidx.test.espresso.IdlingPolicies.setMasterPolicyTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        }
    }

    /** The screen let come to rest: Compose's rest, or with a text field's dialog open, a few frames (bounded). */
    private fun idleScreen() {
        followTextDialogs()
        if (!textDialog) {
            // Audit method v2 (META-AUDIT Q2, A4/C2; round 4 « Effacer » 240 s = 4 waits of 60 s): Compose's rest is awaited
            // at most [IDLE_CAP_S] seconds (Espresso's master policy); past that, the bench says it is the bench's and lets
            // the screen settle by frames, never a « frozen app » for the tester.
            try {
                restless = false
                idleCapped { compose.waitForIdle() }
            } catch (e: Throwable) {
                if (e.javaClass.simpleName !in NOT_IDLE) throw e
                restless = true
                benchStalls++
                if (benchStalls == 1 || benchStalls % 10 == 0) {
                    say("Banc : l'écran ne s'est pas posé en ${IDLE_CAP_S} s sous le banc (Robolectric) ; il continue image par image. Ce n'est pas un défaut de l'appli.")
                }
                repeat(4) {
                    runCatching { compose.mainClock.advanceTimeByFrame() }
                    shadowOf(Looper.getMainLooper()).idle()
                }
            }
        } else {
            repeat(4) {
                runCatching { compose.mainClock.advanceTimeByFrame() }
                shadowOf(Looper.getMainLooper()).idle()
            }
        }
        followTextDialogs()
    }

    /** « ecris » in a dialog's field: the text put at the cursor of the focused field, as the keyboard does (no wait for rest). */
    private fun typeInDialog(text: String) {
        val node = compose.onAllNodes(isFocused()).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull() ?: error("no focused field")
        val insert = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsActions.InsertTextAtCursor)?.action ?: error("not a text field")
        compose.runOnUiThread { insert(androidx.compose.ui.text.AnnotatedString(text)) }
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** « tape » gestures played so far (each one's hand is drawn from the seat and this number). */
    private var tapes = 0

    /** Time goes by up to [at] ([SystemClock.uptimeMillis]), in steps short enough not to pass it by more than a few ms. */
    private fun passUntil(at: Long) {
        while (true) {
            val left = at - SystemClock.uptimeMillis()
            if (left <= 0) return
            pass(minOf(left, 8L))
        }
    }

    /** Time goes by, then the screen is let settle (the work off the main thread too, a rebuilt activity). */
    private fun settle(ms: Long) {
        pass(ms)
        backgroundQuiet()
        repeat(4) {
            Thread.sleep(40)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            // A failure in the screen's work stops its updates (a frozen screen): kept for the report.
            runCatching { idleScreen() }.onFailure { e -> problems += "${e.javaClass.name}: ${e.message?.take(300)}\n${e.stackTrace.take(8).joinToString("\n") { "    at $it" }}" }
        }
        vm()
        meters(lastVm)
        picker()
        services()
        flac.register()
        engineDone()
        lastVm?.let(::lightPads)
        shareMenu()
        permissions()
        toasts()
    }

    /**
     * Audit ultra round 1 (lot banc, « mesures en direct : que des tirets », E d and F c): the mixer's meters and the
     * master's loudness are the native engine's only ([RhythmViewModel.pollMeters] returns without it). While the music
     * plays the bench shows what a phone's engine would read on a mix at the streaming level: the channels that have
     * something to play move with their level, the master reads the bench's render of what the live engine was told
     * (PLAN-RESTE 5.11: no constants; the same project, the same reading); stopped, the momentary and short-term readings
     * go, the integrated one and the peak stay.
     */
    private var metersPlaying = false

    private fun meters(v: RhythmViewModel?) {
        if (v == null) return
        val playing = v.transport.value.playing
        if (playing == metersPlaying && !playing) return
        metersPlaying = playing
        val l = FloatArray(NativeEngine.LOUDNESS) { NativeEngine.LOUDNESS_NONE }
        val m = FloatArray(NativeEngine.METERS).also { it[NativeEngine.METER_LIMITER] = 1f }
        if (playing) {
            val p = v.project.value
            val on = buildSet {
                if ((p.allScenes.map { it.drums } + p.drums).any { d -> d.lanes.any { it.hits.isNotEmpty() } }) add(0)
                if (p.acid.steps.any { it.gate }) add(1)
                if (p.synthNotes.notes.isNotEmpty()) add(2)
                if (p.remix.setId != null) add(3)
                p.tracks.filter { it.notes.notes.isNotEmpty() }.forEach { add(com.pocketstudio.model.InstrumentTrack.mixerChannel(it.id)) }
                p.audioTracks.filter { it.clip != null }.forEach { add(com.pocketstudio.model.AudioTrack.mixerChannel(it.id)) }
            }
            // PLAN-RESTE 5.11 (control 12): the bench's render of what the live engine was told, measured with the app's
            // meter (it moves with the channels' levels); no reading without the bench's engine: « — » (banc).
            val r = if (NativeEngine.sink === engine && on.isNotEmpty()) engine.liveReading() else null
            val channels = p.mixer.channels
            on.filter { it in 0 until NativeEngine.MIXER_CHANNELS }.forEach { ch ->
                m[ch] = if (r == null) 0f else (0.42f * (channels.getOrNull(ch)?.volume ?: 0.8f) / 0.8f).coerceIn(0f, 1f)
            }
            if (r != null) { m[NativeEngine.MASTER] = r[4]; m[NativeEngine.METER_MASTER_IN] = r[4] }
            l[NativeEngine.LOUDNESS_MOMENTARY] = r?.get(0) ?: NativeEngine.LOUDNESS_NONE
            l[NativeEngine.LOUDNESS_SHORT_TERM] = r?.get(1) ?: NativeEngine.LOUDNESS_NONE
            l[NativeEngine.LOUDNESS_INTEGRATED] = r?.get(2) ?: NativeEngine.LOUDNESS_NONE
            l[NativeEngine.LOUDNESS_TRUE_PEAK] = r?.get(3) ?: NativeEngine.LOUDNESS_NONE
            l[NativeEngine.LOUDNESS_RECENT_PEAK] = l[NativeEngine.LOUDNESS_TRUE_PEAK]
            l[NativeEngine.LOUDNESS_DRIVE] = 0f
            l[NativeEngine.LOUDNESS_LOSS] = 0f
        } else {
            val was = v.loudness.value
            l[NativeEngine.LOUDNESS_INTEGRATED] = was.getOrElse(NativeEngine.LOUDNESS_INTEGRATED) { NativeEngine.LOUDNESS_NONE }
            l[NativeEngine.LOUDNESS_TRUE_PEAK] = was.getOrElse(NativeEngine.LOUDNESS_TRUE_PEAK) { NativeEngine.LOUDNESS_NONE }
        }
        v.showMetersForTest(m, l)
    }

    /** A Toast is on the phone's screen for a few seconds but in no screenshot: the bench says what it reads (lot « remix 1 »). */
    private fun toasts() {
        val shown = org.robolectric.shadows.ShadowToast.shownToastCount()
        if (shown <= toastsSeen) return
        org.robolectric.shadows.ShadowToast.getTextOfLatestToast()?.let { say("Un petit message s'affiche quelques secondes en bas de l'écran : « $it ».") }
        toastsSeen = shown
    }

    private var toastsSeen = 0

    /**
     * The screen [n] for the tester: its image (screen-N.png, with grey bars over the small texts for
     * `lit=non`) and its text (screen-N.txt, see [ScreenText]). The bench's own work: not a stall.
     */
    private fun capture(dir: File, n: Int): ScreenText.Snapshot {
        watchdog?.paused = true
        try {
            val shot = File(dir, "screen-$n.png")
            captureScreenRoboImage(shot.path, RoborazziOptions(taskType = RoborazziTaskType.Record, captureType = RoborazziOptions.CaptureType.Screenshot()))
            val snap = screenText().read()
            if (!options.reads) greyOut(shot, snap.small)
            val m = app.resources.displayMetrics
            File(dir, "screen-$n.txt").writeText((listOf("Écran $n (${m.widthPixels}x${m.heightPixels})") + snap.lines).joinToString("\n", postfix = "\n"))
            val routes = File(dir, "routes.txt")
            val seen = routes.takeIf { it.isFile }?.readLines().orEmpty()
            File(dir, "meta-$n.txt").writeText("route=${snap.route}\nnouvelle=${if (snap.route in seen) 0 else 1}\n")
            if (snap.route !in seen) routes.appendText(snap.route + "\n")
            return snap
        } finally {
            watchdog?.paused = false
        }
    }

    /** The sound of screen [n] (with the bench's [notes]) and the turn: the tester's script waits for it. */
    private fun publish(dir: File, n: Int, notes: List<String>): String {
        val sound = File(dir, "sound-$n.txt")
        if (!sound.isFile) sound.writeText(heard())
        if (notes.isNotEmpty()) sound.appendText("\n" + notes.joinToString("\n"))
        File(dir, "turn.tmp").writeText("$n")
        File(dir, "turn.tmp").renameTo(File(dir, "turn"))
        return sound.readText()
    }

    /** `lit=non`: the texts under 18 sp as grey bars, as a person who cannot read them sees them. */
    private fun greyOut(png: File, rects: List<androidx.compose.ui.geometry.Rect>) {
        if (rects.isEmpty()) return
        val img = javax.imageio.ImageIO.read(png) ?: return
        val g = img.createGraphics()
        g.color = java.awt.Color(0x9E, 0x9E, 0x9E)
        for (r in rects) g.fillRect(r.left.toInt(), r.top.toInt(), r.width.toInt().coerceAtLeast(1), r.height.toInt().coerceAtLeast(1))
        g.dispose()
        javax.imageio.ImageIO.write(img, "png", png)
    }

    /** The files Android's share menu was given (their names), kept with the seat's state. */
    private val shared = mutableListOf<String>()

    /** The shared file, copied into the seat's state (etat/partage/) for the mission's oracle. */
    private fun keepShared(name: String) {
        shared += name
        val dir = seatDir ?: return
        File(dir, "journal.txt").appendText("#$turnNow PARTAGE $name\n")
        sharedFile(name)?.copyTo(File(dir, "etat/partage/$name"), overwrite = true)
    }

    /** The file Android's share menu was given, by its name (the app's cache, files or external files). */
    private fun sharedFile(name: String): File? = listOf(app.cacheDir, app.filesDir, app.getExternalFilesDir(null)).filterNotNull()
        .firstNotNullOfOrNull { root -> root.walkTopDown().firstOrNull { it.isFile && it.name == name } }

    private fun sharedSize(name: String): Long? = sharedFile(name)?.length()

    /**
     * `fresh=reel`: the system's question when the app asks for a permission, as a phone shows it (in
     * French or English, as the phone's language): the answer is given to the app as Android does.
     */
    private fun permissions() {
        if (!options.fresh) return
        scenario?.onActivity { a ->
            val req = shadowOf(a).lastRequestedPermission ?: return@onActivity
            if (req === permissionAsked) return@onActivity
            permissionAsked = req
            val perms = req.requestedPermissions
            val fr = options.lang == com.pocketstudio.model.i18n.Lang.FR
            val name = a.applicationInfo.loadLabel(a.packageManager)
            val mic = perms.any { it == android.Manifest.permission.RECORD_AUDIO }
            val title = when {
                mic && fr -> "Autoriser $name à enregistrer des contenus audio ?"
                mic -> "Allow $name to record audio?"
                fr -> "Autoriser $name à vous envoyer des notifications ?"
                else -> "Allow $name to send you notifications?"
            }
            val choices = when {
                mic && fr -> arrayOf("Lorsque vous utilisez l'appli", "Uniquement cette fois-ci", "Ne pas autoriser")
                mic -> arrayOf("While using the app", "Only this time", "Don't allow")
                fr -> arrayOf("Autoriser", "Ne pas autoriser")
                else -> arrayOf("Allow", "Don't allow")
            }
            fun answer(granted: Boolean) {
                if (granted) shadowOf(app).grantPermissions(*perms) else shadowOf(app).denyPermissions(*perms)
                // Round 1 (lot banc): the microphone granted, an audio track's take starts now: on the engine's microphone.
                if (granted && mic) NativeEngine.sink = NativeEngine.sink ?: engine
                val results = IntArray(perms.size) { if (granted) android.content.pm.PackageManager.PERMISSION_GRANTED else android.content.pm.PackageManager.PERMISSION_DENIED }
                seatDir?.let { File(it, "journal.txt").appendText("#$turnNow PERMISSION ${perms.joinToString { p -> p.substringAfterLast('.') }} ${if (granted) "accordée" else "refusée"}\n") }
                @Suppress("DEPRECATION")
                a.onRequestPermissionsResult(req.requestCode, perms, results)
            }
            android.app.AlertDialog.Builder(a)
                .setTitle(title)
                .setItems(choices) { d, which -> d.dismiss(); answer(which < choices.size - 1) }
                .setOnCancelListener { answer(false) }
                .show()
        }
        shadowOf(Looper.getMainLooper()).idle()
    }

    private var permissionAsked: Any? = null

    /** What a person would hear now (the pads' own sounds are not heard here, but the Remix's parts are). */
    protected fun heard(): String = buildList {
        if (ears.playing) add("l'exemple du tuto joue (encore ${(ears.frames - ears.heard()) / OfflineExport.SAMPLE_RATE + 1} s)")
        val v = vm()
        if (v?.transport?.value?.playing == true) {
            // Audit ultra tour 0 (seat a): the song from a part (the guide's « Écouter »), not the loop of a part.
            val part = v.sessionUi.value.songIndex.takeIf { it >= 0 }?.let { v.project.value.song.getOrNull(it) }
            // Audit ultra round 4 (seats B c, C c): every Live block stopped (« ■ Tout »): the tempo goes on, nothing sounds.
            val allStopped = v.liveGrid.value?.let { g -> (0 until com.pocketstudio.model.LiveSlots.COUNT).all { g.slot(it).playing < 0 } } == true
            if (allStopped) add("le tempo tourne mais tous les blocs Live sont arrêtés : la musique du projet ne sonne plus")
            else add(part?.let { "le morceau joue (partie « ${v.project.value.scenes.getOrNull(it.scene)?.name ?: "?"} »)" } ?: "la musique du projet joue en boucle")
            remixHeard(v)?.let(::add)
            stemPads().filter { it.value.second }.forEach { (pad, s) -> add("le pad ${pad + 1} (« ${s.first} ») sonne dans la boucle") }
        }
        previewHeard()?.let(::add)
        libraryHeard()?.let(::add)
        // Round 1 (lot banc): « Écouter le bout » of the cut screen (and the stem board's « Écouter ») plays through Android's
        // player on the bench (the engine's preview player falls back to it): heard, as on a phone.
        v?.let { pieceHeard(it) }?.let(::add)
        // The pads and notes the app sent to the bench's engine (« Montre-moi », each of its gestures heard).
        v?.let { engineHitsHeard(it) }?.let(::add)
        // Plan 4.1 (P3): the challenge's pulse, what its example and its tempo light beat (the project's tempo kept between 80
        // and 120 BPM), so that a tester taps at about the right interval (« tape X Y N M » or « tape X Y N tempo »).
        // Review of 4.1: said as the ear gets it (about N BPM), not in exact milliseconds.
        challengePulse()?.let { c ->
            val example = if (challengeNow() is com.pocketstudio.app.padlive.ChallengeUi.Listening) " (exemple en cours)" else ""
            add("défi « ${c.name} »$example : pulsation ~${Math.round(c.bpm)} BPM")
        }
        // PLAN-RESTE 4.7: « Mes exports » open (sizes and dates on screen) or an export under way: its size and time are the bench's.
        if (v?.export?.value?.running == true || shownOnScreen("my_export")) add("export $EXPORTS_NOT_A_PHONES")
        // The Live pads, as the bench's engine plays them (A.16): heard with the transport stopped too.
        v?.let { livePadsHeard(it) }?.let(::add)
    }.let { if (it.isEmpty()) "Son : rien ne joue." else "Son : " + it.joinToString(" ; ") + "." }
        .let { s -> note?.let { n -> note = null; "$s\n$n" } ?: s }

    /** Pad Live's challenge as its panel shows it (null: no app yet). */
    private fun challengeNow(): com.pocketstudio.app.padlive.ChallengeUi? = runCatching { lastVm?.padLive?.challenge?.ui?.value }.getOrNull()

    /** The challenge shown on the panel (null: closed, or no « coup » pad). */
    private fun challengePulse(): com.pocketstudio.model.PadChallenge.Challenge? = when (val u = challengeNow()) {
        is com.pocketstudio.app.padlive.ChallengeUi.Ready -> u.c
        is com.pocketstudio.app.padlive.ChallengeUi.Listening -> u.c
        is com.pocketstudio.app.padlive.ChallengeUi.YourTurn -> u.c
        is com.pocketstudio.app.padlive.ChallengeUi.Done -> u.c
        else -> null
    }

    /** « Écouter le bout »: the stretch of one part of the song that plays, and how long it is. */
    private fun pieceHeard(v: RhythmViewModel): String? {
        val sel = runCatching { v.stemBoard.state.value.playing }.getOrNull() ?: return null
        val part = sel.stem.takeIf { it in Demucs.STEMS.indices }?.let { parts(listOf(it)) } ?: "une partie de la chanson"
        val seconds = sel.frames.toDouble() / OfflineExport.SAMPLE_RATE
        return "un bout de la chanson joue : $part, ${"%.1f".format(seconds).replace('.', ',')} s"
    }

    /** Pads and notes played through the bench's engine since the last screen, as the ear tells them. */
    private fun engineHitsHeard(v: RhythmViewModel): String? {
        val (lanes, notes) = engine.heardHits()
        if (lanes.isEmpty() && notes == 0) return null
        val p = v.project.value
        val pads = lanes.distinct().joinToString(", ") { "« ${com.pocketstudio.app.rhythm.padName(p, it)} »" }
        return listOfNotNull(
            pads.takeIf { lanes.isNotEmpty() }?.let { "le pad $it sonne" + if (lanes.size > 1) " (${lanes.size} coups)" else "" },
            notes.takeIf { it > 0 }?.let { "$it note${if (it > 1) "s" else ""} du synthé" },
        ).joinToString(" ; ")
    }

    /** The song of the Remix along the project: which of its parts are heard. */
    private fun remixHeard(v: RhythmViewModel): String? {
        val r = v.project.value.remix
        if (r.setId == null) return null
        if (v.stemOverview.value == null) return "la chanson « ${r.name} » du Remix ne s'entend pas encore (ses pistes se chargent)"
        val solo = r.stems.any { it.solo }
        val heard = Demucs.STEMS.indices.filter { i -> val m = r.stems[i]; (if (solo) m.solo else !m.mute) && m.volume > 0.01f }
        val from = (if (r.startBar > 0) ", depuis sa mesure ${r.startBar + 1}" else "") +
            // R2: placed later in the song (its voice on the drop), said as the ear would tell.
            (if (r.atStep > 0) ", qui entre à la mesure ${r.atStep / v.project.value.stepsPerBar.coerceAtLeast(1) + 1} du morceau" else "")
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
        // « Voix · <chanson> » (older cuts) or « Voix 2 » (R2: numbered cuts).
        val stem = Regex("^(${STEM_NAMES.joinToString("|")})( · | \\d+$)")
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

    /**
     * Where a touch aimed at [tag] lands ([middle]: the middle of its first node): the nearest point of that node's part shown on the
     * screen (cut by its parents, as boundsInWindow is) that [hand]'s thumb reaches and where the element under the finger is that
     * node (the journal's « DOIGT » line names it). Null: no such point, the touch is refused (review of rejeux6, grave 2).
     */
    private fun reachableNear(tag: String, middle: Pair<Float, Float>, hand: SeatOptions.Hand): Pair<Float, Float>? {
        val m = app.resources.displayMetrics
        val screen = screenText()
        val hits = { x: Float, y: Float -> fingerOn(tag, screen.under(x, y)) }
        if (tag.startsWith("vue_")) {
            return middle.takeIf { (x, y) -> SeatOptions.reachable(hand, x, y, m.widthPixels, m.heightPixels, m.density) && hits(x, y) }
        }
        val n = runCatching { taggedNodes(tag).fetchSemanticsNodes().firstOrNull() }.getOrNull() ?: return null
        val p = BenchWindows.drawnPosition(n, m)
        val shown = BenchWindows.drawnBounds(n, m)
        return SeatOptions.nearestReachable(
            hand, SeatOptions.Box(p.x, p.y, p.x + n.size.width, p.y + n.size.height), SeatOptions.Box(shown.left, shown.top, shown.right, shown.bottom),
            middle, m.widthPixels, m.heightPixels, m.density, hits,
        )
    }

    /**
     * The finger ([under], a « DOIGT » line: « <tag> « words » », « dans:<tag> » for a part of it, « désactivé … ») is on [tag]
     * (« prefix* »: on one of that family; « vue_X »: the Android view showing X). As scripts/bench/rejeu-defauts.sh reads it.
     */
    private fun fingerOn(tag: String, under: String): Boolean {
        if (tag.startsWith("vue_")) return under.startsWith("vue « ${tag.removePrefix("vue_")} »")
        val name = under.removePrefix("désactivé ").removePrefix("dans:").substringBefore(' ')
        return if (tag.endsWith("*")) name.startsWith(tag.dropLast(1)) else name == tag
    }

    /**
     * The nodes of a replay's [tag]: the tag itself, or « prefix* » for the first of a family whose ids change at every run
     * (« project_p-* »: the projects' ids are random).
     */
    private fun taggedNodes(tag: String) =
        if (tag.endsWith("*")) {
            val prefix = tag.dropLast(1)
            compose.onAllNodes(SemanticsMatcher("test tag starts with $prefix") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true })
        } else compose.onAllNodesWithTag(tag)

    protected fun centerOf(tag: String): Pair<Float, Float>? = (if (tag.startsWith("vue_")) screenText().viewCenter(tag.removePrefix("vue_")) else null) ?: runCatching {
        taggedNodes(tag).fetchSemanticsNodes().firstOrNull()?.let { n ->
            val at = BenchWindows.drawnPosition(n, app.resources.displayMetrics)
            at.x + n.size.width / 2f to at.y + n.size.height / 2f
        }
    }.getOrNull()

    /** The node showing [text] (the first one), its middle on the screen as drawn, or null. */
    protected fun centerOfText(text: String, substring: Boolean = false): Pair<Float, Float>? = runCatching {
        compose.onAllNodes(androidx.compose.ui.test.hasText(text, substring = substring)).fetchSemanticsNodes().firstOrNull()?.let { n ->
            val at = BenchWindows.drawnPosition(n, app.resources.displayMetrics)
            at.x + n.size.width / 2f to at.y + n.size.height / 2f
        }
    }.getOrNull()

    protected fun project() = lastVm?.project?.value
    protected fun rhythm() = lastVm

    /** What the Remix's song is waiting for, for a failure's message. */
    protected fun remixState(): String = lastVm?.let { v ->
        "engine=${NativeEngine.sink != null} setId=${v.project.value.remix.setId} overview=${v.stemOverview.value != null} " +
            "loading=${v.remixLoading.value} stretching=${v.remixStretching.value} remixPage=${remixPage()} " +
            "lifecycle=${scenario?.state} waves=${runCatching { compose.onAllNodesWithTag("waveform_3", useUnmergedTree = true).fetchSemanticsNodes().size }.let { r -> r.getOrNull() ?: r.exceptionOrNull()?.let { "${it.javaClass.simpleName}: ${it.message?.take(200)}" } }} " +
            "problems=${problems.size}${problems.lastOrNull()?.let { ": " + it.take(400) }.orEmpty()}"
    } ?: "no view model"
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
        /** Socle du banc, point 3: the gestures that may name a test tag instead of pixels (« touche <étiquette> »). */
        val TAG_GESTURES = setOf("touche", "appui-long", "double-touche")

        /** PLAN-RESTE 5.10: the bench's own exceptions a seat lives through before it ends « banc ». */
        const val BENCH_EXCEPTIONS_MAX = 4
        /** PLAN-RESTE 5.3: a gesture in a window with a text field longer than this (real seconds) is said the bench's. */
        const val DIALOG_SLOW_S = 30

        /** A phone like the owner's (Honor), at 1.5 px a dp: images small enough to read quickly. */
        const val PHONE = "w392dp-h852dp-hdpi"
        /** The bench's end waits this long at most for the app's network work (PackStore's catalogue: 10 s to connect, 15 s a read); past it, the detector names what is left. */
        const val NETWORK_END_CAP_MS = 30_000L

        /**
         * PLAN-RESTE 4.7 (T5-Cc-41b, round 5 seat C c: « 1 KB »; the phone: 42 Mo for 2 min 26): the bench's encoder is a shadow
         * that copies what it is given and its engine a stand-in: an export's size and the time it took are not a phone's. Said
         * wherever the bench shows one, so a tester never reports them as the app's.
         */
        const val EXPORTS_NOT_A_PHONES = "(banc : taille et durée d'un export non représentatives, encodeur et moteur du banc ; un téléphone fait bien plus gros)"

        /** What the bench says once a file is sent from Android's share menu ([files]: name and bytes, -1 unknown; [app]: the app chosen). */
        fun shareNote(files: List<Pair<String, Long>>, app: String): String {
            val named = files.joinToString(", ") { (name, bytes) -> if (bytes >= 0) "$name, ${"%.1f".format(bytes / 1e3).replace('.', ',')} Ko" else name }
            return "Ton morceau « ${named.ifEmpty { "?" }} » part avec $app (ici rien ne part vraiment : c'est la fin du partage) $EXPORTS_NOT_A_PHONES."
        }

        /** « tape »: how far a touch may land from its place (ms), a hand keeping time (review of plan 4.1). */
        const val TAPE_JITTER_MS = 30L

        const val AMENEE_SONG = "premier-morceau"
        const val AMENEE_REMIX = "remix"
        const val AMENEE_PADLIVE = "padlive"

        /**
         * Audit method v2 (META-AUDIT change 2): the bench's engine stays from the first gesture to the last, as a phone's.
         * `-Phuman.moteur=liste` brings back the old lending by lists of tags (way back, docs/DECISIONS.md 2026-11-06).
         */
        /** Compose's rest awaited this long at most (Espresso's default, 60 s, made 4 × 60 s frozen gestures in round 4). */
        const val IDLE_CAP_S = 8L
        /** The real time one « attends » may give the bench's slow stand-ins (banc fiable 1.4). */
        const val SLOW_WORK_CAP_MS = 120_000L
        /** PLAN-RESTE 4.4: « attends » gives the hand back once nothing of the bench's slow work moved for this long. */
        const val STILL_MS = 15_000L
        /** PLAN-RESTE 4.6: a blank first screen waited for this long at most, and only while the bench's workers are busy. */
        const val FIRST_SCREEN_CAP_MS = 30_000L
        private val NOT_IDLE = setOf("AppNotIdleException", "ComposeNotIdleException", "IdlingResourceTimeoutException")

        internal val ENGINE_ALWAYS: Boolean get() = System.getProperty("ps.human.moteur", "toujours") != "liste"

        /** Touches that play the song from a part, from its start or from a take (round 3), by their tags' start. */
        private val SONG_STARTS = listOf("part_play_", "play_from_", "audio_play_from_")

        /** « Tout couper » of Pad Live, the Live page's pads and their full screen (RhythmViewModel.stopLive). */

        /** See [needsTheEngine]. */
        private val ENGINE_TAGS = listOf("export", "video_create", "padlive_perf_listen", "live_take_listen", "tuto_show")

        /** A stem's name on a pad (DefaultNames.stemPad, kept in French whatever the language). */
        private val STEM_NAMES = listOf("Batterie", "Basse", "Reste", "Voix")
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
