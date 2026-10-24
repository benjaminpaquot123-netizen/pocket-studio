package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.ArpMode
import com.pocketstudio.model.NewTracks
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.TrackKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** "+ Piste" asks what to add: a kind of track with its sound, or a melody written in the song's key. */
@RunWith(RobolectricTestRunner::class)
class AddTrackKindTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    private fun freshVm(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        return RhythmViewModel(app)
    }

    @Test
    fun eachChoiceMakesItsTrackSelectedAtOnce() {
        val vm = freshVm()
        TrackKind.entries.forEach { kind ->
            val before = vm.project.value.tracks.map { it.id }.toSet()
            vm.addTrackOf(kind)
            val t = vm.project.value.tracks.first { it.id !in before }
            assertEquals(kind.trackName, t.name)
            assertEquals(t.id, vm.selectedTrack.value)
            if (kind == TrackKind.Arp) assertEquals(ArpMode.UpDown, t.notes.performance.arp)
        }
    }

    @Test
    fun aMelodyWrittenForMeTakesTheProjectsStyleAndIsOneUndoStep() {
        val vm = freshVm()
        // A song the assistant made in Acid house without a melody: named after its style.
        vm.renameProject("Acid house")
        val acidHouse = StarterWizard.style("acid_house")!!
        val before = vm.project.value.tracks.size
        vm.addTrackOf(TrackKind.Melody, write = true)
        val p = vm.project.value
        assertEquals(before + 1, p.tracks.size)
        val t = p.tracks.last()
        assertEquals("Mélodie", t.name)
        assertEquals(StarterWizard.PHRASE_STEPS, t.notes.steps)
        assertTrue(t.notes.notes.isNotEmpty())
        // The style's key, its lead sound.
        assertEquals(acidHouse.melody.root, p.harmony.root)
        t.notes.notes.forEach { assertTrue(p.harmony.inScale(it.pitch)) }
        // The style's sound, at the level of its part (phase 30: GAIN lowered for a lead).
        assertEquals(NewTracks.sound(TrackKind.Melody, acidHouse).synth.sanitized().withGainOf(t.synth), t.synth)
        vm.undo()
        assertEquals(before, vm.project.value.tracks.size)
    }
}
