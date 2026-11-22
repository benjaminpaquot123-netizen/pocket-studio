package com.pocketstudio.bentest

import androidx.compose.ui.geometry.Rect
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * « Scénarios » run after run (the phone's first run, 2026-11-22, Xiaomi, Android 17: 0 passed, 9 failed in
 * 4 min 15 s): each scenario starts from the open song whatever the one before left (BenStudio Test's own
 * screen in front, a full-screen view hiding the bar, a sheet), and a failed one leaves a picture and the
 * tags on screen in the report.
 */
class ScenariosRemiseTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** A studio as the robot sees it, with BenStudio Test's screen, the full-screen timeline and the rail. */
    private class Studio : JourneyDriver, Sonde, Appli {
        /** bentest (BenStudio Test's screen in front), launch, projects, studio. */
        var where = "studio"
        var overlay = ""
        var landscape = false
        var tab = "Structure"
        var sheet = ""
        var parties = 4
        var playing = false
        var opened = 0
        var backs = 0
        val shots = ArrayList<File>()

        // The faults: the break's touch opens the full-screen timeline (and adds nothing).
        var breakOpensTimeline = false

        // The phone's sound and the latency's measures (null: a measure that failed), taken in order.
        var volume: Appli.Volume? = Appli.Volume(10, 15)
        val latences = ArrayList<Double?>()
        var latencyCalls = 0

        // The engine's true worst block and longest gap while it plays.
        var cpuWorst = 0.3f
        var gapWorst = 1f

        /** Waits a step may make before it gives up, as the phone's driver does at the step's end. */
        var idles = 0
        var cap = 300

        private fun screen(): Set<String> = buildSet {
            when (where) {
                "bentest" -> add("bentest_step")
                "projects" -> addAll(listOf("projects", "projects_back", "project_p1"))
                "launch" -> addAll(listOf("launch", "launch_continue"))
                else -> {
                    add("thumb_play")
                    if (overlay == "timeline") {
                        add("timeline_screen")
                        return@buildSet
                    }
                    addAll(listOf(if (landscape) "space_rail" else "space_bar", "tab_Mix", "tab_Live", "tab_Structure"))
                    if (tab != "Live") add("top_projects")
                    when (tab) {
                        "Live" -> addAll(listOf("live_screen", "live_play"))
                        else -> {
                            repeat(parties) { add("part_menu_$it") }
                            if (sheet == "part") addAll(listOf("part_sheet", "part_sheet_close", "part_break_4"))
                        }
                    }
                }
            }
        }

        override fun count(tag: String, merged: Boolean) = if (tag in screen()) 1 else 0
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) = Unit
        override fun tags(pattern: Regex) = screen().filter { pattern.matches(it) }

        override fun idle(ms: Long) {
            if (++idles > cap) throw EtapeTropLongue("l'étape a dépassé son temps")
            Thread.sleep(minOf(ms, 2))
        }

        override fun click(tag: String) {
            check(tag in screen()) { "nothing tagged $tag" }
            when {
                tag.startsWith("tab_") -> { tab = tag.removePrefix("tab_"); sheet = "" }
                tag == "top_projects" -> where = "projects"
                tag == "projects_back" -> where = "launch"
                tag == "launch_continue" -> where = "studio"
                tag == "thumb_play" -> playing = !playing
                tag == "live_play" -> playing = !playing
                tag.startsWith("part_menu_") -> sheet = "part"
                tag == "part_sheet_close" -> sheet = ""
                tag == "part_break_4" -> { sheet = ""; if (breakOpensTimeline) overlay = "timeline" else parties++ }
            }
        }

        override fun lire() = Sonde.Lecture(0, 0.2f, if (playing) 0.5f else 0f, false, true, playing, cpuWorst = cpuWorst, gapWorst = gapWorst)

        override fun volume() = volume
        override fun latence(): Double? {
            latencyCalls++
            return latences.removeFirstOrNull()
        }

        override fun latenceEchec() = "pas de retour, niveau 0.00"

        /** The app's launcher activity: from BenStudio Test's screen, « Que veux-tu faire ? ». */
        override fun ouvrirStudio() {
            opened++
            if (where == "bentest") where = "launch"
        }

        /** Android's « retour »: the full-screen timeline closes. */
        override fun retour() {
            backs++
            overlay = ""
        }

        override fun studioDevant() = where != "bentest"

        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun capture(file: File): Boolean {
            shots += file
            file.parentFile?.mkdirs()
            file.writeBytes(byteArrayOf(1, 2, 3))
            return true
        }

        override fun demander(question: String, maxMs: Long): Boolean? = null
        override fun projet() = ProjetVu("p1", "Mon morceau", parties, listOf(emptyList()), ProjetVu.Disque("Mon morceau", parties, listOf(emptyList())))
    }

    private fun run(s: Studio, vararg ids: String, c: Carnet = Carnet(tmp.newFile()), shots: File = tmp.newFolder()): List<Scenarios.Companion.Resultat> {
        val sc = Scenarios(
            Journey(s), s, s, c, shots, ecran = { s.tags(Regex(".+")) },
            ecouteMs = 100, decrochagesMs = 100, sauvegardeMs = 100, liveMs = 200, liveEcouteMs = 50, effetEcouteMs = 50,
        )
        Passage(c, sc.etapes().filter { it.name in ids }, crashText = { null }).run { s.idles = 0 }
        return Scenarios.resultats(c.lines()).filter { it.scenario.id in ids }
    }

    private fun assertPassed(r: List<Scenarios.Companion.Resultat>) {
        for (x in r) assertEquals("${x.scenario.id} : ${x.detail}", Scenarios.PASSE, x.resultat)
    }

    // --- The run starts from BenStudio Test's own screen ---------------------------------------------

    @Test
    fun aRunStartedFromBenStudioTestsScreenOpensTheStudioFirst() {
        val s = Studio().apply { where = "bentest" }
        assertPassed(run(s, Scenarios.STRUCTURE, Scenarios.LIVE))
        assertEquals(1, s.opened)
    }

    @Test
    fun lyingDownTheRailStandsForTheBar() {
        val s = Studio().apply { where = "bentest"; landscape = true }
        assertPassed(run(s, Scenarios.STRUCTURE, Scenarios.LIVE))
    }

    // --- One red does not make the next ones red ------------------------------------------------------

    @Test
    fun aScenarioThatLeftAFullScreenViewDoesNotBringDownTheNextOne() {
        val s = Studio().apply { breakOpensTimeline = true }
        val r = run(s, Scenarios.STRUCTURE, Scenarios.LIVE)
        assertEquals(Scenarios.ECHOUE, r.first { it.scenario.id == Scenarios.STRUCTURE }.resultat)
        assertPassed(r.filter { it.scenario.id == Scenarios.LIVE })
        assertTrue("« retour » closes the full-screen view", s.backs >= 1)
    }

    @Test
    fun aScenarioThatLeftBenStudioTestsScreenInFrontDoesNotBringDownTheNextOne() {
        val s = Studio()
        val c = Carnet(tmp.newFile())
        // The phone went back to BenStudio Test's screen between two scenarios.
        val sc = Scenarios(
            Journey(s), s, s, c, tmp.newFolder(), ecran = { s.tags(Regex(".+")) },
            ecouteMs = 100, decrochagesMs = 100, sauvegardeMs = 100, liveMs = 200, liveEcouteMs = 50, effetEcouteMs = 50,
        )
        val steps = sc.etapes().filter { it.name in setOf(Scenarios.STRUCTURE, Scenarios.LIVE) }
        Passage(c, steps, crashText = { null }).run { step -> s.idles = 0; if (step.name == Scenarios.LIVE) s.where = "bentest" }
        assertPassed(Scenarios.resultats(c.lines()).filter { it.scenario.id in setOf(Scenarios.STRUCTURE, Scenarios.LIVE) })
        assertEquals(1, s.opened)
    }

    // --- A failed scenario leaves a picture and the tags on screen ------------------------------------

    @Test
    fun aFailedScenarioLeavesAPictureAndTheTagsOnScreenInTheReport() {
        val s = Studio().apply { breakOpensTimeline = true }
        val c = Carnet(tmp.newFile())
        val shots = tmp.newFolder()
        run(s, Scenarios.STRUCTURE, c = c, shots = shots)
        val picture = File(shots, "echec-structure.jpg")
        val tags = File(shots, "echec-structure-tags.txt")
        assertTrue("the picture is taken", picture.isFile && picture in s.shots)
        assertTrue("the tags are written", tags.isFile)
        val text = tags.readText()
        assertTrue(text, "timeline_screen" in text && "thumb_play" in text && "studio devant : oui" in text)
        assertTrue(c.lines().any { it.kind == Carnet.NOTE && it.step == Scenarios.STRUCTURE && "captures/echec-structure.jpg" in it.text && "captures/echec-structure-tags.txt" in it.text })
        val resume = Scenarios.resume(c.lines(), emptyList())
        assertTrue(resume, "## Indices" in resume && "captures/echec-structure.jpg" in resume && "captures/echec-structure-tags.txt" in resume)
    }

    @Test
    fun aPassedRunLeavesNoClue() {
        val s = Studio()
        val c = Carnet(tmp.newFile())
        val shots = tmp.newFolder()
        assertPassed(run(s, Scenarios.STRUCTURE, c = c, shots = shots))
        assertTrue(shots.listFiles().orEmpty().none { it.name.startsWith("echec-") })
        assertTrue("## Indices" !in Scenarios.resume(c.lines(), emptyList()))
    }

    // --- The phone's sound cut: the latency is not measured -------------------------------------------

    private fun latency(s: Studio, c: Carnet = Carnet(tmp.newFile())) = run(s, Scenarios.LATENCE, c = c).single()

    @Test
    fun withTheMediaVolumeAtZeroTheLatencyIsNotMeasured() {
        // The Xiaomi's run, 2026-11-22: sound cut, « 501.71 ms (mesures : 501.71, 2 ratée(s)) » in red.
        val s = Studio().apply { volume = Appli.Volume(0, 15); latences += listOf(501.71, null, null) }
        val c = Carnet(tmp.newFile())
        val r = latency(s, c)
        assertEquals(r.detail, Scenarios.NON_MESURE, r.resultat)
        assertTrue(r.detail, "son coupé : remonte le volume pour mesurer la latence" in r.detail)
        assertEquals("no measure through a muted speaker", 0, s.latencyCalls)
        assertTrue(r.mesures.toString(), r.mesures.none { it.first == "latence_ms" })
        val verdict = Scenarios.verdict(c.lines())
        assertTrue(verdict, "1 non mesuré" in verdict && "0 échoué" in verdict && "0 passé" in verdict)
        val resume = Scenarios.resume(c.lines(), emptyList())
        assertTrue(resume, "Niveau mesure : 0 sur 2 (1 non mesuré)" in resume)
        assertTrue(resume, "non mesuré (son coupé)" in resume && "❌" !in resume)
        assertTrue(resume, resume.lines().take(4).any { it == "Son : coupé" })
        val json = Scenarios.json(c.lines(), emptyList())
        assertTrue(json, "non mesuré" in json && "non_mesures" in json)
    }

    @Test
    fun aVolumeTooLowOrTheSilentModeOrAMutedStreamIsSoundCutToo() {
        for (v in listOf(Appli.Volume(1, 15), Appli.Volume(10, 15, silencieux = true), Appli.Volume(10, 15, muet = true))) {
            val s = Studio().apply { volume = v; latences += listOf(40.0, 40.0, 40.0) }
            val r = latency(s)
            assertEquals("$v : ${r.detail}", Scenarios.NON_MESURE, r.resultat)
            assertEquals(0, s.latencyCalls)
        }
    }

    @Test
    fun aLatencyWithMoreThanHalfItsMeasuresFailedIsNotAMeasure() {
        val s = Studio().apply { latences += listOf(501.71, null, null) }
        val c = Carnet(tmp.newFile())
        val r = latency(s, c)
        assertEquals(r.detail, Scenarios.NON_MESURE, r.resultat)
        assertTrue(r.detail, "2" in r.detail && "ratée" in r.detail)
        assertTrue(r.mesures.toString(), r.mesures.none { it.first == "latence_ms" })
        assertTrue(Scenarios.verdict(c.lines()), "1 non mesuré" in Scenarios.verdict(c.lines()))
    }

    @Test
    fun aLatencyWithTheSoundUpAndItsMeasuresTakenIsRead() {
        val s = Studio().apply { latences += listOf(40.0, null, 42.0) }
        val r = latency(s)
        assertEquals(r.detail, Scenarios.PASSE, r.resultat)
        assertEquals(3, s.latencyCalls)
        assertTrue(r.mesures.toString(), r.mesures.any { it.first == "latence_ms" })
    }

    @Test
    fun withTheSoundCutEverythingButTheSpeakersMeasuresRunsAsUsual() {
        // Benjamin, 2026-11-22: the test is often run with the sound cut; it is a normal case, not an error.
        val s = Studio().apply { volume = Appli.Volume(0, 15); latences += listOf(40.0, 40.0, 40.0) }
        val c = Carnet(tmp.newFile())
        val r = run(s, Scenarios.STRUCTURE, Scenarios.LIVE, Scenarios.LATENCE, Scenarios.DECROCHAGES, c = c)
        assertPassed(r.filter { it.scenario.id != Scenarios.LATENCE })
        assertEquals(Scenarios.NON_MESURE, r.first { it.scenario.id == Scenarios.LATENCE }.resultat)
        assertEquals("Son : coupé", Scenarios.resume(c.lines(), emptyList()).lines()[2])
    }

    @Test
    fun withTheSoundOnTheResumeSaysSo() {
        val c = Carnet(tmp.newFile())
        assertPassed(run(Studio(), Scenarios.STRUCTURE, c = c))
        assertEquals("Son : allumé", Scenarios.resume(c.lines(), emptyList()).lines()[2])
    }
}
