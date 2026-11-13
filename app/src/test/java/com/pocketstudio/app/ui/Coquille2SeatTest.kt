package com.pocketstudio.app.ui

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pocketstudio.app.human.HumanTester
import com.pocketstudio.app.human.SeatOptions
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lot « coquille2 » (tour 6): the acceptance phrases left in the shared files, each proven by the bench in the conditions
 * of its seat, the whole app. Seat A c (exports): fr, phone, font 100, Mode simple.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = HumanTester.PHONE)
class Coquille2SeatTest : HumanTester("coquille2") {
    private fun tap(tag: String): String {
        val at = reachTag(tag) ?: error("no $tag on:\n" + screenLines().joinToString("\n"))
        return act("touche ${at.first} ${at.second}")
    }

    private fun screen() = screenLines().joinToString("\n")

    /** Seat A c's start: « Fais-moi un morceau », Simple, Hip-hop › Trap, « Créer et écouter ». */
    private fun trapSong() {
        begin(SeatOptions.parse(mapOf("lang" to "fr", "device" to "phone", "font" to "100", "mode" to "simple")).first)
        tap("welcome_guided"); tap("mode_card_simple"); tap("wizard_family_Hip-hop"); tap("wizard_style_trap"); tap("wizard_style_play_trap")
        repeat(5) { act("attends 1") }
    }

    /** Seat A c, geste 18 (E2): a file made, the song not touched: never « Le morceau a changé depuis cet export ». */
    @Test
    fun e2_noSongChangeSaidWhenOnlyTheFilesVolumeIsChosen() {
        assumeTrue(System.getProperty("ps.human.dir").isNullOrBlank())
        trapSong()
        tap("tab_simple_Envoyer")
        tap("share_go")
        repeat(40) { if (rhythm()?.export?.value?.running == true) act("attends 1") }
        act("touche 294 543")  // the phone's share sheet: WhatsApp
        tap("share_bilan_ok")
        // « Autres fichiers » (the Pro export, as the seat went): its row is half under the format buttons (E3, lot exports2).
        compose.onNodeWithTag("share_other").performClick()
        repeat(2) { act("attends 1") }
        assertNull("« a changé » right after the file:\n" + screen(), centerOf("export_stale"))
        // The volume of the next file chosen (the seat's « Streaming »): the song itself is the same.
        tap("export_target_STREAMING")
        act("attends 1")
        assertNull("« Le morceau a changé » though only the file's volume was chosen:\n" + screen(), centerOf("export_stale"))
    }
}
