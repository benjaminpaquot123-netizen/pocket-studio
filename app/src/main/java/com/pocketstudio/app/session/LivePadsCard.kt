@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.session

import com.pocketstudio.model.i18n.tr
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.pocketstudio.ui.fadingHorizontalScroll
import com.pocketstudio.ui.HelpDot
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import com.pocketstudio.app.help.Glossary
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.pocketstudio.ui.aspectRatioAtMost
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.LivePads
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

class LivePadsUi(
    val pads: LivePads,
    /** Per pad: 0 off, 1 waiting for its beat, 2 playing. */
    val states: List<Int>,
    /** The pad being set up, or null when the grid is for playing. */
    val editing: Int?,
    /** Drum pads that have a sound of their own (index to name), to reuse on a live pad. */
    val drumSounds: List<Pair<Int, String>> = emptyList(),
    val busy: String? = null,
)

class LivePadsActions(
    val tap: (Int) -> Unit = {},
    val stopAll: () -> Unit = {},
    val setSize: (Int) -> Unit = {},
    /** Pad to set up, or null to go back to playing. */
    val edit: (Int?) -> Unit = {},
    val import: (Int) -> Unit = {},
    /** The sound library, to put one of its sounds on this pad. */
    val library: (Int) -> Unit = {},
    /** Audio › Générer: a loop made from a sentence. */
    val generate: () -> Unit = {},
    val fromDrumPad: (Int, Int) -> Unit = { _, _ -> },
    val setBeats: (Int, Int) -> Unit = { _, _ -> },
    val setOneShot: (Int, Boolean) -> Unit = { _, _ -> },
    val setGain: (Int, Float) -> Unit = { _, _ -> },
    val clear: (Int) -> Unit = {},
)

private fun beatsLabel(beats: Int) = when {
    beats == 1 -> tr("1 temps")
    beats < 4 -> tr("%d temps", beats)
    beats % 4 == 0 -> com.pocketstudio.app.ui.barsShort(beats / 4)
    else -> tr("%d temps", beats)
}

/** Live pads: a tap switches a sound on (in time), the next tap switches it off. */
@Composable
fun LivePadsCard(ui: LivePadsUi, actions: LivePadsActions) {
    NeonCard(accent = Neon.Cyan, modifier = Modifier.testTag("live_pads")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Pads live"), Modifier.weight(1f), help = Glossary.LIVE_PADS)
            TextButton(onClick = actions.stopAll, modifier = Modifier.testTag("live_stop_all")) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Text(" " + tr("Tout couper"))
            }
        }
        Text(
            if (ui.editing == null) tr("Touche un pad pour lancer son son, retouche-le pour l'arrêter. Tout reste calé sur le temps.")
            else tr("Choisis le son du pad %d et sa longueur, puis « Jouer ».", ui.editing + 1),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Pads :"), style = MaterialTheme.typography.bodyMedium)
            LivePads.SIZES.forEach { size ->
                FilterChip(
                    selected = ui.pads.size == size,
                    onClick = { actions.setSize(size) },
                    enabled = size >= ui.pads.used,
                    label = { Text("$size") },
                    modifier = Modifier.testTag("live_size_$size"),
                )
            }
        }
        val blink = rememberInfiniteTransition(label = "waiting")
        val waitAlpha by blink.animateFloat(0.3f, 1f, infiniteRepeatable(tween(250), RepeatMode.Reverse), label = "waitAlpha")
        (0 until ui.pads.size step 4).forEach { start ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in start until start + 4) {
                    LivePad(i, ui.pads.clips.getOrNull(i), ui.states.getOrElse(i) { 0 }, ui.editing == i, waitAlpha, Modifier.weight(1f)) {
                        when {
                            ui.editing != null -> actions.edit(i)
                            ui.pads.clips.getOrNull(i) == null -> actions.edit(i)
                            else -> actions.tap(i)
                        }
                    }
                }
            }
        }
        ui.busy?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Amber) }
        if (ui.editing == null) {
            OutlinedButton(onClick = { actions.edit(0) }, modifier = Modifier.fillMaxWidth().testTag("live_edit")) { Text(tr("Régler les pads")) }
        } else {
            PadEditor(ui.editing, ui.pads.clips.getOrNull(ui.editing), ui.drumSounds, actions)
        }
    }
}

