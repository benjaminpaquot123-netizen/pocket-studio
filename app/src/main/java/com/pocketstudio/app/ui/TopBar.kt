package com.pocketstudio.app.ui

import com.pocketstudio.ui.ScaledDropdownMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pocketstudio.model.Scene
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.liveGlow
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.learn.spot
import androidx.compose.material.icons.filled.MoreVert

/**
 * The scene being edited, shown as a chip in the top bar: [current] is its index, [titles] every
 * scene of the session as it is shown ("A · Intro", "B"), [empty] those with nothing in them yet.
 */
class SceneChipUi(val current: Int, val titles: List<String>, val empty: Set<Int> = emptySet())

class TopBarUi(
    val projectName: String,
    val playing: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    /** Live recording armed (what is played is written into the loop), or a microphone take on. */
    val recording: Boolean = false,
    /** The ● records the microphone on an audio track (the Audio tracks screen, or a take on). */
    val micTake: Boolean = false,
    /** Audit ultra round 1 (n° 30): what the ● records, in words (null: the words of before). */
    val recordLabel: String? = null,
    /** The scene chip, on the screens that edit a scene (null: not shown). */
    val scene: SceneChipUi? = null,
    /** The project is saved (on its own, after each change): a small "✓ Enregistré" under its name. */
    val saved: Boolean = false,
    /** Recording into a scene that is not the one shown (staying on a scene during the song): its name, said under the project's. */
    val recordInto: String? = null,
    /** Redo offered when there is room (false: the Mode simple's lighter bar, ↶ alone). */
    val redo: Boolean = true,
    /** The project's mode chip (« Simple » or « Pro », owner 2026-10-29): true Simple, false Pro, null not shown. */
    val mode: Boolean? = null,
    /**
     * Relecture tour 1 (intégration): redo has nowhere else to be (the thumb's bar gave its room to the
     * Guide's panel on a phone under 820 dp): it keeps its place here before the extras and the name.
     */
    val redoFirst: Boolean = false,
    /**
     * ↶ here; false: it is at the thumb, in the thumb's bar right under the page (audit ultra round 2, n° 15, the Mode
     * simple: its 48 dp go to the project's name and « ✓ Sauvegardé », cut to « Mon pr… » and a lone tick before).
     */
    val undo: Boolean = true,
    /**
     * Tour 5, chantier 4 (Benjamin's 2B): the top ▶ is « ▶ cette partie » (the part open, looped); it shows ■ while that
     * part loops. Defaults to [playing] (one ▶ for whatever plays, as before).
     */
    val partPlaying: Boolean = playing,
    /** « ▶ le morceau » here too: no thumb's bar to hold it (lying down, a tablet, the keys on a short screen). */
    val songButton: Boolean = false,
    /** The song plays (not one part looped): « ▶ le morceau » shows ■. */
    val songPlaying: Boolean = playing,
)

class TopBarActions(
    val projects: () -> Unit = {},
    /** A long press on the project's name: rename it there. */
    val rename: () -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    /** « ▶ le morceau » ([TopBarUi.songButton]). */
    val playSong: () -> Unit = {},
    val toggleRecord: () -> Unit = {},
    val selectScene: (Int) -> Unit = {},
    val newScene: () -> Unit = {},
    /** ⤴ « Prêt à partager ? »: the song checked, then shared (on every tab). */
    val share: () -> Unit = {},
    /**
     * Phase 36, Mode simple: « ⋮ » in place of ⤴ (the « Partager » tab shares there). Tour 5, chantier 5 (G15): it opens
     * the Plus page, the same as Pro's (with the Recette), no menu of bare lines any more. Null: ⤴ as usual.
     */
    val more: (() -> Unit)? = null,
    /** A touch on the mode chip: the change of mode. */
    val mode: () -> Unit = {},
)

/** How the scene chip shows: not at all, its letter alone, or its letter and section name. */
enum class SceneChipMode { Hidden, Letter, Full }

/**
 * What fits in the top bar: [sceneMaxWidth] caps a full scene chip (its name is cut with "…" beyond);
 * [projectName] says whether the project's name shows next to its folder (not on a very narrow bar
 * with a scene, where the folder alone leaves room for the scene's letter: the name then gets a
 * line of its own above the buttons). (The tempo lives in the play strip under the bar, see PlayStrip.)
 */
