@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.pocketstudio.app.mixer

import com.pocketstudio.app.learn.spot
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
import com.pocketstudio.model.Buses
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.bringIntoViewRequester
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
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
const val LIMITER_METER = com.pocketstudio.engine.NativeEngine.METER_LIMITER

/** Index of the master's peak going into the limiter in the meters. */
const val MASTER_IN_METER = com.pocketstudio.engine.NativeEngine.METER_MASTER_IN

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
    /** The master's « Cible » (loudness target). */
    val setLoudnessTarget: (com.pocketstudio.model.LoudnessTarget) -> Unit = {},
    /** Exports at 44.1 kHz (true) or 48 kHz; in 16 bits (true) or 24. */
    val setExportCdRate: (Boolean) -> Unit = {},
    val setExportBits16: (Boolean) -> Unit = {},
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
    /** Each strip's peak level (the mixer's meters, by channel): each effect's light and mini view, read when drawn. */
    val meters: kotlinx.coroutines.flow.StateFlow<FloatArray> = kotlinx.coroutines.flow.MutableStateFlow(FloatArray(0)),
    /** Keeps [meters] fresh while an effects box is shown (null: someone else does, or no engine). */
    val watchMeters: (suspend () -> Unit)? = null,
    /** Each insert's compressor gain reduction in dB (channel * 8 + slot, ≤ 0), kept fresh with [meters]. */
    val reductions: kotlinx.coroutines.flow.StateFlow<FloatArray> = kotlinx.coroutines.flow.MutableStateFlow(FloatArray(0)),
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
    /** Peaks by channel, then the limiter's gain: read only when the meters are drawn. */
    meters: () -> FloatArray,
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
    /** « Ce qui joue » (null: every track), or « Ce qui bouge sur cette piste » (a channel). */
    overview: (Int?) -> Unit = {},
    /** Where « Ce qui joue » sent the user (shown, then [onFocused]). */
    focus: Where? = null,
    onFocused: () -> Unit = {},
    /** The master's loudness and true peak (NativeEngine.LOUDNESS_*): read only when shown. */
    loudness: () -> FloatArray = { FloatArray(0) },
    /** The master's « Cible », and the loudness « Auto » aims at for this project's style. */
    target: com.pocketstudio.model.LoudnessTarget = com.pocketstudio.model.LoudnessTarget.OFF,
    styleLufs: Float = com.pocketstudio.model.LoudnessTargets.DEFAULT_LUFS,
    /** Phase 36: a strip's sheet › « Courbes »: that strip first in « Automatiser un réglage ». */
    preferChannel: (Int) -> Unit = {},
) {
    val exportCard = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val automationCard = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val masterCard = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val busesCard = androidx.compose.runtime.remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(focus) {
        when (focus) {
            Where.Master -> masterCard.bringIntoView()
            Where.Buses -> busesCard.bringIntoView()
            Where.Export -> exportCard.bringIntoView()
            // The lane itself is brought into view by the automation card.
            is Where.Lane -> automationCard.bringIntoView()
            else -> return@LaunchedEffect
        }
        kotlinx.coroutines.delay(1500)
        onFocused()
    }
    val lanesOn = androidx.compose.runtime.remember(automation.lanes) {
        automation.lanes.groupingBy { it.target.ch.takeIf { _ -> it.target.kind != com.pocketstudio.model.AutoTarget.BUS_PARAM && it.target.kind != com.pocketstudio.model.AutoTarget.LIVE } ?: -1 }.eachCount()
    }
    // On a tablet the channel strips stay on the left; the master, buses, automation and export on the right.
    com.pocketstudio.ui.AdaptivePanes(modifier.testTag("mixer"), first = {
        // Phase 36: the console first, every fader in sight; automation and « Ce qui joue » right under it.
        Console(
            strips, channels, meters, actions, lanesOn,
            openMoves = { ch -> overview(ch) },
            openCurves = { ch -> preferChannel(ch); scope.launch { automationCard.bringIntoView() } },
        )
        AutomationEntry(automation.lanes.size, show = { scope.launch { automationCard.bringIntoView() } }, overview = { overview(null) })
    }, second = {
        AutomationCard(
            automation.copy(focusLane = (focus as? Where.Lane)?.lane ?: -1), channels, master,
            automationActions.copy(overview = { overview(null) }), strips,
            Modifier.bringIntoViewRequester(automationCard).spot("mixer_automation"),
        )
        NeonCard(Modifier.bringIntoViewRequester(masterCard).spot("mixer_master"), accent = if (focus == Where.Master) Neon.Amber else Neon.Cyan) {
            SectionLabel(tr("Sortie générale"), help = Glossary.MASTER)
            // How loud it is, in a word and a number; the units are under « Détails » (MasterLoudnessPanel).
            LoudnessChip(loudness)
            // What goes into the limiter (red above −1 dBFS, where it starts to work), not what is left after it.
            MasterMeter { meters().getOrElse(MASTER_IN_METER) { 0f } }
            MasterGain(masterDb, actions.setMasterDb)
            LimiterLine { meters().getOrElse(LIMITER_METER) { 1f } }
            // With a target the limiter works on purpose: lowering the master would only raise its gain.
            if (target == com.pocketstudio.model.LoudnessTarget.OFF) LimiterHint({ meters().getOrElse(LIMITER_METER) { 1f } }, masterDb, actions.setMasterDb)
            MasterLoudnessPanel(loudness, target, styleLufs, actions.setLoudnessTarget)
            var open by rememberSaveable { mutableStateOf(false) }
            EffectsToggle(effectsSummary(master), open, Modifier.testTag("master_effects")) { open = !open }
            if (open) InsertChain(MASTER_CHANNEL, master, actions)
            FilterChip(selected = analyserOpen, onClick = { setAnalyser(!analyserOpen) }, label = { Text(tr("Analyseur de spectre")) },
                modifier = Modifier.testTag("analyser"))
            if (analyserOpen) com.pocketstudio.app.analysis.SpectrumView(spectrum)
        }
        androidx.compose.foundation.layout.Box(Modifier.bringIntoViewRequester(busesCard)) { BusesCard(buses, actions) }
        ExportCard(export, actions, hasSong, Modifier.bringIntoViewRequester(exportCard))
    })
}

