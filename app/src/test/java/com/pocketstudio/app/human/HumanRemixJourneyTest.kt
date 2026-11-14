package com.pocketstudio.app.human

import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.remix.SeparationJob
import com.pocketstudio.app.remix.SeparationState
import com.pocketstudio.app.remix.StemStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.sqrt

/**
 * The Remix on the « testeur humain » bench (owner, 2026-10-28), walked as seats E and F will:
 * only touches on the screen's pixels, and what the sound line says. A new project, the Remix
 * page, « Choisir une chanson » (the bench's picker gives « Ma chanson préférée »), the model's
 * download and the separation with their progress, the four parts; the voice alone, then the voice
 * in the project (the song played along, voice solo) and on a pad, heard with the project's loop.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanRemixJourneyTest : HumanTester("remix-journey") {
    // The download and the separation twice as fast as the testers see them (the test stays short).
    override val remixTimeScale = 0.5

    private fun tap(where: Pair<Float, Float>?, what: String): String {
        assertNotNull("nothing to touch: $what", where)
        return act("touche ${where!!.first} ${where.second}")
    }

    /** Brings the node tagged [tag] into the screen's middle part (a swipe up at a time), then its middle. */
    private fun reach(tag: String): Pair<Float, Float> {
        // Its list scrolled to it first, as a swipe on the list would (round 1 integration: the thumb's bar, n° 21, raised
        // the screen's middle onto the sticky « Choisir une chanson », the swipes started there and nothing moved).
        runCatching { compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag))[0].performScrollTo() }
        act("attends 0.5")
        repeat(8) {
            val at = centerOf(tag) ?: error("no $tag")
            // Above the bars at the bottom (the Guide's, the thumb's, the tabs).
            if (at.second in 320f..(screenHeight - 380f)) return at
            val dy = (at.second - screenHeight / 2f).coerceIn(-500f, 500f)
            // On the page's margin (not on a slider or a waveform), then the scroll comes to rest.
            act("glisse 10 ${screenHeight / 2f + dy / 2} 10 ${screenHeight / 2f - dy / 2}")
            act("attends 1")
        }
        return centerOf(tag)!!
    }

    /**
     * Waits (« attends 1 » at a time) until [done] holds: on the real condition, not a count of
     * seconds, so a loaded machine only makes it slower. Five minutes only stop a test that hangs (51 s alone, past two minutes under a full run).
     */
    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 300_000
        while (!done()) {
            if (System.currentTimeMillis() >= until) {
                // What the screen showed then, for the failure (never committed: under build/).
                runCatching { shot(File("build/remix-journey-timeout.png")) }
                assertTrue("$what never came (${remixState()})", false)
            }
            act("attends 1")
        }
    }

    @Test
    fun aSongsVoiceGoesIntoTheProjectWithTouchesOnly() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        begin()
        // A new project, as a person who knows what they want: Partir de zéro, then Pro.
        tap(centerOfText("Partir de zéro"), "Partir de zéro")
        tap(centerOf("mode_card_pro"), "Pro")
        // The Remix, from the sounds' strip.
        tap(centerOfText("Remix"), "the Remix chip")
        assertNotNull("the Remix page is not there", centerOf("remix_pick"))

        // « Choisir une chanson »: the phone's picker gives the song of the test.
        // Each state the separation goes through, read every 5 ms in real time (the work runs off the main thread: a
        // reading between two touches only, after the screen settled, could miss a short one on a loaded machine).
        val states = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val sampling = java.util.concurrent.atomic.AtomicBoolean(true)
        val sampler = Thread {
            while (sampling.get()) {
                when (SeparationJob.state.value) {
                    is SeparationState.DownloadingModel -> states += "download"
                    is SeparationState.Decoding -> states += "decode"
                    is SeparationState.Separating -> states += "separate"
                    is SeparationState.Analysing -> states += "analyse"
                    else -> Unit
                }
                Thread.sleep(5)
            }
        }.apply { isDaemon = true; start() }
        // Audit ultra round 2, n° 31: no model yet, its download (size, Wi-Fi) is asked before the phone's picker.
        tap(centerOf("remix_pick"), "Choisir une chanson")
        val picked = tap(centerOf("remix_model_ask_ok"), "Télécharger, puis choisir la chanson")
        assertTrue(picked, picked.contains("« Ma chanson préférée.wav »"))

        // The model comes down, the song is read and separated: each state is on the screen.
        val seen = LinkedHashSet<String>()
        val until = System.currentTimeMillis() + 300_000
        while (SeparationJob.state.value.busy || SeparationJob.state.value == SeparationState.Idle && seen.isEmpty()) {
            assertTrue("the separation never ended", System.currentTimeMillis() < until)
            if (states.isNotEmpty()) seen += states
            if (centerOf("remix_progress") != null) seen += "progress shown"
            act("attends 1")
        }
        sampling.set(false)
        sampler.join()
        seen += states
        assertTrue("no progress: $seen", seen.containsAll(listOf("download", "separate", "progress shown")))
        val done = SeparationJob.state.value
        assertTrue("$done", done is SeparationState.Done)
        val set = (done as SeparationState.Done).set
        assertEquals("Ma chanson préférée", set.name)
        assertEquals(1, separationRuns)

        // The « Voix » stem is the sung voice, clean: the voice alone, nothing of the others.
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val stems = StemStore.list(app).single()
        val voice = FloatWav.read(stems.stem("vocals")).data
        val expected = BenchSong.part(BenchSong.Part.Vocals, 44_100)
        assertEquals(expected.size, voice.size)
        assertTrue("the voice stem is not the voice", (0 until voice.size step 97).all { kotlin.math.abs(voice[it] - expected[it]) < 1e-5f })
        val silentGap = rms(voice, 44_100, 7.2, 8.8)
        val singing = rms(voice, 44_100, 4.5, 6.5)
        assertTrue("the voice is not heard where it sings ($singing)", singing > 0.02)
        assertEquals("the voice is heard between its phrases", 0.0, silentGap, 1e-6)

        // « Voix » alone, as a person listens before choosing.
        val heardVoice = tap(reach("stem_${set.id}_vocals"), "the Voix switch")
        assertTrue(heardVoice, heardVoice.contains("le Remix fait écouter « Ma chanson préférée » : la voix seule") && heardVoice.contains(" — la voix "))
        // Remix lot 1 (Sarah, e/80): the project's ▶ stops « Écouter », never two voices at once.
        val both = tap(centerOf("thumb_play"), "▶ of the project")
        assertTrue(both + "\n" + screenLines().joinToString("\n"), both.contains("la musique du projet") && !both.contains("le Remix fait écouter"))
        tap(centerOf("thumb_play"), "■ of the project")

        // « Jouer avec le projet »: the song plays along the project.
        tap(reach("use_${set.id}"), "Jouer avec le projet")
        // Step 5c: « Mettre … dans mon projet », its choices as they are (the parts read first).
        waitFor("the song in the project") {
            if (project()?.remix?.setId == null) centerOf("import_go")?.let { tap(it, "Mettre dans mon projet") }
            project()?.remix?.setId != null
        }
        assertEquals(set.id, project()?.remix?.setId)
        // Tour 6 (lot coquille3, A6): a project with no song made as long as the song says so in a window; read, then
        // « Compris », as a person does (the window is in front of the page).
        centerOf("notice_ok")?.let { tap(it, "Compris") }
        // Its four parts are drawn (the screen follows: nothing froze it). Their waves are read off the main
        // thread: on a loaded machine (the whole suite at once) it takes what it takes, a person waits for them.
        waitFor("the waveforms") { centerOf("waveform_3") != null }
        // The voice alone (S), then ▶.
        tap(reach("stem_solo_3"), "S of Voix")
        val playing = tap(reach("remix_play"), "Jouer")
        // Audit ultra tour 0: the Remix page's ▶ plays the song from its part, and the ear says so (not « en boucle »).
        assertTrue(playing, playing.contains("le morceau joue (partie « A »)") && playing.contains("avec la voix seule de « Ma chanson préférée » (solo)"))
        assertNotNull("the button does not say Arrêter", centerOfText("Arrêter", substring = true))

        // Stopped (■), its first phrase onto a pad: from bar 3, two bars.
        tap(reach("remix_play"), "Arrêter")
        tap(reach("remix_bar_next"), "+")
        tap(reach("remix_bar_next"), "+")
        tap(reach("remix_pad_bars_2"), "2 mes.")
        // Round 3, zone 1, n° 4: every drum pad holds a kit sound: the pads are shown first, the person picks the free one.
        val proposed = com.pocketstudio.model.RemixVoice.padFor(project()!!, rhythm()!!.transport.value.selectedPad) ?: com.pocketstudio.model.RemixVoice.SPARE_FROM
        tap(reach("stem_pad_3"), "→ Pad of Voix")
        waitFor("the pads to pick from") { centerOf("pad_pick_$proposed") != null }
        val padded = tap(centerOf("pad_pick_$proposed")!!, "Pad ${proposed + 1}")
        // The phrase is cut and written off the main thread: waited for as a person waits for the pad to light up.
        // Round 1 (n° 2): on a free pad from 9 on, never over one of the kit's first 8 unasked.
        fun voicePad() = project()?.drums?.lanes?.indexOfFirst { it.sample?.name == "Voix 1" } ?: -1
        waitFor("the voice on a pad") { voicePad() >= 0 && rhythm()?.sampleTask?.value !is com.pocketstudio.app.rhythm.SampleTask.Busy }
        val at = voicePad()
        val lane = project()!!.drums.lanes[at]
        assertTrue("${project()!!.drums.lanes.map { it.sample?.name }} ${rhythm()?.sampleTask?.value} sel=${rhythm()?.transport?.value?.selectedPad}", at >= com.pocketstudio.model.RemixVoice.SPARE_FROM)
        assertTrue(padded, padded.contains("Le pad ${at + 1} reçoit « Voix 1 »"))
        // Two bars from bar 3: the first phrase, sung (4 s at 120 BPM).
        val file = app.filesDir.walk().first { it.name == lane.sample!!.file }
        val pad = FloatWav.read(file)
        assertEquals(4.0, pad.data.size / 2.0 / pad.sampleRate, 0.05)
        assertTrue("the pad is silent", rms(pad.data, pad.sampleRate, 0.2, 2.8) > 0.02)
        // The pad in the beat: two steps on the Batterie page, then ▶ back on the Remix page.
        tap(centerOfText("16 pads", substring = true), "the Batterie chip")
        if (rhythm()?.transport?.value?.selectedPad != at) tap(reach("pad_$at"), "the voice's pad")
        tap(reach("step_0"), "step 1")
        tap(reach("step_8"), "step 9")
        assertTrue("no hits on pad ${at + 1}", project()!!.drums.lanes[at].hits.size == 2)
        // Step 5a: the chip now bears the song's name (« Ma chanson préférée · Chanson »).
        // Past the screen's edge with a long name: the chips are swiped aside first, as a person does.
        val width = app.resources.displayMetrics.widthPixels
        centerOf("sons_Remix")?.let { (x, y) -> if (x > width - 80) { act("glisse ${width * 0.8f} $y ${width * 0.2f} $y"); act("attends 1") } }
        tap(centerOf("sons_Remix"), "the song's chip")
        // The song is in the project's structure now (step 5a): « Boucler » keeps the part going round, as before.
        tap(centerOf("top_loop_part"), "Boucler")
        val loop = tap(reach("remix_play"), "Jouer")
        assertTrue(loop, loop.contains("le pad ${at + 1} (« Voix 1 ») sonne dans la boucle"))
        assertTrue(loop, loop.contains("avec la voix seule de « Ma chanson préférée », depuis sa mesure 3 (solo)"))
        // Leaving the page while it plays (the song's chip swiped the chips aside: Structure, at the bottom): the loop
        // goes on, the screen still follows.
        val away = tap(centerOf("tab_Mix"), "Mix")
        assertTrue(away, away.contains("la musique du projet joue en boucle"))
        assertNotNull(centerOf("mixer"))
        assertTrue("the screen failed: $problems", problems.isEmpty())
        shot(File(System.getProperty("java.io.tmpdir"), "remix-journey-end.png"))

        // Audit ultra tour 0 (seat b): the stem board's work goes on with the engine when its page is left at once,
        // as a phone's engine never goes away (the bench let its engine go: « Voix vers le pad… » forever, then a crash).
        tap(centerOf("tab_Sons"), "Sons")
        // Stopped from the Remix page itself (its own control keeps the bench's engine).
        tap(reach("remix_play"), "Arrêter")
        waitFor("the song's waveforms") { rhythm()!!.stemOverview.value != null }
        val opened = tap(reach("remix_open_board"), "Ouvrir le tableau des pistes")
        if (centerOf("board") == null) act("attends 1")
        if (centerOf("board") == null) runCatching { shot(File("build/remix-journey-board.png")) }
        assertNotNull("the stem board is not there: $opened ${remixState()} playing=${rhythm()!!.transport.value.playing}", centerOf("board"))
        val vm = rhythm()!!
        fun boardDone(what: String) = waitFor(what) { vm.stemBoard.state.value.busy == null }
        fun pickVoice() {
            val (x, y) = reach("board_track_3")
            act("glisse ${x - 120f} $y ${x + 120f} $y")
            act("attends 1")
            assertNotNull("no stretch chosen", centerOf("board_to_pad"))
        }
        fun backToBoard() {
            tap(centerOf("tab_Sons"), "Sons")
            centerOf("sons_Remix")?.let { tap(it, "the song's chip") }
            if (centerOf("board") == null) tap(reach("remix_open_board"), "Ouvrir le tableau des pistes")
        }
        val padsBefore = project()!!.drums.lanes.count { it.sample != null }
        val livePadsBefore = project()!!.live.clips.count { it != null }
        pickVoice()
        tap(reach("board_to_pad"), "→ Pad")
        tap(centerOf("tab_Mix"), "Mix, at once")
        boardDone("the stretch on a pad")
        // Round 3, zone 1, n° 4: on the first empty pad, a live pad: no drum sound covered.
        assertTrue("${vm.stemBoard.state.value.message}", project()!!.live.clips.count { it != null } == livePadsBefore + 1)
        assertEquals(padsBefore, project()!!.drums.lanes.count { it.sample != null })
        backToBoard()
        tap(reach("board_chop"), "Découper la voix en phrases")
        tap(centerOf("tab_Mix"), "Mix, at once")
        boardDone("the phrases")
        assertTrue("${vm.stemBoard.state.value.message}", project()!!.drums.lanes.count { it.sample != null } > padsBefore)
        backToBoard()
        pickVoice()
        tap(reach("board_to_live"), "→ Live")
        tap(centerOf("tab_Mix"), "Mix, at once")
        boardDone("the stretch in a Live cell")
        val live = vm.stemBoard.state.value.message
        assertTrue("$live", live != null && !live.contains("Ça n'a pas marché"))
        assertTrue("the screen failed: $problems", problems.isEmpty())
        // As it was: the board closed, the loop played again, the page left.
        tap(centerOf("tab_Sons"), "Sons")
        centerOf("sons_Remix")?.let { tap(it, "the song's chip") }
        centerOf("board_close")?.let { tap(it, "close the board") }
        tap(reach("remix_play"), "Jouer")
        tap(centerOf("tab_Mix"), "Mix")

        // Step 5b (Benjamin): played without « Boucler », the song goes on when the page is left too.
        tap(centerOf("tab_Sons"), "Sons")
        tap(reach("remix_play"), "Arrêter")
        tap(centerOf("top_loop_part"), "Boucler (off)")
        val once = tap(reach("remix_play"), "Jouer")
        // The song from its part (audit ultra tour 0: said as the song, not as a loop).
        assertTrue(once, once.contains("le morceau joue (partie"))
        val left = tap(centerOf("tab_Mix"), "Mix")
        assertTrue(left, (left.contains("le morceau joue") || left.contains("la musique du projet joue")) && left.contains("la voix seule de « Ma chanson préférée »"))
    }

    private fun rms(stereo: FloatArray, rate: Int, from: Double, to: Double): Double {
        val a = (from * rate).toInt() * 2
        val b = (to * rate).toInt() * 2
        var sum = 0.0
        for (i in a until b) sum += stereo[i].toDouble() * stereo[i]
        return sqrt(sum / (b - a))
    }
}