data class TopBarLayout(
    val showRedo: Boolean, val scene: SceneChipMode, val sceneMaxWidth: Dp = 0.dp, val projectName: Boolean = true, val modeInRow: Boolean = false,
    /** False: the folder alone in the row, no name anywhere in the bar (too little room for a few words, no line given up). */
    val nameShown: Boolean = true,
)

/**
 * Fits the top bar into [width]. Never removed: the projects button (with room for a bit of the
 * name), undo, record, play and share. Then, while there is room, by priority: the scene's letter, the
 * scene's section name, redo.
 */
fun topBarLayout(width: Dp, hasScene: Boolean, modeChip: Dp? = null, redoFirst: Boolean = false, fontScale: Float = 1f): TopBarLayout {
    // Redo nowhere else ([TopBarUi.redoFirst]): its 48 dp taken first, the rest laid out as usual.
    if (redoFirst) {
        val plain = topBarLayout(width, hasScene, modeChip, fontScale = fontScale)
        return if (plain.showRedo) plain else topBarLayout(width - BUTTON, hasScene, modeChip, fontScale = fontScale).copy(showRedo = true)
    }
    // The mode chip (owner, 2026-10-29): in the row when it leaves the project's name a few words
    // (tour 5, moyens M1: « M… » at Taille 130 % on a 411 dp phone) and the scene its letter; else on
    // the name's own line above the buttons (a narrow phone, a big text), the name whole there.
    if (modeChip != null) {
        val inRow = topBarLayout(width - modeChip, hasScene)
        val fits = width - modeChip - ALWAYS_SHOWN >= PROJECT_FOLDER_ROOM + PROJECT_NAME_TEXT * fontScale && inRow.projectName && (!hasScene || inRow.scene != SceneChipMode.Hidden)
        if (fits) return inRow.copy(modeInRow = true)
        // Fresh review of the moyens lot: where the row used to hold « M… », it stays one row with the folder alone
        // (the name's own line took 48 dp from the keys at Taille 130 %); the name's line only where it always was.
        val folderOnly = width - modeChip - ALWAYS_SHOWN >= PROJECT_ROOM && inRow.projectName && (!hasScene || inRow.scene != SceneChipMode.Hidden)
        return if (folderOnly) inRow.copy(modeInRow = true, nameShown = false) else topBarLayout(width, hasScene).copy(projectName = false)
    }
    // No scene shown (Morceau, Outils…): redo gives way below this width (48 dp more since ⤴ joined the bar).
    if (!hasScene) return TopBarLayout(showRedo = width >= 388.dp, scene = SceneChipMode.Hidden)
    var budget = width - ALWAYS_SHOWN - PROJECT_MIN
    // A small phone with big text: the project's name gives its room to the scene's letter (the
    // projects button keeps its folder, 48 dp, and shows no name at all).
    val squeezed = budget < SCENE_LETTER && width - ALWAYS_SHOWN - PROJECT_ICON >= SCENE_LETTER
    val letter = budget >= SCENE_LETTER || squeezed
    if (letter) budget -= SCENE_LETTER
    // The extras leave the project name a readable width, not just its first letters.
    budget -= EXTRAS_RESERVE
    fun take(w: Dp): Boolean = (budget >= w).also { if (it) budget -= w }
    // The section name ("C · Refrain") before redo: knowing what is edited matters more than
    // redo, which stays one step away (undo is always there). It takes what is left, up to a cap.
    val full = letter && budget >= SCENE_NAME_MIN
    var sceneMax = SCENE_LETTER
    if (full) {
        val extra = minOf(budget, SCENE_FULL_MAX - SCENE_LETTER)
        sceneMax += extra
        budget -= extra
    }
    val redo = take(BUTTON)
    val mode = when {
        full -> SceneChipMode.Full
        letter -> SceneChipMode.Letter
        else -> SceneChipMode.Hidden
    }
    return TopBarLayout(showRedo = redo, scene = mode, sceneMaxWidth = if (mode == SceneChipMode.Hidden) 0.dp else sceneMax, projectName = !squeezed)
}

