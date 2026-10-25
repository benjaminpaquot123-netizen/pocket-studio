@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.session

import com.pocketstudio.ui.ScaledBottomSheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.buildUpBefore
import com.pocketstudio.model.breakAfter
import com.pocketstudio.model.fadeIn
import com.pocketstudio.model.fadeOut
import com.pocketstudio.model.fillAtEnd
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.isFillPart
import com.pocketstudio.model.lengthenPart
import com.pocketstudio.model.partEnergy
import com.pocketstudio.model.partPlayedTimes
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.model.variationOfPart
import com.pocketstudio.model.withEnding
import com.pocketstudio.model.withPartBars
import com.pocketstudio.model.withPartEnergy
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors

/** One intention of the part sheet (« Une intention, un bouton »): each is one undo step. */
sealed interface PartIntent {
    /** A copy of the part's scene for this part only. */
    data object Variation : PartIntent
    /** The scene shared by several parts, shown to edit them all (« Toutes les 3 »). */
    data object EditAll : PartIntent
    data object Fill : PartIntent
    data class Break(val bars: Int) : PartIntent
    data class BuildUp(val bars: Int) : PartIntent
    data object FadeIn : PartIntent
    data object FadeOut : PartIntent
    data object Ending : PartIntent
    data class Energy(val level: Int) : PartIntent
    /** One pass of its scene more (1) or less (-1). */
    data class Length(val by: Int) : PartIntent
    data class Bars(val bars: Int) : PartIntent
}

/** [intent] done on song part [index] of [p] (the project as it is when nothing could be done). */
fun PartIntent.applyTo(p: Project, index: Int): Project = when (this) {
    PartIntent.Variation -> p.variationOfPart(index)
    PartIntent.EditAll -> p
    PartIntent.Fill -> p.fillAtEnd(index)
    is PartIntent.Break -> p.breakAfter(index, bars)
    is PartIntent.BuildUp -> p.buildUpBefore(index, bars)
    PartIntent.FadeIn -> p.fadeIn(index)
    PartIntent.FadeOut -> p.fadeOut(index)
    PartIntent.Ending -> p.withEnding()
    is PartIntent.Energy -> p.withPartEnergy(index, level)
    is PartIntent.Length -> p.lengthenPart(index, by)
    is PartIntent.Bars -> p.withPartBars(index, bars)
}

/** What the « Fait : … » line says once [intent] was done on the part titled [title]. */
fun PartIntent.doneText(title: String): String = when (this) {
    PartIntent.Variation -> tr("Fait : variation de %s, à modifier seule", title)
    PartIntent.EditAll -> title
    PartIntent.Fill -> tr("Fait : fill à la fin de %s", title)
    is PartIntent.Break -> tr("Fait : break de %1\$d mesures après %2\$s", bars, title)
    is PartIntent.BuildUp -> tr("Fait : montée de %1\$d mesures avant %2\$s", bars, title)
    PartIntent.FadeIn -> tr("Fait : fondu d'entrée sur %s", title)
    PartIntent.FadeOut -> tr("Fait : fondu de sortie sur %s", title)
    PartIntent.Ending -> tr("Fait : une fin de 4 mesures, le morceau s'arrête là")
    is PartIntent.Energy -> tr("Fait : énergie %1\$d sur %2\$s", level, title)
    is PartIntent.Length, is PartIntent.Bars -> tr("Fait : nouvelle longueur pour %s", title)
}

/** Why [intent] changed nothing (no room for a scene, a part or a curve). */
fun PartIntent.refusal(): String = when (this) {
    is PartIntent.BuildUp, PartIntent.FadeIn, PartIntent.FadeOut ->
        tr("Rien n'a changé : les 16 courbes d'automation sont prises. Retires-en une dans Mix.")
    else -> tr("Rien n'a changé : il n'y a plus de place pour une partie (24 au plus, 128 dans le morceau). Vide-en une pour continuer.")
}

/** What the part sheet shows about song part [index]. */
data class PartSheetUi(
    val index: Int,
    /** « B · Drop ». */
    val title: String,
    val bars: Int,
    /** Parts of the song that play its scene (this one included). */
    val played: Int,
    val first: Boolean,
    /** 1..4 (see [com.pocketstudio.model.partEnergy]). */
    val energy: Int = 3,
    val endsOnFill: Boolean = false,
    /** Whole passes of its scene (null: a length of its own, in bars). */
    val repeats: Int? = null,
    val songSize: Int = 1,
)

