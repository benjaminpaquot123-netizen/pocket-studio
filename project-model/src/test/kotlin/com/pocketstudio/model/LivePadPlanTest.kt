package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The live pads the assistant prepares from its song: what goes on them, and the stems they are rendered from. */
class LivePadPlanTest {
    /** A style's song as the assistant makes it with its advised choices (every line, its parts, its 303). */
    private fun song(id: String): Project {
        val s = StarterWizard.style(id)!!
        val parts = StarterWizard.defaultParts(s).associateWith { StarterWizard.soundsFor(it, s).first() }
        return StarterWizard.build(s, s.lines.associate { it.role to null }, s.melody.mood, parts)
    }

    @Test
    fun everyStyleFillsItsPadsWithItsTracksThenTheWholeScene() {
        for (style in StarterWizard.STYLES) {
            val p = song(style.id)
            val plan = LivePadPlan.of(p)
            val id = style.id
            assertTrue(id, plan.isNotEmpty())
            assertTrue(id, plan.size <= p.live.size)
            // The drums first; each track at most once, playing in the scene it is taken from.
            assertEquals(id, SongPart.DRUMS, plan.first().key)
            val keys = plan.mapNotNull { it.key }
            assertEquals(id, keys.size, keys.toSet().size)
            for (spec in plan) {
                assertTrue("$id $spec", spec.name.isNotBlank() && spec.name.length <= 40)
                assertTrue("$id $spec", spec.scene in p.allScenes.indices)
                if (spec.key != null) assertTrue("$id $spec", p.cellHasContent(spec.key, spec.scene))
                if (spec.oneShot) assertTrue("$id $spec", spec.bars in 1..LivePadPlan.MAX_ONE_SHOT_BARS)
                else assertTrue("$id $spec", spec.bars in 1..LivePadPlan.MAX_LOOP_BARS)
            }
            // With several tracks and room left, the whole scene comes last (a loop of every track).
            if (keys.size >= 2 && keys.size < p.live.size) assertEquals(id, null, plan.last().key)
        }
    }

    @Test
    fun theRiserGivesItsImpactFromTheSceneWhereTheSongOpensIt() {
        val p = StarterWizard.STYLES.map { song(it.id) }.first { pr -> pr.instrumentTracks.any { it.name == StarterWizard.RISER_TRACK } }
        val riser = p.instrumentTracks.first { it.name == StarterWizard.RISER_TRACK }
        val spec = LivePadPlan.of(p, max = LivePads.MAX).first { it.key == SongPart.inst(riser.id) }
        assertTrue(spec.oneShot)
        assertEquals(LivePadPlan.IMPACT, spec.name)
        // Rendered alone, the riser's filter is held where the song first plays that scene: open.
        val view = p.stemView(spec.key, spec.scene)
        val cutoff = AutoTarget(AutoTarget.INSERT_PARAM, InstrumentTrack.mixerChannel(riser.id), 0, 0)
        assertTrue(view.settingOf(cutoff) > p.settingOf(cutoff))
        assertTrue(view.automation.none { it.mode == AutomationLane.SONG })
    }

    @Test
    fun aStemKeepsOnlyItsTrack() {
        val p = song("acid_techno")
        val plan = LivePadPlan.of(p)
        val scene = plan.first { it.key == SongPart.DRUMS }.scene
        val drums = p.stemView(SongPart.DRUMS, scene)
        assertTrue(drums.drums.lanes.any { it.hits.isNotEmpty() })
        assertFalse(drums.acid.steps.any { it.gate })
        assertTrue(drums.instrumentTracks.all { it.notes.notes.isEmpty() })
        val acid = p.stemView(SongPart.ACID, plan.first { it.key == SongPart.ACID }.scene)
        assertTrue(acid.drums.lanes.all { it.hits.isEmpty() })
        assertTrue(acid.acid.steps.any { it.gate })
        // The whole scene keeps every track; the sounds (pad samples) stay the project's.
        val all = p.stemView(null, scene)
        assertEquals(p.sceneView(scene).drums.lanes.map { it.hits }, all.drums.lanes.map { it.hits })
        assertEquals(p.drums.lanes.map { it.sample }, all.drums.lanes.map { it.sample })
        assertTrue(all.live.clips.all { it == null } && !all.livePadsPending)
    }

    @Test
    fun anOldOrEmptyProjectHasNothingToPrepareAndTheFlagIsNotSaved() {
        assertTrue(LivePadPlan.of(Project()).isEmpty())
        assertFalse(ProjectJson.encode(Project()).contains("livePadsPending"))
        val pending = Project(livePadsPending = true)
        assertTrue(ProjectJson.decode(ProjectJson.encode(pending)).livePadsPending)
    }

    @Test
    fun aSettingIsReadAndWrittenAsALaneWritesIt() {
        val p = song("house")
        val pan = AutoTarget(AutoTarget.PAN, 0)
        assertEquals(0.5f, p.settingOf(pan), 1e-6f)
        assertEquals(-1f, p.withSetting(pan, 0f).mixer.channels[0].pan, 1e-6f)
        val volume = AutoTarget(AutoTarget.VOLUME, 1)
        assertEquals(0.3f, p.withSetting(volume, 0.3f).settingOf(volume), 1e-6f)
        val cutoff = AutoTarget.acid(0)
        assertEquals(0.9f, p.withSetting(cutoff, 0.9f).acid.sound.cutoff, 1e-6f)
        // A Live page effect is no setting: the project stays as it is.
        assertEquals(p, p.withSetting(AutoTarget.live(AutoTarget.LIVE_FILTER), 0.1f))
    }
}
