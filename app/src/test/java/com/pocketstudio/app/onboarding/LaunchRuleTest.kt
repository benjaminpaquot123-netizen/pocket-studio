package com.pocketstudio.app.onboarding

import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectInfo
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** When « Que veux-tu faire ? » comes back, what its state keeps, and what « Continuer » says of the project. */
class LaunchRuleTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    private val now = 1_800_000_000_000L
    private val minute = 60_000L

    @Test
    fun askedAgainOnlyAfterHalfAnHourAwayAndNeverWhileMusicPlays() {
        // Never left (a first start, the prefs cleared): the gate's own state decides.
        assertFalse(LaunchRule.askAgain(leftAt = 0, now = now, busy = false))
        // A rotation, a trip to another app: not asked.
        assertFalse(LaunchRule.askAgain(now, now, busy = false))
        assertFalse(LaunchRule.askAgain(now - 29 * minute, now, busy = false))
        // Half an hour away or more: asked again.
        assertTrue(LaunchRule.askAgain(now - 30 * minute, now, busy = false))
        assertTrue(LaunchRule.askAgain(now - 5 * 60 * minute, now, busy = false))
        // The song kept playing (or a take ran) in the background: the user comes back to it.
        assertFalse(LaunchRule.askAgain(now - 5 * 60 * minute, now, busy = true))
        // A clock set back: not « away ».
        assertFalse(LaunchRule.askAgain(now + 60 * minute, now, busy = false))
    }

    @Test
    fun theGateGoesBackToTheChooserOnlyFromWhatItOpened() {
        val gate = LaunchGate(open = true)
        // « Continuer »: closed, nothing comes back to it.
        gate.choose()
        assertFalse(gate.open)
        assertFalse(gate.returned())
        assertFalse(gate.open)
        // « Mes projets », then Retour: the chooser again.
        gate.ask()
        gate.choose(comeBack = true)
        assertFalse(gate.open)
        assertTrue(gate.back)
        assertTrue(gate.returned())
        assertTrue(gate.open)
        // « Mes projets », then a project opened: never back to it.
        gate.choose(comeBack = true)
        gate.settled()
        assertFalse(gate.returned())
        assertFalse(gate.open)
    }

    @Test
    fun theGateIsClosedAtTheFirstLaunchAndKeptBySavedState() {
        val saved = with(LaunchGate.Saver) {
            val scope = androidx.compose.runtime.saveable.SaverScope { true }
            scope.save(LaunchGate(open = false, back = true))!!
        }
        val back = LaunchGate.Saver.restore(saved)!!
        assertFalse(back.open)
        assertTrue(back.back)
    }

    @Test
    fun continueShowsTheNameStyleLengthAndLastEdit() {
        val style = StarterWizard.STYLES.first()
        // 3 parts of 4 bars at 120 BPM: 3 × 8 s = 0:24.
        val p = Project(name = "Ma nuit", tempoBpm = 120.0, style = style.id).copy(song = List(3) { SongPart(scene = 0, bars = 4) })
        val info = ProjectInfo("p1", "Ma nuit", modified = now - 2 * 60 * minute, tempoBpm = 120.0, bars = 4, scenes = 1, hasRemix = false)
        val ui = launchUi(p, info, now, simple = true, missionsActive = false)
        assertEquals("Ma nuit", ui.name)
        assertEquals(style.name, ui.style)
        assertEquals("0:24 · 3 parties", ui.length)
        assertEquals("modifié il y a 2 h", ui.edited)
        assertTrue(ui.simple)
        // A loop (no song yet), never saved: its bars, no date.
        val loop = launchUi(Project(name = "Brouillon"), ProjectInfo("x", "Brouillon", 0, 120.0, 2, 1, false), now, simple = false, missionsActive = true)
        assertNull(loop.style)
        assertEquals("Boucle de 2 mes.", loop.length)
        assertNull(loop.edited)
        assertTrue(loop.missionsActive)
        // Translated.
        I18n.lang = Lang.NL
        assertEquals("bewerkt gisteren", launchUi(p, info.copy(modified = now - 30 * 60 * minute), now, false, false).edited)
    }
}
