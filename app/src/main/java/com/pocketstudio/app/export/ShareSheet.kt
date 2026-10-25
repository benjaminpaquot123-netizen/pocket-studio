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
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.learn.spot
import com.pocketstudio.model.partSteps
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
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

    /** Tracks soloed ([names]): nothing else would be heard. Fix: « Tout réactiver ». */
    data class Solo(val names: List<String>) : ShareCheck() {
        override val warning get() = true
    }

    /** Tracks muted in the mixer although they play in the song ([names]): said, not changed (it may be wanted). */
    data class Muted(val names: List<String>) : ShareCheck()

    /** Nothing to hear at all. */
    data object Empty : ShareCheck() {
        override val warning get() = true
    }

    /** No song yet: the scene is shared looped, [bars] bars long. Fix: « Modifier le morceau ». */
    data class NoSong(val bars: Int) : ShareCheck()

    /** A song of a single short part ([seconds] long). Fix: « Modifier le morceau ». */
    data class Short(val seconds: Int) : ShareCheck()

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
 * single short part, saturation. [strips]: the mixer's strips (their names); [loopBars]: the
 * length of the scene shared when there is no song.
 */
fun shareChecks(p: Project, strips: List<StripInfo>, levels: ShareLevels, loopBars: Int): List<ShareCheck> = buildList {
    val channels = p.mixer.channels
    fun name(s: StripInfo) = DefaultNames.shown(s.name)
    val soloed = strips.filter { channels.getOrNull(it.ch)?.solo == true }
    if (soloed.isNotEmpty()) add(ShareCheck.Solo(soloed.map(::name)))
    val sounding = stripSounds(p, song = p.song.isNotEmpty()).filter { it.second }.map { it.first.second }.toSet()
    val muted = strips.filter { it.ch in sounding && channels.getOrNull(it.ch)?.mute == true }
    if (muted.isNotEmpty()) add(ShareCheck.Muted(muted.map(::name)))
    when {
        sounding.isEmpty() -> add(ShareCheck.Empty)
        p.song.isEmpty() -> add(ShareCheck.NoSong(loopBars))
        p.song.size == 1 && shareSeconds(p, loopBars) < SHORT_SONG_SECONDS -> add(ShareCheck.Short(shareSeconds(p, loopBars)))
    }
    if (levels.saturates) add(ShareCheck.Saturation)
}

/** What the name field starts with: the project's own name, or a suggestion when the app gave it one. */
fun initialShareName(name: String): String =
    if (ShareSong.isDefaultName(name)) ShareSong.suggestedName(DefaultNames.shown(name)) else name.trim()

/** "2:38" from seconds. */
fun shareClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

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
    /** S5: how much the loudest moments are tassés for the volume ([ShareLevels.lossDb]). */
    val lossDb: Double? = null,
)

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
    val toMixer: () -> Unit = {},
    /** « Modifier le morceau »: the song's parts (the Morceau tab, which the button is named after). */
    val toStructure: () -> Unit = {},
    /** « Autres exports »: separate tracks, MIDI, FLAC… (Mixeur › Exporter). */
    val otherExports: () -> Unit = {},
    /** « Vidéo pour les réseaux »: 30 s of the song in 9:16 (phase 39). */
    val video: () -> Unit = {},
    val close: () -> Unit = {},
)

/** « Prêt à partager ? » as a bottom sheet above the tab it was opened from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(ui: ShareSheetUi, actions: ShareSheetActions) {
    // Opened whole: the « Partager » button is never below the fold.
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = actions.close, sheetState = state, containerColor = Neon.Surface) {
        ShareSheetContent(ui, actions)
    }
}

/**
 * The sheet's content: the name (already filled in), how long the song lasts, the checks with
 * their one-touch fixes, M4A or WAV, and one big « Partager ». The rarer exports are a link away.
 */
