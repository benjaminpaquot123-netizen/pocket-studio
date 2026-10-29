package com.pocketstudio.model.tuto

import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.renameScene
import com.pocketstudio.model.switchScene
import com.pocketstudio.model.track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Socle 2: the course's own drums (every part, or a part's own), the crash played once at a part's
 * start, a second pad and a counter-melody beside the chords and the lead, the style's kit from its
 * packs, the examples on the user's drum sounds.
 */
class TutoSocle2Test {
    private val style = "trance"

    private fun course(json: String) = TutoCourse.parse(json).also { TutoCourse.forTest(style, it) }

    @After
    fun back() = TutoCourse.forTest(style, null)

    private fun step(check: String, target: String = "", part: String = "", keep: String = "", song: String = "") =
        TutoStep("x", "x", check = check, target = target, part = part, keep = keep, song = song)

    /** Applies [s]'s main button then « Montre-moi » until it is done. */
    private fun doIt(s: TutoStep, p0: Project, t: TutoTarget): Project {
        var p = p0
        repeat(12) {
            if (TutoChecks.done(s, p, t)) return p
            val need = TutoChecks.need(s, p, t)
            p = (if (need != null && need !is TutoNeed.Copy) TutoChecks.fulfil(need, s, p, t)?.first else TutoChecks.show(s, p, t) ?: need?.let { TutoChecks.fulfil(it, s, p, t)?.first })?.sanitized()
                ?: error("${s.check}: nothing to do")
        }
        assertTrue("${s.check} not done", TutoChecks.done(s, p, t))
        return p
    }

    private val base = """
        {"style":"trance","chapters":[{"id":"c","title":"T","steps":[
          {"id":"g","text":"t","check":"label:Groove"},
          {"id":"i","text":"t","check":"part:Intro:any","keep":"*"},
          {"id":"d","text":"t","check":"part:Drop:full","keep":"*"},
          {"id":"b","text":"t","check":"part:Break:nokick","keep":"hat"},
          {"id":"o","text":"t","check":"song:order","song":"Intro:8,Drop:16,Break:8,Drop:16,Fin:8"}
        ]}],
    """.trimIndent()

    @Test
    fun theCoursesDrumsReplaceTheRecipes() {
        course(base + """ "drums": {
            "*": {"kick": "X...X...X...X...", "openhat": "..x...x...x...x.", "shaker": "o.x.o.x.o.x.o.x.", "hat": "x.x.x.x.x.x.x.x."},
            "Break": {"hat": "x...x...x...x...", "crash": "X..............."}
        }}""")
        val t = TutoTarget.of(style)!!
        assertEquals("X...X...X...X...", t.line("kick")!!.pattern)
        assertEquals("o.x.o.x.o.x.o.x.", t.line("perc2")!!.pattern)
        assertNull("the recipe's ride is gone", t.line("ride"))
        // The Break's own hats, in the Break.
        assertEquals("x...x...x...x...", t.line("hat", "Break")!!.pattern)
        assertEquals("x.x.x.x.x.x.x.x.", t.line("hat")!!.pattern)
        val br = t.song.allScenes.first { it.label == "Break" }.drums
        assertEquals((0 until br.steps step 4).toSet(), br.lanes[5].hits.keys)
        assertTrue("only its pads", br.lanes[0].hits.isEmpty())
        assertTrue("its crash is not looped", br.lanes[TutoChecks.CRASH_LANE].hits.isEmpty())
        // The crash once at the Break's start in the example song.
        assertTrue(TutoChecks.done(step("crash:once", part = "Break"), t.song, t))
        // A drum step in the Break: that part first, then its own cells.
        val hats = step("lane:hat", target = "hat", part = "Break")
        var p = t.blank()
        for (s in listOf(step("lane:kick"), step("lane:hat"), step("label:Groove"), step("part:Break:nokick", keep = "hat"))) p = doIt(s, p, t)
        assertEquals(setOf(0, 4, 8, 12), TutoChecks.hint(hats, t, 16)!!.second)
        // « Créer la partie » brought the Break's own hats.
        assertTrue(TutoChecks.done(hats, p, t))
        // Without them: to the Break first, then its cells.
        p = p.switchSceneTo("Break").let { q -> q.copy(drums = q.drums.copy(lanes = q.drums.lanes.mapIndexed { i, l -> if (i == 5) l.copy(hits = emptyMap()) else l })) }.sanitized().switchSceneTo("Groove")
        assertFalse(TutoChecks.done(hats, p, t))
        assertEquals(TutoNeed.Scene("Break"), TutoChecks.need(hats, p, t))
        p = doIt(hats, p, t)
        assertTrue(TutoChecks.done(hats, p, t))
    }

