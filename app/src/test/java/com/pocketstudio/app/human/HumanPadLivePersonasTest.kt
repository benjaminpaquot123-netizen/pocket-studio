package com.pocketstudio.app.human

import com.pocketstudio.app.padlive.ChallengeUi
import com.pocketstudio.app.ui.DoubleTouchGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Pad Live's three personas (n° 25, lot B3) on the « testeur humain » bench: the app as just
 * installed, driven only by touches on the screen's pixels where the screen shows things (the
 * bench's own lookups stand for the eyes), and what the sound line says.
 * - Léo, 8: the home card, a pad, the challenge (heard, played back on the lit pad), « Fête ».
 * - Karim, DJ: rows on the bar, the filter held and slid, the tempo, the pages, « Partager ma perf ».
 * - Claire, never made music: the card, « Plus », TalkBack's words on a pad, « Annuler », back home.
 * Each screen is saved under -Pps.persona.dir (if given) for whoever looks at them.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class HumanPadLivePersonasTest : HumanTester("padlive-personas") {
    @Before
    fun notOnTheBench() {
        assumeTrue("not while the bench runs seats", System.getProperty("ps.human.dir").isNullOrBlank())
        DoubleTouchGuard.reset()
    }

    private var shots = 0
    private var who = ""

    /** The screen as the persona sees it now (kept only when a folder is given). */
    private fun look(what: String) {
        val dir = System.getProperty("ps.persona.dir")?.takeIf { it.isNotBlank() } ?: return
        shot(File(dir, "$who-${"%02d".format(++shots)}-$what.png").also { it.parentFile?.mkdirs() })
    }

    private fun tap(where: Pair<Float, Float>?, what: String): String {
        assertNotNull("nothing to touch: $what", where)
        return act("touche ${where!!.first} ${where.second}")
    }

    private fun waitFor(what: String, done: () -> Boolean) {
        val until = System.currentTimeMillis() + 60_000
        while (!done()) {
            if (System.currentTimeMillis() >= until) {
                look("echec")
                val p = project()
                error("$what never came (padlive=${centerOf("padlive")} busy=${centerOf("padlive_busy")} project=${p?.name} home=${p?.padLive?.home} pads=${p?.live?.all?.count { it != null }} starter=${p?.live?.starterOffered})")
            }
            act("attends 0.5")
        }
    }

    /** Brings the node tagged [tag] into the screen's middle part (a swipe at a time), then its middle. */
    private fun reach(tag: String): Pair<Float, Float> {
        repeat(8) {
            val at = centerOf(tag) ?: error("no $tag")
            if (at.second in 200f..(screenHeight - 200f)) return at
            val dy = (at.second - screenHeight / 2f).coerceIn(-500f, 500f)
            act("glisse 300 ${screenHeight / 2f + dy / 2} 300 ${screenHeight / 2f - dy / 2}")
        }
        return centerOf(tag)!!
    }

    /** Home › « Pad Live »: the pads ready to play, on the whole screen. */
    private fun openPadLive() {
        begin()
        look("accueil")
        // The first launch's choices: the Pad Live card is under the others (a swipe up to see it).
        tap(reach("launch_padlive"), "the Pad Live card")
        waitFor("Pad Live") { centerOf("padlive") != null }
        standInForTheStarter()
        waitFor("the pads ready to play") { centerOf("padlive_busy") == null && project()?.live?.at(4) != null }
        look("padlive")
    }

    @Test
    fun leo8PlaysBackTheRhythmAndThrowsAParty() {
        who = "leo"
        openPadLive()
        // A hit pad, straight away (« Klaxon », second row).
        tap(centerOf("padlive_pad_4"), "a hit pad")
        // « Défi »: the panel, the example heard with its pads lit, then his turn.
        tap(centerOf("padlive_challenge_open"), "Défi")
        look("defi")
        assertNotNull("the challenge's panel", centerOf("padlive_challenge"))
        assertNull("the effects make room for it", centerOf("padlive_fx_filter"))
        val c = rhythm()!!.padLive.challenge
        val ready = c.ui.value as ChallengeUi.Ready
        val heard = tap(centerOf("padlive_challenge_listen"), "Écouter")
        assertTrue(heard, heard.contains("exemple"))
        look("ecoute")
        waitFor("his turn") { c.ui.value is ChallengeUi.YourTurn }
        look("a-toi")
        // He plays it back on the pad that lit, one touch a beat (the bench's touches take ~0.9 s each:
        // at 80 BPM, what he hears is 0.75 s a beat; close enough for two stars or more).
        val pad = ready.c.hits.first().pad
        ready.c.hits.forEach { _ -> tap(centerOf("padlive_pad_$pad"), "the rhythm's pad") }
        waitFor("his score") { c.ui.value is ChallengeUi.Done }
        look("resultat")
        assertNotNull(centerOf("padlive_challenge_stars"))
        tap(centerOf("padlive_challenge_close"), "close the challenge")
        // « Plus » › « Fête »: the pads big on the whole screen, then out.
        tap(centerOf("padlive_more"), "Plus")
        tap(centerOf("padlive_party_open"), "Fête")
        look("fete")
        assertNotNull("the party", centerOf("padlive_party"))
        tap(centerOf("padlive_party_pad_$pad"), "a big pad")
        tap(centerOf("padlive_party_leave"), "leave the party")
        assertNull(centerOf("padlive_party"))
        assertNotNull(centerOf("padlive"))
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun karimDjLaunchesRowsHoldsTheFilterAndLooksForTheShare() {
        who = "karim"
        openPadLive()
        // A row on the bar, the tempo up, the filter held and slid into its panel.
        tap(centerOf("padlive_row_0"), "the first row's ▶")
        tap(centerOf("padlive_tempo_more"), "tempo +")
        assertEquals(125.0, project()!!.tempoBpm, 0.01)
        val (x, y) = centerOf("padlive_fx_filter")!!
        act("glisse $x $y $x ${y - 400}")
        look("filtre")
        // Page B: empty pads, then back to A.
        tap(centerOf("padlive_page_B"), "page B")
        assertEquals(1, project()!!.padLive.page)
        tap(centerOf("padlive_page_A"), "page A")
        // « Plus » › « Partager »: no perf kept yet, said plainly.
        tap(centerOf("padlive_more"), "Plus")
        tap(centerOf("padlive_share"), "Partager")
        look("partager")
        assertNotNull("« Partager ma perf »", centerOfText("Partager ma perf"))
        assertNotNull(centerOfText("Pas encore de perf gardée", substring = true))
        act("retour")
        // A harder challenge straight away (the next one is the first).
        tap(centerOf("padlive_challenge_open"), "Défi")
        tap(centerOf("padlive_challenge_go"), "À moi !")
        look("defi-direct")
        assertTrue(rhythm()!!.padLive.challenge.ui.value is ChallengeUi.YourTurn)
        tap(centerOf("padlive_challenge_finish"), "J'ai fini")
        assertTrue(rhythm()!!.padLive.challenge.ui.value is ChallengeUi.Done)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun claireBeginnerFindsHerWayAndBackHome() {
        who = "claire"
        openPadLive()
        // Every pad says what it is (TalkBack), the big ones too.
        tap(centerOf("padlive_pad_0"), "the first pad")
        tap(centerOf("padlive_more"), "Plus")
        look("plus")
        // « Annuler » after a pack change: the page as it was.
        tap(centerOf("padlive_party_open"), "Fête")
        tap(centerOf("padlive_party_page_B"), "page B in the party")
        assertEquals(1, project()!!.padLive.page)
        act("retour")
        assertNull("back leaves the party first", centerOf("padlive_party"))
        assertNotNull(centerOf("padlive"))
        tap(centerOf("padlive_back"), "←")
        look("retour")
        assertNull("Pad Live closed", centerOf("padlive"))
        // Pad Live is an app of its own: ← goes back to « Que veux-tu faire ? », where its card is.
        assertNotNull("back on the home screen", centerOf("launch_padlive"))
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
