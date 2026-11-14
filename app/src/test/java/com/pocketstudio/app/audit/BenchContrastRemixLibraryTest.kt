package com.pocketstudio.app.audit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.window.DialogProperties
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.LibraryActions
import com.pocketstudio.app.library.LibraryScreen
import com.pocketstudio.app.library.LibraryUi
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.remix.RemixActions
import com.pocketstudio.app.remix.RemixScreen
import com.pocketstudio.app.remix.RemixUi
import com.pocketstudio.app.remix.SeparationState
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.Palette
import com.pocketstudio.ui.Palettes
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.ScaledDialog
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Audit ultra round 2, n° 30: the bench wrote LAYOUT CONTRAST 1.38–2.04:1 on the Remix page's « Télécharger le
 * modèle » / « Choisir une chanson » (seat A c, Studio theme) and 1.21–2.75:1 on the library's « Poser » (seat B d,
 * night theme): dark words on the orange (cyan) of a filled button, the accent on a panel. The same pages, in the
 * same windows (a full-screen dialog) and both themes, drawn and settled without the bench: every one of these
 * words reads (4.5:1). The bench's lines came from a screen still drawing, not from the colours (n° 30: « banc »).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class BenchContrastRemixLibraryTest {
    @get:Rule
    val compose = createComposeRule()

    private val words = listOf("Télécharger le modèle", "Choisir une chanson", "Poser", "Kick", "Sons intégrés")

    private fun scanned(palette: Palette, page: @androidx.compose.runtime.Composable () -> Unit, prepare: () -> Unit = {}): List<String> {
        compose.setContent {
            PocketStudioTheme(palette) {
                ScaledDialog({}, DialogProperties(usePlatformDefaultWidth = false)) {
                    Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) { page() }
                }
            }
        }
        compose.waitForIdle()
        prepare()
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        return LayoutScan.scan(compose, compose.density.density, setOf(LayoutScan.Kind.CONTRAST)).map { it.toString() }
            .filter { f -> words.any { it in f } }
    }

    private val remix = @androidx.compose.runtime.Composable {
        RemixScreen(RemixUi(false, SeparationState.Idle, emptyList(), home = true), RemixActions())
    }

    private val library = @androidx.compose.runtime.Composable {
        LibraryScreen(LibraryUi(PacksUi(IndexState.Unavailable, emptyList()), target = "le pad 1 (Kick)"), LibraryActions())
    }

    private fun openBuiltIn() {
        compose.onNodeWithTag("library_folder_Sons intégrés").performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun remixReads(p: Palette) {
        val found = scanned(p, remix)
        // Not empty-handed: the two buttons the bench flagged are on the page.
        compose.onNodeWithTag("remix_download_thumb").assertExists()  // Tour 6, A10: the one download button left
        compose.onNodeWithTag("remix_pick").assertExists()
        assertTrue("$p:\n" + found.joinToString("\n"), found.isEmpty())
    }

    @Test
    fun theRemixButtonsReadInTheStudioTheme() = remixReads(Palettes.Studio)

    @Test
    fun theRemixButtonsReadAtNight() = remixReads(Palettes.Dark)

    @Test
    fun theLibrarysPoserReadsAtNight() {
        val found = scanned(Palettes.Dark, library, ::openBuiltIn)
        compose.onNodeWithTag("library_sound_use_0").assertExists()
        assertTrue(found.joinToString("\n"), found.isEmpty())
    }
}
