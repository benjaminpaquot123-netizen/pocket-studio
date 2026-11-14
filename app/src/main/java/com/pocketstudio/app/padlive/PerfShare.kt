@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.padlive

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.FloatWav
import com.pocketstudio.app.audio.PadBounce
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.export.ExportRun
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.ExportFormat
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePadTake
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.PadSpanRecord
import com.pocketstudio.model.PadTakeRecord
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledBottomSheet
import com.pocketstudio.ui.pillColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The export of a performance (lot B3): the pads a Live take kept, one file per pad. The take's
 * « Pads live » audio track holds them mixed; its [PadTakeRecord] (kept with the project, undone
 * with the take) says how each pad played, so each one is rendered again alone, exactly as on the
 * track (its pitch, its « Fin », its volume, the strength it was played at): together the files
 * give back the track.
 */
object PerfExport {
    /** The record of a take that kept pads [spans] of [clips] on audio track [track]. */
    fun record(track: Int, spans: List<LivePadTake.Span>, clips: List<LiveClip?>, start: Int, end: Int, bpm: Double): PadTakeRecord {
        val kept = spans.take(PadTakeRecord.MAX_SPANS)
        val used = kept.map { it.pad }.distinct().mapNotNull { pad -> clips.getOrNull(pad)?.let { pad to it } }.toMap()
        return PadTakeRecord(track, start, end, bpm, kept.map(PadSpanRecord::of), used)
    }

    /** The take of [p] that can be split into pads (its track still there), or null. */
    fun takeOf(p: Project): PadTakeRecord? = p.padLive.take?.takeIf { r -> r.spans.isNotEmpty() && p.audioTracks.any { it.id == r.track && it.clip != null } }

    /** Pad [pad] of [r] alone, as on the take's track ([sounds]: each pad's sound as the engine plays it). */
    fun padSound(r: PadTakeRecord, pad: Int, sounds: Map<Int, Pcm>, rate: Int = ResampleSource.RATE): Pcm? =
        PadBounce.render(r.spans.filter { it.pad == pad }.map { it.span() }, r.clipList(), sounds, r.start, r.end, r.bpm, rate)

    /** A pad's file name: « a1-kick » (its page and place, then its sound). */
    fun label(pad: Int, clip: LiveClip?): String {
        val name = clip?.let { DefaultNames.shownSample(it.name) }.orEmpty().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').take(20)
        val where = padName(pad).lowercase()
        return if (name.isEmpty()) "pad-$where" else "$where-$name"
    }
}

/** « Pistes séparées » as the sheet shows it. */
sealed interface PerfStemsUi {
    data object Idle : PerfStemsUi
    /** Pad [done] of [total] being written. */
    data class Running(val done: Int, val total: Int) : PerfStemsUi
    /** Written ([files]), each also copied to Musique › BenStudio when [inMusic]. */
    data class Done(val files: List<File>, val inMusic: Boolean) : PerfStemsUi
    data class Failed(val message: String) : PerfStemsUi
}

/**
 * « Partager ma perf », held by [PadLiveController.perf]: the sound (the song the take wrote, with
 * its effects: the app's « Partager mon morceau ») and « Pistes séparées » (one WAV per pad of the
 * take, [PerfExport]), copied to Musique › BenStudio and handed to Android's share menu.
 */
class PerfShare(private val vm: RhythmViewModel) {
    private val _stems = MutableStateFlow<PerfStemsUi>(PerfStemsUi.Idle)
    val stems: StateFlow<PerfStemsUi> = _stems.asStateFlow()

    private var job: Job? = null

    /** Tests: where the files go instead of Android's share menu. */
    internal var shareForTest: ((List<File>) -> Unit)? = null

    /** « Partager le son »: the song the take wrote, in [format], through the app's own share. */
    fun shareSound(format: ExportFormat) {
        if (job?.isActive == true) return
        vm.shareSong(format)
    }

