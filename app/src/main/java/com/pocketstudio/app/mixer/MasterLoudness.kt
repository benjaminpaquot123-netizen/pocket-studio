package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

/** « −8,1 LUFS · TP −1,2 »: the master's short-term loudness and its true peak since ▶. */
internal fun loudnessChipText(values: FloatArray): String =
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
            tr(
                "Maintenant %s · sur 3 s %s · depuis ▶ %s LUFS",
                loudnessText(v.getOrElse(NativeEngine.LOUDNESS_MOMENTARY) { -100f }),
                loudnessText(v.getOrElse(NativeEngine.LOUDNESS_SHORT_TERM) { -100f }),
                loudnessText(v.getOrElse(NativeEngine.LOUDNESS_INTEGRATED) { -100f }),
            ) to (drive * 10f).roundToInt() / 10f
        }
    }
    val (line, drive) = readings
    WithHelp(Glossary.LUFS) {
        Text(line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("loudness_readings"))
    }
    Text(tr("Volume visé"), style = MaterialTheme.typography.bodyMedium)
    val colors = pillColors(Neon.Cyan)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(LoudnessTarget.AUTO, LoudnessTarget.CLUB, LoudnessTarget.STREAMING, LoudnessTarget.OFF).forEach { t ->
            val lufs = if (t == LoudnessTarget.AUTO) styleLufs else LoudnessTargets.lufs(t, null)
            FilterChip(
                selected = target == t, onClick = { setTarget(t) },
                label = { Text(if (lufs == null) t.label else "${t.label} · ${LoudnessTargets.formatTarget(lufs)}") },
                colors = colors, modifier = Modifier.testTag("loudness_target_${t.name}"),
            )
        }
    }
    val lufs = LoudnessTargets.formatTarget(if (target == LoudnessTarget.AUTO) styleLufs else LoudnessTargets.lufs(target, null) ?: 0f)
    Text(
        when (target) {
            LoudnessTarget.AUTO -> tr("Le volume des morceaux de ce style (%s LUFS). La sortie monte ou baisse doucement pour y arriver ; l'export y est exactement.", lufs)
            LoudnessTarget.CLUB -> tr("Fort, comme en club (%s LUFS). La sortie monte doucement pour y arriver ; l'export y est exactement.", lufs)
            LoudnessTarget.STREAMING -> tr("%s LUFS : le volume où Spotify et YouTube ramènent tous les morceaux.", lufs)
            LoudnessTarget.OFF -> tr("Le volume reste celui que tu règles ; la sortie est seulement protégée (crête à −1 dBTP).")
        },
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
    )
    if (target != LoudnessTarget.OFF && drive != 0f) {
        Text(
            tr("Gain ajouté pour la cible : %s dB", (if (drive > 0f) "+" else "") + LoudnessTargets.format(drive.toDouble())),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("loudness_drive"),
        )
    }
}

/** "3 min 12 s" (or "48 s"). */
internal fun durationText(seconds: Double): String {
    val total = seconds.roundToInt()
    return if (total >= 60) tr("%d min %d s", total / 60, total % 60) else tr("%d s", total)
}

/** The end of an export: how loud it is, its true peak and its length (the mix; not separate tracks). */
@Composable
internal fun ExportReportLine(export: ExportUi) {
    val lufs = export.loudness ?: return
    WithHelp(Glossary.LUFS) {
        Text(
            tr("Volume perçu : %s LUFS, %s", LoudnessTargets.format(lufs), Loudness.describe(lufs)),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f, fill = false).testTag("export_loudness"),
        )
    }
    val peak = export.truePeakDb
    val seconds = export.seconds
    if (peak != null && seconds != null && peak > -100.0) {
        Text(
            tr("Crête vraie %s dBTP · durée %s", LoudnessTargets.format(peak), durationText(seconds)),
            style = MaterialTheme.typography.bodySmall, color = if (peak > -1.0) Neon.Amber else Neon.TextSecondary,
            modifier = Modifier.testTag("export_report"),
        )
    }
}
