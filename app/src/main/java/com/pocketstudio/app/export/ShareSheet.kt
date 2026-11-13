@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.export

import com.pocketstudio.ui.ScaledBottomSheet

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pocketstudio.app.mixer.HardLimiting
import com.pocketstudio.app.mixer.LIMITER_METER
import com.pocketstudio.app.mixer.StripInfo
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.rhythm.stripSounds
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectCatalog
import com.pocketstudio.model.Remix
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.app.learn.spot
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.remixOverrunSeconds
import com.pocketstudio.model.withRemixCutAtSongEnd
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SCROLL_FADE
import com.pocketstudio.ui.fadingVerticalEdges
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt

/**
 * The format « Prêt à partager ? » sends (SharedPreferences "share"): M4A (small, plays
 * everywhere) until the user picks WAV (studio quality); the last choice is kept for next time.
 */
class SharePrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var format by mutableStateOf(parse(prefs.getString(KEY, null)))
        private set

    fun choose(format: ExportFormat) {
        val f = if (format == ExportFormat.Wav) ExportFormat.Wav else ExportFormat.M4a
        this.format = f
        prefs.edit().putString(KEY, f.name).apply()
    }

    companion object {
        const val PREFS = "share"
        private const val KEY = "format"

        /** Only M4A and WAV are offered here (FLAC stays in the Mixeur's « Autres exports »). */
        fun parse(saved: String?): ExportFormat = if (saved == ExportFormat.Wav.name) ExportFormat.Wav else ExportFormat.M4a
    }
}

/**
 * What the master's meters say, for « Prêt à partager ? ». [limiterHard]: the Mixeur's
 * « Le limiteur travaille fort » watch ([HardLimiting]) fired while the sheet was open and the
 * song played, with no loudness target (with one, the master chain works hard on purpose: see
 * [shareLevels]). [masterDb]: the master's gain, for the one-touch fix (6 dB lower), offered only
 * while [canLowerMaster] (a target would give the 6 dB straight back).
 *
 * [lufs] (integrated since ▶, in LUFS) and [truePeakDb] (since ▶, in dBTP) come from the master
 * section's loudness meter: the sheet shows the loudness under the duration, and a true peak
 * above [TRUE_PEAK_MAX_DB] counts as saturation too.
 */
data class ShareLevels(
    val limiterHard: Boolean = false,
    val masterDb: Float = 0f,
    val lufs: Double? = null,
    val truePeakDb: Double? = null,
    val canLowerMaster: Boolean = true,
    /** S5: what the master chain takes off for the loudness target (dB), as its estimate reads it; null without a target. */
    val lossDb: Double? = null,
) {
    val saturates: Boolean get() = limiterHard || (truePeakDb ?: Double.NEGATIVE_INFINITY) > TRUE_PEAK_MAX_DB

    /** The fix can still lower the master by [Mixer.HINT_STEP_DB]. */
    val canLower: Boolean get() = canLowerMaster && masterDb - Mixer.HINT_STEP_DB >= Mixer.MASTER_MIN_DB

    companion object {
        /** A streaming service's ceiling: above it, the M4A clips once decoded. */
        const val TRUE_PEAK_MAX_DB = -1.0
    }
}

/**
 * The levels of [p] as the sheet reads them: [loudness] is the master meter's reading
 * ([RhythmViewModel.loudness], laid out as [NativeEngine.LOUDNESS_INTEGRATED]…, [NativeEngine.LOUDNESS_NONE]
 * for none yet) and [limiterHard] the Mixeur's watch. With a target ([Mixer.target]), the master
 * chain pulls the limiter hard on purpose (up to 6 LU): the watch says nothing then, and lowering
 * the master is no fix (the target's drive would undo it); the true peak still tells the truth.
 */
fun shareLevels(p: Project, loudness: FloatArray, limiterHard: Boolean): ShareLevels {
    val noTarget = p.mixer.target == LoudnessTarget.OFF
    fun reading(i: Int) = loudness.getOrElse(i) { NativeEngine.LOUDNESS_NONE }.takeIf { it > NativeEngine.LOUDNESS_NONE + 1f }?.toDouble()
    return ShareLevels(
        limiterHard = limiterHard && noTarget, masterDb = p.mixer.masterDb,
        lufs = reading(NativeEngine.LOUDNESS_INTEGRATED), truePeakDb = reading(NativeEngine.LOUDNESS_TRUE_PEAK),
        canLowerMaster = noTarget,
        lossDb = if (noTarget) null else loudness.getOrNull(NativeEngine.LOUDNESS_LOSS)?.toDouble()?.takeIf { it.isFinite() && it >= 0.1 },
    )
}

/** One line of « Prêt à partager ? »: something that would make the shared song sound wrong, and its fix. */
sealed class ShareCheck {
    /** A warning (amber), or a hint (grey) the user may well want as it is. */
    open val warning: Boolean get() = false

    /** Tracks soloed ([names]; the remix's own stems soloed: [stems]): nothing else would be heard. Fix: « Tout réactiver ». */
    data class Solo(val names: List<String>, val stems: List<Int> = emptyList()) : ShareCheck() {
        override val warning get() = true
    }

    /**
     * Tracks muted in the mixer although they play in the song ([names], their strips [chs]): said,
     * not changed by itself (it may be wanted); « Tout réentendre » brings them back.
     */
    data class Muted(val names: List<String>, val chs: List<Int> = emptyList(), val stems: List<Int> = emptyList()) : ShareCheck()

