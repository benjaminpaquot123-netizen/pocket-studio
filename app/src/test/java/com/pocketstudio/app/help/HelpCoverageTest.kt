package com.pocketstudio.app.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.export.ExportOptions
import com.pocketstudio.app.export.ExportOptionsPanel
import com.pocketstudio.app.export.ShareSheet
import com.pocketstudio.app.export.ShareSheetActions
import com.pocketstudio.app.export.ShareSheetUi
import com.pocketstudio.app.live.LiveActions
import com.pocketstudio.app.live.LiveScreen
import com.pocketstudio.app.live.LiveScreenUi
import com.pocketstudio.app.mixer.MasterProActions
import com.pocketstudio.app.mixer.MasterProController
import com.pocketstudio.app.mixer.MasterProPanel
import com.pocketstudio.app.padlive.ChallengeUi
import com.pocketstudio.app.padlive.PadLiveActions
import com.pocketstudio.app.padlive.PadLiveScreen
import com.pocketstudio.app.padlive.PadLiveUi
import com.pocketstudio.app.remix.CutModel
import com.pocketstudio.app.remix.CutScreen
import com.pocketstudio.app.remix.NoStemBoardActions
import com.pocketstudio.app.remix.StemBoardActions
import com.pocketstudio.app.remix.StemBoardState
import com.pocketstudio.app.remix.StemBoardUi
import com.pocketstudio.app.remix.StemPeakSource
import com.pocketstudio.app.remix.StemPeaks
import com.pocketstudio.app.remix.rememberStemBoardView
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.rhythm.PadSheetActions
import com.pocketstudio.app.rhythm.PadSheetContent
import com.pocketstudio.app.rhythm.padSheetUi
import com.pocketstudio.app.ui.TempoActions
import com.pocketstudio.app.ui.TempoSheet
import com.pocketstudio.model.DrumPattern
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.model.LiveStarter
import com.pocketstudio.model.LiveState
import com.pocketstudio.model.LiveSlots
import com.pocketstudio.model.MasterPro
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLayer
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.PadMode
import com.pocketstudio.model.PadSample
import com.pocketstudio.model.Project
import com.pocketstudio.model.Remix
import com.pocketstudio.model.RemixCell
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.addTrack
import com.pocketstudio.app.remix.BOARD_RATE
import com.pocketstudio.model.CutPiece
import com.pocketstudio.model.SongCuts
import com.pocketstudio.model.StemCuts
import com.pocketstudio.ui.HelpAbout
import com.pocketstudio.ui.PocketStudioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lot « Aide à jour » (Benjamin, 2026-11-03: « Tous les boutons avec ? en petit… est-ce bien à
 * jour ? »): every control of the screens that came after the Lexique — Pad Live (its pads, pages,
 * held effects, « Plus », the pad's sheet, the challenge), the tempo sheet, the Live's grid and its
 * Remix cell, « Découper la chanson », the master pro, the file's final form and the pro readings,
 * the share sheet, « Couches » — is either explained by a « ? » ([com.pocketstudio.ui.HelpDot] names
 * the controls it explains, [HelpAbout]) or listed here as obvious (back, play, close, a value).
 * A control added to one of these screens without its « ? » (or without being judged obvious here)
 * fails this test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h1600dp-xxhdpi")
class HelpCoverageTest {
    @get:Rule
    val compose = createComposeRule()

    /** A tag pattern: `*` any text, `#` a number. */
    private fun regex(glob: String) = Regex(glob.split('*').joinToString(".*") { part -> part.split('#').joinToString("[0-9]+") { Regex.escape(it) } })

    private class Scan(val controls: Set<String>, val explained: List<Regex>)

    /** Every control (a tagged node one can touch, tick or slide) on screen, sheets and menus included, and what the « ? » explain. */
    private fun scan(): Scan {
        compose.waitForIdle()
        val nodes = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()
        val controls = nodes.filter { n ->
            n.config.getOrNull(SemanticsActions.OnClick) != null || n.config.getOrNull(SemanticsActions.SetProgress) != null ||
                n.config.getOrNull(SemanticsProperties.ToggleableState) != null || n.config.getOrNull(SemanticsActions.OnLongClick) != null
        }.mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.filter { !it.startsWith("help_") }.toSet()
        val explained = nodes.flatMap { it.config.getOrNull(HelpAbout).orEmpty() }.map(::regex)
        return Scan(controls, explained)
    }

    /**
     * On the screen shown: each control whose tag starts with one of [prefixes] is explained by a « ? »
     * or matches [obvious]; each of [must] is there and explained (the screen really shows what is checked).
     */
    private fun check(what: String, prefixes: List<String>, obvious: List<String>, must: List<String>) {
        val s = scan()
        val easy = obvious.map(::regex)
        fun explained(tag: String) = s.explained.any { it.matches(tag) }
        val ours = s.controls.filter { t -> prefixes.any { t.startsWith(it) } }
        val alone = ours.filter { t -> !explained(t) && easy.none { it.matches(t) } }.sorted()
        assertTrue("$what: controls without a « ? » (add one, or list them as obvious here): $alone", alone.isEmpty())
        val missing = must.filter { it !in s.controls }
        assertTrue("$what: not on screen (the test no longer shows what it checks): $missing", missing.isEmpty())
        val unexplained = must.filter { !explained(it) }
        assertTrue("$what: must have a « ? »: $unexplained", unexplained.isEmpty())
    }

    // --- Pad Live -------------------------------------------------------------------------------

    private val pads: LivePads = PadLive.placed(
        LivePads(), 0,
        LiveStarter.PADS.mapIndexed { i, p -> LiveClip("s$i.wav", p.name, if (p.oneShot) 1 else p.beats, p.oneShot, p.gain) } +
            listOf(PadLive.withMode(LiveClip("m.wav", "Montée", 1, oneShot = true), PadMode.HOLD), LiveClip("c.wav", "Chute", 1, oneShot = true, group = 1)),
    )
    private val states = List(LivePads.ALL) { if (it == 0) 2 else 0 }

    private fun padLive(challenge: ChallengeUi = ChallengeUi.Off) {
        compose.setContent {
            PocketStudioTheme {
                PadLiveScreen(
                    PadLiveUi(pads, states, page = 0, pack = "fete", playing = true, tempoBpm = 124.0, beatsPerBar = 4, canUndo = true, challenge = challenge),
                    PadLiveActions(), clockOf = { 6 }, record = { com.pocketstudio.app.live.RecordButton(com.pocketstudio.app.live.TakeUi.Ready) {} }, still = true,
                )
            }
        }
    }

    /** What Pad Live's screen shows and needs no « ? »: back, ▶, the tempo (its sheet has one), ●, the pads, a row's ▶, « Annuler ». */
    private val padLiveObvious = listOf(
        "padlive_back", "padlive_play", "padlive_tempo", "padlive_tempo_less", "padlive_tempo_more", "padlive_rec", "padlive_pad_#", "padlive_mode_#",
        "padlive_row_#", "padlive_undo", "live_record",
        // « Tout couper » (step 4, n° 7): a red ■ with its word; « Plus »'s « ? » says it stops the music too.
        "padlive_stop_all",
        // « Plus » opens its row, which has its own « ? ».
        "padlive_more",
    )

    @Test
    fun padLiveWithPlusOpen() {
        padLive()
        check(
            "Pad Live", listOf("padlive_", "live_"), padLiveObvious,
            must = listOf("padlive_page_A", "padlive_page_D", "padlive_pack", "padlive_challenge_open", "padlive_fx_filter", "padlive_fx_echo", "padlive_fx_roll", "padlive_played"),
        )
        compose.onNodeWithTag("padlive_more").performClick()
        // « Plus » open: « Enregistrer ce que je joue » keeps its words beside « Moins », its « ? » waits (seen above).
        check(
            "Pad Live › Plus", listOf("padlive_", "live_"), padLiveObvious + "padlive_played",
            must = listOf("padlive_fx_riser", "padlive_fx_brake", "padlive_fx_stutter", "padlive_stop_all", "padlive_live", "padlive_effects", "padlive_party_open", "padlive_share"),
        )
    }

    @Test
    fun padLiveSettingsSheet() {
        padLive()
        compose.onNodeWithTag("padlive_mode_0").performClick()
        check(
            "Pad Live › réglages du pad", listOf("padlive_"),
            padLiveObvious + listOf(
                "padlive_beats_#", "padlive_gain", "padlive_change_sound", "padlive_clear", "padlive_pitch_down", "padlive_pitch_up", "padlive_reverse",
            ),
            must = listOf("padlive_mode_shot", "padlive_mode_loop", "padlive_mode_hold", "padlive_group_0", "padlive_group_1", "padlive_attack", "padlive_release"),
        )
    }

    @Test
    fun padLiveChallenge() {
        val challenge = PadChallenge.make(3, PadChallenge.shotsOf(pads, 0), 124.0)!!
        padLive(ChallengeUi.Ready(challenge))
        check("Pad Live › Défi", listOf("padlive_challenge"), listOf("padlive_challenge_close"), must = listOf("padlive_challenge_listen", "padlive_challenge_go"))
    }

    @Test
    fun tempoSheet() {
        compose.setContent { PocketStudioTheme { TempoSheet(124.0, TempoActions(), onClose = {}) } }
        check(
            "Tempo", listOf("tempo_"),
            listOf("tempo_close", "tempo_minus", "tempo_plus", "tempo_preset_#", "tempo_key_*", "tempo_back_to_lead"),
            must = listOf("tempo_tap", "tempo_half", "tempo_double"),
        )
    }

    // --- Where a beginner gets stuck (hands-on audit, M5): recording on the keys, the Mix simple, « Partie en boucle » ---

    /** The Clavier's record bar, as AppRoot wires it (« Enregistrer ma mélodie »): its « ? » says where the notes go. */
    @Test
    fun recordOnTheKeys() {
        java.io.File(ApplicationProvider.getApplicationContext<android.app.Application>().filesDir, "projects").deleteRecursively()
        val vm = com.pocketstudio.app.rhythm.RhythmViewModel(ApplicationProvider.getApplicationContext())
        compose.setContent {
            PocketStudioTheme {
                com.pocketstudio.app.record.RecordBar(
                    com.pocketstudio.app.record.recordBarUi(vm, "Enregistrer ma mélodie", com.pocketstudio.app.record.TakeSource.KEYS),
                    com.pocketstudio.app.record.recordBarActions(vm, com.pocketstudio.app.record.TakeSource.KEYS),
                )
            }
        }
        check("Clavier › Enregistrer", listOf("record_", "keep_played"), listOf("record_count_in"), must = listOf("record_start"))
    }

    /** The Mode simple's « Mix »: one « ? » for the faders, the output's words and « Tout à zéro », one for « Partie en boucle ». */
    @Test
    fun simpleMix() {
        val bass = com.pocketstudio.app.ui.SimpleTrack("bass", 2, "Basse", androidx.compose.ui.graphics.Color.Cyan, 0.7f, mute = false, solo = false)
        val ui = com.pocketstudio.app.mixer.SimpleMixUi("Mon tube", listOf(bass), emptyMap(), 0f, looping = false, listening = null, tip = null)
        compose.setContent {
            PocketStudioTheme { com.pocketstudio.app.mixer.SimpleMixScreen(ui, { FloatArray(com.pocketstudio.engine.NativeEngine.METERS) }, { FloatArray(com.pocketstudio.engine.NativeEngine.LOUDNESS) }, com.pocketstudio.app.mixer.SimpleMixActions()) }
        }
        check(
            "Mix simple", listOf("mix_"), listOf("mix_mute_#", "mix_alone_*", "mix_pad_alone_#", "mix_tip_fix"),
            must = listOf("mix_loop", "mix_reset", "mix_fader_2", "mix_out_fader"),
        )
        // Two « ? »: « Partie en boucle » has its own, beside it.
        val terms = compose.onAllNodes(SemanticsMatcher("any") { true }, useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }.filter { it.startsWith("help_") }
        assertTrue("Mix simple: « Partie en boucle » has no « ? » of its own: $terms", terms.size >= 2 && scan().explained.count { it.matches("mix_loop") } == 1)
    }

    // --- The Live's grid and its Remix cell -------------------------------------------------------

    private fun live() {
        val drums = DrumPattern().toggle(0, 0).toggle(0, 8)
        val p = Project(
            tempoBpm = 113.0, drums = drums,
            scenes = listOf(Scene("A", drums, label = "Intro"), Scene("B", drums, label = "Couplet")),
            remix = Remix("boys", "Boys", songBpm = 113.0, seconds = 180.0, cells = listOf(RemixCell(1, "v.wav", "Voix", 2, 113.0, 3, 42.0))),
        ).addTrack("Basse")!!.sanitized()
        val ui = LiveScreenUi(p, LiveState.on(0, 0).launched(LiveSlots.REMIX, 1, 16), clock = 22, playing = true)
        compose.setContent { PocketStudioTheme { LiveScreen(ui, LiveActions()) } }
    }

    /** The grid's own controls: the bricks' letters, the cells, the ■, the mutes, « Lancer à », « Cases − + ». */
    private val gridTags = listOf("live_row_", "live_pad_", "live_stop_", "live_mute_", "live_quantum", "live_cases", "live_columns_more")

    @Test
    fun liveGrid() {
        live()
        check(
            "Live › grille", gridTags, listOf(),
            must = listOf("live_row_0", "live_pad_${SongPart.DRUMS}_0", "live_pad_${SongPart.REMIX}_1", "live_stop_all", "live_quantum", "live_cases_more"),
        )
    }

    @Test
    fun liveRemixCellMenu() {
        live()
        compose.onNodeWithTag("live_pad_${SongPart.REMIX}_1").performTouchInput { longClick() }
        check(
            "Live › case Remix", listOf("remix_cell_"), listOf("remix_cell_clear", "remix_cell_cancel", "remix_cell_scrim", "remix_cell_menu"),
            must = listOf("remix_cell_earlier", "remix_cell_later", "remix_cell_put"),
        )
    }

    // --- « Découper la chanson » -------------------------------------------------------------

    @Test
    fun cutScreen() {
        val frames = 145L * BOARD_RATE
        val bar = 4 * 60.0 / 113.0 * BOARD_RATE
        val peaks = StemPeakSource { _ -> StemPeaks.of(frames, 1) { _, count -> FloatArray(count) { 0.4f } } }
        val ui = StemBoardUi("boys", "Boys", frames, 113.0, 0.0, peaks, peaksContext = kotlin.coroutines.EmptyCoroutineContext)
        val cuts = SongCuts("boys", listOf(StemCuts(3, listOf(CutPiece(0, Math.round(2 * bar)), CutPiece(Math.round(2 * bar), Math.round(4 * bar))), chosen = listOf(0), auto = "Phrases")))
        val actions = object : StemBoardActions by NoStemBoardActions {
            override val state = MutableStateFlow(StemBoardState())
            override fun savedCuts(setId: String, frames: Long) = cuts
        }
        compose.setContent {
            PocketStudioTheme {
                val model = remember { CutModel("boys", frames, 113.0, 0.0, cuts, mode = com.pocketstudio.model.AutoCut.Phrases) }
                CutScreen(ui, actions, onClose = {}, model = model, view = rememberStemBoardView(ui))
            }
        }
        check(
            "Découper la chanson", listOf("cut_"),
            listOf("cut_close", "cut_play", "cut_stem_#", "cut_undo", "cut_piece_play_#", "cut_message_ok", "cut_zoom", "cut_overview"),
            must = listOf("cut_auto", "cut_magnet", "cut_here", "cut_start", "cut_end", "cut_remove", "cut_loop", "cut_to_pads", "cut_to_live", "cut_to_library", "cut_to_track", "cut_piece_0"),
        )
    }

    /** « Un bout » (its default): one « ? » by the title explains the bars, both listens, the menus and where it goes. */
    @Test
    fun cutScreenOnePiece() {
        val frames = 145L * BOARD_RATE
        val bar = 4 * 60.0 / 113.0 * BOARD_RATE
        val peaks = StemPeakSource { _ -> StemPeaks.of(frames, 1) { _, count -> FloatArray(count) { 0.4f } } }
        val ui = StemBoardUi("boys", "Boys", frames, 113.0, 0.0, peaks, peaksContext = kotlin.coroutines.EmptyCoroutineContext)
        val cuts = SongCuts("boys", listOf(StemCuts(3, listOf(CutPiece(0, Math.round(2 * bar))), auto = "Phrases", span = CutPiece(0, Math.round(2 * bar)))))
        val actions = object : StemBoardActions by NoStemBoardActions {
            override val state = MutableStateFlow(StemBoardState())
            override fun savedCuts(setId: String, frames: Long) = cuts
        }
        compose.setContent {
            PocketStudioTheme {
                val model = remember { CutModel("boys", frames, 113.0, 0.0, cuts) }
                CutScreen(ui, actions, onClose = {}, model = model, view = rememberStemBoardView(ui))
            }
        }
        check(
            "Découper la chanson › Un bout", listOf("cut_"),
            listOf("cut_close", "cut_stem_#", "cut_undo", "cut_message_ok", "cut_zoom", "cut_overview"),
            must = listOf("cut_play", "cut_auto", "cut_magnet", "cut_start", "cut_end", "cut_span_play", "cut_to_pads", "cut_to_live", "cut_to_library", "cut_to_track"),
        )
    }

    // --- Master pro, the file, the readings, sharing, « Couches » ----------------------------------

    private fun masterActions(): MasterProActions {
        val project = MutableStateFlow(
            Project().let { p ->
                p.copy(mixer = p.mixer.copy(
                    master = p.mixer.master.mapIndexed { i, ins -> if (i == 0) Insert(type = EffectType.PARAM_EQ.ordinal) else ins },
                    pro = MasterPro(targetLufs = -11f, reference = "ref.wav", referenceName = "Réf", referenceLufs = -8.4f),
                ))
            },
        )
        return MasterProActions(
            MasterProController(CoroutineScope(Dispatchers.Unconfined), ApplicationProvider.getApplicationContext(), project, { _, _ -> }, {}, { _, _, _ -> }, {}, { "help" }),
        )
    }

    @Test
    fun masterPro() {
        val actions = masterActions()
        compose.setContent { PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { MasterProPanel(actions) } } }
        compose.onNodeWithTag("master_pro").performClick()
        check(
            "Master pro", listOf("pro_", "master_pro"), listOf("master_pro"),
            must = listOf(
                "pro_ceiling_-1.0", "pro_mono_120", "pro_attack", "pro_release", "pro_chain",
                "pro_listen_${com.pocketstudio.engine.NativeEngine.LISTEN_WITHOUT}", "pro_compare", "pro_reference",
            ),
        )
    }

    @Test
    fun finalFile() {
        val actions = masterActions()
        val export = ExportUi(what = ExportWhat.Song, parts = 5, songBars = 64, pro = ExportOptions(bits = 16, fromBar = 9, toBar = 24))
        compose.setContent { PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { ExportOptionsPanel(export, hasSong = true, actions) } } }
        compose.onNodeWithTag("export_final").performClick()
        check(
            "Fichier final", listOf("export_"), listOf(),
            must = listOf("export_final", "export_bits_16", "export_shaping", "export_range_bars", "export_range_from", "export_trim", "export_artist", "export_report"),
        )
    }

    @Test
    fun stemsWithOrWithoutSends() {
        val export = ExportUi(what = ExportWhat.Stems, parts = 5, songBars = 64)
        val actions = masterActions()
        compose.setContent { PocketStudioTheme { Column(Modifier.verticalScroll(rememberScrollState())) { ExportOptionsPanel(export, hasSong = true, actions) } } }
        compose.onNodeWithTag("export_final").performClick()
        check("Fichier final › pistes", listOf("export_"), listOf(), must = listOf("export_stems_dry"))
    }

    @Test
    fun proReadings() {
        val report = com.pocketstudio.engine.ExportReport(lufs = -9.0, truePeakDb = -1.2, seconds = 120.0, sampleRate = 48000, driveDb = 5.0, lossDb = 2.0, lra = 6.0)
        val export = ExportUi(loudness = -9.0, truePeakDb = -1.2, seconds = 120.0, proReport = report)
        compose.setContent { PocketStudioTheme { Column { com.pocketstudio.app.mixer.ExportReportLine(export) } } }
        check("Mesures pro", listOf("export_"), listOf(), must = listOf("export_pro_readings"))
    }

    @Test
    fun shareSheet() {
        compose.setContent {
            PocketStudioTheme { ShareSheet(ShareSheetUi("Mon tube", seconds = 180, parts = 6, checks = emptyList(), lufs = -9.0, truePeakDb = -1.1), ShareSheetActions()) }
        }
        check(
            "Partager", listOf("share_"),
            listOf("share_close", "share_name", "share_listen", "share_video", "share_other", "share_go", "share_fix_*", "share_project", "share_my_exports"),
            must = listOf("share_format_M4a", "share_format_Wav", "share_details"),
        )
    }

    @Test
    fun padSheetLayers() {
        val p = Project().let { it.copy(drums = it.drums.withSample(1, PadSample("a.wav", "A", layers = listOf(PadLayer("b.wav", "Fort", 100))))) }
        compose.setContent { PocketStudioTheme { PadSheetContent(padSheetUi(p, 1, listOf("Kick 1", "Kick 2"), pro = true), PadSheetActions(), close = {}) } }
        compose.onNodeWithTag("pad_layer_add").performClick()
        check("Rythme › Couches", listOf("pad_layer"), listOf("pad_layer_sound_#"), must = listOf("pad_layer_add", "pad_layer_slider_0", "pad_layer_remove_0"))
    }

    /** Each new « ? » reads in one go: a term, one to three short sentences; no two share a term (nor with the Lexique). */
    @Test
    fun theNewHelpsAreShortAndDistinct() {
        val all = ScreenHelp.ALL
        assertTrue(all.all { it.term.isNotBlank() && it.text.length in 40..330 })
        val terms = (all + Glossary.ALL).map { it.term }
        assertTrue("terms used twice: ${terms.groupBy { it }.filter { it.value.size > 1 }.keys}", terms.size == terms.toSet().size)
        // Every one is shown by a « ? » somewhere in the app's code.
        val src = java.io.File("src/main/java").walkTopDown().filter { it.extension == "kt" && it.name != "ScreenHelp.kt" }.joinToString("\n") { it.readText() }
        val names = Regex("""val (\w+): Help get\(\)""").findAll(java.io.File("src/main/java/com/pocketstudio/app/help/ScreenHelp.kt").readText()).map { it.groupValues[1] }.toList()
        assertTrue(names.size == all.size)
        val unused = names.filter { "ScreenHelp.$it" !in src }
        assertTrue("ScreenHelp never shown: $unused", unused.isEmpty())
    }
}
