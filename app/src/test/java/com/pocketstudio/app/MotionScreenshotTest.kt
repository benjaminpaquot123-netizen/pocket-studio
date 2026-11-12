package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.app.ui.PlayPosition
import com.pocketstudio.app.ui.PlayStrip
import com.pocketstudio.app.ui.PlayStripActions
import com.pocketstudio.app.ui.PlayStripUi
import com.pocketstudio.ui.FamilyChip
import com.pocketstudio.ui.LocalReducedMotion
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.StatRow
import com.pocketstudio.ui.TileGrid
import com.pocketstudio.ui.pressScale
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Frames taken in the middle of a motion (the clock held still): a press, a number that changes,
 * a list that opens. They show the touch feedback a still screenshot cannot, and keep it from
 * going unnoticed if it breaks.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class MotionScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Before
    fun holdClock() {
        compose.mainClock.autoAdvance = false
    }

    /** [still]: as with the phone's animations off (each change shows at once, as before the effects). */
    private fun show(still: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalReducedMotion provides still) {
                PocketStudioTheme {
                    Box(Modifier.fillMaxSize().background(Neon.Background).padding(16.dp)) { content() }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun shot(name: String) = compose.onRoot().captureRoboImage("screenshots/motion/$name.png")

    private val names = listOf("Kick 808", "Basse ronde", "Piano doux", "Nappe", "Lead", "Pluck", "Voix", "Cordes")
    private val icons = listOf(Icons.Filled.Album, Icons.Filled.GraphicEq, Icons.Filled.Piano, Icons.Filled.MusicNote)
    private val colors @Composable get() = listOf(Neon.Cyan, Neon.Magenta, Neon.Amber, Neon.Acid)

    @Composable
    private fun Tiles(count: Int) {
        val c = colors
        TileGrid(count) { i, m ->
            SoundTile(
                names[i % names.size], icons[i % icons.size], c[i % c.size], selected = i == 0, onClick = {},
                subtitle = "Son ${i + 1}", modifier = m.testTag("tile_$i"),
            )
        }
    }

    /** A sound tile held under the finger, 120 ms after the touch. */
    @Test
    fun pressTile() {
        show {
            NeonCard {
                SectionLabel("Sons")
                Tiles(6)
            }
        }
        compose.onNodeWithTag("tile_3").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(120)
        shot("press_tile")
    }

    /** Buttons and chips: the main button held, 120 ms after the touch. */
    @Test
    fun pressButton() {
        show {
            NeonCard {
                SectionLabel("Actions")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FamilyChip("Batterie", Icons.Filled.Album, Neon.Cyan, selected = true, onClick = {})
                    FamilyChip("Basses", Icons.Filled.GraphicEq, Neon.Magenta, selected = false, onClick = {}, modifier = Modifier.testTag("chip"))
                }
                // As the app's main buttons have it.
                Button(onClick = {}, modifier = Modifier.pressScale().fillMaxWidth().testTag("main")) { Text("Créer le morceau") }
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Annuler") }
            }
        }
        compose.onNodeWithTag("main").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(120)
        shot("press_button")
    }

    /** The tempo and the bar change (120 → 121 BPM, bar 17 → 18): 70 ms later, rolling. */
    @Test
    fun numbersRoll() = numbers(still = false, "numbers_roll")

    /** The same with the phone's animations off: the new values at once. */
    @Test
    fun numbersStill() = numbers(still = true, "numbers_still")

    private fun numbers(still: Boolean, name: String) {
        var bpm by mutableStateOf(120.0)
        var bar by mutableStateOf(17)
        var db by mutableStateOf("-12,0 dB")
        show(still) {
            Column(Modifier.width(400.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayStrip(
                    PlayStripUi(bpm, hasSong = true, loopPart = false, playing = true, position = { PlayPosition("Refrain", bar, 60) }, parts = listOf("Intro", "Refrain")),
                    PlayStripActions(),
                )
                NeonCard {
                    SectionLabel("Mesures")
                    StatRow("Crête", db)
                    StatRow("Tempo", "${Math.round(bpm)} BPM")
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        bpm = 121.0
        bar = 18
        db = "-11,5 dB"
        Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeBy(70)
        shot(name)
    }

    /** A list of sounds just opened: 80 ms after it shows. */
    @Test
    fun listOpens() {
        show {
            NeonCard {
                SectionLabel("Sons")
                Tiles(12)
            }
        }
        compose.mainClock.advanceTimeBy(64)
        shot("list_opens")
    }
}
