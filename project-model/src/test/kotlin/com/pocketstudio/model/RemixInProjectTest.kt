package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 5a (n° 15, Benjamin's video APK 233): the separated song is a track of the project, its four
 * stems always there, the parts not heard while listening muted, and it is placed in the song's
 * structure so the Structure shows it on the bars.
 */
class RemixInProjectTest {
    /** « Boys », 120 BPM, 64 s: 32 bars of 4/4 at the project's 120 BPM. */
    private fun withSong(song: List<SongPart> = emptyList(), placed: Boolean = false) = Project(
        tempoBpm = 120.0,
        song = song,
        remix = Remix("set-boys", "Boys", songBpm = 120.0, seconds = 64.0, placed = placed),
    )

    @Test
    fun theFourStemsAlwaysComeInTheOnesNotHeardMuted() {
        val voice = Remix.stemsHeard(listOf(false, false, false, true))
        assertEquals(Remix.STEMS, voice.size)
        assertEquals(listOf(true, true, true, false), voice.map { it.mute })
        // Nothing chosen, all or none: all four heard.
        listOf(null, List(4) { true }, List(4) { false }).forEach { heard ->
            assertTrue(Remix.stemsHeard(heard).none { it.mute })
        }
    }

    @Test
    fun aSongInAnEmptySongGetsAPartAsLongAsItOnBriqueA() {
        val p = withSong().withRemixPlaced()
        assertEquals(listOf(SongPart(scene = 0, bars = 32)), p.song)
        assertTrue(p.remix.placed)
        // Its four lines on the bars: heard over the whole part.
        assertEquals(listOf(listOf(0f..1f)), p.remixSpans())
    }

    @Test
    fun aSongPlacedLaterCoversItsPlaceOnly() {
        val p = withSong().copy(remix = withSong().remix.copy(atStep = 64)).withRemixPlaced()
        // 4 bars of silence then the song's 32: 36 bars, the song from the fifth bar.
        assertEquals(36, p.song.single().bars)
        assertEquals(4f / 36f, p.remixSpans().single().single().start, 1e-6f)
    }

    @Test
    fun aSongWithPartsAlreadyIsOnlyMarkedPlaced() {
        val parts = listOf(SongPart(scene = 0, repeats = 2), SongPart(scene = 1))
        val p = withSong(parts).withRemixPlaced()
        assertEquals(parts, p.song)
        assertTrue(p.remix.placed)
        // Once placed, parts the user takes away stay away.
        val emptied = p.copy(song = emptyList())
        assertSame(emptied, emptied.withRemixPlaced())
    }

    @Test
    fun aPartThatLeavesTheRemixOutShowsNoSongThere() {
        val parts = listOf(SongPart(scene = 0, bars = 8), SongPart(scene = 0, bars = 8, muted = setOf(SongPart.REMIX)))
        val p = withSong(parts, placed = true)
        assertEquals(listOf(listOf(0f..1f), emptyList()), p.remixSpans())
    }

    @Test
    fun anOldProjectWithTheHiddenRemixStripMigratesWithoutLosingItsSound() {
        // Saved before step 5a: the song only on strip 3 (its effects and volume) and the Live column, no part.
        val strip = Channel(volume = 0.6f, inserts = Mixer.sanitizeInserts(listOf(Insert(type = EffectType.REVERB.ordinal, params = EffectType.REVERB.defaults))))
        val old = withSong().copy(
            remix = withSong().remix.copy(stems = Remix.stemsHeard(listOf(false, false, false, true))),
            mixer = Mixer().let { m -> m.copy(channels = m.channels.mapIndexed { i, c -> if (i == Remix.STRIP) strip else c }) },
        )
        val json = ProjectJson.encode(old)
        assertFalse(json.contains("\"placed\""))
        val opened = ProjectJson.decode(json)
        // Placed in the structure, once.
        assertEquals(1, opened.song.size)
        assertTrue(opened.remix.placed)
        // Its sound as it was: the same stems, the voice alone, the strip with its volume and effect.
        assertEquals(old.remix.stems, opened.remix.stems)
        assertEquals(old.mixer.channels[Remix.STRIP], opened.mixer.channels[Remix.STRIP])
        assertTrue(opened.remixEndBars() > 0)
        // Saved and opened again: nothing more added.
        assertEquals(opened, ProjectJson.decode(ProjectJson.encode(opened)))
    }

    @Test
    fun stemKeysNameTheFourLinesUnderTheFolder() {
        assertEquals(listOf(3, 0, 1, 2), Remix.SHOWN_ORDER)
        (0 until Remix.STEMS).forEach { assertEquals(it, Remix.stemOfKey(Remix.stemKey(it))) }
        assertEquals(null, Remix.stemOfKey(SongPart.REMIX))
        assertEquals(null, Remix.stemOfKey("remix:9"))
    }
}
