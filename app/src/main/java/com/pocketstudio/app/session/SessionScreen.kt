@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.pocketstudio.app.session

import com.pocketstudio.ui.ScaledAlertDialog

import com.pocketstudio.app.learn.spot
import com.pocketstudio.model.i18n.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import com.pocketstudio.ui.aspectRatioAtMost
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material.icons.filled.GraphicEq
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.model.Scene
import com.pocketstudio.model.SongPart
import com.pocketstudio.ui.FoldSection
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.WithHelp
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.pillColors

class SessionScreenUi(
    /** Every scene slot A..X: null when it does not exist yet. */
    val scenes: List<Scene?>,
    val current: Int,
    val pending: Int?,
    val song: List<SongPart>,
    /** The song part playing, -1 when not playing the song. */
    val songIndex: Int,
    val playing: Boolean,
    val tempoBpm: Double,
    /** Time signature n/4. */
    val beatsPerBar: Int = 4,
    /** What the scene shown is (new and empty, a copy of another…), said under the grid. */
    val sceneInfo: String = "",
    /** The timeline's rows: every track a song part can switch off. */
    val rows: List<TimelineRow> = emptyList(),
    /** The part lit up as playing (-1 while a part taken out of the song finishes). */
    val shownSongIndex: Int = songIndex,
    /** The part that comes next while the song plays (-1: none, or the song ends). */
    val upcoming: Int = -1,
    /** At the song's end: start over (true) or stop (false). */
    val songLoops: Boolean = false,
    /** While the song plays, the screens follow it (true) or stay on the scene picked (false). */
    val followSong: Boolean = true,
    /** The song step playing (16ths from the song's start), -1 outside the song: read only when drawn. */
    val songStep: () -> Int = { -1 },
    /** Each part's length in steps (null: from [barsOf]). */
    val partSteps: List<Int>? = null,
    /** The part sheet of part (index), from the project (null: built from what this screen knows). */
    val sheetOf: (Int) -> PartSheetUi? = { null },
    /** Versions of the song Live takes put aside (« Versions du morceau »). */
    val versions: Int = 0,
    /** Phase 37: each scene of the song with its chords' names (« Accords du morceau »); empty: not shown. */
    val chords: List<Pair<String, List<String>>> = emptyList(),
) {
    /** The part sheet of part [i]: from the project when given, else from the song and the scenes. */
    fun sheet(i: Int): PartSheetUi? = sheetOf(i) ?: song.getOrNull(i)?.let { part ->
        PartSheetUi(
            index = i, title = title(part.scene), bars = barsOf(part), played = song.count { it.scene == part.scene }, first = i == 0,
            repeats = if (part.bars == null) part.repeats else null, songSize = song.size,
        )
    }

    /** Each part's length in steps, and where it starts. */
    val partLengths: List<Int> get() = partSteps ?: song.map { barsOf(it) * stepsPerBar }
    val partStarts: List<Int> get() = partLengths.runningFold(0) { a, b -> a + b }

    val stepsPerBar get() = beatsPerBar * 4
    fun barsOf(part: SongPart) = part.bars ?: ((scenes.getOrNull(part.scene)?.stepsFor(stepsPerBar) ?: stepsPerBar) / stepsPerBar * part.repeats)

    /** The section name of scene [i] ("Refrain"), or "". */
    fun label(i: Int): String = scenes.getOrNull(i)?.label.orEmpty()

    /** "A · Refrain", or "A" alone. */
    fun title(i: Int): String = Scene.NAMES.getOrElse(i) { "?" } + label(i).takeIf { it.isNotBlank() }?.let { " · ${Scene.shownLabel(it)}" }.orEmpty()
}

/**
 * A timeline row: a track key (SongPart.DRUMS…), its name and colour. [spans]: an audio track's,
 * for each song part where its clip is really heard (fractions of the part); null for the other
 * tracks (a lit block plays all along). [peaks]: an audio take's waveform (0..1) when loaded, over
 * its [takeSteps]; [takeStart] the song step a song take starts at (-1: it starts again with each
 * stretch it is heard).
 */
class TimelineRow(
    val key: String,
    val name: String,
    val color: androidx.compose.ui.graphics.Color,
    val spans: List<List<ClosedFloatingPointRange<Float>>>? = null,
    val peaks: FloatArray? = null,
    val takeSteps: Int = 0,
    val takeStart: Int = -1,
)

