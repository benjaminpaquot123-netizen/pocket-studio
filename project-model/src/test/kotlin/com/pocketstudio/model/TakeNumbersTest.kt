package com.pocketstudio.model

import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Audit ultra round 2, zone 4 (seat B b): n° 18, takes numbered once (« Prise 1 », « Prise 2 ») in an
 * order that a choice never moves; n° 20, the audio track's own name « Audio 1 » in every language.
 */
class TakeNumbersTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private fun take(p: Project, file: String): Project =
        p.withTake(PendingTake(0, file, "Prise 03:37", 6.0, 120.0, replaces = p.audioTracks.firstOrNull { it.id == 0 }?.clip?.file))

    @Test
    fun eachTakeGetsTheNextNumberAndChoosingOneMovesNothing() {
        var p = Project()
        listOf("a.wav", "b.wav", "c.wav").forEach { p = take(p, it) }
        val t = p.audioTracks[0]
        assertEquals(listOf(1, 2, 3), listOf("a.wav", "b.wav", "c.wav").map { f -> (listOfNotNull(t.clip) + t.otherTakes).first { it.file == f }.take })
        val order = t.takesInOrder().map { it.first.file to it.second }
        assertEquals(listOf("a.wav" to 1, "b.wav" to 2, "c.wav" to 3), order)
        // « Prise 1 » chosen: it plays, the order stays.
        val chosen = p.chooseTake(0, t.otherTakes.indexOfFirst { it.file == "a.wav" })
        assertEquals("a.wav", chosen.audioTracks[0].clip?.file)
        assertEquals(order, chosen.audioTracks[0].takesInOrder().map { it.first.file to it.second })
        // A take recorded after: number 4, last.
        assertEquals(4, take(chosen, "d.wav").audioTracks[0].clip?.take)
        // One left out: the others keep their number.
        val forgot = chosen.forgetTake(0, chosen.audioTracks[0].otherTakes.indexOfFirst { it.file == "b.wav" })
        assertEquals(listOf("a.wav" to 1, "c.wav" to 3), forgot.audioTracks[0].takesInOrder().map { it.first.file to it.second })
    }

    @Test
    fun takesSavedBeforeNumbersAreNumberedByNameAndKeptWhenSavedAgain() {
        val old = AudioTrack(0, "Voix", clip = AudioClip("z.wav", "Prise 21:14", 4.0), previous = AudioClip("y.wav", "Prise 21:09", 3.0))
        assertEquals(listOf("y.wav" to 1, "z.wav" to 2), old.takesInOrder().map { it.first.file to it.second })
        assertEquals(3, old.nextTake())
        // The number is saved (and left out of the file when there is none).
        val json = ProjectJson.encode(Project(audioTracks = listOf(old.copy(clip = old.clip!!.copy(take = 7)))))
        assertEquals(7, ProjectJson.decode(json).audioTracks[0].clip?.take)
        assertEquals(0, ProjectJson.decode(json).audioTracks[0].previous?.take)
    }

    @Test
    fun anAudioTracksOwnNameIsShownInTheLanguage() {
        val p = Project().addAudioTrack()!!
        assertEquals("Audio 1", p.audioTracks[0].name)
        I18n.lang = Lang.KO
        assertEquals("오디오 1", DefaultNames.shown("Audio 1"))
        I18n.lang = Lang.ZH_CN
        assertEquals("音频 2", DefaultNames.shown("Audio 2"))
        // A name typed by the user stays as it is.
        assertEquals("Audio guitare", DefaultNames.shown("Audio guitare"))
    }
}
