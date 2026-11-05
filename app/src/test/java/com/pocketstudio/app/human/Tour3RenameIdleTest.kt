package com.pocketstudio.app.human

import android.app.Application
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
 * ⋮ › Renommer) the bench never came back, Compose never idle (scratchpad jstack-B-a.txt). App or bench? The same
 * touches without the bench (no HumanTester, no BenchEngine): the app as the seat had it, the name typed, then idle.
 * Verdict: the app. Both Simple tests (night and studio theme) never come back to rest, already at the finger's touch on
 * « Nom du projet » (performTouchInput click, line of the touch: AppNotIdleException after 60 s), before any typing.
 * The same field alone (Tour3NameFieldIdleTest) rests: the loop comes from the field inside the app's window.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Tour3RenameIdleTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun close() {
        scenario?.close()
    }

    private val projectMenu = androidx.compose.ui.test.SemanticsMatcher("a project's ⋮") { n ->
        n.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith("project_menu_")
    }

    private fun shown(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()

    @Test
    @org.junit.Ignore("Round 3, zone 3 verdict: the bench, not the app. Any text field in a Compose Dialog never idles under Robolectric, without one line of the app (Zone3DialogDiagTest).")
    fun typingAProjectNameInSimpleModeAtNightLetsTheScreenRest() = typeAProjectName(simple = true, theme = "Night")

    @Test
    @org.junit.Ignore("Round 3, zone 3 verdict: the bench, not the app. Any text field in a Compose Dialog never idles under Robolectric, without one line of the app (Zone3DialogDiagTest).")
    fun typingAProjectNameInSimpleModeInStudioLetsTheScreenRest() = typeAProjectName(simple = true, theme = "Studio")

    private fun typeAProjectName(simple: Boolean, theme: String) {
        for (d in listOf("projects", "projects-trash")) File(app.filesDir, d).deleteRecursively()
        app.getSharedPreferences("settings", 0).edit().clear()
            .putString("language", "fr").putBoolean("simpleMode", simple).putBoolean("guideSeen", !simple).putString("theme", theme).commit()
        SettingsStore.forgetForTests()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitForIdle()
        // The seat's way: the welcome's « Plus tard », the project's name on top, its ⋮, « Renommer », the field.
        if (shown("welcome_later")) compose.onNodeWithTag("welcome_later").performClick()
        compose.waitUntil(20_000) { shown("top_projects") }
        compose.onNodeWithTag("top_projects").performClick()
        compose.waitUntil(20_000) { compose.onAllNodes(projectMenu).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(projectMenu)[0].performClick()
        compose.onNodeWithText("Renommer").performClick()
        compose.onNodeWithTag("project_name_field").performTouchInput { click() }
        compose.onNodeWithTag("project_name_field").performTextInput(" exam")
        val at = System.currentTimeMillis()
        compose.waitForIdle()
        compose.onNodeWithTag("project_name_field").performTextInput("Révisions lofi")
        compose.waitForIdle()
        assertTrue("idle again in ${System.currentTimeMillis() - at} ms", System.currentTimeMillis() - at < 30_000)
        assertTrue(compose.onAllNodes(hasText(" examRévisions lofi", substring = true)).fetchSemanticsNodes().isNotEmpty())
    }
}

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
    /** The project rename's own window (ScaledAlertDialog, its field rescaled), at night, without the app around it. */
    @Test
    @org.junit.Ignore("Round 3, zone 3 verdict: the bench, not the app. Any text field in a Compose Dialog never idles under Robolectric, without one line of the app (Zone3DialogDiagTest).")
    fun aNameFieldInTheRenameWindowTouchedThenTypedInRests() {
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme(com.pocketstudio.ui.Palettes.Dark) {
                var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Mon premier rythme") }
                com.pocketstudio.ui.ScaledAlertDialog(
                    onDismissRequest = {},
                    title = { androidx.compose.material3.Text("Renommer") },
                    text = { com.pocketstudio.app.ui.NameField(name, { name = it }, max = 40, label = "Nom du projet", modifier = androidx.compose.ui.Modifier.testTag("field")) },
                    confirmButton = { androidx.compose.material3.Text("[$name]") },
                )
            }
        }
        typeAndRest("[ examRévisions lofi]")
    }
}
