package com.pocketstudio.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Hands-on audit, chantier 2 (G7): the track chip was copied three times (Sons' « Batterie », « Voix », « Remix »; the
 * Acid 303's; every instrument track's), each 44 dp high whatever the font: its second line cut from font 1.23 (photo
 * b4e53e1f, the owner's Galaxy Tab A11). One shared `TrackChip` now, and none of the three draws a chip of its own.
 * What it looks like on the screen is checked by [com.pocketstudio.app.audit.SonsChipsTabletScanTest] and its variants.
 */
class TrackChipEverywhereTest {
    private val app = File("src/main/java/com/pocketstudio/app")

    @Test
    fun theTrackChipsAreOneComponent() {
        val places = mapOf(
            "ui/AppSpaces.kt" to "fun RowScope.EditorChip(",
            "acid/AcidLook.kt" to "fun AcidChip(",
            "synth/TrackBar.kt" to "ui.tracks.forEach { t ->",
        )
        val own = places.filter { (file, start) ->
            val s = File(app, file).readText()
            val body = s.substring(s.indexOf(start)).take(1800)
            "TrackChip(" !in body || ".height(44.dp)" in body
        }.keys
        assertTrue("A track chip drawn by hand (not the shared TrackChip, or 44 dp fixed around its two lines): $own", own.isEmpty())
    }
}
