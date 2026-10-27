package com.pocketstudio.app.nav

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.MainActivity
import com.pocketstudio.app.guide.GuideViewModel
import com.pocketstudio.app.journey.Journey
import com.pocketstudio.app.onboarding.LaunchRule
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.app.ui.GestureBudget
import com.pocketstudio.app.ui.withinGestures
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.GuideStages
import com.pocketstudio.model.guide.GuideStart
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.layersOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration

/**
 * G6 (docs/PLAN-GUIDE.md §5, « Vérification »; docs/guide/G6-verification.md): the Guide followed as
 * a beginner follows it, in the real app (MainActivity, rebuilt when the song opens, as on a phone),
 * from a phone where it was just installed: the welcome › « Créer mon premier morceau (guidé) » › the
 * assistant › « Ton morceau est prêt » and the other « Pour commencer » cards › cards of the stages
 * (applied, skipped, refused, heard « Sans / Avec », « Voir le réglage », a track's sheet, a layer
 * added then brought back) › the export; then the app closed and opened again three days later
 * (the Guide's clock moved): « Te revoilà », the same card, every decision kept, nothing asked again.
 * The same, shorter, in the Mode simple. No engine here: the « Sans / Avec » is rendered by a stand-in
 * (two constant levels) and what plays is checked on the project and the ViewModel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h890dp")
class GuideJourneyTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val j = Journey(compose)
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    /** The Guide's clock: the phone's at the start, moved ahead to come back days later. */
    @Volatile
    private var now = System.currentTimeMillis()

    /** The phone as just installed: no project, no settings (as RealUse.freshInstall on the emulator). */
    @Before
    fun freshInstall() {
        for (dir in listOf("projects", "projects-trash", "stems")) File(app.filesDir, dir).deleteRecursively()
        val names = File(app.filesDir, "../shared_prefs").listFiles()?.map { it.name.removeSuffix(".xml") }.orEmpty() + PREFS
        names.distinct().forEach { app.getSharedPreferences(it, 0).edit().clear().commit() }
        // A French phone (Robolectric's is American: the first start would take English).
        app.getSharedPreferences("settings", 0).edit().putString("language", "fr").commit()
        SettingsStore.forgetForTests()
        LaunchRule.askAtStartForTest = true
        shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
        GuideViewModel.clockForTest = { now }
        forgetTheViewModelFactory()
    }

    /**
     * The app's own ViewModels come from the default factory, a process-wide one that keeps the first
     * application it was given: in Robolectric, an earlier test's (its settings, its open project).
     * Forgotten, it is made again with this test's.
     */
    private fun forgetTheViewModelFactory() {
        ViewModelProvider.AndroidViewModelFactory::class.java.getDeclaredField("_instance").apply { isAccessible = true }.set(null, null)
    }

    @After
    fun end() {
        runCatching { scenario?.close() }
        // The closing save lands now, not in the next test's fresh phone.
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        for (dir in listOf("projects", "projects-trash", "stems")) File(app.filesDir, dir).deleteRecursively()
        GuideViewModel.clockForTest = null
        LaunchRule.askAtStartForTest = true
        SettingsStore.forgetForTests()
        com.pocketstudio.model.i18n.I18n.everyday = false
        com.pocketstudio.model.i18n.I18n.lang = com.pocketstudio.model.i18n.Lang.FR
        forgetTheViewModelFactory()
    }

    // --- The app, as a user holds it ---------------------------------------------------------------

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    /** The open project's ViewModel (fetched at each use: opening a song rebuilds the activity and its ViewModels). */
    private fun vm(): RhythmViewModel {
        lateinit var v: RhythmViewModel
        scenario!!.onActivity { v = ViewModelProvider(it)[RhythmViewModel::class.java] }
        return v
    }

    private fun project(): Project = vm().project.value

    private fun ui() = vm().guide.ui.value

    private fun status(card: String) = project().guide.statusOf(card)

    /** The status the panel shows (« fait ailleurs » is worked out by the analysis, not stored). */
    private fun shownStatus(card: String) = ui().cards[card]?.status

    /** Lets the main thread run (its clock too: the analysis waits for the edits to settle) until [ok]. */
    private fun await(what: String, seconds: Int = 60, ok: () -> Boolean) {
        val end = System.currentTimeMillis() + seconds * 1000L
        while (!runCatching(ok).getOrDefault(false)) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            runCatching { compose.waitForIdle() }
            check(System.currentTimeMillis() < end) { "$what: not in time" }
            Thread.sleep(5)
        }
        runCatching { compose.waitForIdle() }
    }

    private fun matches(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun shown(tag: String) = matches(tag) > 0

    /** Waits for [tag] (once on screen), then touches it, scrolled to first. */
    private fun tap(tag: String) {
        await("$tag on screen") { matches(tag) == 1 }
        val node = compose.onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    /** A counted touch (gesture budgets, §4.5 « Gestes »), once [tag] is there. */
    private fun GestureBudget.touch(tag: String) {
        await("$tag on screen") { matches(tag) == 1 }
        tap(tag)
    }

    /** The Guide's next card is [card], on screen in its panel. */
    private fun onCard(card: String) {
        runCatching { await("the card $card") { !ui().loading && ui().next?.id == card && shown("guide_card_$card") } }
            .onFailure { throw IllegalStateException("the card $card: next is ${ui().next?.id}", it) }
    }

    /** The song stopped (« Écouter » starts it; the Guide's « Sans / Avec » never plays over it). */
    private fun stopTheSong() {
        if (vm().transport.value.playing) tap("top_play")
        await("stopped") { !vm().transport.value.playing }
    }

    /** One second of a constant level, stereo: a stand-in for the engine's « Sans / Avec ». */
    private fun tone(level: Float) = FloatArray(OfflineExport.SAMPLE_RATE * 2) { level }

    private fun Project.trackNamed(name: String) = instrumentTracks.first { it.name == name }.id

    private fun Project.strip(name: String) = mixer.channels[InstrumentTrack.mixerChannel(trackNamed(name))]

    /** The welcome › « Créer mon premier morceau (guidé) » › [style] › « Créer maintenant »: the song opens on « Ton morceau est prêt ». */
    private fun firstSong(simple: Boolean, style: String, family: String) {
        launch()
        j.firstLaunch(simple = simple, guided = true)
        j.chooseStyle(style, family)
        j.createNow()
        // Made, saved, then opened: the activity is rebuilt on it and the Guide opens by itself on its first card.
        runCatching { await("the song opened in the Guide", 180) { shown("guide_card_start.ready") && shown("guide_panel") } }.onFailure {
            throw AssertionError("${it.message}; on screen: ${j.tagsMatching(Regex(".+")).distinct().take(80)}; project ${runCatching { project().name + " " + project().style + " " + project().guide.start }.getOrNull()}; wizard ${runCatching { lateinit var w: Any; scenario!!.onActivity { a -> w = ViewModelProvider(a)[com.pocketstudio.app.library.SoundLibraryViewModel::class.java].wizard.value.let { x -> "open=${x.open} creating=${x.creating} error=${x.error} style=${x.styleId}" } }; w }.getOrNull()}; current ${com.pocketstudio.app.projects.CurrentProject.get(app)} vm ${runCatching { vm().projectId }.getOrNull()} on disk ${File(app.filesDir, "projects").list()?.toList()}; log ${com.pocketstudio.app.report.AppLog.dump(8)}", it)
        }
        val p = project()
        assertEquals(style, p.style)
        assertEquals(simple, SettingsStore.state.value.simpleMode)
        // « Mon premier morceau »: the Guide's first cards (the old Missions), the assistant's guide kept with the song.
        assertEquals(GuideStart.FIRST, p.guide.start)
        assertTrue(p.guide.songGuide.isNotEmpty())
        await("analysis") { !ui().loading && ui().simple == simple }
        assertEquals(listOf("start.ready", "start.sound", "start.melody", "start.arrange"), ui().plan.first().items.map { it.id })
        assertEquals(GuideStages.START, ui().plan.first().stage)
        // One voice: none of the old surfaces.
        listOf("next_step_strip", "next_steps", "mission_overlay", "mission_card", "song_ready", "song_ready_bar").forEach { assertFalse(it, shown(it)) }
        compose.onNodeWithText("Ton morceau est prêt !").assertIsDisplayed()
    }

    /** « Ton morceau est prêt » › « Écouter » (about 8 bars before the first Drop): decided, the song plays. */
    private fun listenToTheSong() {
        vm().playStartsForTest = ArrayList()
        tap("guide_option_drop")
        await("ready decided") { status("start.ready") == GuideStatus.DONE }
        assertEquals(listOf("listen ${com.pocketstudio.model.FirstListen.startPart(project())}"), vm().playStartsForTest)
        stopTheSong()
        assertFalse(GuideStart.welcomes(project()))
    }

    // --- The tests ------------------------------------------------------------------------------------

    @Test
    fun aFirstSongFollowedThroughTheGuideToTheExportThenResumedThreeDaysLater() {
        firstSong(simple = false, style = "jump_up", family = "Bass music")

        // « Pour commencer ». Ton morceau est prêt › Écouter.
        listenToTheSong()
        // Change un son › the melody's sounds (« Voir le réglage »), one picked: the card ticks itself.
        onCard("start.sound")
        val lead = com.pocketstudio.app.guide.GuideNav.mainTrackOf(project())!!
        val soundBefore = project().tracks.first { it.id == lead }
        tap("guide_open_melody")
        await("the sounds of the melody") { shown("sound_chooser") }
        val tiles = j.tagsMatching(Regex("(synth|plaits)_sounds_\\d+")).distinct()
        assertTrue("the chooser's sounds: $tiles", tiles.size >= 3)
        tap(tiles[2])
        await("chooser closed") { !shown("sound_chooser") }
        assertNotEquals(soundBefore, project().tracks.first { it.id == lead })
        await("sound ticked") { shownStatus("start.sound") == GuideStatus.DONE_ELSEWHERE }
        // Enregistre une mélodie › Sauter; Arrange ton morceau › Plus tard.
        onCard("start.melody")
        tap("guide_skip")
        await("melody skipped") { status("start.melody") == GuideStatus.SKIPPED }
        onCard("start.arrange")
        tap("guide_later")
        await("arrange later") { status("start.arrange") == GuideStatus.LATER }

        // Audit final: the style's lesson, its sounds one by one (« Garder ce son »).
        await("the sounds") { !ui().loading && ui().next != null && ui().next!!.id != "start.arrange" }
        while (ui().next?.stage == 1) {
            val id = ui().next!!.id
            // The analysis may settle meanwhile and move on (this card no longer next): take the new one.
            if (runCatching { onCard(id) }.isFailure) {
                if (ui().next?.id != id) continue
                onCard(id)
            }
            // « Garder ce son » of this very card (the next one may already be up in its place); the
            // panel may be redrawn under the finger: tried again then.
            val keep = androidx.compose.ui.test.hasTestTag("guide_option_keep") and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("guide_card_$id"))
            var kept = false
            repeat(5) {
                if (!kept && ui().next?.id == id && status(id) != GuideStatus.DONE) {
                    kept = runCatching {
                        compose.onNode(keep).apply { if (!isDisplayed()) performScrollTo() }.performClick()
                        compose.waitForIdle()
                    }.isSuccess
                    if (!kept) await("the card $id again", seconds = 10) { !ui().loading && (ui().next?.id != id || matches("guide_card_$id") > 0) }
                }
            }
            await("$id kept") { status(id) == GuideStatus.DONE || ui().next?.id != id }
        }
        // The stages: « Voilà ce que j'ai mis » first (the assistant's chains, effect by effect).
        onCard("fx.review")
        compose.onNodeWithTag("guide_effects").assertExists()
        // « Voir le réglage »: « Ce qui joue », nothing changed, the card still to decide.
        val beforeLook = project()
        tap("guide_open_all")
        await("what plays") { shown("what_plays") }
        assertEquals(beforeLook, project())
        tap("what_plays_close")
        await("closed") { !shown("what_plays") }
        assertEquals(GuideStatus.TODO, status("fx.review"))
        // Applied (« Garder »): decided.
        tap("guide_option_keep")
        await("review kept") { status("fx.review") == GuideStatus.DONE }

        // The drums tighter: two gestures from the folded Guide (§4.5), the song changed, one line of history.
        onCard("fx.drums")
        tap("guide_bar")
        await("folded") { !shown("guide_panel") }
        val drumsBefore = project()
        compose.withinGestures("appliquer une carte", 2) {
            touch("guide_bar")
            touch("guide_option_softer")
        }
        await("drums applied") { status("fx.drums") == GuideStatus.DONE }
        assertNotEquals(drumsBefore.mixer, project().mixer)
        assertTrue(project().guide.history.any { it.card == "fx.drums" })
        assertEquals("fx.drums", project().guide.step)

        // The bass › Sauter.
        onCard("fx.bass")
        tap("guide_skip")
        await("bass skipped") { status("fx.bass") == GuideStatus.SKIPPED }

        // The Sub's saturation: heard « Sans / Avec » first (one gesture), nothing changed; then refused for good.
        onCard("fx.sub")
        val rendered = ArrayList<Pair<Project, Project>>()
        vm().guideRenderForTest = { with, without -> rendered += with to without; tone(0.5f) to tone(0.25f) }
        val beforeListen = project()
        compose.withinGestures("écouter une proposition", 1) { touch("guide_listen_light") }
        await("rendered") { rendered.isNotEmpty() && (shown("guide_listen_takes") || shown("guide_listen_failed")) }
        val (with, without) = rendered.single()
        assertTrue(with.strip("Sub").inserts.any { it.effect == EffectType.SATURATION })
        assertFalse(without.strip("Sub").inserts.any { it.effect == EffectType.SATURATION })
        assertEquals(beforeListen, project())
        tap("guide_refuse")
        await("sub refused") { project().guide.refused("fx.sub") }
        assertFalse(project().strip("Sub").inserts.any { it.effect == EffectType.SATURATION })

        // The next card is about a track (the pad's space): its track's sheet in one gesture, the five columns there.
        await("next card") { !ui().loading && ui().next?.id !in setOf(null, "fx.sub") }
        val next = ui().next!!
        assertTrue(next.id, next.sheet != null && next.sheetName.isNotEmpty())
        // §4.5 says 1: the link is under the options, out of sight on a phone (a scroll first, docs/guide/G6-verification.md).
        compose.withinGestures("ouvrir la fiche d'une piste", 2) { touch("guide_sheet") }
        await("the track's sheet") { shown("track_sheet_title") }
        compose.onNodeWithTag("track_sheet_title").assertTextContains(next.sheetName, substring = true)
        for (tag in listOf("track_sheet_sound", "track_sheet_when", "track_sheet_chain", "track_sheet_often", "track_sheet_elsewhere", "track_sheet_history")) {
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        }
        tap("track_sheet_close")
        await("sheet closed") { !shown("track_sheet_title") }

        // The layers (picked in the plan): « Air » on the bass from its card (the editor, the role, the sound).
        val bass = project().trackNamed("Basse")
        val layersBefore = project().layersOf(bass)
        tap("guide_stage_2")
        tap("guide_item_layers.bass")
        compose.onNodeWithTag("guide_card_layers.bass").assertIsDisplayed()
        // §4.5 says 2: the editor, the role and the sound make 3, the link 1 more under the options (docs/guide/G6-verification.md).
        val addLayer = compose.withinGestures("ajouter une couche conseillée", 4) {
            touch("guide_open_layers")
            touch("layers_add_air")
            touch("layer_synth_sounds_0")
        }
        await("layer added") { project().layersOf(bass).size == layersBefore.size + 1 }
        tap("layers_close")
        await("editor closed") { !shown("layers_editor") }
        // Brought back from the bass's sheet: its history, « Ramener » (§4.5: fiche › historique › Ramener).
        await("the sheet's history") { ui().sheets.first { it.track.key == GuideTrack.inst(bass) }.lines.isNotEmpty() }
        val added = ui().sheets.first { it.track.key == GuideTrack.inst(bass) }.lines.first()
        assertTrue(added.text, added.text.startsWith("Couche"))
        // §4.5 says 2 (fiche › historique › Ramener): the history is at the sheet's bottom, a scroll more.
        val revert = compose.withinGestures("revenir sur un choix", 3) {
            touch("guide_sheet")
            touch("track_sheet_revert_${added.id}")
        }
        await("brought back") { project().layersOf(bass) == layersBefore }
        tap("track_sheet_close")
        println("G6 gestures: add a layer ${addLayer.gestures} (${addLayer.describe()}), bring a choice back ${revert.gestures} (${revert.describe()})")

        // The export, from the plan's last stage: « Prêt à partager ? » opens.
        tap("guide_stage_7")
        tap("guide_item_share.ready")
        compose.onNodeWithTag("guide_card_share.ready").assertIsDisplayed()
        tap("guide_open_export")
        await("share sheet") { shown("share_sheet") }
        await("share button") { shown("share_go") && shown("share_format_M4a") }
        compose.onNodeWithTag("share_sheet").assertExists()
        tap("share_close")
        await("share closed") { !shown("share_sheet") }

        // --- Three days later. What the Guide says now, then the app closed (saved) and killed.
        tap("guide_back_to_next")
        await("analysis settled") { !ui().loading && ui().next != null }
        val left = project().guide
        val nextCard = ui().next!!.id
        val step = ui().step
        assertTrue(runBlocking { vm().flushNow() })
        scenario!!.close()
        scenario = null
        com.pocketstudio.app.rhythm.ClosingSaves.awaitIdleBlocking(15_000)
        SettingsStore.forgetForTests()
        now += 3 * 24 * 3600 * 1000L
        // « Que veux-tu faire ? » › Continuer: the Guide opens by itself on « Te revoilà ».
        launch()
        j.continueLastProject()
        await("welcome back", 120) { shown("guide_panel") && shown("guide_summary") }
        assertTrue(ui().back)
        val title = ui().cards.getValue(nextCard).title
        compose.onNodeWithText("Te revoilà ! Tu en étais à : $title.").assertIsDisplayed()
        compose.onNodeWithText("Reste :", substring = true).assertIsDisplayed()
        // The same card, at the same step; every decision kept; nothing asked again.
        assertEquals(nextCard, ui().next!!.id)
        assertEquals(step, ui().step)
        compose.onNodeWithTag("guide_card_$nextCard").assertExists()
        val back = project().guide
        assertEquals(left.cards, back.cards)
        assertEquals(left.refusals, back.refusals)
        assertEquals(left.history, back.history)
        assertEquals(left.step, back.step)
        assertFalse(GuideStart.welcomes(project()))
        assertFalse(shown("guide_card_start.ready"))
        for (decided in listOf("start.ready", "start.melody", "fx.review", "fx.drums", "fx.bass", "fx.sub")) {
            assertTrue(decided, ui().cards[decided]?.status?.settled == true)
        }
        assertTrue(project().guide.refused("fx.sub"))
        // Still to decide later: « Arrange ton morceau », put off, waits at the end.
        assertEquals(GuideStatus.LATER, status("start.arrange"))
        // Folded, it stays folded for the rest of the visit.
        tap("guide_bar")
        await("folded") { !shown("guide_panel") }
    }

    @Test
    fun theModeSimpleFollowsTheSameGuideShorter() {
        firstSong(simple = true, style = "jump_up", family = "Bass music")
        // Only the Mode simple's stages, the others under « Aller plus loin ».
        assertTrue(ui().plan.all { it.stage in GuideStages.SIMPLE })
        assertTrue(ui().further.isNotEmpty())
        compose.onNodeWithTag("tab_simple_Morceau").assertIsDisplayed()

        listenToTheSong()
        // Change un son › Plus tard.
        onCard("start.sound")
        tap("guide_later")
        await("sound later") { status("start.sound") == GuideStatus.LATER }
        // Enregistre une mélodie › « Voir le réglage »: the keyboard, « ● Enregistrer » lit, the Guide folded.
        onCard("start.melody")
        tap("guide_open_keys")
        await("the keyboard") { shown("guide_spot") && !shown("guide_panel") }
        assertTrue(shown("record_start"))
        tap("guide_spot_close")
        tap("guide_bar")
        onCard("start.melody")
        tap("guide_skip")
        await("melody skipped") { status("start.melody") == GuideStatus.SKIPPED }
        onCard("start.arrange")
        tap("guide_later")
        await("arrange later") { status("start.arrange") == GuideStatus.LATER }

        // A card of the arrangement (picked in the plan), applied in its everyday words: a Break made
        // from the Drop, one undo step.
        tap("guide_stage_4")
        tap("guide_item_arr.subtract")
        val before = project()
        tap("guide_option_break")
        await("break applied") { status("arr.subtract") == GuideStatus.DONE }
        assertEquals(before.song.size + 1, project().song.size)
        vm().undo()
        await("undone") { project().song == before.song && status("arr.subtract") != GuideStatus.DONE }
        vm().redo()
        await("redone") { project().song.size == before.song.size + 1 && status("arr.subtract") == GuideStatus.DONE }

        // « Aller plus loin »: the other stages, one touch away.
        tap("guide_further_toggle")
        await("further") { shown("guide_stage_3") }
        // The export: « Prêt à partager ? » › Envoyer, its button lit.
        tap("guide_stage_7")
        tap("guide_item_share.ready")
        tap("guide_open_export")
        await("Envoyer") { shown("share_go") && !shown("guide_panel") }
        assertTrue(shown("share_format_Wav"))
    }

    private companion object {
        /** The app's settings files (Robolectric keeps them in memory: listed rather than found on disk). */
        val PREFS = listOf("nav", "missions", "settings", "next_steps", "song_ready", "share", "tools", "launch", "projects")
    }
}
