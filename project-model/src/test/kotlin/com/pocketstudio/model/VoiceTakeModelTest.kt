package com.pocketstudio.model

import com.pocketstudio.model.video.VideoPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Final audit, voice lot: « Décaler la prise » apart from a cut, the first take's level and chain, the voice presets, the video's part. */
class VoiceTakeModelTest {
    @Test
    fun theManualOffsetIsKeptApartFromWhatACutLeft() {
        val clip = AudioClip("a.wav", "Prise", start = 16, song = true)
        assertNull(clip.nudgeMs)
        val nudged = clip.withOffset(50f)
        assertEquals(50f, nudged.offsetMs, 0f)
        assertEquals(50f, nudged.manualOffsetMs, 0f)
        // A cut adds its rest to the offset; the manual part stays what the user set.
        val cut = nudged.afterCutStart(0.04, 120.0)
        assertEquals(90f, cut.offsetMs, 1e-3f)
        assertEquals(50f, cut.manualOffsetMs, 1e-3f)
        // Moved again by hand: both move together.
        val again = cut.withOffset(100f)
        assertEquals(100f, again.offsetMs, 0f)
        assertEquals(60f, again.manualOffsetMs, 1e-3f)
        // A retake keeps only the manual part (the cut's rest belonged to the old recording).
        var p = Project().addAudioTrack("Voix")!!
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = again.copy(file = "old.wav")))
        val q = p.withTake(PendingTake(0, "new.wav", "Prise 2", 4.0, 120.0, song = true, start = 16, replaces = "old.wav"))
        assertEquals(60f, q.audioTracks[0].clip!!.offsetMs, 1e-3f)
        assertEquals(60f, q.audioTracks[0].clip!!.manualOffsetMs, 1e-3f)
    }

    @Test
    fun oldClipsReadTheirWholeOffsetAsManualAndTheFileOnlyGrowsWhenCut() {
        val text = ProjectJson.encode(Project().addAudioTrack("Voix")!!.let { p ->
            p.withAudioTrack(p.audioTracks[0].copy(clip = AudioClip("a.wav", "Prise", offsetMs = -15f)))
        })
        assertFalse("nudgeMs" in text)
        val back = ProjectJson.decode(text).audioTracks[0].clip!!
        assertNull(back.nudgeMs)
        assertEquals(-15f, back.manualOffsetMs, 0f)
        val cut = back.afterCutStart(0.04, 120.0)
        val round = ProjectJson.decode(ProjectJson.encode(Project().addAudioTrack("Voix")!!.let { p -> p.withAudioTrack(p.audioTracks[0].copy(clip = cut)) }))
        assertEquals(-15f, round.audioTracks[0].clip!!.manualOffsetMs, 1e-3f)
        // Louder clips are allowed (+12 dB), never beyond.
        assertEquals(3f, AudioClip("a.wav", "x", gain = 3f).sanitized().gain, 0f)
        assertEquals(AudioClip.MAX_GAIN, AudioClip("a.wav", "x", gain = 9f).sanitized().gain, 0f)
    }

    @Test
    fun aFirstTakeGetsItsLevelAndAClearVoiceChain() {
        val p = Project().addAudioTrack("Voix")!!.withTake(PendingTake(0, "t.wav", "Prise", 4.0, 120.0))
        val q = p.withFirstTakeLevel(0, "t.wav", 2.5f)
        assertEquals(2.5f, q.audioTracks[0].clip!!.gain, 0f)
        val strip = q.mixer.channels[AudioTrack.mixerChannel(0)]
        val chain = FxPresets.byId(FxPresets.VOICE_CLEAR)!!
        assertEquals(chain.inserts.map { it.effect }, strip.inserts.filter { it.effect != EffectType.NONE }.map { it.effect })
        // Nothing else of the strip changes.
        val before = p.mixer.channels[AudioTrack.mixerChannel(0)]
        assertEquals(before.volume, strip.volume, 0f)
        assertEquals(before.sends, strip.sends)
        // A strip with effects already keeps them; another file: nothing at all.
        val withFx = p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.mapIndexed { i, c ->
            if (i == AudioTrack.mixerChannel(0)) c.withFx(FxPresets.byId("phone")!!) else c
        }))
        val r = withFx.withFirstTakeLevel(0, "t.wav", 2f)
        assertEquals(withFx.mixer, r.mixer)
        assertEquals(2f, r.audioTracks[0].clip!!.gain, 0f)
        assertEquals(p, p.withFirstTakeLevel(0, "other.wav", 2f))
    }

    @Test
    fun theVoicePresetsComeFirstForARecordedSound() {
        val audio = FxPresets.suggestedFor(SoundKind.AUDIO).map { it.id }
        assertEquals(listOf(FxPresets.VOICE_RAP, FxPresets.VOICE_CLEAR), audio.take(2))
        val rap = FxPresets.byId(FxPresets.VOICE_RAP)!!
        assertEquals(listOf(EffectType.PARAM_EQ, EffectType.COMPRESSOR, EffectType.REVERB), rap.inserts.map { it.effect })
        // A low cut, 4:1 compression and a short, light room.
        assertTrue(rap.inserts[0].params[3] > 0.4f)
        assertTrue(rap.inserts[1].params[1] > 0.4f)
        assertTrue(rap.inserts[2].params[3] < 0.2f)
        assertTrue(rap.inserts.all { i -> i.params.all { it in 0f..1f } })
    }

    /** One bar per scene (2 s at 120 BPM); a voice clip on the track when given. */
    private fun rap(voice: AudioClip?): Project {
        val scenes = listOf("Intro", "Couplet", "Refrain").mapIndexed { i, label ->
            Scene(name = Scene.NAMES[i], drums = DrumPattern().toggle(0, 0), label = label)
        }
        var p = Project(tempoBpm = 120.0, drums = scenes[0].drums, scenes = scenes,
            song = listOf(SongPart(0, 4), SongPart(1, 8), SongPart(2, 8), SongPart(1, 8)))
        if (voice == null) return p
        p = p.addAudioTrack("Voix")!!
        return p.withAudioTrack(p.audioTracks[0].copy(clip = voice))
    }

    @Test
    fun theVideoGoesWhereTheVoiceIsOrWhereTheUserChose() {
        // Without a voice: the Refrain, as before.
        assertEquals(2, VideoPlanner.defaultPart(rap(null)))
        // A song take sung from the Couplet (bar 5, step 64): the video starts there.
        val song = rap(AudioClip("v.wav", "Prise", seconds = 16.0, start = 64, song = true))
        assertEquals(1, VideoPlanner.voicePart(song))
        val plan = VideoPlanner.plan(song, "x")
        assertEquals(1, plan.part)
        assertEquals(4 * 2L * 48_000, plan.startFrame)  // 4 bars of 2 s
        // A scene take on the Couplet's scene: its first part too.
        assertEquals(1, VideoPlanner.voicePart(rap(AudioClip("v.wav", "Prise", seconds = 8.0, scene = 1))))
        // Chosen by hand, and 15 s long.
        val chosen = VideoPlanner.plan(song, "x", length = 15.0, part = 2)
        assertEquals(2, chosen.part)
        assertEquals(12 * 2L * 48_000, chosen.startFrame)
        assertEquals(15.0, chosen.seconds, 1e-9)
        assertEquals("Refrain", chosen.sections.first().label)
        // A part out of the song: back to the default.
        assertEquals(1, VideoPlanner.plan(song, "x", part = 9).part)
    }
}