class SessionActions(
    val select: (Int) -> Unit = {},
    val duplicate: () -> Unit = {},
    /** A new empty scene in the first free slot. */
    val newScene: () -> Unit = {},
    /** Empties the scene shown. */
    val clear: () -> Unit = {},
    /** Names scene (index) after a song section (blank: its letter alone). */
    val rename: (Int, String) -> Unit = { _, _ -> },
    /** Scene (first) dragged onto slot (second): moved or swapped, or put in its place (third: replace). */
    val moveScene: (Int, Int, Boolean) -> Unit = { _, _, _ -> },
    val addPart: (Int) -> Unit = {},
    /** A part playing scene (second) inserted at place (first). */
    val insertPart: (Int, Int) -> Unit = { _, _ -> },
    val duplicatePart: (Int) -> Unit = {},
    /** Part (first) moved to place (second). */
    val movePart: (Int, Int) -> Unit = { _, _ -> },
    val removePart: (Int) -> Unit = {},
    val setRepeats: (Int, Int) -> Unit = { _, _ -> },
    /** A touch on a part: the song plays from it (or it comes next). */
    val playPart: (Int) -> Unit = {},
    val toggleSong: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val setSongLoops: (Boolean) -> Unit = {},
    val setFollowSong: (Boolean) -> Unit = {},
    /** Live master effects (PerformanceFx parameter, value). */
    val performance: (Int, Float) -> Unit = { _, _ -> },
    /** The timeline: a track on or off in a song part. */
    val togglePartTrack: (Int, String) -> Unit = { _, _ -> },
    /** The timeline's long press: in part (first), track (second) plays scene (third). */
    val setPartScene: (Int, String, Int) -> Unit = { _, _, _ -> },
    val setBeatsPerBar: (Int) -> Unit = {},
    /** The Live page (full screen): every track's sections on pads, effects and 303 under the fingers. */
    val openLive: () -> Unit = {},
    /** « Ce qui joue »: everything acting on the sound of a scene (null: of the whole song). */
    val whatPlays: (Int?) -> Unit = {},
    /** « Ligne du temps »: the whole song, every track, full screen. */
    val openTimeline: () -> Unit = {},
    /** The part sheet: an intention done on part (index), one undo step. */
    val partIntent: (Int, PartIntent) -> Unit = { _, _ -> },
    /** « Versions du morceau »: the songs Live takes put aside. */
    val openVersions: () -> Unit = {},
    /** Phase 37: « Accords du morceau ». */
    val openChords: () -> Unit = {},
)

/**
 * The song first (the arrangement), then the scenes it is made of, the timeline, and the live tools.
 * Phase 36: [arrangeOnly] in the Structure space (no way to Live nor to the timeline: Live is a
 * space of its own and the timeline is this view's other half); [simple] in the Mode simple (the
 * song and [tracks], the tracks' card, without the scenes nor the song's rare settings).
 */
@Composable
fun SessionScreen(
    ui: SessionScreenUi, actions: SessionActions, live: LivePadsUi? = null, liveActions: LivePadsActions = LivePadsActions(),
    arrangeOnly: Boolean = false,
    simple: Boolean = false,
    tracks: (@Composable () -> Unit)? = null,
    /** Above everything, in the first pane (the « Ligne du temps / Parties » switch). */
    top: (@Composable () -> Unit)? = null,
) {
    var renaming by rememberSaveable { mutableStateOf<Int?>(null) }
    val entries = !arrangeOnly && !simple
    // On a tablet the song stays on the left; the scenes, the timeline and the live tools on the right.
    com.pocketstudio.ui.AdaptivePanes(
        Modifier.testTag("session"),
        spacing = 12.dp,
        first = {
            top?.invoke()
            if (entries) LiveEntry(actions.openLive)
            if (entries) TimelineEntry(ui, actions)
            SongCard(ui, actions, simple)
        },
        // A wide screen: the live pads right under the scenes, in reach without scrolling.
        second = {
            if (simple) {
                tracks?.invoke()
            } else {
                ScenesCard(ui, actions, rename = { renaming = it })
                if (entries) LiveSection(live, liveActions, actions)
            }
        },
        // A phone keeps its order: the ways to Live and to the timeline, the song, the scenes, then the live tools.
        phone = {
            top?.invoke()
            if (entries) LiveEntry(actions.openLive)
            if (entries) TimelineEntry(ui, actions)
            // The Mode simple: the tracks first (their sounds and volumes, what a beginner changes), then the parts.
            if (simple) tracks?.invoke()
            SongCard(ui, actions, simple)
            if (!simple) {
                ScenesCard(ui, actions, rename = { renaming = it })
                if (entries) LiveSection(live, liveActions, actions)
            }
        },
    )
    renaming?.let { i -> RenameSceneDialog(i, ui.label(i), onDone = { label -> actions.rename(i, label); renaming = null }, onDismiss = { renaming = null }) }
}

