package com.pocketstudio.app.remix

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material.icons.filled.ViewStream
import com.pocketstudio.ui.ScaledDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.AutoCut
import com.pocketstudio.model.CutMagnet
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.separation.Demucs
import com.pocketstudio.ui.LocalFolding
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.pillColors
import kotlinx.coroutines.launch

/*
 * Lot C (Benjamin's n° 24, mockup docs/maquettes/decoupe-deux.png, « C'est parfait »): « Découper
 * la chanson », cutting a stem into pieces the way a sampler slices: the stem chosen on top, the
 * whole song small, the part shown big with its numbered pieces; auto cuts, magnet, undo; « Couper
 * ici », « Début », « Fin », « Enlever », « Boucle »; the pieces listed, each heard in one touch,
 * ticked, then sent together to the pads, the Live, « Ma bibliothèque » or a track (through the
 * board's [StemBoardActions]: [StemDestination] only).
 */

/** The cut screen's state for [ui], from the cuts the project kept, keeping each change in it. */
@Composable
fun rememberCutModel(ui: StemBoardUi, actions: StemBoardActions): CutModel =
    remember(ui.setId, ui.frames) { CutModel(ui.setId, ui.frames, ui.bpm, ui.firstBeat, actions.savedCuts(ui.setId, ui.frames), actions::saveCuts) }

/** The order of the stem chips: the voice first, as on the mockup. */
private val CHIP_ORDER = listOf(3, 0, 1, 2)

@Composable
fun CutScreen(
    ui: StemBoardUi,
    actions: StemBoardActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    model: CutModel = rememberCutModel(ui, actions),
    view: StemBoardView = rememberStemBoardView(ui),
) {
    androidx.activity.compose.BackHandler(onBack = { actions.stop(); onClose() })
    val levels = remember(ui.setId, ui.frames, ui.peaks) { BoardPeaks(Demucs.STEMS.size) }
    LaunchedEffect(levels) { levels.load(ui.peaks, ui.peaksContext) }
    val work = actions.state.collectAsState()
    val scope = rememberCoroutineScope()
    var finding by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }

    /** Auto cuts [mode] on the part shown (the whole song when nothing is zoomed). */
    fun autoCut(mode: AutoCut, whole: Boolean = false) {
        if (mode == AutoCut.Bars && !model.hasGrid) {
            note = tr("Le tempo de la chanson n'est pas connu : pas de mesures. Essaie Phrases ou Attaques.")
            return
        }
        val from = if (whole || !view.zoomedIn) 0L else view.start.toLong()
        val to = if (whole || !view.zoomedIn) ui.frames else (view.start + view.width * view.framesPerPx).toLong()
        val stem = model.stem
        finding = true
        note = null
        scope.launch {
            val found = try { actions.autoCuts(ui.setId, stem, mode, from, to) } finally { finding = false }
            when {
                found == null -> note = tr("Les pistes de ce morceau ne sont plus sur l'appareil : sépare-le à nouveau.")
                found.isEmpty() -> note = tr("Aucune coupe trouvée ici : la piste est presque muette.")
                model.stem == stem -> model.applyAuto(mode, from, to, found)
            }
        }
    }

    // A stem never cut gets its own auto cuts the first time it is shown (the voice in phrases).
    LaunchedEffect(model.stem) { if (!model.cutYet) autoCut(model.defaultAuto(model.stem), whole = true) }

    CompositionLocalProvider(LocalFolding provides false) {
        Column(modifier.fillMaxSize().background(Neon.Background).testTag("cut_screen")) {
            CutHeader(ui, model, actions, work) { actions.stop(); onClose() }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StemChips(model) { actions.stop() }
                CutOverview(model, view, levels, Modifier.fillMaxWidth().height(40.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(shownPart(model, view), modifier = Modifier.weight(1f).testTag("cut_part"))
                    Text(tr("Pince pour zoomer"), style = MaterialTheme.typography.labelLarge, color = Neon.Cyan)
                }
                CutZoom(model, view, levels, work, onTouched = { note = null }, modifier = Modifier.fillMaxWidth().height(210.dp))
                AutoRow(model, finding, onAuto = { autoCut(it) })
                ToolRow(model, actions, work)
                PieceList(model, actions, work, Modifier.weight(1f))
            }
            SendBar(model, actions, work, note) { note = null }
        }
    }
}