    /** Nothing to hear at all. */
    data object Empty : ShareCheck() {
        override val warning get() = true
    }

    /** No song yet: the scene is shared looped, [bars] bars long. Fix: « Modifier le morceau ». */
    data class NoSong(val bars: Int) : ShareCheck()

    /** A song of a single short part ([seconds] long). Fix: « Modifier le morceau ». */
    data class Short(val seconds: Int) : ShareCheck()

    /**
     * Chantier 10 (G16, T5-Ec-28): the remix's song goes on [seconds] past the song's end, where the file stops:
     * its end will be cut. [canCut]: « Couper la chanson à la fin » would change something (not when the song comes in
     * after the end: nothing of it is before the cut).
     */
    data class RemixOverrun(val seconds: Int, val canCut: Boolean = true) : ShareCheck() {
        override val warning get() = true
    }

    /** The limiter works hard (or the true peak goes over −1 dBTP). Fix: the master 6 dB lower, or the Mixeur. */
    data object Saturation : ShareCheck() {
        override val warning get() = true
    }
}

/** A song made of one part shorter than this (in seconds) is pointed out. */
const val SHORT_SONG_SECONDS = 30

/** How long the shared file lasts, in seconds: the whole song, or the scene looped [loopBars] bars. */
fun shareSeconds(p: Project, loopBars: Int): Int {
    val steps = if (p.song.isNotEmpty()) p.song.sumOf { p.partSteps(it).toLong() } else loopBars.toLong() * p.stepsPerBar
    // A sixteenth lasts 15/bpm seconds.
    return (steps * 15.0 / p.tempoBpm).roundToInt()
}

/**
 * Everything « Prêt à partager ? » points out before the song leaves the phone, in the order it
 * shows them: a solo left on, tracks muted although they play, nothing to hear, no song yet or a
 * single short part, the remix's song cut at the end, saturation. [strips]: the mixer's strips (their names); [loopBars]: the
 * length of the scene shared when there is no song.
 */
fun shareChecks(p: Project, strips: List<StripInfo>, levels: ShareLevels, loopBars: Int): List<ShareCheck> = buildList {
    val channels = p.mixer.channels
    fun name(s: StripInfo) = DefaultNames.shown(s.name)
    val soloed = strips.filter { channels.getOrNull(it.ch)?.solo == true }
    val sounding = stripSounds(p, song = p.song.isNotEmpty()).filter { it.second }.map { it.first.second }.toSet()
    // Audit ultra round 3 (n° 12, seat F b 58): the remix's song is four stems, each muted or soloed on its own
    // (a « MR » version without the voice): « aucune piste coupée » was said over a muted voice.
    val remixHeard = Remix.STRIP in sounding && channels.getOrNull(Remix.STRIP)?.mute != true
    val remixName = strips.firstOrNull { it.ch == Remix.STRIP }?.let(::name) ?: "Remix"
    fun stemName(stem: Int) = com.pocketstudio.app.remix.StemRouting.stemLabel(stem) + " (" + remixName + ")"
    val stems = if (remixHeard) Remix.SHOWN_ORDER.filter { it < p.remix.stems.size } else emptyList()
    val soloStems = stems.filter { p.remix.stems[it].solo }
    if (soloed.isNotEmpty() || soloStems.isNotEmpty()) add(ShareCheck.Solo(soloed.map(::name) + soloStems.map(::stemName), soloStems))
    val muted = strips.filter { it.ch in sounding && channels.getOrNull(it.ch)?.mute == true }
    val mutedStems = stems.filter { p.remix.stems[it].mute }
    if (muted.isNotEmpty() || mutedStems.isNotEmpty()) add(ShareCheck.Muted(muted.map(::name) + mutedStems.map(::stemName), muted.map { it.ch }, mutedStems))
    when {
        sounding.isEmpty() -> add(ShareCheck.Empty)
        p.song.isEmpty() -> add(ShareCheck.NoSong(loopBars))
        p.song.size == 1 && shareSeconds(p, loopBars) < SHORT_SONG_SECONDS -> add(ShareCheck.Short(shareSeconds(p, loopBars)))
    }
    if (remixHeard) p.remixOverrunSeconds().takeIf { it > 0 }?.let { add(ShareCheck.RemixOverrun(it, canCut = p.withRemixCutAtSongEnd() != p)) }
    if (levels.saturates) add(ShareCheck.Saturation)
}

/**
 * What the name field starts with: the project's own name as the app shows it (audit ultra round 3, n° 13, seat
 * F d 71: « Beat for Tom (copie) » in the English app), or a suggestion when the app gave it one.
 */
fun initialShareName(name: String): String =
    if (ShareSong.isDefaultName(name)) ShareSong.suggestedName(DefaultNames.shown(name)) else DefaultNames.shown(name).trim()

/**
 * Audit ultra round 3 (n° 13, seat F b 41): the name typed for the shared song renames the project only while
 * the project still has a name the app gave it (« Mon projet 3 »): a version named « Spring night (MR) » leaves
 * the project « Spring night » as it is (it was renamed at each export).
 */
fun shareRenamesProject(projectName: String, typed: String): Boolean {
    val clean = typed.trim()
    return clean.isNotEmpty() && ShareSong.isDefaultName(projectName) && clean != DefaultNames.shown(projectName).trim() && clean != projectName.trim()
}