/** Automation near the top of the mixer: how many curves move, a touch to them, a touch to « Ce qui joue ». */
@Composable
private fun AutomationEntry(lanes: Int, show: () -> Unit, overview: () -> Unit) {
    NeonCard(Modifier.testTag("automation_entry"), accent = Neon.Amber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = Neon.Amber)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(tr("Automation et effets"), style = MaterialTheme.typography.titleMedium)
                Text(
                    when (lanes) {
                        0 -> tr("Rien ne bouge encore : fais s'ouvrir un filtre, monter un volume…")
                        1 -> tr("1 réglage bouge tout seul.")
                        else -> tr("%d réglages bougent tout seuls.", lanes)
                    },
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.Button(
                onClick = show, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("automation_entry_open"),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Neon.Amber, contentColor = Neon.OnAccent),
            ) { Text(if (lanes == 0) tr("Automatiser") else tr("Voir les courbes"), maxLines = 1) }
            androidx.compose.material3.OutlinedButton(onClick = overview, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("automation_entry_overview")) {
                Text(tr("Ce qui joue"), maxLines = 1)
            }
        }
    }
}

/**
 * The limiter's gain as read from the engine, made safe: 0 (nothing read yet, the engine closed)
 * or a value that is not a number is « no limiting » (1), never « 214748368,0 dB » (lot 2).
 */
internal fun limiterGainRead(g: Float): Float = if (g.isFinite() && g > 0f) g.coerceAtMost(1f) else 1f

/** Tenths of a dB the limiter holds the sound back, and whether it works hard: what its line shows. */
internal fun limiterShown(raw: Float): Pair<Int, Boolean> {
    val g = limiterGainRead(raw)
    return (if (g < 0.98f) (-200 * log10(g)).roundToInt() else 0) to (g < 0.7f)
}