@Composable
fun ShareSheetContent(ui: ShareSheetUi, actions: ShareSheetActions, modifier: Modifier = Modifier, page: Boolean = false) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = if (page) 12.dp else 0.dp, bottom = 12.dp)
            .then(if (page) Modifier else Modifier.navigationBarsPadding()).testTag("share_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (page) tr("Fini ? Envoie ton morceau") else tr("Prêt à partager ?"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            // Phase 36: the Mode simple's « Envoyer » tab is this page: nothing to close.
            if (!page) IconButton(onClick = actions.close, modifier = Modifier.testTag("share_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary)
            }
        }
        // « Partager » renames the project too (a default « Mon projet 3 » becomes the name typed): the label says so.
        OutlinedTextField(
            value = ui.name, onValueChange = actions.setName, singleLine = true, label = { Text(tr("Nom du morceau (et du projet)")) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().testTag("share_name"),
        )
        // How long it lasts, and one last listen (the saturation is checked while it plays).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(shareClock(ui.seconds), style = MonoStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = Neon.Cyan,
                        modifier = Modifier.testTag("share_duration"))
                    Text(
                        "  " + if (ui.parts == 0) tr("une partie en boucle") else if (ui.parts == 1) tr("1 partie") else tr("%d parties", ui.parts),
                        style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                    )
                }
                ui.lufs?.let { lufs ->
                    // Plain words first; the units (LUFS, dBTP) under « détails », for whoever knows them.
                    var details by rememberSaveable { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tr("Volume : %s, %s", String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f", lufs).replace('-', '−'), Loudness.describe(lufs)),
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f).testTag("share_loudness"),
                        )
                        Text(
                            if (details) tr("masquer") else tr("détails"), style = MaterialTheme.typography.labelSmall, color = Neon.Cyan,
                            modifier = Modifier.clickable(role = Role.Button) { details = !details }.padding(6.dp).testTag("share_details"),
                        )
                    }
                    if (details) {
                        Text(
                            tr("%s LUFS · crête %s dBTP", LoudnessTargets.format(lufs), ui.truePeakDb?.let { LoudnessTargets.format(it) } ?: "—"),
                            style = MonoStyle.copy(fontSize = 12.sp), color = Neon.TextSecondary, modifier = Modifier.testTag("share_loudness_details"),
                        )
                    }
                    ui.lossDb?.let { com.pocketstudio.app.mixer.LossLine(it, Modifier.testTag("share_loss")) }
                }
            }
            OutlinedButton(onClick = actions.listen, modifier = Modifier.heightIn(min = 48.dp).testTag("share_listen")) {
                Icon(if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + if (ui.playing) tr("Arrêter") else tr("Écouter"), maxLines = 1)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("share_checks")) {
            if (ui.checks.isEmpty()) {
                CheckLine(Icons.Filled.CheckCircle, Neon.Acid, tr("Tout est prêt : aucun solo oublié, aucune piste coupée."), "share_check_ok")
            }
            ui.checks.forEach { check -> CheckRow(check, ui, actions) }
        }
        Text(tr("Format"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ExportFormat.M4a to tr("M4A · pour partager"), ExportFormat.Wav to tr("WAV · qualité max")).forEach { (format, label) ->
                FilterChip(
                    selected = ui.format == format, onClick = { actions.setFormat(format) }, label = { Text(label) },
                    colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("share_format_${format.name}"),
                )
            }
        }
        Button(
            onClick = actions.share, enabled = ui.name.isNotBlank() && !ui.running,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).spot("share_go", tr("Partager")).testTag("share_go"),
        ) {
            Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + tr("Partager"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = actions.video, enabled = !ui.running, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("share_video")) {
            Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  " + tr("Vidéo pour les réseaux · 30 s"), maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        TextButton(onClick = actions.otherExports, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("share_other")) {
            Column(Modifier.weight(1f)) {
                Text(tr("Autres exports"), color = Neon.Cyan)
                // Lot 2: the Mode simple has no Mix (the button takes it to the full mode).
                Text(if (page) tr("Piste par piste, MIDI, FLAC… (passe en mode complet)") else tr("Piste par piste, MIDI, FLAC… dans Mix"),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("share_other_where"))
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.Cyan)
        }
    }
}

