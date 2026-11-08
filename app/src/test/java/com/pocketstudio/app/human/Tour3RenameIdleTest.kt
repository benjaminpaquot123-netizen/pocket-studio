package com.pocketstudio.app.human

import android.app.Application
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import org.robolectric.Shadows.shadowOf
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.settings.SettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Audit ultra round 3 (lot banc, seat B a: Mode simple, night theme): after « ecris » in « Nom du projet » (Mes projets ›
 * ⋮ › Renommer) the bench never came back. Verdict of zone 3 (Zone3DialogDiagTest): Robolectric, not the app: a text
 * field in any Compose dialog never lets Compose rest there. What a person does is checked here, without waiting for that
 * rest while the window is open (frames instead): the name typed in the window, « OK », the project renamed on the list,
 * and once the window is closed the screen comes back to rest. To check on a phone too (recette: rename a project).
 */
abstract class Tour3RenameIdleBase(private val theme: String) {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        com.pocketstudio.app.onboarding.LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
    }

    private val projectMenu = androidx.compose.ui.test.SemanticsMatcher("a project's ⋮") { n ->
        n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith("project_menu_")
    }

    private fun shown(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    @Test
    fun aProjectRenamedInSimpleModeKeepsItsNewName() = renameAProject(simple = true, theme = theme)

    private fun renameAProject(simple: Boolean, theme: String) {
        // A phone just set up (as ForegroundSoundTest): no project, no preference left by a test before.
        for (d in listOf("projects", "projects-trash", "stems")) File(app.filesDir, d).deleteRecursively()
        File(app.filesDir, "../shared_prefs").listFiles()?.forEach { app.getSharedPreferences(it.name.removeSuffix(".xml"), 0).edit().clear().commit() }
        app.getSharedPreferences("settings", 0).edit()
            .putString("language", "fr").putBoolean("simpleMode", simple).putBoolean("guideSeen", !simple).putString("theme", theme).commit()
        SettingsStore.forgetForTests()
        // The project to rename, on the phone and open (as GestureFuzzTest): the list never waits for the starter's first write.
        val id = "renommer-" + System.nanoTime()
        com.pocketstudio.model.ProjectStore(File(app.filesDir, "projects")).save(id, com.pocketstudio.model.Project(name = "Mon premier rythme").sanitized())
        com.pocketstudio.app.projects.CurrentProject.set(app, id)
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance").apply { isAccessible = true }.set(null, null)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
        // The seat's way: the welcome's « Plus tard », the project's name on top, its ⋮, « Renommer », the field.
        // Banc fiable 1.2 (red twice by luck, 2026-11-08: « no project's ⋮ », still on the pads): a window of the start
        // (the welcome, the launch's choice) can come just after the first look and take the touch. As a person does, look
        // again and touch « Mes projets » again, three times at most; the screen's rest is awaited before each touch.
        for (attempt in 1..3) {
            compose.waitForIdle()
            if (shown("welcome_later")) compose.onNodeWithTag("welcome_later").performClick()
            compose.waitUntil(20_000) { shown("top_projects") }
            compose.onNodeWithTag("top_projects").performClick()
            val opened = runCatching { compose.waitUntil(7_000) { compose.onAllNodes(projectMenu).fetchSemanticsNodes().isNotEmpty() } }.isSuccess
            if (opened) break
        }
        runCatching { compose.waitUntil(1_000) { compose.onAllNodes(projectMenu).fetchSemanticsNodes().isNotEmpty() } }.onFailure {
            val tags = compose.onAllNodes(androidx.compose.ui.test.SemanticsMatcher("tagged") { n -> androidx.compose.ui.semantics.SemanticsProperties.TestTag in n.config })
                .fetchSemanticsNodes(atLeastOneRootRequired = false).map { n -> n.config[androidx.compose.ui.semantics.SemanticsProperties.TestTag] }
            error("no project's ⋮ in Mes projets; tags: $tags")
        }
        compose.onAllNodes(projectMenu)[0].performClick()
        compose.onNodeWithText("Renommer").performClick()
        // The window is open: no wait for Compose's rest (Robolectric's limit), frames go by instead.
        compose.runWithoutImplicitWait {
            frames()
            val field = node("project_name_field")
            assertTrue("the field shows the project's name", field.config.getOrNull(SemanticsProperties.EditableText)?.text?.isNotBlank() == true)
            compose.runOnUiThread { field.config[SemanticsActions.RequestFocus].action!!() }
            frames()
            compose.runOnUiThread { node("project_name_field").config[SemanticsActions.SetText].action!!(AnnotatedString("Révisions lofi")) }
            frames()
            assertEquals("Révisions lofi", node("project_name_field").config.getOrNull(SemanticsProperties.EditableText)?.text)
            compose.runOnUiThread { node("project_rename_ok").config[SemanticsActions.OnClick].action!!() }
            frames()
        }
        // The window closed: the screen rests again, the project has its new name.
        compose.waitUntil(20_000) { !shown("project_name_field") }
        compose.waitForIdle()
        compose.waitUntil(20_000) { compose.onAllNodes(hasText("Révisions lofi")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun node(tag: String): SemanticsNode =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull() ?: error("$tag absent")

    /** A few frames of the test clock and the main looper (a bounded wait, see the class). */
    private fun frames() = repeat(6) {
        compose.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idle()
    }
}

/** The night theme (seat B a of round 3). Banc fiable 1.2: one test, one activity a class (two in one class went red by order). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour3RenameIdleTest : Tour3RenameIdleBase("Night")

/** The studio theme, in a class of its own (banc fiable 1.2). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour3StudioRenameIdleTest : Tour3RenameIdleBase("Studio")

/**
 * Round 3 (coordinator: seat E c, a track's « Name » field, the first typing unanswered for 5 min): the field the
 * renames share (NameField, SelectAllTextField, round 2's n° 26), alone, touched as a finger does (a real touch that
 * moves the cursor, as the bench's, not the semantics click of NameFieldTest), typed in, then the screen must rest.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour3NameFieldIdleTest {
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()

    private fun typeAndRest(shown: String) {
        compose.onNodeWithTag("field").performTouchInput { click() }
        compose.waitForIdle()
        compose.onNodeWithTag("field").performTextInput(" exam")
        val at = System.currentTimeMillis()
        compose.waitForIdle()
        compose.onNodeWithTag("field").performTextInput("Révisions lofi")
        compose.waitForIdle()
        assertTrue("idle again in ${System.currentTimeMillis() - at} ms", System.currentTimeMillis() - at < 30_000)
        compose.onNodeWithText(shown).assertExists()
    }

    @Test
    fun aNameFieldTouchedThenTypedInRests() {
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Mon premier rythme") }
                com.pocketstudio.app.ui.NameField(name, { name = it }, max = 40, label = "Nom du projet", modifier = androidx.compose.ui.Modifier.testTag("field"))
                androidx.compose.material3.Text("[$name]")
            }
        }
        typeAndRest("[ examRévisions lofi]")
    }

    @Test
    fun aSelectAllFieldTouchedThenTypedInRests() {
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Audio 1") }
                com.pocketstudio.app.ui.SelectAllTextField(name, { name = it }, modifier = androidx.compose.ui.Modifier.testTag("field"))
                androidx.compose.material3.Text("[$name]")
            }
        }
        typeAndRest("[ examRévisions lofi]")
    }
    /**
     * The project rename's own window (ScaledAlertDialog, its field rescaled), at night, without the app around it: the name
     * typed is the one the window confirms. No wait for Compose's rest while it is open (Robolectric's limit, Zone3DialogDiagTest).
     */
    @Test
    fun aNameFieldInTheRenameWindowTakesTheNameTyped() {
        // The window opens after setContent (whose own wait for rest would never end with it open).
        val open = androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme(com.pocketstudio.ui.Palettes.Dark) {
                var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Mon premier rythme") }
                if (open.value) com.pocketstudio.ui.ScaledAlertDialog(
                    onDismissRequest = {},
                    title = { androidx.compose.material3.Text("Renommer") },
                    text = { com.pocketstudio.app.ui.NameField(name, { name = it }, max = 40, label = "Nom du projet", modifier = androidx.compose.ui.Modifier.testTag("field")) },
                    confirmButton = { androidx.compose.material3.Text("[$name]") },
                )
            }
        }
        compose.runWithoutImplicitWait {
            fun frames() = repeat(6) { compose.mainClock.advanceTimeByFrame(); shadowOf(Looper.getMainLooper()).idle() }
            fun field() = compose.onAllNodesWithTag("field").fetchSemanticsNodes(atLeastOneRootRequired = false).firstOrNull() ?: error("field absent")
            compose.runOnUiThread { open.value = true }
            frames()
            compose.runOnUiThread { field().config[SemanticsActions.RequestFocus].action!!() }
            frames()
            compose.runOnUiThread { field().config[SemanticsActions.SetText].action!!(AnnotatedString("Révisions lofi")) }
            frames()
            assertTrue(compose.onAllNodes(hasText("[Révisions lofi]")).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty())
        }
    }
}
