package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.model.i18n.tr

/**
 * The scene being edited, shown as a chip in the top bar: [current] is its index, [titles] every
 * scene of the session as it is shown ("A · Intro", "B"), [empty] those with nothing in them yet.
 */
class SceneChipUi(val current: Int, val titles: List<String>, val empty: Set<Int> = emptySet())

class TopBarUi(
    val projectName: String,
    val tempoBpm: Double,
    val playing: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    /** Live recording armed (what is played is written into the loop), or a microphone take on. */
    val recording: Boolean = false,
    /** The ● records the microphone on an audio track (the Audio tracks screen, or a take on). */
    val micTake: Boolean = false,
    /** The scene chip, on the screens that edit a scene (null: not shown). */
    val scene: SceneChipUi? = null,
    /** The project is saved (on its own, after each change): a small "✓ Enregistré" under its name. */
    val saved: Boolean = false,
)

class TopBarActions(
    val projects: () -> Unit = {},
    val undo: () -> Unit = {},
    val redo: () -> Unit = {},
    val togglePlay: () -> Unit = {},
    val toggleRecord: () -> Unit = {},
    val selectScene: (Int) -> Unit = {},
    val newScene: () -> Unit = {},
)

/** How the scene chip shows: not at all, its letter alone, or its letter and section name. */
enum class SceneChipMode { Hidden, Letter, Full }

/** What fits in the top bar: [sceneMaxWidth] caps a full scene chip (its name is cut with "…" beyond). */
data class TopBarLayout(val showTempo: Boolean, val showRedo: Boolean, val scene: SceneChipMode, val sceneMaxWidth: Dp = 0.dp)

/**
 * Fits the top bar into [width]. Never removed: the projects button (with room for a bit of the
 * name), undo, record and play. Then, while there is room, by priority: the scene's letter, the
 * scene's section name, redo, the tempo window.
 */
fun topBarLayout(width: Dp, hasScene: Boolean): TopBarLayout {
    // No scene shown (Morceau, Outils…): the tempo window, then redo, give way below these widths.
    if (!hasScene) return TopBarLayout(showTempo = width >= 400.dp, showRedo = width >= 340.dp, scene = SceneChipMode.Hidden)
    var budget = width - ALWAYS_SHOWN - PROJECT_MIN
    val letter = budget >= SCENE_LETTER
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
    val tempo = take(TEMPO)
    val mode = when {
        full -> SceneChipMode.Full
        letter -> SceneChipMode.Letter
        else -> SceneChipMode.Hidden
    }
    return TopBarLayout(showTempo = tempo, showRedo = redo, scene = mode, sceneMaxWidth = if (mode == SceneChipMode.Hidden) 0.dp else sceneMax)
}

private val BUTTON = 48.dp
/** Undo, record, play (48 dp each) and the bar's side padding. */
private val ALWAYS_SHOWN = BUTTON * 3 + 12.dp
/** The projects button keeps its icon and a few letters of the name. */
private val PROJECT_MIN = 92.dp
private val EXTRAS_RESERVE = 36.dp
private val SCENE_LETTER = 52.dp
private val SCENE_NAME_MIN = 60.dp
private val SCENE_FULL_MAX = 140.dp
private val TEMPO = 84.dp

