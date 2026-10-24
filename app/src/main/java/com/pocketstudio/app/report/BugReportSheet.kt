package com.pocketstudio.app.report

import android.graphics.Bitmap
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.app.Metrics
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** A picture of the sheet: its small copy, and whether it is the automatic capture. */
class ShotUi(val id: Long, val thumb: ImageBitmap?, val auto: Boolean = false)

/** What « Signaler un bug » shows. */
class BugReportUi(
    val text: String = "",
    /** The capture of the screen behind the sheet (null: not taken yet, or it failed). */
    val autoShot: ShotUi? = null,
    val attachAuto: Boolean = true,
    /** The pictures added (taken with 📸 or chosen on the phone). */
    val shots: List<ShotUi> = emptyList(),
    val maxShots: Int = BugReportViewModel.MAX_ADDED,
    val attachProject: Boolean = false,
    /** The open project's name (null: none, no « Joindre le projet »). */
    val projectName: String? = null,
    /** Reporting the previous session's crash. */
    val crash: Boolean = false,
    /** « Ce qui sera envoyé », in plain words. */
    val summary: List<String> = emptyList(),
    /** report.md as it would leave now, shown under « Voir le rapport » (null: folded). */
    val preview: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

class BugReportActions(
    val setText: (String) -> Unit = {},
    val setAttachAuto: (Boolean) -> Unit = {},
    val setAttachProject: (Boolean) -> Unit = {},
    /** « Ajouter une capture »: Android's photo picker. */
    val addPicture: () -> Unit = {},
    /** « Prendre une capture »: the sheet hides, the 📸 chip waits. */
    val takePicture: () -> Unit = {},
    val remove: (Long) -> Unit = {},
    /** « Voir le rapport » / « Masquer le rapport ». */
    val togglePreview: () -> Unit = {},
    val share: () -> Unit = {},
    val copy: () -> Unit = {},
    val close: () -> Unit = {},
)

/**
 * « Signaler un bug »: what happened in the user's words, the screenshots (the screen behind,
 * taken at once; more taken in the app or chosen on the phone), the project if wanted, what will
 * be sent in plain words, then « Partager » (Android's menu: the user picks who gets it) or « Copier le texte ».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BugReportContent(ui: BugReportUi, actions: BugReportActions, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 12.dp).navigationBarsPadding()
            .testTag("bug_sheet"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BugReport, contentDescription = null, tint = Neon.Amber)
            Text(
                tr("Signaler un bug"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            IconButton(onClick = actions.close, modifier = Modifier.testTag("bug_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary)
            }
        }
        if (ui.crash) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Amber.copy(alpha = 0.16f)).padding(12.dp).testTag("bug_crash"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = Neon.Amber)
                Text(
                    tr("L'appli s'est fermée d'un coup la dernière fois : ce qui s'est passé avant est joint."),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        OutlinedTextField(
            value = ui.text, onValueChange = actions.setText, minLines = 3, maxLines = 8,
            label = { Text(tr("Qu'est-ce qui s'est passé ?")) },
            placeholder = { Text(tr("Ex. : le son a coupé quand j'ai touché ▶ dans Live")) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth().testTag("bug_text"),
        )
        // The screenshots: first-class, the automatic one on by default.
        Text(tr("Captures d'écran"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        SwitchRow(
            tr("Joindre la capture de l'écran"), tr("L'écran derrière cette fiche, pris quand tu l'as ouverte"),
            ui.attachAuto, actions.setAttachAuto, "bug_attach_auto",
        )
        val thumbs = listOfNotNull(ui.autoShot?.takeIf { ui.attachAuto }) + ui.shots
        if (thumbs.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("bug_thumbs"), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                thumbs.forEach { Thumb(it, actions.remove) }
            }
        }
        val room = ui.maxShots - ui.shots.size
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.takePicture, enabled = room > 0, modifier = Modifier.heightIn(min = 48.dp).testTag("bug_take")) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  " + tr("Prendre une capture"), maxLines = 1)
            }
            OutlinedButton(onClick = actions.addPicture, enabled = room > 0, modifier = Modifier.heightIn(min = 48.dp).testTag("bug_add")) {
                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  " + tr("Ajouter une capture"), maxLines = 1)
            }
        }
        Text(
            if (room > 0) tr("%1\$d / %2\$d images ajoutées. « Prendre une capture » : va sur l'écran à montrer, puis touche 📸.", ui.shots.size, ui.maxShots)
            else tr("%1\$d / %2\$d images : c'est le maximum.", ui.shots.size, ui.maxShots),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        ui.projectName?.let { name ->
            SwitchRow(tr("Joindre le projet"), tr("« %s » : le fichier du morceau, sans ses sons", name), ui.attachProject, actions.setAttachProject, "bug_attach_project")
        }
        // What leaves the phone, said plainly before anything does.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.SurfaceHigh.copy(alpha = 0.6f))
                .border(1.dp, Neon.Outline, RoundedCornerShape(12.dp)).padding(12.dp).testTag("bug_preview"),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(tr("Ce qui sera envoyé"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan)
            ui.summary.forEach { line -> Text("• $line", style = MaterialTheme.typography.bodySmall) }
            TextButton(onClick = actions.togglePreview, modifier = Modifier.heightIn(min = 48.dp).testTag("bug_preview_toggle")) {
                Icon(if (ui.preview == null) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  " + if (ui.preview == null) tr("Voir le rapport tel qu'il partira") else tr("Masquer le rapport"))
            }
            ui.preview?.let { text ->
                // The very text that leaves (report.md, in French for the developer), in its own small scroll.
                Text(
                    text, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(8.dp)).background(Neon.Background)
                        .verticalScroll(rememberScrollState()).padding(8.dp).testTag("bug_preview_text"),
                )
            }
            Text(
                tr("Rien ne part tout seul : « Partager » ouvre le menu d'Android et tu choisis à qui l'envoyer."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(top = 4.dp),
            )
        }
        ui.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.Amber, modifier = Modifier.testTag("bug_message")) }
        Button(onClick = actions.share, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("bug_share")) {
            if (ui.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Neon.OnAccent)
            else Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + tr("Partager"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = actions.copy, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("bug_copy")) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  " + tr("Copier le texte"))
        }
    }
}

@Composable
private fun SwitchRow(title: String, line: String, on: Boolean, set: (Boolean) -> Unit, tag: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Switch) { set(!on) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        Switch(checked = on, onCheckedChange = set, modifier = Modifier.padding(start = 8.dp).testTag(tag))
    }
}

/** A thumbnail with its ✕ (the automatic capture's ✕ turns it off). */
@Composable
private fun Thumb(shot: ShotUi, remove: (Long) -> Unit) {
    Box(Modifier.testTag(if (shot.auto) "bug_thumb_auto" else "bug_thumb_${shot.id}")) {
        Box(
            Modifier.padding(top = 6.dp, end = 6.dp).height(132.dp).widthIn(min = 64.dp, max = 120.dp).clip(RoundedCornerShape(10.dp))
                .background(Neon.Background).border(BorderStroke(1.dp, if (shot.auto) Neon.Cyan else Neon.Outline), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (shot.thumb != null) {
                Image(shot.thumb, contentDescription = if (shot.auto) tr("Capture de l'écran") else tr("Image ajoutée"), contentScale = ContentScale.Fit,
                    modifier = Modifier.height(132.dp).aspectRatio(shot.thumb.width.toFloat() / shot.thumb.height.coerceAtLeast(1)))
            } else {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            if (shot.auto) {
                Text(
                    tr("auto"), style = MaterialTheme.typography.labelSmall, color = Neon.OnAccent,
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).clip(RoundedCornerShape(6.dp)).background(Neon.Cyan).padding(horizontal = 4.dp),
                )
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).size(28.dp).clip(CircleShape).background(Neon.SurfaceHigh).border(1.dp, Neon.Outline, CircleShape)
                .clickable(role = Role.Button, onClickLabel = tr("Retirer")) { remove(shot.id) }
                .testTag(if (shot.auto) "bug_remove_auto" else "bug_remove_${shot.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = tr("Retirer"), tint = Neon.TextPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

/** The sheet itself, above the screen it was opened from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportSheet(ui: BugReportUi, actions: BugReportActions) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = actions.close, sheetState = state, containerColor = Neon.Surface) {
        BugReportContent(ui, actions)
    }
}

/**
 * « Prendre une capture »: a small chip floating above the app (its own window, so it is never in
 * the picture) while the user goes to the screen to show; « 📸 Capturer » takes it, ✕ comes back.
 */
@Composable
fun CaptureChip(capture: () -> Unit, cancel: () -> Unit) {
    val lift = with(LocalDensity.current) { 96.dp.roundToPx() }
    Popup(alignment = Alignment.BottomCenter, offset = IntOffset(0, -lift), properties = PopupProperties(focusable = false)) {
        CaptureChipContent(capture, cancel)
    }
}

/** The 📸 chip itself (drawn in its own window by [CaptureChip]). */
@Composable
fun CaptureChipContent(capture: () -> Unit, cancel: () -> Unit) {
        Row(
            Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(28.dp)).background(Color.Black.copy(alpha = 0.82f)).border(1.5.dp, Neon.Amber, RoundedCornerShape(28.dp))
                .padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp).testTag("bug_capture_chip"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = capture, colors = ButtonDefaults.buttonColors(containerColor = Neon.Amber, contentColor = Color.Black),
                modifier = Modifier.heightIn(min = 48.dp).testTag("bug_capture_now"),
            ) {
                Text("📸 " + tr("Capturer"), fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(
                tr("Va sur l'écran à montrer"), style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp).padding(horizontal = 8.dp),
            )
            IconButton(onClick = cancel, modifier = Modifier.testTag("bug_capture_cancel")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Revenir au rapport"), tint = Color.White)
            }
        }
}

/** After a crash, at the next launch: a discreet line under the bar, never a window in the way. */
@Composable
fun CrashBanner(send: () -> Unit, dismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(Neon.Amber.copy(alpha = 0.16f)).padding(start = 12.dp, end = 4.dp).heightIn(min = 48.dp).testTag("crash_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.BugReport, contentDescription = null, tint = Neon.Amber, modifier = Modifier.size(18.dp))
        Text(
            tr("L'appli s'est fermée d'un coup : envoyer le rapport ?"), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        TextButton(onClick = send, modifier = Modifier.testTag("crash_send")) { Text(tr("Envoyer"), fontWeight = FontWeight.Bold) }
        IconButton(onClick = dismiss, modifier = Modifier.testTag("crash_dismiss")) {
            Icon(Icons.Filled.Close, contentDescription = tr("Ignorer"), tint = Neon.TextSecondary)
        }
    }
}

/** The screen as it is now (the app's own window: sheets and menus above it are not in it). */
fun captureScreen(view: View): Bitmap? = runCatching {
    val root = view.rootView
    if (root.width <= 0 || root.height <= 0) null else root.drawToBitmap(Bitmap.Config.ARGB_8888)
}.onFailure { Journal.error("capture", it) }.getOrNull()

/** « Ce qui sera envoyé », in the interface's language. */
fun bugSummary(s: BugReportState, projectName: String?): List<String> = buildList {
    add(if (s.text.isBlank()) tr("Ton texte : rien d'écrit pour l'instant (dis ce que tu faisais et ce qui a cloché).") else tr("Ton texte (%d caractères)", s.text.length))
    add(tr("La version de l'appli, le modèle du téléphone, sa version d'Android et la sortie du son"))
    val stream = Metrics.streamInfo.value
    add(tr("Le son de la dernière minute : niveau, décrochages (%1\$d), son rouvert (%2\$d)", stream.xruns, stream.restarts))
    add(tr("Les %1\$d derniers écrans et gestes, et %2\$d erreurs", minOf(Journal.events.size, BugReportText.EVENTS), Journal.errors.size))
    if (s.crash != null) add(tr("Le rapport du plantage de la dernière fois"))
    val shots = (if (s.attachAuto && s.autoShot != null) 1 else 0) + s.shots.size
    if (shots > 0) add(tr("Captures d'écran : %d", shots))
    if (s.attachProject && projectName != null) add(tr("Le projet « %s » (sans ses sons)", projectName))
}

/**
 * Where « Signaler un bug » lives: opened through [BugReportViewModel.request] from anywhere (Plus,
 * the Mode simple's ⋯, a long press on « ? », the crash banner), it captures the screen first, then
 * shows the sheet; the 📸 chip while a capture is being taken.
 */
@Composable
fun BugReportHost(vm: BugReportViewModel, projectName: String?) {
    val s by vm.state.collectAsStateWithLifecycle()
    val view = LocalView.current
    LaunchedEffect(s.requested) {
        if (s.requested == null) return@LaunchedEffect
        // One frame: a menu that was touched closes first.
        withFrameNanos { }
        vm.opened(captureScreen(view))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(BugReportViewModel.MAX_ADDED)) { uris -> vm.picked(uris) }
    if (s.capturing) {
        CaptureChip(capture = { vm.captured(captureScreen(view)) }, cancel = vm::cancelCapture)
    }
    if (!s.open) return
    val ui = BugReportUi(
        text = s.text,
        autoShot = s.autoShot?.let { ShotUi(it.id, it.thumb, auto = true) },
        attachAuto = s.attachAuto,
        shots = s.shots.map { ShotUi(it.id, it.thumb) },
        attachProject = s.attachProject,
        projectName = projectName,
        crash = s.crash != null,
        summary = bugSummary(s, projectName),
        preview = if (s.previewOpen) remember(s) { vm.reportText() } else null,
        busy = s.busy,
        message = s.message,
    )
    BugReportSheet(
        ui,
        BugReportActions(
            setText = vm::setText, setAttachAuto = vm::setAttachAuto, setAttachProject = vm::setAttachProject,
            addPicture = {
                runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    .onFailure { Journal.error("sélecteur de photos", it) }
            },
            takePicture = vm::startCapture, remove = vm::remove, togglePreview = vm::togglePreview, share = vm::share, copy = vm::copyText, close = vm::close,
        ),
    )
}
