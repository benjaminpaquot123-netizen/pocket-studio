package com.pocketstudio.app.mixer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextEquals
import com.pocketstudio.model.Channel
import com.pocketstudio.model.SoundKind
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** F136, the card under a sound too: « Les effets rapides proposés disent le nom de la piste (« Pour : Rhodes »). » */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi")
class Tour9EffectsCardNameTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun f136_theCardUnderASoundSaysItsTrack() {
        compose.setContent {
            PocketStudioTheme { EffectsCard(EffectsUi(3, Channel(), MixerActions(kindOf = { SoundKind.PLUCK }), trackName = "Rhodes")) }
        }
        compose.onNodeWithTag("fx_category_suggested", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("fx_category_suggested").assertTextEquals("Pour : Rhodes")
    }
}
