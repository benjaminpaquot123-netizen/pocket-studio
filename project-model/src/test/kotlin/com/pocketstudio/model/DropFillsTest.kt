package com.pocketstudio.model

import com.pocketstudio.model.SongBars.drumBars
import com.pocketstudio.model.SongBars.drumSceneOfBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * G0 review: the drummer's fills at the 8 and the 16 and the crashes at the 16 all find a scene,
 * even in the long songs whose last Drop has a scene of its own (Benjamin's « Jump up DnB », made
 * before, had 21 scenes and lost its fills at bars 135 (its second Break), 159 and 175). The same written fill shares
 * one scene wherever it falls.
 */
class DropFillsTest {
    /** The bars of [p]'s Drops where a fill (every 8th bar but the Drop's last) or a crash (every 16th from the first) is due, and missing. */
    private fun missing(p: Project): List<String> {
        val drums = p.drumSceneOfBars()
        val out = ArrayList<String>()
        for (d in DropVariation.drops(p)) {
            for (k in 7 until d.bars - 1 step 8) if (p.allScenes[drums[d.start + k]].label != PartIntents.FILL_LABEL) out += "fill ${d.start + k}"
        }
        return out
    }

    @Test
    fun everyFillOfTheDropsIsPlacedInTheLongSongs() {
        val bad = ArrayList<String>()
        for (style in StarterWizard.STYLES) {
            val p = DropVariationTest.song(style)
            missing(p).takeIf { it.isNotEmpty() }?.let { bad += "${style.id}: $it" }
            // And every mark of the song (crashes, fills at the 16 and at the 8) has its drum scene.
            val lines = style.lines.filter { !it.off }.map { it.role }.toSet()
            val plan = StarterWizard.planFor(style, lines, StarterWizard.defaultParts(style), acid = style.melody.acid != null)
            val crash = StarterWizard.Role.Ride.lane.takeIf { p.allScenes.any { it.label == DrumArrangement.CRASH_LABEL } }
            val marks = DrumArrangement.marks(SongShape.shape(style, plan), crash, p.songBars)
            if (marks == null) bad += "${style.id}: no marks" else {
                val drums = p.drumSceneOfBars()
                // A bar without drums (acidcore's Break: the 303 alone) has nothing to fill.
                val beat = p.drumBars()
                val lost = marks.filter { m -> beat[m.bar].any { it.isNotEmpty() } && p.allScenes[drums[m.bar]].label.let { it != PartIntents.FILL_LABEL && it != DrumArrangement.CRASH_LABEL } }
                if (lost.isNotEmpty()) bad += "${style.id}: lost $lost"
            }
            assertTrue(style.id, p.allScenes.size <= Scene.MAX - DrumArrangement.FREE_SLOTS)
        }
        assertTrue(bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun benjaminsSongHadLostThreeFillsAtTheEight() {
        val p = DropVariationTest.benj()
        assertEquals(listOf("fill 159", "fill 175"), missing(p))
        // His song made again (jump up, the same parts): none missing.
        assertEquals(emptyList<String>(), missing(DropVariationTest.song(StarterWizard.style("jump_up")!!)))
    }
}
