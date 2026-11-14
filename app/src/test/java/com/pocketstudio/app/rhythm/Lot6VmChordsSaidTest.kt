package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.model.ChordTrack
import com.pocketstudio.model.Harmony
import com.pocketstudio.model.Progression
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scale
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.switchScene
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Lot 6, U14 (seat F a, screen 30): « Appliquer à tout le morceau » says the chords placed, not the whole progression. */
@RunWith(RobolectricTestRunner::class)
class Lot6VmChordsSaidTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun theChordsSaidAreTheOnesPlacedInTheSong() {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        // Two parts of one bar each: a 4-chord progression only places its first chord.
        vm.session.edit { Project(harmony = Harmony(0, Scale.Major), scenes = listOf(Scene(), Scene()), song = listOf(SongPart(1), SongPart(0))).switchScene(0) }
        assertEquals(1, ChordTrack.bars(vm.project.value, 0))
        vm.tryProgression(Progression.Pop)
        vm.applyChords()
        val p = vm.project.value
        val english = com.pocketstudio.app.settings.SettingsStore.state.value.englishNotes
        assertEquals(listOf(0), ChordTrack.chordsOf(p, 0))
        assertEquals(tr("Fait : accords du morceau %s", ChordTrack.name(p, 0, english)), vm.done.value)
    }
}
