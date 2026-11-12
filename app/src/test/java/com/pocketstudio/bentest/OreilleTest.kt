package com.pocketstudio.bentest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.concurrent.thread

/** « Questions à l'oreille » (niveau 3): the sequence, the skips, the answers, the ❌ proof, the report, the banner. */
class OreilleTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val log = ArrayList<String>()
    private val asked = ArrayList<Question>()
    private var swipes = ArrayList<String>()

    private val prep = object : Preparateur {
        override fun ouverture() { log += "ouverture" }
        override fun morceau(style: String) { log += "morceau:$style" }
        override fun rejouer() { log += "rejouer" }
        override fun arreter() { log += "arreter" }
        override fun ouvrirPads() { log += "pads" }
        override fun ouvrirBatterie(): String { log += "batterie"; return "pad_0" }
    }

    private val sonde = object : Sonde {
        override fun lire() = Sonde.Lecture(0, 0.2f, 0.4f, false, true, playing = true)
    }

    /** An app that answers [answer] to each question (by id), has [materiel], and a kick that stays at the centre unless [kickMoves]. */
    private fun appli(
        materiel: Materiel = Materiel(casqueFilaire = true, bluetooth = true),
        answer: (Question) -> Reponse = { Reponse(Verdict.OUI) },
        kickMoves: Boolean = false,
        bluetoothAfterReady: Boolean = false,
    ) = object : Appli {
        override fun ouvrirStudio() = Unit
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun capture(file: File): Boolean { file.parentFile?.mkdirs(); file.writeText("jpg"); return true }
        override fun demander(question: String, maxMs: Long): Boolean? = null
        // « Prêt » (the plug-in question of Q6) was asked: the phone has what Benjamin connected, if he did.
        override fun materiel() = materiel.copy(bluetooth = materiel.bluetooth || (bluetoothAfterReady && asked.any { it.id == "oreille_6_bluetooth" && it.texte.startsWith("Connecte") }))
        override fun poser(question: Question, maxMs: Long): Reponse { asked += question.copy(maxMs = maxMs); return answer(question) }
        override fun glisserDuBordGauche(tag: String) { swipes += tag }
        override fun kickAuCentre(): Boolean? = !(kickMoves && swipes.size >= 3)
        override fun journalFin(lignes: Int) = "ligne une\nligne deux"
    }

    private fun run(appli: Appli): Pair<Carnet, List<Etape>> {
        val carnet = Carnet(File(tmp.root, "c.tsv"))
        val steps = QuestionsOreille(prep, sonde, appli, carnet, File(tmp.root, "captures")).etapes()
        Passage(carnet, steps).run()
        return carnet to steps
    }

    private fun Carnet.end(step: String) = lines().last { it.step == step && it.kind in setOf(Carnet.REUSSI, Carnet.ECHEC) }

    @Test
    fun theEightQuestionsAreAskedInOnePass() {
        val (carnet, steps) = run(appli())
        assertEquals(
            listOf("oreille_ouverture", "oreille_1_jump_up", "oreille_2_house", "oreille_3_trap", "oreille_4_techno", "oreille_5_casque", "oreille_6_bluetooth", "oreille_7_doigts", "oreille_8_retour"),
            steps.map { it.name },
        )
        // All eight in the banner (the eighth asks for the back gestures, the robot reads the pan around them).
        assertEquals(steps.drop(1).map { it.name }, asked.map { it.id })
        assertTrue(asked.all { it.maxMs in 1..300_000 })
        assertEquals(listOf("jump_up", "house", "trap", "techno"), log.filter { it.startsWith("morceau:") }.map { it.removePrefix("morceau:") })
        assertTrue(steps.all { carnet.end(it.name).kind == Carnet.REUSSI })
        assertEquals("8 oui, 0 non", QuestionsOreille.verdict(carnet.lines()))
    }

    @Test
    fun withoutAWiredHeadsetQuestionFiveIsSkippedNotAsked() {
        val (carnet, _) = run(appli(Materiel(casqueFilaire = false, bluetooth = true)))
        assertFalse(asked.any { it.id == "oreille_5_casque" })
        assertEquals("passée : pas de casque branché", carnet.end("oreille_5_casque").text)
        assertEquals(Carnet.REUSSI, carnet.end("oreille_5_casque").kind)
        // The others still go on.
        assertTrue(asked.any { it.id == "oreille_6_bluetooth" })
        assertEquals(Verdict.PASSEE, QuestionsOreille.reponses(carnet.lines()).first { it.id == "oreille_5_casque" }.verdict)
    }

    @Test
    fun withoutAConnectedBluetoothDeviceTheBannerAsksToConnectOneThenSkipsIfStillNone() {
        val (carnet, _) = run(appli(Materiel(casqueFilaire = true, bluetooth = false)))
        val q6 = asked.filter { it.id == "oreille_6_bluetooth" }
        assertEquals(listOf("Connecte ton casque ou ton enceinte Bluetooth, puis appuie sur Prêt"), q6.map { it.texte })
        assertEquals("passée : pas de Bluetooth connecté", carnet.end("oreille_6_bluetooth").text)
        assertEquals(Carnet.REUSSI, carnet.end("oreille_6_bluetooth").kind)
        assertEquals(Verdict.PASSEE, QuestionsOreille.reponses(carnet.lines()).first { it.id == "oreille_6_bluetooth" }.verdict)
        assertTrue(asked.any { it.id == "oreille_5_casque" })
    }

    @Test
    fun onceBluetoothIsConnectedAfterReadyQuestionSixIsAsked() {
        val (carnet, _) = run(appli(Materiel(casqueFilaire = true, bluetooth = false), bluetoothAfterReady = true))
        val q6 = asked.filter { it.id == "oreille_6_bluetooth" }
        assertEquals(2, q6.size)
        assertTrue(q6[0].texte.startsWith("Connecte ton casque"))
        assertTrue(q6[1].texte, q6[1].texte.startsWith("Bluetooth :"))
        assertEquals(Carnet.REUSSI, carnet.end("oreille_6_bluetooth").kind)
    }

    @Test
    fun skippingTheBluetoothPlugInQuestionSkipsQuestionSix() {
        val (carnet, _) = run(appli(Materiel(casqueFilaire = true, bluetooth = false), bluetoothAfterReady = true, answer = { q -> if (q.texte.startsWith("Connecte")) Reponse(Verdict.PASSEE) else Reponse(Verdict.OUI) }))
        assertEquals(1, asked.count { it.id == "oreille_6_bluetooth" })
        assertEquals("passée : pas de Bluetooth connecté", carnet.end("oreille_6_bluetooth").text)
    }

    @Test
    fun aNoKeepsItsPictureItsJournalAndItsMeasuresAndTheRunGoesOn() {
        val (carnet, _) = run(appli(answer = { q -> if (q.id == "oreille_3_trap") Reponse(Verdict.NON, "808 plates") else Reponse(Verdict.OUI) }))
        val end = carnet.end("oreille_3_trap")
        assertEquals(Carnet.ECHEC, end.kind)
        assertEquals("❌ non : 808 plates", end.text)
        assertTrue(File(tmp.root, "captures/oreille_3_trap.jpg").isFile)
        val notes = carnet.lines().filter { it.step == "oreille_3_trap" && it.kind == Carnet.NOTE }.map { it.text }
        assertTrue(notes.toString(), notes.any { it == "journal : ligne une | ligne deux" })
        assertTrue(notes.toString(), notes.any { it.startsWith("mesures : ") && it.contains("playing=true") })
        // A yes keeps no picture; the next question was asked.
        assertFalse(File(tmp.root, "captures/oreille_2_house.jpg").exists())
        assertTrue(asked.any { it.id == "oreille_4_techno" })
        val kept = QuestionsOreille.reponses(carnet.lines()).first { it.id == "oreille_3_trap" }
        assertEquals(Verdict.NON, kept.verdict)
        assertEquals("oreille_3_trap.jpg", kept.capture)
        assertEquals("808 plates", kept.commentaire)
    }

    @Test
    fun noAnswerFailsTheStepInsteadOfWaitingForever() {
        val (carnet, _) = run(appli(answer = { Reponse(Verdict.SANS_REPONSE) }))
        assertEquals("pas de réponse", carnet.end("oreille_1_jump_up").text)
        assertEquals(Carnet.ECHEC, carnet.end("oreille_1_jump_up").kind)
        assertTrue(carnet.lines().any { it.kind == Carnet.FINI })
    }

    @Test
    fun aSkippedQuestionIsNotAFailure() {
        val (carnet, _) = run(appli(answer = { Reponse(Verdict.PASSEE) }))
        assertEquals("passée", carnet.end("oreille_2_house").text)
        assertEquals(Carnet.REUSSI, carnet.end("oreille_2_house").kind)
        assertEquals("1 oui, 0 non, 7 passées", QuestionsOreille.verdict(carnet.lines()))
    }

    @Test
    fun questionEightAsksBenjaminForTheBackGesturesInsteadOfSwipingForHim() {
        val (carnet, _) = run(appli())
        assertTrue("no synthetic swipe", swipes.isEmpty())
        val q8 = asked.single { it.id == "oreille_8_retour" }
        assertEquals("Fais 3 gestes retour depuis le bord gauche de l'écran, puis appuie sur Fait", q8.texte)
        assertEquals(Carnet.REUSSI, carnet.end("oreille_8_retour").kind)
    }

    @Test
    fun aKickThatLeftTheCentreIsARedWithItsPicture() {
        val (carnet, _) = run(appli(kickMoves = true))
        val end = carnet.end("oreille_8_retour")
        assertEquals(Carnet.ECHEC, end.kind)
        assertTrue(end.text, end.text.contains("le kick a quitté le centre"))
        assertTrue(File(tmp.root, "captures/oreille_8_retour.jpg").isFile)
    }

    @Test
    fun theReportHasItsSectionAndTheMeasuresHoldTheAnswers() {
        val (carnet, steps) = run(appli(answer = { q -> if (q.id == "oreille_1_jump_up") Reponse(Verdict.NON, "pas de | kick") else Reponse(Verdict.OUI) }))
        val about = listOf("Version" to "1.0")
        val md = Rapport.markdown(carnet.lines(), steps, about, oreille = true)
        assertTrue(md, md.contains("## Questions à l'oreille"))
        assertTrue(md, md.contains("| Question 1 : ça tape ? (") && md.contains("❌ non | pas de / kick | captures/oreille_1_jump_up.jpg |"))
        assertTrue(md, md.contains("✅ oui"))
        assertTrue(md, md.contains("**Verdict : 7 oui, 1 non**"))
        val json = Rapport.json(carnet.lines(), about)
        assertTrue(json, json.contains("\"questions\"") && json.contains("\"reponse\":\"non\"") && json.contains("oreille_1_jump_up"))
        // No answers, no section: the other runs' reports do not change.
        assertFalse(Rapport.markdown(emptyList(), steps, about).contains("Questions à l'oreille"))
        assertFalse(Rapport.json(emptyList(), about).contains("\"questions\""))
    }

    @Test
    fun theBannerWaitsForTheAnswerAndClearsItself() {
        var got: Reponse? = null
        val t = thread { got = Banniere.poser(Question("q", "Ça tape ?"), 5_000) }
        val end = System.currentTimeMillis() + 3_000
        while (Banniere.etat.value == null && System.currentTimeMillis() < end) Thread.sleep(10)
        assertEquals("Ça tape ?", Banniere.etat.value?.question?.texte)
        Banniere.repondre(Verdict.NON, "plat")
        t.join(3_000)
        assertEquals(Reponse(Verdict.NON, "plat"), got)
        assertNull(Banniere.etat.value)
    }

    @Test
    fun theBannerGivesUpAtItsTimeout() {
        val r = Banniere.poser(Question("q", "?"), 100)
        assertEquals(Verdict.SANS_REPONSE, r.verdict)
        assertNull(Banniere.etat.value)
        // An answer given with no question waiting does not stay for the next one.
        Banniere.repondre(Verdict.OUI)
        assertEquals(Verdict.SANS_REPONSE, Banniere.poser(Question("q2", "?"), 100).verdict)
    }
}