private val BUTTON = 48.dp
/** Undo, record, play, share (48 dp each) and the bar's side padding. */
private val ALWAYS_SHOWN = BUTTON * 4 + 12.dp
/** The projects button keeps its icon and a few letters of the name. */
private val PROJECT_MIN = 92.dp
/** The projects button reduced to its folder icon. */
private val PROJECT_ICON = 48.dp
private val EXTRAS_RESERVE = 36.dp
private val SCENE_LETTER = 52.dp
private val SCENE_NAME_MIN = 60.dp
private val SCENE_FULL_MAX = 140.dp
/** The mode chip's room in the row: its text capped at [modeChipMax], with a little margin. */
fun modeChipRoom(simple: Boolean): Dp = modeChipMax(simple) + 4.dp
/** Next to the mode chip, the projects button kept its folder and a few letters of the name before moyens M1: the row stays one. */
private val PROJECT_ROOM = 72.dp
/** The projects button's folder (34 dp), the gap to its name (10 dp) and its padding (8 dp). */
private val PROJECT_FOLDER_ROOM = 52.dp
/** Next to the mode chip, the name keeps this much text at the usual size (« Mon prem… »), more as the text grows. */
private val PROJECT_NAME_TEXT = 56.dp

private val TOP_SHEEN = Color.White.copy(alpha = 0.06f)
private val FOOT_SHADE = Color.Black.copy(alpha = 0.32f)
private val FOOT_LIGHT = Color.White.copy(alpha = 0.07f)

/** The light on a raised button's upper half (the projects' folder, Play). */
private val GLOSS = Brush.verticalGradient(0f to Color.White.copy(alpha = 0.2f), 0.5f to Color.White.copy(alpha = 0.04f), 0.55f to Color.Transparent)

/**
 * Always on top: the open project (tap for "Mes projets", hold to rename it), the scene edited,
 * undo/redo, record, play and share, on every screen. The tempo and the song's position are in the
 * play strip right under it.
 */
