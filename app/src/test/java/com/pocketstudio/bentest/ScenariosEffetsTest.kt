package com.pocketstudio.bentest

import androidx.compose.ui.geometry.Rect
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import com.pocketstudio.model.EffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The effects' catalogue (tour 9, « Effets »): the list itself, then each kind of scenario on a made-up studio with
 * slots (an effect taken off leaves its slot empty, as in the app), healthy then with one fault at a time.
 */
class ScenariosEffetsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    /** Four strips of four slots; a chip and its editor per slot in use; the sliders of the open editor. */
    private class Studio : JourneyDriver, Sonde, Appli {
        var slots: List<List<String?>> = listOf(listOf("EQ", null, null, null), List(4) { null }, List(4) { null }, List(4) { null })
        var disque: ProjetVu.Disque? = null
        var tab = "Structure"
        var sheet = -1
        var picker = false
        var editor: Pair<Int, Int>? = null
        var playing = false
        var xruns = 0
        val values = HashMap<String, Float>()

        // The faults.
        var saves = true
        var removeWorks = true
        var nan = false
        var dropouts = false
        var sliders = true
        /** The second knob of every effect does not take a value. */
        var brokenKnob = false
        var undoShown = true
        var undoWorks = true
        /** Taking an effect off stops the song. */
        var stopsOnRemove = false
        /** Silence once a knob sits at 1. */
        var silentOnMax = false
        var editorOpens = true

        private val undo = ArrayList<List<List<String?>>>()
        private val redo = ArrayList<List<List<String?>>>()

        fun effets() = slots.map { it.filterNotNull() }
        private fun save() { if (saves) disque = ProjetVu.Disque("Mon morceau", 4, effets()) }
        private fun change(work: () -> Unit) { undo += slots; redo.clear(); work(); save() }
        private fun back(from: ArrayList<List<List<String?>>>, to: ArrayList<List<List<String?>>>) {
            if (!undoWorks || from.isEmpty()) return
            to += slots
            slots = from.removeAt(from.size - 1)
            save()
        }
        private fun set(ch: Int, slot: Int, v: String?) { slots = slots.mapIndexed { i, s -> if (i == ch) s.mapIndexed { k, x -> if (k == slot) v else x } else s } }

        init { save() }

        private fun screen(): Set<String> = buildSet {
            addAll(listOf("space_bar", "tab_Mix", "tab_Live", "tab_Structure", "top_projects", "thumb_play"))
            if (undoShown) add("top_undo")
            if (undoShown && redo.isNotEmpty()) add("top_redo")
            if (tab == "Mix") {
                add("mixer"); repeat(4) { add("strip_more_$it") }
                if (sheet >= 0) {
                    val ch = sheet
                    addAll(listOf("strip_sheet", "strip_sheet_close", "fx_add_$ch"))
                    slots[ch].forEachIndexed { k, s -> if (s != null) addAll(listOf("fx_chip_${ch}_$k", "fx_remove_${ch}_$k", "fx_bypass_${ch}_$k")) }
                    editor?.let { (c, k) ->
                        val type = slots[c][k]
                        if (c == ch && type != null) {
                            add("fx_editor_${c}_$k")
                            EffectType.valueOf(type).params.indices.forEach { add("fxp_${c}_${k}_$it") }
                        }
                    }
                    if (picker) { add("fx_picker_$ch"); EffectType.entries.forEach { add("fx_pick_${ch}_${it.name}") } }
                }
            }
        }

        override fun count(tag: String, merged: Boolean) = if (tag in screen()) 1 else 0
        override fun bounds(tag: String) = Rect(0f, 0f, 10f, 10f)
        override fun scrollTo(tag: String) = Unit
        override fun idle(ms: Long) = Thread.sleep(minOf(ms, 5))
        override fun tags(pattern: Regex) = screen().filter { pattern.matches(it) }

        override fun click(tag: String) {
            when {
                tag.startsWith("tab_") -> { tab = tag.removePrefix("tab_"); sheet = -1; picker = false; editor = null }
                tag == "thumb_play" -> playing = !playing
                tag == "top_undo" -> back(undo, redo)
                tag == "top_redo" -> back(redo, undo)
                tag.startsWith("strip_more_") -> sheet = tag.removePrefix("strip_more_").toInt()
                tag == "strip_sheet_close" -> { sheet = -1; picker = false; editor = null }
                tag.startsWith("fx_add_") -> picker = !picker
                tag.startsWith("fx_pick_") -> {
                    val ch = tag.split('_')[2].toInt()
                    val type = tag.split('_').drop(3).joinToString("_")
                    val free = slots[ch].indexOfFirst { it == null }
                    if (free >= 0) change { set(ch, free, type) }
                }
                tag.startsWith("fx_chip_") -> {
                    val (c, k) = tag.removePrefix("fx_chip_").split('_').map { it.toInt() }
                    if (editorOpens) editor = if (editor == c to k) null else c to k
                }
                tag.startsWith("fx_remove_") -> {
                    val (c, k) = tag.removePrefix("fx_remove_").split('_').map { it.toInt() }
                    editor = null
                    if (removeWorks) change { set(c, k, null); values.keys.removeAll { it.startsWith("fxp_${c}_${k}_") } }
                    if (stopsOnRemove) playing = false
                }
            }
        }

        fun regler(tag: String, value: Float): Boolean {
            if (!sliders || tag !in screen()) return false
            if (brokenKnob && tag.endsWith("_1")) return false
            values[tag] = value
            save()
            return true
        }

        override fun lire(): Sonde.Lecture {
            if (playing && dropouts) xruns++
            val sound = playing && !(silentOnMax && values.values.any { it >= 1f })
            return Sonde.Lecture(xruns, 0.2f, if (sound) 0.5f else 0f, playing && nan, true, playing, cpuWorst = 0.3f, gapWorst = 1f)
        }

        override fun ouvrirStudio() = Unit
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun capture(file: File): Boolean = false
        override fun demander(question: String, maxMs: Long): Boolean? = null
        override fun projet() = ProjetVu("p1", "Mon morceau", 4, effets(), disque)
        override fun studioDevant(): Boolean = true
    }

    private fun carnet() = Carnet(tmp.newFile())

    private fun scenarios(s: Studio, c: Carnet) = Scenarios(
        Journey(s), s, s, c, tmp.newFolder(), ecran = { emptyList() },
        ecouteMs = 300, decrochagesMs = 300, sauvegardeMs = 300, liveMs = 300, liveEcouteMs = 60, effetEcouteMs = 60,
        styleEcouteMs = 60, longueMs = 300, fondMs = 300, regler = s::regler, courtMs = 60,
    )

    private fun played(s: Studio, vararg ids: String): Map<String, Scenarios.Companion.Resultat> {
        val c = carnet()
        Passage(c, scenarios(s, c).etapes().filter { it.name in ids }, crashText = { null }).run()
        return Scenarios.resultats(c.lines()).filter { it.scenario.id in ids }.associateBy { it.scenario.id }
    }

    private fun one(s: Studio, id: String) = played(s, id).getValue(id)

    // --- The list ------------------------------------------------------------------------------------

    @Test
    fun theCatalogueCoversEveryEffectHasAWorkAndStaysUnderHalfAnHour() {
        val l = ScenariosEffets.LISTE
        val types = EffectType.entries.filter { it != EffectType.NONE }
        assertEquals(2 * types.size + 28 + ScenariosEffets.CHAINES.size, l.size)
        assertEquals(89, l.size)
        assertEquals(l.size, l.map { it.id }.toSet().size)
        assertTrue(l.all { it.id.startsWith(ScenariosEffets.PREFIXE) && it.geste != null && it.oracle.isNotBlank() && it.estimeS * 1000L <= it.maxMs })
        // In the full pack, after the rest; never in the quick one.
        assertTrue(Scenarios.LISTE.map { it.id }.containsAll(l.map { it.id }))
        assertTrue(Scenarios.liste(Scenarios.RAPIDE).none { it.id.startsWith(ScenariosEffets.PREFIXE) })
        assertEquals(Scenarios.LISTE.map { it.id }.distinct().size, Scenarios.LISTE.size)
        for (et in types) {
            assertTrue(et.name, l.any { it.geste == "fx:bouts:${et.name}" })
            assertTrue(et.name, l.any { it.geste == "fx:retrait:${et.name}" })
        }
        assertTrue(l.all { s -> ScenariosEffets.connu(s.geste!!.split(':')) })
        assertTrue(ScenariosEffets.CHAINES.all { it.size == ProjetVu.EFFETS_MAX && it.toSet().size == it.size && ScenariosEffets.LOURDS.containsAll(it) })
        // Every scenario has its work, and a broken table line has none.
        val c = carnet()
        assertEquals(emptyList<String>(), scenarios(Studio(), c).sansTravail())
        assertTrue(!ScenariosEffets.connu(listOf("fx", "paire", "REVERB")) && !ScenariosEffets.connu(listOf("fx", "bouts")) && !ScenariosEffets.connu(listOf("fx", "rien", "EQ")))
        val total = l.sumOf { it.estimeS }
        assertTrue(total.toString(), total <= ScenariosEffets.DUREE_MAX_S)
    }

    // --- Both ends of the knobs ----------------------------------------------------------------------

    @Test
    fun everyEffectAtBothEndsPlaysAndIsTakenOffInItsOrder() {
        val s = Studio()
        val ids = ScenariosEffets.LISTE.filter { it.geste!!.startsWith("fx:bouts:") }.map { it.id }
        val r = played(s, *ids.toTypedArray())
        assertEquals(ids.size, r.size)
        r.values.forEach { assertEquals(it.scenario.id + " : " + it.detail, Scenarios.PASSE, it.resultat) }
        // Each one goes into the free slot after EQ and leaves the strip as it found it.
        assertEquals(listOf("EQ", null, null, null), s.slots[0])
        assertEquals(listOf("EQ"), s.disque!!.effets[0])
        // Each knob's value went with its effect.
        assertTrue(s.values.toString(), s.values.isEmpty())
    }

    @Test
    fun aKnobThatDoesNotTakeItsValueASilentEndAndABrokenRemoveAreRed() {
        val id = "fx_bouts_reverb"
        assertEquals(Scenarios.PASSE, one(Studio(), id).resultat)
        val knob = one(Studio().apply { brokenKnob = true }, id)
        assertEquals(knob.detail, Scenarios.ECHOUE, knob.resultat)
        assertTrue(knob.detail, knob.detail.contains("ne se règle pas"))
        val silent = one(Studio().apply { silentOnMax = true }, id)
        assertEquals(silent.detail, Scenarios.ECHOUE, silent.resultat)
        assertTrue(silent.detail, silent.detail.contains("silence avec REVERB réglé à 1"))
        val nan = one(Studio().apply { nan = true }, id)
        assertEquals(nan.detail, Scenarios.ECHOUE, nan.resultat)
        assertTrue(nan.detail, nan.detail.contains("NaN"))
        val stuck = one(Studio().apply { removeWorks = false }, id)
        assertEquals(stuck.detail, Scenarios.ECHOUE, stuck.resultat)
        assertTrue(stuck.detail, stuck.detail.contains("« Retirer l'effet »"))
        val closed = one(Studio().apply { editorOpens = false }, id)
        assertEquals(closed.detail, Scenarios.ECHOUE, closed.resultat)
        assertTrue(closed.detail, closed.detail.contains("ses réglages ne s'ouvrent pas"))
        val unsaved = one(Studio().apply { saves = false }, id)
        assertEquals(unsaved.detail, Scenarios.ECHOUE, unsaved.resultat)
        assertTrue(unsaved.detail, unsaved.detail.contains("disque"))
    }

    @Test
    fun noSliderTheRobotCanSetIsNotMeasuredAndTheStripIsLeftClean() {
        val s = Studio().apply { sliders = false }
        val r = one(s, "fx_bouts_chorus")
        assertEquals(r.detail, Scenarios.NON_MESURE, r.resultat)
        assertTrue(r.detail, r.detail.contains("aucun réglage de CHORUS"))
        assertEquals(listOf("EQ"), s.effets()[0])
    }

    @Test
    fun aFullStripMovesToTheNextAndNoRoomIsNotMeasured() {
        val s = Studio().apply { slots = listOf(listOf("EQ", "GATE", "WAH", "VINYL"), List(4) { null }, List(4) { null }, List(4) { null }) }
        assertEquals(Scenarios.PASSE, one(s, "fx_bouts_eq").resultat)
        assertEquals(listOf("EQ", "GATE", "WAH", "VINYL"), s.effets()[0])
        val full = Studio().apply { slots = List(4) { listOf("EQ", "GATE", "WAH", "VINYL") } }
        assertEquals(Scenarios.NON_MESURE, one(full, "fx_bouts_eq").resultat)
    }

    // --- Taken off while playing ---------------------------------------------------------------------

    @Test
    fun everyEffectTakenOffWhilePlayingComesBackWithUndoAndGoesWithRedo() {
        val s = Studio()
        val ids = ScenariosEffets.LISTE.filter { it.geste!!.startsWith("fx:retrait:") }.map { it.id }
        played(s, *ids.toTypedArray()).values.forEach { assertEquals(it.scenario.id + " : " + it.detail, Scenarios.PASSE, it.resultat) }
        assertEquals(listOf("EQ"), s.effets()[0])
        assertEquals(listOf("EQ"), s.disque!!.effets[0])
    }

    @Test
    fun aRemovalThatStopsTheSongOrThatUndoCannotBringBackIsRed() {
        val id = "fx_retrait_phaser"
        val stops = one(Studio().apply { stopsOnRemove = true }, id)
        assertEquals(stops.detail, Scenarios.ECHOUE, stops.resultat)
        assertTrue(stops.detail, stops.detail.contains("la lecture s'arrête quand PHASER est retiré"))
        val undo = one(Studio().apply { undoWorks = false }, id)
        assertEquals(undo.detail, Scenarios.ECHOUE, undo.resultat)
        assertTrue(undo.detail, undo.detail.contains("« Annuler » ne remet pas PHASER"))
        val stuck = one(Studio().apply { removeWorks = false }, id)
        assertEquals(stuck.detail, Scenarios.ECHOUE, stuck.resultat)
        assertTrue(stuck.detail, stuck.detail.contains("touché en jouant"))
        val hidden = one(Studio().apply { undoShown = false }, id)
        assertEquals(hidden.detail, Scenarios.NON_MESURE, hidden.resultat)
        assertTrue(hidden.detail, hidden.detail.contains("pas de bouton « Annuler »"))
    }

    // --- Heavy pairs and fours -----------------------------------------------------------------------

    @Test
    fun heavyPairsAndFoursPlayWithoutDropoutsAndAreUndone() {
        val s = Studio()
        val ids = ScenariosEffets.LISTE.filter { it.geste!!.startsWith("fx:paire:") || it.geste!!.startsWith("fx:chaine:") }.map { it.id }
        assertEquals(31, ids.size)
        played(s, *ids.toTypedArray()).values.forEach { assertEquals(it.scenario.id + " : " + it.detail, Scenarios.PASSE, it.resultat) }
        assertEquals(listOf("EQ"), s.effets()[0])
        assertTrue(s.effets().drop(1).all { it.isEmpty() })
    }

    @Test
    fun dropoutsNanAndAnUndoThatLeavesThemAreRedNoUndoIsNotMeasured() {
        val id = "fx_paire_reverb_ott"
        val drops = one(Studio().apply { dropouts = true }, id)
        assertEquals(drops.detail, Scenarios.ECHOUE, drops.resultat)
        assertTrue(drops.detail, drops.detail.contains("décrochages avec REVERB + OTT"))
        val chain = one(Studio().apply { dropouts = true }, "fx_chaine_1")
        assertEquals(chain.detail, Scenarios.ECHOUE, chain.resultat)
        val nan = one(Studio().apply { nan = true }, "fx_chaine_2")
        assertEquals(nan.detail, Scenarios.ECHOUE, nan.resultat)
        val left = one(Studio().apply { undoWorks = false }, id)
        assertEquals(left.detail, Scenarios.ECHOUE, left.resultat)
        assertTrue(left.detail, left.detail.contains("les effets restent"))
        val hidden = one(Studio().apply { undoShown = false }, id)
        assertEquals(hidden.detail, Scenarios.NON_MESURE, hidden.resultat)
        // A four needs one strip with four free slots: the first strip (EQ) is skipped.
        val s = Studio()
        assertEquals(Scenarios.PASSE, one(s, "fx_chaine_3").resultat)
        assertEquals(listOf("EQ"), s.effets()[0])
    }

    @Test
    fun theSongsLimitOnLongEffectsIsNotMeasuredNotRed() {
        val longs = EffectType.entries.filter { it.long }
        val reverb = EffectType.REVERB
        val limit = EffectType.longLimit(reverb)
        val pool = longs.filter { EffectType.samePool(it, reverb) }.map { it.name }
        // The strips already hold the pool's limit: one more cannot go in.
        val full = pool.let { p -> List(limit) { p[it % p.size] } }
        val s = Studio().apply { slots = (full.chunked(4).map { it + List(4 - it.size) { null } } + List(4) { List<String?>(4) { null } }).take(4) }
        val r = one(s, "fx_bouts_reverb")
        assertEquals(r.detail, Scenarios.NON_MESURE, r.resultat)
        assertTrue(r.detail, r.detail.contains("pas de place pour REVERB"))
    }
}
