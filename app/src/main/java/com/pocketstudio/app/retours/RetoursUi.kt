package com.pocketstudio.app.retours

import android.app.Activity
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.export.WavExporter
import com.pocketstudio.app.report.captureScreen
import com.pocketstudio.app.settings.SettingsStore
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * « Mode retours » over the whole app ([content]): while [show], a small floating bubble
 * (● REC / ■, ✎ Note, ⋯) and the taps logged; « Envoyer mes retours » and the notes' sheet.
 */
@Composable
fun RetoursHost(show: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(Unit) { Retours.attach(context) }
    var note by remember { mutableStateOf<NoteDraft?>(null) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK && r.data != null) RetoursService.start(context, r.resultCode, r.data)
        else Retours.recordingEnded(tr("L'enregistrement de l'écran a été refusé."))
    }
    val startRecording = {
        Retours.setRec(RecState.Starting)
        runCatching {
            val mpm = context.getSystemService(MediaProjectionManager::class.java)
            // The whole screen (dialogs and sheets included; it goes on in the background).
            val ask = if (Build.VERSION.SDK_INT >= 34) mpm.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
            else mpm.createScreenCaptureIntent()
            consent.launch(ask)
        }.onFailure { Retours.recordingEnded(tr("L'enregistrement n'a pas pu démarrer.")) }
        Unit
    }
    Box(Modifier.fillMaxSize().then(if (show) Modifier.tapLog(view) else Modifier)) {
        content()
        if (show) {
            RetoursBubble(
                record = startRecording,
                stop = { RetoursService.stop(context) },
                note = { note = NoteDraft(captureScreen(view)) },
                hide = {
                    SettingsStore.update(context) { it.copy(retoursMode = false) }
                    Retours.message.value = tr("Le Mode retours se rallume dans Aide.")
                },
            )
        }
        note?.let { draft ->
            RetoursNoteSheet(
                save = { text -> Retours.addNote(text, draft.shot); note = null; Retours.message.value = tr("Note enregistrée") },
                cancel = { note = null },
            )
        }
        val exportOpen by Retours.exportOpen.collectAsState()
        if (exportOpen) RetoursExportPanel(close = { Retours.exportOpen.value = false })
        val message by Retours.message.collectAsState()
        message?.let { m ->
            LaunchedEffect(m) { delay(4000); if (Retours.message.value == m) Retours.message.value = null }
            Surface(
                color = Neon.SurfaceHigh, shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
                    .testTag("retours_message").clickable { Retours.message.value = null },
            ) { Text(m, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

private class NoteDraft(val shot: android.graphics.Bitmap?)

/** Every tap (not a drag) while the mode is on: what it touched (its tag and text) and where. */
private fun Modifier.tapLog(view: View) = pointerInput(view) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val start = down.position
        var moved = false
        while (true) {
            val e = awaitPointerEvent(PointerEventPass.Initial)
            val c = e.changes.firstOrNull { it.id == down.id } ?: break
            if ((c.position - start).getDistance() > viewConfiguration.touchSlop) moved = true
            if (!c.pressed) break
        }
        if (!moved && Retours.active) Retours.event("toucher", RetoursTaps.describe(view, start))
    }
}

/** What a tap touched, from the screen's semantics (its test tag, its text), and where in % of the screen. */
object RetoursTaps {
    fun describe(view: View, at: Offset): String {
        val label = runCatching { labelAt(view, at) }.getOrNull()
        val w = view.width.coerceAtLeast(1)
        val h = view.height.coerceAtLeast(1)
        val where = "(${(at.x * 100 / w).roundToInt()} %, ${(at.y * 100 / h).roundToInt()} %)"
        return if (label.isNullOrEmpty()) where else "$label $where"
    }

    private fun labelAt(view: View, at: Offset): String? {
        val owner = (view as? RootForTest)?.semanticsOwner ?: return null
        var best: SemanticsNode? = null
        fun visit(n: SemanticsNode) {
            if (!n.boundsInRoot.contains(at)) return
            val c = n.config
            if (SemanticsProperties.TestTag in c || SemanticsActions.OnClick in c) best = n
            n.children.forEach(::visit)
        }
        visit(owner.rootSemanticsNode)
        val n = best ?: return null
        val tag = n.config.getOrNull(SemanticsProperties.TestTag)
        val text = n.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
            ?: n.config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
        return listOfNotNull(tag?.let { "[$it]" }, text?.take(60)?.let { "« $it »" }).joinToString(" ")
    }
}

/** The floating bubble: draggable, it snaps to the nearest side (left or right, whatever the language's direction). */
@Composable
private fun RetoursBubble(record: () -> Unit, stop: () -> Unit, note: () -> Unit, hide: () -> Unit) {
    val rec by Retours.rec.collectAsState()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val bw = with(density) { BUBBLE_WIDTH.toPx() }
        val bh = with(density) { 132.dp.toPx() }
        val margin = with(density) { 4.dp.toPx() }
        // Physical place (px from the top left); first on the right, a third down: off the transport and the tabs.
        var x by rememberSaveable { mutableStateOf(Float.NaN) }
        var y by rememberSaveable { mutableStateOf(Float.NaN) }
        if (x.isNaN()) { x = maxW - bw - margin; y = maxH * 0.3f }
        x = x.coerceIn(margin, (maxW - bw - margin).coerceAtLeast(margin))
        y = y.coerceIn(margin, (maxH - bh - margin).coerceAtLeast(margin))
        var menu by remember { mutableStateOf(false) }
        val active = rec !is RecState.Idle
        Column(
            Modifier.align(AbsoluteAlignment.TopLeft)
                .absoluteOffset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .width(BUBBLE_WIDTH)
                .alpha(if (active) 0.92f else 0.78f)
                .background(Neon.Surface, RoundedCornerShape(BUBBLE_WIDTH / 2))
                .border(1.dp, if (rec is RecState.Recording) Neon.Red else Neon.Outline, RoundedCornerShape(BUBBLE_WIDTH / 2))
                .pointerInput(maxW, maxH) {
                    detectDragGestures(
                        onDragEnd = { x = if (x + bw / 2 < maxW / 2) margin else maxW - bw - margin },
                    ) { change, drag ->
                        change.consume()
                        x = (x + drag.x).coerceIn(margin, maxW - bw - margin)
                        y = (y + drag.y).coerceIn(margin, maxH - bh - margin)
                    }
                }
                .padding(vertical = 4.dp)
                .testTag("retours_bubble"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val r = rec) {
                is RecState.Recording -> {
                    var now by remember { mutableLongStateOf(System.nanoTime()) }
                    val since = remember { System.nanoTime() }
                    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.nanoTime() } }
                    BubbleButton("■", Retours.clock((now - since) / 1_000_000).substringBefore('.'), tr("Arrêter l'enregistrement"), "retours_stop", Neon.Red, stop)
                    if (r.segment > 1) Text("${r.segment}", fontSize = 9.sp, color = Neon.TextSecondary)
                }
                RecState.Idle -> BubbleButton("●", "REC", tr("Filmer l'écran avec le son"), "retours_rec", Neon.Red, record)
                else -> BubbleButton("…", "", tr("Patiente…"), "retours_wait", Neon.TextSecondary) {}
            }
            BubbleButton("✎", tr("Noter"), tr("Écrire une note"), "retours_note", Neon.Cyan, note)
            Box {
                BubbleButton("⋯", "", tr("Mode retours"), "retours_more", Neon.TextSecondary) { menu = true }
                com.pocketstudio.ui.ScaledDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(tr("Envoyer mes retours")) }, onClick = { menu = false; Retours.exportOpen.value = true },
                        modifier = Modifier.testTag("retours_menu_send"))
                    DropdownMenuItem(text = { Text(tr("Masquer la bulle")) }, onClick = { menu = false; hide() },
                        modifier = Modifier.testTag("retours_menu_hide"))
                }
            }
        }
    }
}

