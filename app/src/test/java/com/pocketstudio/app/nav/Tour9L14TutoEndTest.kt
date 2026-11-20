package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.tuto.PrefsTutoStore
import com.pocketstudio.app.tuto.TutoTestHooks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.model.tuto.TutoTarget
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L14, F019 (t9-10, t9-61, t9-63, t9-72): a tutorial finished then quit still offered « Reprendre le tuto »
 * (nothing left to resume). Finished: « Recommencer » only; not finished: « Reprendre » as before.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour9L14TutoEndTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() {
        TutoTestHooks.kitOffer = true
        h.finish()
    }

    private fun quit(finished: Boolean) {
        val target = TutoTarget.of("house")!!
        val steps = TutoCourse.of("house")!!.chaptersFor(TutoLevel.Facile).flatMap { it.steps }
        val done = if (finished) steps.map { it.id }.toSet() else steps.dropLast(1).map { it.id }.toSet()
        TutoTestHooks.kitOffer = false
        h.start(simple = true, build = {
            session.edit { target.blank() }
            PrefsTutoStore(h.app).save(projectId, TutoProgress("house", TutoLevel.Facile, done, steps.last().id, active = false))
        })
        assertTrue("the tutorial's line under the page", shell.reached { shell.has("tuto_restart_quit") })
        shell.idle(300)
    }

    @Test
    fun finishedTutorialOffersNoResume() {
        quit(finished = true)
        assertFalse("« Reprendre le tuto » on a tutorial finished", shell.has("tuto_resume"))
        assertTrue("« Recommencer le tuto »", shell.has("tuto_restart_quit"))
    }

    @Test
    fun unfinishedTutorialStillResumes() {
        quit(finished = false)
        assertTrue("« Reprendre le tuto » on a tutorial left before its end", shell.has("tuto_resume"))
    }
}