@Composable
private fun CutHeader(ui: StemBoardUi, model: CutModel, actions: StemBoardActions, work: State<StemBoardState>, close: () -> Unit) {
    val playing = work.value.playing?.let { it.stem == model.stem && it.endFrame == ui.frames } == true
    Row(
        Modifier.fillMaxWidth().background(Neon.Surface).padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = close, modifier = Modifier.testTag("cut_close")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"), tint = Neon.Cyan)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Découper la chanson"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                // Lot « Aide à jour »: how to cut, by the title (the rows under it keep their height).
                com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.ScreenHelp.CUT_TOOLS, about = listOf("cut_here", "cut_start", "cut_end", "cut_remove", "cut_loop"))
            }
            Text(
                listOf(ui.name, boardClock(ui.frames).substringBefore('.')).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        // The stem from the play head, to cut while listening (« Couper ici » cuts where it is heard).
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(Neon.Cyan)
                .clickable(role = Role.Button) {
                    if (playing) actions.stop() else actions.play(StemSelection(ui.setId, model.stem, model.cursor.coerceAtMost(ui.frames - 1), ui.frames))
                }
                .semantics { contentDescription = if (playing) tr("Arrêter") else tr("Écouter") }
                .testTag("cut_play"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.size(8.dp))
    }
}

/** Voix, Batterie, Basse, Reste: each with its colour dot, as on the Remix page. */
@Composable
private fun StemChips(model: CutModel, onChange: () -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CHIP_ORDER.forEach { stem ->
            val name = Demucs.STEMS[stem]
            FilterChip(
                selected = model.stem == stem,
                onClick = { if (model.stem != stem) { onChange(); model.stem = stem } },
                label = { Text(StemStore.label(name), style = MaterialTheme.typography.titleSmall) },
                leadingIcon = { Box(Modifier.size(10.dp).background(stemDot(name), CircleShape)) },
                colors = pillColors(),
                modifier = Modifier.testTag("cut_stem_$stem"),
            )
        }
    }
}

private fun stemDot(stem: String): Color = when (stem) {
    "vocals" -> Color(0xFF8BC34A)
    "drums" -> Color(0xFF7986CB)
    "bass" -> Color(0xFFFF7043)
    else -> Color(0xFF29B6F6)
}

/** « Mes. 13 à 21 » (the bars on screen), or the seconds without a tempo. */
private fun shownPart(model: CutModel, view: StemBoardView): String {
    val from = view.start.toLong()
    val to = (view.start + view.width * view.framesPerPx).toLong().coerceAtMost(model.frames)
    if (!model.hasGrid) return tr("%1\$s à %2\$s", boardClock(from).substringBefore('.'), boardClock(to).substringBefore('.'))
    val bar = model.beat * 4
    val first = (Math.floorDiv(from - model.firstBeat, bar.toLong().coerceAtLeast(1)) + 1).coerceAtLeast(1)
    val last = (Math.floorDiv(to - 1 - model.firstBeat, bar.toLong().coerceAtLeast(1)) + 1).coerceAtLeast(first)
    return tr("Mes. %1\$d à %2\$d", first.toInt(), last.toInt())
}

private fun autoName(a: AutoCut): String = when (a) {
    AutoCut.Phrases -> tr("Phrases")
    AutoCut.Attacks -> tr("Attaques")
    AutoCut.Bars -> tr("Mesures")
}

private fun magnetName(m: CutMagnet): String = when (m) {
    CutMagnet.Beat -> tr("Au temps")
    CutMagnet.Bar -> tr("À la mesure")
    CutMagnet.Free -> tr("Libre")
}

/** « Coupes auto : Phrases ▾ », « Aimant : Temps ▾ », « ↶ Annuler ». */
@Composable
private fun AutoRow(model: CutModel, finding: Boolean, onAuto: (AutoCut) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Menu(
            label = tr("Coupes :"), value = if (finding) "…" else autoName(model.auto), tag = "cut_auto",
            options = AutoCut.entries.map { autoName(it) to { onAuto(it) } }, modifier = Modifier.weight(1f, fill = false),
        )
        Menu(
            label = tr("Aimant :"), value = magnetName(model.magnet), tag = "cut_magnet",
            options = (if (model.hasGrid) CutMagnet.entries else listOf(CutMagnet.Free)).map { m -> magnetName(m) to { model.magnet = m } },
            modifier = Modifier.weight(1f, fill = false),
        )
        com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.ScreenHelp.CUT_AUTO, about = listOf("cut_auto", "cut_magnet"))
        Spacer(Modifier.weight(0.01f))
        // The arrow alone: both menus keep their words on a phone.
        IconButton(onClick = model::undo, enabled = model.canUndo, modifier = Modifier.testTag("cut_undo")) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = tr("Annuler"), tint = if (model.canUndo) Neon.Cyan else Neon.TextSecondary)
        }
    }
}

