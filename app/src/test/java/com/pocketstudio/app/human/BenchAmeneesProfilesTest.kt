package com.pocketstudio.app.human

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Audit ultra round 1 (lot B seat c, lot E seat c): the amenées failed before the tester got the seat (« welcome_remix »
 * introuvable in Mode Pro, where the app opens on « Que veux-tu faire ? »; Pad Live off the screen on a tablet in landscape,
 * the bench's swipes on the screen's margin, outside the welcome's centred column). Each amenée on each start.txt profile
 * of the round (device, font, mode, language, hand, lit), as a seat gets it.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchAmeneesProfilesTest(private val amenee: String, private val start: String) : HumanTester("amenees-profils") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @Test
    fun theAmeneeIsReady() {
        val (o, warnings) = SeatOptions.parse(start.split(' ').associate { it.substringBefore('=') to it.substringAfter('=') } + ("amenee" to amenee))
        assertTrue(warnings.toString(), warnings.isEmpty())
        begin(o)
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        val failed = runCatching { amenee(amenee, journal) }.exceptionOrNull()
        val lines = journal.readLines()
        assertTrue("$amenee on « $start »: ${failed?.message}\n${lines.joinToString("\n")}\n${screenLines().joinToString("\n")}", failed == null && lines.last().startsWith("#0 AMENEE prête"))
        when (amenee) {
            AMENEE_REMIX -> assertTrue("« Choisir une chanson »", centerOf("remix_pick") != null)
            AMENEE_PADLIVE -> assertTrue("the ready-to-play pads", centerOf("padlive") != null && project()?.live?.at(4) != null)
            AMENEE_SONG -> assertTrue("a project's tabs", screenLines().any { it.startsWith("onglet") })
        }
        // The mode the seat asked for is the one the tester gets.
        o.simple?.let { assertTrue("mode", com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode == it) }
    }

    companion object {
        /**
         * The round's start.txt profiles (lots A to F): its seven devices, both modes, its font sizes, `lit=non`, one hand
         * (left, right), right to left, Japanese; the two that failed first.
         */
        private val PROFILES = listOf(
            "lang=fr device=phone411 font=100 mode=pro main=gauche",
            "lang=fr device=tablet11Land font=100 mode=simple lit=non",
            "lang=fr device=phone font=200 mode=simple lit=non",
            "lang=de device=phone360 font=100 mode=pro main=une",
            "lang=fr device=tablet11Land font=100 mode=pro theme=daltonien",
            "lang=fr device=tablet8 font=130 mode=pro",
            "lang=fr device=phoneLand font=100 mode=pro",
            "lang=fr device=tablet8 font=200 mode=simple",
            "lang=ar device=foldable font=100 mode=simple",
            "lang=ja device=phone411 font=100 mode=simple theme=night",
        )

        /**
         * Banc fiable 3.2 (control 13): the profiles of the lot about to be played, when scripts/bench/selftest.sh passes
         * its seats' start.txt (-Pbench.profiles=<file>, one « k=v k=v » line per seat, the bench's own keys only);
         * the round-1 list above otherwise.
         */
        private fun lotProfiles(): List<String> {
            val file = System.getProperty("ps.bench.profiles")?.takeIf { it.isNotBlank() }?.let(::File) ?: return PROFILES
            return file.readLines().map { it.trim() }.filter { it.isNotEmpty() }.distinct().ifEmpty { PROFILES }
        }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
        fun cases(): List<Array<Any>> = lotProfiles().let { profiles -> SeatOptions.AMENEES.flatMap { a -> profiles.map { arrayOf<Any>(a, it) } } }
    }
}