@Composable
fun TopBar(ui: TopBarUi, actions: TopBarActions) = androidx.compose.foundation.layout.BoxWithConstraints(
    Modifier.fillMaxWidth()
        .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.Black, 0.12f), lerp(Neon.Background, Color.Black, 0.1f))))
        // A machined edge (lot « design pro »): a faint light line on top, a dark one then a light one at the foot.
        .drawBehind {
            drawLine(TOP_SHEEN, Offset.Zero, Offset(size.width, 0f), 1f)
            drawLine(FOOT_SHADE, Offset(0f, size.height - 1.5f), Offset(size.width, size.height - 1.5f), 1f)
            drawLine(FOOT_LIGHT, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
        }
        // The status bar above; lying down, the 3-button navigation bar and the camera hole on the
        // sides: the bar's own buttons stay clear of them (the background goes under).
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
) {
    // Narrow (a small phone, a big text size): redo, then the scene's name give their room to the
    // project name; the projects button, the scene's letter and play never go.
    // Each ▶ has its word under it (tour 5, chantier 4): as wide as the word, never cut, at any text size and language.
    val (partStyle, partRoom) = playWord(tr("cette partie"))
    val (songStyle, songRoom) = playWord(tr("le morceau"))
    val rowWidth = (if (ui.undo) maxWidth else maxWidth + BUTTON) - (partRoom - BUTTON)
    // A narrow bar (the keys on a small phone at 130 %): « ▶ le morceau » goes up to the name's line, at its end.
    val songOnTop = ui.songButton && rowWidth - songRoom - PLAY_GAP - (if (ui.redoFirst) BUTTON else 0.dp) < ALWAYS_SHOWN + PROJECT_ICON
    val fitted = topBarLayout(if (songOnTop || !ui.songButton) rowWidth else rowWidth - songRoom - PLAY_GAP, ui.scene != null, modeChip = ui.mode?.let(::modeChipRoom), redoFirst = ui.redoFirst, fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale)
    val layout = if (songOnTop) fitted.copy(projectName = false, modeInRow = false) else fitted
    val showRedo = layout.showRedo && ui.redo
    androidx.compose.foundation.layout.Column {
    // Squeezed (a small phone with a big text size): the project's name gets a line of its own
    // above the buttons (never gone), the folder alone stays in the row.
    if (!layout.projectName && layout.nameShown) {
        Row(
            // With the mode chip (no room for it in the row) the line is as high as its touch target.
            // At least that high: a big text (the phone's font at 200 %) makes the name taller than 24 dp, never cut.
            Modifier.fillMaxWidth().heightIn(min = if (songOnTop) 52.dp else if (ui.mode != null && !layout.modeInRow) 48.dp else 24.dp)
                .padding(start = 12.dp, end = if (songOnTop || (ui.mode != null && !layout.modeInRow)) 6.dp else 12.dp)
                .combinedClickable(onClick = actions.projects, onLongClick = actions.rename, onLongClickLabel = tr("Renommer le projet"))
                .testTag("top_project_line"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                ui.projectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (ui.recordInto != null) {
                Text(tr("REC dans %s", ui.recordInto), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = Neon.RedText, maxLines = 1, modifier = Modifier.testTag("top_record_into"))
            } else if (ui.saved) {
                Icon(Icons.Filled.Check, contentDescription = tr("Sauvegardé"), tint = Neon.Acid, modifier = Modifier.size(16.dp).testTag("top_saved"))
            }
            if (ui.mode != null && !layout.modeInRow) ModeChip(ui.mode, actions.mode)
            if (songOnTop) NamedPlay(tr("le morceau"), songStyle, tr("Jouer le morceau"), ui.songPlaying, "top_song", songRoom, actions.playSong)
        }
    }

    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp).testTag("top_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = actions.projects, onLongClick = actions.rename, onLongClickLabel = tr("Renommer le projet"))
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .spot("top_projects", tr("Mes projets"))
                .testTag("top_projects"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Neon.Cyan, lerp(Neon.Cyan, Color.Black, 0.3f))))
                    .background(GLOSS),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = tr("Mes projets"), tint = Neon.OnAccent, modifier = Modifier.size(18.dp))
            }
            if (layout.projectName && layout.nameShown) androidx.compose.foundation.layout.Column {
                // Testers (lot « tuto 4 »): « Mon pro… »: a name that does not fit is set a size smaller first.
                var smaller by remember(ui.projectName) { mutableStateOf(false) }
                Text(
                    ui.projectName, style = if (smaller) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, onTextLayout = { if (!smaller && it.hasVisualOverflow) smaller = true },
                )
                if (ui.recordInto != null) {
                    Text(
                        tr("REC dans %s", ui.recordInto), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = Neon.RedText,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("top_record_into"),
                    )
                } else if (ui.saved) {
                    // Nothing to save by hand: said where the project is named, quietly.
                    // Audit ultra round 1, n° 26 (seat B b: « une minuscule coche »): the tick and its word larger, in the accent.
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("top_saved")) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(16.dp))
                        // Too narrow for the word (a small phone): the tick alone, never "E…".
                        var cut by remember { mutableStateOf(false) }
                        Text(
                            tr("Sauvegardé"), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp, fontWeight = FontWeight.Bold), color = Neon.Acid,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, onTextLayout = { cut = it.hasVisualOverflow },
                            modifier = Modifier.padding(start = 2.dp).graphicsLayer { alpha = if (cut) 0f else 1f },
                        )
                    }
                }
            }
        }
        if (ui.mode != null && layout.modeInRow) ModeChip(ui.mode, actions.mode)
        if (ui.scene != null && layout.scene != SceneChipMode.Hidden) {
            SceneChip(ui.scene, layout.scene == SceneChipMode.Full, layout.sceneMaxWidth, actions)
        }
        if (ui.undo) IconButton(onClick = actions.undo, enabled = ui.canUndo, modifier = Modifier.spot("top_undo", tr("Annuler (défaire)")).testTag("top_undo")) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = tr("Annuler la dernière action"))
        }
        if (showRedo) {
            IconButton(onClick = actions.redo, enabled = ui.canRedo, modifier = Modifier.testTag("top_redo")) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = tr("Rétablir"))
            }
        }
        // Record: a red dot, pulsing and glowing while armed.
        val blink = rememberInfiniteTransition(label = "rec")
        // Audit final (22): towards full, so with animations turned off (the scale at 0) the dot stays plain.
        val alpha by blink.animateFloat(0.35f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "recAlpha")
        IconButton(onClick = actions.toggleRecord, modifier = Modifier.spot("top_record", tr("Enregistrer")).testTag("top_record")) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(if (ui.recording) Neon.Red.copy(alpha = 0.18f) else Color.Transparent)
                    .border(1.5.dp, if (ui.recording) Neon.Red.copy(alpha = alpha) else Neon.TextSecondary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.FiberManualRecord,
                    contentDescription = ui.recordLabel ?: when {
                        ui.micTake && ui.recording -> tr("Arrêter la prise et la garder")
                        ui.micTake -> tr("Enregistrer le micro sur la piste audio")
                        ui.recording -> tr("Arrêter l'enregistrement")
                        else -> tr("Enregistrer ce que je joue")
                    },
                    tint = if (ui.recording) Neon.Red.copy(alpha = alpha) else Neon.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        // « ▶ cette partie » (tour 5, chantier 4): the part open, looped. Round 3, n° 34: the label says what a touch does now.
        // Tour 5, moyens M2: during a take on the song it stops too (RhythmViewModel.togglePart), so it shows ■ « Arrêter ».
        val stopsTake = ui.recording && ui.songPlaying
        NamedPlay(tr("cette partie"), partStyle, tr("Jouer cette partie"), ui.partPlaying || stopsTake, "top_play", partRoom, actions.togglePlay)
        // Apart, the two words never read as one (« cette partie le morceau »).
        if (ui.songButton && !songOnTop) Spacer(Modifier.width(PLAY_GAP))
        if (ui.songButton && !songOnTop) NamedPlay(tr("le morceau"), songStyle, tr("Jouer le morceau"), ui.songPlaying, "top_song", songRoom, actions.playSong)
        val more = actions.more
        if (more != null) {
            // Mode simple: « Partager » is a tab; the rest of the app is on the Plus page.
            IconButton(onClick = more, modifier = Modifier.spot("top_more", tr("Plus de choix")).testTag("top_more")) {
                Icon(Icons.Filled.MoreVert, contentDescription = tr("Plus de choix"), tint = Neon.TextPrimary)
            }
        } else {
            // ⤴ Partager, the last thing done to a song, always one touch away (« Prêt à partager ? »).
            IconButton(onClick = actions.share, modifier = Modifier.spot("top_share", tr("Partager")).testTag("top_share")) {
                Icon(Icons.Filled.Share, contentDescription = tr("Partager mon morceau"), tint = Neon.TextPrimary)
            }
        }
    }
    }
}

