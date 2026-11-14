package com.pocketstudio.bentest

import androidx.compose.ui.geometry.Rect
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The robot and the apps outside (v4 run, 2026-11-12: the Guide's « reference » card opened the file manager, the robot
 * waited there for Benjamin's back, three times): it never touches what opens another app, and when the app leaves the
 * front anyway it brings it back and does not count that step's timing.
 */
class SortieDeLAppliTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun carnet() = Carnet(File(tmp.root, "c.tsv"))

    // --- Controls that open another app -------------------------------------------------------------------------

    @Test
    fun controlsThatOpenAnotherAppAreRecognised() {
        for (tag in listOf("export_my_exports", "share_my_exports", "my_export_send", "guide_reference_change", "export_cover", "pro_reference", "permission_settings")) {
            assertTrue(tag, Externe.lance(tag))
        }
        for (tag in listOf("guide_option_drop", "guide_skip", "export", "share_go", "tab_Mix")) assertFalse(tag, Externe.lance(tag))
        // « Écouter » opens the file picker only on the reference card (no song picked yet).
        assertTrue(Externe.lance("guide_option_listen") { listOf("guide_panel", "guide_card_mix.reference") })
        assertFalse(Externe.lance("guide_option_listen") { listOf("guide_panel", "guide_card_mix.mono") })
    }

    private class Guide : JourneyDriver {
        val tags = linkedSetOf("guide_panel", "guide_card_mix.reference", "guide_option_listen", "guide_skip")
        val clicks = ArrayList<String>()
        override fun count(tag: String, merged: Boolean) = if (tag in tags) 1 else 0
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) = Unit
        override fun click(tag: String) {
            clicks += tag
            if (tag == "guide_skip") {
                tags -= "guide_card_mix.reference"
                tags -= "guide_option_listen"
                tags += "guide_all_done"
            }
        }
        override fun idle(ms: Long) = Thread.sleep(minOf(ms, 5))
        override fun tags(pattern: Regex) = tags.filter { pattern.matches(it) }
    }

    @Test
    fun theGuideSkipsTheCardThatOpensTheFilePickerAndSaysSo() {
        val d = Guide()
        val c = carnet()
        val t = ToutTester(Journey(d), Still, NoAppli, c, tmp.root)
        assertFalse("skipped, not applied", t.carteTraitee("guide_house", "guide_card_mix.reference"))
        assertEquals("only « Passer » touched: ${d.clicks}", listOf("guide_skip"), d.clicks)
        val note = c.lines().single { it.kind == Carnet.NOTE }
        assertEquals("guide_house", note.step)
        assertTrue(note.text, note.text.startsWith(Externe.RAISON) && "guide_option_listen" in note.text)
    }

    @Test
    fun aStepThatMeetsSuchAControlIsPassedNotFailed() {
        val c = carnet()
        val steps = listOf(Etape("a", "A", 10_000) { throw PasseExterne("export_my_exports") })
        Passage(c, steps).run()
        val end = c.lines().single { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC }
        assertEquals(Carnet.REUSSI, end.kind)
        assertTrue(end.text, end.text.startsWith(Externe.RAISON))
    }

    // --- The app leaves the front -------------------------------------------------------------------------------

    private class Horloge(var now: Long = 1_000_000L) : () -> Long {
        override fun invoke() = now
    }

    @Test
    fun anExitIsBroughtBackNotedAndMarksTheStepSkewed() {
        val c = carnet()
        val h = Horloge()
        var back = 0
        val s = Sorties(c, h) { back++ }
        s.etape = "guide_house"
        s.arret()
        assertEquals(1, back)
        s.arret()  // a second stop before the return: still one return
        assertEquals(1, back)
        h.now += 7_400
        s.retour()
        val lines = c.lines()
        val note = lines.single { it.kind == Carnet.NOTE }
        assertEquals("sortie de l'appli à l'étape guide_house, revenue après 7 s", note.text)
        assertEquals("guide_house", lines.single { it.kind == Carnet.FAUSSE }.step)
    }

    @Test
    fun whatTheRobotWantedIsNotAnExit() {
        val c = carnet()
        var back = 0
        val s = Sorties(c) { back++ }
        s.arret()                                   // no run
        s.etape = "style_house"
        s.arret(rotation = true)                    // the phone turning
        s.arret(ecranAllume = false)                // the screen went off
        s.voulue = true
        s.arret()                                   // a question over the studio
        s.voulue = false
        s.etape = "ecran_eteint"
        s.arret()                                   // the screen-off step's own
        assertEquals(0, back)
        s.retour()
        assertTrue(c.lines().isEmpty())
    }

    @Test
    fun aSkewedStepIsNotLitAndIsNotTooLong() {
        val c = carnet()
        val h = Horloge()
        val steps = listOf(Etape("a", "A", 1_000) { h.now += 5_000; "fini" }, Etape("b", "B", 1_000) { "fini" })
        c.add(Carnet.DEBUT, "a")
        c.add(Carnet.FEU, "a", "rouge;3 décrochage(s)")
        c.add(Carnet.FAUSSE, "a", "sortie de l'appli")
        c.add(Carnet.REUSSI, "a", "fait")
        c.add(Carnet.DEBUT, "b")
        c.add(Carnet.FEU, "b", "rouge;3 décrochage(s)")
        c.add(Carnet.REUSSI, "b", "fait")
        assertEquals(Feu.GRIS, Rapport.feux(c.lines(), steps)["a"])
        assertEquals(Feu.ROUGE, Rapport.feux(c.lines(), steps)["b"])
        val md = Rapport.markdown(c.lines(), steps, emptyList(), tout = true)
        assertTrue(md, md.contains("faussé"))
        // The run itself: a step that took long because the app was away is not « trop longue ».
        val c2 = Carnet(File(tmp.root, "c2.tsv"), h)
        Passage(c2, listOf(Etape("a", "A", 1_000) { c2.add(Carnet.FAUSSE, "a", "sortie"); h.now += 5_000; "fini" }), h).run()
        assertEquals(Carnet.REUSSI, c2.lines().single { it.kind == Carnet.REUSSI || it.kind == Carnet.ECHEC }.kind)
    }

    private object Still : Sonde {
        override fun lire() = Sonde.Lecture(0, 0f, 0f, false, true)
    }

    private object NoAppli : Appli {
        override fun ouvrirStudio() = Unit
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun capture(file: File) = false
        override fun demander(question: String, maxMs: Long): Boolean? = null
    }
}
