package com.pocketstudio.app.export

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.partSteps

/**
 * Lot E (« Master pro », PLAN-AUDITS-FINAUX §1 bis point 4): the file's final form, chosen under
 * « Avancé » in Pro (Mixeur › Exporter). The defaults are the export as it was before: the Mode simple
 * and « Partager » never change them.
 * - [rate] 44 100, 48 000 or 96 000 Hz (null: « 44,1 kHz » or 48 kHz as before); M4A stays at 48 kHz at most.
 * - [bits] 16, 24 or 32 (float); null: « 16 bits » or 24 as before. A 32-bit float WAV keeps the limiter
 *   (the mix without one is « Sans limiteur »); FLAC holds 24 at most, M4A is encoded from 16.
 * - [shaping]: at 16 bits, the dither's noise shaped (OfflineRenderer::Finish).
 * - [fadeInMs], [trimStart], [trimEnd], [endSilenceMs]: the start and the end (never on separate tracks:
 *   they keep one length, from the song's frame 0).
 * - [part] (a song part's index) or [fromBar]..[toBar] (bars of the song, from 1, [toBar] included): the
 *   range exported, inside the whole song (null: the whole song, or the loop).
 * - [stemsDry]: separate tracks without their sends, the reverb's and the echo's returns as files of their own.
 * - [report]: a text report beside the file (the pro readings).
 * - [artist]: the files' « artiste » tag (the title is the song's name).
 */
data class ExportOptions(
    val rate: Int? = null,
    val bits: Int? = null,
    val shaping: Boolean = false,
    val fadeInMs: Int = 0,
    val trimStart: Boolean = false,
    val trimEnd: Boolean = false,
    val endSilenceMs: Int = 0,
    val part: Int? = null,
    val fromBar: Int? = null,
    val toBar: Int? = null,
    val stemsDry: Boolean = false,
    val report: Boolean = true,
    val artist: String = "",
) {
    /** The file's rate for [format] ([cdRate]: the card's « 44,1 kHz »). */
    fun rateFor(format: ExportFormat, cdRate: Boolean): Int {
        val r = rate ?: if (cdRate) OfflineExport.CD_RATE else OfflineExport.SAMPLE_RATE
        return if (format == ExportFormat.M4a) r.coerceAtMost(OfflineExport.SAMPLE_RATE) else r
    }

    /**
     * The file's bits: 32 (float) without a limiter in WAV, or asked in WAV; 16 for an M4A's source;
     * FLAC at most 24.
     */
    fun bitsFor(format: ExportFormat, limited: Boolean, bits16: Boolean): Int = when {
        format == ExportFormat.Wav && !limited -> 32
        format == ExportFormat.M4a -> 16
        else -> {
            val b = bits ?: if (bits16) 16 else 24
            if (format == ExportFormat.Flac) b.coerceAtMost(24) else b
        }
    }

    /** The engine's finish for a render whose song frames before [rangeStart] are left out. */
    fun finish(stems: Boolean, rangeStart: Long, bits: Int): OfflineExport.Finish = OfflineExport.Finish(
        fadeInFrames = if (stems) 0 else fadeInMs.toLong() * OfflineExport.SAMPLE_RATE / 1000,
        trimStart = !stems && trimStart,
        trimEnd = !stems && trimEnd,
        endSilenceFrames = if (stems) 0 else endSilenceMs.toLong() * OfflineExport.SAMPLE_RATE / 1000,
        rangeStart = rangeStart,
        noiseShaping = shaping && bits == 16,
    )

    /** A range inside the song is asked ([part] or a selection). */
    val ranged: Boolean get() = part != null || (fromBar != null && toBar != null)

    /**
     * The song's frames [start, end) at 48 kHz this export keeps: a part, or bars [fromBar]..[toBar],
     * within the song (null: the whole song).
     */
    fun songRange(p: Project): LongRange? {
        if (p.song.isEmpty()) return null
        val sixteenth = 15.0 / p.tempoBpm * OfflineExport.SAMPLE_RATE
        val total = p.song.sumOf { p.partSteps(it).toLong() }
        part?.let { k ->
            val i = k.coerceIn(0, p.song.lastIndex)
            val before = p.song.take(i).sumOf { p.partSteps(it).toLong() }
            return Math.round(before * sixteenth) until Math.round((before + p.partSteps(p.song[i])) * sixteenth)
        }
        val from = fromBar ?: return null
        val to = toBar ?: return null
        val first = ((from.coerceAtLeast(1) - 1).toLong() * p.stepsPerBar).coerceIn(0, total)
        val last = (to.coerceAtLeast(from).toLong() * p.stepsPerBar).coerceIn(first + 1, total)
        return Math.round(first * sixteenth) until Math.round(last * sixteenth)
    }

    companion object {
        val RATES = listOf(OfflineExport.CD_RATE, OfflineExport.SAMPLE_RATE, OfflineExport.HIGH_RATE)
        val BITS = listOf(16, 24, 32)
        val FADES_IN_MS = listOf(0, 500, 2000)
        val END_SILENCES_MS = listOf(0, 500, 2000)
    }
}

