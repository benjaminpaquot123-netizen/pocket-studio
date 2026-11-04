package com.pocketstudio.app.human

import com.pocketstudio.app.settings.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The scripted starts of the three most frequent missions (Audit ultra §3.1, « amenées », lot P0-A):
 * from the app as just installed to the mission's starting point, as a seat gets it.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchAmeneesTest : HumanTester("amenees") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    private fun bring(name: String, o: SeatOptions): List<String> {
        begin(o.copy(amenee = name))
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        amenee(name, journal)
        return journal.readLines()
    }

    @Test
    fun firstSongStartsInAnEmptyProjectInSimpleMode() {
        val j = bring(AMENEE_SONG, SeatOptions(simple = true))
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertTrue("Mode simple", SettingsStore.state.value.simpleMode)
        assertNotNull("a project is open", project())
        assertTrue(screenLines().joinToString("\n"), screenLines().any { it.startsWith("onglet") })
    }

    @Test
    fun remixStartsOnTheRemixPage() {
        val j = bring(AMENEE_REMIX, SeatOptions())
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertNotNull("« Choisir une chanson »", centerOf("remix_pick"))
    }

    @Test
    fun padLiveStartsWithItsPadsReady() {
        val j = bring(AMENEE_PADLIVE, SeatOptions())
        assertTrue(j.joinToString("\n"), j.last().startsWith("#0 AMENEE prête"))
        assertNotNull(centerOf("padlive"))
        assertNotNull("the ready-to-play pads", project()?.live?.at(4))
        assertEquals(null, centerOf("padlive_busy"))
        // Pad Live covers the project: a person sees its pads, not the drum pads under it.
        val lines = screenLines()
        assertEquals("padlive", screenRoute().substringBefore('/'))
        assertTrue(lines.joinToString("\n"), lines.any { "Klaxon" in it } && lines.none { "Tom médium" in it })
    }
}
