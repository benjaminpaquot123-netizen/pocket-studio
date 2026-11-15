package com.pocketstudio.app.rhythm

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Tour 7, lot 4, D1 (U28, seat 3a #41, #50, #61): in the Rhythm, part B picked to edit, « ▶ le morceau » played the
 * song from B. The button says the song: it plays from its first bar (A then B); « ▶ cette partie » is the part's.
 */
@RunWith(RobolectricTestRunner::class)
class SongButtonFromStartTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    /** Two scenes, the song A B, stopped. */
    private fun twoParts(): RhythmViewModel {
        File(app.filesDir, "projects").deleteRecursively()
        val vm = RhythmViewModel(app)
        vm.toggleHit(0, 0)
        vm.duplicateScene()
        vm.selectScene(0)
        listOf(0, 1).forEach(vm::addSongPart)
        return vm
    }

    @Test
    fun partBPickedTheSongButtonStillPlaysFromBarOne() {
        val vm = twoParts()
        vm.selectScene(1)
        vm.playStartsForTest = mutableListOf()
        vm.toggleWholeSong()
        assertEquals("« ▶ le morceau » with B shown: the song from its first part", listOf("song 0"), vm.playStartsForTest)
    }

    @Test
    fun aPartChosenInPlayFromIsStillWhereTheSongStarts() {
        val vm = twoParts()
        vm.playSongPart(1)
        vm.playStartsForTest = mutableListOf()
        vm.toggleWholeSong()
        assertEquals("« Jouer depuis… › B » is a choice: kept", listOf("song 1"), vm.playStartsForTest)
    }
}
