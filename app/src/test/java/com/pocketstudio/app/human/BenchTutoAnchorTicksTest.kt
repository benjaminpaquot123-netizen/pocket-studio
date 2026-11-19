package com.pocketstudio.app.human

import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
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
 * Tour 9 (seats t9-02, t9-04, t9-06, t9-08, t9-10, t9-12): `amenee=tuto:<style>:<chapter>` built the skipped chapters
 * into the project and left their steps to do in the tutorial (an example to hear, « C'est fait », a knob to move: only
 * what the project shows was ticked). The seat finished every step of its slice and got « Il te reste 10 étapes », never
 * the « Bravo » its oracle reads. A person who played the chapters before has them ticked: each course, each level.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class BenchTutoAnchorTicksTest(private val style: String, private val level: TutoLevel) : HumanTester("amenees-tuto") {
    @Before
    fun notOnTheBench() = assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())

    @Test
    fun theSkippedChaptersAreTicked() {
        val chapters = TutoCourse.of(style)!!.chaptersFor(level)
        val name = "tuto:$style:${chapters.size}"
        begin(SeatOptions(simple = level == TutoLevel.Facile, amenee = name))
        val journal = File.createTempFile("amenee", ".txt").apply { deleteOnExit() }
        val failed = runCatching { amenee(name, journal) }.exceptionOrNull()
        val lines = journal.readLines()
        assertTrue("$name: ${failed?.message}\n${lines.joinToString("\n")}", failed == null && lines.last().startsWith("#0 AMENEE prête"))
        val progress = PrefsTutoStore(androidx.test.core.app.ApplicationProvider.getApplicationContext()).load(rhythm()!!.projectId)!!
        val left = chapters.dropLast(1).flatMap { it.steps }.map { it.id }.filter { it !in progress.done }
        assertTrue("$name: ${left.size} steps of the chapters skipped left to do: $left", left.isEmpty())
        assertTrue("$name: the tuto shown, at « ${progress.current} »", progress.active && progress.current in chapters.last().steps.map { it.id })
        assertTrue("$name: the panel is back\n" + screenLines().joinToString("\n"), centerOf("tuto_chapters") != null)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
        fun cases(): List<Array<Any>> = TutoCourse.STYLES.flatMap { s -> TutoLevel.values().map { arrayOf<Any>(s, it) } }
    }
}
