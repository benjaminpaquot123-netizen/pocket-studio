package com.pocketstudio.app.nav

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.ui.AppRoot
import com.pocketstudio.app.ui.GestureBudget
import com.pocketstudio.model.Project
import com.pocketstudio.model.TrackKind
import com.pocketstudio.model.partSpans
import com.pocketstudio.model.songBars
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import com.pocketstudio.ui.PocketStudioTheme
import java.io.File

/**
 * The whole app as it opens (AppRoot, its spaces, its sheets), on a project prepared first, for
 * the tests that count the gestures of the navigation itself (phase 36). No engine in the tests:
 * what the engine would play is checked on the ViewModel.
 */
internal class AppHarness(val compose: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>) {
    // Read when used: Robolectric makes a new application for each test, after the test class is built.
    val app: Application get() = ApplicationProvider.getApplicationContext()
    lateinit var vm: RhythmViewModel

    /** Fresh files and settings, [simple] or the five spaces, then [build] on the project before the app shows it. */
    fun start(
        simple: Boolean = false, firstLaunch: Boolean = false, scale: Float = 1f, lang: com.pocketstudio.model.i18n.Lang = com.pocketstudio.model.i18n.Lang.FR,
        /** Shown through it (to save and restore the screens' state) rather than straight. */
        restore: androidx.compose.ui.test.junit4.StateRestorationTester? = null,
        build: RhythmViewModel.() -> Unit = { songWithTracks() },
    ) {
        File(app.filesDir, "projects").deleteRecursively()
        listOf("nav", "missions", "settings", "next_steps", "song_ready", "share", "tools").forEach {
            app.getSharedPreferences(it, 0).edit().clear().commit()
        }
        // A project of its own: a save still running from an earlier test lands in another file.
        com.pocketstudio.app.projects.CurrentProject.set(app, "nav-test-" + System.nanoTime())
        SettingsStore.forgetForTests()
        SettingsStore.update(app) {
            it.copy(guideSeen = !firstLaunch, simpleMode = simple, uiScale = scale, language = lang, englishNotes = lang != com.pocketstudio.model.i18n.Lang.FR)
        }
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        // Made here with this test's application (the default factory keeps the first test's one).
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = RhythmViewModel(app) as T
        }
        vm = ViewModelProvider(compose.activity, factory)[RhythmViewModel::class.java]
        vm.build()

        val content: @androidx.compose.runtime.Composable () -> Unit = {
            com.pocketstudio.app.ui.UiScaled(scale) { PocketStudioTheme { com.pocketstudio.app.learn.LearnLayer { AppRoot() } } }
        }
        if (restore != null) restore.setContent(content) else compose.setContent(content)
        compose.waitForIdle()
    }

    /** The settings back as a test that does not use them expects (the Mode simple's words off). */
    fun finish() {
        SettingsStore.forgetForTests()
        com.pocketstudio.model.i18n.I18n.everyday = false
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
    }

    /** The project's track key of the instrument track named [name] (« inst:3 »). */
    fun inst(name: String): String = "inst:" + vm.project.value.tracks.first { it.name == name }.id

    fun trackId(name: String): Int = vm.project.value.tracks.first { it.name == name }.id

    /**
     * Where part [part] is on the timeline's ruler (relative to it), as the timeline opens: the
     * whole song fits when it can (see TimelineScreen: (width - 16 dp) / bars, at most 48 dp a bar).
     */
    fun partX(part: Int): Float {
        val p: Project = vm.project.value
        val ruler = compose.onNodeWithTag("timeline_ruler").fetchSemanticsNode().boundsInRoot
        val d = compose.density
        val fit = (ruler.width - with(d) { 16.dp.toPx() }) / p.songBars.coerceAtLeast(1)
        val barPx = if (fit >= with(d) { 14.dp.toPx() }) minOf(fit, with(d) { 48.dp.toPx() }) else with(d) { 20.dp.toPx() }
        val span = p.partSpans[part]
        return (span.start + span.end) / 2f / p.stepsPerBar * barPx
    }

    /** The point of the screen in the middle of track [key]'s clip in part [part]. */
    fun clip(part: Int, key: String): Offset {
        val ruler = compose.onNodeWithTag("timeline_ruler").fetchSemanticsNode().boundsInRoot
        val header = compose.onNodeWithTag("timeline_header_$key").fetchSemanticsNode().boundsInRoot
        return Offset(ruler.left + partX(part), header.center.y)
    }

    /** The point on the ruler's band of part flags over part [part]. */
    fun flag(part: Int): Offset = Offset(partX(part), with(compose.density) { 15.dp.toPx() })

    /** Lets a single touch on the timeline through (it waits to know it is not the first of two). */
    fun settle() {
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
    }
}

