package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.projects.ProjectStarter
import com.pocketstudio.app.projects.ProjectsActions
import com.pocketstudio.app.projects.ProjectsScreen
import com.pocketstudio.app.projects.ProjectsUi
import com.pocketstudio.app.projects.StarterGroup
import com.pocketstudio.app.tuto.TutoActions
import com.pocketstudio.app.tuto.TutoChapterList
import com.pocketstudio.app.tuto.TutoChooser
import com.pocketstudio.app.tuto.TutoPanel
import com.pocketstudio.app.tuto.TutoUi
import com.pocketstudio.app.tuto.tutoMap
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoProgress
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** « Tuto de A à Z » and the new « Nouveau projet » (app/screenshots/tuto_*.png). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class TutoScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun shoot(name: String, prepare: () -> Unit = {}, content: @Composable () -> Unit) {
        compose.setContent { PocketStudioTheme(Palettes.Studio) { Box(Modifier.fillMaxSize().background(Neon.Background)) { content() } } }
        prepare()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/tuto_$name.png")
    }

    /** The course at step [id] of [level], the steps before it done. */
    private fun ui(level: TutoLevel, id: String, open: Boolean = true): TutoUi {
        val chapters = TutoCourse.of("jump_up")!!.chaptersFor(level)
        val flat = chapters.flatMap { it.steps }
        val step = flat.first { it.id == id }
        val c = chapters.indexOfFirst { step in it.steps }
        return TutoUi(TutoProgress("jump_up", level, flat.takeWhile { it != step }.map { it.id }.toSet(), id), chapters, c, chapters[c].steps.indexOf(step), open = open)
    }

    @Composable
    private fun Panel(u: TutoUi, canShow: Boolean = true) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        TutoPanel(u, canShow, TutoActions(), map = tutoMap("jump_up"))
    }

    @Test
    fun newProject() = shoot("new_project") {
        ProjectsScreen(
            ProjectsUi(loading = false), "p-0",
            listOf(ProjectStarter("Rythme de départ", "Un rythme et une ligne d'acide prêts à tourner.", "starter", StarterGroup.Style), ProjectStarter("Projet vide", "Tout est vierge : à toi de jouer.", "empty", StarterGroup.Empty)),
            ProjectsActions(), newOnly = true,
        )
    }

    @Test
    fun chooser() = shoot("chooser") { TutoChooser(start = { _, _ -> }, close = {}) }

    @Test
    fun stepFacile() = shoot("step_facile") { Panel(ui(TutoLevel.Facile, "kick_do")) }

    @Test
    fun stepPro() = shoot("step_pro", prepare = { compose.onNodeWithTag("tuto_why").performClick() }) { Panel(ui(TutoLevel.Pro, "map_drop"), canShow = false) }

    @Test
    fun chapters() = shoot("chapters") { TutoChapterList(ui(TutoLevel.Facile, "make_intro"), {}, {}, {}, {}) }
}
