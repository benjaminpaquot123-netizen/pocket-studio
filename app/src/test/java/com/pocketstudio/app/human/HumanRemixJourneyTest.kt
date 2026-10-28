package com.pocketstudio.app.human

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
        repeat(8) {
            val at = centerOf(tag) ?: error("no $tag")
            if (at.second in 320f..(screenHeight - 260f)) return at
            val dy = (at.second - screenHeight / 2f).coerceIn(-500f, 500f)
            // On the page's margin (not on a slider or a waveform), then the scroll comes to rest.
            act("glisse 10 ${screenHeight / 2f + dy / 2} 10 ${screenHeight / 2f - dy / 2}")
            act("attends 1")
        }
        return centerOf(tag)!!
    }

    @Test
    fun aSongsVoiceGoesIntoTheProjectWithTouchesOnly() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        begin()
        // A new project, as a person who knows what they want: Complet, Partir de zéro.
        tap(centerOfText("Complet"), "Complet")
        tap(centerOfText("Partir de zéro"), "Partir de zéro")
        // The Remix, from the sounds' strip.
        tap(centerOfText("Remix"), "the Remix chip")
        assertNotNull("the Remix page is not there", centerOf("remix_pick"))

        // « Choisir une chanson »: the phone's picker gives the song of the test.
        val picked = tap(centerOf("remix_pick"), "Choisir une chanson")
        assertTrue(picked, picked.contains("« Ma chanson préférée.wav »"))

        // The model comes down, the song is read and separated: each state is on the screen.
        val seen = LinkedHashSet<String>()
        val until = System.currentTimeMillis() + 120_000
        while (SeparationJob.state.value.busy || SeparationJob.state.value == SeparationState.Idle && seen.isEmpty()) {
            assertTrue("the separation never ended", System.currentTimeMillis() < until)
            when (SeparationJob.state.value) {
                is SeparationState.DownloadingModel -> seen += "download"
                is SeparationState.Decoding -> seen += "decode"
                is SeparationState.Separating -> seen += "separate"
                is SeparationState.Analysing -> seen += "analyse"
                else -> Unit
            }
            if (centerOf("remix_progress") != null) seen += "progress shown"
            act("attends 1")
        }
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
        tap(reach("listen_${set.id}"), "Arrêter")

        // « Jouer avec le projet »: the song plays along the project.
        tap(reach("use_${set.id}"), "Jouer avec le projet")
        assertEquals(set.id, project()?.remix?.setId)
        act("attends 1")
        // Its four parts are drawn (the screen follows: nothing froze it).
        assertNotNull("no waveform", centerOf("waveform_3"))
        // The voice alone (S), then ▶.
        tap(reach("stem_solo_3"), "S of Voix")
        val playing = tap(reach("remix_play"), "Jouer")
        assertTrue(playing, playing.contains("la musique du projet joue en boucle") && playing.contains("avec la voix seule de « Ma chanson préférée » (solo)"))
        assertNotNull("the button does not say Arrêter", centerOfText("Arrêter", substring = true))

        // Stopped (■), its first phrase onto a pad: from bar 3, two bars.
        tap(reach("remix_play"), "Arrêter")
        tap(reach("remix_bar_next"), "+")
        tap(reach("remix_bar_next"), "+")
        tap(centerOfText("2 mes."), "2 mes.")
        val padded = tap(reach("stem_pad_3"), "→ Pad of Voix")
        act("attends 1")
        val lane = project()!!.drums.lanes[0]
        assertTrue("${project()!!.drums.lanes.map { it.sample?.name }} ${rhythm()?.sampleTask?.value} ${project()!!.live.clips.map { it?.name }} sel=${rhythm()?.transport?.value?.selectedPad}", lane.sample?.name == "Voix 1")
        assertTrue(padded, padded.contains("Le pad 1 reçoit « Voix 1 »"))
        // Two bars from bar 3: the first phrase, sung (4 s at 120 BPM).
        val file = app.filesDir.walk().first { it.name == lane.sample!!.file }
        val pad = FloatWav.read(file)
        assertEquals(4.0, pad.data.size / 2.0 / pad.sampleRate, 0.05)
        assertTrue("the pad is silent", rms(pad.data, pad.sampleRate, 0.2, 2.8) > 0.02)
        // The pad in the beat: two steps on the Batterie page, then ▶ back on the Remix page.
        tap(centerOfText("16 pads", substring = true), "the Batterie chip")
        tap(centerOf("step_0"), "step 1")
        tap(centerOf("step_8"), "step 9")
        assertTrue("no hits on pad 1", project()!!.drums.lanes[0].hits.size == 2)
        tap(centerOfText("Remix"), "the Remix chip")
        val loop = tap(reach("remix_play"), "Jouer")
        assertTrue(loop, loop.contains("le pad 1 (« Voix 1 ») sonne dans la boucle"))
        assertTrue(loop, loop.contains("avec la voix seule de « Ma chanson préférée », depuis sa mesure 3 (solo)"))
        // Leaving the page while it plays: the loop goes on, the screen still follows.
        val away = tap(centerOfText("16 pads", substring = true), "the Batterie chip")
        assertTrue(away, away.contains("la musique du projet joue en boucle"))
        assertNotNull(centerOf("step_0"))
        assertTrue("the screen failed: $problems", problems.isEmpty())
        shot(File(System.getProperty("java.io.tmpdir"), "remix-journey-end.png"))
    }

    private fun rms(stereo: FloatArray, rate: Int, from: Double, to: Double): Double {
        val a = (from * rate).toInt() * 2
        val b = (to * rate).toInt() * 2
        var sum = 0.0
        for (i in a until b) sum += stereo[i].toDouble() * stereo[i]
        return sqrt(sum / (b - a))
    }
}