/**
 * The word under a top ▶ (« cette partie », « le morceau »): small, semi-bold, one line. 10.5 sp: at 11 sp bold « cette
 * partie » took 20 dp more than the old ▶ and pushed the project's name to a line of its own at Taille 130 % on a
 * 411 dp phone (48 dp less for the keys); now about 10.
 */
@Composable
private fun playWordStyle() = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 12.sp, letterSpacing = 0.sp, fontWeight = FontWeight.SemiBold)

/**
 * A named ▶'s word as drawn and its button's width: the word whole, 48 dp at least. A big text (the phone's font at 200 %)
 * would make « cette partie » 115 dp wide and taller than the bar: past [PLAY_WORD_MAX] the word is set smaller, never
 * smaller than at the usual size (CaptureMatrixBigFontTest: « cette partie » cut at its foot, 21 dp shown of 25).
 */
@Composable
private fun playWord(word: String): Pair<androidx.compose.ui.text.TextStyle, Dp> {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val base = playWordStyle()
    val density = androidx.compose.ui.platform.LocalDensity.current
    return remember(word, base, density) {
        fun width(style: androidx.compose.ui.text.TextStyle) = with(density) { measurer.measure(word, style, softWrap = false, maxLines = 1).size.width.toDp() }
        val w = width(base)
        val style = if (w > PLAY_WORD_MAX) {
            val k = PLAY_WORD_MAX / w
            base.copy(fontSize = base.fontSize * k, lineHeight = base.lineHeight * k)
        } else base
        style to maxOf(BUTTON, (if (style === base) w else width(style)) + PLAY_WORD_MARGIN)
    }
}

