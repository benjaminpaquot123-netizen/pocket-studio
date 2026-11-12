package com.pocketstudio.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.app.library.IndexState
import com.pocketstudio.app.library.LibraryActions
import com.pocketstudio.app.library.LibraryScreen
import com.pocketstudio.app.library.LibraryUi
import com.pocketstudio.app.library.PacksUi
import com.pocketstudio.app.library.WizardActions
import com.pocketstudio.app.library.WizardRow
import com.pocketstudio.app.library.WizardScreen
import com.pocketstudio.app.library.WizardUi
import com.pocketstudio.app.library.soundCategories
import com.pocketstudio.app.session.LivePadsActions
import com.pocketstudio.app.session.LivePadsCard
import com.pocketstudio.app.session.LivePadsUi
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.StarterWizard
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The one sound browser (Benjamin's list n° 13–14), « Fête » and the ready-to-play live pads
 * (n° 12): the families as folders instead of 45 chips, a family opened on its measured
 * characters, the assistant's kick, the eight pads ready to play (app/screenshots/).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
open class BrowserScreenshotTest : ScreenshotTest() {
    override fun wanted(name: String) = name in SHOTS

    private fun kicks(id: String, family: String, name: String) =
        PackManifest(id, name, family, sounds = (1..16).map { PackSound("kick_%02d.flac".format(it), "kick", "Kick $it") })

    /** The kicks of every family on the phone (the 45 chips Benjamin scrolled). */
    private val installed = listOf(
        kicks("techno", "Techno", "Techno"), kicks("hard_techno", "Techno", "Hard techno"), kicks("gabber", "Techno", "Gabber"),
        kicks("detroit", "Techno", "Techno de Detroit"), kicks("house", "House", "House"), kicks("deep_house", "House", "Deep house"),
        kicks("trap", "Hip-hop", "Trap"), kicks("boom_bap", "Hip-hop", "Boom bap"), kicks("dnb", "Bass music", "Drum & bass"),
        kicks("hyperpop", "Sons actuels", "Hyperpop"), kicks("pop", "Autres", "Pop"),
        kicks("tr909", "Machines", "Roland TR-909"), kicks("tr808", "Machines", "Roland TR-808"), kicks("linndrum", "Machines", "LinnDrum"),
    )

    @androidx.compose.runtime.Composable
    private fun kickLibrary() = LibraryScreen(
        LibraryUi(PacksUi(IndexState.Unavailable, installed), target = "le pad 1 (Kick)", recents = listOf("party:air_horn", "pack:tr909/kick_03.flac")),
        LibraryActions(),
    )

    @Test
    fun libraryShelves() = shoot("library_shelves") { kickLibrary() }

    @Test
    fun libraryShelfTechno() = shoot("library_shelf_techno", prepare = {
        compose.onNodeWithTag("library_folder_Techno").performScrollTo().performClick()
        compose.waitForIdle()
    }) { kickLibrary() }

    @Test
    fun libraryShelfParty() = shoot("library_shelf_party", prepare = {
        compose.onNodeWithTag("library_folder_Fête").performScrollTo().performClick()
        compose.waitForIdle()
    }) { kickLibrary() }

    /** The assistant's kick (Acidcore): its machine first, opened on its folder, back to every family. */
    @Test
    fun wizardKickShelf() = shoot("wizard_kick_shelf", prepare = {
        compose.onNodeWithTag("wizard_browser").performScrollTo()
        compose.waitForIdle()
    }) {
        val style = StarterWizard.style("acidcore")!!
        val packs = installed + kicks("acidcore", "Techno", "Acidcore")
        val rows = style.lines.associate { l ->
            val cats = soundCategories(l.role, style.id, packs)
            l.role to WizardRow(true, cats.first().id, cats.first().options.first())
        }
        val ui = WizardUi(true, 0, style.id, rows, StarterWizard.Mood.Dark, emptyMap())
        val kick = ui.steps.indexOfFirst { it is StarterWizard.Step.Drum && it.line.role == StarterWizard.Role.Kick }
        WizardScreen(ui.copy(step = kick), PacksUi(IndexState.Unavailable, packs), null, WizardActions())
    }

    /** Pads opened empty: the eight sounds ready to play. */
    @Test
    fun livePadsStarter() = shoot("live_pads_starter") {
        val made = LiveStarter.PADS.indices.associateWith { LiveStarter.Made("starter_$it.wav", if (LiveStarter.PADS[it].oneShot) 1 else 4) }
        val pads = LiveStarter.applied(com.pocketstudio.model.LivePads(), made)
        LivePadsCard(LivePadsUi(pads, List(16) { if (it == 0 || it == 2) 2 else 0 }, editing = null), LivePadsActions())
    }

    companion object {
        val SHOTS = setOf("library_shelves", "library_shelf_techno", "library_shelf_party", "wizard_kick_shelf", "live_pads_starter")
    }
}
