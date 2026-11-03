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
import androidx.compose.material3.DropdownMenu
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.clip(RoundedCornerShape(14.dp)).background(Neon.Surface).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            PadLive.PAGE_NAMES.forEachIndexed { page, letter ->
                val chosen = page == ui.page
                val filled = PadLive.filled(ui.pads, page) > 0
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (chosen) Neon.SurfaceHigh else Neon.Surface)
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
        PackPicker(ui, actions)
    }
}

@Composable
private fun PackPicker(ui: PadLiveUi, actions: PadLiveActions) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = PadLive.pack(ui.pack)
    Box {
        Row(
            Modifier.heightIn(min = 52.dp).widthIn(min = 120.dp).clip(RoundedCornerShape(14.dp)).background(Neon.Surface)
                .clickable(enabled = ui.busy == null, role = Role.Button) { open = true }
                .padding(horizontal = 14.dp).testTag("padlive_pack"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current?.let { tr("Pack : %s", packName(it)) } ?: tr("Pack…"),
                style = MaterialTheme.typography.titleMedium, color = Neon.TextPrimary, maxLines = 1,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Neon.TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
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