    private fun Project.switchSceneTo(label: String) = switchScene(allScenes.indexOfFirst { it.label == label })

    /** The crash once: a one-bar part at each start of the Drop, never looped; the order and the lengths still right. */
    @Test
    fun aCrashOnceAtThePartsStart() {
        course(base + """ "drums": {"*": {"kick": "X...X...X...X...", "hat": "x.x.x.x.x.x.x.x."}}}""")
        val t = TutoTarget.of(style)!!
        var p = t.blank()
        for (s in listOf(step("lane:kick"), step("lane:hat"), step("label:Groove"), step("part:Intro:any", keep = "*"), step("part:Drop:full", keep = "*"), step("part:Break:nokick", keep = "hat"))) p = doIt(s, p, t)
        val order = step("song:order", song = "Intro:8,Drop:16,Break:8,Drop:16,Fin:8")
        val phrases = step("song:phrases", song = order.song)
        p = doIt(order, p, t)
        p = doIt(phrases, p, t)
        val once = step("crash:once", part = "Drop")
        assertFalse(TutoChecks.done(once, p, t))
        p = TutoChecks.show(once, p, t)!!
        assertTrue(TutoChecks.done(once, p, t))
        // Two Drops, each split: its crash bar, then the rest; the song reads the same.
        assertEquals(7, p.song.size)
        assertTrue(TutoChecks.done(order, p, t))
        assertTrue(TutoChecks.done(phrases, p, t))
        val drop = p.allScenes.indexOfFirst { it.label == "Drop" }
        assertTrue("the Drop's loop has no crash", p.allScenes[drop].drums.lanes[TutoChecks.CRASH_LANE].hits.isEmpty())
        assertEquals(listOf(8, 16, 8, 16, 8), TutoChecks.runs(p).map { it.bars })
        // Done again: nothing more.
        assertNull(TutoChecks.crashOnce(p, "Drop"))
    }

    /** A second pad (« Nappe ») beside the chords, a counter-melody beside the lead: their own tracks and lines. */
    @Test
    fun aSecondPadAndACounterMelody() {
        course(base + """ "lines": {"pad2": "0:A3+C4+E4:32", "counter": "0:E5:2 4:G5:2 8:A5:4"}}""")
        val t = TutoTarget.of(style)!!
        var p = t.blank()
        for (role in listOf("pad", "pad2", "counter")) {
            val s = step("notes:$role")
            assertEquals(TutoNeed.Track(role), TutoChecks.need(s, p, t))
            p = doIt(s, p, t)
        }
        val names = p.instrumentTracksNames()
        assertTrue(names.toString(), "Nappe" in names && "Contre-chant" in names && "Accords" in names)
        assertEquals(TutoTarget.PAD2, TutoChecks.roleOf(p, TutoChecks.trackFor(p, "pad2")!!))
        assertTrue(TutoChecks.trackFor(p, "pad") != TutoChecks.trackFor(p, "pad2"))
        assertEquals(listOf(57, 60, 64), p.track(TutoChecks.trackFor(p, "pad2")!!).notes.notes.filter { it.start == 0 }.map { it.pitch }.sorted())
        // In an example of the parts.
        val loop = TutoChecks.loop(t, setOf("kick", "pad2", "counter"))
        assertNotNull(TutoChecks.trackFor(loop, "counter"))
    }

    private fun Project.instrumentTracksNames() = instrumentTracks.map { it.name }

    /** The style's kit: the packs its lines come from, the sound each pad takes once they are there. */
    @Test
    fun theStylesKitFromItsPacks() {
        val t = TutoTarget.of("techno")!!
        fun entry(id: String) = PackEntry(id, id, "$id.zip", "0".repeat(64), 1_000_000)
        val index = listOf(entry("techno"), entry("tr909"), entry("house"))
        // Techno's open hat from the TR-909, the rest from its own pack.
        assertEquals(listOf("tr909", "techno"), TutoKit.packs(t, index).map { it.id })
        assertEquals(listOf("tr909", "techno"), TutoKit.missing(t, index, emptyList()).map { it.id })
        val techno = PackManifest("techno", "Techno", sounds = listOf(PackSound("k.wav", "kick", "Kick"), PackSound("h.wav", "hat", "Hat"), PackSound("o.wav", "open_hat", "Open")))
        val tr909 = PackManifest("tr909", "TR-909", sounds = listOf(PackSound("oh.wav", "open_hat", "909 OH")))
        assertEquals(listOf("tr909"), TutoKit.missing(t, index, listOf(techno)).map { it.id })
        val sounds = TutoKit.sounds(t, listOf(techno, tr909))
        assertEquals("techno" to "k.wav", sounds.getValue(0).let { it.first to it.second.file })
        assertEquals("tr909" to "oh.wav", sounds.getValue(6).let { it.first to it.second.file })
        assertEquals("techno" to "h.wav", sounds.getValue(5).let { it.first to it.second.file })
        // Without its packs: nothing (the built-in kit); a catalogue without them: no offer.
        assertTrue(TutoKit.sounds(t, emptyList()).isEmpty())
        assertTrue(TutoKit.packs(t, emptyList()).isEmpty())
    }

