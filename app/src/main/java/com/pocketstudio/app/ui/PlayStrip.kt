package com.pocketstudio.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.sceneLabel
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.songBars
import com.pocketstudio.model.songStepOf
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon

/**
 * Where the music is, or where ▶ starts: part [name], bar [bar] (from 1) of [bars]. [loop]: the
 * part loops (« ⟳ Partie »), the bars are the part's; else they are the whole song's.
 */
data class PlayPosition(val name: String, val bar: Int, val bars: Int, val loop: Boolean = false)

/** How a part is named in the strip: its section name (« Refrain »), else its scene's letter. */
fun partName(p: Project, scene: Int): String =
    p.sceneLabel(scene).takeIf { it.isNotBlank() }?.let(Scene::shownLabel) ?: Scene.NAMES.getOrElse(scene) { "?" }

/**
 * The position said under the top bar. Playing the song: the part playing ([songIndex]) and the
 * song's bar [bar] (from 0, -1 not known yet). Playing a scene in a loop: that scene's bar.
 * Stopped: where ▶ starts, the part [playFrom] of the song, or the scene shown with [loopPart].
 * Null: nothing to say (stopped, no song).
 */
fun playPosition(p: Project, playing: Boolean, songIndex: Int, bar: Int, playFrom: Int, loopPart: Boolean): PlayPosition? {
    val spb = p.stepsPerBar.coerceAtLeast(1)
    val songBars = p.songBars.coerceAtLeast(1)
    val sceneBars = ((p.sceneSteps(p.scene) + spb - 1) / spb).coerceAtLeast(1)
    val song = p.song
    return when {
        playing && songIndex >= 0 && songIndex < song.size -> {
            val start = p.songStepOf(songIndex) / spb
            PlayPosition(partName(p, song[songIndex].scene), (if (bar >= 0) bar else start).coerceIn(0, songBars - 1) + 1, songBars)
        }
        playing -> PlayPosition(partName(p, p.scene), bar.coerceIn(0, sceneBars - 1) + 1, sceneBars, loop = true)
        song.isEmpty() -> null
        loopPart -> PlayPosition(partName(p, p.scene), 1, sceneBars, loop = true)
        else -> {
            val from = playFrom.takeIf { it in song.indices } ?: 0
            PlayPosition(partName(p, song[from].scene), p.songStepOf(from) / spb + 1, songBars)
        }
    }
}

/** What the play strip shows. */
class PlayStripUi(
    val tempoBpm: Double,
    /** The project has a song: ▶ plays it (unless [loopPart]); « ⟳ Partie » and « Jouer depuis… » show. */
    val hasSong: Boolean,
    val loopPart: Boolean,
    val playing: Boolean,
    /** The position: read where it is shown, so only that text follows the bars (once a bar). */
    val position: () -> PlayPosition?,
    /** The song's parts, named as « Jouer depuis… » lists them. */
    val parts: List<String> = emptyList(),
    /** The tracks soloed, when the strip says it itself (landscape: no room for [SoloBanner] under it). */
    val soloed: List<String> = emptyList(),
)

class PlayStripActions(
    /** A touch on the tempo: the tempo sheet (keypad, ready-made tempos, tap). */
    val openTempo: () -> Unit = {},
    val setLoopPart: (Boolean) -> Unit = {},
    /** « Jouer depuis… »: the song from that part. */
    val playFrom: (Int) -> Unit = {},
    /** The « Solo » chip: every solo off. */
    val clearSolos: () -> Unit = {},
)

/**
 * Right under the top bar, on every tab: the tempo (a touch to change it), where the music is
 * (or where ▶ starts; a touch to play from another part), the « Solo » chip when the strip has
 * to say it ([PlayStripUi.soloed]), and « ⟳ Partie », under ▶: on, ▶ loops the part being edited
 * instead of playing the song. Every control is touchable over the strip's whole height (48 dp).
 */