private val BUBBLE_WIDTH = 48.dp

@Composable
private fun BubbleButton(symbol: String, label: String, description: String, tag: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.size(width = BUBBLE_WIDTH, height = 40.dp).clickable(onClick = onClick)
            .semantics { contentDescription = description }.testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(symbol, color = tint, fontSize = if (label.isEmpty()) 18.sp else 14.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
        if (label.isNotEmpty()) Text(label, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1, color = Neon.TextPrimary, textAlign = TextAlign.Center)
    }
}

/** ✎ Note: a few words (the keyboard's microphone dictates), saved with the screen, the tutorial's step and a screenshot. */
@Composable
fun RetoursNoteSheet(save: (String) -> Unit, cancel: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).pointerInput(Unit) { detectTapGestures { cancel() } }.imePadding(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            color = Neon.Background, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), shadowElevation = 8.dp,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().pointerInput(Unit) { detectTapGestures {} }.testTag("retours_note_sheet"),
        ) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Écrire une note"), style = MaterialTheme.typography.titleMedium)
                val where = listOf(Retours.screen, Retours.tuto).filter { it.isNotEmpty() }.joinToString(" · ")
                if (where.isNotEmpty()) Text(where, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2)
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, label = { Text(tr("Ta note")) }, minLines = 3,
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("retours_note_text"),
                )
                Text(tr("Astuce : le micro du clavier permet de dicter. Une capture de l'écran est jointe."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = cancel, modifier = Modifier.testTag("retours_note_cancel")) { Text(tr("Annuler")) }
                    Button(onClick = { save(text) }, enabled = text.isNotBlank(), modifier = Modifier.testTag("retours_note_save")) { Text(tr("Enregistrer la note")) }
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

/** « Envoyer mes retours »: each file (videos ≤ 20 MB, notes.zip) through Android's share menu, then « effacer ». */
@Composable
fun RetoursExportPanel(close: () -> Unit) {
    val context = LocalContext.current
    var files by remember { mutableStateOf<List<File>?>(null) }
    var reload by remember { mutableStateOf(0) }
    var sent by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val recording = Retours.rec.collectAsState().value !is RecState.Idle
    LaunchedEffect(reload) { files = withContext(Dispatchers.IO) { Retours.exportFiles() } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).pointerInput(Unit) { detectTapGestures { close() } }, contentAlignment = Alignment.Center) {
        Surface(
            color = Neon.Background, shape = RoundedCornerShape(16.dp), shadowElevation = 8.dp,
            modifier = Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 640.dp).pointerInput(Unit) { detectTapGestures {} }
                .testTag("retours_export"),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Envoyer mes retours"), style = MaterialTheme.typography.titleMedium)
                if (recording) Text(tr("La vidéo en cours s'ajoute à la liste quand tu l'arrêtes."), style = MaterialTheme.typography.bodySmall, color = Neon.Amber)
                val list = files
                when {
                    list == null -> Text(tr("Patiente…"), color = Neon.TextSecondary)
                    list.isEmpty() -> Text(tr("Rien à envoyer pour l'instant."), color = Neon.TextSecondary, modifier = Modifier.testTag("retours_export_empty"))
                    else -> {
                        list.forEachIndexed { i, f ->
                            Row(Modifier.fillMaxWidth().testTag("retours_file_$i"), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(if (f.extension == "zip") tr("Notes et captures") else tr("Vidéo %d", i + 1), style = MaterialTheme.typography.bodyLarge)
                                    Text("${sizeOf(f.length())} · ${f.name}", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2)
                                }
                                OutlinedButton(onClick = { if (WavExporter.share(context, listOf(f), tr("Envoyer mes retours"))) sent = true }) { Text(tr("Envoyer")) }
                            }
                        }
                        Button(
                            onClick = { if (WavExporter.share(context, list, tr("Envoyer mes retours"))) sent = true },
                            modifier = Modifier.fillMaxWidth().testTag("retours_send_all"),
                        ) { Text(tr("Tout envoyer")) }
                        Text(tr("Une vidéo à la fois si ta messagerie refuse les gros envois."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                        OutlinedButton(
                            onClick = {
                                if (confirm) { Retours.deleteAll(); confirm = false; reload++ } else confirm = true
                            },
                            modifier = Modifier.fillMaxWidth().testTag("retours_delete"),
                        ) { Text(if (confirm) tr("Toucher encore pour tout effacer") else if (sent) tr("C'est envoyé ? Tout effacer") else tr("Effacer mes retours"), color = Neon.RedText) }
                    }
                }
                TextButton(onClick = close, modifier = Modifier.align(Alignment.End).testTag("retours_export_close")) { Text(tr("Fermer")) }
            }
        }
    }
}

private fun sizeOf(bytes: Long): String = tr("%.1f Mo", bytes / 1_000_000.0)

/** ⋯ › Aide: the mode on or off, and « Envoyer mes retours ». */
@Composable
fun RetoursHelpCard() {
    val context = LocalContext.current
    val settings by SettingsStore.state.collectAsState()
    NeonCard {
        SectionLabel(tr("Mode retours"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Une bulle pour filmer l'écran avec le son et écrire des notes, à envoyer ensuite."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
            Switch(
                checked = settings.retoursMode,
                onCheckedChange = { on -> SettingsStore.update(context) { it.copy(retoursMode = on) } },
                modifier = Modifier.testTag("help_retours_switch").semantics { contentDescription = tr("Mode retours") },
            )
        }
        OutlinedButton(onClick = { Retours.exportOpen.value = true }, modifier = Modifier.fillMaxWidth().testTag("help_retours_send")) {
            Text(tr("Envoyer mes retours"))
        }
    }
}