/** The way to the Live page: one wide button, on top. */
@Composable
private fun LiveEntry(open: () -> Unit) {
    Button(
        onClick = open,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("open_live"),
        colors = ButtonDefaults.buttonColors(containerColor = Neon.Cyan, contentColor = Neon.OnAccent),
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(tr("Live : jouer les parties piste par piste"), style = MaterialTheme.typography.titleSmall)
            Text(tr("Pads de toutes les pistes, effets et 303 sous les doigts"), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * The way to the « Ligne du temps »: a panel with the whole song in miniature (a stripe per
 * track, lit where it plays, the playhead moving); a touch opens it full screen.
 */
@Composable
private fun TimelineEntry(ui: SessionScreenUi, actions: SessionActions) {
    if (ui.song.isEmpty() || ui.rows.isEmpty()) return
    val shape = RoundedCornerShape(16.dp)
    val open = tr("Ouvrir la ligne du temps")
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Neon.SurfaceHigh, Neon.Surface)))
            .border(1.dp, Neon.Magenta.copy(alpha = 0.45f), shape)
            .clickable(role = Role.Button, onClickLabel = open, onClick = actions.openTimeline)
            .padding(12.dp)
            .testTag("open_timeline"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timeline, contentDescription = null, tint = Neon.Magenta)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                WithHelp(Glossary.TIMELINE) {
                    Text(tr("Ligne du temps"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
                }
                Text(tr("Toutes les pistes du morceau, partie par partie, en plein écran"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Neon.TextSecondary)
        }
        TimelineMiniMap(ui)
    }
}

/** The song in miniature: a thin stripe per track over its parts, lit where it plays; the playhead read when drawn. */
@Composable
private fun TimelineMiniMap(ui: SessionScreenUi) {
    val rows = ui.rows
    val lengths = ui.partLengths
    val starts = ui.partStarts
    val total = starts.last().coerceAtLeast(1)
    val track = Color.Black.copy(alpha = 0.25f)
    val playing = Neon.Magenta
    val head = Neon.TextPrimary
    val height = (rows.size * 7).coerceIn(28, 84).dp
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(6.dp)).testTag("timeline_minimap")) {
        drawRect(track)
        val rowH = size.height / rows.size
        val gap = 1.dp.toPx()
        ui.song.forEachIndexed { i, part ->
            val left = size.width * starts[i] / total
            val w = (size.width * lengths.getOrElse(i) { 0 } / total - gap).coerceAtLeast(1f)
            if (i == ui.shownSongIndex) drawRect(playing.copy(alpha = 0.18f), androidx.compose.ui.geometry.Offset(left, 0f), androidx.compose.ui.geometry.Size(w, size.height))
            rows.forEachIndexed { r, row ->
                if (!part.plays(row.key)) return@forEachIndexed
                drawRect(
                    row.color.copy(alpha = if (i == ui.shownSongIndex) 0.95f else 0.7f),
                    androidx.compose.ui.geometry.Offset(left, r * rowH + gap / 2), androidx.compose.ui.geometry.Size(w, (rowH - gap).coerceAtLeast(1f)),
                )
            }
        }
        val s = ui.songStep()
        if (s >= 0) {
            val x = size.width * (s + 1).toFloat() / total
            drawLine(head, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 2.dp.toPx())
        }
    }
}

@Composable
private fun LiveSection(live: LivePadsUi?, liveActions: LivePadsActions, actions: SessionActions) {
    FoldSection(tr("Jouer en direct")) {
        if (live != null) LivePadsCard(live, liveActions)
        LivePad(actions.performance)
    }
}

