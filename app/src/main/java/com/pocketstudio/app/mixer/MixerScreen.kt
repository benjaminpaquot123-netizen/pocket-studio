@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.HelpDot
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.app.rhythm.ExportWhat
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.ExportFade
import com.pocketstudio.app.export.Loudness
import com.pocketstudio.model.Buses
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.pocketstudio.ui.LevelMeter
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.log10
import kotlin.math.roundToInt
import com.pocketstudio.model.i18n.tr

/** A strip on screen: its mixer channel, its name and, for an instrument track, its colour. */
data class StripInfo(val ch: Int, val name: String, val color: androidx.compose.ui.graphics.Color? = null)

/** The mixer channel index of the master (inserts, automation, meters). */
const val MASTER_CHANNEL = 4

/** Drums, acid, the first instrument track, the remix: a project with no added track (names shown through tr()). */
val BASIC_STRIPS = listOf(StripInfo(0, "Batterie"), StripInfo(1, "Basse acide"), StripInfo(2, "Synthé"), StripInfo(3, "Remix"))

/** Index of the limiter gain in the meters (after one peak per mixer channel). */
const val LIMITER_METER = com.pocketstudio.engine.NativeEngine.MIXER_CHANNELS

data class MixerActions(
    val setVolume: (Int, Float) -> Unit = { _, _ -> },
    val setPan: (Int, Float) -> Unit = { _, _ -> },
    val toggleMute: (Int) -> Unit = {},
    val toggleSolo: (Int) -> Unit = {},
    val setExportBars: (Int) -> Unit = {},
    val export: () -> Unit = {},
    val setExportWhat: (ExportWhat) -> Unit = {},
    val setExportFormat: (ExportFormat) -> Unit = {},
    val exportMidi: () -> Unit = {},
    val cancelExport: () -> Unit = {},
    val share: () -> Unit = {},
    val setExportFade: (ExportFade) -> Unit = {},
    /** « Sans limiteur (pour le mastering) » for the mix. */
    val setExportNoLimiter: (Boolean) -> Unit = {},
    /** Separate tracks through the master's effects and limiter. */
    val setStemsWithMaster: (Boolean) -> Unit = {},
    /** The master's gain before the limiter, in dB. */
    val setMasterDb: (Float) -> Unit = {},
    /** « Partager mon morceau »: the whole song in M4A, then the share sheet. */
    val shareSong: () -> Unit = {},
    val setInsertType: (ch: Int, slot: Int, type: EffectType) -> Unit = { _, _, _ -> },
    val setInsertParam: (ch: Int, slot: Int, param: Int, value: Float) -> Unit = { _, _, _, _ -> },
    val setSend: (ch: Int, bus: Int, level: Float) -> Unit = { _, _, _ -> },
    val setBusParam: (bus: Int, param: Int, value: Float) -> Unit = { _, _, _ -> },
    val quick: QuickFxActions = QuickFxActions(),
    /** An effect kept with its settings but not heard (true), or heard again. */
    val setInsertBypass: (ch: Int, slot: Int, bypass: Boolean) -> Unit = { _, _, _ -> },
    /** Two slots of a strip swapped (an effect moved up or down the chain). */
    val moveInsert: (ch: Int, from: Int, to: Int) -> Unit = { _, _, _ -> },
    /** What sounds on a strip (its suggested ready-made effects come first); null: not known. */
    val kindOf: (ch: Int) -> com.pocketstudio.model.SoundKind? = { null },
    /** How many effects of this long kind (echo, reverb, tape stop) the project already plays. */
    val longFxCount: (EffectType) -> Int = { 0 },
    /** The audio thread has almost no time to spare: the box warns (see [cpuBusyNext]). */
    val cpuBusy: kotlinx.coroutines.flow.StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(false),
    /** « Joue dans »: an effect plays only in some scenes (null: in all of them). */
    val setInsertScenes: (ch: Int, slot: Int, scenes: Set<Int>?) -> Unit = { _, _, _ -> },
    /** The project's scenes, by index: each one's section name ("" for its letter alone). */
    val scenes: kotlinx.coroutines.flow.StateFlow<List<String>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList()),
)

