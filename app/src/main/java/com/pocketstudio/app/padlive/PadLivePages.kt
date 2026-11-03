package com.pocketstudio.app.padlive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.TrackChanges
import com.pocketstudio.ui.ScaledDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import kotlinx.coroutines.flow.update
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon

/** A pack's name as shown: its style's (translated), « Fête » for the ready-to-play set. */
internal fun packName(pack: PadLive.Pack): String = if (pack.style == null) tr("Fête") else DefaultNames.shown(pack.name)

/**
 * Under the bar: the pages A to D (a small dot under a page that holds sounds) and « Pack : Fête ▾ »,
 * which puts another style's pads on the page shown (« Annuler » brings the page back).
 */
@Composable
internal fun PageRow(ui: PadLiveUi, actions: PadLiveActions) {
    // A small phone (360 dp): the page letters a little narrower, « Fête ▾ » without « Pack : »,
    // the challenge's target alone (TalkBack still says « Défi »).
    // Lot B3 (persona Léo): the « Mode retours » bubble sat on « Défi » and « Pack » and took their touches: it keeps clear of this row.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { com.pocketstudio.app.retours.Retours.keepClear.update { it - "padlive_pages" } }
    }
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().onGloballyPositioned { c -> com.pocketstudio.app.retours.Retours.keepClear.update { it + ("padlive_pages" to c.boundsInWindow()) } },
    ) {
        val compact = maxWidth < 380.dp
        // Narrower (most phones, since the row's « ? »): « Défi » shows only its target, the pack keeps its name.
        val tiny = maxWidth < 420.dp
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Neon.Surface).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                PadLive.PAGE_NAMES.forEachIndexed { page, letter ->
                    val chosen = page == ui.page
                    val filled = PadLive.filled(ui.pads, page) > 0
                    Box(
                        Modifier.size(width = if (compact) 36.dp else 44.dp, height = 44.dp).clip(RoundedCornerShape(10.dp)).background(if (chosen) Neon.SurfaceHigh else Neon.Surface)
                            .clickable(role = Role.Tab) { actions.setPage(page) }
                            .semantics {
                                selected = chosen
                                contentDescription = tr("Page %s", letter) + if (filled) "" else " · " + tr("vide")
                            }
                            .testTag("padlive_page_$letter"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(letter, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            color = if (chosen) Neon.TextPrimary else Neon.TextSecondary)
                        if (filled && !chosen) {
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(4.dp).clip(CircleShape).background(Neon.TextSecondary))
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) { PackPicker(ui, actions, compact) }
            // Lot « Aide à jour »: pages, pack and the challenge's target in one « ? » (it fits beside them at 360 dp).
            com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.ScreenHelp.PADLIVE_PAGES, about = listOf("padlive_page_*", "padlive_pack", "padlive_challenge_open"))
            ChallengeButton(ui, actions, tiny)
        }
    }
}

/** Lot B3: « 🎯 Défi » (the mockup's), lit while a challenge is open. */
@Composable
private fun ChallengeButton(ui: PadLiveUi, actions: PadLiveActions, compact: Boolean) {
    val on = ui.challenge != ChallengeUi.Off
    Row(
        Modifier.heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Neon.Amber.copy(alpha = 0.22f) else Neon.Surface)
            .clickable(role = Role.Button, onClick = actions.challenge)
            .semantics { contentDescription = tr("Défi : refais ce rythme") }
            .padding(horizontal = if (compact) 10.dp else 14.dp).testTag("padlive_challenge_open"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(androidx.compose.material.icons.Icons.Filled.TrackChanges, contentDescription = null, tint = Neon.Red, modifier = Modifier.size(20.dp))
        if (!compact) Text(" " + tr("Défi"), style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun PackPicker(ui: PadLiveUi, actions: PadLiveActions, compact: Boolean = false) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = PadLive.pack(ui.pack)
    Box {
        Row(
            Modifier.heightIn(min = 52.dp).widthIn(min = if (compact) 80.dp else 120.dp).clip(RoundedCornerShape(14.dp)).background(Neon.Surface)
                .clickable(enabled = ui.busy == null, role = Role.Button) { open = true }
                .padding(horizontal = 14.dp).testTag("padlive_pack"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current?.let { if (compact) packName(it) else tr("Pack : %s", packName(it)) } ?: tr("Pack…"),
                style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Neon.TextSecondary)
        }
        ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            PadLive.PACKS.forEach { pack ->
                DropdownMenuItem(
                    text = { Text(packName(pack)) },
                    onClick = {
                        open = false
                        actions.choosePack(pack.id)
                    },
                    modifier = Modifier.testTag("padlive_pack_${pack.id}"),
                )
            }
        }
    }
}
