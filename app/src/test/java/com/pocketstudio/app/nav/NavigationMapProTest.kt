package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.ui.GestureBudget
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra §1: [NavigationMapTest]'s map for the Pro, where the state machine had no map: from the
 * home screen (« Que veux-tu faire ? ») with the Pro on, the five spaces (Structure, Sons, Mix, Live,
 * Plus), their pages and the top bar's sheets; and Pad Live as a root of its own (an app in the app:
 * its « Live » project opened by « Continuer », then its own pages). For each path:
 *  - reached in 3 touches at most, or 4 when the first touch only gives the context (« Continuer » into
 *    the project, the space, then the page and its sheet) — a scroll to reach a button counts as one more;
 *  - its screen shows what it promises (one of its tags);
 *  - the system's Retour leads back home in a few presses, without leaving the app.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class NavigationMapProTest(private val name: String, private val root: String, private val path: List<String>, private val expect: List<String>) {
    companion object {
        const val HOME = "home"
        const val PAD_LIVE = "padlive"

        private fun p(name: String, path: List<String>, expect: List<String>, root: String = HOME) = arrayOf<Any>(name, root, path, expect)

        /** From the Pro's home ([HOME]) or from Pad Live ([PAD_LIVE]): the touches, then the tags one of which proves the function's screen. */
        val PATHS: List<Array<Any>> = listOf(
            p("Continuer (Structure)", listOf("launch_continue"), listOf("session", "tab_Structure")),
            p("Structure › Ligne du temps", listOf("launch_continue", "tab_Structure", "structure_timeline"), listOf("timeline_ruler", "timeline_grid")),
            p("Structure › Parties", listOf("launch_continue", "tab_Structure", "structure_parts"), listOf("part_0", "add_part_0")),
            p("Sons (pistes)", listOf("launch_continue", "tab_Sons"), listOf("sons_Rythme", "rhythm")),
            p("Sons › Batterie", listOf("launch_continue", "tab_Sons", "sons_Rythme"), listOf("rhythm", "pad_0")),
            p("Sons › Acid 303", listOf("launch_continue", "tab_Sons", "sons_Acide"), listOf("acid")),
            p("Sons › Voix", listOf("launch_continue", "tab_Sons", "sons_Audio"), listOf("audio_tracks")),
            p("Mix", listOf("launch_continue", "tab_Mix"), listOf("mixer")),
            p("Live (cases, briques)", listOf("launch_continue", "tab_Live"), listOf("live_screen")),
            p("Plus", listOf("launch_continue", "tab_Plus"), listOf("plus")),
            p("Plus › Aide", listOf("launch_continue", "tab_Plus", "tool_Aide"), listOf("help_page")),
            p("Plus › Réglages", listOf("launch_continue", "tab_Plus", "tool_Reglages"), listOf("settings")),
            p("Plus › Clavier MIDI", listOf("launch_continue", "tab_Plus", "tool_Midi"), listOf("midi_page", "midi", "settings_midi")),
            p("Plus › Latence", listOf("launch_continue", "tab_Plus", "tool_Latence"), listOf("latency")),
            p("Plus › Recette", listOf("launch_continue", "tab_Plus", "tool_Recette"), listOf("recette")),
            p("Plus › Mes projets", listOf("launch_continue", "tab_Plus", "plus_projects"), listOf("projects")),
            p("Plus › Mon premier morceau (Guide)", listOf("launch_continue", "tab_Plus", "plus_missions"), listOf("mode_step", "wizard", "guide_panel")),
            p("Plus › Les mots de la musique", listOf("launch_continue", "tab_Plus", "plus_glossary"), listOf("glossary")),
            p("Plus › Signaler un bug", listOf("launch_continue", "tab_Plus", "plus_bug"), listOf("bug_sheet")),
            p("Tempo", listOf("launch_continue", "top_tempo"), listOf("tempo_sheet")),
            p("Partager (export, master)", listOf("launch_continue", "top_share"), listOf("share_sheet", "share_go")),
            p("Annuler", listOf("launch_continue"), listOf("top_undo")),
            p("Enregistrer", listOf("launch_continue", "top_record"), listOf("record_running", "record_stop", "record_bar")),
            p("Mes projets (accueil)", listOf("launch_projects"), listOf("projects")),
            p("Remix", listOf("launch_remix"), listOf("remix_start")),
            p("Nouveau morceau (assistant)", listOf("launch_new", "starter_wizard"), listOf("wizard", "mode_step")),
            p("Tutos (J'apprends)", listOf("launch_new", "start_learn"), listOf("tuto_chooser", "mode_step")),
            // Pad Live, a root of its own: « Continuer » on its « Live » project opens it (not counted).
            p("Pad Live", listOf(), listOf("padlive_play", "padlive_title"), PAD_LIVE),
            p("Pad Live › Plus", listOf("padlive_more"), listOf("padlive_party_open", "padlive_stop_all", "padlive_share"), PAD_LIVE),
            p("Pad Live › Pack", listOf("padlive_pack"), listOf("padlive_pack_fete", "padlive_pack_house"), PAD_LIVE),
            p("Pad Live › Défi", listOf("padlive_challenge_open"), listOf("padlive_challenge", "padlive_challenge_title"), PAD_LIVE),
            p("Pad Live › Tempo", listOf("padlive_tempo"), listOf("tempo_sheet", "padlive_tempo_more"), PAD_LIVE),
            p("Pad Live › Briques (Live)", listOf("padlive_more", "padlive_live"), listOf("live_screen"), PAD_LIVE),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "pro{index}")
        fun paths(): List<Array<Any>> = PATHS

        const val BUDGET = 3
        /** One more when the first touch only gives the context (« Continuer »: into the project). */
        const val BUDGET_IN_CONTEXT = 4
        const val BACKS = 5
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun shown(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun tagsNow(): List<String> = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.distinct().sorted()

    private fun back() {
        androidx.test.espresso.Espresso.pressBackUnconditionally()
        compose.waitForIdle()
    }

    private fun settle() {
        repeat(4) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
        }
    }

    @Test
    fun reachedInFewTouchesAndBackHome() {
        if (root == PAD_LIVE) {
            h.start(simple = false, launchChoice = true, build = { session.edit { com.pocketstudio.model.PadLive.newProject() } })
            h.awaitTag("launch_page")
            h.awaitTag("launch_continue")
            compose.onNodeWithTag("launch_continue").performClick()
            settle()
            h.awaitTag("padlive_play", 10_000)
        } else {
            h.start(simple = false, launchChoice = true)
            h.awaitTag("launch_page")
        }
        val g = GestureBudget(compose, name)
        path.forEach { tag ->
            runCatching { h.awaitTag(tag, 5_000) }
            g.tap(tag)
            settle()
        }
        g.assertWithin(if (path.firstOrNull() == "launch_continue") BUDGET_IN_CONTEXT else BUDGET)
        val end = System.currentTimeMillis() + 5_000
        while (expect.none(::shown) && System.currentTimeMillis() < end) { Thread.sleep(50); settle() }
        assertTrue("« $name »: none of $expect after ${g.describe()}; on screen: ${tagsNow()}", expect.any(::shown))

        // No dead end: Retour comes home (« Que veux-tu faire ? ») and never leaves the app on the way.
        var presses = 0
        while (!(shown("launch_page") && compose.onNodeWithTag("launch_page").isDisplayed()) && presses < BACKS) {
            back()
            settle()
            presses++
            assertFalse("« $name »: Retour left the app after $presses presses", compose.activity.isFinishing)
        }
        assertTrue("« $name »: still not home after $presses × Retour; on screen: ${tagsNow()}", shown("launch_page") && compose.onNodeWithTag("launch_page").isDisplayed())
    }
}
