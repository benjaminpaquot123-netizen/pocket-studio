@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.mixer

import com.pocketstudio.model.i18n.inSentence
import com.pocketstudio.ui.ScaledAlertDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Acting
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.Buses
import com.pocketstudio.model.CurveShape
import com.pocketstudio.model.Macros
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.Scene
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.model.whatActs
import com.pocketstudio.ui.Neon
import kotlin.math.abs
import kotlin.math.roundToInt

/** Where a row of « Ce qui joue » is edited. */
sealed class Where {
    /** A track's effects box (its effects, quick controls and sends). */
    data class Effects(val ch: Int) : Where()

    /** Automation lane [lane] (Mixeur › Automation). */
    data class Lane(val lane: Int) : Where()

    /** The master (Mixeur › Sortie générale). */
    data object Master : Where()

    /** The shared reverb and echo (Mixeur). */
    data object Buses : Where()

    /** Every export choice (Mixeur › Exporter), from « Prêt à partager ? » › « Autres exports ». */
    data object Export : Where()
}

/** One thing acting on the sound, said in plain words, and where it is edited. */
data class ActRow(val title: String, val detail: String, val where: Where, val moves: Boolean, val tag: String, val short: String = title)

/** A track (or the master, or the shared buses) and what acts on it. */
data class ActGroup(val ch: Int, val name: String, val color: Color?, val rows: List<ActRow>)

/** A percentage of 0..1. */
private fun pct(v: Float) = (v * 100).roundToInt()

/** What a curve does, in a sentence. */
fun shapeText(s: CurveShape): String {
    val what = when (s.kind) {
        CurveShape.Kind.EMPTY -> return tr("Courbe vide : rien ne bouge encore.")
        CurveShape.Kind.STEADY -> tr("Reste à %d %%.", pct(s.first))
        CurveShape.Kind.RISES -> tr("Monte de %1\$d %% à %2\$d %%.", pct(s.first), pct(s.last))
        CurveShape.Kind.FALLS -> tr("Descend de %1\$d %% à %2\$d %%.", pct(s.first), pct(s.last))
        CurveShape.Kind.UP_DOWN -> tr("Monte jusqu'à %d %% puis redescend.", pct(s.max))
        CurveShape.Kind.DOWN_UP -> tr("Descend jusqu'à %d %% puis remonte.", pct(s.min))
        CurveShape.Kind.MOVES -> tr("Bouge entre %1\$d %% et %2\$d %%.", pct(s.min), pct(s.max))
    }
    return if (s.bars > 0) what + " " + trCount(s.bars, "Sur 1 mesure.", "Sur %d mesures.") else what
}

/** How a lane plays: each loop, each scene its own, or over the whole song (and who wrote it). */
fun laneModeText(lane: AutomationLane): String {
    val mode = when (lane.mode) {
        AutomationLane.LOOP -> tr("À chaque tour de boucle.")
        AutomationLane.PER_SCENE -> tr("Une courbe par partie.")
        else -> tr("Sur tout le morceau.")
    }
    val extra = listOfNotNull(
        tr("Écrite depuis la page Live.").takeIf { lane.songPerBeat || lane.target.kind == AutoTarget.LIVE },
        tr("Entendue seulement quand tout le morceau joue.").takeIf { lane.songOnly },
    )
    return (listOf(mode) + extra).joinToString(" ")
}

/**
 * Everything acting on the sound of [p] in scene [scene] (null: the whole song), by track, in plain
 * words: names from [strips] (the drum pads' own strips by their number); [onlyCh]: one track's.
 */
fun whatPlaysGroups(p: Project, scene: Int?, strips: List<StripInfo>, onlyCh: Int? = null): List<ActGroup> {
    val acts = p.whatActs(scene).filter { onlyCh == null || it.ch == onlyCh }
    val channels = p.mixer.channels
    val master = p.mixer.master
    fun nameOf(ch: Int): String? = when {
        ch == Mixer.MASTER -> tr("Sortie générale")
        ch == Acting.BUSES -> tr("Réverbe et écho communs")
        Mixer.isPad(ch) -> tr("Pad %d", ch - Mixer.FIRST_PAD + 1)
        else -> strips.firstOrNull { it.ch == ch }?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) }
    }
    val order = strips.map { it.ch } + (Mixer.FIRST_PAD until Mixer.FIRST_PAD + Mixer.PADS) + Mixer.MASTER + Acting.BUSES
    return acts.groupBy { it.ch }.mapNotNull { (ch, list) ->
        val name = nameOf(ch) ?: return@mapNotNull null
        val rows = list.map { describe(it, channels, master, strips) }
        // What moves first, one row each; the effects and sends (edited in one place) in one row.
        val still = rows.filter { !it.moves }
        val merged = if (still.size > 1) listOf(
            ActRow(
                if (ch == Mixer.MASTER) tr("Effets de la sortie") else tr("Effets et envois"),
                still.joinToString(" · ") { it.short } + (still.firstOrNull { it.detail.contains("⏻") }?.let { " — " + tr("certains sont coupés (⏻).") }.orEmpty()),
                still.first().where, false, "act_fx_${ch}", 
            ),
        ) else still
        ActGroup(ch, name, strips.firstOrNull { it.ch == ch }?.color, rows.filter { it.moves } + merged)
    }.sortedBy { order.indexOf(it.ch).let { i -> if (i < 0) order.size else i } }
}

