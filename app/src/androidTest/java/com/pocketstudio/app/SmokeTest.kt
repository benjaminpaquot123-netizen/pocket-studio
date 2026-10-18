package com.pocketstudio.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pocketstudio.engine.NativeEngine
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Emulator smoke test run by CI: the app starts, plays, and every tab opens without crashing. */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun appStartsPlaysAndNavigates() {
        assertTrue("native engine must load", NativeEngine.isAvailable)
        compose.onNodeWithText("Pocket Studio").assertIsDisplayed()

        compose.onNodeWithTag("play").performClick()
        compose.waitUntil(5_000) { NativeEngine.streamInfo().callbacks > 0 || !NativeEngine.streamInfo().running }
        compose.onNodeWithText("Arrêter").assertIsDisplayed()
        compose.onNodeWithTag("play").performClick()

        compose.onNodeWithTag("tab_Latence").performClick()
        compose.onNodeWithTag("latency").assertIsDisplayed()

        compose.onNodeWithTag("tab_Recette").performClick()
        compose.onNodeWithTag("recette").assertIsDisplayed()
        compose.onNodeWithText("Phase 0 : fondations").assertIsDisplayed()
    }
}
