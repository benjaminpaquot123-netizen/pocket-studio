package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.export.Loudness
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.app.rhythm.ExportUi
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.LoudnessTarget
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt

/** A loudness or a true peak for the screen: "−8,1", or "—" before there is one. */
internal fun loudnessText(value: Float): String =
    if (value <= NativeEngine.LOUDNESS_NONE + 1f || !value.isFinite()) "—" else LoudnessTargets.format(value.toDouble())

/** « Volume −8,1 · fort, comme en club »: the master's short-term loudness, in a number and in words (the units are under « Détails »). */
internal fun loudnessChipText(values: FloatArray): String {
    val lufs = values.getOrElse(NativeEngine.LOUDNESS_SHORT_TERM) { NativeEngine.LOUDNESS_NONE }
    val text = loudnessText(lufs)
    return if (text == "—") tr("Volume : —") else tr("Volume %1\$s · %2\$s", text, Loudness.describe(lufs.toDouble()))
}

/** « −8,1 LUFS · TP −1,2 »: the master's short-term loudness and its true peak since ▶, for whoever knows the units. */
internal fun loudnessDetailsText(values: FloatArray): String =
    tr("%s LUFS · TP %s", loudnessText(values.getOrElse(NativeEngine.LOUDNESS_SHORT_TERM) { NativeEngine.LOUDNESS_NONE }),
        loudnessText(values.getOrElse(NativeEngine.LOUDNESS_TRUE_PEAK) { NativeEngine.LOUDNESS_NONE }))

/**
 * Holds the highest of the last [readings] values given: the true peak of the last 50 ms
 * ([NativeEngine.LOUDNESS_RECENT_PEAK]) kept a second, so that a peak light stays lit long enough to be seen.
 */
class PeakHold(private val readings: Int = 20) {
    private var held = NativeEngine.LOUDNESS_NONE
    private var age = 0

    fun update(fresh: Float): Float {
        if (fresh >= held || ++age >= readings) {
            held = fresh
            age = 0
        }
        return held
    }
}

/**
 * The compact reading near the master: short-term loudness and the true peak since ▶; amber
 * while a peak went over −1 dBTP in the last second ([NativeEngine.LOUDNESS_RECENT_PEAK], held by
 * [PeakHold] in the ViewModel): a clip light. [values] is read when the text changes, not at each reading.
 */
@Composable
internal fun LoudnessChip(values: () -> FloatArray, modifier: Modifier = Modifier) {
    val text by remember(values) { derivedStateOf { loudnessChipText(values()) } }
    val hot by remember(values) { derivedStateOf { values().getOrElse(NativeEngine.LOUDNESS_RECENT_PEAK) { -100f } > -1f } }
    Text(text, style = MonoStyle, color = if (hot) Neon.Amber else Neon.Cyan, modifier = modifier.testTag("loudness_chip"))
}

/** What [target] aims at in a project whose « Auto » is [styleLufs] (« Club »: never under it). */
internal fun shownLufs(target: LoudnessTarget, styleLufs: Float): Float? = when (target) {
    LoudnessTarget.AUTO -> styleLufs
    LoudnessTarget.CLUB -> LoudnessTargets.clubLufs(styleLufs)
    else -> LoudnessTargets.lufs(target, null)
}