/** The sheet of song part [index] of [p] (null: no such part). */
fun partSheetUi(p: Project, index: Int): PartSheetUi? {
    val part = p.song.getOrNull(index) ?: return null
    return PartSheetUi(
        index = index, title = p.sceneTitle(part.scene), bars = (p.partSteps(part) / p.stepsPerBar).coerceAtLeast(1),
        played = p.partPlayedTimes(index), first = index == 0, energy = p.partEnergy(index),
        endsOnFill = p.isFillPart(part), repeats = if (part.bars == null) part.repeats else null, songSize = p.song.size,
    )
}

/** One button of the sheet (the timeline's menu shows those with [inMenu]). */
class PartChoice(val intent: PartIntent, val label: String, val tag: String, val icon: ImageVector, val enabled: Boolean = true, val inMenu: Boolean = true)

/** The intentions offered for a part, in the sheet's order: one list for the Morceau list and the timeline. */
fun partChoices(ui: PartSheetUi): List<PartChoice> = buildList {
    if (ui.played > 1) {
        add(PartChoice(PartIntent.Variation, tr("Celle-ci seulement"), "part_variation", Icons.Filled.AutoAwesome))
        add(PartChoice(PartIntent.EditAll, tr("Toutes les %d", ui.played), "part_edit_all", Icons.Filled.Edit, inMenu = false))
    } else {
        add(PartChoice(PartIntent.Variation, tr("Variation"), "part_variation", Icons.Filled.AutoAwesome))
    }
    add(PartChoice(PartIntent.Fill, tr("Fill à la fin"), "part_fill", Icons.Filled.GraphicEq, enabled = !ui.endsOnFill))
    add(PartChoice(PartIntent.Break(4), tr("Break après · 4 mes."), "part_break_4", Icons.Filled.Pause))
    add(PartChoice(PartIntent.Break(8), tr("Break après · 8 mes."), "part_break_8", Icons.Filled.Pause, inMenu = false))
    add(PartChoice(PartIntent.BuildUp(4), tr("Montée avant · 4 mes."), "part_buildup_4", Icons.AutoMirrored.Filled.TrendingUp, enabled = !ui.first))
    add(PartChoice(PartIntent.BuildUp(8), tr("Montée avant · 8 mes."), "part_buildup_8", Icons.AutoMirrored.Filled.TrendingUp, enabled = !ui.first, inMenu = false))
    add(PartChoice(PartIntent.FadeIn, tr("Fondu d'entrée"), "part_fade_in", Icons.AutoMirrored.Filled.VolumeUp))
    add(PartChoice(PartIntent.FadeOut, tr("Fondu de sortie"), "part_fade_out", Icons.AutoMirrored.Filled.VolumeDown))
    add(PartChoice(PartIntent.Ending, tr("Ajouter une fin"), "part_ending", Icons.Filled.Flag, enabled = ui.songSize < SongPart.MAX_PARTS))
}

/** The part's other actions (the Morceau list's: what plays, move, copy, insert, remove); null: none (the timeline has its own). */
class PartMoreActions(
    val whatPlays: () -> Unit = {},
    val moveUp: (() -> Unit)? = null,
    val moveDown: (() -> Unit)? = null,
    val duplicate: (() -> Unit)? = null,
    val insertAfter: (() -> Unit)? = null,
    val remove: () -> Unit = {},
)

