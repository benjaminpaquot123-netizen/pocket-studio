package com.pocketstudio.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.pocketstudio.app.mixer.AutomationActions
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.songBars
import androidx.compose.ui.test.click
import com.pocketstudio.app.mixer.AutomationCard
import com.pocketstudio.app.mixer.AutomationUi
import com.pocketstudio.app.mixer.Where
import com.pocketstudio.app.mixer.targetRank
import com.pocketstudio.app.mixer.whatPlaysGroups
import com.pocketstudio.app.ui.mixerStrips
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** « Ce qui joue » says everything in words with a way to it; the picker puts the filter first; the ramp tool. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h1600dp-xxhdpi")
class WhatPlaysTest {
    @get:Rule
    val compose = createComposeRule()

    companion object {
        fun wizard(id: String): Project {
            val s = StarterWizard.STYLES.single { it.id == id }
            val lines = s.lines.associate { it.role to null as PadSample? }
            val chosen = StarterWizard.Part.entries.associateWith { StarterWizard.soundsFor(it, s).first() }
            return StarterWizard.build(s, lines, s.melody.mood, chosen)
        }
    }

    @Test
    fun everyLaneOfTheAssistantsSongHasARowThatLeadsToIt() {
        val p = wizard("jump_up")
        assertTrue(p.automation.isNotEmpty())
        val groups = whatPlaysGroups(p, null, mixerStrips(p))
        val rows = groups.flatMap { it.rows }
        p.automation.indices.forEach { i -> assertTrue("lane $i", rows.any { it.where == Where.Lane(i) && it.moves }) }
        rows.forEach { assertTrue(it.title, it.title.isNotBlank() && it.detail.isNotBlank()) }
        // The Break has its own list, and the track names are the project's.
        val brk = p.allScenes.indexOfFirst { it.label == StarterWizard.SongForm.BREAK_LABEL }.takeIf { it >= 0 } ?: 3
        assertTrue(whatPlaysGroups(p, brk, mixerStrips(p)).isNotEmpty())
        val one = groups.first { it.rows.any { r -> r.moves } && it.ch in 0..15 }
        assertTrue(whatPlaysGroups(p, null, mixerStrips(p), onlyCh = one.ch).all { it.ch == one.ch })
    }

    @Test
    fun theFilterComesFirstInThePicker() {
        val ch = List(16) { com.pocketstudio.model.Channel() }.toMutableList()
        ch[2] = ch[2].copy(inserts = listOf(Insert.of(EffectType.CHORUS), Insert.of(EffectType.FILTER)) + ch[2].inserts.drop(2))
        val targets = listOf(
            AutoTarget(AutoTarget.PAN, 2), AutoTarget(AutoTarget.INSERT_PARAM, 2, 0, 0), AutoTarget(AutoTarget.SEND, 2, 0),
            AutoTarget(AutoTarget.VOLUME, 2), AutoTarget(AutoTarget.INSERT_PARAM, 2, 1, 0),
        ).sortedBy { targetRank(it, ch, emptyList()) }
        assertEquals(AutoTarget(AutoTarget.INSERT_PARAM, 2, 1, 0), targets[0])
        assertEquals(AutoTarget(AutoTarget.VOLUME, 2), targets[1])
        assertEquals(AutoTarget(AutoTarget.SEND, 2, 0), targets[2])
        assertEquals(AutoTarget(AutoTarget.PAN, 2), targets.last())
    }

    @Test
    fun twoTapsDrawARampInOneEdit() {
        val lane = AutomationLane(AutoTarget(AutoTarget.VOLUME, 0))
        var edits = 0
        var result = lane
        compose.setContent {
            PocketStudioTheme {
                AutomationCard(
                    AutomationUi(listOf(lane), 16),
                    List(16) { com.pocketstudio.model.Channel() }, emptyList(),
                    AutomationActions(edit = { _, _, f -> edits++; result = f(result, Project()) }),
                )
            }
        }
        compose.onNodeWithTag("auto_tool_line_0").performClick()
        val tag = "lane_${lane.target.pack()}"
        compose.onNodeWithTag(tag).performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 0.5f / 16, height * 0.95f)) }
        compose.mainClock.advanceTimeBy(1000)
        compose.onNodeWithTag(tag).performTouchInput { click(androidx.compose.ui.geometry.Offset(width * 12.5f / 16, height * 0.05f)) }
        compose.waitForIdle()
        assertEquals(1, edits)
        val v = result.values
        assertTrue(v[0]!! < 0.1f && v[12]!! > 0.9f)
        // Straight in between, nothing after.
        assertEquals((v[0]!! + v[12]!!) / 2, v[6]!!, 0.02f)
        assertEquals(null, v[13])
    }
}

/** For the screenshots (the extension properties imported here). */
internal fun Project.allScenesList() = allScenes
internal fun Project.songBarsCount() = songBars
