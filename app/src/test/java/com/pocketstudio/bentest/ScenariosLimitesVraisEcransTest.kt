package com.pocketstudio.bentest

import androidx.activity.ComponentActivity
import android.os.Looper
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.text.AnnotatedString
import org.robolectric.Shadows.shadowOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pocketstudio.app.journey.ComposeRuleDriver
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.journey.JourneyDriver
import com.pocketstudio.app.nav.AppHarness
import com.pocketstudio.app.rhythm.ProjectOpening
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectCatalog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tour 9 catalogue « Styles, structure et limites » ([ScenariosLimites]) on the app's real screens (AppRoot, no engine):
 * each new gesture finds its button where the robot looks for it, and its oracle reads the real project and the real file.
 * The engine (playing, the level) is made up from the ▶ touched, as in [ScenariosVraisEcransTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h780dp")
class ScenariosLimitesVraisEcransTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()
    private val h = AppHarness(compose)

    @After
    fun back() = h.finish()

    private val RENOMMER = setOf("Renommer")

    private inner class Vrai(private val d: JourneyDriver = ComposeRuleDriver(compose)) : JourneyDriver by d, Sonde, Appli {
        var playing = false
        var partie = -1
        /** The other project opened from « Mes projets » (see [click]); null: the harness's own. */
        var ailleurs: String? = null
        override fun partieJouee(): Int? = partie.takeIf { playing && it >= 0 }
        /** The guided pack: Benjamin's « Fait » plugs a headset in ([branche]), and an unplug may stop the song ([arretAuChangement]). */
        var sortie = Materiel(casqueFilaire = false, bluetooth = false)
        var branche = true
        var arretAuChangement = false
        var eco: Boolean? = true
        val questions = ArrayList<String>()
        override fun materiel() = sortie
        override fun economieEnergie() = eco
        override fun poser(question: Question, maxMs: Long): Reponse {
            questions += question.id
            if (question.id == Epreuves.SORTIE && branche) sortie = Materiel(casqueFilaire = true, bluetooth = false)
            if (question.id == Epreuves.SORTIE && arretAuChangement) playing = false
            return Reponse(Verdict.OUI)
        }
        val touched = ArrayList<String>()
        /** A hole while the kit sheet is open (the gestures of « Changement de kit »): silence although it plays. */
        var muetKit = false
        @Volatile private var kitOuvert = false

        /**
         * « Renommer » open: under Robolectric a text field in a Compose dialog never lets Compose rest
         * (Tour3RenameIdleTest), so while it is open the screen is read and touched without waiting for rest, frame by
         * frame, as that test does. The phone has no such wait.
         */
        @Volatile var fenetre = false

        private fun frames() = repeat(6) {
            runCatching { compose.mainClock.advanceTimeByFrame() }
            shadowOf(Looper.getMainLooper()).idle()
        }

        private fun noeuds(tag: String, merged: Boolean): List<SemanticsNode> = compose.runWithoutImplicitWait {
            frames()
            compose.onAllNodesWithTag(tag, useUnmergedTree = !merged).fetchSemanticsNodes(atLeastOneRootRequired = false)
        }

        /** The name typed in the open window (its SetText action, as Tour3RenameIdleTest). */
        fun saisirDansFenetre(tag: String, text: String): Boolean = runCatching {
            val n = noeuds(tag, merged = false).first()
            compose.runOnUiThread { n.config[SemanticsActions.RequestFocus].action!!() }
            frames()
            compose.runOnUiThread { noeuds(tag, merged = false).first().config[SemanticsActions.SetText].action!!(AnnotatedString(text)) }
            frames()
        }.isSuccess

        /** The project menu's items have no tag: found by their words (« texte:Dupliquer »), as the in-app driver does. */
        override fun count(tag: String, merged: Boolean): Int = when {
            fenetre -> noeuds(tag, merged).size
            tag.startsWith(TEXTE) -> compose.onAllNodesWithText(tag.removePrefix(TEXTE), useUnmergedTree = true).fetchSemanticsNodes().size
            else -> d.count(tag, merged)
        }

        override fun bounds(tag: String): Rect = if (fenetre) noeuds(tag, merged = true).first().boundsInWindow else d.bounds(tag)
        override fun scrollTo(tag: String) { if (!fenetre) d.scrollTo(tag) }
        override fun idle(ms: Long) {
            if (!fenetre) return d.idle(ms)
            val end = System.currentTimeMillis() + ms
            do compose.runWithoutImplicitWait { frames() } while (System.currentTimeMillis() < end)
        }

        override fun click(tag: String) {
            touched += tag
            if (fenetre) {
                compose.runWithoutImplicitWait {
                    val n = noeuds(tag, merged = true).first()
                    compose.runOnUiThread { n.config[SemanticsActions.OnClick].action!!() }
                    frames()
                }
                // The window's « OK » or « Annuler » closes it: the screen rests again.
                if (tag.startsWith("project_rename_")) fenetre = false
                return
            }
            if (tag.startsWith(TEXTE)) {
                val texte = tag.removePrefix(TEXTE)
                if (texte in RENOMMER) {
                    // The touch opens the window: no wait for rest after it.
                    compose.runWithoutImplicitWait {
                        // The item's own node (merged), the one that holds the touch, not its inner text.
                        var n: SemanticsNode? = compose.onAllNodesWithText(texte).fetchSemanticsNodes(atLeastOneRootRequired = false).first()
                        while (n != null && SemanticsActions.OnClick !in n.config) n = n.parent
                        val item = n!!
                        fenetre = true
                        compose.runOnUiThread { item.config[SemanticsActions.OnClick].action!!() }
                        frames()
                    }
                    return
                }
                compose.onAllNodesWithText(texte, useUnmergedTree = true).onFirst().performClick()
                runCatching { compose.waitForIdle() }
                return
            }
            if (tag == Epreuves.DRUMS_NAME) kitOuvert = true
            if (tag.startsWith("kit_choice_") || tag == "kit_sheet_close") kitOuvert = false
            if (tag in setOf("thumb_play", "top_song", "live_play")) playing = !playing
            // A pad of the Live launches its cell; « ■ Tout » stops them.
            if (tag.startsWith("live_pad_")) playing = true
            if (tag == "live_stop_all") playing = false
            // A part's ▶ on the Structure plays the song from it (or queues it while the song plays).
            if (tag.startsWith("part_play_")) { playing = true; partie = tag.removePrefix("part_play_").toInt() }
            // Another project in « Mes projets »: the app rebuilds its activity on it (Activity.recreate), which leaves
            // no screen to this rule under Robolectric. Played here: this one saved, the list closed, the other one
            // read from the disk as the open one; the song stops with the old screens. The list is closed by the open
            // project's own row (the app's « resume », no rebuild). The phone does the real rebuild.
            val id = tag.removePrefix("project_")
            if (tag.startsWith("project_") && id in ProjectOpening.projectStore(h.app).list()) {
                if (id != h.vm.projectId) {
                    kotlinx.coroutines.runBlocking { h.vm.flushNow() }
                    playing = false
                    ailleurs = id
                    d.click("project_${h.vm.projectId}")
                    return
                }
                ailleurs = null
            }
            d.click(tag)
        }

        override fun lire() = Sonde.Lecture(0, 0.2f, if (playing && !(muetKit && kitOuvert)) 0.5f else 0f, false, true, playing, cpuWorst = 0.3f, gapWorst = 1f)
        override fun ouvrirStudio() = Unit

        /** « Another app » takes the sound: a duck lowers it (plays on), a call stops the song as PlaybackService does. */
        val focus = ArrayList<Boolean>()
        override fun prendreSon(duck: Boolean, pendant: () -> Unit): Boolean {
            focus += duck
            if (!duck) playing = false
            pendant()
            return true
        }
        override fun export() = Appli.ExportVu(false, emptyList(), null)
        override fun memoireMo() = 200
        override fun imagesFin() = 100 to 2
        override fun capture(file: File) = false
        override fun demander(question: String, maxMs: Long): Boolean? = null

        override fun projets(): Map<String, ProjetVu.Disque> {
            val store = ProjectOpening.projectStore(h.app)
            return store.list().mapNotNull { id -> store.load(id)?.let { id to ProjetVu.Disque(it.name, it.song.size, emptyList()) } }.toMap()
        }

        override fun projet(): ProjetVu {
            ailleurs?.let { id ->
                val o = ProjectOpening.projectStore(h.app).load(id) ?: error("$id n'est plus sur le disque")
                return ProjetVu(id, o.name, o.song.size, emptyList(), ProjetVu.Disque(o.name, o.song.size, emptyList()))
            }
            val p = h.vm.project.value
            fun effets(of: Project) = of.mixer.channels.map { ch -> ch.inserts.filter { it.effect != EffectType.NONE }.map { it.effect.name } }
            val disk = runCatching { ProjectOpening.projectStore(h.app).load(h.vm.projectId) }.getOrNull()
            return ProjetVu(
                h.vm.projectId, p.name, p.song.size, effets(p), disk?.let { ProjetVu.Disque(it.name, it.song.size, effets(it), tempo = it.tempoBpm, montee = ProjetVu.montee(it)) },
                tempo = p.tempoBpm, montee = ProjetVu.montee(p), pads = ProjetVu.pads(p),
            )
        }
    }

    private fun scenarios(v: Vrai) = Scenarios(
        Journey(v), v, v, Carnet(tmp.newFile()), tmp.newFolder(), ecran = { v.tags(Regex(".+")) }, courtMs = 1_500,
        ecouteMs = 1_000, decrochagesMs = 1_000, sauvegardeMs = 20_000, liveMs = 5_000, liveEcouteMs = 1_000, effetEcouteMs = 1_000,
        saisir = { tag, text -> if (v.fenetre) v.saisirDansFenetre(tag, text) else runCatching { compose.onNodeWithTag(tag, useUnmergedTree = true).performTextReplacement(text) }.isSuccess },
    )

    /** Runs the scenario [id] as the robot does (its reset first, its oracle at the end). */
    private fun jouer(v: Vrai, id: String): String = scenarios(v).etapes().first { it.name == id }.work(Long.MAX_VALUE)

    private fun disque() = ProjectOpening.projectStore(h.app).load(h.vm.projectId)!!

    @Test
    fun eachStructureGestureIsUndoneRedoneAndLeftAsItWas() {
        h.start()
        val v = Vrai()
        val parts = h.vm.project.value.song.size
        for (s in ScenariosLimites.LISTE.filter { it.id.startsWith("lim_refaire_") && !it.id.startsWith("lim_refaire_lecture_") }) {
            val text = jouer(v, s.id)
            assertTrue(s.id + " : " + text, text.contains("annulé") && text.contains("rétabli") && text.endsWith("puis remis"))
            assertEquals(s.id, parts, h.vm.project.value.song.size)
        }
        assertEquals(parts, disque().song.size)
        assertEquals(0, ProjetVu.montee(h.vm.project.value))
        assertTrue(v.touched.count { it == "top_redo" || it == "thumb_redo" } >= 6)
    }

    @Test
    fun theGesturesAreUndoneAndRedoneWhileTheSongPlays() {
        h.start()
        val v = Vrai()
        val parts = h.vm.project.value.song.size
        for (s in ScenariosLimites.LISTE.filter { it.id.startsWith("lim_refaire_lecture_") }) {
            val text = jouer(v, s.id)
            assertTrue(s.id + " : " + text, text.contains("rétabli") && text.contains("lecture par"))
            assertEquals(s.id, parts, h.vm.project.value.song.size)
            assertTrue(s.id, !v.playing)
        }
    }

    @Test
    fun threeBreaksAreUndoneOneByOneThenRedone() {
        h.start()
        val v = Vrai()
        val parts = h.vm.project.value.song.size
        val text = jouer(v, "lim_annuler_pile_3")
        assertTrue(text, text.startsWith("3 breaks ($parts puis ${parts + 3} parties)"))
        assertEquals(3, v.touched.count { it == "top_redo" || it == "thumb_redo" })
        assertEquals(6, v.touched.count { it == "top_undo" })
        assertEquals(parts, h.vm.project.value.song.size)
        assertEquals(parts, disque().song.size)
    }

    @Test
    fun theLowestAndHighestTemposAreTypedHeldAndPutBack() {
        h.start()
        val v = Vrai()
        val tempo = h.vm.project.value.tempoBpm
        for ((id, limit) in listOf("lim_tempo_min" to Project.MIN_TEMPO, "lim_tempo_max" to Project.MAX_TEMPO, "lim_tempo_max_lecture" to Project.MAX_TEMPO)) {
            val text = jouer(v, id)
            assertTrue(id + " : " + text, text.startsWith("tempo ${ToutTester.fmt(limit)} bpm tapé, enregistré") && text.contains("lecture par"))
            assertEquals(id, tempo, h.vm.project.value.tempoBpm, 0.001)
            assertTrue(id, !v.playing)
        }
        assertTrue(v.touched.containsAll(listOf("tempo_key_4", "tempo_key_0", "tempo_key_3", "tempo_key_ok", "tempo_minus", "tempo_plus")))
        assertEquals(tempo, disque().tempoBpm, 0.001)
        assertEquals(0, v.count("tempo_sheet", false))
    }

    @Test
    fun temposOutOfRangeAreRefused() {
        h.start()
        val v = Vrai()
        val tempo = h.vm.project.value.tempoBpm
        val text = jouer(v, "lim_tempo_hors_bornes")
        assertTrue(text, text.startsWith("39, 301, 999 bpm refusés"))
        assertEquals(tempo, h.vm.project.value.tempoBpm, 0.001)
        assertEquals(0, v.count("tempo_sheet", false))
    }

    @Test
    fun aVeryLongNameIsCutAndAnAccentedOneKept() {
        h.start()
        val v = Vrai()
        val store = ProjectOpening.projectStore(h.app)
        val before = store.list().toSet()
        val name = h.vm.project.value.name
        val long = jouer(v, "lim_nom_long")
        assertTrue(long, long.contains("« ${ScenariosLimites.NOM_LONG.take(ProjectCatalog.MAX_NAME)} » (${ProjectCatalog.MAX_NAME}) relu sur le disque"))
        val accents = jouer(v, "lim_nom_accents")
        assertTrue(accents, accents.contains("« ${ScenariosLimites.NOM_ACCENTS} »"))
        // The robot's copies are deleted, the open song keeps its name.
        assertEquals(before, store.list().toSet())
        assertEquals(name, h.vm.project.value.name)
        assertTrue(v.touched.count { it.startsWith("project_delete_") } == 2)
    }

    @Test
    fun aSongSixteenPartsLongerPlaysThenIsUndone() {
        h.start()
        val v = Vrai()
        val parts = h.vm.project.value.song.size
        val text = jouer(v, "lim_projet_enorme")
        assertTrue(text, text.startsWith("$parts puis ${parts + 16} parties, enregistré, lecture par"))
        assertEquals(parts, h.vm.project.value.song.size)
        assertEquals(parts, disque().song.size)
    }

    private companion object {
        const val TEXTE = InAppDriver.TEXTE
    }
}