/** A song of three parts (Intro, Refrain, Drop) with the drums, a « Basse » and a « Lead » track, on the scene A. */
internal fun RhythmViewModel.songWithTracks() {
    // The new project's acid line is drawn at random: a fixed one, for screenshots that stay the same.
    session.edit { it.copy(acid = com.pocketstudio.model.AcidGenerator(kotlin.random.Random(3)).generate(com.pocketstudio.model.AcidPattern(), com.pocketstudio.model.AcidGenerator.NOTE_SETS[5])) }
    toggleHit(0, 0)
    addTrackOf(TrackKind.Bass)
    addTrackOf(TrackKind.Melody)
    renameTrack(project.value.tracks.last().id, "Lead")
    renameTrack(project.value.tracks.first { it.name == "Basse" }.id, "Basse")
    duplicateScene()
    toggleHit(0, 4)
    duplicateScene()
    toggleHit(0, 8)
    selectScene(0)
    renameScene(0, "Intro")
    renameScene(1, "Refrain")
    renameScene(2, "Drop")
    listOf(0, 1, 2).forEach(::addSongPart)
    selectTrack(0)
}

/** A touch at [at] (in the screen's coordinates) on node [name] (a canvas: the timeline), counted. */
internal fun GestureBudget.tapAt(name: String, at: Offset, compose: AndroidComposeTestRule<*, *>) {
    val node = compose.onNodeWithTag(name)
    val origin = node.fetchSemanticsNode().boundsInRoot.topLeft
    node.performTouchInput { click(at - origin) }
    count(name)
    compose.mainClock.advanceTimeBy(600)
    compose.waitForIdle()
}

/** Two touches at [at]: one gesture, as FL Studio Mobile counts it. */
internal fun GestureBudget.doubleTapAt(name: String, at: Offset, compose: AndroidComposeTestRule<*, *>) {
    val node = compose.onNodeWithTag(name)
    val origin = node.fetchSemanticsNode().boundsInRoot.topLeft
    node.performTouchInput { doubleClick(at - origin) }
    count("double $name")
    compose.waitForIdle()
}

/** A long press at [at] on node [tag] (coordinates relative to it). */
internal fun GestureBudget.longPressOn(tag: String, at: Offset, compose: AndroidComposeTestRule<*, *>) {
    compose.onNodeWithTag(tag).performTouchInput { longClick(at) }
    count("hold $tag")
    compose.waitForIdle()
}

/** A long press then a slide from [from] to [to] on node [tag]: one gesture. */
internal fun GestureBudget.holdAndSlide(tag: String, from: Offset, to: Offset, compose: AndroidComposeTestRule<*, *>) {
    compose.onNodeWithTag(tag).performTouchInput {
        down(from)
        advanceEventTime(viewConfiguration.longPressTimeoutMillis + 200)
        // The press held still past the long press: it is recognised, then the finger slides.
        moveBy(Offset(0.5f, 0f))
        advanceEventTime(50)
        val steps = 10
        for (i in 1..steps) {
            moveTo(Offset(from.x + (to.x - from.x) * i / steps, from.y + (to.y - from.y) * i / steps))
            advanceEventTime(16)
        }
        up()
    }
    count("slide $tag")
    compose.waitForIdle()
}

/** A slide on node [tag] (a fader): one gesture. */
internal fun GestureBudget.slide(tag: String, compose: AndroidComposeTestRule<*, *>, up: Boolean = true) {
    compose.onNodeWithTag(tag).performTouchInput { if (up) swipeUp() else swipeDown() }
    count("slide $tag")
    compose.waitForIdle()
}
