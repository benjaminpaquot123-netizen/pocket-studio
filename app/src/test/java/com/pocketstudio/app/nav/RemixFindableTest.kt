package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertTextContains
import org.junit.Assert.assertTrue
import com.pocketstudio.app.ui.withinGestures
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot 2 (audit 35, gestes.md): Benjamin wants the Remix (a song split into drums, bass, voice and
 * the rest) « mis en avant ». It was the second page of Sons › Voix: 3 gestures from Structure,
 * 4 from the opening, none in the Mode simple. Now: one card on « Que veux-tu faire ? », a
 * « Remix » chip in Sons, a line in the Mode simple's « ⋯ » (docs/NAVIGATION.md).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class RemixFindableTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() = h.finish()

    companion object {
        /** docs/NAVIGATION.md: the Remix's budget (Benjamin: « en deux gestes »). */
        const val FROM_OPENING = 1
        const val FROM_A_SPACE = 2
    }

    private fun assertRemix() = compose.onNodeWithTag("remix_start").assertIsDisplayed()

    @Test
    fun fromTheOpeningItIsOneTouch() {
        h.start(launchChoice = true)
        h.awaitTag("launch_remix")
        // Owner (2026-10-29): a showcase card of its own, « Remix », the separation said under it.
        compose.onNodeWithTag("launch_remix").assertTextContains("Remix", substring = true)
            .assertTextContains("Séparation des pistes : batterie, basse, voix et le reste.")
        // On the project open: straight on the Remix, no new project, so no « Simple ou Pro ? ».
        compose.withinGestures("Remixer une chanson (ouverture)", FROM_OPENING) { tap("launch_remix") }
        compose.onNodeWithTag("mode_step").assertDoesNotExist()
        assertRemix()
    }

    /**
     * Remix testers (Sarah installed the app to extract a voice): the welcome has a way in, with the
     * word « voix »: a project of its own (« Remix »), opened on the Remix page (as « Que veux-tu faire ? » does).
     */
    @Test
    fun fromTheWelcomeItIsOneTouch() {
        h.start(simple = true, firstLaunch = true)
        h.awaitTag("welcome_remix")
        val before = com.pocketstudio.app.projects.CurrentProject.get(h.app)
        compose.onNodeWithText("Extraire la voix d'une chanson").assertExists()
        // The last card of the welcome (its order kept, Benjamin's): one scroll on a small phone, one touch,
        // then « Simple ou Pro ? » (a new project, owner 2026-10-29).
        compose.withinGestures("Extraire la voix (accueil)", FROM_A_SPACE + 1) {
            tap("welcome_remix")
            tap("mode_card_simple")
        }
        val end = System.currentTimeMillis() + 10_000
        while (com.pocketstudio.app.projects.CurrentProject.get(h.app) == before) {
            check(System.currentTimeMillis() < end) { "no project made for the Remix" }
            Thread.sleep(50)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        }
        // The project opens (the activity is rebuilt on it) with the Remix page asked for (the request
        // may already be taken by the screen before the rebuild: its page is kept across it, as
        // « Que veux-tu faire ? »'s Remix does).
        com.pocketstudio.app.remix.SeparationJob.openHandled()
    }

    @Test
    fun fromStructureItIsTwoTouches() {
        h.start()
        compose.withinGestures("Remixer une chanson", FROM_A_SPACE) {
            tap("tab_Sons")
            tap("sons_Remix")
        }
        assertRemix()
        // The voice keeps its own chip, and goes back to the audio tracks.
        compose.onNodeWithTag("sons_Audio").assertIsDisplayed()
    }

    @Test
    fun inTheModeSimpleItIsTwoTouches() {
        h.start(simple = true)
        compose.withinGestures("Remixer une chanson (Mode simple)", FROM_A_SPACE) {
            tap("top_more")
            compose.onNodeWithTag("more_remix").assertTextContains("Remix")
            tap("more_remix")
        }
        assertRemix()
        // Its way back to the song.
        compose.onNodeWithTag("remix_back").assertIsDisplayed().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("tab_simple_Morceau").assertIsSelected()
        compose.onNodeWithTag("remix_start").assertDoesNotExist()
    }

    /** G5: the Guide's card « Remixer un morceau » (the Missions' last card said it) opens the Remix. */
    @Test
    fun theGuidesRemixCardOpensIt() {
        h.start(simple = true) {
            val style = com.pocketstudio.model.StarterWizard.style("house")!!
            val song = com.pocketstudio.model.StarterWizard.build(style, style.lines.associate { it.role to null as com.pocketstudio.model.PadSample? }, style.melody.mood,
                com.pocketstudio.model.StarterWizard.defaultParts(style).associateWith { com.pocketstudio.model.StarterWizard.soundsFor(it, style).first() })
            session.edit { com.pocketstudio.model.guide.GuideStart.begin(song, com.pocketstudio.model.guide.GuideStart.SONG) }
        }
        val end = System.currentTimeMillis() + 60_000
        while (h.vm.guide.ui.value.cards["share.remix"] == null) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
            compose.waitForIdle()
            check(System.currentTimeMillis() < end) { "no remix card" }
        }
        compose.onNodeWithTag("guide_stage_7").performScrollTo().performClick()
        compose.onNodeWithTag("guide_item_share.remix").performScrollTo().performClick()
        compose.onNodeWithTag("guide_open_remix").performScrollTo().performClick()
        compose.waitForIdle()
        assertRemix()
    }

    @Test
    fun inTheModeSimpleOpeningToo() {
        h.start(simple = true, launchChoice = true)
        h.awaitTag("launch_remix")
        compose.withinGestures("Remixer une chanson (Mode simple, ouverture)", FROM_OPENING) { tap("launch_remix") }
        assertRemix()
    }
}