/** "2:38" from seconds. */
fun shareClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/**
 * Chantier 1 of the hands-on audit (G1, recette tel-06: « perdu pour régler la longueur »): the loop's length, shown and
 * set in « Prêt à partager ? » when the project has no song yet. [bars]: the length sent (the same setting as Mix's
 * « Longueur de la boucle »); [options]: the lengths offered.
 */
data class LoopPick(val bars: Int, val options: List<Int>) {
    companion object {
        /** The lengths a beginner is offered (Mix offers 1 and 2 bars too). */
        val BARS = listOf(4, 8, 16, 32)

        /** [bars] chosen among [BARS], the length in use and, when a voice or the remix's song ends later, that end ([audioBars]). */
        fun of(bars: Int, sceneBars: Int, audioBars: Int): LoopPick {
            val toTheEnd = audioBars.coerceAtMost(RhythmViewModel.MAX_LONG_EXPORT_BARS).takeIf { it > sceneBars }
            return LoopPick(bars, (BARS + bars + listOfNotNull(toTheEnd)).distinct().sorted())
        }
    }
}

/** What « Prêt à partager ? » shows. [running]: an export is already being made. */
data class ShareSheetUi(
    val name: String,
    val seconds: Int,
    val parts: Int,
    val checks: List<ShareCheck>,
    val format: ExportFormat = ExportFormat.M4a,
    val playing: Boolean = false,
    val running: Boolean = false,
    val canLower: Boolean = true,
    /** The song's loudness, once the master's meter gives it (see [ShareLevels.lufs]). */
    val lufs: Double? = null,
    /** Its true peak, in dBTP (under « détails »). */
    val truePeakDb: Double? = null,
    /** The project's own name now (« Mon projet 1 »): said when « Partager » will rename it ([name] differs). */
    val projectName: String? = null,
    /** The project, for the Remix rights reminder (lot C, [com.pocketstudio.app.remix.RightsLine]). */
    val rightsProject: com.pocketstudio.model.Project? = null,
    /** S5: how much the loudest moments are tassés for the volume ([ShareLevels.lossDb]). */
    val lossDb: Double? = null,
    /**
     * Audit ultra round 3 (n° 10): the loudness target the file will aim at, chosen here (the project's one target,
     * the same as in Mix); null: not offered (Mode simple: no jargon).
     */
    val target: com.pocketstudio.app.mixer.TargetPick? = null,
    /** The loop's length to pick (no song yet: a part goes out looped); null: the whole song goes out. */
    val loop: LoopPick? = null,
    /** Lot 6, U2: the loudness the file aims at (the project's target, LUFS); null: no target. */
    val exportLufs: Double? = null,
)

/** Lot 6, U2: what the file of [p] aims at (its one loudness target), in LUFS; null without a target. */
fun shareExportLufs(p: Project): Double? = LoudnessTargets.of(p)?.toDouble()

class ShareSheetActions(
    val setName: (String) -> Unit = {},
    val setFormat: (ExportFormat) -> Unit = {},
    /** « Partager »: the name kept, the song exported in the format chosen, then Android's share sheet. */
    val share: () -> Unit = {},
    /** ▶ the whole song once more (or stop it). */
    val listen: () -> Unit = {},
    /** « Tout réactiver »: no solo left. */
    val unsolo: () -> Unit = {},
    /** « Baisser le volume général »: the master 6 dB lower. */
    val lowerMaster: () -> Unit = {},
    /** « Voir dans Mix » (null: not offered, the Mode simple has no Mix). */
    val toMixer: (() -> Unit)? = {},
    /** « Tout réentendre »: the muted strips given heard again. */
    val unmute: (List<Int>) -> Unit = {},
    /** Audit ultra round 3 (n° 12): the remix's stems muted ([com.pocketstudio.model.Remix.stems] indices) heard again. */
    val unmuteStems: (List<Int>) -> Unit = {},
    /** The remix's soloed stems back to normal. */
    val unsoloStems: (List<Int>) -> Unit = {},
    /** « Modifier le morceau »: the song's parts (the Morceau tab, which the button is named after). */
    val toStructure: () -> Unit = {},
    /** « Autres exports »: separate tracks, MIDI, FLAC… (Mixeur › Exporter). */
    val otherExports: () -> Unit = {},
    /** « Vidéo pour les réseaux »: 30 s of the song in 9:16 (phase 39). */
    val video: () -> Unit = {},
    val close: () -> Unit = {},
    /** Audit ultra round 3 (n° 10): « Volume visé » chosen here (the project's target, as in Mix). */
    val chooseTarget: (com.pocketstudio.model.TargetChoice) -> Unit = {},
    /** Audit ultra round 3 (n° 13, seat F d 22, 61): the project itself (.zip), to go on with it on another device; null: not offered. */
    val sendProject: (() -> Unit)? = null,
    /** « Longueur de la boucle » picked here ([LoopPick]): the same setting as Mix's. */
    val setLoopBars: (Int) -> Unit = {},
    /** Chantier 10: « Allonger le morceau »: parts added until the remix's song ends in the song. */
    val lengthenForRemix: () -> Unit = {},
    /** Chantier 10: « Couper la chanson à la fin »: the remix's song stops with the song, faded. */
    val cutRemix: () -> Unit = {},
)