/** What the limiter does: composed again only when its words change, not at each meter reading. */
@Composable
private fun LimiterLine(gain: () -> Float) {
    val shown by androidx.compose.runtime.remember(gain) {
        androidx.compose.runtime.derivedStateOf { limiterShown(gain()) }
    }
    val (tenths, hard) = shown
    Text(
        if (tenths > 0) tr("La sortie est protégée : le son est retenu de %.1f dB.", tenths / 10f)
        else tr("Pas de saturation : tout va bien."),
        style = MaterialTheme.typography.bodySmall,
        color = if (hard) Neon.Amber else Neon.TextSecondary,
        modifier = Modifier.testTag("limiter_line"),
    )
}

/**
 * The master's level going into the limiter, in dB (−40..+6 dBFS): red from −1 dBFS, the
 * limiter's ceiling, where it takes the peaks down. Read only when drawn.
 */
@Composable
private fun MasterMeter(peak: () -> Float) {
    val off = Neon.SurfaceHigh
    val low = Neon.Acid
    val mid = Neon.Amber
    val high = Neon.Red
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(12.dp).testTag("master_meter")) {
        val segments = 32
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (segments - 1)) / segments
        val lit = masterMeterFraction(peak()) * segments
        val radius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
        for (i in 0 until segments) {
            val x = i * (w + gap)
            val db = METER_MIN_DB + (METER_MAX_DB - METER_MIN_DB) * i / segments
            val color = when {
                db >= METER_RED_DB -> high
                db >= METER_RED_DB - 9f -> mid
                else -> low
            }
            val on = (lit - i).coerceIn(0f, 1f)
            // The red zone is always shown dimly: where the level must not stay.
            drawRoundRect(color = if (db >= METER_RED_DB) high.copy(alpha = 0.18f) else off,
                topLeft = androidx.compose.ui.geometry.Offset(x, 0f), size = androidx.compose.ui.geometry.Size(w, size.height), cornerRadius = radius)
            if (on > 0f) drawRoundRect(color = color.copy(alpha = 0.35f + 0.65f * on),
                topLeft = androidx.compose.ui.geometry.Offset(x, 0f), size = androidx.compose.ui.geometry.Size(w, size.height), cornerRadius = radius)
        }
    }
}

/** The master meter's scale: from silence (−40 dBFS) to +6 dBFS; red from the limiter's ceiling. */
private const val METER_MIN_DB = -40f
private const val METER_MAX_DB = 6f
internal const val METER_RED_DB = -1f

/** How much of the master meter a peak (linear, 1 = 0 dBFS) lights, 0..1. */
fun masterMeterFraction(peak: Float): Float {
    if (!(peak > 0f)) return 0f
    val db = 20f * log10(peak)
    return ((db - METER_MIN_DB) / (METER_MAX_DB - METER_MIN_DB)).coerceIn(0f, 1f)
}

/**
 * « Le limiteur travaille fort » : after two seconds of the limiter taking more than 6 dB off,
 * one touch lowers the master by 6 dB (one undo step). Polled from the meters, not composed with them.
 */
