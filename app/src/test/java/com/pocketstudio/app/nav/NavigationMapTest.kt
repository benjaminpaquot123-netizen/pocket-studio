package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
 * Audit stage 2, point 2: the app as a state machine, walked from the home screen (« Que veux-tu
 * faire ? ») in the Mode simple on a small phone. Each transition of [PATHS] is one user function
 * (docs/audits/CARTE-FONCTIONS.md, column « ≤ 3 touchers »):
 *  - it is reached in 3 touches at most (a scroll to reach a button counts as one more);
 *  - its screen shows what it promises (one of its tags);
 *  - the system's Retour always leads back home in a few presses, without leaving the app (no dead end).
 * A function moved deeper, or a screen that swallows Retour, fails here.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h780dp")
class NavigationMapTest(private val name: String, private val path: List<String>, private val expect: List<String>) {
    companion object {
        /** From « Que veux-tu faire ? »: the touches, then the tags one of which proves the function's screen. */
        val PATHS: List<Triple<String, List<String>, List<String>>> = listOf(
            // Pad Live opens its own « Live » project: the activity is rebuilt (not in this harness), its project proves it.
            Triple("Pad Live", listOf("launch_padlive"), listOf(PAD_LIVE_PROJECT)),
            Triple("Remix", listOf("launch_remix"), listOf("remix_start")),
            Triple("Nouveau morceau (assistant)", listOf("launch_new", "starter_wizard"), listOf("wizard", "mode_step")),
            Triple("Tutos (J'apprends)", listOf("launch_new", "start_learn"), listOf("tuto_chooser", "mode_step")),
            Triple("Mes projets", listOf("launch_projects"), listOf("projects")),
            Triple("Guide (Mon premier morceau)", listOf("launch_missions"), listOf("mode_step", "wizard", "guide_panel")),
            Triple("Continuer (le morceau, ses parties)", listOf("launch_continue"), listOf("tab_simple_Morceau")),
            Triple("Pads de batterie", listOf("launch_continue", "tab_simple_Rythme"), listOf("pad_0")),
            Triple("Bibliothèque de sons", listOf("launch_continue", "top_more", "plus_library"), listOf("library")),
            Triple("Notes au clavier", listOf("launch_continue", "tab_simple_Clavier"), listOf("synth")),
            Triple("Acid 303", listOf("launch_continue", "tab_simple_Clavier", "sons_Acide"), listOf("acid")),
            Triple("Mix et effets", listOf("launch_continue", "tab_simple_Mix"), listOf("simple_mix")),
            Triple("Export (Partager)", listOf("launch_continue", "tab_simple_Envoyer"), listOf("share_name", "share_go")),
            // Hands-on audit, chantier 1 (G2, recette tel-03): the files already shared, in sight under « Partager » (no scroll).
            Triple("Mes fichiers", listOf("launch_continue", "tab_simple_Envoyer", "share_my_exports"), listOf("my_exports")),
            Triple("Tempo", listOf("launch_continue", "top_tempo"), listOf("tempo_sheet")),
            // Audit ultra round 1 (n° 30): the ● asks what to record (keys, pads, the microphone).
            Triple("Enregistrer", listOf("launch_continue", "top_record", "record_choice_keys"), listOf("tab_simple_Clavier")),
            Triple("Enregistrer au micro", listOf("launch_continue", "top_record", "record_choice_mic"), listOf("audio_tracks")),
            // Audit ultra round 2 (n° 15): with the thumb's bar, the Mode simple's ↶ is there only.
            Triple("Annuler", listOf("launch_continue"), listOf("top_undo", "thumb_undo")),
            Triple("Live (cases, briques)", listOf("launch_continue", "top_more", "plus_live"), listOf("live_screen")),
            Triple("Ma voix", listOf("launch_continue", "top_more", "plus_voice"), listOf("audio_tracks")),
            Triple("Réglages (MIDI, MiniLab)", listOf("launch_continue", "top_more", "tool_Reglages"), listOf("settings")),
            Triple("Aide", listOf("launch_continue", "top_more", "tool_Aide"), listOf("help_page")),
            Triple("Signaler un bug", listOf("launch_continue", "top_more", "plus_bug"), listOf("bug_sheet")),
            Triple("Remix depuis un projet", listOf("launch_continue", "top_more", "plus_remix"), listOf("remix_start", "remix_dest", "stem_board")),
            // Tour 5, chantier 5 (G15): the Recette in the Mode simple too, on the same Plus page as in Pro.
            Triple("Recette", listOf("launch_continue", "top_more", "tool_Recette"), listOf("recette")),
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "path{index}")
        fun paths(): List<Array<Any>> = PATHS.map { (n, p, e) -> arrayOf<Any>(n, p, e) }

        const val PAD_LIVE_PROJECT = "@pad_live_project"
        const val BUDGET = 3
        const val BACKS = 4
    }

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    private fun shown(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    /** The system's Retour, to the window on top (a sheet or a dialog takes it before the activity). */
    private fun back() {
        androidx.test.espresso.Espresso.pressBackUnconditionally()
        compose.waitForIdle()
    }

    private fun padLiveProjectOpened(): Boolean =
        com.pocketstudio.app.projects.CurrentProject.get(h.app).let { id -> id != before && id != com.pocketstudio.app.projects.CurrentProject.NONE }

    private var before: String? = null

    private fun settle() {
        repeat(4) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.mainClock.advanceTimeBy(400)
            compose.waitForIdle()
        }
    }

    @Test
    fun reachedInThreeTouchesAndBackHome() {
        h.start(simple = true, launchChoice = true)
        h.awaitTag("launch_page")
        before = com.pocketstudio.app.projects.CurrentProject.get(h.app)
        val g = GestureBudget(compose, name)
        path.forEach { tag ->
            // A screen that comes after work off the main thread (the projects' list, the Remix's songs).
            runCatching { h.awaitTag(tag, 5_000) }
            g.tap(tag)
            settle()
        }
        g.assertWithin(BUDGET)
        if (expect == listOf(PAD_LIVE_PROJECT)) {
            val until = System.currentTimeMillis() + 10_000
            while (!padLiveProjectOpened() && System.currentTimeMillis() < until) { Thread.sleep(50); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
            assertTrue("« $name »: the Live project never opened", padLiveProjectOpened())
            return
        }
        val end = System.currentTimeMillis() + 5_000
        while (expect.none(::shown) && System.currentTimeMillis() < end) { Thread.sleep(50); settle() }
        assertTrue("« $name »: none of $expect after ${g.describe()}", expect.any(::shown))

        // No dead end: Retour comes home (« Que veux-tu faire ? ») and never leaves the app on the way.
        var presses = 0
        while (!(shown("launch_page") && compose.onNodeWithTag("launch_page").isDisplayed()) && presses < BACKS) {
            back()
            settle()
            presses++
            assertFalse("« $name »: Retour left the app after $presses presses", compose.activity.isFinishing)
        }
        assertTrue("« $name »: still not home after $presses × Retour", shown("launch_page"))
    }
}