@Composable
private fun Menu(label: String, value: String, tag: String, options: List<Pair<String, () -> Unit>>, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(Neon.SurfaceHigh)
                .clickable(role = Role.Button) { open = true }.padding(start = 10.dp, end = 4.dp).testTag(tag),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The value is what matters: the label gives way first on a narrow phone.
            Text(label + " ", style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, (name, pick) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { open = false; pick() }, modifier = Modifier.testTag("${tag}_$i"))
            }
        }
    }
}

/** « Couper ici » (the main action, orange), « Début », « Fin », « Enlever », « Boucle ». */
@Composable
private fun ToolRow(model: CutModel, actions: StemBoardActions, work: State<StemBoardState>) {
    // Where the tools act: what is heard of this stem while it plays, else the orange play head.
    fun here(): Long = work.value.playing?.takeIf { it.stem == model.stem && work.value.playFrame >= 0 }?.let { work.value.playFrame } ?: model.cursor
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tool(Icons.Filled.ContentCut, tr("Couper ici"), "cut_here", main = true, modifier = Modifier.weight(1.5f)) {
            val at = here()
            model.cursor = at
            model.cutHere(at)
        }
        Tool(Icons.Filled.FirstPage, tr("Début"), "cut_start", modifier = Modifier.weight(1f)) { model.cursor = here(); model.setStart() }
        Tool(Icons.Filled.LastPage, tr("Fin"), "cut_end", modifier = Modifier.weight(1f)) { model.cursor = here(); model.setEnd() }
        Tool(Icons.Filled.Delete, tr("Enlever"), "cut_remove", modifier = Modifier.weight(1f)) { actions.stop(); model.remove() }
        Tool(Icons.Filled.Loop, tr("Boucle"), "cut_loop", modifier = Modifier.weight(1f)) {
            val i = model.loop()
            val p = model.pieces.getOrNull(i)
            if (p != null && p.loop) model.selection(i)?.let { actions.play(it, loop = true) } else actions.stop()
        }
    }
}

