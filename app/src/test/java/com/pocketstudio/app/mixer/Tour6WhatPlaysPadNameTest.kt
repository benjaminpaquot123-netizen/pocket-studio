package com.pocketstudio.app.mixer

import com.pocketstudio.app.rhythm.padName
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.I18n
import com.pocketstudio.model.i18n.Lang
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 6, seat Fa-1 (French, Mode simple, screen 199): « Ce qui joue » said « Pad 5 » where the pad reads
 * « Claquement de mains ». Each pad's group is named as the pad shows it, in the 15 languages and in the Mode simple.
 */
class Tour6WhatPlaysPadNameTest {
    @After
    fun end() {
        I18n.lang = Lang.FR
        I18n.everyday = false
    }

    @Test
    fun whatPlaysNamesAPadAsThePadShowsIt() {
        val p0 = Project()
        // Every pad with a reverb send: each one has its group.
        val p = p0.copy(mixer = p0.mixer.copy(channels = p0.mixer.channels.mapIndexed { ch, c ->
            if (Mixer.isPad(ch)) c.copy(sends = c.sends.mapIndexed { i, v -> if (i == 0) 0.5f else v }) else c
        }))
        val bad = ArrayList<String>()
        for (lang in Lang.entries) for (everyday in listOf(false, true)) {
            I18n.lang = lang
            I18n.everyday = everyday
            val groups = whatPlaysGroups(p, null, emptyList()).filter { Mixer.isPad(it.ch) }
            assertTrue("16 pads' groups (${groups.size})", groups.size == Mixer.PADS)
            for (g in groups) {
                val lane = g.ch - Mixer.FIRST_PAD
                val shown = padName(p, lane)
                if (g.name != shown) bad += "${lang.code}${if (everyday) " simple" else ""} pad ${lane + 1}: « ${g.name} », pad « $shown »"
            }
        }
        assertTrue("${bad.size} pads named otherwise in « Ce qui joue »:\n" + bad.take(40).joinToString("\n"), bad.isEmpty())
    }
}