/** « Prêt à partager ? » as a bottom sheet above the tab it was opened from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(ui: ShareSheetUi, actions: ShareSheetActions, simple: Boolean = false) {
    // Opened whole: the « Partager » button is never below the fold.
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = actions.close, sheetState = state, containerColor = Neon.Surface) {
        ShareSheetContent(ui, actions, simple = simple)
    }
}

/**
 * The sheet's content: the name (already filled in), how long the song lasts, the checks with
 * their one-touch fixes, M4A or WAV, and one big « Partager ». The rarer exports are a link away.
 */
@Composable
fun ShareSheetContent(
    ui: ShareSheetUi, actions: ShareSheetActions, modifier: Modifier = Modifier, page: Boolean = false,
    /** The Mode simple's words (its tracks are in « Mes pistes », it has no Mix). */
    simple: Boolean = page,
) {
    // Audit final (09): « Partager » stays in a footer under what scrolls, never below the fold (the
    // A11 lying down at 130 % cut it in half at the screen's bottom).
    // Little room (a phone at 130 %: about 430 dp between the bars): « Mes fichiers » under « Partager » keeps its name
    // alone, so that what scrolls above (the name, the duration) keeps a line more.
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
    val short = constraints.hasBoundedHeight && maxHeight < 500.dp
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).then(if (page) Modifier else Modifier.navigationBarsPadding()).testTag("share_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
    // Lot 6, U4 / U15 / N7 (seats A c, F a, H a, D a): the line cut by the bottom edge, right above the pinned M4A / WAV
    // row, looked covered by it. Rule 17 (« Tout défilement se voit »): the cut edge fades while more waits below (and
    // the top edge once scrolled), its band tagged for the screen scans (LayoutScan).
    val scroll = rememberScrollState()
    // Tour 6, control seat Ac-2 (E3, screen 40): the sheet opened with « Autres fichiers » and its « ? » cut in half by
    // the bottom edge, right above the pinned M4A / WAV row. What scrolls now ends where a line ends: its height is cut
    // down to the top of the first line that does not fit whole ([snapHeight]), so every line shows whole or not at all.
    var stops by remember { mutableStateOf(SnapStops()) }
    val topPad = if (page) 12.dp else 0.dp
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().weight(1f, fill = false).layout { measurable, c ->
        val h = if (c.hasBoundedHeight) snapHeight(stops, topPad.roundToPx(), c.maxHeight) else c.maxHeight
        val p = measurable.measure(c.copy(minHeight = minOf(c.minHeight, h), maxHeight = h))
        layout(p.width, p.height) { p.place(0, 0) }
    }) {
    SnapColumn(
        Modifier.fillMaxWidth().fadingVerticalEdges(scroll, SCROLL_FADE).verticalScroll(scroll).padding(horizontal = 20.dp).padding(top = topPad),
        gap = 12.dp, onStops = { if (it != stops) stops = it },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (page) tr("Fini ? Partage ton morceau") else tr("Prêt à partager ?"), // Little room: the title on one line, so that the duration under the name stays in sight.
                style = if (short) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            // Phase 36: the Mode simple's « Envoyer » tab is this page: nothing to close.
            if (!page) IconButton(onClick = actions.close, modifier = Modifier.testTag("share_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary)
            }
        }
        // « Partager » renames the project too (a default « Mon projet 3 » becomes the name typed): the label says so.
        // Lot 6, U16 (seat F a #66): otherwise the label is « Nom » alone (it read « Nom du morceau envoyé », « already
        // sent? », after a share closed without sending); the line under it says the project keeps its name.
        // Round 2, n° 26: the name selected at the touch, what is typed replaces it.
        val renames = ui.projectName?.let { shareRenamesProject(it, ui.name) || ShareSong.isDefaultName(it) } ?: true
        com.pocketstudio.app.ui.SelectAllTextField(
            value = ui.name, onValueChange = actions.setName, label = { Text(if (renames) tr("Nom du morceau (et du projet)") else tr("Nom")) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().testTag("share_name"),
        )
        // Testers (lot « tuto 4 »): « Mon projet 1 » became « Mon morceau » without a word.
        ui.projectName?.let { DefaultNames.shown(it).trim() }?.takeIf { it.isNotEmpty() && it != ui.name.trim() }?.let { old ->
            if (renames) Text(tr("« Partager » renomme aussi le projet « %s ».", old), style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary, modifier = Modifier.testTag("share_renames"))
            else Text(tr("Le projet garde son nom « %s ».", old), style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary, modifier = Modifier.testTag("share_keeps_name"))
        }
        // How long it lasts, and one last listen (the saturation is checked while it plays).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(shareClock(ui.seconds), style = MonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = Neon.Cyan,
                        modifier = Modifier.testTag("share_duration"))
                    // Its own column beside the clock: wrapped (130 %), its second line no longer runs into the clock.
                    Text(
                        if (ui.parts == 0) tr("une partie en boucle") else trCount(ui.parts, "1 partie", "%d parties"),
                        style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.padding(start = 10.dp).weight(1f, fill = false),
                    )
                }
                // Lot 6, U2 (seat A c): the level said is the one the file will have (its target), not the one heard while
                // it played (« streaming (−14) » said of a file aiming at −8,5). Tour 6, control seat Ac-2 (new, screen 40):
                // Pro said what played too (« −13,0, comme la musique en streaming » under « Club »): both modes say the
                // file's level, with the same words and the same number (E1); the reading heard stays under « détails ».
                (ui.exportLufs ?: ui.lufs)?.let { lufs ->
                    // Plain words first; the units (LUFS, dBTP) under « détails », for whoever knows them.
                    var details by rememberSaveable { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tr("Volume : %s, %s", String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f", lufs).replace('-', '−'), shareLevelWords(lufs, ui.exportLufs?.let { ui.target?.choice })),
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f).testTag("share_loudness"),
                        )
                        if (!simple) Text(
                            if (details) tr("masquer") else tr("détails"), style = MaterialTheme.typography.labelSmall, color = Neon.Cyan,
                            modifier = Modifier.clickable(role = Role.Button) { details = !details }.padding(6.dp).testTag("share_details"),
                        )
                        if (!simple) com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.Glossary.LUFS, about = listOf("share_details"))
                    }
                    if (details && !simple) {
                        Text(
                            tr("%s LUFS · crête %s dBTP", LoudnessTargets.format(ui.lufs ?: lufs), ui.truePeakDb?.let { LoudnessTargets.format(it) } ?: "—"),
                            style = MonoStyle.copy(fontSize = 12.sp), color = Neon.TextSecondary, modifier = Modifier.testTag("share_loudness_details"),
                        )
                    }
                    if (!simple) ui.lossDb?.let { com.pocketstudio.app.mixer.LossLine(it, Modifier.testTag("share_loss")) }
                }
            }
            OutlinedButton(onClick = actions.listen, modifier = Modifier.heightIn(min = 48.dp).testTag("share_listen")) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + if (ui.playing) tr("Arrêter") else tr("Écouter"), maxLines = 1)
            }
        }
        // G1: how long the loop sent lasts, set where the song leaves (it was five levels away, under Mix › « Avancé », and
        // out of reach in the Mode simple); the duration above follows at once.
        ui.loop?.let { loop ->
            Column(Modifier.testTag("share_loop")) {
                Text(tr("Longueur de la boucle"), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    loop.options.forEach { bars ->
                        val label = trCount(bars, "%d mesure", "%d mesures")
                        FilterChip(
                            selected = loop.bars == bars, onClick = { actions.setLoopBars(bars) }, enabled = !ui.running,
                            // The number alone on the chip (four fit a phone's line); TalkBack hears « 16 mesures ».
                            label = { Text("$bars", modifier = Modifier.semantics { contentDescription = label }) },
                            colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("share_bars_$bars"),
                        )
                    }
                }
            }
        }
        // Audit ultra round 3 (n° 10, seats A d 3, F b 26): the target chosen here, where the song leaves (it was only
        // said, « se règle dans Mix », and only once set there). The same setting as Mix's. Pro keeps it above the checks.
        if (!simple) ui.target?.let { com.pocketstudio.app.mixer.TargetPicker(it, actions.chooseTarget, "share_target", enabled = !ui.running, note = tr("Le même réglage que dans Mix.")) }
        if (simple && ui.checks.isEmpty()) ui.target?.let { SimpleTargetPicker(it, actions.chooseTarget, enabled = !ui.running) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = (if (ui.checks.isEmpty()) Modifier.layoutId(WHOLE_LINE) else Modifier).testTag("share_checks")) {
            if (ui.checks.isEmpty()) {
                CheckLine(Icons.Filled.CheckCircle, Neon.Acid, tr("Tout est prêt : aucun solo oublié, aucune piste coupée."), "share_check_ok")
            }
            ui.checks.forEach { check -> CheckRow(check, ui, actions, simple) }
        }
        // Tour 6, seat A c (E1): in the Mode simple, a warning (« Rien à entendre »…) keeps its place at the top of what
        // scrolls and the target comes under it; with nothing to fix, the target comes first, in sight when the page
        // opens (control seat Ac-2: under « Tout est prêt » it fell below the edge on a phone): two touches, no scroll.
        if (simple && ui.checks.isNotEmpty()) ui.target?.let { SimpleTargetPicker(it, actions.chooseTarget, enabled = !ui.running) }
        OutlinedButton(onClick = actions.video, enabled = !ui.running, modifier = Modifier.layoutId(WHOLE_LINE).fillMaxWidth().heightIn(min = 48.dp).testTag("share_video")) {
            Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  " + tr("Vidéo pour les réseaux · 30 s"), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        // The formats' « ? » sits here, at the end of the line about the other formats: beside the M4A / WAV
        // chips it did not fit a 360 dp phone and fell alone on a line of its own.
        Row(Modifier.layoutId(WHOLE_LINE), verticalAlignment = Alignment.CenterVertically) {
        // Chantier 2 (G3): the shared link row. Lot 2: the Mode simple has no Mix (the line takes it to the full mode).
        com.pocketstudio.ui.LinkRow(
            tr("Autres fichiers"), onClick = actions.otherExports, modifier = Modifier.weight(1f).testTag("share_other"), icon = Icons.Filled.LibraryMusic,
            subtitle = if (simple) tr("Piste par piste, MIDI, FLAC… (passe en mode Pro)") else tr("Piste par piste, MIDI, FLAC… dans Mix"),
            subtitleModifier = Modifier.testTag("share_other_where"),
        )
            com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.Glossary.FORMATS, about = listOf("share_format_*"))
        }
        // Audit ultra round 3 (n° 13, seat F d 22, 61): the project itself, to go on with it elsewhere, where one sends
        // (it was only in Mes projets › ⋯).
        actions.sendProject?.let { send ->
            Row(Modifier.layoutId(WHOLE_LINE), verticalAlignment = Alignment.CenterVertically) {
                // Tour 6, seat A c (E6): one touch here opens Android's share menu (it sends the .zip, it does not save it
                // on its own): the line says « Partager » (the app's one verb, answer 1A), so the word matches what the touch does.
                com.pocketstudio.ui.LinkRow(
                    tr("Partager le projet (.zip)"), onClick = send, modifier = Modifier.weight(1f).testTag("share_project"), icon = Icons.Filled.Archive,
                    subtitle = tr("Pour un autre appareil ou une copie de sécurité"), enabled = !ui.running,
                )
                // Round 3 integration: its « ? », as the other exports' line has one (what a project file is, where it opens).
                com.pocketstudio.ui.HelpDot(
                    com.pocketstudio.ui.Help(tr("Partager le projet (.zip)"), tr("Pour le reprendre sur un autre appareil : un fichier .zip avec tous ses sons, à ouvrir là-bas dans Mes projets › Importer.")),
                    about = listOf("share_project"),
                )
            }
        }
        // Tour 6, seat A c (E3): a scrollable tail the height of the fade band, so the last line (« Autres fichiers », the
        // « .zip » line) can scroll fully clear of the fade and never looks covered by the pinned M4A / WAV row.
        androidx.compose.foundation.layout.Spacer(Modifier.height(SCROLL_FADE))
    }
        if (scroll.canScrollForward) {
            androidx.compose.foundation.layout.Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(SCROLL_FADE).testTag("scroll_hint_fade"),
            )
        }
    }
        // Testers (lot « tuto 4 »): M4A or WAV stays in sight with « Partager » (a short page, the tutorial's
        // panel under it, cut the chips in half at the scroll's edge).
        FlowRow(
            Modifier.padding(horizontal = 20.dp), itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The chips say what they are (« M4A · pour partager »): on one line on a phone, no « Format » before them.
            listOf(ExportFormat.M4a to tr("M4A · pour partager"), ExportFormat.Wav to tr("WAV · qualité max")).forEach { (format, label) ->
                FilterChip(
                    selected = ui.format == format, onClick = { actions.setFormat(format) }, label = { Text(label) },
                    colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("share_format_${format.name}"),
                )
            }
        }
        ui.rightsProject?.let { com.pocketstudio.app.remix.RightsLine(it, Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
        Button(
            onClick = actions.share, enabled = ui.name.isNotBlank() && !ui.running,
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 56.dp).spot("share_go", tr("Partager")).testTag("share_go"),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + tr("Partager"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        // G2 (recette tel-03: « bloqué » once the file was sent): the files made, right under « Partager », in sight
        // without scrolling in both modes (it was the last line of what scrolls, drawn as a plain sentence).
        MyExportsButton("share_my_exports", Modifier.padding(horizontal = 20.dp), compact = short)
    }
    }
}

