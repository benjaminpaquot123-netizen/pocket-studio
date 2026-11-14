package com.pocketstudio.app.padlive

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.onboarding.LaunchActions
import com.pocketstudio.app.onboarding.LaunchScreen
import com.pocketstudio.app.onboarding.LaunchUi
import com.pocketstudio.app.ui.GestureBudget
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import com.pocketstudio.ui.PocketStudioTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pad Live's exit test (n° 25, lot B1), counted from the home screen as a user touches: the first
 * sound in 2 touches (the card, then a pad: Pad Live opens in silence, tour 6 padlive2), the first rhythm that
 * plays in 3 at most, and no function of Pad Live deeper than 3 touches. The seconds (first sound
 * under 5 s, a rhythm under 30 s) are measured on Benjamin's phone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp-xxhdpi")  // a phone upright, as the mockup's
class PadLiveTouchCountTest {
    @get:Rule
    val compose = createComposeRule()

    /** What the app does with each gesture, as a log (« touch 0 down », « page 1 »…). */
    private val done = ArrayList<String>()

    /** Page A as a new « Live » project gets it: the ready-to-play set (its loops, its hits). */
    private val pads: LivePads = PadLive.placed(LivePads(), 0, LiveStarter.PADS.mapIndexed { i, p ->
        LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain)
    }).withAt(15, null)  // audit ultra round 1, n° 5: the set fills the page; its last pad emptied by the user

    private var openedFromHome = false
    private var open by mutableStateOf(false)
    private var shown = false

    /** The home screen (once per test: then only « back home »). */
    private fun home() {
        open = false
        if (shown) {
            compose.waitForIdle()
            return
        }
        shown = true
        compose.setContent {
            PocketStudioTheme {
                if (!open) {
                    LaunchScreen(LaunchUi("Mon projet 3"), LaunchActions(padLive = { open = true; openedFromHome = true }))
                } else {
                    PadLiveScreen(
                        PadLiveUi(pads, List(LivePads.ALL) { 0 }, page = 0, pack = "fete", playing = false, tempoBpm = 124.0, beatsPerBar = 4, canUndo = true),
                        actions(), clockOf = { -1 }, record = { RecSpot { done += "rec" } }, still = true,
                    )
                }
            }
        }
    }

    private fun actions() = PadLiveActions(
        back = { done += "back" },
        togglePlay = { done += "play" },
        nudgeTempo = { done += "tempo $it" },
        openTempo = { done += "tempo sheet" },
        touch = { pad, down -> done += "touch $pad ${if (down) "down" else "up"}" },
        setPage = { done += "page $it" },
        choosePack = { done += "pack $it" },
        browse = { done += "browse $it" },
        setMode = { pad, m -> done += "mode $pad $m" },
        setGroup = { pad, g -> done += "group $pad $g" },
        setBeats = { pad, b -> done += "beats $pad $b" },
        setGain = { pad, _ -> done += "gain $pad" },
        clear = { done += "clear $it" },
        stopAll = { done += "stop all" },
        filter = { done += "filter ${it != null}" },
        echo = { done += "echo $it" },
        roll = { done += "roll $it" },
        riser = { done += "riser" },
        brake = { done += "brake $it" },
        stutter = { done += "stutter $it" },
        undo = { done += "undo" },
        openLive = { done += "live" },
        effects = { done += "effects" },
        launchRow = { done += "row $it" },
        setPitch = { pad, v -> done += "pitch $pad $v" },
        setReverse = { pad, v -> done += "reverse $pad $v" },
        played = { done += "played" },
        challenge = { done += "challenge" },
        share = { done += "share" },
    )

    /** [action] from the home screen in [budget] touches at most, and the app did [expect]. */
    private fun reach(action: String, budget: Int, expect: String, path: GestureBudget.() -> Unit) {
        done.clear()
        openedFromHome = false
        home()
        val g = GestureBudget(compose, action)
        g.tap("launch_padlive")
        assertTrue(openedFromHome)
        g.path()
        g.assertWithin(budget)
        assertTrue("« $action »: $expect not done (${g.describe()}; done: $done)", done.any { it.startsWith(expect) })
    }

    /** A pad pressed and let go, as a finger does (a pad acts on the finger's down). */
    private fun GestureBudget.press(tag: String) {
        compose.onNodeWithTag(tag).performTouchInput { down(center); up() }
        count(tag)
        compose.waitForIdle()
    }

    @Test
    fun theFirstSoundIsTwoTouchesAway() {
        // Tour 6, padlive2 (P2): the card opens Pad Live in silence (no welcome hit any more, PadLiveSilentOpenTest);
        // the first sound is the first pad touched: two touches from the home screen.
        reach("Premier son", 2, "touch 1 down") { press("padlive_pad_1") }
        assertTrue(PadLive.modeOf(pads.at(1)!!) == PadMode.SHOT)
    }

    @Test
    fun theFirstRhythmPlaysInThreeTouchesAtMost() {
        // The kick's loop: one touch on its pad (it starts at once, the transport stopped).
        reach("Premier rythme (une boucle)", 2, "touch 0 down") { press("padlive_pad_0") }
        assertTrue(!pads.at(0)!!.oneShot)
        // Or the loop and the 303 together: three touches.
        reach("Premier rythme (deux boucles)", 3, "touch 2 down") { press("padlive_pad_0"); press("padlive_pad_2") }
        assertEquals(PadMode.LOOP, PadLive.modeOf(pads.at(2)!!))
    }

    @Test
    fun noFunctionOfPadLiveIsDeeperThanThreeTouches() {
        reach("Lecture", 2, "play") { tap("padlive_play") }
        reach("Tempo plus vite", 2, "tempo 1") { tap("padlive_tempo_more") }
        reach("Tempo moins vite", 2, "tempo -1") { tap("padlive_tempo_less") }
        reach("Tempo au chiffre", 2, "tempo sheet") { tap("padlive_tempo") }
        reach("Enregistrer", 2, "rec") { tap("padlive_rec_spot") }
        reach("Retour", 2, "back") { tap("padlive_back") }
        PadLive.PAGE_NAMES.drop(1).forEachIndexed { i, letter ->
            reach("Page $letter", 2, "page ${i + 1}") { tap("padlive_page_$letter") }
        }
        PadLive.PACKS.forEach { pack ->
            reach("Pack ${pack.name}", 3, "pack ${pack.id}") { tap("padlive_pack"); tap("padlive_pack_${pack.id}") }
        }
        reach("Son d'un pad vide", 2, "browse") { press("padlive_pad_15") }
        reach("Filtre", 2, "filter true") { press("padlive_fx_filter") }
        reach("Écho", 2, "echo true") { press("padlive_fx_echo") }
        reach("Roulement", 2, "roll SIXTEENTH") { press("padlive_fx_roll") }
        // Lot B2: a row launched on the bar, « Enregistrer ce que je joue », a pad's own sound.
        reach("Lancer une ligne", 2, "row 0") { tap("padlive_row_0") }
        reach("Enregistrer ce que je joue", 2, "played") { tap("padlive_played") }
        reach("Hauteur d'un pad", 3, "pitch 0 1") { tap("padlive_mode_0"); tap("padlive_pitch_up") }
        reach("Pad à l'envers", 3, "reverse 0 true") { tap("padlive_mode_0"); tap("padlive_reverse") }
        reach("Montée", 3, "riser") { tap("padlive_more"); press("padlive_fx_riser") }
        reach("Chute", 3, "brake true") { tap("padlive_more"); press("padlive_fx_brake") }
        reach("Bégaie", 3, "stutter true") { tap("padlive_more"); press("padlive_fx_stutter") }
        // G8 (plan of the audits, step 4, n° 7): in sight, one touch.
        reach("Tout couper", 2, "stop all") { tap("padlive_stop_all") }
        reach("Annuler", 3, "undo") { tap("padlive_more"); tap("padlive_undo") }
        reach("Briques", 3, "live") { tap("padlive_more"); tap("padlive_live") }
        reach("Effets", 3, "effects") { tap("padlive_more"); tap("padlive_effects") }
        PadMode.entries.forEach { m ->
            reach("Mode ${m.name}", 3, "mode 0 $m") { tap("padlive_mode_0"); tap("padlive_mode_${m.name.lowercase()}") }
        }
        reach("Un seul à la fois", 3, "group 0 1") { tap("padlive_mode_0"); tap("padlive_group_1") }
        reach("Longueur", 3, "beats 0 8") { tap("padlive_mode_0"); tap("padlive_beats_8") }
        reach("Changer le son", 3, "browse 0") { tap("padlive_mode_0"); tap("padlive_change_sound") }
        reach("Vider", 3, "clear 0") { tap("padlive_mode_0"); tap("padlive_clear") }
        // Lot B3: the challenge, « Partager ma perf », « Fête » (its pads played: the 3rd touch).
        reach("Défi", 2, "challenge") { tap("padlive_challenge_open") }
        reach("Partager ma perf", 3, "share") { tap("padlive_more"); tap("padlive_share") }
        reach("Fête", 3, "party") {
            tap("padlive_more"); tap("padlive_party_open")
            compose.onNodeWithTag("padlive_party").assertExists()
            done += "party"
        }
    }
}

/** The ● of the bar in these tests (the app gives the Live take's own). */
@androidx.compose.runtime.Composable
private fun RecSpot(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier.size(48.dp).clickable(onClick = onClick).testTag("padlive_rec_spot"),
    )
}