private fun describe(a: Acting, channels: List<com.pocketstudio.model.Channel>, master: List<com.pocketstudio.model.Insert>, strips: List<StripInfo>): ActRow = when (a) {
    is Acting.Effect -> {
        val e = a.insert.effect
        val notes = listOfNotNull(
            e.blurb.takeIf { it.isNotBlank() }?.let { tr(it) } ?: tr("Un effet sur le son, dans l'ordre de la chaîne."),
            tr("Coupé (⏻) : ses réglages sont gardés.").takeIf { a.insert.bypass },
            a.insert.scenes?.let { s -> tr("Joue seulement dans : %s.", s.sorted().joinToString(", ") { Scene.NAMES.getOrElse(it) { "?" } }) },
        )
        ActRow(tr(e.label), notes.joinToString(" "), if (a.ch == Mixer.MASTER) Where.Master else Where.Effects(a.ch), false, "act_fx_${a.ch}_${a.slot}",
            short = tr(e.label) + (if (a.insert.bypass) " (⏻)" else ""))
    }
    is Acting.Send -> ActRow(
        if (a.bus == 0) tr("Envoi vers la réverbe") else tr("Envoi vers l'écho"),
        if (a.bus == 0) tr("%d %% du son part dans la réverbe commune : de l'espace.", pct(a.level))
        else tr("%d %% du son part dans l'écho commun : des répétitions en rythme.", pct(a.level)),
        Where.Effects(a.ch), false, "act_send_${a.ch}_${a.bus}",
        short = (if (a.bus == 0) tr("réverbe %d %%") else tr("écho %d %%")).format(pct(a.level)),
    )
    is Acting.Macro -> {
        val (low, high) = Macros.ENDS[a.macro]
        val centred = Macros.DEFAULTS[a.macro] > 0f
        val amount = if (centred) pct(abs(a.value - 0.5f) * 2f) else pct(a.value)
        val side = if (centred && a.value < 0.5f) low else high
        ActRow(tr(Macros.NAMES[a.macro]), tr("Réglage rapide : %1\$s à %2\$d %%.", tr(side).inSentence(), amount), Where.Effects(a.ch), false, "act_macro_${a.ch}_${a.macro}", short = tr(Macros.NAMES[a.macro]).inSentence() + " " + amount + " %")
    }
    is Acting.Layered -> {
        val l = a.layer
        val notes = listOfNotNull(
            tr("Joue les notes de la piste avec un autre son : %s.", l.sound.label),
            tr("Transposée de %s demi-tons.", com.pocketstudio.model.Layers.signedSemitones(l.transpose)).takeIf { l.transpose != 0 },
            tr("Ses effets : %s.", l.inserts.joinToString(", ") { ins -> val e = ins.effect; tr(e.label) }).takeIf { l.inserts.isNotEmpty() },
            tr("Coupée pour l'instant.").takeIf { l.mute },
        )
        ActRow(tr("Couche : %s", l.title), notes.joinToString(" "), Where.Effects(a.ch), false, "act_layer_${a.track}_${a.index}", short = l.title)
    }
    is Acting.MasterTrim -> ActRow(
        tr("Volume général"),
        tr("%s dB sur la sortie générale.", com.pocketstudio.model.i18n.ltrNumber(String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%+.1f", a.db))),
        Where.Master, false, "act_trim",
        short = tr("volume %s dB", com.pocketstudio.model.i18n.ltrNumber(String.format(com.pocketstudio.model.i18n.I18n.lang.locale, "%+.1f", a.db))),
    )
    is Acting.Moves -> ActRow(
        tr("Bouge : %s", targetShortName(a.automation.target, channels, master, strips)),
        listOf(targetHint(a.automation.target, channels, master), laneModeText(a.automation), shapeText(a.shape)).filter { it.isNotBlank() }.joinToString(" "),
        Where.Lane(a.lane), true, "act_lane_${a.lane}",
    )
}

/** A track has automation: how many lanes move it (the strip's badge). */
fun movesOn(p: Project, ch: Int): Int = p.automation.count { it.target.ch == ch && it.target.kind != AutoTarget.BUS_PARAM && it.target.kind != AutoTarget.LIVE }

/**
 * « Ce qui joue »: everything acting on the sound of a scene or of the whole song, by track, each
 * row with what it does and a button that opens where it is edited. [scene]: null for the whole
 * song; [scenes]: the scenes to choose from (index to title); [onlyCh]: one track's, with a way back to all.
 */
@Composable
fun WhatPlaysDialog(
    p: Project, strips: List<StripInfo>, scene: Int?, onlyCh: Int?, scenes: List<Pair<Int, String>>,
    setScene: (Int?) -> Unit, showAll: () -> Unit, go: (Where) -> Unit, dismiss: () -> Unit,
    /** Where « Modifier » is offered (the Mode simple: a track's effects and the master's level only). */
    canGo: (Where) -> Boolean = { true },
) {
    val groups = androidx.compose.runtime.remember(p, strips, scene, onlyCh) { whatPlaysGroups(p, scene, strips, onlyCh) }
    val trackName = onlyCh?.let { ch -> strips.firstOrNull { it.ch == ch }?.name?.let { com.pocketstudio.model.DefaultNames.shown(it) } }
    ScaledAlertDialog(
        onDismissRequest = dismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 10.dp).testTag("what_plays"),
        title = { Text(if (trackName != null) tr("Ce qui bouge sur %s", trackName) else tr("Ce qui joue")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) { WhatPlaysContent(groups, scene, onlyCh, scenes, setScene, showAll, go, canGo) }
        },
        confirmButton = { TextButton(onClick = dismiss, modifier = Modifier.testTag("what_plays_close")) { Text(tr("Fermer")) } },
    )
}

@Composable
private fun GroupBlock(g: ActGroup, go: (Where) -> Unit, canGo: (Where) -> Boolean = { true }) {
    Column(Modifier.fillMaxWidth().testTag("what_plays_group_${g.ch}"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 4.dp, height = 20.dp).background(g.color ?: Neon.Cyan, RoundedCornerShape(2.dp)))
            Text(g.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp).weight(1f))
            val moving = g.rows.count { it.moves }
            if (moving > 0) Text(trCount(moving, "1 courbe", "%d courbes"), style = MaterialTheme.typography.labelMedium, color = Neon.Amber)
        }
        g.rows.forEach { r ->
            Row(
                Modifier.fillMaxWidth().background(Neon.SurfaceHigh.copy(alpha = 0.6f), RoundedCornerShape(10.dp)).padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp).testTag(r.tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (r.moves) Icons.AutoMirrored.Filled.ShowChart else if (r.where is Where.Effects) Icons.Filled.AutoAwesome else Icons.Filled.Tune,
                    contentDescription = null, tint = if (r.moves) Neon.Amber else Neon.Cyan, modifier = Modifier.size(20.dp),
                )
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(r.title, style = MaterialTheme.typography.bodyLarge)
                    if (r.detail.isNotBlank()) Text(r.detail, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
                if (!canGo(r.where)) {
                    // Nothing to open from here (the Mode simple has no Mix): said, not offered.
                } else if (r.moves) {
                    FilledTonalButton(
                        onClick = { go(r.where) }, modifier = Modifier.heightIn(min = 48.dp).testTag("${r.tag}_go"),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = Neon.Amber, contentColor = Neon.OnAccent),
                    ) { Text(tr("Modifier")) }
                } else {
                    androidx.compose.material3.OutlinedButton(onClick = { go(r.where) }, modifier = Modifier.heightIn(min = 48.dp).testTag("${r.tag}_go")) { Text(tr("Modifier")) }
                }
            }
        }
    }
}

