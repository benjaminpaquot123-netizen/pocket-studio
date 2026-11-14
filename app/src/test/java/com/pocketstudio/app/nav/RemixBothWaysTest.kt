package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.projects.CurrentProject
import com.pocketstudio.app.remix.RemixFromHome
import com.pocketstudio.app.remix.SongInfo
import com.pocketstudio.app.remix.StemSet
import com.pocketstudio.app.remix.StemStore
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectStore
import com.pocketstudio.model.Remix
import com.pocketstudio.model.Scale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Owner (2026-10-29), the Remix both ways (docs/audits/PLAN-AUDITS-FINAUX.md §3, parcours 5): from
 * « Que veux-tu faire ? » a separated song goes into a new song built on it (its tempo and key, its
 * parts playing), a new empty project (the song silent, to take pieces from) or a project already
 * there, never a dead end; from a project, the Remix opens without leaving it and shows its way back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class RemixBothWaysTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val h = AppHarness(compose)

    @After
    fun end() {
        h.finish()
        StemStore.dirFor(h.app, SET).parentFile?.deleteRecursively()
    }

    @org.junit.Before
    fun appViewModels() = h.appViewModelsOfThisTest()

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** A song already separated on the phone: « Ma chanson », 98 BPM, in A minor. */
    private fun song(): StemSet {
        val dir = StemStore.dirFor(h.app, SET)
        dir.mkdirs()
        listOf("drums", "bass", "other", "vocals").forEach { File(dir, "$it.wav").writeBytes(ByteArray(44)) }
        return StemStore.commit(dir, "Ma chanson", 180.0, SongInfo(bpm = 98.0, firstBeat = 0.1, root = 9, minor = true))
    }

    private val store get() = ProjectStore(File(h.app.filesDir, "projects"))

    /** The project the app switched to (it is rebuilt on it), read from its file. */
    private fun switchedFrom(before: String): Pair<String, Project> {
        val end = System.currentTimeMillis() + 10_000
        while (CurrentProject.get(h.app) == before) {
            check(System.currentTimeMillis() < end) { "no other project opened" }
            Thread.sleep(50)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.waitForIdle()
        }
        val id = CurrentProject.get(h.app)
        return id to assertNotNullAndGet(store.load(id))
    }

    private fun <T> assertNotNullAndGet(v: T?): T {
        assertNotNull(v)
        return v!!
    }

    /** From the home screen: the song chosen, then « Que faire de… ? » with its three ways. */
    private fun chooseTheSongFromHome() {
        h.awaitTag("launch_remix")
        tap("launch_remix")
        h.awaitTag("use_$SET")
        tap("use_$SET")
        compose.onNodeWithTag("remix_dest").assertIsDisplayed()
    }

    @Test
    fun fromHomeASongBecomesANewSongAtItsTempoAndKey() {
        song()
        h.start(launchChoice = true)
        val before = CurrentProject.get(h.app)
        chooseTheSongFromHome()
        tap("remix_dest_build")
        // t5-05: the new project opens at once, in the mode shown (no second question once the song is picked).
        val (_, p) = switchedFrom(before)
        assertEquals("Ma chanson", p.name)
        assertEquals(98.0, p.tempoBpm, 0.01)
        assertEquals(9, p.harmony.root)
        assertEquals(Scale.Minor, p.harmony.scale)
        assertEquals(SET, p.remix.setId)
        assertEquals(98.0, p.remix.songBpm, 0.01)
        // Its parts are ready and heard.
        assertTrue(p.remix.stems.none { it.mute })
    }

    @Test
    fun fromHomeIntoANewEmptyProjectTheSongWaitsSilent() {
        song()
        h.start(launchChoice = true)
        val before = CurrentProject.get(h.app)
        chooseTheSongFromHome()
        tap("remix_dest_new")
        val (_, p) = switchedFrom(before)
        assertEquals(SET, p.remix.setId)
        // Nothing heard over what the user makes; its pieces still at hand on the Remix page.
        assertTrue(p.remix.stems.all { it.mute })
        assertEquals(120.0, p.tempoBpm, 0.01)
    }

    /**
     * Tour 5, F d #34 (t5-05, a left-handed seat in Simple): « Dans un nouveau projet » fell back on « Simple ou Pro ? »
     * (its cards out of the thumb) instead of opening the project. The Remix's ways open it at once in the mode shown
     * (Simple here, then Pro), no mode recorded as chosen for the user; the top bar's chip still changes it.
     */
    /** « Simple ou Pro ? » on screen; false while the app rebuilds on the project it is opening (no screen to ask on). */
    private fun askedTheMode(): Boolean = runCatching { exists("mode_step") }.getOrDefault(false)

    @Test
    fun fromHomeANewProjectOpensInTheModeShownWithoutAskingAgain() {
        song()
        h.start(simple = true, launchChoice = true)
        val before = CurrentProject.get(h.app)
        chooseTheSongFromHome()
        tap("remix_dest_new")
        assertFalse("« Simple ou Pro ? » asked again after « Dans un nouveau projet »", askedTheMode())
        val (_, p) = switchedFrom(before)
        assertEquals(SET, p.remix.setId)
        assertTrue(p.remix.stems.all { it.mute })
        assertTrue(com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode)
        assertNull("nothing chosen for the user", com.pocketstudio.app.settings.ProjectModes.last(h.app))
    }

    @Test
    fun fromHomeASongBuiltInProOpensInProWithoutAsking() {
        song()
        h.start(simple = false, launchChoice = true)
        val before = CurrentProject.get(h.app)
        chooseTheSongFromHome()
        tap("remix_dest_build")
        assertFalse("« Simple ou Pro ? » asked again after « Faire un morceau… »", askedTheMode())
        val (_, p) = switchedFrom(before)
        assertEquals(98.0, p.tempoBpm, 0.01)
        assertFalse(com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode)
        assertNull(com.pocketstudio.app.settings.ProjectModes.last(h.app))
    }

    @Test
    fun fromHomeIntoAnExistingProjectChosenInTheList() {
        val set = song()
        h.start(launchChoice = true)
        val before = CurrentProject.get(h.app)
        val other = "remix-other-" + System.nanoTime()
        store.save(other, Project(name = "Mon autre projet", tempoBpm = 140.0))
        chooseTheSongFromHome()
        tap("remix_dest_existing")
        h.awaitTag("remix_dest_project_$other")
        tap("remix_dest_project_$other")
        val (id, p) = switchedFrom(before)
        assertEquals(other, id)
        // Never changed on disk behind its back: the song goes in once the project is open (undo then works).
        assertNull(p.remix.setId)
        assertEquals(RemixFromHome.Waiting(set.id), RemixFromHome.take(other))
    }

    /**
     * Benjamin's video (APK 233, n° 15): « Voix » alone heard, then the song put in a project: only
     * the voice played at first, then after ▶ the whole song (the choice lost). Step 5a: the four
     * parts come in, the ones not heard muted, and ▶ or reopening keeps it.
     */
    @Test
    fun fromHomeTheVoiceHeardAloneComesInWithTheOtherPartsMuted() {
        song()
        h.start(launchChoice = true)
        val before = CurrentProject.get(h.app)
        h.awaitTag("launch_remix")
        tap("launch_remix")
        h.awaitTag("stem_${SET}_vocals")
        tap("stem_${SET}_vocals")
        tap("use_$SET")
        tap("remix_dest_build")
        val (_, p) = switchedFrom(before)
        // The new project, as opened from its file: the four parts, the voice heard, the others muted
        // (▶ and saving keep it: RemixSongEverywhereTest).
        assertEquals(Remix.STEMS, p.remix.stems.size)
        assertEquals(VOICE_ONLY, p.remix.stems.map { !it.mute })
    }

    @Test
    fun fromHomeIntoAnExistingProjectTheVoiceHeardAloneGoesWithTheSong() {
        val set = song()
        h.start(launchChoice = true)
        val before = CurrentProject.get(h.app)
        val other = "remix-other-" + System.nanoTime()
        store.save(other, Project(name = "Mon autre projet", tempoBpm = 140.0))
        h.awaitTag("launch_remix")
        tap("launch_remix")
        h.awaitTag("stem_${SET}_vocals")
        tap("stem_${SET}_vocals")
        tap("use_$SET")
        tap("remix_dest_existing")
        h.awaitTag("remix_dest_project_$other")
        tap("remix_dest_project_$other")
        switchedFrom(before)
        assertEquals(RemixFromHome.Waiting(set.id, VOICE_ONLY), RemixFromHome.take(other))
    }

    @Test
    fun aSongWaitingWithTheVoiceHeardAloneComesInWithTheOtherPartsMuted() {
        val set = song()
        val id = "remix-waiting-voice-" + System.nanoTime()
        RemixFromHome.put(id, set.id, VOICE_ONLY)
        h.start(projectId = id, build = {})
        val end = System.currentTimeMillis() + 10_000
        // Step 5c: its import screen first.
        h.confirmImport()
        while (h.vm.project.value.remix.setId != set.id) {
            check(System.currentTimeMillis() < end) { "the song never reached the project" }
            Thread.sleep(20)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.waitForIdle()
        }
        assertEquals(4, h.vm.project.value.remix.stems.size)
        assertEquals(VOICE_ONLY, h.vm.project.value.remix.stems.map { !it.mute })
    }

    @Test
    fun aSongWaitingForAProjectIsInItsRemixOnceItOpens() {
        val set = song()
        val id = "remix-waiting-" + System.nanoTime()
        RemixFromHome.put(id, set.id)
        h.start(projectId = id, build = {})
        val end = System.currentTimeMillis() + 10_000
        // Step 5c: its import screen first.
        h.confirmImport()
        while (h.vm.project.value.remix.setId != set.id) {
            check(System.currentTimeMillis() < end) { "the song never reached the project" }
            Thread.sleep(20)
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            compose.waitForIdle()
        }
        // Its Remix page is the one shown, the song in the project.
        compose.waitForIdle()
        compose.onNodeWithTag("remix_project").assertExists()
        assertNull(RemixFromHome.take(id))
    }

    @Test
    fun fromHomeNoDeadEnd() {
        song()
        h.start(launchChoice = true)
        chooseTheSongFromHome()
        // « Dans un projet existant » then « Retour »: the three ways again; « Annuler »: the song list.
        tap("remix_dest_existing")
        tap("remix_dest_back")
        compose.onNodeWithTag("remix_dest_build").assertIsDisplayed()
        tap("remix_dest_back")
        assertFalse(exists("remix_dest"))
        compose.onNodeWithTag("remix_home").assertIsDisplayed()
        // « ← Retour »: « Que veux-tu faire ? ».
        tap("remix_home_back")
        compose.onNodeWithTag("launch").assertIsDisplayed()
    }

    /** From a project in Pro: opened from the song (the Guide, a notification), the Remix is in it and « ← Mon morceau » comes back. */
    @Test
    fun fromAProjectTheRemixOpensInItAndComesBack() {
        h.start()
        tap("tab_Structure")
        val before = h.vm.project.value.name
        // Opened from the song (the Guide's card, a notification…): the Remix page of this project.
        com.pocketstudio.app.remix.SeparationJob.requestOpen()
        compose.waitForIdle()
        compose.onNodeWithTag("remix_start").assertIsDisplayed()
        assertEquals(before, h.vm.project.value.name)
        compose.onNodeWithTag("remix_back").assertIsDisplayed()
        tap("remix_back")
        assertFalse(exists("remix_start"))
        assertFalse(exists("remix_back"))
        compose.onNodeWithTag("tab_Structure").assertIsSelected()
    }

    private companion object {
        const val SET = "song-both-ways"
        /** Drums, bass, other, vocals: « Voix » alone heard. */
        val VOICE_ONLY = listOf(false, false, false, true)
    }
}