/**
 * The words of the share line's level: the target's own when the file aims at Club or Streaming (bench replay of Ac-2:
 * « Club · −8 LUFS » chosen, the line said « très fort (les plateformes le baisseront) »), else what [lufs] sounds like.
 */
internal fun shareLevelWords(lufs: Double, choice: com.pocketstudio.model.TargetChoice?): String = when (choice) {
    com.pocketstudio.model.TargetChoice.CLUB -> tr("fort, comme la musique de club")
    com.pocketstudio.model.TargetChoice.STREAMING -> tr("comme la musique en streaming (−14)")
    else -> Loudness.describe(lufs)
}

/** One line of [SnapColumn]: where it starts and ends, in pixels; [whole]: never to be cut by the edge ([WHOLE_LINE]). */
internal data class SnapLine(val top: Int, val bottom: Int, val whole: Boolean)

/** The lines of [SnapColumn], top to bottom. */
internal data class SnapStops(val lines: List<SnapLine> = emptyList())

/** The layout id of a line that shows whole or not at all (a link row and its « ? »): [snapHeight]. */
internal const val WHOLE_LINE = "whole_line"

/**
 * The height of what scrolls, at most [max] px: [max] itself, unless the bottom edge would cut a [SnapLine.whole] line;
 * then down to that line's top (the gap above it at the bottom), so it shows whole once scrolled, never cut in half.
 * [pad]: the space above the lines.
 */