@Composable
private fun SongCard(ui: SessionScreenUi, actions: SessionActions, simple: Boolean = false) {
    var inserting by rememberSaveable { mutableStateOf<Int?>(null) }
    NeonCard(accent = Neon.Magenta) {
        SectionLabel(tr("Le morceau"), color = Neon.Magenta, help = Glossary.SONG)
        Text(
            when {
                ui.song.isEmpty() -> tr("Ton morceau est vide : ajoute tes parties dans l'ordre où elles jouent (Intro, Couplet, Refrain…).")
                ui.songIndex >= 0 -> tr("Touche une partie : elle joue juste après celle en cours.")
                else -> tr("Touche une partie pour écouter le morceau à partir d'elle.")
            },
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        // What Live takes put aside: one touch to see and bring back an earlier song.
        if (ui.versions > 0) {
            TextButton(onClick = actions.openVersions, modifier = Modifier.heightIn(min = 48.dp).testTag("song_versions")) {
                Text(tr("Versions du morceau (%d)", ui.versions))
            }
        }
        // Where the song is (time, bar, a line over the parts), right above them.
        val totalBars = ui.song.sumOf { ui.barsOf(it) }
        if (totalBars > 0) SongClock(ui, totalBars)
        // Phase 37: the song's chords, every part, a touch from changing them all.
        if (ui.song.isNotEmpty() && ui.chords.isNotEmpty()) SongChordsStrip(ui.chords, actions.openChords)
        ui.song.forEachIndexed { i, part -> PartRow(i, part, ui, actions, insertAfter = { inserting = it }) }
        Text(if (ui.song.isEmpty()) tr("Ajouter :") else tr("Ajouter à la fin :"), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.scenes.forEachIndexed { i, s ->
                if (s != null) OutlinedButton(onClick = { actions.addPart(i) }, enabled = ui.song.size < SongPart.MAX_PARTS, modifier = Modifier.testTag("add_part_$i")) {
                    Text("+ ${ui.title(i)}", maxLines = 1, softWrap = false)
                }
            }
        }
        // ▶ in the top bar plays the song (a part touched above plays from it): no second button here.
        // Everything acting on the sound (effects, sends, curves), each with a way to where it is set.
        val playingScene = ui.song.getOrNull(ui.shownSongIndex)?.scene
        OutlinedButton(
            onClick = { actions.whatPlays(playingScene) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("song_what_plays"),
        ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  " + if (playingScene != null) tr("Ce qui joue dans %s", ui.title(playingScene)) else tr("Ce qui joue dans le morceau"), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!simple) FoldSection(tr("Réglages du morceau"), subtle = true, spacing = 8.dp) {
            ChoiceRow(tr("À la fin :"), tr("s'arrêter"), tr("recommencer"), ui.songLoops, actions.setSongLoops, "song_end")
            ChoiceRow(tr("Pendant la lecture :"), tr("suivre le morceau"), tr("rester sur ma partie"), !ui.followSong, { actions.setFollowSong(!it) }, "follow")
            Text(
                if (ui.followSong) tr("Les écrans montrent la partie qui joue.")
                else tr("Les écrans restent sur la partie choisie : tu la modifies pendant que le morceau continue."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
            WithHelp(Glossary.TIME_SIGNATURE) {
                Text(tr("Temps par mesure :"), style = MaterialTheme.typography.bodyMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (2..7).forEach { b ->
                    FilterChip(
                        selected = ui.beatsPerBar == b, onClick = { actions.setBeatsPerBar(b) }, label = { Text("$b/4") },
                        colors = pillColors(), modifier = Modifier.testTag("meter_$b"),
                    )
                }
            }
            Text(
                tr("4/4 pour la plupart des morceaux, 3/4 pour une valse, 6/4 ou 7/4 pour des rythmes plus rares."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
    inserting?.let { after ->
        ScenePickerDialog(
            title = tr("Insérer après la partie %d", after + 1),
            ui = ui,
            onPick = { scene -> actions.insertPart(after + 1, scene); inserting = null },
            onDismiss = { inserting = null },
        )
    }
}

/** "1:05" from seconds. */
private fun clock(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)

/**
 * Where the song is: the time gone and the whole length, the bar and beat, and a line over the
 * whole song (its parts marked, the playhead moving). The words change once a beat at most; the
 * line is drawn from the step playing, read only when drawn.
 */
@Composable
private fun SongClock(ui: SessionScreenUi, totalBars: Int) {
    val spb = ui.stepsPerBar
    val totalSteps = ui.partStarts.last().coerceAtLeast(1)
    val beatSeconds = 60.0 / ui.tempoBpm
    val total = (totalSteps / 4 * beatSeconds).toInt()
    val beat by remember(ui.songStep) { derivedStateOf { ui.songStep().let { if (it < 0) -1 else it / 4 } } }
    val playing = ui.playing && beat >= 0
    Row(Modifier.fillMaxWidth().testTag("song_clock"), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (playing) clock((beat * beatSeconds).toInt()) + " / " + clock(total)
            else (if (totalBars == 1) tr("1 mesure") else tr("%d mesures", totalBars)) + " · " + clock(total),
            style = MonoStyle, fontSize = if (playing) 18.sp else MonoStyle.fontSize,
            color = if (playing) Neon.TextPrimary else Neon.TextSecondary, modifier = Modifier.weight(1f),
        )
        if (playing) {
            val beatsPerBar = (spb / 4).coerceAtLeast(1)
            Text(tr("mes. %1\$d.%2\$d", beat / beatsPerBar + 1, beat % beatsPerBar + 1) + " / " + totalBars, style = MonoStyle, fontSize = 18.sp, color = Neon.Magenta)
        }
    }
    val track = Neon.SurfaceHigh
    val done = Neon.Magenta
    val tick = Neon.Outline.copy(alpha = 0.55f)
    val head = Neon.TextPrimary
    val starts = ui.partStarts
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(14.dp).testTag("song_progress")) {
        val h = 6.dp.toPx()
        val y = (size.height - h) / 2
        val r = androidx.compose.ui.geometry.CornerRadius(h / 2)
        drawRoundRect(track, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, h), r)
        val s = ui.songStep()
        val f = if (s < 0) 0f else (s + 1).toFloat() / totalSteps
        if (f > 0f) drawRoundRect(done, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(size.width * f.coerceIn(0f, 1f), h), r)
        // Where each part starts.
        for (st in starts.drop(1).dropLast(1)) {
            val x = size.width * st / totalSteps
            drawLine(tick, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 2f)
        }
        if (s >= 0) drawCircle(head, radius = size.height / 2, center = androidx.compose.ui.geometry.Offset(size.width * f.coerceIn(0f, 1f), size.height / 2))
    }
}

/** "2 mes." ("1 mes.": one bar, singular in other languages). */
private fun barsLabel(bars: Int) = com.pocketstudio.app.ui.barsShort(bars)

/** A label and two choices side by side ([second] chosen when [secondOn]). */
@Composable
private fun ChoiceRow(label: String, first: String, second: String, secondOn: Boolean, choose: (Boolean) -> Unit, tag: String) {
    FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 2.dp))
        FilterChip(selected = !secondOn, onClick = { choose(false) }, label = { Text(first) }, colors = pillColors(), modifier = Modifier.testTag("${tag}_0"))
        FilterChip(selected = secondOn, onClick = { choose(true) }, label = { Text(second) }, colors = pillColors(), modifier = Modifier.testTag("${tag}_1"))
    }
}

/** One line of the song: a touch plays from it; repeats; a long press or ⋮ opens its sheet (intentions, move, copy, remove). */
@Composable
private fun PartRow(i: Int, part: SongPart, ui: SessionScreenUi, actions: SessionActions, insertAfter: (Int) -> Unit) {
    val active = i == ui.shownSongIndex
    val next = i == ui.upcoming
    val shape = RoundedCornerShape(10.dp)
    val fill = Neon.Magenta.copy(alpha = 0.22f)
    val start = ui.partStarts.getOrElse(i) { 0 }
    val length = ui.partLengths.getOrElse(i) { 1 }.coerceAtLeast(1)
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(shape)
            .background(if (active) Neon.Magenta.copy(alpha = 0.16f) else Color.Transparent)
            // How far the part playing has got: filled from the left, read only when drawn.
            .then(
                if (active) Modifier.drawBehind {
                    val s = ui.songStep()
                    if (s >= start) drawRect(fill, size = androidx.compose.ui.geometry.Size(size.width * ((s - start + 1).toFloat() / length).coerceIn(0f, 1f), size.height))
                } else Modifier,
            )
            .border(1.dp, if (next) Neon.Amber else if (active) Neon.Magenta.copy(alpha = 0.6f) else Color.Transparent, shape)
            .testTag("part_$i"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp).clip(shape)
                .combinedClickable(
                    role = Role.Button, onClickLabel = tr("Jouer à partir de cette partie"),
                    onLongClickLabel = tr("Variation, fill, break…"), onLongClick = { sheetOpen = true },
                ) { actions.playPart(i) }
                .padding(horizontal = 6.dp)
                .spot("part_play_$i", if (i == 0) tr("Jouer · appui long : Fill") else null)
                .testTag("part_play_$i"),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = if (active) Neon.Magenta else Neon.TextSecondary, modifier = Modifier.size(20.dp))
            Column(Modifier.padding(start = 6.dp)) {
                // A narrow row (a big text size): the name goes on a second line, never « B · Co… ».
                Text(
                    ui.title(part.scene), fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (active) Neon.Magenta else Neon.TextPrimary,
                )
                Text(
                    (if (part.bars == null) "× ${part.repeats}  ·  " else "") + barsLabel(ui.barsOf(part)) +
                        (if (part.mixed) "  ·  " + tr("mélange") else "") + when {
                        active -> "  ·  " + tr("joue")
                        next -> "  ·  " + tr("ensuite")
                        else -> ""
                    },
                    style = MonoStyle, maxLines = 1,
                    color = when {
                        next -> Neon.Amber
                        active -> Neon.Magenta
                        else -> Neon.TextSecondary
                    },
                )
            }
        }
        // A part written from the live grid has its own length: no repeats.
        IconButton(onClick = { actions.setRepeats(i, part.repeats - 1) }, enabled = part.bars == null && part.repeats > 1) { Icon(Icons.Filled.Remove, contentDescription = tr("Une fois de moins")) }
        IconButton(onClick = { actions.setRepeats(i, part.repeats + 1) }, enabled = part.bars == null && part.repeats < SongPart.MAX_REPEATS) { Icon(Icons.Filled.Add, contentDescription = tr("Une fois de plus")) }
        IconButton(onClick = { sheetOpen = true }, modifier = Modifier.testTag("part_menu_$i")) { Icon(Icons.Filled.MoreVert, contentDescription = tr("Plus d'actions")) }
    }
    if (sheetOpen) {
        val sheet = ui.sheet(i)
        if (sheet == null) {
            sheetOpen = false
        } else {
            val full = ui.song.size >= SongPart.MAX_PARTS
            PartSheet(
                sheet, onIntent = { actions.partIntent(i, it) }, close = { sheetOpen = false },
                more = PartMoreActions(
                    whatPlays = { actions.whatPlays(part.scene) },
                    moveUp = if (i > 0) ({ actions.movePart(i, i - 1) }) else null,
                    moveDown = if (i < ui.song.lastIndex) ({ actions.movePart(i, i + 1) }) else null,
                    duplicate = if (!full) ({ actions.duplicatePart(i) }) else null,
                    insertAfter = if (!full) ({ insertAfter(i) }) else null,
                    remove = { actions.removePart(i) },
                ),
            )
        }
    }
}

