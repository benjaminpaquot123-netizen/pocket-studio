package com.pocketstudio.model

import com.pocketstudio.model.SongBars.drumBars
import com.pocketstudio.model.SongBars.sceneOfBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Remix lot 1 (Mehdi): a phrase of a song's voice put on a pad in the Drop sounded in the Drop
 * looped, not in the song: the Drop's first bar (its crash) and last bar (its fill) play one-bar
 * copies of the Drop's drums made when the song was built. Hits written in the Drop afterwards now
 * reach those copies too, where the copy still agrees with the Drop (the crash and the fill stay).
 */
class DrumBarCopiesTest {
    private val style = StarterWizard.style("jump_up")!!

    private fun song(): Project =
        StarterWizard.build(style, style.lines.associate { it.role to null as PadSample? }, style.melody.mood,
            StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, style).first() })

    /** [p] with scene [scene]'s drums changed by [edit], as the drum page writes them (the scene shown). */
    private fun editDrums(p: Project, scene: Int, edit: (List<DrumLane>) -> List<DrumLane>): Project {
        val shown = p.switchScene(scene)
        return shown.copy(drums = shown.drums.copy(lanes = edit(shown.drums.lanes))).sanitized()
    }

    private fun Project.drop(): Int = allScenes.indexOfFirst { it.label == "Drop" }

    /** For each bar of the song's first Drop: its bar in the Drop's loop, and what the drums play there. */
    private fun firstDrop(p: Project): List<Pair<Int, List<Map<Int, Hit>>>> {
        val drop = p.drop()
        val scenes = p.sceneOfBars()
        val start = scenes.indexOf(drop)
        val end = (start until scenes.size).firstOrNull { scenes[it] != drop } ?: scenes.size
        val loop = (p.allScenes[drop].drums.steps / p.stepsPerBar).coerceAtLeast(1)
        val bars = p.drumBars()
        return (start until end).map { b -> (b - start) % loop to bars[b] }
    }

    @Test
    fun aPadPutInTheDropSoundsOnEveryBarOfTheDropInTheSong() {
        val made = song()
        val drop = made.drop()
        // The song's Drop starts on a crash and ends on a fill, each a copy of its own.
        val before = firstDrop(made)
        assertTrue(before.size >= 8)
        // Mehdi's two phrases: pad 16 on the Drop's bar 1, pad 15 on its bar 3.
        val p = editDrums(made, drop) { lanes ->
            lanes.mapIndexed { i, l ->
                when (i) {
                    15 -> l.copy(hits = l.hits + (0 to Hit()))
                    14 -> l.copy(hits = l.hits + (2 * 16 to Hit(velocity = 0.8f)))
                    else -> l
                }
            }
        }
        val after = firstDrop(p)
        assertEquals(before.size, after.size)
        after.forEachIndexed { k, (loopBar, bar) ->
            val was = before[k].second
            assertEquals("bar ${k + 1} of the Drop, pad 16", if (loopBar == 0) was[15] + (0 to Hit()) else was[15], bar[15])
            assertEquals("bar ${k + 1} of the Drop, pad 15", if (loopBar == 2) was[14] + (0 to Hit(velocity = 0.8f)) else was[14], bar[14])
            // The crash and the fill are still there: every other pad plays as before.
            for (lane in 0 until 14) assertEquals("bar ${k + 1}, lane $lane", was[lane], bar[lane])
        }
        // The first bar, the crash's copy, has its phrase.
        assertEquals(Hit(), after.first().second[15][0])
        // The case of the report: the Drop starts on a crash's copy and has a fill's.
        val first = made.song.first { it.scene == drop }
        assertEquals(DrumArrangement.CRASH_LABEL, made.allScenes[first.sceneFor(SongPart.DRUMS)].label)
        assertTrue(made.song.any { it.scene == drop && made.isFillPart(it) })
    }

    @Test
    fun aHitTakenOutOfTheDropLeavesItsCopiesToo() {
        val made = song()
        val drop = made.drop()
        val with = editDrums(made, drop) { lanes -> lanes.mapIndexed { i, l -> if (i == 15) l.copy(hits = l.hits + (0 to Hit())) else l } }
        val without = editDrums(with, drop) { lanes -> lanes.mapIndexed { i, l -> if (i == 15) l.copy(hits = l.hits - 0) else l } }
        assertTrue(firstDrop(without).all { it.second[15].isEmpty() })
        // Undone by hand, the song is the one made: the copies too.
        assertEquals(made.switchScene(0).scenes, without.switchScene(0).scenes)
    }

    @Test
    fun theCrashAndTheFillKeepTheirOwnHits() {
        val made = song()
        val drop = made.drop()
        val spb = made.stepsPerBar
        val dropDrums = made.allScenes[drop].drums
        val loop = (dropDrums.steps / spb).coerceAtLeast(1)
        // Every step where the fill's copy differs from the Drop: the Drop changes there, the fill does not.
        val fillPart = made.song.first { it.scene == drop && made.isFillPart(it) }
        val fill = made.allScenes[fillPart.sceneFor(SongPart.DRUMS)].drums
        val bar = (fillPart.from.filterKeys { it != SongPart.DRUMS }.values.maxOrNull() ?: 0) % loop
        val own = fill.lanes.indices.flatMap { lane ->
            (0 until spb).filter { st -> fill.lanes[lane].hits[st] != dropDrums.lanes[lane].hits[bar * spb + st] }.map { lane to it }
        }
        assertTrue("the fill has hits of its own", own.isNotEmpty())
        val p = editDrums(made, drop) { lanes ->
            lanes.mapIndexed { i, l ->
                var hits = l.hits
                own.filter { it.first == i }.forEach { (_, st) -> hits = hits + ((bar * spb + st) to Hit(velocity = 0.35f)) }
                l.copy(hits = hits)
            }
        }
        assertEquals(fill, p.allScenes[fillPart.sceneFor(SongPart.DRUMS)].drums)
        // The Drop itself took them.
        own.forEach { (lane, st) -> assertEquals(Hit(velocity = 0.35f), p.allScenes[drop].drums.lanes[lane].hits[bar * spb + st]) }
    }

    @Test
    fun anotherSceneTheDrumsPlayIsNotACopy() {
        // A part written from the live grid: the Drop with the Intro's drums. The Intro is not the Drop's copy.
        val made = song()
        val drop = made.drop()
        val intro = made.allScenes.indexOfFirst { it.label == "Intro" }
        val p0 = made.copy(song = made.song + SongPart(drop, play = mapOf(SongPart.DRUMS to intro), bars = 1)).sanitized()
        val p = editDrums(p0, drop) { lanes -> lanes.mapIndexed { i, l -> if (i == 15) l.copy(hits = l.hits + (0 to Hit())) else l } }
        assertEquals(p0.allScenes[intro], p.allScenes[intro])
    }
}
