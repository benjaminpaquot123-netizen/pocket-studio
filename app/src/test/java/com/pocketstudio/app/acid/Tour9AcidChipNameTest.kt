package com.pocketstudio.app.acid

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9 lot L5a, F064 (t9-39 and 5 seats): after « Créer la piste Basse » the track bar showed « Acid 303 ·
 * Ligne de basse » next to « Basse »: two basses, and the tuto spoke of one. The 303's chip names no bass.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-port-hdpi")
class Tour9AcidChipNameTest {
    @get:Rule
    val compose = createComposeRule()

    @After fun lang() { I18n.lang = Lang.FR }

    @Test
    fun theAcidChipNamesNoSecondBass() {
        I18n.lang = Lang.FR
        compose.setContent { PocketStudioTheme { AcidChip(selected = false, onClick = {}) } }
        compose.onNodeWithText("Acid 303").assertIsDisplayed()
        assertEquals("the 303's chip reads as a bass track", 0,
            compose.onAllNodesWithText("basse", substring = true, ignoreCase = true).fetchSemanticsNodes().size)
    }
}
