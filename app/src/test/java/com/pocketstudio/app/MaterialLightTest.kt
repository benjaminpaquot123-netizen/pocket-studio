package com.pocketstudio.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.foundation.layout.width
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.pocketstudio.ui.Knob
import com.pocketstudio.ui.KnobColors
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.PanelGrain
import com.pocketstudio.ui.PocketStudioTheme
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.liveGlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The panels' material and the lights of what plays, close up: the grain (off in every other
 * capture), a knob's cap, a meter's peak held after the level fell, the Play button's glow.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class MaterialLightTest {
    @get:Rule
    val compose = createComposeRule()

    @After
    fun grainOff() {
        PanelGrain.enabled = false
    }

    @Test
    fun materialsCloseUp() {
        PanelGrain.enabled = true
        val level = mutableFloatStateOf(0.92f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.fillMaxSize().background(Neon.Background).padding(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        NeonCard {
                            SectionLabel("Matière")
                            Text("Un panneau graphite, éclairé par le haut.", color = Neon.TextSecondary)
                            LevelMeter({ level.floatValue })
                        }
                        NeonCard(accent = Neon.Cyan) {
                            SectionLabel("Carte importante")
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                val colors = KnobColors(Neon.SurfaceHigh, Neon.Outline, Neon.TextPrimary, Neon.Background, Neon.Cyan, Neon.TextSecondary)
                                Knob(0.3f, {}, "Coupure", colors = colors, valueText = "1 200 Hz")
                                Knob(0.75f, {}, "Résonance", colors = colors, valueText = "75 %")
                                Box(Modifier.padding(top = 8.dp).requiredSize(48.dp).liveGlow(true, Neon.Acid, inset = 3.dp).clip(CircleShape)) {
                                    Box(Modifier.requiredSize(42.dp).clip(CircleShape).background(Neon.Acid))
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(100)
        // The level falls: its peak stays on the LED it reached.
        level.floatValue = 0.55f
        compose.mainClock.advanceTimeBy(300)
        compose.onRoot().captureRoboImage("screenshots/design_materials.png")
    }

    @Test
    fun grainIsFaint() {
        // Every pixel of the grain tile moves the light by less than 4 % (felt, not seen).
        val bitmap = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap.asImageBitmap())
        val paint = androidx.compose.ui.graphics.Paint()
        PanelGrain.brush.applyTo(androidx.compose.ui.geometry.Size(96f, 96f), paint, 1f)
        canvas.drawRect(0f, 0f, 96f, 96f, paint)
        var maxAlpha = 0
        var marked = 0
        for (y in 0 until 96) for (x in 0 until 96) {
            val a = bitmap.getPixel(x, y) ushr 24
            maxAlpha = maxOf(maxAlpha, a)
            if (a > 0) marked++
        }
        assert(maxAlpha in 1..10) { "grain too strong: alpha $maxAlpha" }
        assertEquals("a third of the pixels", 1f / 3f, marked / (96f * 96f), 0.05f)
    }

    @Test
    fun peakHoldsThenFalls() {
        val level = mutableFloatStateOf(0.92f)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.background(Neon.Background).padding(8.dp)) {
                    Box(Modifier.width(320.dp).testTag("meter")) { LevelMeter({ level.floatValue }) }
                }
            }
        }
        compose.mainClock.advanceTimeBy(100)
        level.floatValue = 0.55f
        compose.mainClock.advanceTimeBy(300)
        // 0.92 was reached: a line stays there above the level (0.55), nothing between them.
        assert(litAt(0.917f)) { "the peak is held" }
        assert(!litAt(0.75f)) { "only the peak's line above the level" }
        assert(litAt(0.30f)) { "the level shows, solid" }
        compose.mainClock.advanceTimeBy(2_500)
        assert(!litAt(0.917f)) { "the peak fell back to the level" }
        assert(litAt(0.30f)) { "the level still shows" }
    }

    @Test
    fun tipDissolvesIntoAStablePattern() {
        compose.setContent {
            PocketStudioTheme {
                Box(Modifier.background(Neon.Background).padding(8.dp)) {
                    Box(Modifier.width(320.dp).testTag("meter")) { LevelMeter({ 0.5f }) }
                }
            }
        }
        val first = compose.onNodeWithTag("meter").captureToImage().toPixelMap()
        // Along the 20 dp tip (144..160 dp of 320), lit pixels thin out towards the end.
        fun litShare(from: Float, to: Float): Float {
            var lit = 0
            var all = 0
            for (x in (first.width * from).toInt() until (first.width * to).toInt()) for (y in 0 until first.height) {
                val c = first[x, y]
                all++
                if (c.red + c.green + c.blue > 1.2f) lit++
            }
            return lit / all.toFloat()
        }
        val solid = litShare(0.30f, 0.40f)
        val nearTip = litShare(0.455f, 0.47f)
        val tip = litShare(0.485f, 0.499f)
        assert(solid > 0.95f) { "solid under the tip: $solid" }
        assert(nearTip in 0.3f..0.95f) { "pixels near the solid part: $nearTip" }
        assert(tip < nearTip) { "sparser at the very tip: $tip vs $nearTip" }
        // The same level draws the same pixels: no shimmer.
        compose.mainClock.advanceTimeBy(500)
        val again = compose.onNodeWithTag("meter").captureToImage().toPixelMap()
        for (x in 0 until first.width step 3) for (y in 0 until first.height step 2) assertEquals(first[x, y], again[x, y])
    }

    /** The meter is lit at [f] of its length (sampled across its middle). */
    private fun litAt(f: Float): Boolean {
        val image = compose.onNodeWithTag("meter").captureToImage().toPixelMap()
        val c = image[(image.width * f).toInt(), image.height / 2]
        return c.red + c.green + c.blue > 1.2f
    }
}