val EXPORT_BARS = listOf(1, 2, 4, 8, 16)

/** Fader position (0.8 = unity, quadratic law, as in the engine) shown in dB. */
fun faderDb(v: Float): String {
    if (v <= 0.001f) return "−∞ dB"
    val gain = (v / 0.8f) * (v / 0.8f)
    val db = 20 * log10(gain)
    return (if (db >= 0) "+" else "") + String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f dB", db)
}

@Composable
fun MixerScreen(
    channels: List<Channel>,
    meters: FloatArray,
    actions: MixerActions,
    modifier: Modifier = Modifier,
    export: ExportUi = ExportUi(),
    master: List<Insert> = List(com.pocketstudio.model.Mixer.INSERTS) { Insert() },
    /** The master's gain before the limiter, in dB. */
    masterDb: Float = 0f,
    buses: List<List<Float>> = Buses.DEFAULTS,
    automation: AutomationUi = AutomationUi(),
    automationActions: AutomationActions = AutomationActions(),
    hasSong: Boolean = false,
    strips: List<StripInfo> = BASIC_STRIPS,
    /** The output spectrum while the analyser is open (null: closed or not yet measured). */
    spectrum: FloatArray? = null,
    analyserOpen: Boolean = false,
    setAnalyser: (Boolean) -> Unit = {},
) {
    val exportCard = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // On a tablet the channel strips stay on the left; the master, buses, automation and export on the right.
    com.pocketstudio.ui.AdaptivePanes(modifier.testTag("mixer"), first = {
        // Sharing first: one touch, nothing to scroll for; the export card's choices are a touch away.
        ShareSongCard(export, hasSong, actions.shareSong) { scope.launch { exportCard.bringIntoView() } }
        // Many tracks: each strip shows its volume on one line; ▾ opens pan and effects.
        val compact = strips.size > BASIC_STRIPS.size
        strips.forEach { st ->
            Strip(st, channels.getOrElse(st.ch) { Channel() }, meters.getOrElse(st.ch) { 0f }, actions, compact)
        }
    }, second = {
        NeonCard(accent = Neon.Cyan) {
            SectionLabel(tr("Sortie générale"), help = Glossary.MASTER)
            LevelMeter(meters.getOrElse(MASTER_CHANNEL) { 0f } / 0.89f)
            MasterGain(masterDb, actions.setMasterDb)
            val gain = meters.getOrElse(LIMITER_METER) { 1f }
            Text(
                if (gain < 0.98f) tr("Le limiteur protège la sortie (−%.1f dB)", -20 * log10(gain))
                else tr("Pas de saturation : tout va bien."),
                style = MaterialTheme.typography.bodySmall,
                color = if (gain < 0.7f) Neon.Amber else Neon.TextSecondary,
            )
            var open by rememberSaveable { mutableStateOf(false) }
            EffectsToggle(effectsSummary(master), open, Modifier.testTag("master_effects")) { open = !open }
            if (open) InsertChain(MASTER_CHANNEL, master, actions)
            FilterChip(selected = analyserOpen, onClick = { setAnalyser(!analyserOpen) }, label = { Text(tr("Analyseur de spectre")) },
                modifier = Modifier.testTag("analyser"))
            if (analyserOpen) com.pocketstudio.app.analysis.SpectrumView(spectrum)
        }
        BusesCard(buses, actions)
        AutomationCard(automation, channels, master, automationActions, strips)
        ExportCard(export, actions, hasSong, Modifier.bringIntoViewRequester(exportCard))
    })
}