/** « Fichier final » under « Avancé » of the export card (Pro mode). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@androidx.compose.runtime.Composable
internal fun ExportOptionsPanel(export: com.pocketstudio.app.rhythm.ExportUi, hasSong: Boolean, actions: com.pocketstudio.app.mixer.MasterProActions) {
    val o = export.pro
    val set = actions.setExportOptions
    val colors = com.pocketstudio.ui.pillColors(com.pocketstudio.ui.Neon.Acid)
    val enabled = !export.running
    val stems = export.what == com.pocketstudio.app.rhythm.ExportWhat.Stems
    val small = androidx.compose.material3.MaterialTheme.typography.bodySmall
    val title = androidx.compose.material3.MaterialTheme.typography.bodyMedium
    @androidx.compose.runtime.Composable
    fun chip(selected: Boolean, label: String, tag: String, onClick: () -> Unit) = androidx.compose.material3.FilterChip(
        selected = selected, onClick = onClick, enabled = enabled, label = { androidx.compose.material3.Text(label) }, colors = colors,
        modifier = Modifier.testTag(tag),
    )
    @androidx.compose.runtime.Composable
    fun row(content: @androidx.compose.runtime.Composable () -> Unit) = androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) { content() }
    val openState = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val open = openState.value
    com.pocketstudio.ui.WithHelp(
        com.pocketstudio.app.help.ScreenHelp.FINAL_FILE,
        about = listOf("export_final", "export_rate_*", "export_bits_*", "export_shaping", "export_range_*", "export_fade_in_*", "export_trim", "export_end_silence_*",
            "export_artist", "export_cover", "export_report"),
    ) {
        androidx.compose.material3.TextButton(onClick = { openState.value = !open }, modifier = Modifier.weight(1f, fill = false).testTag("export_final")) {
            androidx.compose.material3.Text(tr("Fichier final : %s", finalFileSummary(export)), color = com.pocketstudio.ui.Neon.Cyan)
        }
    }
    if (!open) return
    val rate = o.rateFor(export.format, export.cdRate)
    val bits = o.bitsFor(export.format, export.limited, export.bits16)
    androidx.compose.material3.Text(tr("Fréquence"), style = title)
    row {
        ExportOptions.RATES.forEach { r ->
            if (export.format == ExportFormat.M4a && r > OfflineExport.SAMPLE_RATE) return@forEach
            chip(rate == r, rateLabel(r), "export_rate_$r") { set(o.copy(rate = r)) }
        }
    }
    if (export.format != ExportFormat.M4a && !(export.limiterOff && export.format == ExportFormat.Wav)) {
        androidx.compose.material3.Text(tr("Bits"), style = title)
        row {
            ExportOptions.BITS.forEach { b ->
                if (export.format == ExportFormat.Flac && b == 32) return@forEach
                chip(bits == b, if (b == 32) tr("32 bits flottants") else tr("%d bits", b), "export_bits_$b") { set(o.copy(bits = b)) }
            }
            if (bits == 16) chip(o.shaping, tr("Dither mis en forme"), "export_shaping") { set(o.copy(shaping = !o.shaping)) }
        }
        if (bits == 16) androidx.compose.material3.Text(
            tr("En 16 bits, un léger bruit (dither) évite le grain des fondus ; mis en forme, il part dans les aigus où l'oreille l'entend moins."),
            style = small, color = com.pocketstudio.ui.Neon.TextSecondary,
        )
    }
    if (hasSong && export.what != com.pocketstudio.app.rhythm.ExportWhat.Loop) {
        androidx.compose.material3.Text(tr("Plage exportée"), style = title)
        row {
            chip(!o.ranged, tr("Tout le morceau"), "export_range_all") { set(o.copy(part = null, fromBar = null, toBar = null)) }
            chip(o.part != null, if (o.part != null) tr("Partie %d", o.part + 1) else tr("Une partie"), "export_range_part") {
                set(o.copy(part = ((o.part ?: -1) + 1).takeIf { it < export.parts } ?: 0, fromBar = null, toBar = null))
            }
            chip(o.fromBar != null, if (o.fromBar != null) tr("Mesures %1\$d à %2\$d", o.fromBar, o.toBar ?: o.fromBar) else tr("Une sélection"), "export_range_bars") {
                set(o.copy(part = null, fromBar = o.fromBar ?: 1, toBar = o.toBar ?: minOf(8, export.songBars.coerceAtLeast(1))))
            }
        }
        if (o.fromBar != null) {
            val total = export.songBars.coerceAtLeast(1).toFloat()
            androidx.compose.material3.Text(tr("Début : mesure %d", o.fromBar), style = small)
            com.pocketstudio.ui.ProSlider(value = o.fromBar.toFloat(), onValueChange = { v -> val f = v.toInt().coerceAtLeast(1); set(o.copy(fromBar = f, toBar = maxOf(f, o.toBar ?: f))) },
                valueRange = 1f..total, label = tr("Début"), valueText = tr("Début : mesure %d", o.fromBar), modifier = Modifier.testTag("export_range_from"))
            androidx.compose.material3.Text(tr("Fin : mesure %d", o.toBar ?: o.fromBar), style = small)
            com.pocketstudio.ui.ProSlider(value = (o.toBar ?: o.fromBar).toFloat(), onValueChange = { v -> set(o.copy(toBar = maxOf(o.fromBar, v.toInt()))) },
                valueRange = 1f..total, label = tr("Fin"), valueText = tr("Fin : mesure %d", o.toBar ?: o.fromBar), modifier = Modifier.testTag("export_range_to"))
        }
    }
    if (!stems) {
        androidx.compose.material3.Text(tr("Début et fin"), style = title)
        row {
            ExportOptions.FADES_IN_MS.forEach { ms ->
                chip(o.fadeInMs == ms, if (ms == 0) tr("Sans fondu de début") else tr("Fondu de début %s s", seconds(ms)), "export_fade_in_$ms") { set(o.copy(fadeInMs = ms)) }
            }
            chip(o.trimStart && o.trimEnd, tr("Couper les silences"), "export_trim") {
                val on = !(o.trimStart && o.trimEnd)
                set(o.copy(trimStart = on, trimEnd = on))
            }
            ExportOptions.END_SILENCES_MS.drop(1).forEach { ms ->
                chip(o.endSilenceMs == ms, tr("Silence de fin %s s", seconds(ms)), "export_end_silence_$ms") {
                    set(o.copy(endSilenceMs = if (o.endSilenceMs == ms) 0 else ms))
                }
            }
        }
        androidx.compose.material3.Text(
            tr("« Couper les silences » : le fichier part du premier son et s'arrête au dernier (la queue de réverbe est gardée)."),
            style = small, color = com.pocketstudio.ui.Neon.TextSecondary,
        )
    } else {
        row {
            chip(o.stemsDry, tr("Sans les envois (réverbe et écho à part)"), "export_stems_dry") { set(o.copy(stemsDry = !o.stemsDry)) }
            com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.ScreenHelp.STEMS_SENDS, about = listOf("export_stems_dry"))
        }
        androidx.compose.material3.Text(
            tr("Toutes les pistes ont la même longueur et partent du début : posées dans un autre logiciel, elles redonnent le mix."),
            style = small, color = com.pocketstudio.ui.Neon.TextSecondary,
        )
    }
    androidx.compose.material3.Text(tr("Infos du fichier"), style = title)
    // Round 2, n° 26: the name selected at the touch, what is typed replaces it.
    com.pocketstudio.app.ui.SelectAllTextField(
        value = o.artist, onValueChange = { set(o.copy(artist = it.take(60))) }, enabled = enabled,
        label = { androidx.compose.material3.Text(tr("Artiste")) },
        modifier = Modifier.fillMaxWidth().testTag("export_artist"),
    )
    androidx.compose.material3.Text(tr("Titre, tempo et tonalité viennent du projet."), style = small, color = com.pocketstudio.ui.Neon.TextSecondary)
    actions.controller?.let { c ->
        val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> uri?.let(c::setCover) }
        row {
            androidx.compose.material3.OutlinedButton(onClick = { pick.launch(arrayOf("image/*")) }, enabled = enabled, modifier = Modifier.testTag("export_cover")) {
                androidx.compose.material3.Text(tr("Choisir une pochette"))
            }
            if (!stems) chip(o.report, tr("Rapport avec le fichier"), "export_report") { set(o.copy(report = !o.report)) }
        }
    }
}


private fun rateLabel(r: Int): String = if (r == OfflineExport.CD_RATE) tr("44,1 kHz") else "${r / 1000} kHz"
private fun seconds(ms: Int): String = if (ms % 1000 == 0) "${ms / 1000}" else String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%.1f", ms / 1000.0)

/** « 48 kHz · 24 bits », the card's line before it is opened. */
internal fun finalFileSummary(export: com.pocketstudio.app.rhythm.ExportUi): String {
    val rate = export.pro.rateFor(export.format, export.cdRate)
    val bits = export.pro.bitsFor(export.format, export.limited, export.bits16)
    // Audit ultra round 2 (n° 10, seat A d): an M4A says its bit rate, never « 16 bits » (AAC has no bits per sample).
    if (export.format == ExportFormat.M4a) return rateLabel(rate) + " · " + tr("%d kb/s", AacEncoder.BIT_RATE / 1000)
    return rateLabel(rate) + " · " + if (bits == 32) tr("32 bits flottants") else tr("%d bits", bits)
}
