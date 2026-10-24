package com.pocketstudio.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.learn.spot
import com.pocketstudio.app.synth.TrackBar
import com.pocketstudio.app.synth.TracksUi
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.TWO_PANES_FROM
import com.pocketstudio.ui.pillColors

// --- Sons: every track as a chip above its editor ---------------------------------------------------

/**
 * The Sons space's chips, above every editor: « Batterie », « Basse acide », each instrument
 * track (its menu, « Changer le son », « + Piste ») and « Voix et audio ». A touch opens that
 * track's editor; the chip of the editor shown is lit. [simple]: the Mode simple's Clavier, the
 * instrument tracks alone.
 */
@Composable
internal fun SonsHeader(tab: AppTab, tracks: TracksUi, go: (AppTab) -> Unit, audioTracks: Int, simple: Boolean = false) {
    val lying = androidx.compose.ui.platform.LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    Box(Modifier.fillMaxWidth().background(lerp(Neon.Background, Color.Black, 0.06f)).padding(horizontal = 8.dp, vertical = if (lying) 2.dp else 4.dp)) {
        TrackBar(
            tracks,
            modifier = Modifier.spot("sons_chips", tr("Tes pistes")),
            leading = {
                if (!simple) {
                    EditorChip(tr("Batterie"), tr("16 pads"), com.pocketstudio.app.live.DRUMS_COLOR, tab == AppTab.Rythme, "sons_Rythme") { go(AppTab.Rythme) }
                    EditorChip(tr("Basse acide"), "303", Color(0xFF81C784), tab == AppTab.Acide, "sons_Acide") { go(AppTab.Acide) }
                    // The voice next to them: always in sight, whatever the number of tracks after.
                    EditorChip(
                        tr("Voix"),
                        when (audioTracks) {
                            0 -> tr("Micro")
                            1 -> tr("%d piste", 1)
                            else -> tr("%d pistes", audioTracks)
                        },
                        Color(0xFFE57373), tab == AppTab.Audio, "sons_Audio",
                    ) { go(AppTab.Audio) }
                }
            },
        )
    }
}

/** A chip of the Sons space for an editor that is not an instrument track (the drums, the acid, the audio tracks). */
@Composable
private fun RowScope.EditorChip(label: String, sub: String, color: Color, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.height(44.dp).widthIn(max = 160.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) color.copy(alpha = 0.22f) else Neon.Surface)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) color else Neon.Outline), RoundedCornerShape(10.dp))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Column(Modifier.weight(1f, fill = false)) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// --- Structure: the timeline, the parts' list a switch away ------------------------------------------