/** The master's fader: a gain before the limiter, −24..+6 dB (one move gives the limiter less work). */
@Composable
private fun MasterGain(db: Float, setDb: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Volume général"), Modifier.weight(1f))
        Text(masterDbLabel(db), style = MonoStyle, color = Neon.Cyan)
    }
    ProSlider(
        value = db,
        // Snaps to 0 dB near the middle, as the pan snaps to the centre.
        onValueChange = { v -> setDb(if (kotlin.math.abs(v) < 0.4f) 0f else (v * 2).roundToInt() / 2f) },
        valueRange = com.pocketstudio.model.Mixer.MASTER_MIN_DB..com.pocketstudio.model.Mixer.MASTER_MAX_DB,
        modifier = Modifier.testTag("master_gain"),
    )
}

/** The master's gain as the screen shows it ("+1.5 dB", "0.0 dB", "−6.0 dB"). */
fun masterDbLabel(db: Float): String =
    (if (db > 0f) "+" else "") + String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f dB", db).replace('-', '−')

/** « Partager mon morceau » at the top of the mixer, and a way down to every export choice. */
@Composable
internal fun ShareSongCard(export: ExportUi, hasSong: Boolean, share: () -> Unit, toExport: () -> Unit) {
    // One compact row: the share button and, beside it, the way down to every export choice.
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = share,
            enabled = !export.running,
            modifier = Modifier.weight(1f).height(40.dp).testTag("share_song"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Color.Black),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  " + tr("Partager mon morceau"), style = MaterialTheme.typography.labelLarge, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        TextButton(onClick = toExport, modifier = Modifier.testTag("to_export")) {
            Text(tr("Autres exports"), color = Neon.Cyan, maxLines = 1)
            Icon(Icons.Filled.ExpandMore, contentDescription = tr("Autres choix d'export (WAV, pistes séparées…)"), tint = Neon.Cyan)
        }
    }
    Text(
        if (hasSong) tr("Tout le morceau, avec un fondu à la fin, en M4A léger : prêt à envoyer.")
        else tr("Pas encore de morceau : la scène en boucle (%d mesures), en M4A léger.", com.pocketstudio.app.export.ShareSong.loopBars(export.sceneBars, export.audioBars)),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2,
    )
    }
}