/** Always on top: the open project (tap for "Mes projets"), the scene edited, undo/redo, tempo and play, on every screen. */
@Composable
fun TopBar(ui: TopBarUi, actions: TopBarActions) = androidx.compose.foundation.layout.BoxWithConstraints(
    Modifier.fillMaxWidth()
        .background(Brush.verticalGradient(listOf(lerp(Neon.Surface, Color.Black, 0.12f), lerp(Neon.Background, Color.Black, 0.1f))))
        .drawBehind { drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }
        // The status bar above; lying down, the 3-button navigation bar and the camera hole on the
        // sides: the bar's own buttons stay clear of them (the background goes under).
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
) {
    // Narrow (a small phone, a big text size): the tempo window, redo, then the scene's name give
    // their room to the project name; the projects button, the scene's letter and play never go.
    val layout = topBarLayout(maxWidth, ui.scene != null)
    val showTempo = layout.showTempo
    val showRedo = layout.showRedo
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp).testTag("top_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = actions.projects).padding(vertical = 6.dp, horizontal = 4.dp)
                .testTag("top_projects"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Neon.Cyan, lerp(Neon.Cyan, Color.Black, 0.3f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = tr("Mes projets"), tint = Neon.OnAccent, modifier = Modifier.size(18.dp))
            }
            androidx.compose.foundation.layout.Column {
                Text(ui.projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Nothing to save by hand: said where the project is named, quietly.
                if (ui.saved) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("top_saved")) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(12.dp))
                        // Too narrow for the word (a small phone): the tick alone, never "E…".
                        var cut by remember { mutableStateOf(false) }
                        Text(
                            tr("Sauvegardé"), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp), color = Neon.TextSecondary,
                            maxLines = 1, softWrap = false, overflow = TextOverflow.Clip, onTextLayout = { cut = it.hasVisualOverflow },
                            modifier = Modifier.padding(start = 2.dp).graphicsLayer { alpha = if (cut) 0f else 1f },
                        )
                    }
                }
            }
        }
        if (ui.scene != null && layout.scene != SceneChipMode.Hidden) {
            SceneChip(ui.scene, layout.scene == SceneChipMode.Full, layout.sceneMaxWidth, actions)
        }
        IconButton(onClick = actions.undo, enabled = ui.canUndo, modifier = Modifier.testTag("top_undo")) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = tr("Annuler la dernière action"))
        }
        if (showRedo) {
            IconButton(onClick = actions.redo, enabled = ui.canRedo, modifier = Modifier.testTag("top_redo")) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = tr("Rétablir"))
            }
        }
        // Record: a red dot, pulsing and glowing while armed.
        val blink = rememberInfiniteTransition(label = "rec")
        val alpha by blink.animateFloat(1f, 0.35f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "recAlpha")
        IconButton(onClick = actions.toggleRecord, modifier = Modifier.testTag("top_record")) {
            Box(
                Modifier.size(30.dp).clip(CircleShape)
                    .background(if (ui.recording) Neon.Red.copy(alpha = 0.18f) else Color.Transparent)
                    .border(1.5.dp, if (ui.recording) Neon.Red.copy(alpha = alpha) else Neon.TextSecondary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.FiberManualRecord,
                    contentDescription = when {
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
        // The tempo in a small dark window, like a hardware display.
        if (showTempo) Row(
            Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("${ui.tempoBpm.toInt()}", style = MonoStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Neon.Cyan)
            Text(" BPM", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        }
        val playColor = if (ui.playing) Neon.Acid else Neon.Cyan
        // A 42 dp disc inside a 48 dp touch target.
        Box(
            Modifier.requiredSize(48.dp).clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = if (ui.playing) tr("Arrêter") else tr("Jouer"), onClick = actions.togglePlay)
                .testTag("top_play"),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.requiredSize(42.dp)
                    .shadow(if (ui.playing) 14.dp else 6.dp, CircleShape, ambientColor = playColor, spotColor = playColor)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(lerp(playColor, Color.White, 0.2f), playColor))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (ui.playing) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (ui.playing) tr("Arrêter") else tr("Jouer"),
                    tint = Neon.OnAccent,
                )
            }
        }
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
                .clickable(role = Role.Button, onClickLabel = tr("Changer de scène")) { open = true }
                .semantics { contentDescription = tr("Scène en cours : %s. Toucher pour changer de scène.", title) }
                .padding(start = 10.dp, end = 4.dp)
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
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("scene_menu")) {
            Text(
                tr("Scène à modifier"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary,
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
                        if (current) Icon(Icons.Filled.Check, contentDescription = tr("Scène en cours"), tint = Neon.Cyan)
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        open = false
                        if (!current) actions.selectScene(i)
                    },
                    modifier = Modifier.testTag("scene_pick_$i"),
                )
            }
            HorizontalDivider(color = Neon.Outline)
            DropdownMenuItem(
                text = { Text(tr("Nouvelle scène vide")) },

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