@Composable
private fun ScenesCard(ui: SessionScreenUi, actions: SessionActions, rename: (Int) -> Unit) {
    NeonCard {
        // Lot 2: « Briques », the grid of A to H; « Parties » is the song's list (two « Parties » side by side before).
        // Its foldKey stays the former title: a card folded before stays folded.
        SectionLabel(tr("Briques"), help = Glossary.BRICK, foldKey = "Parties")
        Text(
            when {
                ui.playing && ui.songIndex >= 0 && !ui.followSong -> tr("Touche une brique pour la modifier : le morceau continue de jouer.")
                ui.playing -> tr("Touche une brique : elle prend le relais à la mesure suivante.")
                else -> tr("Une brique = la batterie, l'acide et les notes d'une partie. Les parties du morceau en sont faites. Touche-en une pour la modifier, maintiens et glisse pour la déplacer.")
            },
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        // 8 slots by default; 16 or 24 on demand, and never fewer than the scenes in use.
        val used = ui.scenes.indexOfLast { it != null } + 1
        var chosen by rememberSaveable { mutableIntStateOf(Scene.GRID_SIZES.first()) }
        val shown = Scene.GRID_SIZES.firstOrNull { it >= maxOf(chosen, used, ui.current + 1) } ?: Scene.MAX
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Afficher :"), style = MaterialTheme.typography.bodyMedium)
            Scene.GRID_SIZES.forEach { size ->
                FilterChip(
                    selected = size == shown,
                    onClick = { chosen = size },
                    enabled = size >= used,
                    label = { Text("$size") },
                    modifier = Modifier.testTag("scene_grid_$size"),
                )
            }
        }
        SceneGrid(ui, shown, actions)
        // What the scene shown is: a new one starts empty, a copy says whose copy it is.
        if (ui.sceneInfo.isNotEmpty()) {
            Text(ui.sceneInfo, style = MaterialTheme.typography.bodyMedium, color = Neon.Cyan, modifier = Modifier.testTag("scene_info"))
        }
        val letter = Scene.NAMES[ui.current]
        val named = ui.label(ui.current).isNotBlank()
        val empty = ui.scenes.getOrNull(ui.current)?.isEmpty != false
        // The scene's four actions on one compact row of tools (icon over a short word), the full
        // sentence kept for TalkBack.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SceneAction(Icons.Filled.Edit, if (named) tr("Renommer") else tr("Nommer"),
                if (named) tr("Renommer la brique %s", letter) else tr("Nommer la brique %s", letter), true, { rename(ui.current) }, "scene_rename", Modifier.weight(1f))
            SceneAction(Icons.Filled.Add, tr("Nouvelle"), tr("Nouvelle brique vide"), true, actions.newScene, "scene_new", Modifier.weight(1f))
            SceneAction(Icons.Filled.ContentCopy, tr("Dupliquer"), tr("Dupliquer la brique %s", letter), true, actions.duplicate, "scene_duplicate", Modifier.weight(1f))
            SceneAction(Icons.Filled.DeleteSweep, tr("Vider"), tr("Vider la brique %s", letter), !empty, actions.clear, "scene_clear", Modifier.weight(1f))
        }
    }
}