@Composable
private fun Strip(info: StripInfo, c: Channel, meter: Float, actions: MixerActions, compact: Boolean = false) {
    val name = com.pocketstudio.model.DefaultNames.shown(info.name)
    val index = info.ch
    var expanded by rememberSaveable(index) { mutableStateOf(false) }
    var open by rememberSaveable(index) { mutableStateOf(false) }
    val chipColors = pillColors()
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // The strip's colour as a lit bar, like a channel of a mixing desk.
            val tint = info.color ?: Neon.Cyan
            androidx.compose.foundation.layout.Box(
                Modifier.size(width = 4.dp, height = 22.dp)
                    .background(tint, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
            )
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            FilterChip(
                selected = c.mute,
                onClick = { actions.toggleMute(index) },
                label = { Text(tr("Muet")) },
                colors = pillColors(Neon.Red),
                modifier = Modifier.testTag("mute_$index"),
            )
            FilterChip(selected = c.solo, onClick = { actions.toggleSolo(index) }, label = { Text(tr("Solo")) }, colors = chipColors,
                modifier = Modifier.testTag("solo_$index"))
            if (index == 0) HelpDot(Glossary.MUTE_SOLO)
        }
        LevelMeter(meter / 0.89f)
        if (compact) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ProSlider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, modifier = Modifier.weight(1f).testTag("volume_$index"))
                Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan, modifier = Modifier.padding(start = 6.dp))
                // The effects in one touch, right on the strip (they used to hide two levels down).
                val active = c.inserts.count { it.effect != com.pocketstudio.model.EffectType.NONE }
                FilterChip(
                    selected = expanded && open,
                    onClick = {
                        // Lit while the effects show; a touch shows them (or folds the strip back).
                        val show = !(expanded && open)
                        expanded = show
                        open = show
                    },
                    leadingIcon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(if (active > 0) tr("Effets · %d", active) else tr("Effets"), maxLines = 1, softWrap = false) },
                    colors = pillColors(Neon.Magenta),
                    modifier = Modifier.padding(start = 6.dp).testTag("strip_more_$index"),
                )
            }
            if (!expanded) {
                val fx = androidx.compose.runtime.remember(c) { quickFxSummary(c) }
                if (c.pan != 0f || fx != tr("Aucun")) {
                    Text(tr("Effets : %s", fx), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                return@NeonCard
            }
        }
        val panSlider: @Composable () -> Unit = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Panoramique"))
                HelpDot(Glossary.PAN)
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                val pan = (c.pan * 100).roundToInt()
                Text(
                    when {
                        pan < -2 -> tr("Gauche %d", -pan)
                        pan > 2 -> tr("Droite %d", pan)
                        else -> tr("Centre")
                    },
                    style = MonoStyle, color = Neon.Cyan,
                )
            }
            ProSlider(value = c.pan, onValueChange = { v -> actions.setPan(index, if (kotlin.math.abs(v) < 0.04f) 0f else v) }, valueRange = -1f..1f)
        }
        if (compact) {
            panSlider()
        } else {
            // Volume and pan side by side when the strip is wide (tablet), one under the other on a phone.
            val volumeSlider: @Composable () -> Unit = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Volume"), Modifier.weight(1f))
                    Text(faderDb(c.volume), style = MonoStyle, color = Neon.Cyan)
                }
                ProSlider(value = c.volume, onValueChange = { actions.setVolume(index, it) }, modifier = Modifier.testTag("volume_$index"))
            }
            com.pocketstudio.ui.AdaptiveColumns(listOf(volumeSlider, panSlider), minCellWidth = 240.dp)
        }
        val summary = androidx.compose.runtime.remember(c) { quickFxSummary(c) }
        EffectsToggle(summary, open, Modifier.testTag("effects_$index")) { open = !open }
        if (open) EffectsBox(index, c, actions)
    }
}

@Composable
private fun EffectsToggle(summary: String, open: Boolean, modifier: Modifier = Modifier, toggle: () -> Unit) {
    TextButton(onClick = toggle, modifier = modifier.fillMaxWidth()) {
        Text(tr("Effets : %s", summary), Modifier.weight(1f), color = Neon.Cyan)
        Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.Cyan)
    }
}

