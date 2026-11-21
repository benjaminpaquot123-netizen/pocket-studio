package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectGroup
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, lot L2, F029 (finition): after adding an effect from the bottom of the list, the effect added stayed off
 * screen, above. « Après avoir ajouté un effet, la liste me montre l'effet ajouté. » A plain Compose finger, no bench.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-port-hdpi")
class Tour9EffectAddTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun f029_anEffectAddedFromTheBottomOfTheListIsShown() {
        compose.setContent {
            var channel by remember { mutableStateOf(Channel()) }
            val actions = MixerActions(setInsertType = { _, slot, type ->
                channel = channel.copy(inserts = channel.inserts.toMutableList().also { it[slot] = Insert(type.ordinal, type.defaults) })
            })
            PocketStudioTheme {
                Column(Modifier.height(640.dp).verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(120.dp))
                    EffectsBox(1, channel, actions)
                    Spacer(Modifier.height(300.dp))
                }
            }
        }
        compose.onNodeWithTag("fx_add_1").performScrollTo().performClick()
        compose.waitForIdle()
        // The last effect of the list, at its bottom: the list scrolled down to reach it.
        val last = EffectType.entries.last { it.group == EffectGroup.CREATIVE && it != EffectType.NONE }
        compose.onNodeWithTag("fx_pick_1_${last.name}").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
        compose.onNodeWithTag("fx_chip_1_0").assertIsDisplayed()
    }
}