    /** « Écouter l'exemple » with the user's own drum sounds (the style's pack once loaded). */
    @Test
    fun theExamplesPlayTheUsersSounds() {
        val t = TutoTarget.of("techno")!!
        val sample = PadSample("kick.wav", "Kick · Techno")
        val mine = t.blank().let { p -> p.copy(drums = p.drums.copy(lanes = p.drums.lanes.mapIndexed { i, l -> if (i == 0) l.copy(sample = sample) else l }), padVoices = p.padVoices - 0) }
        val ex = TutoChecks.withKitOf(TutoChecks.example(step("listen").copy(listen = "drums:kick"), t)!!.after, mine)
        assertEquals(sample, ex.drums.lanes[0].sample)
        assertFalse(0 in ex.padVoices)
        assertTrue(ex.drums.lanes[0].hits.isNotEmpty())
    }

    /** Synthwave's chords in an arpeggio: the track the course makes plays them so from its start. */
    @Test
    fun theRecipesArpeggiatorStays() {
        val t = TutoTarget.of("synthwave")!!
        val (p, id) = TutoChecks.partTrack(t.blank(), "pad", t)!!
        val wanted = t.notes(com.pocketstudio.model.StarterWizard.Part.Pad).performance
        assertTrue(wanted != com.pocketstudio.model.Performance())
        assertEquals(wanted, p.track(id).notes.performance)
    }

    /** The echo on one track the step names (the bass alone), not the lead's. */
    @Test
    fun anEchoOnTheTrackNamed() {
        val t = TutoTarget.of("techno")!!
        val (p, id) = TutoChecks.partTrack(t.blank(), "bass", t)!!
        val s = step("send:1", target = "bass")
        assertEquals(listOf(com.pocketstudio.model.InstrumentTrack.mixerChannel(id)), TutoChecks.channelsToDo(s, p))
        assertTrue(TutoChecks.done(s, TutoChecks.show(s, p, t)!!, t))
    }

    /** « Montre-moi » of a variation: a new hit on an empty cell (dnb's open hat already on the second to last). */
    @Test
    fun aVariationOnAnEmptyCell() {
        val t = TutoTarget.of("dnb")!!
        val blank = t.blank()
        val full = blank.copy(drums = blank.drums.copy(steps = 32, lanes = blank.drums.lanes.mapIndexed { i, l ->
            if (i == 0) l.copy(hits = mapOf(0 to com.pocketstudio.model.Hit(1f))) else if (i == 6) l.copy(hits = (16 until 32).associateWith { com.pocketstudio.model.Hit(0.5f) }) else l
        })).renamed("Drop 2")
        val s = step("changed:drums:Drop 2")
        val base = TutoChecks.signature("drums:Drop 2", full)
        val shown = TutoChecks.show(s, full, t)!!
        assertTrue(TutoChecks.done(s, shown, t, base))
    }

    private fun Project.renamed(label: String) = sanitized().renameScene(0, label).sanitized()

    @Test
    fun theValidationReadsTheDrums() {
        val bad = TutoCourse.parse(
            (base + """ "drums": {
                "*": {"kick": "X...X...", "guitar": "x...............", "crash": "X..............."},
                "Break": {"ride": "x...x...x...x...", "crash": "X..............Z"},
                "Pont": {"hat": "x...x...x...x..."}
            }}""").replace("""{"id":"g","text":"t","check":"label:Groove"},""", """{"id":"g","text":"t","check":"label:Groove"},{"id":"cr","text":"t","check":"crash:once","part":"Drop"},"""),
        )
        val problems = TutoValidation.problems(bad, "trance.json").joinToString("\n")
        listOf("drums.*.kick: 8 cells", "role « guitar »", "drums.*.crash", "« ride » and « crash » on the same pad", "cell « Z »", "part « Pont »", "crash:once before the song")
            .forEach { assertTrue("$it in\n$problems", it in problems) }
    }
}
