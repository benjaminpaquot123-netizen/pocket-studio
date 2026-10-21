package com.pocketstudio.app.library

import com.pocketstudio.model.StarterWizard
import org.junit.Assert.assertEquals
import org.junit.Test

class ChooseSoundPreviewTest {
    private val house = StarterWizard.style("house")!!
    private val kick = house.lines.first { it.role.id == "kick" }.role
    private val hat = house.lines.first { it.role.id != "kick" }.role
    private val options = soundCategories(kick, house.id, emptyList()).first().options

    @Test
    fun pickingAnotherSoundWhileHeardWithTheBeatKeepsTheBeatGoing() {
        val playing = SoundRef.LineDemo(kick, options[0])
        assertEquals(SoundRef.LineDemo(kick, options[1]), soundOnChoose(playing, kick, options[1]))
    }

    @Test
    fun otherwiseTheSoundIsHeardAlone() {
        assertEquals(options[1].ref, soundOnChoose(null, kick, options[1]))
        assertEquals(options[1].ref, soundOnChoose(options[0].ref, kick, options[1]))
        // The beat heard for another line does not carry over.
        assertEquals(options[1].ref, soundOnChoose(SoundRef.LineDemo(hat, options[0]), kick, options[1]))
    }
}