@Composable
internal fun ExportCard(export: ExportUi, actions: MixerActions, hasSong: Boolean = true, modifier: Modifier = Modifier) {
    val chipColors = pillColors(Neon.Acid)
    NeonCard(modifier, accent = Neon.Acid) {
        SectionLabel(tr("Exporter"), color = Neon.Acid)
        // The plain choices first: what, in which format, how it ends. The rest under « Avancé ».
        Text(tr("Quoi"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ExportWhat.Song to tr("Le morceau entier"), ExportWhat.Loop to tr("La scène en boucle")).forEach { (what, label) ->
                FilterChip(
                    selected = export.what == what, onClick = { actions.setExportWhat(what) }, enabled = !export.running,
                    label = { Text(label) }, colors = chipColors, modifier = Modifier.testTag("export_what_${what.name}"),
                )
            }
        }
        val bars = if (export.bars > 1) tr("%d mesures", export.bars) else tr("%d mesure", export.bars)
        Text(
            when (export.what) {
                ExportWhat.Loop -> tr("La scène en cours, jouée sur %s (la longueur se règle dans « Avancé »).", bars)
                ExportWhat.Song -> if (hasSong) tr("Toutes les parties de l'onglet Morceau, dans l'ordre.") else tr("Le morceau est vide : ajoute des scènes dans l'onglet Morceau.")
                ExportWhat.Stems ->
                    if (hasSong) tr("Un fichier par piste (batterie, acide, synthé, remix) pour les reprendre ailleurs. Les pistes vides ou coupées sont sautées. Sur tout le morceau.")
                    else tr("Un fichier par piste (batterie, acide, synthé, remix) pour les reprendre ailleurs. Les pistes vides ou coupées sont sautées. Sur %s.", bars)
            },
            style = MaterialTheme.typography.bodySmall, color = if (export.what == ExportWhat.Song && !hasSong) Neon.Amber else Neon.TextSecondary,
        )
        WithHelp(Glossary.FORMATS) { Text(tr("Format"), style = MaterialTheme.typography.bodyMedium) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ExportFormat.M4a to tr("M4A · pour partager"), ExportFormat.Wav to tr("WAV · qualité max")).forEach { (format, label) ->
                FilterChip(
                    selected = export.format == format, onClick = { actions.setExportFormat(format) }, enabled = !export.running,
                    label = { Text(label) }, colors = chipColors, modifier = Modifier.testTag("export_format_${format.name}"),
                )
            }
        }
        Text(
            when (export.format) {
                ExportFormat.Wav -> tr("Qualité studio, lu par tous les logiciels. Le plus lourd.")
                ExportFormat.Flac -> tr("FLAC : exactement la même qualité que le WAV, bien plus léger (souvent deux fois).")
                ExportFormat.M4a -> tr("Environ dix fois plus léger, lu par tous les appareils : pour partager.")
            },
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        if (export.what != ExportWhat.Stems) {
            Text(tr("Fondu de fin"), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExportFade.entries.forEach { fade ->
                    FilterChip(
                        selected = export.fade == fade, onClick = { actions.setExportFade(fade) }, enabled = !export.running,
                        label = { Text(if (fade == ExportFade.None) fade.label else "${fade.label} (${fade.seconds} s)") },
                        colors = chipColors, modifier = Modifier.testTag("export_fade_${fade.name}"),
                    )
                }
            }
        }
        // Advanced: separate tracks, FLAC, the loop's length, the notes as MIDI. Open by itself when one is in use.
        var advanced by rememberSaveable { mutableStateOf(export.what == ExportWhat.Stems || export.format == ExportFormat.Flac) }
        TextButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth().testTag("export_advanced")) {
            Text(tr("Avancé"), Modifier.weight(1f), color = Neon.Cyan)
            Icon(if (advanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.Cyan)
        }
        if (advanced) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = export.what == ExportWhat.Stems,
                    onClick = { actions.setExportWhat(if (export.what == ExportWhat.Stems) (if (hasSong) ExportWhat.Song else ExportWhat.Loop) else ExportWhat.Stems) },
                    enabled = !export.running, label = { Text(tr("Piste par piste")) }, colors = chipColors, modifier = Modifier.testTag("export_what_Stems"),
                )
                HelpDot(Glossary.STEMS)
                FilterChip(
                    selected = export.format == ExportFormat.Flac,
                    onClick = { actions.setExportFormat(if (export.format == ExportFormat.Flac) ExportFormat.Wav else ExportFormat.Flac) },
                    enabled = !export.running, label = { Text(tr("FLAC (sans perte, léger)")) }, colors = chipColors,
                    modifier = Modifier.testTag("export_format_Flac"),
                )
                // For a mixing or mastering engineer: no limiter on the mix, stems that add back up to it.
                if (export.what == ExportWhat.Stems) {
                    FilterChip(
                        selected = export.stemsWithMaster, onClick = { actions.setStemsWithMaster(!export.stemsWithMaster) },
                        enabled = !export.running, label = { Text(tr("Avec les effets de la sortie générale")) }, colors = chipColors,
                        modifier = Modifier.testTag("export_stems_master"),
                    )
                } else {
                    FilterChip(
                        selected = export.noLimiter, onClick = { actions.setExportNoLimiter(!export.noLimiter) },
                        enabled = !export.running, label = { Text(tr("Sans limiteur (pour le mastering)")) }, colors = chipColors,
                        modifier = Modifier.testTag("export_no_limiter"),
                    )
                }
            }
            val pro = when {
                // M4A and FLAC cannot hold peaks over 0 dBFS: the limiter stays rather than clip them.
                export.limiterOff && export.format != ExportFormat.Wav ->
                    tr("En M4A et en FLAC, le limiteur reste en place : sans lui, les pointes seraient écrêtées. Choisis WAV pour t'en passer.")
                export.what == ExportWhat.Stems && export.stemsWithMaster -> tr("Chaque piste passe par les effets et le limiteur de la sortie générale, comme si elle jouait seule.")
                export.what == ExportWhat.Stems -> tr("Sans les effets ni le limiteur de la sortie générale : remises ensemble, les pistes redonnent le mix.")
                export.noLimiter -> tr("Le mix garde sa marge, sans limiteur. En WAV, fichier 32 bits flottants : rien n'est écrêté.")
                else -> null
            }
            pro?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
            if (export.what == ExportWhat.Loop || (export.what == ExportWhat.Stems && !hasSong)) {
                Text(tr("Longueur de la boucle"), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (EXPORT_BARS + export.sceneBars).distinct().sorted().forEach { bars ->
                        FilterChip(
                            selected = export.bars == bars,
                            onClick = { actions.setExportBars(bars) },
                            enabled = !export.running,
                            label = { Text(if (bars == 1) tr("1 mesure") else "$bars") },
                            colors = chipColors,
                        )
                    }
                }
            }
            if (!export.running) {
                TextButton(onClick = actions.exportMidi, modifier = Modifier.fillMaxWidth().testTag("export_midi")) {
                    Text(if (hasSong) tr("Exporter les notes en fichier MIDI (tout le morceau)") else tr("Exporter les notes en fichier MIDI"))
                }
            }
        }
        when {
            export.running -> {
                export.stage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
                LinearProgressIndicator(
                    progress = { export.progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    color = Neon.Acid,
                    trackColor = Neon.SurfaceHigh,
                )
                OutlinedButton(onClick = actions.cancelExport, modifier = Modifier.fillMaxWidth()) { Text(tr("Annuler")) }
            }
            else -> {
                Button(
                    onClick = actions.export,
                    enabled = export.what != ExportWhat.Song || hasSong,
                    modifier = Modifier.fillMaxWidth().testTag("export"),
                    colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Color.Black),
                ) {
                    Text(
                        when (export.what) {
                            ExportWhat.Loop -> if (export.bars > 1) tr("Exporter %d mesures", export.bars) else tr("Exporter %d mesure", export.bars)
                            ExportWhat.Song -> tr("Exporter le morceau")
                            ExportWhat.Stems -> tr("Exporter les pistes")
                        },
                    )
                }
            }
        }
        if (export.files.isNotEmpty()) {
            Text(
                export.files.joinToString(", ") { it.name }.let { names ->
                    if (export.savedInMusic) tr("Sauvegardé dans Musique › Pocket Studio : %s", names) else tr("Prêt : %s", names)
                },
                style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary,
            )
            export.loudness?.let { lufs ->
                WithHelp(Glossary.LUFS) {
                    Text(
                        tr("Volume perçu : %s LUFS, %s", String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f", lufs).replace('-', '−'), Loudness.describe(lufs)),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f, fill = false).testTag("export_loudness"),
                    )
                }
            }
            OutlinedButton(onClick = actions.share, modifier = Modifier.fillMaxWidth().testTag("share")) {
                Text(if (export.files.size > 1) tr("Partager les %d fichiers", export.files.size) else tr("Partager"))

            }
        }
        export.error?.let { Text(it, color = Neon.Red, style = MaterialTheme.typography.bodySmall) }
    }
}
