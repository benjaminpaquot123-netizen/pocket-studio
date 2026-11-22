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
 * « Scénarios »: each scenario's oracle on a made-up studio (the gestures change a project in memory
 * and, when the studio saves, on its « disk »), healthy then with one fault at a time; then the
 * report (scenarios.json, resume.md) read from a made-up logbook.
 */
class ScenariosTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** A studio as the robot sees it: the tags of the screen it is on, and what a touch changes. */
    private class Studio : JourneyDriver, Sonde, Appli {
        var parties = 4
        var effets = listOf(listOf("PUMP", "EQ", "COMP", "DRIVE"), listOf("PUMP"), emptyList(), emptyList())
        var disque: ProjetVu.Disque? = null
        var where = "studio"
        var tab = "Structure"
        var sheet = ""
        var picker = false
        var playing = false
        var xruns = 0
        val touched = ArrayList<String>()

        // The faults.
        var saves = true
        var listed = true
        var breakAdds = true
        var chipShown = true
        var liveSound = true
        var liveStops = true
        var reopens = "p1"
        var dropouts = false
        var tempoMoves = true
        var mutes = true
        var undoShown = true
        var undoWorks = true
        var pads = true
        var copies = true
        var renames = true
        var deletes = true
        var stopsBehind = false
        var canLeave = true

        var tempo = 120.0
        var muets = List(4) { false }
        var solos = List(4) { false }
        val others = LinkedHashMap<String, ProjetVu.Disque>()
        var menu: String? = null
        var renaming: String? = null
        var typed = ""
        var behind = false
        private val undo = ArrayList<Pair<Int, List<List<String>>>>()
        private val redo = ArrayList<Pair<Int, List<List<String>>>>()

        private var id = "p1"

        private fun save() {
            if (saves) disque = ProjetVu.Disque("Mon morceau", parties, effets, tempo, muets, solos)
        }

        private fun change(work: () -> Unit) {
            undo += parties to effets
            redo.clear()
            work()
            save()
        }

        private fun back(from: ArrayList<Pair<Int, List<List<String>>>>, to: ArrayList<Pair<Int, List<List<String>>>>) {
            if (!undoWorks || from.isEmpty()) return
            to += parties to effets
            val (p, e) = from.removeAt(from.size - 1)
            parties = p; effets = e
            save()
        }

        init { save() }

        private fun screen(): Set<String> = buildSet {
            when (where) {
                "projects" -> {
                    add("projects"); add("projects_back"); if (listed) add("project_p1")
                    (listOf(id) + others.keys).forEach { add("project_menu_$it") }
                    menu?.let { addAll(listOf("texte:Dupliquer", "texte:Renommer", "project_delete_$it")) }
                    if (renaming != null) addAll(listOf("project_name_field", "project_rename_ok"))
                }
                "launch" -> { add("launch"); add("launch_continue") }
                else -> {
                    addAll(listOf("space_bar", "tab_Mix", "tab_Live", "tab_Structure"))
                    if (tab != "Live") addAll(listOf("top_projects", "thumb_play"))
                    if (undoShown) add("top_undo")
                    if (undoShown && redo.isNotEmpty()) add("top_redo")
                    when (tab) {
                        "Mix" -> {
                            add("mixer")
                            repeat(4) { add("strip_more_$it") }
                            if (sheet.startsWith("strip")) {
                                val ch = sheet.removePrefix("strip").toInt()
                                addAll(listOf("strip_sheet", "strip_sheet_close", "fx_add_$ch", "sheet_mute_$ch", "sheet_solo_$ch"))
                                if (chipShown) effets[ch].indices.forEach { add("fx_chip_${ch}_$it") }
                                if (picker) { add("fx_picker_$ch"); listOf("EQ", "COMPRESSOR", "FILTER").forEach { add("fx_pick_${ch}_$it") } }
                            }
                        }
                        "Live" -> { addAll(listOf("live_screen", "live_play", "live_stop_all")); if (pads) add("live_pad_0") }
                        else -> {
                            add("top_tempo")
                            if (sheet == "tempo") addAll(listOf("tempo_sheet", "tempo_plus", "tempo_minus", "tempo_close"))
                            repeat(parties) { add("part_menu_$it") }
                            if (sheet == "part") addAll(listOf("part_sheet", "part_break_4"))
                        }
                    }
                }
            }
        }

        override fun count(tag: String, merged: Boolean) = if (tag in screen()) 1 else 0
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) = Unit
        override fun idle(ms: Long) = Thread.sleep(minOf(ms, 5))
        override fun tags(pattern: Regex) = screen().filter { pattern.matches(it) }

        override fun click(tag: String) {
            touched += tag
            when {
                tag.startsWith("tab_") -> { tab = tag.removePrefix("tab_"); sheet = ""; picker = false }
                tag == "top_projects" -> where = "projects"
                tag == "projects_back" -> where = "launch"
                tag == "launch_continue" -> { where = "studio"; id = reopens }
                tag == "thumb_play" -> playing = !playing
                tag == "live_play" -> if (!playing) playing = true else if (liveStops) playing = false
                tag.startsWith("part_menu_") -> sheet = "part"
                tag == "part_break_4" -> { sheet = ""; if (breakAdds) change { parties++ } }
                tag == "top_tempo" -> sheet = "tempo"
                tag == "tempo_close" -> sheet = ""
                tag == "tempo_plus" -> if (tempoMoves) { tempo += 1; save() }
                tag == "tempo_minus" -> if (tempoMoves) { tempo -= 1; save() }
                tag.startsWith("sheet_mute_") -> if (mutes) { val ch = tag.removePrefix("sheet_mute_").toInt(); muets = muets.mapIndexed { i, m -> if (i == ch) !m else m }; save() }
                tag.startsWith("sheet_solo_") -> if (mutes) { val ch = tag.removePrefix("sheet_solo_").toInt(); solos = solos.mapIndexed { i, m -> if (i == ch) !m else m }; save() }
                tag == "top_undo" -> back(undo, redo)
                tag == "top_redo" -> back(redo, undo)
                tag == "live_pad_0" -> playing = true
                tag == "live_stop_all" -> playing = false
                tag.startsWith("project_menu_") -> menu = tag.removePrefix("project_menu_")
                tag == "texte:Dupliquer" -> { if (copies) others["c${others.size + 1}"] = disque!!.copy(nom = "Mon morceau (copie)"); menu = null }
                tag == "texte:Renommer" -> { renaming = menu; menu = null }
                tag == "project_rename_ok" -> { val r = renaming!!; if (renames) others[r] = others.getValue(r).copy(nom = typed); renaming = null }
                tag.startsWith("project_delete_") -> { if (deletes) others.remove(tag.removePrefix("project_delete_")); menu = null }
                tag.startsWith("strip_more_") -> sheet = "strip" + tag.removePrefix("strip_more_")
                tag == "strip_sheet_close" -> { sheet = ""; picker = false }
                tag.startsWith("fx_add_") -> picker = !picker
                tag.startsWith("fx_pick_") -> {
                    val ch = tag.split('_')[2].toInt()
                    val type = tag.split('_').drop(3).joinToString("_")
                    change { effets = effets.mapIndexed { i, e -> if (i == ch) e + type else e } }
                }
            }
        }

        override fun lire(): Sonde.Lecture {
            if (playing && dropouts) xruns++
            if (behind && stopsBehind) playing = false
            val sound = playing && (tab != "Live" || liveSound)
            return Sonde.Lecture(xruns, 0.2f, if (sound) 0.5f else 0f, false, true, playing, cpuWorst = 0.3f, gapWorst = 1f)
        }

        override fun ouvrirStudio() = Unit
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        var shots = false
        override fun capture(file: File): Boolean = shots && runCatching { file.writeText("jpg") }.isSuccess
        override fun demander(question: String, maxMs: Long): Boolean? = null
        override fun projet() = ProjetVu(id, "Mon morceau", parties, effets, disque, tempo, muets, solos)
        override fun projets(): Map<String, ProjetVu.Disque> = buildMap { disque?.let { put(id, it) }; putAll(others) }
        override fun arrierePlan(pendant: () -> Unit): Boolean {
            if (!canLeave) return false
            behind = true
            try { pendant() } finally { behind = false }
            return true
        }
        override fun relancerStudio(): Boolean {
            if (!canLeave) return false
            playing = false; where = "launch"; tab = "Structure"; sheet = ""
            return true
        }
        override fun studioDevant(): Boolean = !behind
    }

    private fun carnet() = Carnet(tmp.newFile())

    private fun scenarios(s: Studio, c: Carnet = carnet()) = Scenarios(
        Journey(s), s, s, c, tmp.newFolder(), ecran = { emptyList() },
        ecouteMs = 300, decrochagesMs = 300, sauvegardeMs = 300, liveMs = 300, liveEcouteMs = 60, effetEcouteMs = 60,
        styleEcouteMs = 60, longueMs = 300, fondMs = 300, saisir = { tag, text -> tag in s.tags(Regex(".*")) && run { s.typed = text; true } },
    )

    private fun fails(with: String, work: () -> Unit) {
        val t = runCatching(work).exceptionOrNull()
        assertTrue("no failure, expected « $with »", t != null)
        assertTrue(t!!.message, t.message.orEmpty().contains(with))
    }

    // --- The list ----------------------------------------------------------------------------------

    @Test
    fun theRunHasEveryLevelAndTheNamesToutTesterWritesItsLightsUnder() {
        val steps = scenarios(Studio()).etapes()
        assertEquals(Scenarios.LISTE.map { it.id }, steps.map { it.name })
        assertEquals(steps.size, steps.map { it.name }.toSet().size)
        for (level in listOf(Scenarios.NIVEAU_DEBUTANT, Scenarios.NIVEAU_MOYEN, Scenarios.NIVEAU_PRO, Scenarios.NIVEAU_MESURE)) {
            assertTrue(level, Scenarios.LISTE.count { it.niveau == level } >= 4)
        }
        assertTrue(Scenarios.LISTE.size >= 30)
        // Each one has its work and an oracle that names the precise result it reads.
        assertEquals(emptyList<String>(), scenarios(Studio()).sansTravail())
        for (s in Scenarios.LISTE) assertTrue(s.id, s.oracle.isNotBlank() && s.estimeS > 0 && s.estimeS * 1000L <= s.maxMs)
        assertTrue(steps.map { it.name }.containsAll(listOf("export_m4a", "export_wav", "export_flac", "latence", "images", "style_dnb", "guide_house")))
        // The run stays under 20 minutes on a phone (its estimate).
        assertTrue(Scenarios.LISTE.sumOf { it.estimeS }.toString(), Scenarios.LISTE.sumOf { it.estimeS } <= Scenarios.DUREE_MAX_S)
    }

    // --- The project kept ----------------------------------------------------------------------------

    @Test
    fun theProjectIsReadBackFromTheDiskListedAndReopened() {
        val s = Studio()
        val text = scenarios(s).projet()
        assertTrue(text, text.contains("relu sur le disque (4 parties)"))
        assertEquals(listOf("top_projects", "projects_back", "launch_continue"), s.touched)
        assertEquals("studio", s.where)
    }

    @Test
    fun aProjectThatIsNotOnTheDiskOrNotListedOrNotReopenedFails() {
        fails("aucun fichier de projet") { scenarios(Studio().apply { saves = false; disque = null }).projet() }
        fails("le disque n'a pas le morceau ouvert") { scenarios(Studio().apply { disque = ProjetVu.Disque("Mon morceau", 2, effets) ; saves = false }).projet() }
        val unlisted = Studio().apply { listed = false }
        fails("n'est pas dans Mes projets") { scenarios(unlisted).projet() }
        // Even failed, the robot is back in the studio for the next scenario.
        assertEquals("studio", unlisted.where)
        fails("ne rouvre pas le même morceau") { scenarios(Studio().apply { reopens = "p2" }).projet() }
    }

    // --- The structure -------------------------------------------------------------------------------

    @Test
    fun aBreakIsOnePartMoreInTheSongAndOnTheDisk() {
        val s = Studio()
        val text = scenarios(s).structure()
        assertTrue(text, text.contains("4 parties, puis 5"))
        assertEquals(listOf("tab_Structure", "part_menu_1", "part_break_4"), s.touched)
        fails("le break ne s'ajoute pas") { scenarios(Studio().apply { breakAdds = false }).structure() }
        fails("le break n'est pas enregistré : 4 parties sur le disque") { scenarios(Studio().apply { saves = false }).structure() }
    }

    // --- The effects ---------------------------------------------------------------------------------

    @Test
    fun anEffectGoesOnTheFirstStripWithRoomAndIsReadBackFromTheDisk() {
        val s = Studio()
        val c = carnet()
        val text = scenarios(s, c).effets()
        // Strip 1 is full (4 effects): the second one takes it.
        assertTrue(text, text.contains("égaliseur ajouté à la piste 2"))
        assertEquals(listOf("PUMP", "EQ"), s.effets[1])
        assertEquals(listOf("PUMP", "EQ"), s.disque!!.effets[1])
        assertTrue(s.touched.toString(), s.touched.containsAll(listOf("strip_more_1", "fx_add_1", "fx_pick_1_EQ", "strip_sheet_close")))
        // The studio is left as a user leaves it: the sheet closed, the song stopped.
        assertEquals("", s.sheet)
        assertFalse(s.playing)
        assertTrue(c.lines().any { it.kind == Carnet.FEU && it.step == Scenarios.EFFETS && it.text.startsWith("vert") })
    }

    @Test
    fun anEffectNotShownOrNotSavedFails() {
        fails("ne le montre pas") { scenarios(Studio().apply { chipShown = false }).effets() }
        val unsaved = Studio().apply { saves = false }
        fails("n'est pas enregistré sur le disque (piste 2)") { scenarios(unsaved).effets() }
        assertEquals("", unsaved.sheet)
    }

    // --- The Live ------------------------------------------------------------------------------------

    @Test
    fun theLivePlaysThenStops() {
        val s = Studio()
        val text = scenarios(s).live()
        assertTrue(text, text.startsWith("le Live joue") && text.endsWith("puis s'arrête"))
        assertEquals(listOf("tab_Live", "live_play", "live_play", "tab_Structure"), s.touched)
        assertFalse(s.playing)
    }

    @Test
    fun aSilentLiveOrOneThatNeverStopsFails() {
        val silent = Studio().apply { liveSound = false }
        fails("aucun son ne sort") { scenarios(silent).live() }
        // Stopped and out of the Live all the same.
        assertFalse(silent.playing)
        assertEquals("Structure", silent.tab)
        fails("le Live ne s'arrête pas") { scenarios(Studio().apply { liveStops = false }).live() }
    }

    // --- The dropouts --------------------------------------------------------------------------------

    @Test
    fun dropoutsTurnTheScenarioRedEvenWhenTheStepEnds() {
        val clean = carnet()
        scenarios(Studio(), clean).decrochages()
        assertTrue(clean.lines().any { it.kind == Carnet.FEU && it.step == Scenarios.DECROCHAGES && it.text.startsWith("vert") })
        val c = carnet()
        val s = Studio().apply { dropouts = true }
        Passage(c, scenarios(s, c).etapes().filter { it.name == Scenarios.DECROCHAGES }, crashText = { null }).run()
        val r = Scenarios.resultats(c.lines()).first { it.scenario.id == Scenarios.DECROCHAGES }
        assertEquals(Scenarios.ECHOUE, r.resultat)
        assertTrue(r.detail, r.detail.contains("décrochage"))
        assertTrue(r.mesures.toString(), r.mesures.any { it.first == "decrochages" && it.second.toInt() > 0 })
    }

    // --- A crash of the app --------------------------------------------------------------------------

    /**
     * The app dies in a scenario (its process killed: an exception on the main thread, the engine, an ANR):
     * the robot's thread dies with it. Relaunched, the run notes that scenario as failed by a crash with its
     * cause, the picture and the tags of the screen, and goes on with the next ones to the end; the report
     * is written all the same.
     */
    @Test
    fun aCrashInAScenarioIsItsFailureWithItsCluesAndTheRunGoesOn() {
        val c = carnet()
        // The first life of the app: « écrans » ended, « structure » began, then the process died.
        c.add(Carnet.DEBUT, Scenarios.ECRANS)
        c.add(Carnet.REUSSI, Scenarios.ECRANS, "5 écrans")
        c.add(Carnet.DEBUT, Scenarios.STRUCTURE)
        // The second life: the run picked up.
        val s = Studio().apply { shots = true }
        val captures = tmp.newFolder()
        val sc = Scenarios(Journey(s), s, s, c, captures, ecouteMs = 300, decrochagesMs = 300, sauvegardeMs = 300, liveMs = 300, liveEcouteMs = 60, effetEcouteMs = 60)
        val wanted = setOf(Scenarios.ECRANS, Scenarios.STRUCTURE, Scenarios.EFFETS, Scenarios.LIVE)
        val crash = "Version 1.2.3 · thread main\njava.lang.IllegalStateException: boom\n\tat com.pocketstudio.app.Studio.play(Studio.kt:12)"
        Passage(c, sc.etapes().filter { it.name in wanted }, crashText = { crash }, onCrash = sc::apresPlantage).run()
        val lines = c.lines()
        assertTrue(lines.any { it.kind == Carnet.PLANTAGE && it.step == Scenarios.STRUCTURE && it.text.contains("IllegalStateException: boom") })
        // The clues of the crashed scenario: the screen as the robot finds it, and every tag on it.
        val indice = lines.lastOrNull { it.step == Scenarios.STRUCTURE && it.kind == Carnet.NOTE && it.text.startsWith("indice;") }
        assertTrue(lines.toString(), indice != null && indice.text.contains("capture=captures/echec-structure.jpg") && indice.text.contains("tags=captures/echec-structure-tags.txt"))
        assertTrue(File(captures, "echec-structure-tags.txt").readText().contains("space_bar"))
        // The run went on and ended.
        for (next in listOf(Scenarios.EFFETS, Scenarios.LIVE)) assertTrue(next, lines.any { it.step == next && it.kind == Carnet.REUSSI })
        assertTrue(lines.any { it.kind == Carnet.FINI })
        // The report says it: failed by a crash, its cause and its clues.
        val r = Scenarios.resultats(lines).associateBy { it.scenario.id }.getValue(Scenarios.STRUCTURE)
        assertEquals(Scenarios.ECHOUE, r.resultat)
        assertTrue(r.detail, r.detail.startsWith("plantage de l'appli") && r.detail.contains("boom"))
        assertEquals("captures/echec-structure.jpg", r.indices.toMap()["capture"])
        assertTrue(Scenarios.json(lines, about).contains("plantage de l'appli"))
        assertTrue(Scenarios.resume(lines, about).contains("plantage de l'appli"))
    }

    @Test
    fun aCrashWhoseCauseCannotBeReadStillSaysItCrashed() {
        val c = carnet()
        c.add(Carnet.DEBUT, Scenarios.LIVE)
        val s = Studio()
        val sc = scenarios(s, c)
        Passage(c, sc.etapes().filter { it.name == Scenarios.LIVE }, crashText = { null }, onCrash = sc::apresPlantage).run()
        val r = Scenarios.resultats(c.lines()).first { it.scenario.id == Scenarios.LIVE }
        assertEquals(Scenarios.ECHOUE, r.resultat)
        assertTrue(r.detail, r.detail.startsWith("plantage de l'appli") && r.detail.contains("cause non lue"))
        // Without a picture, the tags all the same.
        assertEquals("", r.indices.toMap()["capture"])
        assertTrue(r.indices.toString(), r.indices.toMap()["tags"].orEmpty().endsWith("echec-live-tags.txt"))
    }

    // --- The scenarios added on 2026-11-22 -----------------------------------------------------------

    @Test
    fun theTempoGoesUpIsSavedAndComesBack() {
        val s = Studio()
        val text = scenarios(s).tempo()
        assertTrue(text, text.contains("tempo 120.00 puis 121.00 bpm, enregistré"))
        assertEquals(120.0, s.tempo, 0.0)
        assertEquals("", s.sheet)
        fails("le tempo ne change pas") { scenarios(Studio().apply { tempoMoves = false }).tempo() }
        fails("le tempo n'est pas enregistré") { scenarios(Studio().apply { saves = false }).tempo() }
        val playing = Studio()
        assertTrue(scenarios(playing).tempoLecture().contains("125.00 bpm en jouant"))
        assertFalse(playing.playing)
    }

    @Test
    fun muteAndSoloAreSetSavedThenTakenOff() {
        val s = Studio()
        assertTrue(scenarios(s).muet().contains("« Muet » sur la piste 1 : mis, enregistré, puis enlevé"))
        assertTrue(scenarios(s).solo().contains("« Solo » sur la piste 2"))
        assertEquals(List(4) { false }, s.muets)
        assertEquals(List(4) { false }, s.solos)
        assertTrue(s.touched.containsAll(listOf("strip_more_0", "sheet_mute_0", "strip_more_1", "sheet_solo_1")))
        fails("la piste 1 ne change pas") { scenarios(Studio().apply { mutes = false }).muet() }
        fails("n'est pas enregistré sur le disque") { scenarios(Studio().apply { saves = false }).solo() }
    }

    @Test
    fun undoTakesTheBreakOffAndRedoPutsItBack() {
        val s = Studio()
        assertTrue(scenarios(s).annulerRetablir().contains("annulé (4), rétabli (5), enregistré"))
        assertEquals(5, s.disque!!.parties)
        fails("« Annuler » ne retire pas le break") { scenarios(Studio().apply { undoWorks = false }).annulerRetablir() }
        // No « Annuler » in the bar (a setting): not measured, never a false failure.
        val c = carnet()
        assertTrue(scenarios(Studio().apply { undoShown = false }, c).annulerRetablir().startsWith("non mesuré"))
        assertTrue(c.lines().any { it.kind == Carnet.NOTE && it.text.startsWith(ToutTester.NON_MESURE) })
        val fx = Studio()
        assertTrue(scenarios(fx).annulerEffet().contains("COMPRESSOR ajouté à la piste 2 puis annulé"))
        assertEquals(listOf("PUMP"), fx.disque!!.effets[1])
    }

    @Test
    fun threeEffectsGoOnOneStripAndAreSaved() {
        val s = Studio()
        val text = scenarios(s).effetsPlusieurs()
        assertTrue(text, text.contains("EQ, COMPRESSOR, FILTER sur la piste 2"))
        assertEquals(listOf("PUMP", "EQ", "COMPRESSOR", "FILTER"), s.disque!!.effets[1])
        assertFalse(s.playing)
        val full = Studio().apply { effets = List(4) { listOf("A", "B") } }
        assertTrue(scenarios(full).effetsPlusieurs().startsWith("non mesuré"))
    }

    @Test
    fun aPadOfTheLiveSoundsOrIsNotMeasured() {
        val s = Studio()
        assertTrue(scenarios(s).pads().contains("pad live_pad_0 : le son sort"))
        assertFalse(s.playing)
        assertEquals("Structure", s.tab)
        assertTrue(scenarios(Studio().apply { pads = false }).pads().startsWith("non mesuré"))
        fails("aucun son ne sort") { scenarios(Studio().apply { liveSound = false }).pads() }
    }

    @Test
    fun theRobotDuplicatesRenamesAndDeletesOnlyItsOwnCopy() {
        val s = Studio()
        val sc = scenarios(s)
        assertTrue(sc.dupliquer().contains("copie « Mon morceau (copie) »"))
        assertEquals(setOf("p1", "c1"), s.projets().keys)
        assertTrue(sc.renommer().contains("relue sur le disque"))
        assertEquals(Scenarios.NOM_ROBOT, s.others.getValue("c1").nom)
        assertTrue(sc.supprimer().contains("copie du robot supprimée"))
        assertEquals(setOf("p1"), s.projets().keys)
        assertEquals("studio", s.where)
        // The open song is never touched.
        assertFalse(s.touched.contains("project_delete_p1"))
        // Without its copy, the robot renames and deletes nothing: the user's projects are not its.
        val user = Studio().apply { others["u1"] = ProjetVu.Disque("Ma chanson", 3, emptyList()) }
        val fresh = scenarios(user)
        assertTrue(fresh.renommer().startsWith("non mesuré"))
        assertTrue(fresh.supprimer().startsWith("non mesuré"))
        assertTrue(user.others.containsKey("u1"))
        assertTrue(user.touched.none { it.startsWith("project_delete_") || it == "texte:Renommer" })
        fails("aucune copie sur le disque") { scenarios(Studio().apply { copies = false }).dupliquer() }
        val kept = Studio()
        val k = scenarios(kept)
        k.dupliquer()
        kept.deletes = false
        fails("encore sur le disque") { k.supprimer() }
        assertEquals("studio", kept.where)
    }

    @Test
    fun theStudioClosedAndOpenedAgainShowsTheSameSong() {
        val s = Studio()
        assertTrue(scenarios(s).reprise().contains("« Mon morceau », 4 parties"))
        assertEquals("studio", s.where)
        fails("rouvert sur") { scenarios(Studio().apply { reopens = "p2" }).reprise() }
        assertTrue(scenarios(Studio().apply { canLeave = false }).reprise().startsWith("non mesuré"))
    }

    @Test
    fun theMusicGoesOnBehindAnotherScreen() {
        val s = Studio()
        assertTrue(scenarios(s).arrierePlan().contains("studio revenu"))
        assertFalse(s.playing)
        fails("la musique s'arrête quand l'appli passe au fond") { scenarios(Studio().apply { stopsBehind = true }).arrierePlan() }
        assertTrue(scenarios(Studio().apply { canLeave = false }).arrierePlan().startsWith("non mesuré"))
    }

    @Test
    fun aNotMeasuredScenarioIsNeitherPassedNorFailedInTheReport() {
        val c = carnet()
        val sc = scenarios(Studio().apply { canLeave = false }, c)
        Passage(c, sc.etapes().filter { it.name == Scenarios.ARRIERE_PLAN }, crashText = { null }).run()
        val r = Scenarios.resultats(c.lines()).first { it.scenario.id == Scenarios.ARRIERE_PLAN }
        assertEquals(Scenarios.NON_MESURE, r.resultat)
    }

    // --- The report ----------------------------------------------------------------------------------

    /** A logbook: [id] to its lines after DEBUT (kind to text), one second each. */
    private fun logbook(vararg steps: Pair<String, List<Pair<String, String>>>): List<Carnet.Ligne> {
        var at = 0L
        val c = Carnet(tmp.newFile()) { at }
        for ((id, lines) in steps) {
            c.add(Carnet.DEBUT, id)
            for ((kind, text) in lines) { at += 1000; c.add(kind, id, text) }
        }
        return c.lines()
    }

    private val about = listOf("version" to "1.2.3", "téléphone" to "Pixel")

    private fun made() = logbook(
        Scenarios.DEBUTANT to listOf(Carnet.FEU to "vert;", Carnet.REUSSI to "lecture par thumb_play"),
        Scenarios.ECRANS to listOf(Carnet.REUSSI to "5 écrans"),
        Scenarios.PROJET to listOf(Carnet.MESURE to "bilan;parties=4;sur_disque=true", Carnet.REUSSI to "relu"),
        Scenarios.STRUCTURE to listOf(Carnet.ECHEC to "le break ne s'ajoute pas au morceau : 4 parties (avant : 4)"),
        Scenarios.EFFETS to listOf(Carnet.FEU to "orange;saut à 3 s", Carnet.REUSSI to "égaliseur ajouté"),
        Scenarios.EXPORT to listOf(Carnet.MESURE to "octets=120000;secondes=8.0;duree=4.1;attendue=10.0;crete=-1.2", Carnet.FEU to "vert;", Carnet.REUSSI to "fichier relu"),
        Scenarios.LATENCE to listOf(Carnet.MESURE to "ms=41.0", Carnet.MESURE to "ms=38.5", Carnet.MESURE to "ms=52.0", Carnet.FEU to "vert;", Carnet.REUSSI to "41 ms"),
        Scenarios.DECROCHAGES to listOf(Carnet.MESURE to "bilan;decrochages=2;pire_bloc=0.40", Carnet.FEU to "rouge;2 décrochage(s)", Carnet.REUSSI to "lecture"),
    )

    @Test
    fun eachScenarioIsPassedFailedOrNotPlayed() {
        val r = Scenarios.resultats(made()).associateBy { it.scenario.id }
        assertEquals(Scenarios.PASSE, r.getValue(Scenarios.DEBUTANT).resultat)
        assertEquals(Scenarios.PASSE, r.getValue(Scenarios.ECRANS).resultat)
        assertEquals(Scenarios.ECHOUE, r.getValue(Scenarios.STRUCTURE).resultat)
        assertTrue(r.getValue(Scenarios.STRUCTURE).detail.contains("le break ne s'ajoute pas"))
        // Orange: it works, a number is to be watched; its reason is in the detail.
        assertEquals(Scenarios.PASSE, r.getValue(Scenarios.EFFETS).resultat)
        assertEquals(Feu.ORANGE, r.getValue(Scenarios.EFFETS).feu)
        assertTrue(r.getValue(Scenarios.EFFETS).detail, r.getValue(Scenarios.EFFETS).detail.contains("saut à 3 s"))
        assertEquals(Scenarios.PAS_JOUE, r.getValue(Scenarios.LIVE).resultat)
        // A red number fails a step that ended.
        assertEquals(Scenarios.ECHOUE, r.getValue(Scenarios.DECROCHAGES).resultat)
        assertEquals(listOf("latence_ms" to "41.00", "mesures" to "3"), r.getValue(Scenarios.LATENCE).mesures)
        assertEquals("120000", r.getValue(Scenarios.EXPORT).mesures.first { it.first == "octets" }.second)
        assertEquals(3L, r.getValue(Scenarios.EXPORT).secondes)
        assertEquals("6 passés, 2 échoués, ${Scenarios.LISTE.size - 8} pas joués", Scenarios.verdict(made()))
    }

    @Test
    fun theJsonHasTheVerdictTheLevelsAndEachScenarioWithItsNumbers() {
        val json = Scenarios.json(made(), about)
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(json.count { it == '[' }, json.count { it == ']' })
        for (part in listOf(
            "\"version\":\"1.2.3\"", "\"passage\":\"scenarios\"", "\"verdict\":\"6 passés, 2 échoués, ${Scenarios.LISTE.size - 8} pas joués\"", "\"tout_passe\":false",
            "\"id\":\"structure\"", "\"resultat\":\"échoué\"", "\"niveau\":\"pro\"", "\"feu\":\"orange\"", "\"latence_ms\":41", "\"decrochages\":2", "\"sur_disque\":\"true\"",
        )) assertTrue("$part in $json", json.replace(": ", ":").replace(" ", " ").contains(part) || json.replace("\": ", "\":").contains(part))
        assertEquals(Scenarios.LISTE.size, Regex("\"id\"").findAll(json).count())
    }

    @Test
    fun theSummaryIsInFrenchWithOneLineAScenario() {
        val md = Scenarios.resume(made(), about)
        assertTrue(md, md.contains("**Verdict : 6 passés, 2 échoués, ${Scenarios.LISTE.size - 8} pas joués**"))
        assertTrue(md, md.contains("- Niveau débutant : 2 sur 8"))
        assertTrue(md, md.contains("- Niveau pro : 1 sur 8"))
        assertTrue(md, md.contains("| Moyen : ajouter un break à la structure | moyen | ❌ échoué |"))
        assertTrue(md, md.contains("🟠 passé, à surveiller"))
        assertTrue(md, md.contains("| Pro : jouer puis arrêter le Live | pro | pas joué |"))
        assertTrue(md, md.contains("latence ms 41.00"))
        assertEquals(Scenarios.LISTE.size, md.lines().count { it.startsWith("| ") && !it.startsWith("| Scénario") })
    }

    @Test
    fun aRunWhereEverythingPassesSaysSo() {
        val all = logbook(*Scenarios.LISTE.map { it.id to listOf(Carnet.REUSSI to "ok") }.toTypedArray())
        assertEquals("${Scenarios.LISTE.size} passés, 0 échoué", Scenarios.verdict(all))
        assertTrue(Scenarios.json(all, about).contains("\"tout_passe\":true") || Scenarios.json(all, about).contains("\"tout_passe\": true"))
    }
}