/** Between « ▶ cette partie » and « ▶ le morceau » side by side. */
private val PLAY_GAP = 8.dp

/** The widest a ▶'s word gets (« cette partie » is 57 dp at the usual size, set a hair smaller): its button 58 dp. */
private val PLAY_WORD_MAX = 56.dp

private val PLAY_WORD_MARGIN = 2.dp

/**
 * A ▶ with its word under it (tour 5, chantier 4, Benjamin's 2B: two ▶, « on ne sait pas ce que chacun fait »): [word]
 * says what it plays, [talk] is what TalkBack says ([playing]: « Arrêter », what a touch does then). A 34 dp disc that
 * glows while it plays, in a touch target [room] wide and 48 dp high at least.
 */
@Composable
private fun NamedPlay(word: String, style: androidx.compose.ui.text.TextStyle, talk: String, playing: Boolean, tag: String, room: Dp, onClick: () -> Unit) {
    val playColor = if (playing) Neon.Acid else Neon.Cyan
    val said = if (playing) tr("Arrêter") else talk
    androidx.compose.foundation.layout.Column(
        // No rounded clip: the word sits at the button's foot, its corners cut « cette partie » to « ette parti ».
        Modifier.width(room).heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClickLabel = said, onClick = onClick)
            .semantics { contentDescription = said }
            .spot(tag, said)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.requiredSize(34.dp).liveGlow(playing, Neon.Acid, inset = 2.dp)
                .shadow(if (playing) 2.dp else 5.dp, CircleShape, ambientColor = playColor, spotColor = playColor)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(lerp(playColor, Color.White, 0.2f), playColor)))
                .background(GLOSS),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.size(22.dp))
        }
        Text(
            word, style = style, color = if (playing) Neon.Acid else Neon.TextPrimary,
            maxLines = 1, softWrap = false, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

/** The scene being edited ("A · Intro ▾"): a touch lists every scene to pick another, or a new empty one. */
@Composable
private fun SceneChip(scene: SceneChipUi, full: Boolean, maxWidth: Dp, actions: TopBarActions) {
    var open by remember { mutableStateOf(false) }
    val title = scene.titles.getOrElse(scene.current) { Scene.NAMES.getOrElse(scene.current) { "?" } }
    val letter = Scene.NAMES.getOrElse(scene.current) { "?" }
    // A 36 dp chip inside a 48 dp touch target (the bar is 56 dp high).
    Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier.padding(horizontal = 2.dp).widthIn(min = 48.dp, max = maxWidth).height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Neon.Cyan.copy(alpha = 0.14f))
                .border(1.dp, Neon.Cyan.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                .clickable(role = Role.Button, onClickLabel = tr("Changer de partie")) { open = true }
                .semantics { contentDescription = tr("Partie en cours : %s. Toucher pour changer de partie.", title) }
                .padding(start = 10.dp, end = 4.dp)
                .spot("top_scene", tr("Partie modifiée"))
                .testTag("top_scene"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (full) title else letter,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).testTag("top_scene_text"),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(20.dp))
        }
        ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("scene_menu")) {
            Text(
                tr("Partie à modifier"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            scene.titles.forEachIndexed { i, t ->
                val current = i == scene.current
                DropdownMenuItem(
                    text = {
                        Text(
                            if (i in scene.empty && !current) tr("%s (vide)", t) else t,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                            color = if (current) Neon.Cyan else Neon.TextPrimary,
                        )
                    },
                    leadingIcon = {
                        if (current) Icon(Icons.Filled.Check, contentDescription = tr("Partie en cours"), tint = Neon.Cyan)
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        open = false
                        if (!current) actions.selectScene(i)
                    },
                    modifier = Modifier.testTag("scene_pick_$i"),
                )
            }
            HorizontalDivider(color = Neon.Outline.copy(alpha = 0.45f))
            DropdownMenuItem(
                text = { Text(tr("Nouvelle partie vide")) },

                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Neon.Acid) },
                onClick = {
                    open = false
                    actions.newScene()
                },
                modifier = Modifier.testTag("scene_new"),
            )
        }
    }
}
