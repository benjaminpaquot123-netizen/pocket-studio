package com.pocketstudio.app.export.video

import com.pocketstudio.ui.ScaledBottomSheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.video.VideoLevels
import com.pocketstudio.model.video.VideoPlan
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

/** What the video sheet shows: the part the 30 seconds are about, a picture of the video, the run. */
data class VideoSheetUi(
    /** The part the video is centred on (« Drop »), or null for a loop. */
    val part: String?,
    val seconds: Double,
    val run: VideoExportUi = VideoExportUi(),
    val preview: ImageBitmap? = null,
)

class VideoSheetActions(
    val create: () -> Unit = {},
    val cancel: () -> Unit = {},
    val share: () -> Unit = {},
    val again: () -> Unit = {},
    val close: () -> Unit = {},
)

/**
 * A still of the video before it is made: the image a second into the chosen part, its waveform
 * drawn from a made-up level (the real one needs the sound).
 */
fun previewFrame(plan: VideoPlan, renderer: (VideoPlan, VideoLevels.Levels) -> VideoFrameRenderer = { p, l -> VideoFrameRenderer(p, l) }): android.graphics.Bitmap {
    val n = plan.videoFrames.coerceAtLeast(1)
    val rms = FloatArray(n) { i -> (0.18f + 0.12f * sin(i * 0.9f) * sin(i * 0.13f)).coerceIn(0.02f, 1f) }
    val peak = FloatArray(n) { rms[it] * 2.2f }
    val at = plan.sections.firstOrNull { it.start > 0.0 }?.let { ((it.start + 1.0) * plan.fps).toInt() } ?: plan.fps
    return renderer(plan, VideoLevels.Levels(rms, peak)).frame(at.coerceIn(0, n - 1))
}

/** « Vidéo pour les réseaux » as a bottom sheet over the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSheet(ui: VideoSheetUi, actions: VideoSheetActions) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // While the video is being made the sheet stays: a swipe would hide the progress, not stop it.
    ScaledBottomSheet(onDismissRequest = { if (!ui.run.running) actions.close() }, sheetState = state, containerColor = Neon.Surface) {
        VideoSheetContent(ui, actions)
    }
}

@Composable
fun VideoSheetContent(ui: VideoSheetUi, actions: VideoSheetActions, modifier: Modifier = Modifier) {
    val run = ui.run
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 12.dp)
            .navigationBarsPadding().testTag("video_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Vidéo pour les réseaux"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = actions.close, enabled = !run.running, modifier = Modifier.testTag("video_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            // The picture, at the video's own proportions.
            Box(
                Modifier.width(112.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(12.dp)).background(Neon.Background)
                    .border(1.dp, Neon.Outline, RoundedCornerShape(12.dp)).testTag("video_preview"),
                contentAlignment = Alignment.Center,
            ) {
                ui.preview?.let { Image(it, contentDescription = tr("Aperçu de la vidéo"), contentScale = ContentScale.Crop) }
                    ?: Icon(Icons.Filled.Movie, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.size(36.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (ui.part != null) tr("%d secondes autour de « %s »", Math.round(ui.seconds).toInt(), ui.part)
                    else tr("%d secondes de ta partie en boucle", Math.round(ui.seconds).toInt()),
                    style = MaterialTheme.typography.titleSmall, color = Neon.Cyan, modifier = Modifier.testTag("video_part"),
                )
                Text(
                    tr("En vertical (9:16) pour les stories, TikTok ou Reels : la forme d'onde, le nom des parties, ton titre."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(tr("Une copie va dans Films › Pocket Studio."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
        }
        when {
            run.running -> {
                Text(
                    if (run.stage == VideoExportUi.Stage.Sound) tr("Fabrication du son…") else tr("Images et compression…"),
                    style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("video_stage"),
                )
                LinearProgressIndicator(
                    progress = { run.progress }, modifier = Modifier.fillMaxWidth().height(6.dp).testTag("video_progress"),
                    color = Neon.Acid, trackColor = Neon.SurfaceHigh,
                )
                OutlinedButton(onClick = actions.cancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("video_cancel")) { Text(tr("Annuler")) }
            }
            run.file != null -> {
                Text(tr("Ta vidéo est prête."), style = MaterialTheme.typography.bodyMedium, color = Neon.Acid, modifier = Modifier.testTag("video_done"))
                Button(onClick = actions.share, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("video_share")) {
                    Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  " + tr("Partager la vidéo"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = actions.again, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("video_again")) { Text(tr("En refaire une")) }
            }
            else -> {
                run.error?.let { Text(it, color = Neon.RedText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("video_error")) }
                Button(onClick = actions.create, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("video_create")) {
                    Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("  " + tr("Créer la vidéo"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(tr("Moins d'une minute, en général."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
        }
    }
}

/**
 * The sheet for the open project: the plan read once when it opens (its picture drawn off the
 * main thread), « Créer la vidéo », then Android's share sheet as soon as the video is made.
 */
@Composable
fun VideoExportRoute(rhythmVm: RhythmViewModel, close: () -> Unit) {
    val context = LocalContext.current
    val vm: VideoExportViewModel = viewModel()
    val run by vm.state.collectAsStateWithLifecycle()
    val plan = remember { vm.plan(rhythmVm) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(plan) {
        preview = withContext(Dispatchers.Default) {
            val (bold, regular) = VideoFrameRenderer.fonts(context)
            runCatching { previewFrame(plan) { p, l -> VideoFrameRenderer(p, l, bold = bold, regular = regular) }.asImageBitmap() }.getOrNull()
        }
    }
    val share = { run.file?.let { WavExporter.share(context, listOf(it), tr("Partager la vidéo")) }; Unit }
    // Made: the share sheet opens once by itself (noted in the ViewModel: not again when the screen turns).
    LaunchedEffect(run.file) {
        if (run.file != null && !run.autoShared) {
            vm.markShared()
            share()
        }
    }
    val project = rhythmVm.project.value
    val part = plan.part.takeIf { it in project.song.indices }?.let { com.pocketstudio.model.video.VideoPlanner.partLabel(project, project.song[it].scene) }
    VideoSheet(
        VideoSheetUi(part = part, seconds = plan.seconds, run = run, preview = preview),
        VideoSheetActions(
            create = { vm.start(rhythmVm) },
            cancel = vm::cancel,
            share = share,
            again = vm::reset,
            close = { vm.reset(); close() },
        ),
    )
}