internal fun snapHeight(stops: SnapStops, pad: Int, max: Int): Int {
    val cut = stops.lines.firstOrNull { pad + it.bottom > max } ?: return max
    return if (cut.whole && pad + cut.top < max && pad + cut.top > 0) pad + cut.top else max
}

/** A column of lines [gap] apart that says where each line is ([onStops]), for [snapHeight]. */
@Composable
private fun SnapColumn(modifier: Modifier, gap: androidx.compose.ui.unit.Dp, onStops: (SnapStops) -> Unit, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, c ->
        val g = gap.roundToPx()
        val loose = c.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity)
        val placeables = measurables.map { it.measure(loose) }
        val lines = ArrayList<SnapLine>()
        var y = 0
        placeables.forEachIndexed { i, p ->
            if (i > 0) y += g
            lines += SnapLine(y, y + p.height, measurables[i].layoutId == WHOLE_LINE)
            y += p.height
        }
        onStops(SnapStops(lines))
        layout(c.maxWidth.takeIf { c.hasBoundedWidth } ?: (placeables.maxOfOrNull { it.width } ?: 0), y) {
            var at = 0
            placeables.forEachIndexed { i, p ->
                if (i > 0) at += g
                p.place(0, at)
                at += p.height
            }
        }
    }
}