@Composable
private fun Tool(icon: ImageVector, label: String, tag: String, modifier: Modifier, main: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.heightIn(min = 64.dp).clip(shape).background(if (main) Neon.Cyan else Neon.SurfaceHigh)
            .clickable(role = Role.Button, onClick = onClick).padding(vertical = 8.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val tint = if (main) Neon.OnAccent else Neon.TextPrimary
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

/** « Tes bouts · 6 », « Tout choisir », then the pieces three by three: number, length, tick, ▶. */
@Composable
private fun PieceList(model: CutModel, actions: StemBoardActions, work: State<StemBoardState>, modifier: Modifier) {
    val pieces = model.pieces
    val chosen = model.chosen.toSet()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Tes bouts · %d", pieces.size), modifier = Modifier.testTag("cut_count"))
            // Lot « Aide à jour »: in this row (48 dp, the button's): the screen keeps its height.
            com.pocketstudio.ui.HelpDot(
                com.pocketstudio.app.help.ScreenHelp.CUT_SEND,
                about = listOf("cut_to_pads", "cut_to_live", "cut_to_library", "cut_to_track", "cut_piece_*", "cut_all"),
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = model::chooseAll, enabled = pieces.isNotEmpty(), modifier = Modifier.testTag("cut_all")) {
                Text(if (chosen.size == pieces.size && pieces.isNotEmpty()) tr("Ne rien choisir") else tr("Tout choisir"), color = Neon.Cyan)
            }
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pieces.indices.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { i ->
                        val sel = model.selection(i)
                        val heard = work.value.playing?.let { p -> sel != null && p.stem == sel.stem && p.startFrame == sel.startFrame && p.endFrame == sel.endFrame } == true
                        PieceTile(i, pieces[i].frames, pieces[i].loop, i in chosen, heard, Modifier.weight(1f),
                            onTick = { model.toggleChosen(i) },
                            onPlay = { if (heard) actions.stop() else sel?.let { actions.play(it, loop = pieces[i].loop) } })
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PieceTile(i: Int, frames: Long, loop: Boolean, ticked: Boolean, heard: Boolean, modifier: Modifier, onTick: () -> Unit, onPlay: () -> Unit) {
    val color = pieceColor(i)
    val shape = RoundedCornerShape(14.dp)
    val ground = androidx.compose.ui.graphics.lerp(Neon.Surface, color, 0.28f)
    Box(
        modifier.heightIn(min = 76.dp).clip(shape).background(ground)
            .border(if (ticked) 2.dp else 0.dp, if (ticked) Neon.Cyan else Color.Transparent, shape)
            // Fix 2, n° 7: a checkbox says whether it is ticked.
            .toggleable(value = ticked, role = Role.Checkbox, onValueChange = { onTick() })
            .semantics { contentDescription = tr("Bout %d", i + 1) }
            .testTag("cut_piece_$i"),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 8.dp)) {
            Text((i + 1).toString() + if (loop) " ⟲" else "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
            Text(boardClock(frames), style = MonoStyle, color = Neon.TextSecondary)
        }
        Icon(
            if (ticked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank, contentDescription = null,
            tint = if (ticked) Neon.Cyan else Neon.TextSecondary,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp),
        )
        IconButton(onClick = onPlay, modifier = Modifier.align(Alignment.BottomEnd).size(40.dp).testTag("cut_piece_play_$i")) {
            Icon(if (heard) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (heard) tr("Arrêter") else tr("Écouter"),
                tint = if (heard) Neon.Acid else Neon.TextPrimary)
        }
    }
}

/** At the bottom: how many pieces are ticked and how long, then where they go; what was done, or why not. */
@Composable
private fun SendBar(model: CutModel, actions: StemBoardActions, work: State<StemBoardState>, note: String?, dismissNote: () -> Unit) {
    val sels = model.chosenSelections()
    val busy = work.value.busy
    val message = work.value.message
    Column(
        Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(18.dp)).background(Neon.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp).testTag("cut_send"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val said = busy ?: note ?: message
        if (said != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(said, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, modifier = Modifier.weight(1f).testTag("cut_message"))
                if (busy == null) TextButton(onClick = { dismissNote(); actions.dismissMessage() }, modifier = Modifier.testTag("cut_message_ok")) { Text("OK") }
            }
        }
        Text(
            if (sels.isEmpty()) tr("Coche des bouts pour les envoyer.")
            else trCount(sels.size, "1 bout choisi · %2\$s au total", "%1\$d bouts choisis · %2\$s au total", sels.size, boardClock(model.chosenFrames).substringBefore('.')),
            style = MaterialTheme.typography.titleSmall, color = if (sels.isEmpty()) Neon.TextSecondary else Neon.Cyan,
            modifier = Modifier.testTag("cut_chosen"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val enabled = sels.isNotEmpty() && busy == null
            Send(Icons.Filled.GridView, tr("Sur les pads"), "cut_to_pads", enabled, main = true) { actions.sendPieces(sels, StemDestination.Pad()) }
            Send(Icons.Filled.ViewStream, tr("Dans le Live"), "cut_to_live", enabled) { actions.sendPieces(sels, StemDestination.Live) }
            Send(Icons.Filled.Star, tr("Garder"), "cut_to_library", enabled) { actions.sendPieces(sels, StemDestination.Mine) }
            Send(Icons.Filled.ViewDay, tr("Sur une piste"), "cut_to_track", enabled) { actions.sendPieces(sels, StemDestination.Track()) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Send(icon: ImageVector, label: String, tag: String, enabled: Boolean, main: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val ground = when {
        !enabled -> Neon.SurfaceHigh.copy(alpha = 0.5f)
        main -> Neon.Cyan
        else -> Neon.SurfaceHigh
    }
    val tint = when {
        !enabled -> Neon.TextSecondary
        main -> Neon.OnAccent
        else -> Neon.TextPrimary
    }
    Column(
        Modifier.weight(1f).heightIn(min = 60.dp).clip(shape).background(ground)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(vertical = 6.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 2, textAlign = TextAlign.Center)
    }
}
