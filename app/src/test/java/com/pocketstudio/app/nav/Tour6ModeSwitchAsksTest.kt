package com.pocketstudio.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.pocketstudio.app.settings.SettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Audit ultra round 6, N17 (seat C b #38): a touch meant for « Mes projets » on the Plus list (still scrolled where it
 * was left) fell on « Passer en mode Simple »: every screen changed at once, without a question or a word. The entry
 * now opens « Mode de ce projet », the same question as the top bar's chip: the two modes, the one in use marked, what
 * changes said; the mode changes only once a card is touched, and « Fermer » leaves it as it was.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp-xhdpi")
class Tour6ModeSwitchAsksTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val h = AppHarness(compose)
    private val shell = Tour6Shell(compose)

    @After
    fun back() = h.finish()

    private fun simpleNow() = SettingsStore.state.value.simpleMode

    @Test
    fun switchToSimpleFromPlusAsksFirst() {
        h.start(simple = false)
        val before = h.vm.project.value
        shell.tap("tab_Plus")
        shell.tap("plus_mode")
        shell.idle(200)
        assertFalse("« Passer en mode Simple » changed the mode at the touch, without asking or saying anything", simpleNow())
        assertTrue("« Mode de ce projet » asks", shell.has("mode_switch"))
        // « Fermer »: nothing changed.
        shell.tap("mode_switch_close")
        assertFalse(simpleNow())
        assertTrue("still on Plus, in Pro", shell.has("plus_mode") && shell.has("tab_Plus"))
        // Asked again and answered: the Mode simple.
        shell.tap("plus_mode")
        shell.tap("mode_card_simple")
        assertTrue("the mode chosen on its card", shell.reached(3_000) { simpleNow() })
        assertTrue(shell.reached(3_000) { shell.has("tab_simple_Morceau") })
        assertEquals("the song is the same", before, h.vm.project.value)
    }

    @Test
    fun switchToProFromTheSimplePlusAsksFirst() {
        h.start(simple = true)
        shell.tap("thumb_more")
        shell.tap("plus_mode")
        shell.idle(200)
        assertTrue("« Passer en mode Pro » changed the mode at the touch, without asking or saying anything", simpleNow())
        assertTrue("« Mode de ce projet » asks", shell.has("mode_switch"))
        shell.tap("mode_card_pro")
        assertTrue("the mode chosen on its card", shell.reached(3_000) { !simpleNow() })
        assertTrue(shell.reached(3_000) { shell.has("tab_Structure") })
    }
}