/**
 * Tour 6, seat A c (E1): « Volume visé » in the Mode simple. The same setting as Pro's « Volume visé » (it changes
 * the project's one target), in plain words and with no LUFS: a « streaming » or a « club » file is two touches away
 * without going Pro. [pick] is the project's target; [choose] sets it (as Pro's TargetPicker does).
 */
@Composable
private fun SimpleTargetPicker(pick: com.pocketstudio.app.mixer.TargetPick, choose: (com.pocketstudio.model.TargetChoice) -> Unit, enabled: Boolean) {
    Column(Modifier.layoutId(WHOLE_LINE).testTag("share_target"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Volume visé"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // The common choices, in the same order as Pro, with plain names (« Club », « Streaming »), no LUFS.
            com.pocketstudio.app.mixer.quickTargets(pick.choice).forEach { c ->
                FilterChip(
                    selected = pick.choice == c, onClick = { if (c != pick.choice) choose(c) }, enabled = enabled,
                    label = { Text(com.pocketstudio.model.LoudnessTargets.label(c, null)) },
                    colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("share_target_${c.name}"),
                )
            }
        }
    }
}

/** One check: what is wrong in plain words, then its one-touch fix. */
@Composable
private fun CheckRow(check: ShareCheck, ui: ShareSheetUi, actions: ShareSheetActions, simple: Boolean) {
    val icon = if (check.warning) Icons.Filled.Warning else Icons.Filled.Info
    val color = if (check.warning) Neon.Amber else Neon.TextSecondary
    when (check) {
        is ShareCheck.Solo -> {
            CheckLine(icon, color, tr("Solo actif sur %s : les autres pistes ne s'entendront pas.", check.names.joinToString(", ")), "share_check_solo")
            Fixes { FixButton(tr("Tout réactiver"), "share_fix_solo", onClick = { actions.unsolo(); if (check.stems.isNotEmpty()) actions.unsoloStems(check.stems) }) }
        }
        is ShareCheck.Muted -> {
            val names = check.names.joinToString(", ")
            CheckLine(
                icon, color,
                if (simple) tr("Coupé dans Mes pistes, donc absent du fichier : %s.", names) else tr("Coupé (muet), donc absent du fichier : %s.", names),
                "share_check_muted",
            )
            if (check.chs.isNotEmpty() || check.stems.isNotEmpty()) Fixes {
                FixButton(tr("Tout réentendre"), "share_fix_muted", onClick = { actions.unmute(check.chs); if (check.stems.isNotEmpty()) actions.unmuteStems(check.stems) })
            }
        }
        ShareCheck.Empty ->
            CheckLine(icon, color, tr("Rien à entendre pour l'instant : pose un rythme ou quelques notes d'abord."), "share_check_empty")
        is ShareCheck.NoSong -> {
            CheckLine(icon, color, trCount(check.bars, "Pas encore de morceau : ta partie part en boucle (1 mesure).", "Pas encore de morceau : ta partie part en boucle (%d mesures)."), "share_check_no_song")
            Fixes { FixButton(tr("Modifier le morceau"), "share_fix_structure", actions.toStructure) }
        }
        is ShareCheck.Short -> {
            CheckLine(icon, color, tr("Une seule partie de %s : ajoute un couplet, un refrain, une fin…", shareClock(check.seconds)), "share_check_short")
            Fixes { FixButton(tr("Modifier le morceau"), "share_fix_structure", actions.toStructure) }
        }
        is ShareCheck.RemixOverrun -> {
            CheckLine(icon, color, tr("La chanson dépasse la fin du morceau de %s : sa fin sera coupée.", shareClock(check.seconds)), "share_check_remix_overrun")
            Fixes {
                FixButton(tr("Allonger le morceau"), "share_fix_remix_longer", actions.lengthenForRemix)
                if (check.canCut) FixButton(tr("Couper la chanson à la fin"), "share_fix_remix_cut", actions.cutRemix, outlined = true)
            }
        }
        ShareCheck.Saturation -> {
            CheckLine(icon, color, tr("Le son risque de saturer : il est trop fort pour le fichier."), "share_check_saturation")
            Fixes {
                if (ui.canLower) FixButton(tr("Baisser le volume général"), "share_fix_master", actions.lowerMaster)
                actions.toMixer?.let { FixButton(tr("Voir dans Mix"), "share_fix_mixer", it, outlined = true) }
            }
        }
    }
}

@Composable
private fun CheckLine(icon: ImageVector, color: Color, text: String, tag: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (color == Neon.TextSecondary) Neon.TextPrimary else color, modifier = Modifier.weight(1f))
    }
}

/** The fixes under a check, lined up with its text (past the icon). */
@Composable
private fun Fixes(content: @Composable () -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(start = 30.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
private fun FixButton(text: String, tag: String, onClick: () -> Unit, outlined: Boolean = false) {
    if (outlined) {
        OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) { Text(text, maxLines = 1) }
    } else {
        // Audit stage 5 (one main action per screen): a fix is outlined in the accent, never filled
        // like « Partager » (the tonal button wore the same orange: three orange buttons, which first?).
        OutlinedButton(
            onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Neon.Cyan),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Neon.Cyan),
        ) { Text(text, maxLines = 1) }
    }
}

/**
 * « Prêt à partager ? » for the open project: the checks read from the project and, while the
 * sheet is open, from the master's meters (the Mixeur's « travaille fort » watch, the loudness
 * meter's LUFS and true peak: [shareLevels]); « Partager »
 * renames the project when the name changed, closes the sheet and exports in the format chosen
 * ([ShareSongProgress] shows the progress, then Android's share sheet). [toStructure],
 * [toMixer] and [otherExports] go where the fixes are made (the sheet closes first).
 */