    /** « Partager les pistes »: one WAV per pad of the last take, then Android's share menu. */
    fun shareStems(context: Context) {
        if (job?.isActive == true) return
        if (vm.export.value.running) {
            _stems.value = PerfStemsUi.Failed(tr("Un export est déjà en cours : attends qu'il finisse."))
            return
        }
        val p = vm.project.value
        val record = PerfExport.takeOf(p)
        if (record == null) {
            _stems.value = PerfStemsUi.Failed(tr("Pas encore de perf gardée : touche ● REC, joue, puis touche Fin."))
            return
        }
        val app = context.applicationContext
        val pads = record.pads
        _stems.value = PerfStemsUi.Running(0, pads.size)
        ExportRun.begin(app, tr("Pistes de « %s »", DefaultNames.shown(p.name)), ::cancel)
        job = vm.viewModelScope.launch {
            val made = ArrayList<File>()
            var ending: String? = null
            try {
                var music = true
                val gain = p.audioTracks.firstOrNull { it.id == record.track }?.clip?.gain ?: 1f
                for ((i, pad) in pads.withIndex()) {
                    _stems.value = PerfStemsUi.Running(i, pads.size)
                    ExportRun.progress(i.toFloat() / pads.size, tr("Pad %1\$d sur %2\$d", i + 1, pads.size))
                    val clip = record.clips[pad] ?: continue
                    val file = withContext(Dispatchers.IO) {
                        val src = runCatching { PadSound.shaped(vm.padSamples.read(clip.file), clip) }.getOrNull() ?: return@withContext null
                        val pcm = PerfExport.padSound(record, pad, mapOf(pad to src)) ?: return@withContext null
                        if (gain != 1f) for (k in pcm.data.indices) pcm.data[k] *= gain
                        if (!isActive) return@withContext null
                        val out = WavExporter.newFile(app, "${DefaultNames.shown(p.name).take(24)}-${PerfExport.label(pad, clip)}")
                        FloatWav.write(out, pcm)
                        out
                    } ?: continue
                    made += file
                    music = withContext(Dispatchers.IO) { WavExporter.copyToMusic(app, file) } && music
                }
                if (made.isEmpty()) throw IllegalStateException(tr("Les sons des pads de la prise sont introuvables."))
                _stems.value = PerfStemsUi.Done(made.toList(), music)
                ending = tr("Pistes prêtes (fichiers : %d)", made.size)
                AppLog.add("Perf : ${made.size} pistes de pads")
                shareForTest?.let { it(made.toList()) } ?: WavExporter.share(app, made, tr("Partager mes pistes"))
            } catch (e: CancellationException) {
                made.forEach { it.delete() }
                _stems.value = PerfStemsUi.Idle
                throw e
            } catch (e: Exception) {
                made.forEach { it.delete() }
                AppLog.add("Perf : pistes impossibles (${e.message ?: e.javaClass.simpleName})")
                _stems.value = PerfStemsUi.Failed(e.message ?: tr("Le fichier n'a pas pu être créé."))
                ending = (_stems.value as PerfStemsUi.Failed).message
            } catch (e: OutOfMemoryError) {
                made.forEach { it.delete() }
                _stems.value = PerfStemsUi.Failed(tr("Pas assez de mémoire pour ces pistes : essaie une prise plus courte."))
                ending = (_stems.value as PerfStemsUi.Failed).message
            } finally {
                ExportRun.end(ending)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    /** The sheet closed: what it said goes. */
    fun reset() {
        if (job?.isActive != true) _stems.value = PerfStemsUi.Idle
    }
}

/** What « Partager ma perf » shows. */
class PerfShareUi(
    /** The take's song lasts this long (0: no song yet). */
    val seconds: Int,
    /** Pads the take kept (0: none, « Pistes séparées » is off). */
    val pads: Int,
    val format: ExportFormat,
    val stems: PerfStemsUi,
    /** The sound's own export runs (its progress is in its own window). */
    val soundRunning: Boolean,
)

class PerfShareActions(
    val setFormat: (ExportFormat) -> Unit = {},
    val shareSound: () -> Unit = {},
    val shareStems: () -> Unit = {},
    val close: () -> Unit = {},
)

/**
 * « Partager ma perf »: the sound in one file (M4A to send, WAV for the best), or the pads one file
 * each (« Pistes séparées », for another app). Nothing to share yet: says how to make a perf.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfShareSheet(ui: PerfShareUi, actions: PerfShareActions) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = actions.close, sheetState = state, containerColor = Neon.Surface) { PerfShareContent(ui, actions) }
}

/** The sheet's content (captured on its own by the screenshots: a sheet is a window of its own). */
@Composable
fun PerfShareContent(ui: PerfShareUi, actions: PerfShareActions) {
    run {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 16.dp)
                .testTag("perf_share"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Partager ma perf"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = actions.close, modifier = Modifier.testTag("perf_share_close")) {
                    Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary)
                }
            }
            if (ui.seconds <= 0) {
                Text(tr("Pas encore de perf gardée : touche ● REC, joue, puis touche Fin."), style = MaterialTheme.typography.bodyMedium,
                    color = Neon.TextPrimary, modifier = Modifier.testTag("perf_share_none"))
                return@Column
            }
            Text(
                tr("Ta perf : %s", "%d:%02d".format(ui.seconds / 60, ui.seconds % 60)) + if (ui.pads > 0) " · " + tr("pads joués : %d", ui.pads) else "",
                style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("perf_share_what"),
            )
            Card {
                Text(tr("Le son"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(tr("Un seul fichier avec tes effets, prêt à partager."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ExportFormat.M4a to tr("M4A · pour partager"), ExportFormat.Wav to tr("WAV · qualité max")).forEach { (f, label) ->
                        FilterChip(selected = ui.format == f, onClick = { actions.setFormat(f) }, label = { Text(label, maxLines = 1) },
                            colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("perf_format_${f.name}"))
                    }
                }
                Button(onClick = actions.shareSound, enabled = !ui.soundRunning && ui.stems !is PerfStemsUi.Running,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("perf_share_sound")) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  " + tr("Partager le son"), fontWeight = FontWeight.Bold)
                }
            }
            Card {
                Text(tr("Pistes séparées"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (ui.pads > 0) tr("Un fichier WAV par pad joué, pour le retravailler dans une autre appli.")
                    else tr("Les pads de cette perf n'ont pas été gardés : refais une prise avec ● REC."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
                when (val s = ui.stems) {
                    is PerfStemsUi.Running -> {
                        Text(tr("Pad %1\$d sur %2\$d…", s.done + 1, s.total), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("perf_stems_running"))
                        LinearProgressIndicator(progress = { (s.done.toFloat() / s.total.coerceAtLeast(1)).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    }
                    is PerfStemsUi.Done -> Text(
                        if (s.inMusic) tr("C'est prêt (fichiers : %d). Une copie est aussi dans Musique › BenStudio.", s.files.size)
                        else tr("C'est prêt (fichiers : %d). Choisis l'appli pour les envoyer.", s.files.size),
                        style = MaterialTheme.typography.bodySmall, color = Neon.Acid,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("perf_stems_done"),
                    )
                    is PerfStemsUi.Failed -> Text(s.message, style = MaterialTheme.typography.bodySmall, color = Neon.RedText,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("perf_stems_failed"))
                    PerfStemsUi.Idle -> Unit
                }
                FilledTonalButton(onClick = actions.shareStems, enabled = ui.pads > 0 && ui.stems !is PerfStemsUi.Running && !ui.soundRunning,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("perf_share_stems")) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  " + tr("Partager les pistes"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Neon.SurfaceHigh).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

/** The take's song length in seconds ([p]'s song as the take wrote it). */
internal fun perfSeconds(p: Project): Int {
    if (p.song.isEmpty()) return 0
    val steps = p.song.sumOf { p.partSteps(it) }
    return (steps * 15.0 / p.tempoBpm.coerceAtLeast(1.0)).toInt()
}
