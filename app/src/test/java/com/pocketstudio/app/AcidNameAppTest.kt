package com.pocketstudio.app

import com.pocketstudio.app.midi.KnobScreen
import com.pocketstudio.app.mixer.BASIC_STRIPS
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.app.ui.simpleTracks
import com.pocketstudio.app.ui.timelineRows
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Benjamin (2026-10-29): « Acid 303 » is the 303's one name, on every screen and in every
 * language: its editor's tab, the controller's page, Mix, Mon morceau, the timeline.
 */
class AcidNameAppTest {
    @After
    fun french() {
        I18n.lang = Lang.FR
    }

    @Test
    fun everyScreenSaysAcid303() {
        val p = Project().let { it.copy(acid = it.acid.copy(steps = it.acid.steps.mapIndexed { i, s -> if (i == 0) s.copy(gate = true) else s })) }
        Lang.entries.forEach { lang ->
            I18n.lang = lang
            val names = listOf(
                AppTab.Acide.label,
                KnobScreen.Acide.label,
                DefaultNames.shown(BASIC_STRIPS.first { it.ch == 1 }.name),
                DefaultNames.shown(timelineRows(p).first { it.key == SongPart.ACID }.name),
                simpleTracks(p).first { it.key == SongPart.ACID }.name,
            )
            names.forEach { assertEquals(lang.code, "Acid 303", it) }
        }
    }
}