/**
 * The master's loudness in full (now, over 3 s, since ▶) and its « Cible »: the style's loudness,
 * a club's, streaming's, or none. [styleLufs]: what « Auto » aims at for this project.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MasterLoudnessPanel(values: () -> FloatArray, target: LoudnessTarget, styleLufs: Float, setTarget: (LoudnessTarget) -> Unit) {
    val readings by remember(values) {
        derivedStateOf {
            val v = values()
            val drive = v.getOrElse(NativeEngine.LOUDNESS_DRIVE) { 0f }
            val loss = v.getOrElse(NativeEngine.LOUDNESS_LOSS) { 0f }.takeIf { it.isFinite() } ?: 0f
            Triple(
                tr(
                    "Maintenant %s · sur 3 s %s · depuis ▶ %s LUFS",
                    loudnessText(v.getOrElse(NativeEngine.LOUDNESS_MOMENTARY) { -100f }),
                    loudnessText(v.getOrElse(NativeEngine.LOUDNESS_SHORT_TERM) { -100f }),
                    loudnessText(v.getOrElse(NativeEngine.LOUDNESS_INTEGRATED) { -100f }),
                ),
                (drive * 10f).roundToInt() / 10f,
                (loss * 10f).roundToInt() / 10f,
            )
        }
    }
    val (line, drive, loss) = readings
    // The units (LUFS, dBTP) and the three readings under « Détails »: a beginner reads the words above.
    var details by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { details = !details }, modifier = Modifier.testTag("loudness_details")) {
        Text(if (details) tr("Masquer les détails") else tr("Détails"), style = MaterialTheme.typography.labelMedium)
    }
    // Testers (lot « tuto 5 »): opened at the screen's bottom, the details were under the tutorial's panel: brought into view.
    val detailsInView = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    androidx.compose.runtime.LaunchedEffect(details) { if (details) { kotlinx.coroutines.delay(60); detailsInView.bringIntoView() } }
    if (details) {
        val detail by remember(values) { derivedStateOf { loudnessDetailsText(values()) } }
        androidx.compose.foundation.layout.Column(Modifier.bringIntoViewRequester(detailsInView)) {
            Text(detail, style = MonoStyle, color = Neon.TextSecondary, modifier = Modifier.testTag("loudness_chip_details"))
            WithHelp(Glossary.LUFS) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("loudness_readings"))
            }
        }
    }
    Text(tr("Volume visé"), style = MaterialTheme.typography.bodyMedium)
    val colors = pillColors(Neon.Cyan)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(LoudnessTarget.AUTO, LoudnessTarget.CLUB, LoudnessTarget.STREAMING, LoudnessTarget.OFF).forEach { t ->
            val lufs = shownLufs(t, styleLufs)
            FilterChip(
                selected = target == t, onClick = { setTarget(t) },
                label = { Text(if (lufs == null) t.label else "${t.label} · ${LoudnessTargets.formatTarget(lufs)}") },
                colors = colors, modifier = Modifier.testTag("loudness_target_${t.name}"),
            )
        }
    }
    val lufs = LoudnessTargets.formatTarget(shownLufs(target, styleLufs) ?: 0f)
    Text(
        when (target) {
            LoudnessTarget.AUTO -> tr("Le volume des morceaux de ce style (%s). La sortie monte ou baisse doucement pour y arriver ; l'export y est exactement.", lufs)
            LoudnessTarget.CLUB -> tr("Fort, comme en club (%s). La sortie monte doucement pour y arriver ; l'export y est exactement.", lufs)
            LoudnessTarget.STREAMING -> tr("%s : le volume où Spotify et YouTube ramènent tous les morceaux.", lufs)
            LoudnessTarget.OFF -> tr("Le volume reste celui que tu règles ; la sortie est seulement protégée contre la saturation.")
        },
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    // No reading yet (the engine not playing, a test without one) reads −100: never « −100 dB », as if the song were silenced.
    if (target != LoudnessTarget.OFF && drive != 0f && drive.isFinite() && kotlin.math.abs(drive) < 40f) {
        Text(
            tr("Gain ajouté pour la cible : %s dB", (if (drive > 0f) "+" else "") + LoudnessTargets.format(drive.toDouble())),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("loudness_drive"),
        )
    }
    if (target != LoudnessTarget.OFF && loss >= 0.1f) LossLine(loss.toDouble(), Modifier.testTag("loudness_loss"))
}

/**
 * S5, in plain words: what the master chain takes off the loudest moments to reach the volume
 * asked (the clipper and the limiter's loss, in dB); amber past 3, where it starts to flatten the Drop.
 */
@Composable
internal fun LossLine(lossDb: Double, modifier: Modifier = Modifier) {
    Text(
        lossText(lossDb),
        style = MaterialTheme.typography.bodySmall, color = if (lossDb > 3.05) Neon.Amber else Neon.TextSecondary, modifier = modifier,
    )
}

/** « Pour sonner aussi fort, les crêtes sont tassées de 2,1 dB (3 au plus, le Drop garde son punch). » */
internal fun lossText(lossDb: Double): String =
    if (lossDb > 3.05) tr("Pour sonner aussi fort, les crêtes sont tassées de %s dB : c'est beaucoup, le Drop perd de son punch.", LoudnessTargets.format(lossDb))
    else tr("Pour sonner aussi fort, les crêtes sont tassées de %s dB (3 au plus : le Drop garde son punch).", LoudnessTargets.format(lossDb))

/** "3 min 12 s" (or "48 s"). */
internal fun durationText(seconds: Double): String {
    val total = seconds.roundToInt()
    return if (total >= 60) tr("%d min %d s", total / 60, total % 60) else tr("%d s", total)
}

/**
 * The end of an export (the mix; not separate tracks), in plain words (phase 38): « Prêt à
 * partager » or what to fix, its length, how loud it is, its peak (checked again after the M4A's
 * compression), the format; the pro readings under it.
 */
@Composable
internal fun ExportReportLine(export: ExportUi) {
    val lufs = export.loudness ?: return
    val summary = export.summary ?: return
    Column(Modifier.testTag("export_summary"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            (if (summary.ready) "✓ " else "") + summary.verdict(),
            style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = if (summary.ready) Neon.Acid else Neon.Amber, modifier = Modifier.testTag("export_verdict"),
        )
        listOfNotNull(summary.durationLine(), summary.levelLine(), summary.peakLine(), summary.formatLine()).forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    }
    // The pro readings (the decoded M4A's peak when it was checked).
    val peak = summary.truePeakDb
    WithHelp(Glossary.LUFS) {
        Text(
            tr("%s LUFS · crête %s dBTP", LoudnessTargets.format(lufs), peak?.takeIf { it > -100.0 }?.let { LoudnessTargets.format(it) } ?: "—"),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            modifier = Modifier.weight(1f, fill = false).testTag("export_report"),
        )
    }
    export.lossDb?.takeIf { it >= 0.1 }?.let { LossLine(it, Modifier.testTag("export_loss")) }
}