/** « Ligne du temps | Parties »: the two views of the Structure space, one touch apart. */
@Composable
internal fun StructureSwitch(parts: Boolean, timelineEnabled: Boolean = true, setParts: (Boolean) -> Unit) {
    Row(
        Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(20.dp)).background(Neon.SurfaceHigh.copy(alpha = 0.55f))
            .spot("structure_switch", tr("Changer de vue")),
    ) {
        listOf(false to tr("Ligne du temps"), true to tr("Parties")).forEach { (isParts, label) ->
            val on = parts == isParts
            // An empty song has no timeline to show yet: its half waits, dimmed.
            val enabled = isParts || timelineEnabled
            Box(
                Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(20.dp)).background(if (on) Neon.Cyan else Color.Transparent)
                    .selectable(selected = on, enabled = enabled, role = Role.Tab) { setParts(isParts) }
                    .padding(horizontal = 14.dp)
                    .testTag(if (isParts) "structure_parts" else "structure_timeline"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1,
                    color = if (on) Neon.OnAccent else if (enabled) Neon.TextPrimary else Neon.TextSecondary.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// --- Plus: projects, settings, help, MIDI, latency, tools ---------------------------------------------

/** One entry of the Plus page: a page of its own ([page]) or something that opens elsewhere ([open]). */
internal class PlusEntry(val key: String, val icon: ImageVector, val title: String, val line: String, val page: ToolPage? = null, val open: (() -> Unit)? = null)

/** The Plus page's entries, in the order a lost beginner looks for them. */
internal fun plusEntries(
    projects: () -> Unit, glossary: () -> Unit, missions: () -> Unit, simple: Boolean, setSimple: (Boolean) -> Unit,
    /** « Signaler un bug » (null: not offered). */
    bugReport: (() -> Unit)? = null,
): List<PlusEntry> = listOfNotNull(
    PlusEntry("projects", Icons.Filled.FolderOpen, tr("Mes projets"), tr("Ouvrir, créer, dupliquer, envoyer un projet"), open = projects),
    PlusEntry("Aide", Icons.AutoMirrored.Filled.HelpOutline, tr("Aide"), tr("Le guide, la carte « Prochaine étape », le lexique"), page = ToolPage.Aide),
    PlusEntry("missions", Icons.Filled.Flag, tr("Missions : Mon premier morceau"), tr("Six petites étapes guidées, du style au partage"), open = missions),
    PlusEntry("glossary", Icons.AutoMirrored.Filled.MenuBook, tr("Les mots de la musique, à écouter"), tr("Chaque mot avec un exemple sonore"), open = glossary),
    PlusEntry("Reglages", Icons.Filled.Settings, tr("Réglages"), tr("Langue, thème, taille, son, mode simple"), page = ToolPage.Reglages),
    PlusEntry(
        "mode", Icons.Filled.ToggleOn, if (simple) tr("Passer en mode complet") else tr("Passer en mode simple"),
        if (simple) tr("Les cinq espaces : Structure, Sons, Mix, Live, Plus") else tr("Quatre onglets : Mon morceau, Rythme, Clavier, Envoyer"),
        open = { setSimple(!simple) },
    ),
    PlusEntry("Midi", Icons.Filled.Piano, tr("Clavier MIDI"), tr("Brancher un clavier, apprendre des potards"), page = ToolPage.Midi),
    PlusEntry("Latence", Icons.Filled.Timer, tr("Latence"), tr("Mesurer le retard du micro pour des prises calées"), page = ToolPage.Latence),
    PlusEntry("Fond", Icons.Filled.Headphones, tr("Lecture en arrière-plan"), tr("La musique continue écran éteint"), page = ToolPage.Fond),
    PlusEntry("Son", Icons.Filled.VolumeUp, tr("Son de test"), tr("Un la à 440 Hz pour vérifier la sortie"), page = ToolPage.Son),
    bugReport?.let { PlusEntry("bug", Icons.Filled.BugReport, tr("Signaler un bug"), tr("Ce qui s'est passé, avec des captures : c'est toi qui l'envoies"), open = it) },
    PlusEntry("Recette", Icons.Filled.Checklist, tr("Recette"), tr("La liste de tests de la version, et signaler un problème"), page = ToolPage.Recette),
)

/**
 * The Plus space: its entries in a list; a page opens in place with « ‹ Plus » to come back (on
 * a tablet, the list on the left and the page on the right, the last one chosen at first).
 */
@Composable
internal fun PlusSpace(entries: List<PlusEntry>, page: ToolPage?, open: (ToolPage) -> Unit, back: () -> Unit, widePage: ToolPage, content: @Composable (ToolPage) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("plus")) {
        if (maxWidth >= TWO_PANES_FROM) {
            val shown = page ?: widePage
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entries.forEach { e -> PlusRow(e, selected = e.page != null && e.page == shown) { e.page?.let(open) ?: e.open?.invoke() } }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { content(shown) }
            }
        } else if (page == null) {
            com.pocketstudio.ui.ReadingWidth(maxWidth = 640.dp) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entries.forEach { e -> PlusRow(e, selected = false) { e.page?.let(open) ?: e.open?.invoke() } }
                }
            }
        } else {
            androidx.activity.compose.BackHandler(onBack = back)
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).background(Neon.Surface).clickable(role = Role.Button, onClick = back).padding(horizontal = 8.dp)
                        .testTag("plus_back"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Revenir à Plus"), tint = Neon.Cyan)
                    Text(tr("Plus"), style = MaterialTheme.typography.labelLarge, color = Neon.Cyan, modifier = Modifier.padding(start = 6.dp))
                    Text(" · " + page.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.weight(1f)) { content(page) }
            }
        }
    }
}