/** The list of « Ce qui joue »: the scope chosen (the song or a scene), then each track and its rows. */
@Composable
fun WhatPlaysContent(
    groups: List<ActGroup>, scene: Int?, onlyCh: Int?, scenes: List<Pair<Int, String>>,
    setScene: (Int?) -> Unit, showAll: () -> Unit, go: (Where) -> Unit,
    canGo: (Where) -> Boolean = { true },
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            tr("Tout ce qui agit sur le son : effets, envois, courbes qui bougent. « Modifier » t'emmène là où ça se règle."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(selected = scene == null, onClick = { setScene(null) }, label = { Text(tr("Tout le morceau")) }, modifier = Modifier.testTag("what_plays_song"))
            scenes.forEach { (i, title) ->
                FilterChip(selected = scene == i, onClick = { setScene(i) }, label = { Text(title, maxLines = 1) }, modifier = Modifier.testTag("what_plays_scene_$i"))
            }
        }
        if (onlyCh != null) {
            TextButton(onClick = showAll, modifier = Modifier.heightIn(min = 48.dp).testTag("what_plays_all")) { Text(tr("Voir toutes les pistes")) }
        }
        if (groups.isEmpty()) {
            Text(tr("Rien n'agit sur le son ici : pas d'effet, pas d'envoi, rien qui bouge."), style = MaterialTheme.typography.bodyMedium)
        }
        groups.forEach { g -> GroupBlock(g, go, canGo) }
    }
}
