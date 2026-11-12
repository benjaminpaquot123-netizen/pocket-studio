package com.pocketstudio.app.human

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** n° 16 (seat D b, Pad Live's « Proje adı »): every name field has ✕, which empties it in one touch. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = HumanTester.PHONE)
class Zone3Round3FieldTest {
    @get:Rule
    val compose = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun aNameFieldIsEmptiedByItsCross() {
        var name by androidx.compose.runtime.mutableStateOf("Live")
        var other by androidx.compose.runtime.mutableStateOf("Mon son 1")
        compose.setContent {
            com.pocketstudio.ui.PocketStudioTheme {
                androidx.compose.foundation.layout.Column {
                    com.pocketstudio.app.ui.NameField(name, { name = it }, max = 40, label = "Nom du projet")
                    com.pocketstudio.app.ui.SelectAllTextField(other, { other = it })
                }
            }
        }
        compose.onAllNodesWithTag("name_clear")[0].performClick()
        compose.waitForIdle()
        org.junit.Assert.assertEquals("", name)
        compose.onAllNodesWithTag("name_clear")[0].performClick()
        compose.waitForIdle()
        org.junit.Assert.assertEquals("", other)
        org.junit.Assert.assertTrue(compose.onAllNodesWithTag("name_clear").fetchSemanticsNodes().isEmpty())
    }
}
