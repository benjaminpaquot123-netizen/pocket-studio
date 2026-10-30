package com.pocketstudio.app.library

import androidx.activity.ComponentActivity
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.PartySounds
import com.pocketstudio.model.SoundShelves
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one sound browser (Benjamin's list n° 13–14): folders by meaning instead of one chip per
 * pack, the callers' own order kept, « Fête » (n° 12), back = the folder above.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class ShelfBrowserTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun kicks(id: String, family: String, name: String = id) =
        PackManifest(id, name, family, sounds = (1..16).map { PackSound("kick_%02d.flac".format(it), "kick", "Kick $it") })

    /** Every style pack and machine of the catalogue with its 16 kicks (the 45 chips Benjamin scrolled). */
    private val installed: List<PackManifest> = run {
        val styles = listOf(
            "techno" to "Techno", "hard_techno" to "Techno", "acid_techno" to "Techno", "gabber" to "Techno", "detroit" to "Techno",
            "house" to "House", "deep_house" to "House", "tech_house" to "House", "acid_house" to "House",
            "trap" to "Hip-hop", "drill" to "Hip-hop", "boom_bap" to "Hip-hop", "dnb" to "Bass music", "dubstep" to "Bass music",
            "hyperpop" to "Sons actuels", "pop" to "Autres", "afrobeats" to "Autres",
        )
        styles.map { (id, f) -> kicks(id, f) } + listOf(
            kicks("tr909", "Machines", "Roland TR-909"), kicks("tr808", "Machines", "Roland TR-808"), kicks("linndrum", "Machines", "LinnDrum"),
            PackManifest("fx", "Montées et impacts", "Sons actuels", sounds = (1..10).map { PackSound("fx_%02d.flac".format(it), "fx", "Montée $it") }),
        )
    }

    @Test
    fun fortyChipsBecomeAFewFoldersByMeaning() {
        val kick = StarterWizard.Role.entries.first { it.id == "kick" }
        assertEquals("the old way: one chip per pack", installed.count { it.ofKind("kick").isNotEmpty() } + 1, soundCategories(kick, null, installed).size)
        val shelves = shelvesOf(libraryChoices("kick", installed, null), installed, null, party = true)
        val families = shelves.map { it.id }
        assertEquals(listOf(KIT_FAMILY, PartySounds.FAMILY, "Machines", "Techno", "House", "Hip-hop", "Bass music", "Sons actuels", "Autres"), families)
        // Two levels: the machine inside « Machines », the measured character in a style family.
        assertEquals(listOf("Roland TR-909", "Roland TR-808", "LinnDrum"), shelves.first { it.id == "Machines" }.children.map { it.title })
        val techno = shelves.first { it.id == "Techno" }
        assertTrue(techno.children.size >= 2)
        val labels = com.pocketstudio.model.SoundCharacter.LABELS.values
        assertTrue(techno.children.all { it.title in labels && it.items.size >= SoundShelves.MIN_FOLDER })
        // Nothing lost: every kick of every pack is somewhere.
        assertEquals(installed.sumOf { it.ofKind("kick").size }, shelves.filter { it.id != KIT_FAMILY && it.id != PartySounds.FAMILY }.sumOf { it.count })
        // The best of every pack first: the first five of « Autres » come from both its packs.
        val first = shelves.first { it.id == "Autres" }.all.take(2).map { (it.ref as SoundRef.Pack).packId }.toSet()
        assertEquals(setOf("pop", "afrobeats"), first)
        // « Fête »: the app's own party sounds and the downloaded risers.
        val party = shelves.first { it.id == PartySounds.FAMILY }
        assertTrue(party.all.any { it.ref == SoundRef.Party("air_horn") })
        assertTrue(party.all.any { (it.ref as? SoundRef.Pack)?.packId == "fx" })
    }

    @Test
    fun theAssistantKeepsTheStylesMachineAndItsKitVoiceFirst() {
        // Acidcore's kick: its machine (Gabber) first, in « Pour ce style », before the kit.
        val style = StarterWizard.STYLES.first { s -> s.kit.isNotEmpty() && s.lines.any { it.voice == null && s.kit[it.role] != null } }
        val line = style.lines.first { it.voice == null && style.kit[it.role] != null }
        val machine = style.kit.getValue(line.role)
        val packs = installed + PackManifest(machine, machine, "Machines", sounds = listOf(PackSound("${line.role.packKind}_01.flac", line.role.packKind, "Son 1")))
        val shelves = shelvesOf(soundCategories(line.role, style.id, packs), packs, style.id)
        assertEquals(STYLE_FAMILY, shelves.first().id)
        assertEquals(machine, (shelves.first().all.first().ref as SoundRef.Pack).packId)
        assertFalse("no party in the assistant's drum lines", shelves.any { it.id == PartySounds.FAMILY })
        // A line that asks for its own kit voice (the afro house shaker) keeps it first.
        val afro = StarterWizard.style("afro_house")!!
        val voiced = afro.lines.first { it.voice != null }
        val withPack = installed + PackManifest(afro.id, "Afro", "House", sounds = listOf(PackSound("${voiced.role.packKind}_01.flac", voiced.role.packKind, "X")))
        val first = shelvesOf(soundCategories(voiced.role, afro.id, withPack), withPack, afro.id).first()
        assertEquals(KIT_FAMILY, first.id)
        assertEquals(SoundRef.Kit(voiced.voice!!), first.all.first().ref)
    }

    @Test
    fun aPadsQuickListIsTheBestOfEveryPack() {
        val picks = quickPicks("kick", installed, 12)
        assertEquals(12, picks.size)
        // The kit's kicks, then one sound per pack (not one pack's twelve).
        val packs = picks.mapNotNull { (it.ref as? SoundRef.Pack)?.packId }
        assertEquals(packs.size, packs.toSet().size)
    }

    @Test
    fun touchPlaysButtonChoosesBackGoesUp() {
        val shelves = shelvesOf(libraryChoices("kick", installed, null), installed, null, party = true)
        val heard = mutableListOf<SoundRef>()
        val chosen = mutableListOf<SoundRef>()
        val starred = mutableListOf<SoundRef>()
        compose.setContent {
            PocketStudioTheme {
                androidx.compose.foundation.layout.Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    ShelfBrowser(
                        ShelfBrowserUi(shelves, "kick", favorites = emptyList(), tag = "b", search = { q -> shelfSearch(shelves, q) }),
                        ShelfBrowserActions(preview = { heard += it.ref }, choose = { chosen += it.ref }, toggleFavorite = { starred += it.ref }),
                    )
                }
            }
        }
        // The families, no sound yet.
        assertTrue(exists("b_folder_Machines"))
        assertFalse(exists("b_sound_0"))
        compose.onNodeWithTag("b_folder_Machines").performScrollTo().performClick()
        compose.onNodeWithTag("b_folder_Machines/Roland TR-808").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("b_sound_0").performScrollTo().performClick()
        assertEquals("tr808", (heard.single() as SoundRef.Pack).packId)
        assertTrue(chosen.isEmpty())
        compose.onNodeWithTag("b_sound_use_0").performScrollTo().performClick()
        assertEquals(heard.single(), chosen.single())
        compose.onNodeWithTag("b_sound_fav_0").performScrollTo().performClick()
        assertEquals(chosen.single(), starred.single())
        // Back: the machines, then the families, then nothing left to go up.
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertTrue(exists("b_folder_Machines/Roland TR-909"))
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertTrue(exists("b_folder_Techno"))
    }

    @Test
    fun recentsAndFavoritesShowOnlyWhatThisChooserOffers() {
        val shelves = shelvesOf(libraryChoices("kick", installed, null), installed, null, party = true)
        compose.setContent {
            PocketStudioTheme {
                ShelfBrowser(
                    ShelfBrowserUi(shelves, "kick", recents = listOf("party:air_horn", "pack:deleted/kick_01.flac"), favorites = listOf("pack:gone/x.flac"), tag = "b"),
                    ShelfBrowserActions(),
                )
            }
        }
        assertTrue(exists("b_recents"))
        // A favourite whose pack was deleted is not offered (no empty folder).
        assertFalse(exists("b_favorites"))
        compose.onNodeWithTag("b_recents").performClick()
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithTag("b_sound_0").fetchSemanticsNodes().size)
        assertFalse(exists("b_sound_1"))
    }

    private fun exists(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
}
