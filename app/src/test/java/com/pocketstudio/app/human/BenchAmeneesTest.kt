package com.pocketstudio.app.human

import com.pocketstudio.app.human.HumanTester.Companion.ENGINE_ALWAYS
import com.pocketstudio.app.rhythm.SampleTask
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.Note
import com.pocketstudio.model.NoteClip
import com.pocketstudio.model.TrackKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The scripted starts of the three most frequent missions (Audit ultra §3.1, « amenées », lot P0-A):
 * from the app as just installed to the mission's starting point, as a seat gets it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchAmeneesTest : HumanTester("amenees") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun bring(name: String, o: SeatOptions): List<String> {
        begin(o.copy(amenee = name))
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        amenee(name, journal)
        return journal.readLines()
    }

    @Test
    fun firstSongStartsInAnEmptyProjectInSimpleMode() {
        val j = bring(AMENEE_SONG, SeatOptions(simple = true))
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertTrue("Mode simple", SettingsStore.state.value.simpleMode)
        assertNotNull("a project is open", project())
        assertTrue(screenLines().joinToString("\n"), screenLines().any { it.startsWith("onglet") })
    }

    @Test
    fun remixStartsOnTheRemixPage() {
        val j = bring(AMENEE_REMIX, SeatOptions())
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertNotNull("« Choisir une chanson »", centerOf("remix_pick"))
    }

    /** From the welcome's « Défis » card to the challenge's panel; challenge 1 as the app makes it on the page shown. */
    private fun openChallenge(): com.pocketstudio.model.PadChallenge.Challenge {
        begin(SeatOptions(simple = true))
        // The welcome's « Défis » card, lower down on a phone: scrolled to as a person does.
        repeat(4) { if ((centerOf("launch_challenges")?.second ?: Float.MAX_VALUE) > 1100f) act("glisse 294 900 294 350") }
        val home = centerOf("launch_challenges") ?: error("no « Défis » on the welcome\n" + screenLines().joinToString("\n"))
        act("touche ${home.first} ${home.second}")
        val until = System.currentTimeMillis() + 60_000
        while (centerOf("padlive_challenge_go") == null) {
            assertTrue("no « À moi ! »\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
            act("attends 1")
        }
        val p = project()!!
        val c = com.pocketstudio.model.PadChallenge.make(1, com.pocketstudio.model.PadChallenge.shotsOf(p.live, p.padLive.shownPage), p.tempoBpm)!!
        assertTrue("challenge 1 is one pad", c.hits.map { it.pad }.distinct().size == 1)
        return c
    }

    private fun stars(c: com.pocketstudio.model.PadChallenge.Challenge, what: String): Int {
        val stars = project()!!.padLive.stars.getOrElse(0) { 0 }
        val log = com.pocketstudio.app.report.AppLog.dump(20).lines().filter { "Défi" in it }.joinToString("\n")
        println("$what: $stars star(s) (bpm ${c.bpm}, every ${c.beatMs.toLong()} ms)\n$log")
        return stars
    }

    /** The pulse the sound line states while a challenge is open (« pulsation ~N BPM »), in BPM. */
    private fun pulseOf(sound: String): Long {
        val pulse = Regex("pulsation ~(\\d+) BPM").find(sound)
        assertNotNull("the sound line states the challenge's pulse\n$sound", pulse)
        return pulse!!.groupValues[1].toLong()
    }

    /** Taps challenge [c]'s pad [n] times every [everyMs] (the bench's hand: ± HumanTester.TAPE_JITTER_MS). */
    private fun tapPad(c: com.pocketstudio.model.PadChallenge.Challenge, every: String) {
        val pad = centerOf("padlive_pad_${c.hits.first().pad}") ?: error("pad ${c.hits.first().pad} not on screen\n" + screenLines().joinToString("\n"))
        act("tape ${pad.first} ${pad.second} ${c.hits.size} $every")
    }

    /**
     * Banc fiable 3.3 (P3; round 5 C a #32: no star in 3 tries), plan 4.1: the tester taps « Boum boum » at the pulse the
     * sound line states (« pulsation ~N BPM », as the ear gets it), after « À moi ! », with a hand's error (±30 ms): 3 stars,
     * as the app gives them (ChallengeControllerTest). Project at 120 BPM: 500 ms.
     */
    @Test
    fun aTappedRhythmIsScored() {
        val c = openChallenge()
        val go = centerOf("padlive_challenge_go")!!
        val sound = act("touche ${go.first} ${go.second}")
        val bpm = pulseOf(sound)
        assertEquals(sound, Math.round(c.bpm), bpm)
        tapPad(c, "${Math.round(60_000.0 / bpm)}")
        act("attends 1")
        assertEquals("challenge 1 tapped at its pulse after « À moi ! »", 3, stars(c, "at the pulse"))
    }

    /**
     * Plan 4.1, cause (d) on the bench: the project at 90 BPM, the challenge's pulse is 90 (667 ms), not the 500 ms the bench
     * used to tap: read from the sound line, tapped with « tempo » after « À moi ! »: 3 stars.
     */
    @Test
    fun aTappedRhythmAtNinetyBpmIsScored() {
        openChallenge()
        rhythm()!!.setTempo(90.0)
        act("attends 1")
        // The challenge made again at the project's new tempo, as its level's button does.
        val level = centerOf("padlive_challenge_level_1") ?: error("no level 1\n" + screenLines().joinToString("\n"))
        act("touche ${level.first} ${level.second}")
        val p = project()!!
        assertEquals(90.0, p.tempoBpm, 0.0)
        val c = com.pocketstudio.model.PadChallenge.make(1, com.pocketstudio.model.PadChallenge.shotsOf(p.live, p.padLive.shownPage), p.tempoBpm)!!
        assertEquals(90.0, c.bpm, 0.0)
        val go = centerOf("padlive_challenge_go") ?: error("no « À moi ! »\n" + screenLines().joinToString("\n"))
        val sound = act("touche ${go.first} ${go.second}")
        assertEquals(sound, 90L, pulseOf(sound))
        tapPad(c, "tempo")
        act("attends 1")
        assertEquals("challenge 1 at 90 BPM tapped at its pulse", 3, stars(c, "at 90 BPM"))
    }

    /**
     * Plan 4.1, cause (c') on the bench (review: the bench no longer waits for the tester): « tape » right after « Écouter »
     * taps during the example, the sound line saying « exemple en cours »: the app counts none of those touches (by design,
     * and T5-DEFI-ECOUTE at its edge). Once « À toi ! » shows, the same « tape » gives 3 stars.
     */
    @Test
    fun aTapDuringTheExampleIsNotCounted() {
        val c = openChallenge()
        val listen = centerOf("padlive_challenge_listen") ?: error("no « Écouter »\n" + screenLines().joinToString("\n"))
        val ctl = rhythm()!!.padLive.challenge
        // The example is rendered off the main thread in REAL time while the bench moves its clock: it starts anywhere from the
        // touch on « Écouter » to the end of that touch's settle. Pinned here to its earliest start (a worker that is done at
        // once), the case the self-test met 1 time in 7 (YourTurn(c, 2): the last taps landed after the example).
        ctl.exampleForTest = { ch -> com.pocketstudio.app.audio.Pcm(FloatArray(com.pocketstudio.model.PadChallenge.exampleFrames(ch, 48_000) * 2), 2, 48_000) }
        val sound = act("touche ${listen.first} ${listen.second}")
        assertTrue("the example plays: ${ctl.ui.value}", ctl.ui.value is com.pocketstudio.app.padlive.ChallengeUi.Listening)
        assertTrue(sound, "exemple en cours" in sound)
        tapPad(c, "tempo")
        val until = System.currentTimeMillis() + 30_000
        while (ctl.ui.value is com.pocketstudio.app.padlive.ChallengeUi.Listening) {
            assertTrue("the example never ends", System.currentTimeMillis() < until)
            act("attends 1")
        }
        assertEquals("touches during the example are not counted", com.pocketstudio.app.padlive.ChallengeUi.YourTurn(c, 0), ctl.ui.value)
        assertEquals(0, stars(c, "during the example"))
        tapPad(c, "tempo")
        act("attends 1")
        assertEquals("challenge 1 tapped at « tempo » once it is the player's turn", 3, stars(c, "after the example"))
    }

    /**
     * Banc fiable 3.3 (P1; round 5 C d: « Importer un projet (.zip) » out of reach, the picker gave nothing outside the Remix):
     * the picker answers a project's .zip (made here from the open project), and the import adds a project.
     */
    @Test
    fun aZipImportedOutsideRemixAddsAProject() {
        bring(AMENEE_SONG, SeatOptions(simple = false))
        val projects = java.io.File(androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>().filesDir, "projects")
        fun count() = projects.listFiles()?.count { it.isDirectory || it.name.endsWith(".json") } ?: 0
        act("attends 2")
        val before = count()
        fun tap(tag: String) {
            val at = centerOf(tag) ?: error("nothing to touch: $tag\n" + screenLines().joinToString("\n"))
            act("touche ${at.first} ${at.second}")
        }
        tap("top_projects")
        tap("project_new")
        // « ▸ Autres façons de commencer » unfolds the import (round 5 C d looked for it there).
        centerOfText("Autres façons de commencer", substring = true)?.let { act("touche ${it.first} ${it.second}") }
        repeat(3) { if ((centerOf("starter_import")?.second ?: Float.MAX_VALUE) > 1100f) act("glisse 294 900 294 350") }
        val at = centerOf("starter_import") ?: error("no « Importer un projet »\n" + screenLines().joinToString("\n"))
        val said = act("touche ${at.first} ${at.second}")
        act("attends 3")
        assertTrue("the picker gave a .zip: $said", "Le-morceau-de-Sam.pocketstudio.zip" in said)
        assertTrue("a project more: $before → ${count()}\n" + screenLines().joinToString("\n"), count() > before)
    }

    @Test
    fun padLiveStartsWithItsPadsReady() {
        val j = bring(AMENEE_PADLIVE, SeatOptions())
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertNotNull(centerOf("padlive"))
        assertNotNull("the ready-to-play pads", project()?.live?.at(4))
        assertEquals(null, centerOf("padlive_busy"))
        // Pad Live covers the project: a person sees its pads, not the drum pads under it.
        val lines = screenLines()
        assertEquals("padlive", screenRoute().substringBefore('/'))
        assertTrue(lines.joinToString("\n"), lines.any { "Klaxon" in it } && lines.none { "Tom médium" in it })
    }

    /** The sound line says Live pad [pad] (0-based) plays in a loop, alone or with others. */
    private fun loopHeard(line: String, pad: Int): Boolean =
        "le pad live ${pad + 1} joue en boucle" in line || Regex("les pads live [0-9, ]*\\b${pad + 1}\\b[0-9, ]* jouent en boucle").containsMatchIn(line)

    /**
     * Audit ultra tour 0 (seat c): on a phone the engine lights a pad that plays, ■ silences the pads, the
     * performance take starts on its line and « Enregistrer ce que je joue » records; the bench had none of it
     * (« Prêt » for good, « Pas assez de mémoire », a dark pad, loops going on after ■).
     */
    @Test
    fun padLiveLightsStopsAndRecordsAsOnAPhone() {
        padsAsOnASeat = true
        bring(AMENEE_PADLIVE, SeatOptions())
        val vm = rhythm()!!
        fun tap(tag: String): String = act("touche ${centerOf(tag)!!.first} ${centerOf(tag)!!.second}")
        fun waitFor(what: String, done: () -> Boolean) {
            val until = System.currentTimeMillis() + 60_000
            while (!done()) {
                assertTrue("$what never came", System.currentTimeMillis() < until)
                act("attends 0.5")
            }
        }
        val loop = (0 until 16).first { project()!!.live.at(it)?.oneShot == false }
        // A.16 (G2): the touch reaches the engine, which plays the loop on its grid (at once if nothing else plays, else on
        // its next line: the pad Pad Live may start as it opens).
        benchEngine.padCommandsHeard()
        var heard = tap("padlive_pad_$loop")
        assertTrue("the touch went to the engine", "toggleClip $loop" in benchEngine.padCommandsHeard())
        waitFor("the loop playing") { vm.liveStates.value[loop] == 2 || false.also { heard = act("attends 0.5") } }
        if ("joue en boucle" !in heard) heard = act("attends 0.1")
        assertTrue(heard, loopHeard(heard, loop))
        assertEquals("the pad that plays is lit", 2, vm.liveStates.value[loop])
        tap("padlive_play")
        assertTrue(vm.transport.value.playing)
        val stopped = tap("padlive_play")
        assertTrue("■ reached the engine as « every pad off »", "toggleClip -1" in benchEngine.padCommandsHeard())
        assertTrue("■ silences the pads: $stopped", "pad live" !in stopped && "pads live" !in stopped)
        assertEquals(0, vm.liveStates.value[loop])

        // « Enregistrer la performance »: armed, then recording on its line, then written into the song.
        tap("live_record")
        tap("padlive_pad_$loop")
        waitFor("the take recording") { vm.take.value is com.pocketstudio.app.live.TakeUi.Recording }
        act("attends 2")
        tap("live_record")
        waitFor("the take written") { vm.take.value is com.pocketstudio.app.live.TakeUi.Done }
        assertTrue("the song got the take: ${project()!!.song.size}", project()!!.song.isNotEmpty())
        if (vm.transport.value.playing) tap("padlive_play")

        // « Enregistrer ce que je joue »: a pad on, recorded, ended on a bar line.
        tap("padlive_pad_$loop")
        tap("padlive_played")
        assertEquals(null, vm.padLive.played.message.value)
        waitFor("the played take recording") { vm.padLive.played.ui.value is com.pocketstudio.app.padlive.PlayedTakeUi.Recording }
        act("attends 2")
        tap("padlive_played")
        waitFor("the played take done") { vm.padLive.played.ui.value is com.pocketstudio.app.padlive.PlayedTakeUi.Done }
    }

    /**
     * PLAN-RESTE 5.8 (control 9), the Live view's pads (Live › Pads call the view model's toggleLive straight, round 3 zone 1
     * n° 5): from Pad Live, « Briques » opens the Live view, its Pads tab the pads; a loop pad touched is lit (2) and the sound
     * line says it plays; touched again it goes out and the sound line no longer says so, as for Pad Live's own pads
     * ([padLiveLightsStopsAndRecordsAsOnAPhone]).
     */
    @Test
    fun livePadsLightGoOutAndAreHeardAsPadLivesDo() {
        padsAsOnASeat = true
        bring(AMENEE_PADLIVE, SeatOptions())
        val vm = rhythm()!!
        fun waitFor(what: String, done: () -> Boolean) {
            val until = System.currentTimeMillis() + 60_000
            while (!done()) {
                assertTrue("$what never came\n" + screenLines().joinToString("\n"), System.currentTimeMillis() < until)
                act("attends 0.5")
            }
        }
        fun tap(tag: String): String {
            waitFor("« $tag »") { centerOf(tag) != null }
            val at = reachTag(tag) ?: centerOf(tag)!!
            return act("touche ${at.first} ${at.second}")
        }
        // « Plus » unfolds the row holding « Briques » (the Live view).
        tap("padlive_more")
        tap("padlive_live")
        waitFor("the Live view") { centerOf("live_screen") != null }
        tap("live_tab_Pads")
        waitFor("Live › Pads") { centerOf("live_pads_full") != null || centerOf("live_pads") != null }
        val loops = (0 until 16).filter { project()!!.live.at(it)?.oneShot == false }
        val (pad, tag) = loops.firstNotNullOfOrNull { p -> listOf("live_full_pad_$p", "live_pad_$p").firstOrNull { centerOf(it) != null }?.let { p to it } }
            ?: error("no loop pad on Live › Pads (loops $loops)\n" + screenLines().joinToString("\n"))
        // A.16: the engine plays it on its grid (at once, or on its line if another pad plays).
        var heard = tap(tag)
        waitFor("the pad playing") { vm.liveStates.value[pad] == 2 || false.also { heard = act("attends 0.5") } }
        if ("joue en boucle" !in heard) heard = act("attends 0.1")
        assertTrue("the sound line says the pad plays: $heard", loopHeard(heard, pad))
        assertEquals("the pad that plays is lit (Live › Pads)", 2, vm.liveStates.value[pad])
        val stopped = tap(tag)
        assertEquals("touched again, the pad goes out (Live › Pads)", 0, vm.liveStates.value[pad])
        assertFalse("the sound line no longer says it plays: $stopped", loopHeard(stopped, pad))
    }

    /**
     * PLAN-RESTE 5.13 (P1; the mirror of [aZipImportedOutsideRemixAddsAProject]): « Importer un fichier MIDI » of the
     * keyboard's page: the picker answers a .mid (made here from the project open now: a bass line of three notes) and the
     * import puts its notes in (the first channel onto the synth).
     */
    @Test
    fun aMidImportedPutsItsNotesIn() {
        bring(AMENEE_SONG, SeatOptions(simple = false))
        val vm = rhythm()!!
        vm.addTrackOf(TrackKind.Bass)
        listOf(48, 50, 53).forEachIndexed { i, pitch -> vm.addNote(Note(i * 4 * NoteClip.TICKS_PER_STEP, 2 * NoteClip.TICKS_PER_STEP, pitch)) }
        act("attends 1")
        fun notes() = project()!!.let { p -> p.synthNotes.notes.size + p.tracks.sumOf { it.notes.notes.size } }
        val before = notes()
        assertEquals("the bass line to export", 3, before)
        assertTrue("the synth has no notes yet", project()!!.synthNotes.notes.isEmpty())
        fun tap(tag: String): String {
            val at = reachTag(tag) ?: error("nothing to touch: $tag\n" + screenLines().joinToString("\n"))
            return act("touche ${at.first} ${at.second}")
        }
        tap("tab_Sons")
        // The keyboard's page: a track's chip (the first track's, else the bass's).
        tap(listOf("track_0", "track_1").firstOrNull { centerOf(it) != null } ?: error("no track chip\n" + screenLines().joinToString("\n")))
        // The page's « Notes » view holds « Importer un fichier MIDI » (the keyboard view does not).
        if (centerOf("mode_notes") != null) tap("mode_notes")
        val said = tap("import_midi")
        act("attends 3")
        assertTrue("the picker gave a .mid: $said", "Le-morceau-de-Sam.mid" in said)
        val until = System.currentTimeMillis() + 30_000
        while (notes() == before && vm.transcription.value !is SampleTask.Failed && System.currentTimeMillis() < until) act("attends 0.5")
        assertFalse("the import failed: ${vm.transcription.value}", vm.transcription.value is SampleTask.Failed)
        assertTrue("the .mid's notes went in: $before → ${notes()}", notes() > before)
        assertEquals("its channel landed on the synth", 3, project()!!.synthNotes.notes.size)
    }

    /** Audit ultra tour 0 (seat c): « Pack… › Techno » on page B left it « vide » (the phone's engine makes the pack). */
    @Test
    fun aPackFillsThePageShown() {
        padsAsOnASeat = true
        bring(AMENEE_PADLIVE, SeatOptions())
        // The ready pads are made on entering (padLive.busy): the pack's chip waits for them; each tag waited for (CI 242: an NPE).
        fun tap(tag: String) {
            val until = System.currentTimeMillis() + 60_000
            while (rhythm()!!.padLive.busy.value != null || centerOf(tag) == null) {
                if (System.currentTimeMillis() > until) error("$tag absent after 60 s (busy ${rhythm()!!.padLive.busy.value}):\n" + screenLines().joinToString("\n"))
                act("attends 0.5")
            }
            val (x, y) = centerOf(tag) ?: error("$tag absent:\n" + screenLines().joinToString("\n"))
            act("touche $x $y")
        }
        tap("padlive_page_B")
        tap("padlive_pack")
        tap("padlive_pack_techno")
        val until = System.currentTimeMillis() + 60_000
        while (rhythm()!!.padLive.busy.value != null) {
            assertTrue("the pack never came", System.currentTimeMillis() < until)
            act("attends 0.5")
        }
        act("attends 0.5")
        val pageB = (0 until com.pocketstudio.model.PadLive.PAGE_SIZE).mapNotNull { project()!!.live.at(com.pocketstudio.model.PadLive.pad(1, it)) }
        assertTrue("page B is empty", pageB.size >= 4)
        assertEquals("techno", project()!!.padLive.packOf(1))
        assertTrue(screenLines().joinToString("\n"), screenLines().none { "Page B · vide" in it })
        // Audit method v2: the bench's engine stays, as a phone's (the old lending let it go here).
        if (ENGINE_ALWAYS) assertTrue("the engine stays", com.pocketstudio.engine.NativeEngine.sink != null)
        else assertEquals("the bench let its engine go once the pack was made", null, com.pocketstudio.engine.NativeEngine.sink)
    }
}