/** A scene being dragged: where it comes from, the finger, and every slot's place (in the root's coordinates). */
@Stable
private class SceneDrag(private val onDrop: (from: Int, to: Int) -> Unit) {
    var from by mutableStateOf<Int?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    val cells = mutableMapOf<Int, Rect>()

    fun start(i: Int, at: Offset) {
        from = i
        pointer = (cells[i]?.topLeft ?: Offset.Zero) + at
    }

    /** The slot under the finger, or null. */
    fun target(): Int? = cells.entries.firstOrNull { it.value.contains(pointer) }?.key

    fun drop() {
        val f = from ?: return
        val t = target()
        from = null
        if (t != null && t != f) onDrop(f, t)
    }
}

/**
 * The scene slots, four per row. Held and dragged, a scene follows the finger: dropped on an
 * empty slot it moves there; on another scene, a question: swap them, replace it, or nothing.
 */
@Composable
private fun SceneGrid(ui: SessionScreenUi, shown: Int, actions: SessionActions) {
    val latest by rememberUpdatedState(ui)
    val latestActions by rememberUpdatedState(actions)
    var asking by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val drag = remember {
        SceneDrag { from, to ->
            if (latest.scenes.getOrNull(to)?.isEmpty != false) latestActions.moveScene(from, to, false) else asking = from to to
        }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxWidth().onGloballyPositioned { origin = it.boundsInRoot().topLeft }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (0 until shown step 4).forEach { start ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in start until start + 4) SceneButton(i, ui, actions, drag, Modifier.weight(1f))
                }
            }
        }
        // The scene carried, just above the finger (not hidden under it).
        drag.from?.let { i ->
            val density = androidx.compose.ui.platform.LocalDensity.current
            val w = with(density) { 72.dp.toPx() }
            val h = with(density) { 60.dp.toPx() }
            val lift = with(density) { 24.dp.toPx() }
            Box(
                Modifier
                    .offset { IntOffset((drag.pointer.x - origin.x - w / 2).roundToInt(), (drag.pointer.y - origin.y - h - lift).roundToInt()) }
                    .size(72.dp, 60.dp)
                    .shadow(12.dp, RoundedCornerShape(12.dp), ambientColor = Neon.Cyan, spotColor = Neon.Cyan)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Neon.SurfaceHigh)
                    .border(2.dp, Neon.Cyan, RoundedCornerShape(12.dp))
                    .testTag("scene_drag_ghost"),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Scene.NAMES[i], fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, lineHeight = 24.sp)
                    if (ui.label(i).isNotBlank()) {
                        Text(Scene.shownLabel(ui.label(i)), fontSize = 12.sp, color = Neon.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
    asking?.let { (from, to) ->
        MoveSceneDialog(from, to, ui, onChoose = { replace -> actions.moveScene(from, to, replace); asking = null }, onDismiss = { asking = null })
    }
}

/** A scene dropped on another one: swap them, or replace it (Annuler brings it back), or nothing. */
@Composable
private fun MoveSceneDialog(from: Int, to: Int, ui: SessionScreenUi, onChoose: (replace: Boolean) -> Unit, onDismiss: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Déplacer %s sur %s", ui.title(from), ui.title(to))) },
        text = {
            Text(
                tr("La partie %s n'est pas vide.", Scene.NAMES[to]) + " " + tr("La remplacer efface aussi son automation et ses prises audio (Annuler les rend)."),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { onChoose(false) }, modifier = Modifier.heightIn(min = 48.dp).testTag("scene_move_swap")) { Text(tr("Échanger les parties")) }
                TextButton(onClick = { onChoose(true) }, modifier = Modifier.heightIn(min = 48.dp).testTag("scene_move_replace")) {
                    Text(tr("Remplacer la partie %s", Scene.NAMES[to]))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("scene_move_cancel")) { Text(tr("Annuler")) }
            }
        },
    )
}

