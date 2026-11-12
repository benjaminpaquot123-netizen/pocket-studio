package com.pocketstudio.model

import org.junit.Assert.assertEquals
import org.junit.Test

/** Audit 06: a Pompe on « Le son du kick » in a project made by hand (no sidechain key) follows the kick's pad. */
class PumpKeyTest {
    private fun withPump(p: Project, ch: Int, onTheSound: Boolean, bypass: Boolean = false): Project {
        val pump = Insert.of(EffectType.PUMP).let { it.copy(params = it.params.mapIndexed { k, v -> if (k == 3) (if (onTheSound) 1f else 0f) else v }, bypass = bypass) }
        val channels = p.mixer.channels.toMutableList()
        channels[ch] = channels[ch].copy(inserts = channels[ch].inserts.mapIndexed { i, ins -> if (i == 0) pump else ins })
        return p.copy(mixer = p.mixer.copy(channels = channels))
    }

    @Test
    fun aPumpOnTheKicksSoundHearsTheKickPad() {
        val plain = Project()
        assertEquals(-1, plain.mixer.heardSidechainKey)
        // On the grid's hits: no key needed.
        assertEquals(-1, withPump(plain, 1, onTheSound = false).mixer.heardSidechainKey)
        // On the kick's sound: the first pad's strip (the kick), never silent.
        assertEquals(Mixer.padChannel(StarterWizard.Role.Kick.lane), withPump(plain, 1, onTheSound = true).mixer.heardSidechainKey)
        // Switched off: no key.
        assertEquals(-1, withPump(plain, 1, onTheSound = true, bypass = true).mixer.heardSidechainKey)
        // A key chosen (the assistant's, the guide's) stays the one heard.
        val keyed = withPump(plain, 1, onTheSound = true).let { it.copy(mixer = it.mixer.copy(sidechainKey = Mixer.padChannel(3))) }
        assertEquals(Mixer.padChannel(3), keyed.mixer.heardSidechainKey)
        // Nothing saved changes: the file still has no key.
        val back = ProjectJson.decode(ProjectJson.encode(withPump(plain, 1, onTheSound = true)))
        assertEquals(-1, back.mixer.sidechainKey)
    }
}

/** Audit 06: a loop curve's own length (the whole part) is kept, sanitized, and absent from old files. */
class LoopCurveLengthTest {
    @Test
    fun aLoopCurveKeepsItsOwnLength() {
        val lane = AutomationLane(AutoTarget(AutoTarget.PAN, 1))
        assertEquals(0, lane.loopSteps)
        assertEquals(32, ProjectJson.decode(ProjectJson.encode(Project(automation = listOf(lane.copy(loopSteps = 32))))).automation[0].loopSteps)
        // Old files (no length): the drum loop's, as before; nonsense: the same.
        assertEquals(false, ProjectJson.encode(Project(automation = listOf(lane))).contains("loopSteps"))
        assertEquals(0, lane.copy(loopSteps = 500).sanitized().loopSteps)
        assertEquals(0, lane.copy(loopSteps = -3).sanitized().loopSteps)
    }
}