@Composable
fun ShareSheetRoute(
    rhythmVm: RhythmViewModel,
    prefs: SharePrefs,
    close: () -> Unit,
    toStructure: () -> Unit,
    /** « Voir dans Mix » (null: not offered, the Mode simple has no Mix). */
    toMixer: (() -> Unit)?,
    otherExports: () -> Unit,
    video: () -> Unit = {},
    /** Phase 36: the Mode simple's « Envoyer » tab, a page rather than a sheet ([close] is not called). */
    page: Boolean = false,
    /** The Mode simple's words and fixes (the « Envoyer » page always is). */
    simple: Boolean = page,
    /** Audit ultra round 3 (n° 13): « Envoyer le projet » (.zip, to go on elsewhere); null: not offered. */
    sendProject: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val project by rhythmVm.project.collectAsStateWithLifecycle()
    val transport by rhythmVm.transport.collectAsStateWithLifecycle()
    val export by rhythmVm.export.collectAsStateWithLifecycle()
    val meters = rhythmVm.meters.collectAsStateWithLifecycle()
    val loudness by rhythmVm.loudness.collectAsStateWithLifecycle()
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(rhythmVm) { lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) { rhythmVm.pollMeters() } }
    // The Mixeur's own watch on the limiter; once it fired, the line stays until fixed.
    val watch = remember { HardLimiting() }
    var hard by remember { mutableStateOf(false) }
    LaunchedEffect(watch) {
        var now = 0L
        while (true) {
            if (watch.update(meters.value.getOrElse(LIMITER_METER) { 1f }, now)) hard = true
            kotlinx.coroutines.delay(HardLimiting.READ_MS)
            now += HardLimiting.READ_MS
        }
    }
    // Audit final: only a name typed here is kept (per project); otherwise the field follows the
    // project's own name (another project opened, a rename elsewhere), never a stale one.
    var edited by rememberSaveable(rhythmVm.projectId) { mutableStateOf<String?>(null) }
    val name = edited ?: initialShareName(project.name)
    // T5-8MES: the length Mix shows and sends (the default loop until one is picked), not a second count of its own.
    val loopBars = export.bars
    val levels = shareLevels(project, loudness, hard)
    val checks = remember(project, levels, loopBars) { shareChecks(project, com.pocketstudio.app.ui.mixerStrips(project), levels, loopBars) }
    val ui = (
        ShareSheetUi(
            name = name, seconds = shareSeconds(project, loopBars), parts = project.song.size, checks = checks, projectName = project.name, rightsProject = project,
            format = prefs.format, playing = transport.playing, running = export.running, canLower = levels.canLower, lufs = levels.lufs,
            truePeakDb = levels.truePeakDb, lossDb = levels.lossDb, exportLufs = shareExportLufs(project),
            // Tour 6, seat A c (E1): the target is offered in the Mode simple too (plain words, SimpleTargetPicker), so
            // a « streaming » or a « club » file is two touches away without slipping into Pro.
            target = com.pocketstudio.app.mixer.TargetPick.of(project),
            loop = if (project.song.isEmpty()) LoopPick.of(export.bars, export.sceneBars, export.audioBars) else null,
        )
    )
    val actions = (
        ShareSheetActions(
            setName = { edited = it.take(ProjectCatalog.MAX_NAME) },
            setFormat = prefs::choose,
            share = {
                if (export.running) {
                    Toast.makeText(context, tr("Un export est déjà en cours : attends qu'il finisse."), Toast.LENGTH_SHORT).show()
                } else {
                    val clean = name.trim()
                    if (shareRenamesProject(project.name, clean)) rhythmVm.renameProject(clean)
                    edited = null
                    if (!page) close()
                    rhythmVm.shareSong(prefs.format, title = clean)
                }
            },
            listen = { if (project.song.isNotEmpty()) rhythmVm.toggleSong() else rhythmVm.togglePlay() },
            unsolo = rhythmVm::clearSolos,
            lowerMaster = {
                rhythmVm.setMasterDb(project.mixer.masterDb - Mixer.HINT_STEP_DB)
                watch.reset()
                hard = false
            },
            toMixer = toMixer?.let { go -> { if (!page) close(); go() } },
            unmute = rhythmVm::unmuteChannels,
            unmuteStems = { stems -> stems.forEach { if (project.remix.stems.getOrNull(it)?.mute == true) rhythmVm.toggleStemMute(it) } },
            unsoloStems = { stems -> stems.forEach { if (project.remix.stems.getOrNull(it)?.solo == true) rhythmVm.toggleStemSolo(it) } },
            toStructure = { if (!page) close(); toStructure() },
            otherExports = { if (!page) close(); otherExports() },
            video = {
                val clean = name.trim()
                if (shareRenamesProject(project.name, clean)) rhythmVm.renameProject(clean)
                edited = null
                if (!page) close()
                video()
            },
            close = close,
            chooseTarget = { rhythmVm.chooseLoudnessTarget(it) },
            sendProject = sendProject,
            setLoopBars = rhythmVm::setExportBars,
            lengthenForRemix = rhythmVm::lengthenSongForRemix,
            cutRemix = rhythmVm::cutRemixAtSongEnd,
        )
    )
    if (page) ShareSheetContent(ui, actions, page = true, simple = simple) else ShareSheet(ui, actions, simple)
}
