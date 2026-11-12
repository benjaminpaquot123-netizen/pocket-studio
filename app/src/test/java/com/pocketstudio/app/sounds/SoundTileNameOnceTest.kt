package com.pocketstudio.app.sounds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.SoundTile
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 5, lot « langues » (seat B d in Arabic, the assistant's sounds: « باص تناظري تناظري », « سرب سرب », « آلة وتريات آلة
 * وتريات »): a sound's tile says its name, then a line about it taken from its engine's model, which repeats the name when the
 * name is the model's (« Essaim », « Machine à cordes ») or holds it (« Basse analogique »). Every Plaits sound of the library,
 * in every language: the tile never shows its line when that line only says the name again.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class SoundTileNameOnceTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun back() {
        I18n.lang = Lang.FR
    }

    @Test
    fun aTileNeverSaysItsNameTwice() {
        val sounds = SoundLibrary.PLAITS
        var lang by mutableStateOf(Lang.FR)
        compose.setContent {
            I18n.lang = lang
            key(lang) {
                PocketStudioTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        sounds.forEachIndexed { i, s ->
                            SoundTile(
                                title = tr(s.name), icon = Icons.Filled.MusicNote, accent = Color.Cyan, selected = false, onClick = {},
                                subtitle = SoundLooks.describe(s.sound), modifier = Modifier.testTag("tile_$i"),
                            )
                        }
                    }
                }
            }
        }
        val twice = ArrayList<String>()
        for (l in Lang.entries) {
            lang = l
            compose.waitForIdle()
            sounds.indices.forEach { i ->
                val node = compose.onNodeWithTag("tile_$i", useUnmergedTree = false).fetchSemanticsNode()
                val texts = node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
                val title = texts.firstOrNull().orEmpty()
                texts.drop(1).filter { line -> line.split(' ', '·').filter { it.isNotBlank() }.all { title.contains(it, ignoreCase = true) } }
                    .forEach { twice += "${l.code}: « $title » then « $it »" }
            }
        }
        assertTrue(twice.joinToString("\n"), twice.isEmpty())
    }
}
