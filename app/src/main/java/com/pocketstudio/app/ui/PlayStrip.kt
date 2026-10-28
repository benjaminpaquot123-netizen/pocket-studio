package com.pocketstudio.app.ui

import com.pocketstudio.ui.ScaledDropdownMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import com.pocketstudio.app.learn.spot
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.foundation.combinedClickable
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
    /** Phase 36: the « ? » that labels the screen, lit while the labels show (null: no « ? » here). */
    val helpOn: Boolean? = null,
    /** « ⟳ Partie » shown (with a song); false: the Mode simple's lighter strip. */
    val loopChip: Boolean = true,
)

class PlayStripActions(
    /** A touch on the tempo: the tempo sheet (keypad, ready-made tempos, tap). */
    val openTempo: () -> Unit = {},
    val setLoopPart: (Boolean) -> Unit = {},
    /** « Jouer depuis… »: the song from that part. */
    val playFrom: (Int) -> Unit = {},
    /** « Jouer depuis… › Tout le morceau, du début »: the whole song from its first bar, « ⟳ Boucler » off. */
    val playWholeSong: () -> Unit = {},
    /** The « Solo » chip: every solo off. */
    val clearSolos: () -> Unit = {},
    /** « ? »: the screen's labels shown, or hidden again. */
    val toggleHelp: () -> Unit = {},
    /** A long press on « ? »: « Signaler un bug » (null: none). */
    val reportBug: (() -> Unit)? = null,
)

/**
 * Right under the top bar, on every tab: the tempo (a touch to change it), where the music is
 * (or where ▶ starts; a touch to play from another part), the « Solo » chip when the strip has
 * to say it ([PlayStripUi.soloed]), and « ⟳ Partie », under ▶: on, ▶ loops the part being edited
 * instead of playing the song. Every control is touchable over the strip's whole height (48 dp).
 */
