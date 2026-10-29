package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.library.LibraryActions
import com.pocketstudio.app.library.LibraryScreen
import com.pocketstudio.app.library.LibraryUi
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.remix.RemixActions
import com.pocketstudio.app.remix.RemixDestinationDialog
import com.pocketstudio.app.remix.RemixScreen
import com.pocketstudio.app.remix.RemixUi
import com.pocketstudio.app.remix.SeparationState
import com.pocketstudio.app.remix.SongInfo
import com.pocketstudio.app.remix.StemSet
import com.pocketstudio.model.MySound
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * « Ma bibliothèque de sons » and the Remix from the home screen (owner, 2026-10-29): the user's
 * sounds among the app's (same tiles, « À moi »), renamed in place; the Remix outside any project
 * and « Que faire de… ? » (app/screenshots/).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class MySoundsScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in SHOTS

    private val mine = listOf(
        MySound("a1", "Voix · Ma chanson", "vocal", "a1.wav", seconds = 2.4, bpm = 98.0, root = 9, minor = true),
        MySound("a2", "Refrain chanté", "vocal", "a2.wav", seconds = 4.1, bpm = 98.0, root = 9, minor = true),
        MySound("b1", "Boucle du refrain", "loop", "b1.wav", seconds = 4.9, bpm = 98.0),
    )

    private val set = StemSet(File("/tmp/stems/abc"), "Ma chanson", 214.0, 0L, SongInfo(98.0, 0.25, 9, true))

    /** The library on « Voix »: the user's sounds first, as the app's tiles, with « À moi ». */
    @Test
    fun mySoundsLibrary() = shoot("my_sounds_library") {
        LibraryScreen(LibraryUi(PacksUi(), target = "le pad 2 (Kick)", startKind = "vocal", mine = mine), LibraryActions())
    }

    /** ✎ touched: name, family, delete, in place. */
    @Test
    fun mySoundsEdit() = shoot("my_sounds_edit", prepare = {
        compose.onNodeWithTag("library_mine_edit_0").performClick()
        compose.waitForIdle()
    }) {
        LibraryScreen(LibraryUi(PacksUi(), target = "le pad 2 (Kick)", startKind = "vocal", mine = mine), LibraryActions())
    }

    /** The Remix from « Que veux-tu faire ? »: a song separated, « Utiliser cette chanson ». */
    @Test
    fun remixHome() = shoot("remix_home") {
        RemixScreen(RemixUi(true, SeparationState.Idle, listOf(set), home = true), RemixActions())
    }

    /** « Que faire de « Ma chanson » ? »: the three ways, the recommended one first. */
    @Test
    fun remixDestination() {
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxSize().background(Neon.Background)) {
                    RemixDestinationDialog(
                        set, listOf(ProjectInfo("p1", "Mon morceau", 0L, 120.0, 4, 2, hasRemix = false)), "p1",
                        onWay = {}, onProject = {}, onDismiss = {},
                    )
                }
            }
        }
        compose.waitForIdle()
        com.github.takahirom.roborazzi.captureScreenRoboImage("screenshots/remix_destination.png")
    }

    companion object {
        val SHOTS = setOf("my_sounds_library", "my_sounds_edit", "remix_home")
    }
}