/** One of the scene's tools: a small icon over a short word, the whole action said to TalkBack. */
@Composable
private fun SceneAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector, short: String, full: String, enabled: Boolean,
    onClick: () -> Unit, tag: String, modifier: Modifier,
) {
    val tint = if (enabled) Neon.TextPrimary else Neon.TextSecondary.copy(alpha = 0.5f)
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Neon.TextSecondary.copy(alpha = if (enabled) 0.45f else 0.2f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = full, onClick = onClick)
            .semantics { contentDescription = full }
            .heightIn(min = 52.dp)
            .padding(vertical = 6.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(short, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1, softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
private fun SceneButton(i: Int, ui: SessionScreenUi, actions: SessionActions, drag: SceneDrag, modifier: Modifier) {
    val scene = ui.scenes.getOrNull(i)
    val current = i == ui.current
    val pending = i == ui.pending
    val empty = scene == null || scene.isEmpty
    // The scene shown glows in its colour; filled scenes stand out, empty ones are hollow.
    val lit = if (ui.playing) Neon.Magenta else Neon.Cyan
    val (container, content) = when {
        current -> lit to Neon.OnAccent
        empty -> Color.Black.copy(alpha = 0.22f) to Neon.TextSecondary
        else -> Neon.SurfaceHigh to Neon.TextPrimary
    }
    val shape = RoundedCornerShape(12.dp)
    val label = ui.label(i)
    val dragged = drag.from == i
    val target = drag.from != null && drag.target() == i && !dragged
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        // Capped on a wide pane (tablet), so 24 scenes still fit on the screen.
        modifier.aspectRatioAtMost(1.2f, 84.dp)
            .onGloballyPositioned { drag.cells[i] = it.boundsInRoot() }
            .graphicsLayer { alpha = if (dragged) 0.35f else 1f }
            .then(if (current) Modifier.shadow(if (ui.playing) 16.dp else 8.dp, shape, ambientColor = lit, spotColor = lit) else Modifier)
            .clip(shape)
            .background(container)
            .border(
                when {
                    target -> BorderStroke(3.dp, Neon.Cyan)
                    pending -> BorderStroke(3.dp, Neon.Amber)
                    current -> BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    empty -> BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
                    else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                },
                shape,
            )
            // A touch shows (or launches) the scene; held, it follows the finger to another slot
            // (inside the clickable: a drop consumes the touch, so it is not also a tap).
            .clickable(role = Role.Button, onClickLabel = tr("Choisir la partie"), onClick = { actions.select(i) })
            .then(
                // Only a scene with something in it (or a name) can be carried.
                if (scene == null || (empty && label.isBlank())) Modifier
                else Modifier.pointerInput(i) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { at ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            drag.start(i, at)
                        },
                        onDrag = { change, amount -> change.consume(); drag.pointer += amount },
                        onDragEnd = { drag.drop() },
                        onDragCancel = { drag.from = null },
                    )
                },
            )
            .padding(2.dp)
            .testTag("scene_$i"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Scene.NAMES[i], fontSize = 22.sp, fontWeight = FontWeight.Bold, color = content, lineHeight = 24.sp)
            if (label.isNotBlank()) {
                Text(Scene.shownLabel(label), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis, lineHeight = 14.sp)
            }
            Text(
                when {
                    pending -> tr("ensuite")
                    empty -> tr("vide")
                    else -> barsLabel(scene!!.stepsFor(ui.stepsPerBar) / ui.stepsPerBar)
                },
                fontSize = 11.sp, color = content.copy(alpha = 0.8f), maxLines = 1, lineHeight = 13.sp,
            )
        }
    }
}