/** One check: what is wrong in plain words, then its one-touch fix. */
@Composable
private fun CheckRow(check: ShareCheck, ui: ShareSheetUi, actions: ShareSheetActions) {
    val icon = if (check.warning) Icons.Filled.Warning else Icons.Filled.Info
    val color = if (check.warning) Neon.Amber else Neon.TextSecondary
    when (check) {
        is ShareCheck.Solo -> {
            CheckLine(icon, color, tr("Solo actif sur %s : les autres pistes ne s'entendront pas.", check.names.joinToString(", ")), "share_check_solo")
            Fixes { FixButton(tr("Tout réactiver"), "share_fix_solo", actions.unsolo) }
        }
        is ShareCheck.Muted ->
            CheckLine(icon, color, tr("Coupé dans le mixeur, donc absent du fichier : %s.", check.names.joinToString(", ")), "share_check_muted")
        ShareCheck.Empty ->
            CheckLine(icon, color, tr("Rien à entendre pour l'instant : pose un rythme ou quelques notes d'abord."), "share_check_empty")
        is ShareCheck.NoSong -> {
            CheckLine(icon, color, tr("Pas encore de morceau : ta partie part en boucle (%d mesures).", check.bars), "share_check_no_song")
            Fixes { FixButton(tr("Modifier le morceau"), "share_fix_structure", actions.toStructure) }
        }
        is ShareCheck.Short -> {
            CheckLine(icon, color, tr("Une seule partie de %s : ajoute un couplet, un refrain, une fin…", shareClock(check.seconds)), "share_check_short")
            Fixes { FixButton(tr("Modifier le morceau"), "share_fix_structure", actions.toStructure) }
        }
        ShareCheck.Saturation -> {
            CheckLine(icon, color, tr("Le son risque de saturer : il est trop fort pour le fichier."), "share_check_saturation")
            Fixes {
                if (ui.canLower) FixButton(tr("Baisser le volume général"), "share_fix_master", actions.lowerMaster)
                FixButton(tr("Voir dans Mix"), "share_fix_mixer", actions.toMixer, outlined = true)
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
        androidx.compose.material3.FilledTonalButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).testTag(tag)) { Text(text, maxLines = 1) }
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
    toMixer: () -> Unit,
    otherExports: () -> Unit,
    video: () -> Unit = {},
    /** Phase 36: the Mode simple's « Envoyer » tab, a page rather than a sheet ([close] is not called). */
    page: Boolean = false,
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
    var name by rememberSaveable { mutableStateOf(initialShareName(project.name)) }
    val loopBars = ShareSong.loopBars(export.sceneBars, export.audioBars)
    val levels = shareLevels(project, loudness, hard)
    val checks = remember(project, levels, loopBars) { shareChecks(project, com.pocketstudio.app.ui.mixerStrips(project), levels, loopBars) }
    val ui = (
        ShareSheetUi(
            name = name, seconds = shareSeconds(project, loopBars), parts = project.song.size, checks = checks,
            format = prefs.format, playing = transport.playing, running = export.running, canLower = levels.canLower, lufs = levels.lufs,
            truePeakDb = levels.truePeakDb, lossDb = levels.lossDb,
        )
    )
    val actions = (
        ShareSheetActions(
            setName = { name = it.take(ProjectCatalog.MAX_NAME) },
            setFormat = prefs::choose,
            share = {
                if (export.running) {
                    Toast.makeText(context, tr("Un export est déjà en cours : attends qu'il finisse."), Toast.LENGTH_SHORT).show()
                } else {
                    val clean = name.trim()
                    if (clean != project.name.trim()) rhythmVm.renameProject(clean)
                    if (!page) close()
                    rhythmVm.shareSong(prefs.format)
                }
            },
            listen = { if (project.song.isNotEmpty()) rhythmVm.toggleSong() else rhythmVm.togglePlay() },
            unsolo = rhythmVm::clearSolos,
            lowerMaster = {
                rhythmVm.setMasterDb(project.mixer.masterDb - Mixer.HINT_STEP_DB)
                watch.reset()
                hard = false
            },
            toMixer = { if (!page) close(); toMixer() },
            toStructure = { if (!page) close(); toStructure() },
            otherExports = { if (!page) close(); otherExports() },
            video = {
                val clean = name.trim()
                if (clean.isNotEmpty() && clean != project.name.trim()) rhythmVm.renameProject(clean)
                if (!page) close()
                video()
            },
            close = close,
        )
    )
    if (page) ShareSheetContent(ui, actions, page = true) else ShareSheet(ui, actions)
}
