package com.pocketstudio.model

import com.pocketstudio.model.StarterWizard.Part
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G0 (docs/PLAN-GUIDE.md): no two Drops alike (« variation entre Drops »), and the first Drop of
 * bass music opening on the bass and the beat alone (docs/hits/bass.md § 5, « Drop 1A »).
 */
class DropVariationTest {
    internal companion object {
        fun song(style: StarterWizard.Style, mood: StarterWizard.Mood = style.melody.mood): Project {
            val lines = style.lines.filter { !it.off }.associate { it.role to null as PadSample? }
            val chosen = StarterWizard.defaultParts(style).associateWith { StarterWizard.soundsFor(it, style).first() }
            return StarterWizard.build(style, lines, mood, chosen)
        }

        fun benj(): Project = ProjectJson.decode(DropVariationTest::class.java.getResource("/fixtures/benj-jumpup1.json")!!.readText())
    }

    @Test
    fun benjaminsThreeDropsAreTheSameDropAndTheMetricSaysSo() {
        val p = benj()
        val drops = DropVariation.drops(p)
        assertEquals(listOf(40, 96, 152), drops.map { it.start })
        val pairs = DropVariation.pairs(p).associate { (it.first to it.second) to it.third }
        // Drop 2 and Drop 3: bar for bar the same but for the drummer's fill at the 8 (the second bass from the second Drop on).
        assertTrue(pairs.getValue(1 to 2) < 0.1f)
        assertTrue(DropVariation.of(p)!! < DropVariation.MIN)
        // Drop 1 against Drop 2: only the bass's sound (and the lead's first 16 bars) differ.
        assertTrue(pairs.getValue(0 to 1) > 0.5f)
    }

    @Test
    fun aDropDiffersFromItselfOnlyWhenSomethingSoundsOtherwise() {
        val p = song(StarterWizard.style("house")!!)
        val prints = DropVariation.barPrints(p)
        assertEquals(p.songBars, prints.size)
        val d = DropVariation.drops(p)
        assertEquals(0f, DropVariation.difference(prints, d[0], d[0]), 0f)
        // Muting the drums in a Drop's part shows.
        val i = p.song.indices.first { p.songStepOf(it) / p.stepsPerBar == d[0].start }
        val muted = p.copy(song = p.song.mapIndexed { k, part -> if (k == i) part.copy(muted = part.muted + SongPart.DRUMS) else part })
        assertTrue(DropVariation.barPrints(muted)[d[0].start] != prints[d[0].start])
    }

    @Test
    fun everyStyleVariesItsDrops() {
        val report = StringBuilder()
        val low = ArrayList<String>()
        for (style in StarterWizard.STYLES) for (mood in StarterWizard.Mood.entries) {
            val p = song(style, mood)
            val v = DropVariation.of(p)
            report.append("${style.id} $mood drops=${DropVariation.drops(p).map { "${it.start}+${it.bars}" }} variation=$v scenes=${p.allScenes.count { !it.isEmpty }}\n")
            if (v != null && v < DropVariation.MIN) low += "${style.id} $mood ($v)"
            // Every assistant song has at least two Drops (or Refrains) to compare.
            assertTrue(style.id, DropVariation.drops(p).size >= 2)
        }
        println(report)
        assertTrue("Drops too alike: $low", low.isEmpty())
    }

    /** The parts' tracks that play notes in [bar] of the song. */
    private fun Project.notesIn(bar: Int, track: Int): Int {
        val spb = stepsPerBar
        var at = 0
        for (part in song) {
            val bars = partSteps(part) / spb
            if (bar < at + bars) {
                if (!part.plays(SongPart.inst(track))) return 0
                val scene = allScenes[part.sceneFor(SongPart.inst(track))]
                val clip = (if (track == 0) scene.synthNotes else scene.trackNotes[track]) ?: return 0
                val loop = (clip.steps / spb).coerceAtLeast(1)
                val b = (part.fromBars(SongPart.inst(track)) + bar - at) % loop
                val t = spb * NoteClip.TICKS_PER_STEP
                return clip.notes.count { it.start in b * t until (b + 1) * t }
            }
            at += bars
        }
        return 0
    }

    @Test
    fun bassMusicOpensItsFirstDropOnTheBassAndTheBeatAlone() {
        for (id in listOf("dnb", "neurofunk", "jump_up", "dubstep", "riddim")) {
            val style = StarterWizard.style(id)!!
            val p = song(style)
            val drop = DropVariation.drops(p).first()
            val pads = p.padTrackIds()
            val lead = p.instrumentTracks.firstOrNull { it.name == "Lead" }?.id
            val bass = p.bassTrackIds()
            val quiet = (pads + listOfNotNull(lead) + p.instrumentTracks.filter { DefaultNames.french(it.name) in setOf(StarterWizard.COUNTER_TRACK, StarterWizard.VOCAL_TRACK) || it.name in setOf(StarterWizard.COUNTER_TRACK, StarterWizard.VOCAL_TRACK) }.map { it.id }).toSet()
            for (bar in drop.start until drop.start + 8) {
                for (t in quiet) assertEquals("$id bar $bar track $t", 0, p.notesIn(bar, t))
            }
            // The bass and the beat play there; the chords come back after.
            assertTrue(id, (drop.start until drop.start + 8).sumOf { b -> bass.sumOf { p.notesIn(b, it) } } > 0)
            if (Part.Pad in StarterWizard.defaultParts(style) && pads.isNotEmpty()) {
                assertTrue(id, (drop.start + 8 until drop.start + drop.bars).sumOf { b -> pads.sumOf { p.notesIn(b, it) } } > 0)
            }
        }
        // Liquid keeps its chords from the Drop's first bar.
        val liquid = song(StarterWizard.style("liquid_dnb")!!)
        val first = DropVariation.drops(liquid).first()
        assertTrue(liquid.padTrackIds().sumOf { liquid.notesIn(first.start, it) } > 0)
    }
}