/**
 * The part sheet (appui long ou ⋮ on a part): the intentions in big buttons, the energy and the
 * length; each touch does it and closes the sheet (one undo step, « Fait : … Annuler »).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartSheet(ui: PartSheetUi, onIntent: (PartIntent) -> Unit, close: () -> Unit, more: PartMoreActions? = null) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("part_sheet")) {
        PartSheetContent(ui, { close(); onIntent(it) }, close, more?.let { m ->
            PartMoreActions(
                whatPlays = { close(); m.whatPlays() },
                moveUp = m.moveUp?.let { f -> { close(); f() } },
                moveDown = m.moveDown?.let { f -> { close(); f() } },
                duplicate = m.duplicate?.let { f -> { close(); f() } },
                insertAfter = m.insertAfter?.let { f -> { close(); f() } },
                remove = { close(); m.remove() },
            )
        })
    }
}

/** The sheet's body (drawn alone in the screenshots). */
@Composable
fun PartSheetContent(ui: PartSheetUi, onIntent: (PartIntent) -> Unit, close: () -> Unit, more: PartMoreActions? = null) {
    val choices = partChoices(ui)
    fun choice(intent: PartIntent) = choices.first { it.intent == intent }
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Partie %1\$d · %2\$s", ui.index + 1, ui.title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("part_sheet_title"))
                Text(
                    com.pocketstudio.app.ui.barsShort(ui.bars) + if (ui.played > 1) "  ·  " + tr("jouée %d fois", ui.played) else "",
                    style = MonoStyle, color = Neon.TextSecondary,
                )
            }
            TextButton(onClick = close, modifier = Modifier.testTag("part_sheet_close")) { Text(tr("Fermer")) }
        }
        // The variation: when the scene is shared, the choice is said (« jouée 3 fois : celle-ci / toutes »).
        if (ui.played > 1) {
            Text(tr("Jouée %d fois dans le morceau. Modifier :", ui.played), style = MaterialTheme.typography.bodyMedium)
            PairRow(choice(PartIntent.Variation), choice(PartIntent.EditAll), onIntent)
        }
        val rest = choices.filter { it.intent != PartIntent.Variation && it.intent != PartIntent.EditAll }
        val buttons = (if (ui.played > 1) emptyList() else listOf(choice(PartIntent.Variation))) + rest
        buttons.chunked(2).forEach { row -> PairRow(row[0], row.getOrNull(1), onIntent) }
        // Energy: what plays (1 the pads, 2 + bass, 3 + drums, 4 everything and more).
        Text(tr("Énergie : %d", ui.energy), style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("part_energy_now"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(1 to tr("1 · nappes"), 2 to tr("2 · + basse"), 3 to tr("3 · + batterie"), 4 to tr("4 · à fond")).forEach { (level, label) ->
                FilterChip(
                    selected = ui.energy == level, onClick = { onIntent(PartIntent.Energy(level)) }, label = { Text(label) },
                    colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("part_energy_$level"),
                )
            }
        }
        // Length: a pass of its scene more or less, or the bars typed.
        Text(tr("Longueur"), style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { onIntent(PartIntent.Length(-1)) }, enabled = ui.bars > 1 && (ui.repeats == null || ui.repeats > 1), modifier = Modifier.testTag("part_shorter")) {
                Icon(Icons.Filled.Remove, contentDescription = tr("Raccourcir"))
            }
            IconButton(onClick = { onIntent(PartIntent.Length(1)) }, enabled = ui.repeats == null || ui.repeats < SongPart.MAX_REPEATS, modifier = Modifier.testTag("part_longer")) {
                Icon(Icons.Filled.Add, contentDescription = tr("Allonger"))
            }
            var typed by rememberSaveable(ui.index, ui.bars) { mutableStateOf(ui.bars.toString()) }
            val n = typed.toIntOrNull()?.takeIf { it in 1..SongPart.MAX_BARS }
            OutlinedTextField(
                typed, { typed = it.filter(Char::isDigit).take(3) }, singleLine = true,
                label = { Text(tr("mesures")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { n?.let { onIntent(PartIntent.Bars(it)) } }),
                modifier = Modifier.width(110.dp).testTag("part_bars_field"),
            )
            TextButton(onClick = { n?.let { onIntent(PartIntent.Bars(it)) } }, enabled = n != null && n != ui.bars, modifier = Modifier.testTag("part_bars_ok")) { Text(tr("OK")) }
        }
        if (more != null) {
            Text(tr("Partie"), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallAction(Icons.Filled.Tune, tr("Ce qui joue"), "part_what_plays_${ui.index}", true, more.whatPlays)
                SmallAction(Icons.Filled.ArrowUpward, tr("Monter"), "part_up_${ui.index}", more.moveUp != null) { more.moveUp?.invoke() }
                SmallAction(Icons.Filled.ArrowDownward, tr("Descendre"), "part_down_${ui.index}", more.moveDown != null) { more.moveDown?.invoke() }
                SmallAction(Icons.Filled.ContentCopy, tr("Dupliquer"), "part_duplicate_${ui.index}", more.duplicate != null) { more.duplicate?.invoke() }
                SmallAction(Icons.Filled.Add, tr("Insérer après"), "part_insert_${ui.index}", more.insertAfter != null) { more.insertAfter?.invoke() }
                SmallAction(Icons.Filled.Delete, tr("Retirer"), "part_remove_${ui.index}", true, more.remove)
            }
        }
        Spacer(Modifier.size(4.dp))
    }
}

/** Two big buttons side by side (one alone takes half the row). */
@Composable
private fun PairRow(a: PartChoice, b: PartChoice?, onIntent: (PartIntent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IntentButton(a, onIntent, Modifier.weight(1f))
        if (b != null) IntentButton(b, onIntent, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun IntentButton(c: PartChoice, onIntent: (PartIntent) -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = { onIntent(c.intent) }, enabled = c.enabled,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 56.dp).testTag(c.tag),
    ) {
        Icon(c.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (c.enabled) Neon.Cyan else Neon.TextSecondary)
        Spacer(Modifier.width(8.dp))
        com.pocketstudio.ui.WholeWordsText(c.label, Modifier.weight(1f), maxLines = 2)
    }
}

@Composable
private fun SmallAction(icon: ImageVector, label: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp).testTag(tag)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, maxLines = 1)
    }
}
