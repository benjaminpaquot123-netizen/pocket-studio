package com.pocketstudio.app.remix

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.addTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where a stretch of a stem goes (B3), through [StemBoardRouter] with a stand-in app: the pad it
 * takes (never the kick, never a pad the beat plays, unless the user named it), the names, the
 * step of a new audio track, what is said, what is refused, the phrases on free pads, the
 * listening and its play head. Each stand-in « put » is counted: one call is one undo step.
 */
class StemBoardRouterTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @After
    fun tearDown() = scope.cancel()

    /** Each frame's sample says where it is in the song: frame f of stem s is (s * 1e6 + f) * 1e-7. */
    private class Source(
        override val frames: Long = 48_000L * 30,
        override val songBpm: Double = 120.0,
        override val firstBeatFrame: Long = 0,
        override val fittedBpm: Double = 100.0,
        val level: (stem: Int, frame: Long) -> Float = { s, f -> ((s * 1_000_000 + f) * 1e-7).toFloat() },
    ) : StemSource {
        val reads = ArrayList<String>()
        override fun read(stem: Int, start: Long, count: Long, fitted: Boolean): FloatArray {
            reads += "$stem:$start:$count:$fitted"
            val n = maxOf(0L, minOf(count, frames - start)).toInt()
            return FloatArray(n * 2) { i -> level(stem, start + i / 2) }
        }
    }

    private class Host(var p: Project, val source: Source? = Source()) : StemBoardHost {
        val pads = ArrayList<List<PadCut>>()
        val instruments = ArrayList<Pair<Pcm, String>>()
        val audio = ArrayList<List<Any>>()
        val live = ArrayList<Triple<Pcm, String, Int>>()
        var busy = false
        var step = -1
        var paused = 0
        var fail: Exception? = null
        override fun project() = p
        override suspend fun <T> withSong(setId: String, block: (StemSource) -> T): T? = source?.let(block)
        override fun busy() = busy
        override suspend fun putOnPads(cuts: List<PadCut>) {
            fail?.let { throw it }
            pads += cuts
        }
        override suspend fun putOnNewInstrument(pcm: Pcm, name: String): Int? {
            instruments += pcm to name
            return 1
        }
        override suspend fun putOnNewAudioTrack(pcm: Pcm, name: String, atStep: Int, bpm: Double): Int? {
            audio += listOf(pcm, name, atStep, bpm)
            return 0
        }
        override suspend fun putOnLivePad(pcm: Pcm, name: String, pad: Int, bpm: Double) {
            live += Triple(pcm, name, pad)
        }
        override fun songStepNow() = step
        override fun pauseMusic() {
            paused++
        }
    }

    /** Cuts as they are (no fade), records what it was asked; [found] phrases in the frames it is given. */
    private class Edits(val found: List<LongRange> = emptyList()) : StemEdits {
        val cuts = ArrayList<StemSelection>()
        var asked: List<Any?>? = null
        override fun cut(stem: Pcm, sel: StemSelection): Pcm {
            cuts += sel
            return Pcm(stem.data.copyOfRange((sel.startFrame * stem.channels).toInt(), (sel.endFrame * stem.channels).toInt()), stem.channels, stem.sampleRate)
        }
        override fun phrases(stem: Pcm, beatFrames: Long?, max: Int): List<LongRange> = phrases(stem, beatFrames, max, 0L)
        override fun phrases(stem: Pcm, beatFrames: Long?, max: Int, firstBeatFrame: Long): List<LongRange> {
            asked = listOf(stem.sampleRate, stem.channels, stem.frames, beatFrames, max, firstBeatFrame)
            return found.take(max)
        }
    }

    private class Player : GuideListenPlayer {
        var played: Pcm? = null
        var ended: (() -> Unit)? = null
        var stops = 0
        @Volatile var heard = 0L
        override fun play(pcm: Pcm, ended: () -> Unit): Boolean {
            played = pcm
            this.ended = ended
            return true
        }
        override fun stop() {
            stops++
        }
        override fun heard(): Long = heard
    }

    /** The kick on pad 1 and the snare on pad 3 play; a second scene plays pad 6. */
    private fun beat(): Project {
        val drums = DrumPattern(steps = 16).toggle(0, 0).toggle(0, 8).toggle(2, 4)
        val drop = DrumPattern(steps = 16).toggle(0, 0).toggle(5, 2)
        return Project(
            tempoBpm = 120.0, drums = drums,
            scenes = listOf(Scene("A", drums), Scene("B", drop)), scene = 0,
            song = listOf(SongPart(0, repeats = 4), SongPart(1, repeats = 4)),
            remix = Remix("song", "Ma chanson", songBpm = 120.0),
        ).sanitized()
    }

    private fun router(host: Host, edits: StemEdits = Edits(), player: Player = Player()) = StemBoardRouter(scope, host, edits, player)

    private fun sel(start: Long = 96_000, end: Long = 120_000, stem: Int = 3) = StemSelection("song", stem, start, end)

    @Test
    fun aStretchGoesOnTheFirstFreePadNeverTheKick() {
        val host = Host(beat())
        val edits = Edits()
        val r = router(host, edits)
        r.send(sel(), StemDestination.Pad())
        assertEquals(1, host.pads.size)
        val cut = host.pads.single().single()
        // Pad 1 (the kick) and pad 3 (the snare) play: pad 2 is the first free one.
        assertEquals(1, cut.lane)
        assertEquals("Voix 1", cut.name)
        // Exactly the stretch, fitted to the project's tempo, its fades kept for the cut.
        assertEquals(24_000, cut.pcm.frames)
        assertEquals(((3 * 1_000_000 + 96_000) * 1e-7).toFloat(), cut.pcm.data[0], 1e-6f)
        assertEquals(StemSelection(sel().setId, 3, 0, 24_000), edits.cuts.single())
        assertEquals("3:96000:24000:true", host.source!!.reads.single())
        assertEquals("« Voix 1 » est sur le pad 2.", r.state.value.message)
        assertNull(r.state.value.busy)
        // The next one is « Voix 2 », on the next free pad (pad 2 now has a sound).
        host.p = host.p.copy(drums = host.p.drums.withSample(1, PadSample("a.wav", "Voix 1")))
        r.send(sel(), StemDestination.Pad())
        assertEquals(3, host.pads.last().single().lane)
        assertEquals("Voix 2", host.pads.last().single().name)
    }

    @Test
    fun aPadPlayedInAnotherSceneOrHoldingASoundIsNotFree() {
        var p = beat()
        p = p.copy(drums = (1 until 5).fold(p.drums) { d, lane -> if (lane == 2) d else d.withSample(lane, PadSample("s$lane.wav", "Son $lane")) })
        // Pads 1-5 used here, pad 6 by the other scene: pad 7 is the first free one.
        assertEquals(6, StemRouting.padFor(p, null))
        assertFalse(StemRouting.padIsFree(p, 5))
        assertEquals((6 until DrumPattern.PAD_COUNT).toList(), StemRouting.freePads(p))
        // A pad the user names is taken, even with a sound.
        assertEquals(0, StemRouting.padFor(p, 0))
    }

    @Test
    fun everyPadUsedNothingIsCoveredWithoutAsking() {
        val full = beat().let { p -> p.copy(drums = (0 until 16).fold(p.drums) { d, i -> d.withSample(i, PadSample("s$i.wav", "Son $i")) }) }
        val host = Host(full)
        val r = router(host)
        r.send(sel(), StemDestination.Pad())
        assertTrue(host.pads.isEmpty())
        assertTrue(r.state.value.message!!.startsWith("Tous les pads ont déjà un son"))
        // Named by the user: replaced, and said, with the way back.
        r.send(sel(), StemDestination.Pad(4))
        assertEquals(4, host.pads.single().single().lane)
        assertEquals("« Voix 1 » remplace « Son 4 » sur le pad 5 (la flèche retour le remet).", r.state.value.message)
    }

    @Test
    fun instrumentTrackAndLivePad() {
        val host = Host(beat())
        val r = router(host)
        r.send(sel(), StemDestination.Instrument)
        assertEquals("Voix 1", host.instruments.single().second)
        assertEquals(24_000, host.instruments.single().first.frames)
        assertEquals("« Voix 1 » est une nouvelle piste d'instrument : joue-la au clavier.", r.state.value.message)
        // Eight instrument tracks: said, nothing made.
        host.p = (1..7).fold(host.p) { q, _ -> q.addTrack()!! }
        r.send(sel(), StemDestination.Instrument)
        assertEquals(1, host.instruments.size)
        assertEquals("Huit pistes d'instruments au maximum : supprime-en une pour en ajouter une autre.", r.state.value.message)

        host.p = beat().copy(live = LivePads(clips = List(LivePads.MAX) { i -> if (i < 3) LiveClip("l$i.wav", "Basse $i") else null }))
        r.send(sel(stem = 1), StemDestination.Live)
        assertEquals(3, host.live.single().third)
        assertEquals("Basse 3", host.live.single().second)
        assertEquals("« Basse 3 » est sur le pad live 4.", r.state.value.message)
        host.p = host.p.copy(live = LivePads(clips = List(LivePads.MAX) { LiveClip("l$it.wav", "x") }))
        r.send(sel(), StemDestination.Live)
        assertEquals(1, host.live.size)
        assertEquals("Les 16 pads live sont pris : vides-en un dans Live › Pads.", r.state.value.message)
    }

    @Test
    fun anAudioTrackStartsWhereAskedWhereTheSongPlaysOrWhereTheStretchIsHeard() {
        val host = Host(beat())
        val r = router(host)
        r.send(sel(), StemDestination.Track(atStep = 64))
        assertEquals(listOf("Voix 1", 64, 100.0), host.audio.last().drop(1))
        assertEquals("« Voix 1 » est sur une nouvelle piste audio, à la mesure 5.", r.state.value.message)
        // The transport plays: where it is.
        host.step = 40
        r.send(sel(), StemDestination.Track())
        assertEquals(40, host.audio.last()[2])
        // Stopped: where the stretch plays along the project (2 s into the song at 120 BPM = step 16).
        host.step = -1
        r.send(sel(start = 96_000), StemDestination.Track())
        assertEquals(16, host.audio.last()[2])
        // The song starts a bar later in the project: the same stretch is heard a bar earlier.
        assertEquals(0, StemRouting.stepOf(host.p.copy(remix = host.p.remix.copy(startBar = 1)), sel(start = 96_000)))
        assertEquals(16, StemRouting.stepOf(host.p.copy(remix = host.p.remix.copy(startBar = 1)), sel(start = 192_000)))
        // Following a slower project (100 BPM): stretched, the stretch comes later in seconds but on the same step.
        val slow = host.p.copy(tempoBpm = 100.0, remix = host.p.remix.copy(followTempo = true))
        assertEquals(16, StemRouting.stepOf(slow, sel(start = 96_000)))
        // Another song than the project's: from the start.
        assertEquals(0, StemRouting.stepOf(host.p, sel().copy(setId = "other")))
        // Five minutes at most on a track; two on a pad.
        r.send(sel(end = 96_000 + 48_000L * 200), StemDestination.Track(0))
        assertEquals(4, host.audio.size)
        r.send(sel(end = 96_000 + 48_000L * 121), StemDestination.Pad())
        assertTrue(host.pads.isEmpty())
        assertEquals("Trop long pour un pad : 120 secondes au plus. Choisis un morceau plus court.", r.state.value.message)
    }

    @Test
    fun refusalsAreSaidAndNothingChanges() {
        val host = Host(beat())
        val r = router(host)
        host.busy = true
        r.send(sel(), StemDestination.Pad())
        assertEquals("Patiente un instant, un son est en cours de préparation.", r.state.value.message)
        host.busy = false
        r.dismissMessage()
        assertNull(r.state.value.message)
        r.send(sel(start = 10, end = 10), StemDestination.Pad())
        assertTrue(r.state.value.message!!.startsWith("Choisis d'abord"))
        // A silent stretch: nothing placed.
        val quiet = Host(beat(), Source(level = { _, _ -> 0f }))
        router(quiet).let {
            it.send(sel(), StemDestination.Pad())
            assertTrue(quiet.pads.isEmpty())
            assertEquals("Ce morceau de la piste est muet : rien n'a été posé.", it.state.value.message)
        }
        // The song gone from the phone.
        val gone = Host(beat(), null)
        router(gone).let {
            it.send(sel(), StemDestination.Pad())
            assertTrue(gone.pads.isEmpty())
            assertEquals("Les pistes de ce morceau ne sont plus sur l'appareil : sépare-le à nouveau.", it.state.value.message)
        }
        // A failure is said, and the board is free again.
        host.fail = java.io.IOException("disque plein")
        r.send(sel(), StemDestination.Pad())
        assertEquals("Ça n'a pas marché : disque plein", r.state.value.message)
        assertNull(r.state.value.busy)
        assertTrue(host.pads.isEmpty())
    }

    @Test
    fun theVoiceCutIntoItsPhrasesOnFreePadsInOneStep() {
        // In the energy's frames (6 kHz): 1 s, 2 s and 0.5 s long.
        val edits = Edits(listOf(6_000L until 12_000L, 30_000L until 42_000L, 60_000L until 63_000L))
        val host = Host(beat().let { it.copy(drums = it.drums.withSample(3, PadSample("v.wav", "Voix 4"))) }, Source(firstBeatFrame = 4_000))
        val r = router(host, edits)
        r.chop("song", 3, max = 8)
        // One call: one undo step for every pad.
        assertEquals(1, host.pads.size)
        val cuts = host.pads.single()
        // Pads 1, 3 (beat), 4 (a sound), 6 (the other scene) are never covered.
        assertEquals(listOf(1, 4, 6), cuts.map { it.lane })
        assertEquals(listOf("Voix 5", "Voix 6", "Voix 7"), cuts.map { it.name })
        assertEquals(listOf(48_000, 96_000, 24_000), cuts.map { it.pcm.frames })
        // The second phrase starts 5 s into the song (30 000 × 8 frames).
        assertEquals(((3 * 1_000_000 + 240_000) * 1e-7).toFloat(), cuts[1].pcm.data[0], 1e-6f)
        assertEquals("3 phrases sur les pads 2, 5 et 7.", r.state.value.message)
        // The energy: mono at 6 kHz, the whole song; the beat grid in its frames (120 BPM, first beat at frame 4000).
        assertEquals("[6000, 1, 180000, 3000, 8, 500]", edits.asked.toString())
    }

    @Test
    fun morePhrasesThanFreePadsTheLongestGo() {
        val edits = Edits(listOf(0L until 1_000L, 2_000L until 9_000L, 10_000L until 14_000L))
        val p = beat().let { q -> q.copy(drums = (0 until 16).filter { it !in listOf(7, 9) }.fold(q.drums) { d, i -> d.withSample(i, PadSample("s$i.wav", "Son $i")) }) }
        val host = Host(p)
        val r = router(host, edits)
        r.chop("song", 3)
        // The two longest, in song order, on pads 8 and 10.
        assertEquals(listOf(7, 9), host.pads.single().map { it.lane })
        assertEquals(listOf(56_000, 32_000), host.pads.single().map { it.pcm.frames })
        assertEquals("2 phrases sur les pads 8 et 10. Plus de pad libre pour la dernière.", r.state.value.message)
        // None found: said.
        val none = Host(beat())
        router(none, Edits()).let {
            it.chop("song", 3)
            assertTrue(none.pads.isEmpty())
            assertEquals("Aucune phrase trouvée sur cette piste : elle est presque muette.", it.state.value.message)
        }
        // No free pad at all: said before anything is read.
        val full = Host(beat().let { q -> q.copy(drums = (0 until 16).fold(q.drums) { d, i -> d.withSample(i, PadSample("s$i.wav", "S")) }) })
        router(full, edits).let {
            it.chop("song", 3)
            assertTrue(full.source!!.reads.isEmpty())
            assertEquals("Tous les pads ont déjà un son : vide un pad dans Batterie, puis recommence.", it.state.value.message)
        }
    }

    @Test
    fun theEnergyKeepsEachWindowsPower() {
        // 0.5 on both sides for 1.5 s, then silence; read in pieces of 10 s.
        val src = Source(frames = 48_000L * 25 + 3, level = { _, f -> if (f < 72_000) 0.5f else 0f })
        val e = StemRouting.energy(src, 2, 8)
        assertEquals(1, e.channels)
        assertEquals(6_000, e.sampleRate)
        assertEquals((48_000 * 25 + 3 + 7) / 8, e.frames)
        assertEquals(0.5f, e.data[0], 1e-6f)
        assertEquals(0.5f, e.data[8_999], 1e-6f)
        assertEquals(0f, e.data[9_000], 0f)
        assertEquals(0f, e.data.last(), 0f)
        assertEquals(3, src.reads.size)
        assertTrue(src.reads.all { it.endsWith(":false") })
    }

    @Test
    fun namesAndPadsAsSaid() {
        assertEquals("Voix 4", StemRouting.nextName(listOf("Voix 1", "Voix 3", "Voix · Ma chanson", "Voix 2b"), "Voix"))
        assertEquals("Basse 1", StemRouting.nextName(listOf("Voix 1"), "Basse"))
        assertEquals("9 à 14", StemRouting.padsText(listOf(13, 8, 9, 10, 11, 12)))
        assertEquals("3, 5 et 9", StemRouting.padsText(listOf(2, 4, 8)))
        assertEquals("7", StemRouting.padsText(listOf(6)))
    }

    @Test
    fun aStretchHeardAloneWithItsPlayHead() {
        val host = Host(beat(), Source(frames = 48_000L * 200))
        val player = Player()
        val r = router(host, Edits(), player)
        val s = sel()
        r.play(s)
        assertEquals(1, host.paused)
        // Heard as it is in the song (not fitted to the project), the whole stretch.
        assertEquals("3:96000:24000:false", host.source!!.reads.single())
        assertEquals(24_000, player.played!!.frames)
        assertEquals(s, r.state.value.playing)
        player.heard = 4_800
        val until = System.currentTimeMillis() + 5_000
        while (r.state.value.playFrame != 96_000L + 4_800 && System.currentTimeMillis() < until) Thread.sleep(5)
        assertEquals(96_000L + 4_800, r.state.value.playFrame)
        // Played to its end: nothing plays.
        player.ended!!.invoke()
        assertNull(r.state.value.playing)
        assertEquals(-1L, r.state.value.playFrame)
        // Again: it plays; once more: it stops.
        r.play(s)
        r.play(s)
        assertNull(r.state.value.playing)
        assertTrue(player.stops >= 2)
        // A long stretch: its first minute.
        r.play(sel(start = 0, end = 48_000L * 200))
        assertEquals(48_000 * 60, player.played!!.frames)
        r.stop()
        assertNull(r.state.value.playing)
    }

    @Test
    fun theRemixListeningStopsFirst() {
        val calls = ArrayList<String>()
        val host = Host(beat())
        val player = object : GuideListenPlayer {
            override fun play(pcm: Pcm, ended: () -> Unit): Boolean { calls += "play"; return true }
            override fun stop() = Unit
        }
        val actions = StemBoardRouter(scope, host, Edits(), player).beforePlay { calls += "remix stop" }
        actions.play(sel())
        assertEquals(listOf("remix stop", "play"), calls)
        assertEquals(sel(), actions.state.value.playing)
        actions.stop()
    }
}
