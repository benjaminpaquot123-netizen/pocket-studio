package com.pocketstudio.app.mixer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.help.Glossary
import com.pocketstudio.model.Channel
import com.pocketstudio.model.EffectGroup
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.Insert
import com.pocketstudio.model.Macros
import com.pocketstudio.model.Scene
import com.pocketstudio.model.freeInsertSlot
import com.pocketstudio.model.matchingFx
import com.pocketstudio.model.scenesToggled
import com.pocketstudio.ui.fadingHorizontalScroll
import kotlin.math.roundToInt
import com.pocketstudio.model.longFxLimitNotice
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.pillColors

/**
 * The effects box of one strip, the same wherever a sound is chosen (synth tracks, acid, drum
 * kit and pads, audio tracks, remix and live pads, mixer): the effects in place (tap one to shape
 * it, switch it off, move it, remove it), « + Ajouter un effet » with every effect by family, the
 * ready-made treatments (the ones that suit the sound first), then, unless [compact], the pad XY,
 * the macros and the sends. [more]: in a compact box, opens the whole box.
 */
@Composable
fun EffectsBox(ch: Int, channel: Channel, actions: MixerActions, compact: Boolean = false, more: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().testTag("effects_box_$ch"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InsertChain(ch, channel.inserts, actions, alsoSet = channel.alsoSet())
        AlsoSet(ch, channel, actions)
        QuickFxPanel(ch, channel, actions.quick, kind = actions.kindOf(ch), compact = compact)
        if (!compact) {
            Text(tr("Réverbe et écho communs"), style = MaterialTheme.typography.labelLarge, color = Neon.Magenta)
            SendSliders(ch, channel.sends, actions)
        } else if (more != null) {
            OutlinedButton(onClick = more, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("fx_more_$ch")) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(tr("  Pad XY, macros, réverbe et écho communs"), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** The strip whose effects a screen shows under its sound, and what the box does. [more]: the whole box. */
class EffectsUi(val ch: Int, val channel: Channel, val actions: MixerActions, val more: (() -> Unit)? = null)

/** The effects box in a card of its own, right under a sound (Synthé, Acide). */
@Composable
fun EffectsCard(ui: EffectsUi) {
    NeonCard(accent = Neon.Magenta, modifier = Modifier.testTag("effects_card")) {
        SectionLabel(tr("Effets"), color = Neon.Magenta, help = Glossary.FX_PRESET, foldKey = "Effets du son")
        EffectsBox(ui.ch, ui.channel, ui.actions, compact = true, more = ui.more)
    }
}

/**
 * The warning's state after a new reading of the audio thread's [load] (1 = no time to spare):
 * on from 80 %, off again under 60 %, so it does not blink around one value.
 */
fun cpuBusyNext(busy: Boolean, load: Float): Boolean = if (busy) load > 0.6f else load >= 0.8f

/** Each family's colour in the list (the creative ones in their own violet). */
@Composable
private fun groupColour(group: EffectGroup): Color = when (group) {
    EffectGroup.COLOUR -> Neon.Magenta
    EffectGroup.MOTION -> Neon.Cyan
    EffectGroup.SPACE -> Neon.Acid
    EffectGroup.DYNAMICS -> Neon.Amber
    EffectGroup.CREATIVE -> Color(0xFFB388FF)
}

/**
 * The effects of a strip in the order the sound goes through them. Each one: tap to shape it,
 * switch it off and on (kept with its settings), move it up or down, remove it. Below,
 * « + Ajouter un effet » opens every effect by family; the list stays open to add several in a row.
 */
@Composable
fun InsertChain(ch: Int, inserts: List<Insert>, actions: MixerActions, alsoSet: Boolean = false) {
    var open by rememberSaveable(ch) { mutableIntStateOf(-1) }
    var picking by rememberSaveable(ch) { mutableStateOf(false) }
    val used = inserts.indices.filter { inserts[it].effect != EffectType.NONE }
    val busy by actions.cpuBusy.collectAsState()
    val scenes by actions.scenes.collectAsState()
    // The strip's level, read only where the lights are drawn (it redraws them, 20 times a second).
    val meters = actions.meters.collectAsState()
    val level = remember(ch, meters) { { meterLevel(meters.value.getOrElse(ch) { 0f }) } }
    actions.watchMeters?.let { watch ->
        val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        androidx.compose.runtime.LaunchedEffect(watch, owner) {
            owner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) { watch() }
        }
    }
    Column(Modifier.fillMaxWidth().testTag("fx_chain_$ch"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(tr("Effets en place : %1\$d sur %2\$d", used.size, inserts.size), style = MaterialTheme.typography.labelLarge, color = Neon.Magenta)
        if (busy) {
            Text(
                tr("Le téléphone est à bout de souffle : trop d'effets lourds en même temps, le son peut craquer. Coupe ou retire quelques effets, ici ou sur d'autres sons."),
                style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("fx_cpu_busy"),
            )
        }
        // With an ambience that sets no effect in place, what it does set is listed just below.
        if (used.isEmpty() && !alsoSet) {
            Text(tr("Aucun pour l'instant : ajoute un effet, ou touche une ambiance toute prête."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        used.forEachIndexed { pos, i ->
            val ins = inserts[i]
            InsertRow(
                ch, i, pos, ins, open == i,
                before = used.getOrNull(pos - 1), after = used.getOrNull(pos + 1),
                toggle = { open = if (open == i) -1 else i }, actions = actions,
                removed = { if (open == i) open = -1 }, level = level,
            )
            // « Joue dans »: only worth it with several scenes (song sections), or to say it plays nowhere.
            if (scenes.size > 1 || ins.scenes?.isEmpty() == true) PlaysIn(ch, i, ins, scenes, actions)
            if (open == i) InsertEditor(ch, i, ins, actions)
        }
        if (scenes.size > 1 && used.isNotEmpty()) {
            Text(tr("Un effet peut ne jouer que dans certaines parties : touche ses lettres."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("fx_scenes_help_$ch"))
        }
        OutlinedButton(
            onClick = { picking = !picking },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("fx_add_$ch"),
        ) {
            Icon(if (picking) Icons.Filled.Close else Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(if (picking) tr("  Fermer la liste des effets") else tr("  Ajouter un effet"))
        }
        if (picking) EffectPicker(ch, inserts, actions) { slot -> open = slot }
    }
}

@Composable
private fun InsertRow(
    ch: Int, slot: Int, pos: Int, ins: Insert, selected: Boolean, before: Int?, after: Int?,
    toggle: () -> Unit, actions: MixerActions, removed: () -> Unit, level: () -> Float,
) {
    SideEffect { insertRowCompositions++ }
    val type = ins.effect
    // A narrow screen (a small phone, a big interface size): the four buttons go under the chip,
    // which keeps the effect's name readable (« 1. V… » otherwise).
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
    val stacked = maxWidth < INSERT_ROW_ONE_LINE
    val chip = @Composable { m: Modifier ->
        FilterChip(
            selected = selected, onClick = toggle,
            label = {
                // A long name (« Compresseur ») shrinks a little rather than ending in « Compres… ».
                com.pocketstudio.ui.WholeWordsText(
                    "${pos + 1}. ${tr(type.label)}" + if (ins.bypass) " · " + tr("coupé") else "",
                    maxLines = 2, minScale = 0.8f,
                    style = androidx.compose.ui.text.TextStyle(textDecoration = if (ins.bypass) TextDecoration.LineThrough else null),
                    color = if (ins.bypass) Neon.TextSecondary else Color.Unspecified,
                )
            },
            // Its light and mini view: what it does, lit by the sound going through it.
            // (On the chip filled with its colour when open: drawn in the dark ink of its label.)
            leadingIcon = { FxActivity(ins, if (selected) Neon.OnAccent else groupColour(type.group), level, "fx_activity_${ch}_$slot") },
            colors = pillColors(groupColour(type.group)),
            modifier = m.testTag("fx_chip_${ch}_$slot"),
        )
    }
    val buttons = @Composable {
        IconButton(onClick = { actions.setInsertBypass(ch, slot, !ins.bypass) }, modifier = Modifier.testTag("fx_bypass_${ch}_$slot")) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = if (ins.bypass) tr("Rallumer l'effet") else tr("Couper l'effet"),
                tint = if (ins.bypass) Neon.TextSecondary else Neon.Acid,
            )
        }
        IconButton(onClick = { before?.let { actions.moveInsert(ch, slot, it) } }, enabled = before != null, modifier = Modifier.testTag("fx_up_${ch}_$slot")) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = tr("Monter l'effet"))
        }
        IconButton(onClick = { after?.let { actions.moveInsert(ch, slot, it) } }, enabled = after != null, modifier = Modifier.testTag("fx_down_${ch}_$slot")) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = tr("Descendre l'effet"))
        }
        IconButton(onClick = { removed(); actions.setInsertType(ch, slot, EffectType.NONE) }, modifier = Modifier.testTag("fx_remove_${ch}_$slot")) {
            Icon(Icons.Filled.Close, contentDescription = tr("Retirer l'effet"), tint = Neon.TextSecondary)
        }
    }
    if (stacked) {
        Column(Modifier.fillMaxWidth()) {
            chip(Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { buttons() }
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            chip(Modifier.weight(1f))
            buttons()
        }
    }
    }
}

/** From this width an effect's chip and its four buttons share one line. */
private val INSERT_ROW_ONE_LINE = 340.dp

/**
 * « Joue dans » under an effect: « Tout » (every scene), or the scenes (song sections) it plays
 * in, by letter and name. Elsewhere the sound goes through it untouched, switching right on the
 * scene change, in the song and in the export alike.
 */
@Composable
private fun PlaysIn(ch: Int, slot: Int, ins: Insert, labels: List<String>, actions: MixerActions) {
    val colours = pillColors(Neon.Acid)
    // No scene left (its only scene was replaced by another one): it is silent, and says so.
    if (ins.scenes?.isEmpty() == true) {
        Text(
            tr("Ne joue dans aucune partie : touche une lettre ou « Tout »."),
            style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("fx_scenes_none_${ch}_$slot"),
        )
    }
    Row(
        Modifier.fillMaxWidth().fadingHorizontalScroll().testTag("fx_scenes_${ch}_$slot"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(tr("Joue dans :"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, softWrap = false)
        FilterChip(
            selected = ins.scenes == null, onClick = { actions.setInsertScenes(ch, slot, null) },
            label = { Text(tr("Tout"), maxLines = 1, softWrap = false) }, colors = colours,
            modifier = Modifier.testTag("fx_scene_${ch}_${slot}_all"),
        )
        labels.forEachIndexed { i, label ->
            val letter = Scene.NAMES.getOrElse(i) { "?" }
            FilterChip(
                selected = ins.scenes?.contains(i) == true,
                onClick = { actions.setInsertScenes(ch, slot, ins.scenesToggled(i, labels.size)) },
                label = { Text(if (label.isBlank()) letter else "$letter · ${Scene.shownLabel(label)}", maxLines = 1, softWrap = false) },
                colors = colours, modifier = Modifier.testTag("fx_scene_${ch}_${slot}_$i"),
            )
        }
    }
}

/** The shared reverb and echo sends and the quick controls this strip has away from rest (see [AlsoSet]). */
private fun Channel.alsoSetKeys(): List<String> =
    sends.indices.filter { sends[it] > 0.005f }.map { "send$it" } +
        macros.indices.filter { kotlin.math.abs(macros[it] - Macros.DEFAULTS[it]) > 0.005f }.map { "macro$it" }

/** Something besides the effects in place changes the sound (a ready-made ambience may set only that). */
internal fun Channel.alsoSet(): Boolean = alsoSetKeys().isNotEmpty()

/**
 * What changes the sound besides the effects in place: the shared reverb and echo sends and the
 * quick controls away from rest, each with its slider, under the ambience that set them. A
 * ready-made ambience like « Petite pièce » sets no effect in place, only these: the box shows
 * them, as the strip's « Effets : » line names it. A line stays until another ambience is chosen
 * (a slider taken down to 0 does not vanish under the finger).
 */
@Composable
private fun AlsoSet(ch: Int, channel: Channel, actions: MixerActions) {
    val vibe = channel.matchingFx()?.takeIf { it.id != FxPresets.NONE.id }
    // Testers (lot « tuto 4 »): a send or a knob moved in this box made its line appear here, above, and the
    // whole box slid down under the finger. The lines are those set when the box opened, or by the ambience
    // chosen since (only what it sets); a slider moved by hand is already in sight below.
    val now = channel.alsoSetKeys().toSet()
    var kept by remember(ch) { mutableStateOf(now) }
    var vibeSeen by remember(ch) { mutableStateOf(vibe?.id) }
    androidx.compose.runtime.SideEffect {
        if (vibe != null && vibe.id != vibeSeen) kept = now
        vibeSeen = vibe?.id
    }
    val keys = kept
    if (keys.isEmpty()) return
    Column(Modifier.fillMaxWidth().testTag("fx_also_$ch"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            vibe?.let { tr("« %s » règle aussi :", tr(it.name)) } ?: tr("Réglé aussi sur ce son :"),
            style = MaterialTheme.typography.labelLarge, color = Neon.Magenta,
        )
        channel.sends.indices.filter { "send$it" in keys }.forEach { bus ->
            val v = channel.sends[bus]
            LabeledSlider(if (bus == 0) tr("Réverbe commune") else tr("Écho commun"), com.pocketstudio.model.i18n.percent((v * 100).roundToInt()), v, "fx_also_send_${ch}_$bus") {
                actions.setSend(ch, bus, it)
            }
        }
        channel.macros.indices.filter { "macro$it" in keys }.forEach { m ->
            val v = channel.macros[m]
            LabeledSlider(tr(Macros.NAMES[m]), macroText(m, v), v, "fx_also_macro_${ch}_$m") { actions.quick.setMacro(ch, m, it) }
        }
    }
}

/** One effect's knobs, under its row. */
@Composable
private fun InsertEditor(ch: Int, slot: Int, ins: Insert, actions: MixerActions) {
    Column(Modifier.fillMaxWidth().testTag("fx_editor_${ch}_$slot")) {
        if (ins.effect.blurb.isNotEmpty()) {
            Text(tr(ins.effect.blurb), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        if (ins.effect == EffectType.EQ || ins.effect == EffectType.PARAM_EQ) EqCurve(ins)
        if (ins.effect == EffectType.COMPRESSOR && !ins.bypass) ReductionMeter(ch, slot, actions)
        ins.effect.params.forEachIndexed { k, param ->
            LabeledSlider(tr(param.label), tr(param.format(ins.params[k])), ins.params[k], "fxp_${ch}_${slot}_$k") {
                actions.setInsertParam(ch, slot, k, it)
            }
        }
    }
}

/** Every effect, by family. Each touch adds one after the others; the list stays open. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectPicker(ch: Int, inserts: List<Insert>, actions: MixerActions, added: (Int) -> Unit) {
    var notice by remember(ch) { mutableStateOf<String?>(null) }
    val full = freeInsertSlot(inserts) == null
    Column(Modifier.fillMaxWidth().testTag("fx_picker_$ch"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            if (full) tr("%d effets au maximum sur ce son : retires-en un pour en ajouter un autre.", inserts.size)
            else tr("Touche les effets voulus, ils s'ajoutent à la suite, dans l'ordre."),
            style = MaterialTheme.typography.bodySmall, color = if (full) Neon.Amber else Neon.TextSecondary,
        )
        EffectGroup.entries.forEach { group ->
            val colour = groupColour(group)
            Text(tr(group.label), style = MaterialTheme.typography.labelLarge, color = colour)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                EffectType.entries.filter { it.group == group && it != EffectType.NONE }.forEach { type ->
                    FilterChip(
                        selected = false, enabled = !full,
                        onClick = {
                            val slot = freeInsertSlot(inserts)
                            val limit = EffectType.longLimit(type)
                            notice = when {
                                slot == null -> null
                                type.long && actions.longFxCount(type) >= limit -> longFxLimitNotice(type)
                                else -> {
                                    actions.setInsertType(ch, slot, type)
                                    added(slot)
                                    null
                                }
                            }
                        },
                        label = { Text(tr(type.label), maxLines = 1, softWrap = false) },
                        leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = pillColors(colour),
                        modifier = Modifier.testTag("fx_pick_${ch}_${type.name}"),
                    )
                }
            }
        }
        notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Amber) }
    }
}

/**
 * How much the compressor in [slot] of strip [ch] turns the sound down right now (its gain
 * reduction, from the engine's meters), as a bar growing to the left from 0 dB and its value.
 */
@Composable
private fun ReductionMeter(ch: Int, slot: Int, actions: MixerActions) {
    val reductions = actions.reductions.collectAsState()
    val index = ch * com.pocketstudio.model.Mixer.INSERTS + slot
    // Read only when drawn: the bar and its number redraw, not the whole box (20 times a second).
    val db = { reductions.value.getOrElse(index) { 0f }.coerceIn(-REDUCTION_RANGE_DB, 0f) }
    val bar = Neon.Amber
    val track = Neon.SurfaceHigh
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).testTag("fx_gr_${ch}_$slot"), verticalAlignment = Alignment.CenterVertically) {
        Text(tr("Réduction"), style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.Canvas(Modifier.weight(1f).padding(horizontal = 10.dp).height(8.dp)) {
            drawRect(track)
            val w = size.width * -db() / REDUCTION_RANGE_DB
            drawRect(bar, topLeft = androidx.compose.ui.geometry.Offset(size.width - w, 0f), size = androidx.compose.ui.geometry.Size(w, size.height))
        }
        ReductionValue(db)
    }
}

@Composable
private fun ReductionValue(db: () -> Float) {
    val v = db()
    Text(if (v > -0.05f) "0 dB" else com.pocketstudio.model.i18n.ltrNumber(String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "−%.1f dB", -v)),
        style = com.pocketstudio.ui.MonoStyle, color = Neon.Cyan)
}

/** The reduction meter's span: a bar full at -24 dB. */
private const val REDUCTION_RANGE_DB = 24f

/** A meter's peak (linear) as a light's brightness: -48 dB dark, 0 dB full. */
internal fun meterLevel(peak: Float): Float =
    if (peak <= 0f || !peak.isFinite()) 0f else ((20f * kotlin.math.log10(peak) + 48f) / 48f).coerceIn(0f, 1f)

/** Tests: how many times an effect's row was composed (the meters must only redraw it). */
@androidx.annotation.VisibleForTesting
internal var insertRowCompositions = 0
