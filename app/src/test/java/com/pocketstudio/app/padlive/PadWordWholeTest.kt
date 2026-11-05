package com.pocketstudio.app.padlive

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.audit.LayoutScan
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Relecture tour 1 (intégration): two hits in a « un seul à la fois » group showed « coup… » on a
 * phone (the word « coup · 1 » had the touch corner's width alone). It is drawn whole.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")
class PadWordWholeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aGroupedPadsWordIsWhole() {
        val pads = PadLive.placed(
            LivePads(), 0,
            LiveStarter.PADS.mapIndexed { i, p -> LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain, group = if (i >= 12) 1 else 0) },
        )
        val ui = PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4)
        compose.setContent { PocketStudioTheme { PadLiveScreen(ui, PadLiveActions(), clockOf = { 0 }, record = {}, still = true) } }
        val density = ApplicationProvider.getApplicationContext<android.content.Context>().resources.displayMetrics.density
        val cut = LayoutScan.scan(compose, density, setOf(LayoutScan.Kind.CUT)).filter { "·" in it.what }
        assertEquals(emptyList<LayoutScan.Finding>(), cut)
    }
}