@Composable
private fun LimiterHint(gain: () -> Float, masterDb: Float, setDb: (Float) -> Unit) {
    val watch = androidx.compose.runtime.remember { HardLimiting() }
    var shown by androidx.compose.runtime.remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(watch) {
        var now = 0L
        while (true) {
            shown = watch.update(gain(), now)
            kotlinx.coroutines.delay(HardLimiting.READ_MS)
            now += HardLimiting.READ_MS
        }
    }
    val lowered = masterDb - com.pocketstudio.model.Mixer.HINT_STEP_DB
    if (!shown || lowered < com.pocketstudio.model.Mixer.MASTER_MIN_DB) return
    Row(
        Modifier.fillMaxWidth()
            .background(Neon.Amber.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(start = 10.dp)
            .testTag("limiter_hint"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tr("Le son risque de saturer : baisser le volume général de 6 dB ?"),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = {
                setDb(lowered)
                watch.reset()
                shown = false
            },
            modifier = Modifier.heightIn(min = 48.dp).testTag("limiter_hint_lower"),
        ) { Text(tr("Baisser de 6 dB"), color = Neon.Amber) }
    }
}

/**
 * Whether the limiter works hard: readings of its gain (one every [READ_MS]) under [HARD_GAIN]
 * (more than 6 dB off) for [HOLD_MS], a gap of at most [GAP_MS] between two (between the beats).
 * Once true, it stays so until [CALM_MS] without one.
 */
class HardLimiting {
    private var runStart = -1L
    private var lastHard = -1L
    private var shown = false

    fun update(gain: Float, nowMs: Long): Boolean {
        // Nothing read yet (0) is not a limiter holding everything back.
        if (limiterGainRead(gain) < HARD_GAIN) {
            if (runStart < 0 || nowMs - lastHard > GAP_MS) runStart = nowMs
            lastHard = nowMs
        }
        val running = runStart >= 0 && nowMs - lastHard <= GAP_MS
        if (!running) runStart = -1
        shown = (shown && lastHard >= 0 && nowMs - lastHard <= CALM_MS) || (running && nowMs - runStart >= HOLD_MS)
        return shown
    }

    /** The master just went down: watched again from scratch. */
    fun reset() {
        runStart = -1
        lastHard = -1
        shown = false
    }

    companion object {
        /** −6 dB. */
        const val HARD_GAIN = 0.5f
        const val HOLD_MS = 2000L
        const val GAP_MS = 1000L
        const val CALM_MS = 4000L
        const val READ_MS = 100L
    }
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
            listOf(ExportWhat.Song to tr("Le morceau entier"), ExportWhat.Loop to tr("La partie en boucle")).forEach { (what, label) ->
                FilterChip(
                    selected = export.what == what, onClick = { actions.setExportWhat(what) }, enabled = !export.running,
                    label = { Text(label) }, colors = chipColors, modifier = Modifier.testTag("export_what_${what.name}"),
                )
            }
        }
        val bars = if (export.bars > 1) tr("%d mesures", export.bars) else tr("%d mesure", export.bars)
        Text(
            when (export.what) {
                ExportWhat.Loop -> tr("La partie en cours, jouée sur %s (la longueur se règle dans « Avancé »).", bars)
                ExportWhat.Song -> if (hasSong) tr("Toutes les parties de Structure, dans l'ordre.") else tr("Le morceau est vide : ajoute des parties dans Structure.")
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
                // For a CD or a distributor that asks for it: 44.1 kHz, 16 bits (dithered).
                FilterChip(
                    selected = export.cdRate, onClick = { actions.setExportCdRate(!export.cdRate) }, enabled = !export.running,
                    label = { Text(tr("44,1 kHz")) }, colors = chipColors, modifier = Modifier.testTag("export_cd_rate"),
                )
                if (export.format != ExportFormat.M4a && !(export.limiterOff && export.format == ExportFormat.Wav)) {
                    FilterChip(
                        selected = export.bits16, onClick = { actions.setExportBits16(!export.bits16) }, enabled = !export.running,
                        label = { Text(tr("16 bits")) }, colors = chipColors, modifier = Modifier.testTag("export_bits16"),
                    )
                }
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
            ExportReportLine(export)
            OutlinedButton(onClick = actions.share, modifier = Modifier.fillMaxWidth().testTag("share")) {
                Text(if (export.files.size > 1) tr("Partager les %d fichiers", export.files.size) else tr("Partager"))

            }
        }
        export.error?.let { Text(it, color = Neon.RedText, style = MaterialTheme.typography.bodySmall) }
    }
}

/** How many times a mixer strip was composed (tests: a meter reading must not compose it again). */
@androidx.annotation.VisibleForTesting
internal var stripCompositions = 0

/**
 * One meter reading: each peak held with a gentle fall (readable bars), the limiter's gain (at
 * [LIMITER_METER]) taken as is. A fallen peak under [METER_FLOOR] is silence. True when
 * something moved, so a silent, stopped mixer publishes nothing.
 */
fun holdMeterPeaks(peaks: FloatArray, fresh: FloatArray): Boolean {
    var changed = false
    for (i in peaks.indices) {
        val v = if (i == LIMITER_METER) fresh[i] else maxOf(fresh[i], peaks[i] * 0.85f).let { if (it < METER_FLOOR) 0f else it }
        if (v != peaks[i]) {
            peaks[i] = v
            changed = true
        }
    }
    return changed
}

/** Below this a meter shows nothing at all (−60 dB). */
private const val METER_FLOOR = 0.001f