@Composable
fun PlayStrip(ui: PlayStripUi, actions: PlayStripActions) {
    Row(
        Modifier.fillMaxWidth()
            .background(lerp(Neon.Background, Color.Black, 0.1f))
            .drawBehind { drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .heightIn(min = STRIP_HEIGHT)
            .padding(horizontal = 6.dp)
            .testTag("play_strip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TempoButton(ui.tempoBpm, actions.openTempo)
        PositionLabel(ui, actions, Modifier.weight(1f))
        if (ui.soloed.isNotEmpty()) SoloChip(ui.soloed, actions.clearSolos)
        if (ui.hasSong) LoopPartChip(ui.loopPart, actions.setLoopPart)
    }
}

/** The strip's height (more with a big text size): 48 dp, the least touch target of each of its controls, both ways (DESIGN.md, rule 4). */
val STRIP_HEIGHT = 48.dp

/** The height of a chip's drawn pill, inside its 48 dp touch target. */
private val CHIP_HEIGHT = 32.dp

/** The tempo in a small dark window, like a hardware display; a touch opens the tempo sheet. */
@Composable
private fun TempoButton(bpm: Double, open: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val said = tr("Tempo : %d BPM", Math.round(bpm).toInt())
    // Touched over the strip's whole height; the window drawn smaller inside.
    Box(
        Modifier.sizeIn(minWidth = STRIP_HEIGHT, minHeight = STRIP_HEIGHT).clip(shape)
            .clickable(role = Role.Button, onClickLabel = tr("Changer le tempo"), onClick = open)
            .semantics(mergeDescendants = true) { contentDescription = said }
            .testTag("top_tempo"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.clip(shape).background(Color.Black.copy(alpha = 0.35f)).border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("${Math.round(bpm)}", style = MonoStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Neon.Cyan)
            Text(" BPM", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary)
        }
    }
}

/** "Refrain · mes. 17/60" (green while it plays); with a song, a touch lists its parts to play from. */
@Composable
private fun PositionLabel(ui: PlayStripUi, actions: PlayStripActions, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    // Read here only: the bars move this text, not the strip.
    val pos = ui.position()
    val pickable = ui.hasSong && ui.parts.isNotEmpty()
    Box(modifier) {
        if (pos != null) {
            // Green while it plays (DESIGN.md: green is « ça joue », as ▶ is), the accent while a part loops.
            val color = when {
                ui.playing -> Neon.Acid
                pos.loop -> Neon.Cyan
                else -> Neon.TextSecondary
            }
            // The part's name over the bar: both stay readable on a narrow phone or a big text size.
            Row(
                Modifier.sizeIn(minWidth = STRIP_HEIGHT, minHeight = STRIP_HEIGHT).clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = pickable, role = Role.Button, onClickLabel = tr("Jouer depuis une partie")) { open = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("top_position"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (pos.loop) {
                            Icon(
                                Icons.Filled.Repeat, contentDescription = tr("en boucle"), tint = color,
                                modifier = Modifier.padding(end = 3.dp).size(14.dp).testTag("top_position_loop"),
                            )
                        }
                        Text(
                            pos.name, style = MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp), fontWeight = FontWeight.Bold, color = color,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        tr("mes. %1\$d/%2\$d", pos.bar, pos.bars), style = MonoStyle.copy(fontSize = 14.sp, lineHeight = 16.sp), color = color,
                        maxLines = 1, softWrap = false, modifier = Modifier.testTag("top_position_bar"),
                    )
                }
                if (pickable) Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("play_from_menu")) {
            Text(
                tr("Jouer depuis…"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            ui.parts.forEachIndexed { i, name ->
                DropdownMenuItem(
                    text = { Text("${i + 1} · $name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        open = false
                        actions.playFrom(i)
                    },
                    modifier = Modifier.testTag("play_from_$i"),
                )
            }
        }
    }
}

/** « ⟳ Partie »: lit in the accent while on (▶ then loops the part being edited). */
@Composable
private fun LoopPartChip(on: Boolean, set: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val state = if (on) tr("activé") else tr("désactivé")
    val content = if (on) Neon.OnAccent else Neon.TextSecondary
    // A plain Row, not a Material component: its touch target is made 48 dp here.
    Box(
        Modifier.sizeIn(minWidth = STRIP_HEIGHT, minHeight = STRIP_HEIGHT).clip(shape)
            .toggleable(value = on, role = Role.Switch, onValueChange = set)
            .semantics {
                contentDescription = tr("Boucler la partie au lieu de jouer le morceau")
                stateDescription = state
            }
            .testTag("top_loop_part"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.height(CHIP_HEIGHT).clip(shape)
                .background(if (on) Neon.Cyan else Color.Transparent)
                .border(1.dp, if (on) Neon.Cyan else Neon.Outline, shape)
                .padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Repeat, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Text(tr("Partie"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = content, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/**
 * « Solo » in amber, in the strip, where the solo banner has no room (landscape): a touch hears
 * every track again. Says which tracks, for a screen reader.
 */
@Composable
private fun SoloChip(names: List<String>, clearAll: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val said = tr("Solo actif sur %s", shortList(names))
    Box(
        Modifier.sizeIn(minWidth = STRIP_HEIGHT, minHeight = STRIP_HEIGHT).clip(shape)
            .clickable(role = Role.Button, onClickLabel = tr("Désactiver tous les solos"), onClick = clearAll)
            .semantics(mergeDescendants = true) { contentDescription = said }
            .testTag("solo_chip"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.height(CHIP_HEIGHT).clip(shape).background(Neon.Amber).padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Headphones, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            Text(tr("Solo"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/**
 * The tracks soloed in [p] that the engine really solos (strips in use), named as the mixer
 * names them: while there is one, only the soloed tracks are heard, everywhere and in the export.
 */
fun soloedTracks(p: Project): List<String> {
    val strips = mixerStrips(p)
    return RhythmViewModel.usedStrips(p).filter { p.mixer.channels.getOrNull(it)?.solo == true }.sorted().map { ch ->
        if (ch >= Mixer.FIRST_PAD) tr("Pad %d", ch - Mixer.FIRST_PAD + 1)
        else strips.firstOrNull { it.ch == ch }?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) } ?: "?"
    }
}

/** "Batterie, Basse" ; beyond two, "Batterie, Basse +2". */
private fun shortList(names: List<String>): String =
    if (names.size <= 2) names.joinToString(", ") else names.take(2).joinToString(", ") + " +${names.size - 2}"

/**
 * « Solo actif sur Batterie : tout désactiver », under the top bar on every tab while a track is
 * soloed (the others are silent, the export too): one touch anywhere on it hears every track again.
 */
@Composable
fun SoloBanner(names: List<String>, clearAll: () -> Unit) {
    if (names.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().background(Neon.Amber)
            .clickable(role = Role.Button, onClickLabel = tr("Désactiver tous les solos"), onClick = clearAll)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag("solo_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Headphones, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Text(
            tr("Solo actif sur %s", shortList(names)), style = MaterialTheme.typography.bodyMedium, color = Color.Black,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            tr("Tout désactiver"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(14.dp)).border(1.5.dp, Color.Black, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
