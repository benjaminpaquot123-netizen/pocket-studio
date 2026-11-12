package com.pocketstudio.app.library

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 3, n° 33 (seats F a, D b): « Changer le son »'s search found none of its own examples (« doigts,
 * verre, conga » : « Aucun son trouvé ») nor the obvious words (« caisse », « davul », « trampet ») while the kit has
 * those sounds; the « Caisses claires » tab's « Sons intégrés » held kicks, hats and toms. Without any pack downloaded.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class SoundSearchWordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    private val ui = LibraryUi(PacksUi(IndexState.Unavailable), target = "le pad 2 (Kick 2)", startKind = "snare")

    private fun found(word: String, kind: String): Boolean =
        librarySearch(word, ui, shelvesOf(libraryChoices(kind, emptyList(), null), emptyList(), null, party = true)).isNotEmpty()

    /** The examples the search field shows, in its language. */
    private fun examples(): List<String> =
        tr("Chercher un son (caisse, conga, sirène…)").substringAfter('(').substringAfter('（').substringBefore(')').substringBefore('）')
            .split(',', '،', '、').map { it.trim().trimEnd('…').trim() }.filter { it.isNotEmpty() }

    @Test
    fun theFieldsOwnExamplesFindASoundInEveryLanguageSimpleAndPro() {
        val missed = ArrayList<String>()
        for (lang in Lang.entries) for (everyday in listOf(false, true)) {
            I18n.lang = lang
            I18n.everyday = everyday
            val words = examples()
            if (words.size != 3) missed += "$lang: examples « ${words.joinToString()} »"
            for (w in words) for (kind in listOf("kick", "snare", "clap")) if (!found(w, kind)) missed += "$lang${if (everyday) " Simple" else ""}, tab $kind: « $w »"
        }
        assertTrue(missed.joinToString("\n"), missed.isEmpty())
    }

    @Test
    fun theObviousWordsFindTheKitsSounds() {
        val words = mapOf(
            Lang.FR to listOf("caisse", "grosse caisse", "conga", "charley", "sirene"),
            Lang.TR to listOf("davul", "trampet", "conga"),
            Lang.NL to listOf("basdrum", "conga"),
            Lang.EN to listOf("snare", "conga", "bass drum"),
            Lang.DE to listOf("snare", "trommel"),
            Lang.JA to listOf("スネア", "コンガ"),
            Lang.KO to listOf("스네어", "콩가"),
            Lang.ZH_CN to listOf("军鼓", "康加鼓"),
        )
        val missed = ArrayList<String>()
        for ((lang, list) in words) for (everyday in listOf(false, true)) {
            I18n.lang = lang
            I18n.everyday = everyday
            for (w in list) if (!found(w, "kick")) missed += "$lang${if (everyday) " Simple" else ""}: « $w »"
        }
        assertTrue(missed.joinToString("\n"), missed.isEmpty())
    }

    /** Seat F a, screens 27-30: typed in the field as a person does, and the « Caisses claires » tab. */
    @Test
    fun typedCaisseFindsTheSnaresAndTheSnareTabHoldsOnlySnares() {
        compose.setContent { PocketStudioTheme { LibraryScreen(ui, LibraryActions()) } }
        compose.onNodeWithTag("library_search").performTextInput("caisse")
        compose.waitForIdle()
        assertEquals("« Aucun son trouvé » for « caisse »", 0,
            compose.onAllNodes(hasText("Aucun son trouvé", substring = true), useUnmergedTree = true).fetchSemanticsNodes().size)
        assertTrue(compose.onAllNodes(hasText("Caisse 2"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        compose.onNodeWithTag("library_search").performTextClearance()
        compose.waitForIdle()
        // « Caisses claires › Sons intégrés »: the snares only (caisse, caisse 2, bord de caisse); the rest apart.
        compose.onNodeWithTag("library_folder_$KIT_FAMILY").performScrollTo().performClick()
        compose.waitForIdle()
        for (other in listOf("Kick", "Charley", "Tom grave")) {
            assertEquals("« $other » in « Caisses claires › Sons intégrés »", 0,
                compose.onAllNodes(hasText(other), useUnmergedTree = true).fetchSemanticsNodes().size)
        }
        assertTrue(compose.onAllNodes(hasText("Caisse 2"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }
}
