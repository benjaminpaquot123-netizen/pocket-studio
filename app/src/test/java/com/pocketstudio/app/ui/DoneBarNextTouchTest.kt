package com.pocketstudio.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tour 9, F111: « Le message « nouvelle longueur » disparaît après quelques secondes ou au geste suivant. » The next touch
 * or swipe lets the message shown at its start go; a message that very touch says stays (a click says it at the release,
 * before the page has drawn it); with no message, a touch changes nothing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h800dp-xxhdpi")
class DoneBarNextTouchTest {
    @get:Rule
    val compose = createComposeRule()

    private val done = MutableStateFlow<String?>(null)
    private var dismissed = 0

    private fun page(say: String = "Fait : kit Studio sur les 16 pads") = compose.setContent {
        Box(Modifier.fillMaxSize().testTag("page").letDoneGoAtNextTouch(done) { dismissed++; done.value = null }) {
            Column {
                Button(onClick = {}, modifier = Modifier.testTag("quiet")) { Text("Rien") }
                Button(onClick = { done.value = say }, modifier = Modifier.testTag("says")) { Text("Dit") }
                Box(Modifier.size(10.dp).height(400.dp))
            }
        }
    }

    @Test fun aTouchLetsTheShownMessageGo() {
        page()
        done.value = "Fait : nouvelle longueur pour C · Couplet"
        compose.onNodeWithTag("quiet").performClick()
        compose.waitForIdle()
        assertNull("the message stayed over the next touch", done.value)
    }

    @Test fun aSwipeLetsTheShownMessageGo() {
        page()
        done.value = "Fait : nouvelle longueur pour C · Couplet"
        compose.onNodeWithTag("page").performTouchInput { swipeUp() }
        compose.waitForIdle()
        assertNull("the message stayed over the next swipe", done.value)
    }

    @Test fun theMessageThisTouchSaysStays() {
        page()
        done.value = "Fait : nouvelle longueur pour C · Couplet"
        compose.onNodeWithTag("says").performClick()
        compose.waitForIdle()
        assertEquals("the touch's own message went with the old one", "Fait : kit Studio sur les 16 pads", done.value)
        assertEquals(0, dismissed)
    }

    @Test fun noMessageNothingToLetGo() {
        page()
        compose.onNodeWithTag("quiet").performClick()
        compose.waitForIdle()
        assertEquals(0, dismissed)
    }
}