@Composable
private fun PlusRow(e: PlusEntry, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(RoundedCornerShape(14.dp))
            .background(if (selected) Neon.Cyan.copy(alpha = 0.16f) else Neon.Surface)
            .border(1.dp, if (selected) Neon.Cyan else Neon.Outline, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(if (e.page != null) "tool_${e.page.name}" else "plus_${e.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Neon.Cyan.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(e.icon, contentDescription = null, tint = Neon.Cyan)
        }
        Column(Modifier.weight(1f)) {
            Text(e.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(e.line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Neon.TextSecondary)
    }
}

// --- Mode simple: « Mon morceau »'s tracks --------------------------------------------------------------

/** A track of « Mes pistes » (Mode simple): [key] as the song knows it, its mixer channel, its name in everyday words. */
class SimpleTrack(val key: String, val ch: Int, val name: String, val color: Color, val volume: Float, val mute: Boolean, val solo: Boolean)

class SimpleTracksActions(
    /** « Changer le son » of the track (the kit for the drums). */
    val changeSound: (String) -> Unit = {},
    val setVolume: (Int, Float) -> Unit = { _, _ -> },
    val toggleMute: (Int) -> Unit = {},
    val toggleSolo: (Int) -> Unit = {},
)

/**
 * « Mes pistes » under the song (Mode simple): a touch on a track's name changes its sound (the
 * kit for the drums); 🔊 opens its sheet (volume, « Couper », « Écouter seule »).
 */
@Composable
fun SimpleTracksCard(tracks: List<SimpleTrack>, actions: SimpleTracksActions) {
    var sheetFor by rememberSaveable { mutableStateOf<String?>(null) }
    NeonCard(Modifier.testTag("simple_tracks")) {
        SectionLabel(tr("Mes pistes"))
        Text(tr("Touche le nom d'une piste pour changer son son ; 🔊 pour son volume."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        tracks.forEachIndexed { i, t ->
            if (i > 0) HorizontalDivider(color = Neon.Outline.copy(alpha = 0.5f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Box(Modifier.size(12.dp).background(t.color, CircleShape))
                Row(
                    Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClickLabel = tr("Changer le son de %s", t.name)) { actions.changeSound(t.key) }
                        .padding(horizontal = 10.dp)
                        .spot("simple_track_${t.key}", if (i == 0) tr("Changer le son") else null)
                        .testTag("simple_track_${t.key}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(t.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Text("  " + tr("Changer le son"), style = MaterialTheme.typography.labelMedium, color = Neon.Cyan, maxLines = 1)
                }
                if (t.mute) Text(tr("coupée"), style = MaterialTheme.typography.labelSmall, color = Neon.Amber)
                IconButton(onClick = { sheetFor = t.key }, modifier = Modifier.testTag("simple_volume_${t.key}")) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = tr("Volume de %s", t.name), tint = if (t.mute) Neon.Amber else Neon.TextSecondary)
                }
            }
        }
    }
    sheetFor?.let { key ->
        val t = tracks.firstOrNull { it.key == key }
        if (t == null) sheetFor = null else SimpleTrackSheet(t, actions) { sheetFor = null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTrackSheet(t: SimpleTrack, actions: SimpleTracksActions, close: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = close, sheetState = state, containerColor = Neon.Surface, modifier = Modifier.testTag("simple_track_sheet")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = close) { Text(tr("Fermer")) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Volume"), Modifier.weight(1f))
                Text(com.pocketstudio.app.mixer.faderDb(t.volume), style = MonoStyle, color = Neon.Cyan)
            }
            ProSlider(value = t.volume, onValueChange = { actions.setVolume(t.ch, it) }, modifier = Modifier.testTag("simple_track_volume"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = t.mute, onClick = { actions.toggleMute(t.ch) }, label = { Text(tr("Couper")) }, colors = pillColors(Neon.Amber), modifier = Modifier.testTag("simple_track_mute"))
                FilterChip(selected = t.solo, onClick = { actions.toggleSolo(t.ch) }, label = { Text(tr("Écouter seule")) }, colors = pillColors(), modifier = Modifier.testTag("simple_track_solo"))
            }
            TextButton(onClick = { close(); actions.changeSound(t.key) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null)
                Text("  " + tr("Changer le son"))
            }
        }
    }
}
