package com.pocketstudio.app.library

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** « Changer le son » without any pack: more than the kit's one or two sounds of a kind. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class LibraryChoicesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun withoutPacksTheKitsOtherSoundsAreOffered() {
        val categories = libraryChoices("kick", emptyList(), styleId = null)
        assertEquals(listOf(KIT_CATEGORY, KIT_OTHER_CATEGORY), categories.map { it.id })
        // Kick and Kick 2, then the 14 other voices of the kit (a tom, a rim… can make a kick).
        assertEquals(2, categories[0].options.size)
        assertEquals(DrumKitInfo.names.size - 2, categories[1].options.size)
        assertTrue(categories[1].options.none { (it.ref as SoundRef.Kit).voice in StarterWizard.Role.Kick.builtIn })
    }

    @Test
    fun theProjectsStyleIsReadFromItsName() {
        val style = StarterWizard.STYLES.first()
        assertEquals(style.id, styleOfProject(style.name))
        // Two songs of one style: « Trap 2 ».
        assertEquals(style.id, styleOfProject(style.name + " 2"))
        assertNull(styleOfProject("Mon premier rythme"))
    }

    @Test
    fun theStylesPacksAreOfferedFirstAndDownloadedInOneTouch() {
        val style = StarterWizard.STYLES.first { it.packs.isNotEmpty() }
        val index = PackIndex(style.packs.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_200_000, 60) })
        val downloads = mutableListOf<String>()
        compose.setContent {
            PocketStudioTheme {
                LibraryScreen(
                    LibraryUi(PacksUi(IndexState.Ready(index)), target = "le pad 1 (Kick)", styleId = style.id),
                    LibraryActions(download = { downloads += it }),
                )
            }
        }
        compose.onNodeWithTag("library_style_packs").assertExists()
        compose.onNodeWithTag("library_style_packs_download").performClick()
        assertEquals(style.packs, downloads)
        // The kit's other sounds are a source too: all sixteen in « Sons intégrés ».
        compose.onNodeWithTag("library_folder_$KIT_FAMILY").assertTextContains("16 sons", substring = true)
    }

    @Test
    fun noOfferOnceThePacksAreThere() {
        val style = StarterWizard.STYLES.first { it.packs.isNotEmpty() }
        val index = PackIndex(style.packs.map { PackEntry(it, "Pack $it", "$it.zip", "a".repeat(64), 1_200_000, 60) })
        val installed = style.packs.map { com.pocketstudio.model.PackManifest(it, "Pack $it", "", 120.0, emptyList()) }
        compose.setContent {
            PocketStudioTheme {
                LibraryScreen(LibraryUi(PacksUi(IndexState.Ready(index), installed), target = "le pad 1 (Kick)", styleId = style.id), LibraryActions())
            }
        }
        compose.onNodeWithTag("library_style_packs").assertDoesNotExist()
    }
}
