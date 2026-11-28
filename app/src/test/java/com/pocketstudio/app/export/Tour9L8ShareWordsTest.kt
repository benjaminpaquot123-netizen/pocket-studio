package com.pocketstudio.app.export

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.pocketstudio.model.TargetChoice
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, lot L8, « Partager »:
 * - F125 (seat t9-55): « Le nombre de parties dit dans Partager est celui que je vois ailleurs » (home said 11, share 6:
 *   a part played twice is one part, but the song's 11 rows are said too);
 * - F122 (seats t9-02, t9-26): « Je sais avant d'exporter que le nom tapé renomme aussi le projet, et je peux l'éviter »;
 * - F121 (seats t9-14, t9-22, t9-59): « Chaque valeur de volume dit en mots simples ce qu'elle veut dire ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class Tour9L8ShareWordsTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun end() {
        I18n.lang = Lang.FR
    }

    private fun texts(tag: String): String =
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes()
            .joinToString(" ") { n -> n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.joinToString(" ") ?: "" }

    private fun allText(): String = compose.onAllNodes(androidx.compose.ui.test.isRoot()).fetchSemanticsNodes().joinToString(" ") { root ->
        fun walk(n: androidx.compose.ui.semantics.SemanticsNode): String =
            (n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }?.joinToString(" ") ?: "") + " " +
                n.children.joinToString(" ") { walk(it) }
        walk(root)
    }

    @Test
    fun theSongsRowsAreSaidBesideItsParts() {
        compose.setContent { PocketStudioTheme { ShareSheetContent(ShareSheetUi("Mon tube", 180, parts = 6, rows = 11, checks = emptyList()), ShareSheetActions()) } }
        val said = allText()
        assertTrue(said, said.contains("6 parties"))
        assertTrue("the 11 rows the home counts are not said: $said", said.contains("11"))
    }

    @Test
    fun noRepeatNoExtraWords() {
        compose.setContent { PocketStudioTheme { ShareSheetContent(ShareSheetUi("Mon tube", 180, parts = 6, rows = 6, checks = emptyList()), ShareSheetActions()) } }
        assertFalse(allText().contains("reprises"))
    }

    @Test
    fun theRenameCanBeTurnedOff() {
        var asked: Boolean? = null
        var rename by androidx.compose.runtime.mutableStateOf(true)
        compose.setContent {
            PocketStudioTheme {
                ShareSheetContent(
                    ShareSheetUi("Mon tube", 180, parts = 1, checks = emptyList(), projectName = "Mon projet 3", renameProject = rename),
                    ShareSheetActions(renameProject = { asked = it; rename = it }),
                )
            }
        }
        assertTrue(allText().contains("renomme aussi le projet"))
        compose.onNodeWithTag("share_rename_project").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(false, asked)
        compose.onNodeWithTag("share_rename_project").assertIsOff()
        assertTrue(texts("share_keeps_name").contains("Mon projet 3"))
        assertTrue(compose.onAllNodes(hasTestTag("share_renames"), useUnmergedTree = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aNamedProjectHasNoRenameSwitch() {
        compose.setContent {
            PocketStudioTheme { ShareSheetContent(ShareSheetUi("Mon tube", 180, parts = 1, checks = emptyList(), projectName = "Ma chanson"), ShareSheetActions()) }
        }
        assertTrue(compose.onAllNodes(hasTestTag("share_rename_project")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun eachLevelSaysWhatItMeans() {
        val streaming = shareLevelWords(-14.0, TargetChoice.STREAMING)
        assertTrue("« (−14) » alone means nothing: $streaming", streaming.contains("LUFS") && streaming.contains("Spotify"))
        val auto = shareLevelWords(-9.0, TargetChoice.AUTO)
        assertFalse("Auto is not « club »: $auto", auto.contains("club"))
        assertTrue(auto, auto.contains("style"))
        // The level heard, no target: the same plain words as Streaming's.
        assertFalse(shareLevelWords(-14.0, null).endsWith("(−14)"))
    }

    @Test
    fun theDetailsSayTheyWereMeasuredWhileListening() {
        compose.setContent {
            PocketStudioTheme { ShareSheetContent(ShareSheetUi("Mon tube", 180, parts = 1, checks = emptyList(), lufs = -14.4, truePeakDb = -1.2), ShareSheetActions()) }
        }
        compose.onNodeWithTag("share_details").performScrollTo().performClick()
        compose.waitForIdle()
        val d = texts("share_loudness_details")
        assertTrue(d, d.contains("pendant l'écoute"))
    }

    @Test
    fun theNewWordsAreTranslatedEverywhere() {
        val keys = listOf(
            "comme la musique en streaming (−14 LUFS, le niveau de Spotify et YouTube)",
            "le volume habituel de ton style",
            "Mesuré pendant l'écoute : %s LUFS · crête %s dBTP",
            "(avec les reprises : %d)",
            "Renommer aussi le projet",
        )
        Lang.entries.filter { it != Lang.FR }.forEach { lang ->
            I18n.lang = lang
            keys.forEach { k -> assertTrue("${lang.code}: « $k » untranslated", com.pocketstudio.model.i18n.tr(k) != k) }
        }
    }
}
