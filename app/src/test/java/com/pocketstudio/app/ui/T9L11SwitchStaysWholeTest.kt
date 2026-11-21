package com.pocketstudio.app.ui

import androidx.compose.ui.test.onAllNodesWithTag
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tour 9, L10's rest N1 (seat t9-27: id, tablet 8", 100 %, Pro, the gabber tutorial, chapter 7 « Memahami bagian »):
 * the Structure space's « Timeline | Bagian » switch rode at the top of the parts' column; the column moved down a little
 * (the tutorial bringing a part into view) and the two tabs were cut under the BPM bar (screens 105, 110, 111).
 * Acceptance: the switch stays whole, at the top of the Structure space, however far its parts are scrolled.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class T9L11SwitchStaysWholeTest : HumanTester("t9l11n1") {
    private fun screen() = screenLines().joinToString("\n")
    private fun has(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    /** The switch's two tabs as the seat reads them: there, and neither cut. */
    private fun tabs() = screenLines().filter { it.startsWith("onglet « Timeline »") || it.startsWith("onglet « Bagian »") }

    @Test
    fun theStructureSwitchStaysWholeWhenThePartsScroll() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        val o = SeatOptions.parse(mapOf("lang" to "id", "device" to "tablet8", "font" to "100", "mode" to "pro", "amenee" to "tuto:gabber:7")).first
        begin(o)
        amenee(o.amenee!!, java.io.File.createTempFile("amenee", ".txt").apply { deleteOnExit() })
        act("attends 1")
        if (!has("structure_parts")) reachTag("tab_Structure")?.let { act("touche ${it.first} ${it.second}") }
        reachTag("structure_parts")?.let { act("touche ${it.first} ${it.second}") } ?: error("no « Bagian »:\n" + screen())
        act("attends 1")
        assertTrue("the switch whole before any scroll:\n" + screen(), tabs().size == 2 && tabs().none { "[coupé]" in it })
        // The parts' column moved, as the seat's did: a little, then further.
        // The finger takes the column by its « Tambah: » line (the tutorial panel below leaves it little room).
        for (dy in listOf(40, 300)) {
            val at = screenLines().firstOrNull { it.startsWith("texte « Tambah") }?.substringAfterLast("@ ")?.split(",")
                ?: error("no « Tambah: » line in the parts' column:\n" + screen())
            val x = at[0].trim().toInt() + 200
            val y = at[1].trim().toInt()
            act("glisse $x $y $x ${y - dy}")
            act("attends 1")
            assertTrue("the switch cut or gone after the parts moved by $dy px:\n" + screen(), tabs().size == 2 && tabs().none { "[coupé]" in it })
        }
    }
}