/** Names scene [index]: a song section in one touch, or any name typed. */
@Composable
internal fun RenameSceneDialog(index: Int, current: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember(index) { mutableStateOf(current) }
    ScaledAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Nommer la partie %s", Scene.NAMES.getOrElse(index) { "?" })) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Quel moment du morceau est-ce ?"), style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Scene.SECTIONS.forEach { s ->
                        FilterChip(
                            selected = name == s, onClick = { name = s }, label = { Text(tr(s)) },
                            colors = pillColors(), modifier = Modifier.testTag("section_$s"),
                        )
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(Scene.MAX_LABEL) }, singleLine = true,
                    label = { Text(tr("Ou un autre nom")) }, modifier = Modifier.fillMaxWidth().testTag("scene_name_field"),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onDone(name) }, modifier = Modifier.testTag("scene_name_ok")) { Text(tr("Valider")) } },
        dismissButton = {
            Row {
                if (current.isNotBlank()) TextButton(onClick = { onDone("") }) { Text(tr("Sans nom")) }
                TextButton(onClick = onDismiss) { Text(tr("Annuler")) }
            }
        },
    )
}

/** Picks one of the scenes in use (to insert it in the song). */
@Composable
private fun ScenePickerDialog(title: String, ui: SessionScreenUi, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    ScaledAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.scenes.forEachIndexed { i, s ->
                    if (s != null) OutlinedButton(onClick = { onPick(i) }, modifier = Modifier.heightIn(min = 48.dp).testTag("pick_scene_$i")) {
                        Text(ui.title(i), maxLines = 1, softWrap = false)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Annuler")) } },
    )
}
