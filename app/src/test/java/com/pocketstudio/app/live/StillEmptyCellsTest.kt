package com.pocketstudio.app.live

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Benjamin (2026-11-01): nothing moves on an inactive pad or cell; the beat's wave through the empty dots is gone. */
class StillEmptyCellsTest {
    @Test
    fun theEmptyDotTakesNoAnimation() {
        val look = File("src/main/java/com/pocketstudio/app/live/LiveGridLook.kt").readText()
        assertTrue("internal fun DrawScope.emptyDot()" in look)
        val screen = File("src/main/java/com/pocketstudio/app/live/LiveScreen.kt").readText()
        assertFalse("A wave still runs through the grid", Regex("""\bwave\(|WaveSource""").containsMatchIn(look + screen))
    }
}