@Composable
fun PlayStrip(ui: PlayStripUi, actions: PlayStripActions) = BoxWithConstraints(
    Modifier.fillMaxWidth()
        .background(lerp(Neon.Background, Color.Black, 0.1f))
        .drawBehind { drawLine(Color.White.copy(alpha = 0.07f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
) {
    // Taller with a big text size (the position is two lines), never shorter than STRIP_HEIGHT.
    val height = STRIP_HEIGHT * maxOf(1f, LocalDensity.current.fontScale)
    // A narrow strip (a small phone, a big text size): the chips keep their icon alone, so the
    // position keeps its words (never « … / me »). The Solo chip gives way first.
    // The « ? » (phase 36) takes a chip's room too.
    val loop = ui.hasSong && ui.loopChip
    val chips = (if (ui.soloed.isNotEmpty()) 1 else 0) + (if (loop) 1 else 0) + (if (ui.helpOn != null) 1 else 0)
    val soloIconOnly = ui.soloed.isNotEmpty() && maxWidth < COMPACT_BELOW
    // On, « ⟳ Boucler » keeps its word where it fits beside the « ? » (remix lot 1: an orange ⟳ alone did
    // not say that ▶ loops one part instead of playing the song); off, its icon is enough.
    val loopIconOnly = ui.hasSong && (
        if (ui.loopPart) maxWidth < (if (chips >= 3) COMPACT_BELOW else LOOP_WORD_BELOW)
        else maxWidth < (if (chips >= 2) COMPACT_BELOW else 300.dp)
        )
    Row(
        Modifier.fillMaxWidth().height(height).padding(horizontal = 6.dp).testTag("play_strip"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TempoButton(ui.tempoBpm, actions.openTempo)
        PositionLabel(ui, actions, Modifier.weight(1f))
        if (ui.soloed.isNotEmpty()) SoloChip(ui.soloed, actions.clearSolos, iconOnly = soloIconOnly)
        if (loop) LoopPartChip(ui.loopPart, actions.setLoopPart, iconOnly = loopIconOnly)
        ui.helpOn?.let { on -> HelpLabelsButton(on, actions.toggleHelp, actions.reportBug) }
    }
}

/**
 * The strip's height: 40 dp (the top bar, this strip and the « Prochaine étape » line cost 120 dp
 * together on a phone). Each control is still touchable over [TOUCH_HEIGHT], both ways (DESIGN.md,
 * rule 4): its touch target overflows the strip by 4 dp above and below, into the bars' margins.
 */
val STRIP_HEIGHT = 40.dp

/** The least touch target of each control of the strip. */
val TOUCH_HEIGHT = 48.dp

/** Under this width, the strip's chips show their icon alone. */
private val COMPACT_BELOW = 400.dp

/** Under this width « ⟳ Boucler », on, shows its icon alone (with the « ? » beside it: the position keeps its 96 dp). */
private val LOOP_WORD_BELOW = 340.dp

/** The height of a chip's drawn pill, inside its 48 dp touch target. */
private val CHIP_HEIGHT = 32.dp

/** The least width of the position: its part's name and « mes. 9/60 » stay readable whatever the chips. */
private val POSITION_MIN = 96.dp

/** A control of the strip: 48 dp of touch target both ways, centred on the strip's height. */
private fun Modifier.control(): Modifier = widthIn(min = TOUCH_HEIGHT).requiredHeight(TOUCH_HEIGHT)

/** The tempo in a small dark window, like a hardware display; a touch opens the tempo sheet. */
@Composable
private fun TempoButton(bpm: Double, open: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val said = tr("Tempo : %d BPM", Math.round(bpm).toInt())
    // Touched over the strip's whole height; the window drawn smaller inside.
    Box(
        Modifier.control().clip(shape)
            .clickable(role = Role.Button, onClickLabel = tr("Changer le tempo"), onClick = open)
            .semantics(mergeDescendants = true) { contentDescription = said }
            .spot("top_tempo", tr("Tempo"))
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
                Modifier.control().widthIn(min = POSITION_MIN).clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = pickable, role = Role.Button, onClickLabel = tr("Jouer depuis une partie")) { open = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .spot("top_position", tr("Jouer d'une partie"))
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
        ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.testTag("play_from_menu")) {
            Text(
                tr("Jouer depuis…"), style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            // Remix lot 1: the plain way to hear the whole song, first, whatever ▶ was set to loop.
            DropdownMenuItem(
                text = { Text(tr("Tout le morceau, du début"), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.Acid) },
                onClick = {
                    open = false
                    actions.playWholeSong()
                },
                modifier = Modifier.testTag("play_from_whole"),
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
private fun LoopPartChip(on: Boolean, set: (Boolean) -> Unit, iconOnly: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    val state = if (on) tr("activé") else tr("désactivé")
    val content = if (on) Neon.OnAccent else Neon.TextSecondary
    // A plain Row, not a Material component: its touch target is made 48 dp here.
    Box(
        Modifier.control().clip(shape)
            .toggleable(value = on, role = Role.Switch, onValueChange = set)
            .semantics {
                contentDescription = tr("Boucler la partie au lieu de jouer le morceau")
                stateDescription = state
            }
            .spot("top_loop_part", tr("Boucler la partie"))
            .testTag("top_loop_part"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.height(CHIP_HEIGHT).clip(shape)
                .background(if (on) Neon.Cyan else Color.Transparent)
                .border(1.dp, if (on) Neon.Cyan else Neon.Outline, shape)
                .padding(start = 8.dp, end = if (iconOnly) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Repeat, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            if (!iconOnly) {
                Text(tr("Boucler"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = content, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

/**
 * « Solo » in amber, in the strip, where the solo banner has no room (landscape): a touch hears
 * every track again. Says which tracks, for a screen reader.
 */
@Composable
private fun SoloChip(names: List<String>, clearAll: () -> Unit, iconOnly: Boolean = false) {
    val shape = RoundedCornerShape(16.dp)
    val said = tr("Solo actif sur %s", shortList(names))
    Box(
        Modifier.control().clip(shape)
            .clickable(role = Role.Button, onClickLabel = tr("Désactiver tous les solos"), onClick = clearAll)
            .semantics(mergeDescendants = true) { contentDescription = said }
            .testTag("solo_chip"),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.height(CHIP_HEIGHT).clip(shape).background(Neon.Amber).padding(start = 8.dp, end = if (iconOnly) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Headphones, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
            if (!iconOnly) {
                Text(tr("Solo"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
            }
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
        if (Mixer.isPad(ch)) tr("Pad %d", ch - Mixer.FIRST_PAD + 1)
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

/**
 * Phase 36, « ? qui étiquette l'écran »: one touch shows a label on each main control of the
 * screen, one more hides them. Lit in amber while they show.
 */
@Composable
private fun HelpLabelsButton(on: Boolean, toggle: () -> Unit, reportBug: (() -> Unit)? = null) {
    Box(
        Modifier.control().clip(CircleShape)
            .then(
                // A long press: « Signaler un bug », where help is looked for (the toggle stays one touch).
                if (reportBug == null) Modifier.toggleable(value = on, role = Role.Switch, onValueChange = { toggle() })
                else Modifier.combinedClickable(role = Role.Switch, onClick = toggle, onLongClick = reportBug, onLongClickLabel = tr("Signaler un bug"))
                    .semantics { toggleableState = androidx.compose.ui.state.ToggleableState(on) },
            )
            .semantics { contentDescription = tr("Étiqueter l'écran") }
            .testTag("help_labels_button").spot("help_labels_button"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(if (on) Neon.Amber else Color.Transparent).border(1.dp, if (on) Neon.Amber else Neon.Outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (on) Color.Black else Neon.TextSecondary)
        }
    }
}
