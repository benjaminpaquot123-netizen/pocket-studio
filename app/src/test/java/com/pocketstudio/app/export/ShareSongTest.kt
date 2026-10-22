package com.pocketstudio.app.export

import com.pocketstudio.app.rhythm.ExportFade
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.pocketstudio.model.addAudioTrack
import com.pocketstudio.model.audioEndBars
import com.pocketstudio.model.withAudioTrack

class ShareSongTest {
    @Test
    fun theExportDefaultsToTheSongWhenThereIsOne() {
        val withSong = ExportUi().forProject(sceneBars = 2, hasSong = true)
        assertEquals(ExportWhat.Song, withSong.what)
        assertEquals(ExportFade.Short, withSong.fade)
        val loop = ExportUi().forProject(sceneBars = 2, hasSong = false)
        assertEquals(ExportWhat.Loop, loop.what)
        assertEquals(ExportFade.None, loop.fade)
        assertEquals(2, loop.bars)
    }

    @Test
    fun theUsersChoicesStay() {
        // The scene picked: it stays even once a song exists; a fade picked stays for any what.
        val picked = ExportUi(what = ExportWhat.Loop, whatChosen = true).forProject(4, hasSong = true)
        assertEquals(ExportWhat.Loop, picked.what)
        assertEquals(ExportFade.None, picked.fade)
        val longFade = ExportUi(fade = ExportFade.Long, fadeChosen = true).forProject(4, hasSong = false)
        assertEquals(ExportFade.Long, longFade.fade)
        // Separate tracks never fade (they are to be mixed again elsewhere).
        val stems = ExportUi(what = ExportWhat.Stems, whatChosen = true, fade = ExportFade.Long, fadeChosen = true).forProject(4, hasSong = true)
        assertEquals(ExportFade.None, stems.fade)
        assertEquals(0L, stems.fadeFrames(48_000L * 60))
    }

    @Test
    fun theFadeLastsItsSecondsButNeverMoreThanHalfTheFile() {
        val rate = OfflineExport.SAMPLE_RATE.toLong()
        val song = ExportUi().forProject(4, hasSong = true)
        assertEquals(3 * rate, song.fadeFrames(60 * rate))
        assertEquals(8 * rate, song.copy(fade = ExportFade.Long).fadeFrames(60 * rate))
        assertEquals(2 * rate, song.copy(fade = ExportFade.Long).fadeFrames(4 * rate))
        assertEquals(0L, song.copy(fade = ExportFade.None).fadeFrames(60 * rate))
    }

    @Test
    fun sharingIsTheWholeSongInM4aWithAShortFade() {
        val s = ShareSong.settings(hasSong = true, sceneBars = 4)
        assertEquals(ExportWhat.Song, s.what)
        assertEquals(ExportFormat.M4a, s.format)
        assertEquals(ExportFade.Short, s.fade)
        // Whatever the card's choices: they are not the share's.
        assertEquals(s.what, s.forProject(4, hasSong = true).what)
        // No song yet: the scene looped long enough to sound like a piece, in whole scenes.
        val loop = ShareSong.settings(hasSong = false, sceneBars = 3)
        assertEquals(ExportWhat.Loop, loop.what)
        assertEquals(9, loop.bars)
        assertEquals(ExportFade.Short, loop.forProject(3, hasSong = false).fade)
    }

    @Test
    fun aSceneIsLoopedToAtLeastEightBars() {
        assertEquals(8, ShareSong.loopBars(1))
        assertEquals(8, ShareSong.loopBars(4))
        assertEquals(10, ShareSong.loopBars(5))
        assertEquals(16, ShareSong.loopBars(16))
        assertEquals(64, ShareSong.loopBars(64))
        assertEquals(8, ShareSong.loopBars(0))
    }

    @Test
    fun withoutASongTheShareLastsAsLongAsTheVoice() {
        // A 27 s take at 140 BPM is 15.75 bars: the 8 bars of the scene would cut it.
        var p = com.pocketstudio.model.Project(tempoBpm = 140.0).addAudioTrack()!!
        p = p.withAudioTrack(p.audioTracks[0].copy(clip = com.pocketstudio.model.AudioClip("v.wav", "Prise", 27.0)))
        assertEquals(16, p.audioEndBars())
        assertEquals(16, ShareSong.settings(hasSong = false, sceneBars = 4, audioBars = p.audioEndBars()).bars)
        // Starting on bar 3 it ends on bar 18; a muted track does not count; never past 64 bars.
        val later = p.withAudioTrack(p.audioTracks[0].copy(clip = p.audioTracks[0].clip!!.copy(start = 32)))
        assertEquals(18, later.audioEndBars())
        val ch = com.pocketstudio.model.AudioTrack.mixerChannel(0)
        val muted = later.copy(mixer = later.mixer.copy(channels = later.mixer.channels.mapIndexed { i, c -> if (i == ch) c.copy(mute = true) else c }))
        assertEquals(0, muted.audioEndBars())
        assertEquals(64, ShareSong.loopBars(4, audioBars = 90))
    }

    @Test
    fun defaultNamesAreAskedForARealOne() {
        listOf("", "  ", "Nouveau projet", "Mon projet 3", "Mon premier rythme", "Mon morceau", "Lo-fi", StarterWizard.STYLES.first().name)
            .forEach { assertTrue("« $it »", ShareSong.isDefaultName(it)) }
        listOf("Da Funk (remix)", "Acide du dimanche", "Mon projet de l'été", "House du samedi")
            .forEach { assertFalse("« $it »", ShareSong.isDefaultName(it)) }
        assertEquals("Mon morceau", ShareSong.suggestedName("Mon projet 3"))
        assertEquals("Mon morceau", ShareSong.suggestedName(""))
        assertEquals("House", ShareSong.suggestedName("House"))
    }

    @Test
    fun aDefaultNameGivenInAnotherLanguageIsStillOne() {
        // "My song 2" (English) or "Mijn nummer" (Dutch): not chosen by the user, whatever the language now.
        listOf("My song 2", "Mijn nummer", "Mon morceau 4").forEach { assertTrue("« $it »", ShareSong.isDefaultName(it)) }
        try {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.EN
            assertEquals("My song", ShareSong.suggestedName("Mon projet 3"))
            assertTrue(ShareSong.isDefaultName("Mon morceau"))
        } finally {
            com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        }
    }
}