@Composable
private fun LivePad(i: Int, clip: LiveClip?, state: Int, selected: Boolean, waitAlpha: Float, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    // Empty: hollow. Loaded: in relief. Playing: lit green with a glow.
    val fill = when {
        clip == null -> Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.3f)))
        state == 2 -> Brush.verticalGradient(listOf(lerp(Neon.Acid, Color.White, 0.2f), Neon.Acid))
        else -> Brush.verticalGradient(listOf(lerp(Neon.SurfaceHigh, Color.White, 0.06f), Neon.SurfaceHigh))
    }
    val border = when {
        selected -> BorderStroke(3.dp, Neon.Cyan)
        state == 1 -> BorderStroke(3.dp, Neon.Acid.copy(alpha = waitAlpha))
        clip == null -> BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    }
    Box(
        modifier.aspectRatioAtMost(1f, 96.dp)
            .then(if (state == 2) Modifier.shadow(14.dp, shape, ambientColor = Neon.Acid, spotColor = Neon.Acid) else Modifier)
            .clip(shape).background(fill).border(border, shape).clickable(onClick = onClick).testTag("live_pad_$i"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
            val ink = if (state == 2) Color(0xFF14210F) else if (clip == null) Neon.TextSecondary else Neon.TextPrimary
            Text(clip?.name?.let { com.pocketstudio.model.DefaultNames.shownSample(it) } ?: "+", color = ink, fontSize = if (clip == null) 22.sp else 12.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
            if (clip != null) Text(if (clip.oneShot) tr("une fois") else beatsLabel(clip.beats), color = ink.copy(alpha = 0.75f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PadEditor(pad: Int, clip: LiveClip?, drumSounds: List<Pair<Int, String>>, actions: LivePadsActions) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("live_editor")) {
        Text(tr("Pad %1\$d : %2\$s", pad + 1, clip?.name?.let { com.pocketstudio.model.DefaultNames.shownSample(it) } ?: tr("vide")), style = MaterialTheme.typography.titleSmall)
        Button(onClick = { actions.library(pad) }, modifier = Modifier.fillMaxWidth().testTag("live_library")) {
            Icon(Icons.Filled.LibraryMusic, contentDescription = null)
            Text("  " + tr("Bibliothèque de sons"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.generate, modifier = Modifier.weight(1f).testTag("live_generate")) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Text(" " + tr("Générer"))
            }
            OutlinedButton(onClick = { actions.import(pad) }, modifier = Modifier.weight(1f).testTag("live_import")) {
                Icon(Icons.Filled.FileOpen, contentDescription = null)
                Text(" " + tr("Du téléphone"))
            }
        }
        Text(tr("« Générer » fabrique une boucle à partir d'une phrase (Audio › Générer), puis la pose sur un pad live libre."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        if (drumSounds.isNotEmpty()) {
            Text(tr("Ou le son d'un pad de batterie :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                drumSounds.forEach { (drum, name) ->
                    OutlinedButton(onClick = { actions.fromDrumPad(pad, drum) }) { Text("${drum + 1}. ${com.pocketstudio.model.DefaultNames.shownSample(name).take(14)}", maxLines = 1) }
                }
            }
        }
        if (clip != null) {
            FlowRow(itemVerticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(false to tr("En boucle"), true to tr("Une fois")).forEach { (once, label) ->
                    FilterChip(selected = clip.oneShot == once, onClick = { actions.setOneShot(pad, once) }, label = { Text(label) })
                }
                HelpDot(Glossary.LOOP)
            }
            if (!clip.oneShot) {
                Text(tr("Repart tous les :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LivePads.BEATS.forEach { b ->
                        FilterChip(selected = clip.beats == b, onClick = { actions.setBeats(pad, b) }, label = { Text(beatsLabel(b)) },
                            modifier = Modifier.testTag("live_beats_$b"))
                    }
                }
            }
            Text(tr("Volume"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            ProSlider(value = clip.gain / 2f, onValueChange = { actions.setGain(pad, it * 2f) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (clip != null) {
                OutlinedButton(onClick = { actions.clear(pad) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(" " + tr("Vider"))
                }
            }
            OutlinedButton(onClick = { actions.edit(null) }, modifier = Modifier.weight(1f).testTag("live_done")) { Text(tr("Terminé")) }
        }
    }
}
