package com.pocketstudio.app.remix

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.WhatPlaysTest
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.bench.RecordingSink
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.projects.ProjectsViewModel
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.partSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Remix lot 1 (Mehdi, f §1): two phrases of a song's voice on pads, put in the Drop of a jump up,
 * sounded in the Drop looped but not on the Drop's first bar in the whole song, nor in its export:
 * that bar plays a copy of the Drop's drums with a crash, made when the song was built.
 *
 * The song is made as the assistant makes it, the phrases are put on two free pads and written in
 * the Drop as the drum page writes them (the Drop shown, a touch per step), then the song is
 * exported as « Partager › Morceau » exports it, with a [RecordingSink] in place of the engine.
 *
 * - Always: the export's own commands. Each part of the song sends the engine its drums; on every
 *   bar of the Drop the pads hold the phrase of the Drop's bar that falls there (the crash's bar
 *   too).
 * - With `-Pbench.render=<ps_render>` (built by scripts/song-bench.sh): the export is rendered
 *   through the engine core on Linux, and each phrase's pad is heard on its bars of the Drop.
 */
@RunWith(RobolectricTestRunner::class)
class VoiceInWholeSongTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val out = File(System.getProperty("java.io.tmpdir"), "voice-in-song-${System.nanoTime()}")

    @After
    fun tearDown() {
        NativeEngine.sink = null
        // With ps_render (a bench run), the export's commands and renders stay to be listened to.
        if (System.getProperty("ps.bench.render").isNullOrBlank()) out.deleteRecursively() else println("render: kept in $out")
    }

    private fun waitFor(what: String, seconds: Int = 60, condition: () -> Boolean) {
        val until = System.currentTimeMillis() + seconds * 1000L
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "waited too long for $what" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    /** A sung phrase, as « → Pad » leaves it: 1 s of a 440 Hz tone. */
    private val phrase = Pcm(FloatArray(2 * 48_000) { k -> 0.4f * kotlin.math.sin(2 * Math.PI * 440.0 * (k / 2) / 48_000).toFloat() }, 2, 48_000)

    @Test
    fun theVoicePutInTheDropIsInTheExportOfTheWholeSong() {
        val made = WhatPlaysTest.wizard("jump_up")
        val drop = made.allScenes.indexOfFirst { it.label == "Drop" }
        // The phrases on pads 15 and 16 (Mehdi's Conga and Zap), as « → Pad » puts them.
        val song = made.copy(drums = made.drums.copy(lanes = made.drums.lanes.mapIndexed { i, l ->
            when (i) {
                A -> l.copy(sample = PadSample("voix-a.wav", "Voix · Ma chanson préférée"), hits = emptyMap())
                B -> l.copy(sample = PadSample("voix-b.wav", "Voix · Ma chanson préférée"), hits = emptyMap())
                else -> l
            }
        }), scenes = made.scenes.map { s -> s.copy(drums = s.drums.copy(lanes = s.drums.lanes.mapIndexed { i, l -> if (i == A || i == B) l.copy(hits = emptyMap()) else l })) },
            // The two pads alone (solo, as « S » in the mixer): the render then holds the phrases only.
            // Solos change nothing of the drums the export sends (checked first, below).
            mixer = made.mixer.copy(channels = made.mixer.channels.mapIndexed { ch, c -> if (ch == Mixer.padChannel(A) || ch == Mixer.padChannel(B)) c.copy(solo = true) else c }))

        val renderer = System.getProperty("ps.bench.render")?.takeIf { it.isNotBlank() }?.let(::File)
        val sink = RecordingSink(File(out, "samples"), renderer)
        NativeEngine.sink = sink
        var id: String? = null
        ProjectsViewModel(app).create(song.copy(name = "Bootleg"), mapOf("voix-a.wav" to phrase, "voix-b.wav" to phrase)) { id = it }
        waitFor("the project") { id != null }
        CurrentProject.set(app, id!!)
        val vm = RhythmViewModel(app)

        // Written as the drum page writes them: the Drop shown, pad 16 on its bar 1, pad 15 on its bar 3.
        vm.selectScene(drop)
        assertEquals(drop, vm.project.value.scene)
        vm.toggleHit(B, 0)
        vm.toggleHit(A, 2 * 16)
        val p = vm.project.value

        val job = OfflineExport()
        var result: ExportResult? = null
        val cmd = File(out, "song.pscmd")
        CoroutineScope(Dispatchers.Main).launch { result = vm.renderSongForBench(job, cmd) }
        waitFor("the export") { result != null }
        job.close()
        assertEquals(ExportResult.Ok, result)

        // What each bar of the first Drop must hold: the phrase of the Drop's bar that falls there.
        val bars = dropBars(p, drop)
        assertTrue(bars.size >= 8)
        val heard = heardBars(p, File(cmd.path).readLines())
        for ((songBar, loopBar) in bars) {
            assertEquals("song bar ${songBar + 1} (Drop bar ${loopBar + 1}): pad 16", loopBar == 0, heard[songBar].getValue(B))
            assertEquals("song bar ${songBar + 1} (Drop bar ${loopBar + 1}): pad 15", loopBar == 2, heard[songBar].getValue(A))
        }
        // Mehdi's case: the Drop's very first bar (its crash) starts on the first phrase.
        assertTrue(heard[bars.first().first].getValue(B))

        // The export rendered by the engine core: each phrase is heard on its bars of the Drop, and only there.
        if (renderer != null) {
            val wav = File(out, "song.wav")
            val process = ProcessBuilder(renderer.absolutePath, "--no-limiter", cmd.absolutePath, wav.absolutePath).redirectErrorStream(true).start()
            val log = process.inputStream.bufferedReader().readText()
            assertEquals(log, 0, process.waitFor())
            val audio = floatWav(wav)
            val sixteenth = 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE
            val levels = bars.map { (songBar, at) ->
                // The first half second of the bar: a phrase starts on its first beat.
                val from = Math.round(songBar * p.stepsPerBar * sixteenth).toInt()
                val level = rms(audio, from, from + OfflineExport.SAMPLE_RATE / 2)
                println("render: song bar ${songBar + 1} (Drop bar ${at + 1}): rms %.4f".format(level))
                Triple(songBar, at, level)
            }
            val loud = levels.filter { it.second == 0 || it.second == 2 }.minOf { it.third }
            val quiet = levels.filter { it.second != 0 && it.second != 2 }.maxOf { it.third }
            assertTrue("a phrase is missing from its bar (quietest %.4f)".format(loud), loud > 0.02)
            assertTrue("a phrase is heard where none was put (%.4f)".format(quiet), quiet < loud * 0.1)
            // The Drop's first bar, its crash's: the first phrase.
            assertTrue(levels.first().third > 0.02)
        }
    }

    /** The song bars of the first Drop and the bar of the Drop's loop each one stands for. */
    private fun dropBars(p: Project, drop: Int): List<Pair<Int, Int>> {
        val loop = (p.allScenes[drop].drums.steps / p.stepsPerBar).coerceAtLeast(1)
        val scenes = p.song.flatMap { part -> List(p.partSteps(part) / p.stepsPerBar) { part.scene } }
        val start = scenes.indexOf(drop)
        val end = (start until scenes.size).firstOrNull { scenes[it] != drop } ?: scenes.size
        return (start until end).map { it to (it - start) % loop }
    }

    /**
     * For each bar of the song, whether pads [A] and [B] start it with a hit, as the export's commands
     * have the engine play it: each part's drums (sent before the song for the first part, then into
     * the waiting slot for each next one), its loop's length and where the drums carry on from.
     */
    private fun heardBars(p: Project, lines: List<String>): List<Map<Int, Boolean>> {
        class Drums(var steps: Int = 16, var offset: Int = 0, val hits: Array<BooleanArray> = Array(2) { BooleanArray(256) })
        val parts = ArrayList<Drums>()
        var current = Drums()
        var staging = false
        for (line in lines) {
            val w = line.split(' ')
            if (w[0] != "C") continue
            val type = w[1].toInt()
            val a = w[3].toInt()
            val b = w[4].toInt()
            when (type) {
                CMD_STAGING -> {
                    val on = w[2].toFloat() > 0f
                    if (on && !staging) {
                        parts += current
                        current = Drums()
                    }
                    staging = on
                }
                CMD_SET_PATTERN_STEPS -> current.steps = a
                CMD_SLOT_OFFSET -> if (a == 0) current.offset = b
                CMD_SET_DRUM_HIT -> if (a == A || a == B) current.hits[if (a == A) 0 else 1][b and 0xffff] = w[2].toFloat() > 0f
            }
        }
        parts += current
        assertEquals("one set of drums per part", p.song.size, parts.size)
        val spb = p.stepsPerBar
        val out = ArrayList<Map<Int, Boolean>>()
        p.song.forEachIndexed { i, part ->
            val d = parts[i]
            for (k in 0 until p.partSteps(part) / spb) {
                val step = (d.offset + k * spb) % d.steps.coerceAtLeast(1)
                out += mapOf(A to (part.plays(SongPart.DRUMS) && d.hits[0][step]), B to (part.plays(SongPart.DRUMS) && d.hits[1][step]))
            }
        }
        return out
    }

    private fun floatWav(file: File): FloatArray {
        val bytes = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        var at = 12
        var channels = 2
        while (at + 8 <= bytes.limit()) {
            val id = String(ByteArray(4) { bytes.get(at + it) }, Charsets.US_ASCII)
            val size = bytes.getInt(at + 4)
            if (id == "fmt ") channels = bytes.getShort(at + 10).toInt()
            if (id == "data") {
                val n = size / 4 / channels
                return FloatArray(n) { f -> bytes.getFloat(at + 8 + f * 4 * channels) }
            }
            at += 8 + size + (size and 1)
        }
        error("no data in $file")
    }

    private fun rms(x: FloatArray, from: Int, to: Int): Double {
        val a = from.coerceIn(0, x.size)
        val b = to.coerceIn(a, x.size)
        if (b <= a) return 0.0
        var sum = 0.0
        for (i in a until b) sum += x[i].toDouble() * x[i]
        return Math.sqrt(sum / (b - a))
    }

    private companion object {
        /** Pads 15 and 16: the free pads Mehdi chose. */
        const val A = 14
        const val B = 15
        // NativeEngine's command numbers (engine/src/main/cpp/core/Engine.h), as the .pscmd writes them.
        const val CMD_SET_PATTERN_STEPS = 9
        const val CMD_SET_DRUM_HIT = 10
        const val CMD_STAGING = 39
        const val CMD_SLOT_OFFSET = 73
    }
}
