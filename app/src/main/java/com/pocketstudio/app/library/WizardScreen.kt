package com.pocketstudio.app.library

import androidx.compose.foundation.layout.heightIn
import com.pocketstudio.ui.ScaledAlertDialog

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import com.pocketstudio.ui.TileGrid
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.ChipRow
import com.pocketstudio.ui.FamilyChip
import com.pocketstudio.ui.FoldSection
import com.pocketstudio.app.sounds.SoundLooks
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Apps
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketstudio.app.rhythm.DrumKitInfo
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.StarterWizard.Role
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

class WizardActions(
    val close: () -> Unit = {},
    val chooseStyle: (String) -> Unit = {},
    val next: () -> Unit = {},
    val back: () -> Unit = {},
    val setEnabled: (Role, Boolean) -> Unit = { _, _ -> },
    val setCategory: (Role, String) -> Unit = { _, _ -> },
    val choose: (Role, SoundOption) -> Unit = { _, _ -> },
    val setMood: (StarterWizard.Mood?) -> Unit = {},
    val setPartEnabled: (StarterWizard.Part, Boolean) -> Unit = { _, _ -> },
    val setAcidEnabled: (Boolean) -> Unit = {},
    /** The 303's step editor: a touch (next note), a long touch (accent, slide). */
    val acidTap: (Int) -> Unit = {},
    val acidHold: (Int) -> Unit = {},
    val generateAcid: () -> Unit = {},
    val setAcidDensity: (Float) -> Unit = {},
    val setAcidBars: (Int) -> Unit = {},
    val acidFromStyle: () -> Unit = {},
    val chooseAcidSound: (String) -> Unit = {},
    val setAcidKnob: (Int, Float) -> Unit = { _, _ -> },
    val choosePartSound: (StarterWizard.Part, StarterWizard.PartSound) -> Unit = { _, _ -> },
    /** « Motifs de basse »: the bass's grid (null: the style's own line). */
    val chooseBassGrid: (String?) -> Unit = {},
    val preview: (SoundRef) -> Unit = {},
    val download: (String) -> Unit = {},
    val cancelDownload: (String) -> Unit = {},
    val deletePack: (String) -> Unit = {},
    val create: () -> Unit = {},
)

/**
 * The beginner's assistant for a new song: the style, then one step per line of the beat (what
 * it does, which sound from the library), then a summary that creates the song.
 */
@Composable
fun WizardScreen(ui: WizardUi, packs: PacksUi, playing: SoundRef?, actions: WizardActions, preparing: SoundRef? = null) {
    // Cards fold here as everywhere (a long step's explanation, the sound choice), remembered by title.
    CompositionLocalProvider(LocalListening provides Listening(playing, preparing)) {
        WizardBody(ui, packs, playing, actions)
    }
}

/** What is heard now, and what is being prepared to be heard. */
private class Listening(val playing: SoundRef?, val preparing: SoundRef?)

private val LocalListening = staticCompositionLocalOf { Listening(null, null) }

/** ▶ to hear [ref], ■ while it plays, a small wheel while it is prepared; with [label], a button. */
@Composable
private fun ListenButton(ref: SoundRef, onClick: () -> Unit, modifier: Modifier = Modifier, label: String? = null) {
    val listening = LocalListening.current
    val icon: @Composable () -> Unit = {
        when (ref) {
            listening.preparing -> CircularProgressIndicator(
                Modifier.size(18.dp).semantics { contentDescription = tr("Préparation de l'écoute") }, strokeWidth = 2.dp, color = Neon.Cyan,
            )
            listening.playing -> Icon(Icons.Filled.Stop, contentDescription = tr("Arrêter l'écoute"), tint = Neon.Cyan)
            else -> Icon(Icons.Filled.PlayArrow, contentDescription = tr("Écouter"), tint = Neon.Cyan)
        }
    }
    if (label == null) IconButton(onClick = onClick, modifier = modifier) { icon() }
    else OutlinedButton(onClick = onClick, modifier = modifier) {
        icon()
        Text("  $label")
    }
}

@Composable
private fun WizardBody(ui: WizardUi, packs: PacksUi, playing: SoundRef?, actions: WizardActions) {
    val style = ui.style
    // Steps to read: centred at a comfortable width on a tablet.
    com.pocketstudio.ui.ReadingWidth {
        Column(Modifier.fillMaxSize().background(Neon.Background).testTag("wizard")) {
            // From the third step on, closing loses real choices: asked first.
            var confirmClose by rememberSaveable { mutableStateOf(false) }
            val total = wizardStepCount(ui)
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Nouveau morceau, pas à pas"), style = MaterialTheme.typography.titleLarge)
                    // Lot 2: no « sur 13 » before a style is chosen (the count is the style's: 10 in Lo-fi).
                    Text(
                        if (style == null) tr("Étape 1 : choisis un style") else tr("Étape %d sur %d", ui.step + 1, total) + " · ${tr(style.name)}",
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("wizard_step"),
                    )
                }
                IconButton(
                    onClick = { if (ui.step >= CONFIRM_CLOSE_FROM) confirmClose = true else actions.close() },
                    modifier = Modifier.testTag("wizard_close"),
                ) { Icon(Icons.Filled.Close, contentDescription = tr("Fermer")) }
            }
            if (style != null) StepProgress(ui.step + 1, total)
            if (confirmClose) {
                ScaledAlertDialog(
                    onDismissRequest = { confirmClose = false },
                    title = { Text(tr("Quitter sans créer le morceau ?")) },
                    text = { Text(tr("Tes choix seront perdus. Tu pourras le relancer depuis Mes projets › « Nouveau morceau guidé ».")) },
                    confirmButton = {
                        TextButton(onClick = { confirmClose = false; actions.close() }, modifier = Modifier.testTag("wizard_close_confirm")) { Text(tr("Quitter")) }
                    },
                    dismissButton = { TextButton(onClick = { confirmClose = false }) { Text(tr("Continuer")) } },
                )
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val current = ui.current) {
                    StarterWizard.Step.Style -> StyleStep(ui.styleId, packs, actions)
                    StarterWizard.Step.Mood -> MoodStep(style!!, ui.mood, packs, actions)
                    is StarterWizard.Step.Drum -> LineStep(style!!, current.line, ui.rows[current.line.role], ui.plan, packs, playing, actions)
                    StarterWizard.Step.Acid -> AcidStep(style!!, ui, actions)
                    is StarterWizard.Step.Melodic -> PartStep(style!!, ui, packs, current.part, playing, actions)
                    StarterWizard.Step.Summary -> SummaryStep(ui, style!!)
                }
                ui.error?.let { Text(it, color = Neon.Amber, modifier = Modifier.testTag("wizard_error")) }
            }
            if (style != null && ui.step > 0 && ui.step < ui.lastStep) {
                // The details are for whoever wants them: the advised choices are already made.
                // Lot 2: a real button (a thin orange link was missed by every beginner).
                androidx.compose.material3.FilledTonalButton(
                    onClick = actions.create, enabled = !ui.creating,
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = Neon.Cyan.copy(alpha = 0.2f), contentColor = Neon.Cyan),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 12.dp).testTag("wizard_create_now"),
                ) { Text(if (ui.creating) tr("Préparation…") else tr("Créer tout de suite avec les choix conseillés"), textAlign = TextAlign.Center) }
            }
            if (style != null && ui.step > 0) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = actions.back, modifier = Modifier.weight(1f).testTag("wizard_back")) { Text(tr("Retour")) }
                    if (ui.step >= ui.lastStep) {
                        Button(onClick = actions.create, enabled = !ui.creating, modifier = Modifier.weight(1f).testTag("wizard_create")) {
                            Text(if (ui.creating) tr("Préparation…") else tr("Créer le morceau"))
                        }
                    } else {
                        Button(onClick = actions.next, modifier = Modifier.weight(1f).testTag("wizard_next")) { Text(tr("Suivant")) }
                    }
                }
            }
        }
    }
}

/** Steps of the assistant: before a style is chosen, those of the first style (most have as many). */
internal fun wizardStepCount(ui: WizardUi): Int =
    if (ui.style != null) ui.lastStep + 1 else StarterWizard.steps(StarterWizard.STYLES.first()).size

/** Closing the assistant from this step on (the third) asks first. */
internal const val CONFIRM_CLOSE_FROM = 2

/** How far the assistant is: the steps done in the accent colour on a plain track, nothing else. */
@Composable
private fun StepProgress(step: Int, total: Int) {
    val fraction = (step.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(6.dp)
            .clip(RoundedCornerShape(3.dp)).background(Neon.SurfaceHigh).testTag("wizard_progress"),
    ) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Neon.Cyan))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleStep(chosen: String?, packs: PacksUi, actions: WizardActions) {
    Text(
        tr("Choisis un style : je prépare la batterie, puis les parties du morceau. Tu pourras tout changer.") + " " +
            tr("Touche une carte pour lire sa description, ▶ pour l'écouter."),
        style = MaterialTheme.typography.bodyMedium,
    )
    // The card opened to be read (the chosen style's when coming back).
    var open by rememberSaveable { mutableStateOf(chosen) }
    StyleFamilies("wizard", style = { s ->
        StyleCard(s, s.id == chosen, s.id == open, packs.installed.any { it.id == s.id }, { open = if (open == s.id) null else s.id }, actions)
    }, open = listOfNotNull(chosen?.let { StarterWizard.style(it) }?.family).toSet(), calm = com.pocketstudio.app.settings.SettingsStore.state.value.simpleMode)
    InstalledPacks(packs, actions)
}

/**
 * One style, compact so that five or six fit on a phone: ▶ to hear it, its name and tempo, two
 * lines of description, its best known reference. A touch opens it ([expanded]): the whole
 * description, every reference, and "Choisir ce style".
 */
@Composable
private fun StyleCard(s: StarterWizard.Style, chosen: Boolean, expanded: Boolean, installed: Boolean, toggle: () -> Unit, actions: WizardActions) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(Neon.Surface)
            .border(if (chosen || expanded) 1.5.dp else 1.dp, if (chosen || expanded) Neon.Cyan else Neon.Outline, shape)
            .clickable(onClick = toggle)
            .padding(start = 2.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)
            .testTag("wizard_style_${s.id}"),
        verticalAlignment = Alignment.Top,
    ) {
        val demo = SoundRef.StyleDemo(s.id)
        ListenButton(demo, { actions.preview(demo) }, Modifier.testTag("wizard_style_listen_${s.id}"))
        Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The name takes what the tempo leaves; the pack's tick stays right after it.
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(s.name), style = MaterialTheme.typography.titleMedium, color = if (chosen || expanded) Neon.Cyan else Neon.TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (installed) {
                        Icon(Icons.Filled.Check, contentDescription = tr("Pack de sons téléchargé"), tint = Neon.Acid,
                            modifier = Modifier.padding(start = 4.dp).size(16.dp).testTag("wizard_style_installed_${s.id}"))
                    }
                }
                // Its family when it says something ("Techno" under Techno, "Autres" do not).
                val family = s.family.takeIf { it != s.name && it != StarterWizard.FAMILIES.last() }
                Text(listOfNotNull(family?.let { tr(it) }, "${s.bpm.roundToInt()} BPM").joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                    color = Neon.TextSecondary, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
            Text(tr(s.description), style = MaterialTheme.typography.bodySmall, color = if (expanded) Neon.TextPrimary else Neon.TextSecondary,
                maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis)
            if (s.references.isNotEmpty() && !expanded) {
                Text(tr("Pour l'idée :") + " " + s.references.first(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                    color = Neon.TextSecondary.copy(alpha = 0.8f), fontStyle = FontStyle.Italic, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (expanded) {
                if (s.references.isNotEmpty()) {
                    Text(tr("Pour l'idée :"), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    // Every reference, the best known first (the recipe's order).
                    s.references.forEach { r ->
                        Text("• $r", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, fontStyle = FontStyle.Italic)
                    }
                }
                // ▶ stays at the card's left: one wide button to choose, readable in every language.
                Button(onClick = { actions.chooseStyle(s.id) }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("wizard_style_choose_${s.id}")) {
                    Text(tr("Choisir ce style"), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** The packs on the phone, each with what it takes and a way to delete it. */
@Composable
private fun InstalledPacks(packs: PacksUi, actions: WizardActions) {
    if (packs.installed.isEmpty()) return
    NeonCard(modifier = Modifier.testTag("wizard_installed_packs")) {
        SectionLabel(tr("Packs téléchargés"), help = Glossary.SOUND_PACK)
        packs.installed.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("%s · %d sons", m.name, m.sounds.size), modifier = Modifier.weight(1f))
                TextButton(onClick = { actions.deletePack(m.id) }, modifier = Modifier.testTag("wizard_delete_${m.id}")) { Text(tr("Supprimer")) }
            }
        }
        Text(tr("Supprimer un pack libère la place ; tes morceaux gardent les sons qu'ils utilisent."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineStep(
    style: StarterWizard.Style, line: StarterWizard.Line, row: WizardRow?, plan: StarterWizard.ScenePlan,
    packs: PacksUi, playing: SoundRef?, actions: WizardActions,
) {
    val role = line.role
    val color = DrumKitInfo.colors[role.lane]
    NeonCard(modifier = Modifier.testTag("wizard_line_${role.id}")) {
        SectionLabel(tr(role.title), color = color)
        Text(tr(role.explanation), style = MaterialTheme.typography.bodyLarge)
        Text(styleTip(style, line.tip), style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Neon.TextSecondary)
        PatternPreview(line.pattern, color)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Utiliser cette ligne"), modifier = Modifier.weight(1f))
            Switch(checked = row?.enabled ?: false, onCheckedChange = { actions.setEnabled(role, it) }, modifier = Modifier.testTag("wizard_enabled"))
        }
        plan.lines[role]?.let { SceneBadge(it, plan) }
    }
    if (row == null || !row.enabled) {
        Text(tr("Cette ligne restera vide : tu pourras l'ajouter plus tard en touchant les cases de la grille."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        return
    }
    val categories = soundCategories(role, style.id, packs.installed)
    val look = SoundLooks.of(role.id)
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Folded or not for this line only (the same title in every step).
            SectionLabel(tr("Choisis le son"), Modifier.weight(1f), foldKey = "Choisis le son : ${role.id}")
            row.sound?.let { sound ->
                val demo = SoundRef.LineDemo(role, sound)
                ListenButton(demo, { actions.preview(demo) }, Modifier.testTag("wizard_line_listen"), label = tr("Avec le rythme"))
            }
        }
        // The sources on one line that scrolls: the sounds stay right under them.
        ChipRow(categories.size, categories.indexOfFirst { it.id == row.category }) { i ->
            val c = categories[i]
            FamilyChip(
                c.title.substringAfter(" · ", c.title), if (c.id == KIT_CATEGORY) Icons.Filled.Apps else Icons.Filled.Inventory2, look.color,
                selected = c.id == row.category, onClick = { actions.setCategory(role, c.id) },
                modifier = Modifier.testTag("wizard_category_${c.id}"),
            )
        }
        val options = categories.firstOrNull { it.id == row.category }?.options.orEmpty()
        val listening = LocalListening.current
        TileGrid(options.size) { i, m ->
            val option = options[i]
            SoundTile(
                title = tr(option.name.substringBefore(" · ")), icon = if (listening.playing == option.ref) Icons.Filled.GraphicEq else look.icon,
                accent = look.color, selected = option == row.sound, onClick = { actions.choose(role, option) },
                subtitle = if (listening.playing == option.ref) tr("En écoute…") else null,
                modifier = m.testTag("wizard_sound_$i"),
            )
        }
        Text(tr("Touche un son pour l'écouter et le choisir ; « Avec le rythme » le fait entendre avec les lignes déjà choisies."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
    PackOffer(style, packs, actions)
}

/**
 * The packs that make the style sound right (its machines, its own sounds) not yet on the phone:
 * one button gets them all, and the assistant then takes its sounds from them.
 */
@Composable
private fun StylePacks(style: StarterWizard.Style, packs: PacksUi, actions: WizardActions) {
    val index = (packs.index as? IndexState.Ready)?.index ?: return
    val missing = style.packs.mapNotNull { id -> index.packs.firstOrNull { it.id == id } }
        .filter { e -> packs.installed.none { it.id == e.id } }
    if (missing.isEmpty()) return
    val downloading = missing.mapNotNull { packs.downloading[it.id] }
    NeonCard(accent = Neon.Cyan, modifier = Modifier.testTag("wizard_style_packs")) {
        SectionLabel(tr("Les vrais sons du style"))
        Text(
            tr("Pour que ça sonne vraiment %s : %s (%s). L'assistant prendra ses sons dedans.", tr(style.name), missing.joinToString(", ") { it.name }, mb(missing.sumOf { it.sizeBytes })),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (downloading.isNotEmpty()) {
            LinearProgressIndicator(progress = { downloading.average().toFloat() }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
        } else {
            Button(onClick = { missing.forEach { actions.download(it.id) } }, modifier = Modifier.fillMaxWidth().testTag("wizard_style_packs_download")) {
                Text(tr("Télécharger les sons du style"))
            }
        }
    }
}

private fun mb(bytes: Long) =
    if (bytes < 1_000_000) tr("%d ko", (bytes / 1000).coerceAtLeast(1)) else tr("%.1f Mo", bytes / 1e6)

/** The style's own pack: to download (with its size), downloading, or there (and deletable). */
@Composable
private fun PackOffer(style: StarterWizard.Style, packs: PacksUi, actions: WizardActions) {
    val installed = packs.installed.firstOrNull { it.id == style.id }
    val entry: PackEntry? = (packs.index as? IndexState.Ready)?.index?.packs?.firstOrNull { it.id == style.id }
    val progress = packs.downloading[style.id]
    when {
        installed != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Pack %s : %d sons sur l'appareil.", installed.name, installed.sounds.size), style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary, modifier = Modifier.weight(1f))
            TextButton(onClick = { actions.deletePack(style.id) }, modifier = Modifier.testTag("wizard_delete_pack")) { Text(tr("Supprimer")) }
        }
        progress != null -> NeonCard {
            Text(tr("Téléchargement du pack %s… %d %%", tr(style.name), (progress * 100).roundToInt()))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
            TextButton(onClick = { actions.cancelDownload(style.id) }) { Text(tr("Annuler")) }
        }
        entry != null -> NeonCard(modifier = Modifier.testTag("wizard_pack_offer")) {
            Text(tr("Plus de sons %s : un pack de %d sons (%s), à télécharger seulement si tu veux.", tr(style.name), entry.sounds, entry.sizeLabel),
                style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { actions.download(style.id) }, modifier = Modifier.fillMaxWidth().testTag("wizard_download")) {
                Text(tr("Télécharger le pack %s (%s)", tr(style.name), entry.sizeLabel))
            }
        }
        else -> Unit
    }
    packs.message?.let { Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodySmall) }
}

/** The line's groove, one square per step, filled as strong as the hit; the beats numbered, a gap between bars. */
@Composable
private fun PatternPreview(pattern: String, color: androidx.compose.ui.graphics.Color) {
    val hits = StarterWizard.hits(pattern)
    val bars = pattern.indices.chunked(16)
    Column(Modifier.fillMaxWidth().testTag("wizard_pattern"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bars.forEach { bar ->
                Row(Modifier.weight(bar.size.toFloat()), horizontalArrangement = Arrangement.spacedBy(if (pattern.length > 16) 2.dp else 3.dp)) {
                    bar.forEach { step ->
                        val hit = hits[step]
                        Box(
                            Modifier.weight(1f).aspectRatio(0.7f).clip(RoundedCornerShape(3.dp))
                                .background(if (hit != null) color.copy(alpha = 0.3f + 0.7f * hit.velocity) else if (step % 4 == 0) Neon.SurfaceHigh else Neon.Surface)
                                .border(1.dp, if (step % 4 == 0) Neon.TextSecondary.copy(alpha = 0.5f) else Neon.Outline, RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            // A roll: its number of hits in the square.
                            if (hit != null && hit.repeats > 1) Text("${hit.repeats}", style = MaterialTheme.typography.labelSmall, color = Neon.Background)
                        }
                    }
                }
            }
        }
        // The beats' numbers under their first square: "1 2 3 4", and again for a second bar.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bars.forEach { bar ->
                Row(Modifier.weight(bar.size.toFloat())) {
                    bar.chunked(4).forEachIndexed { beat, steps ->
                        Text("${beat + 1}", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, modifier = Modifier.weight(steps.size.toFloat()))
                    }
                }
            }
        }
    }
}

/** "En House : un coup…": the style's own word on a line or a part (its first letter in lower case after the colon). */
private fun styleTip(style: StarterWizard.Style, tip: String): String {
    val text = tr(tip)
    val lower = if (text.length > 1 && text[1].isLowerCase()) text.replaceFirstChar { it.lowercase() } else text
    return tr("En %s : %s", tr(style.name), lower)
}

/**
 * The style's 303 line, to make one's own: heard over the beat in a loop, its steps touched one by
 * one (the next note of the scale; a long touch for the accent and the slide), a new line from the
 * generator or the style's back, one or two bars, and its sound (four quick choices, two knobs).
 */
@Composable
private fun AcidStep(style: StarterWizard.Style, ui: WizardUi, actions: WizardActions) {
    NeonCard(modifier = Modifier.testTag("wizard_acid")) {
        SectionLabel(tr("Ligne acide (TB-303)"), color = Neon.Acid)
        Text(tr("La ligne qui couine et qui gronde, jouée par une TB-303 : c'est la star du style. Elle est déjà écrite pour toi : change-la comme tu veux."),
            style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Utiliser la ligne acide"), modifier = Modifier.weight(1f))
            Switch(checked = ui.acid, onCheckedChange = actions.setAcidEnabled, modifier = Modifier.testTag("wizard_acid_enabled"))
        }
        if (!ui.acid) {
            Text(tr("Pas de ligne acide : la basse acide restera libre pour la tienne."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            return@NeonCard
        }
        if (ui.acidPlays) SceneBadge(ui.plan.acid, ui.plan)
        else Text(tr("La ligne n'a plus aucune note : elle ne jouera pas."), style = MaterialTheme.typography.bodySmall, color = Neon.Amber,
            modifier = Modifier.testTag("wizard_acid_silent"))
    }
    val line = ui.acidPattern ?: return
    if (!ui.acid) return
    AcidLineCard(line, ui, actions)
    AcidSoundCard(line, ui, actions)
}

/** The 303 line on its steps, one bar per two rows of eight, with « Avec le rythme », its length and the generator. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AcidLineCard(line: com.pocketstudio.model.AcidPattern, ui: WizardUi, actions: WizardActions) {
    val settings by com.pocketstudio.app.settings.SettingsStore.state.collectAsState()
    val english = settings.englishNotes
    val chipColors = pillColors()
    NeonCard(modifier = Modifier.testTag("wizard_acid_line")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(tr("Les notes"), Modifier.weight(1f), foldKey = "Les notes de la 303")
            ListenButton(SoundRef.AcidDemo, { actions.preview(SoundRef.AcidDemo) }, Modifier.testTag("wizard_acid_listen"), label = tr("Avec le rythme"))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(tr("Longueur"), style = MaterialTheme.typography.bodySmall)
            com.pocketstudio.model.WizardAcid.BARS.forEach { bars ->
                androidx.compose.material3.FilterChip(
                    selected = line.length == bars * com.pocketstudio.model.AcidPattern.STEPS_PER_PAGE,
                    onClick = { actions.setAcidBars(bars) },
                    label = { Text(if (bars == 1) tr("1 mesure") else tr("%d mesures", bars)) },
                    colors = chipColors,
                    modifier = Modifier.testTag("wizard_acid_bars_$bars"),
                )
            }
        }
        val bars = (line.length + 15) / 16
        for (bar in 0 until bars) {
            if (bars > 1) Text(tr("Mesure %d", bar + 1), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
            for (row in 0 until 2) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 8) {
                        val i = bar * 16 + row * 8 + col
                        com.pocketstudio.app.acid.AcidStepCell(
                            line, i, english,
                            modifier = Modifier.weight(1f)
                                .combinedClickable(onClick = { actions.acidTap(i) }, onLongClick = { actions.acidHold(i) })
                                .testTag("wizard_acid_step_$i"),
                        )
                    }
                }
            }
        }
        Text(
            tr("Touche un pas : la note suivante de la gamme (après la plus haute, un silence –). Appui long : accent (couleur pleine), puis slide ⌒, puis les deux."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        Button(onClick = actions.generateAcid, modifier = Modifier.fillMaxWidth().testTag("wizard_acid_generate")) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Text("  " + tr("Génère-moi une ligne"))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Densité"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(com.pocketstudio.model.i18n.percent((ui.acidDensity * 100).roundToInt()), style = com.pocketstudio.ui.MonoStyle, color = Neon.Cyan)
        }
        com.pocketstudio.ui.ProSlider(value = ui.acidDensity, onValueChange = actions.setAcidDensity, modifier = Modifier.testTag("wizard_acid_density"))
        OutlinedButton(onClick = actions.acidFromStyle, enabled = ui.acidNotes != null, modifier = Modifier.fillMaxWidth().testTag("wizard_acid_style")) {
            Text(tr("Celle du style"))
        }
    }
}

/** The 303's sound: four quick choices and the two knobs that make it cry. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AcidSoundCard(line: com.pocketstudio.model.AcidPattern, ui: WizardUi, actions: WizardActions) {
    NeonCard(modifier = Modifier.testTag("wizard_acid_sound")) {
        SectionLabel(tr("Le son de la 303"), foldKey = "Le son de la 303")
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            com.pocketstudio.model.WizardAcid.SOUNDS.forEach { (acidSoundLabel, _) ->
                FamilyChip(
                    tr(acidSoundLabel), Icons.Filled.GraphicEq, Neon.Acid, selected = ui.acidSoundName == acidSoundLabel,
                    onClick = { actions.chooseAcidSound(acidSoundLabel) }, modifier = Modifier.testTag("wizard_acid_sound_$acidSoundLabel"),
                )
            }
        }
        val knobColors = com.pocketstudio.ui.KnobColors(
            cap = Neon.SurfaceHigh, capEdge = Neon.Outline, pointer = Neon.TextPrimary,
            track = Neon.Outline, value = Neon.Acid, label = Neon.TextSecondary,
        )
        val defaults = com.pocketstudio.model.AcidSound().values()
        val values = line.sound.values()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(0 to tr("Coupure"), 1 to tr("Résonance")).forEach { (i, label) ->
                com.pocketstudio.ui.Knob(
                    value = values[i], onValueChange = { actions.setAcidKnob(i, it) }, label = label, default = defaults[i],
                    size = 64.dp, colors = knobColors, valueText = "${(values[i] * 100).roundToInt()}",
                    modifier = Modifier.testTag("wizard_acid_knob_$i"),
                )
            }
        }
        Text(tr("Tourne « Coupure » vers le haut pour la faire crier ; « Résonance » la fait siffler. Le reste se règle dans Sons › Basse acide."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

@Composable
private fun SummaryStep(ui: WizardUi, style: StarterWizard.Style) {
    // The three sections first: what the beginner will find in the song.
    ScenesCard(ui.plan, style)
    NeonCard(modifier = Modifier.testTag("wizard_summary")) {
        SectionLabel(tr("Ton morceau %s", tr(style.name)), foldKey = "Ton morceau")
        Text(if (style.swing > 0) tr("Tempo %d BPM, avec du swing.", style.bpm.roundToInt()) else tr("Tempo %d BPM.", style.bpm.roundToInt()), style = MaterialTheme.typography.bodyMedium)
        style.lines.forEach { line ->
            val row = ui.rows[line.role]
            val used = row?.enabled == true && row.sound != null
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (used) DrumKitInfo.colors[line.role.lane] else Neon.Outline))
                Text(tr(line.role.title), modifier = Modifier.weight(1f))
                Text(if (used) tr(row!!.sound!!.name.substringBefore(" · ")) else tr("pas utilisée"), color = if (used) Neon.TextPrimary else Neon.TextSecondary,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (used) PatternPreview(line.pattern, DrumKitInfo.colors[line.role.lane])
        }
        val mood = ui.mood
        if (mood == null) {
            Text(tr("Mélodie : aucune pour l'instant (la batterie seule)."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        } else {
            Text(tr("Ambiance : %s. Le clavier ne joue que des notes qui vont ensemble.", tr(mood.label)), style = MaterialTheme.typography.bodyMedium)
            if (style.melody.acid != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(StarterWizard.ScenePlan.ACID_TITLE), modifier = Modifier.weight(1f))
                    Text(acidSummary(ui), style = MaterialTheme.typography.bodySmall, color = if (ui.acidPlays) Neon.TextPrimary else Neon.TextSecondary,
                        modifier = Modifier.testTag("wizard_summary_acid"))
                }
            }
            StarterWizard.Part.entries.forEach { part ->
                val row = ui.parts[part]
                val used = row?.enabled == true
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr(part.title), modifier = Modifier.weight(1f))
                    Text(if (used) tr(row!!.sound.name) else tr("pas utilisée"), style = MaterialTheme.typography.bodySmall,
                        color = if (used) Neon.TextPrimary else Neon.TextSecondary)
                }
            }
        }
    }
    Text(
        tr("« Créer le morceau » ouvre un nouveau projet : chaque son est sur son pad et le rythme est dans la grille.") + " " +
            tr("Un petit guide te montre ensuite comment jouer tout le morceau et écouter chaque partie."),
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** The sections the song will have (Intro, Couplet, Refrain; or Intro, Groove, Drop, Break), what each holds, and the song that chains them. */
@Composable
private fun ScenesCard(plan: StarterWizard.ScenePlan, style: StarterWizard.Style) {
    NeonCard(accent = Neon.Magenta, modifier = Modifier.testTag("wizard_scenes")) {
        SectionLabel(if (plan.count == 4) tr("Quatre parties") else tr("Trois parties"), color = Neon.Magenta, help = Glossary.SCENE)
        Text(sectionsIntro(plan), style = MaterialTheme.typography.bodyMedium)
        (0 until plan.count).forEach { index ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SceneLetter(index)
                Column(Modifier.weight(1f)) {
                    Text("${plan.title(index)} · ${plan.role(index)}", style = MaterialTheme.typography.titleSmall)
                    val what = plan.contents(index).joinToString(", ") { it.substringBefore(" (").substringBefore(" / ") }
                    Text(what.ifEmpty { tr("rien pour l'instant") }, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
            }
        }
        Text(tr("Le morceau : %s", songSummary(style, plan)), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("wizard_song"))
    }
}

/** The 303 in the summary: "TB-303 · Saturée · 2 mesures", "ta ligne" when written on its step, or "pas utilisée". */
internal fun acidSummary(ui: WizardUi): String {
    val line = ui.acidPattern
    if (!ui.acidPlays || line == null) return tr("pas utilisée")
    val bars = (line.length + 15) / 16
    return listOfNotNull(
        "TB-303",
        ui.acidSoundName?.let { tr(it) },
        if (bars == 1) tr("1 mesure") else tr("%d mesures", bars),
        tr("ta ligne").takeIf { ui.acidNotes != null },
    ).joinToString(" · ")
}

/** How the sections build up: one after the other, then (club music) a Break before the last Drop; or a Montée before each Drop. */
internal fun sectionsIntro(plan: StarterWizard.ScenePlan): String = when {
    plan.form == StarterWizard.SongForm.Build && plan.hasBreak ->
        tr("Comme un vrai morceau de club : la Montée retire le kick et fait rouler la caisse claire, puis tout tombe d'un coup au Drop. Le Break fait une pause avant la dernière Montée.")
    plan.form == StarterWizard.SongForm.Build ->
        tr("Comme un vrai morceau de club : la Montée retire le kick et fait rouler la caisse claire, puis tout tombe d'un coup au Drop.")
    plan.form.sung && plan.hasBreak -> tr("Comme une vraie chanson : chaque partie ajoute des sons à celle d'avant, puis le Pont fait une pause sans kick ni basse avant le dernier Refrain.")
    plan.hasBreak && 3 in plan.parts[StarterWizard.Part.Bass].orEmpty() ->
        tr("Comme un vrai morceau de club : les sons s'empilent, puis le Break fait une pause sans kick avant le dernier Drop.")
    plan.hasBreak -> tr("Comme un vrai morceau de club : les sons s'empilent, puis le Break fait une pause sans kick ni basse avant le dernier Drop.")
    else -> tr("Comme une vraie chanson : chaque partie ajoute des sons à celle d'avant.")
}

/** "Joue dans Couplet et Refrain", after the letters of those sections. */
@Composable
private fun SceneBadge(scenes: Set<Int>, plan: StarterWizard.ScenePlan) {
    if (scenes.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("wizard_scene_badge")) {
        scenes.sorted().forEach { SceneLetter(it) }
        Text(
            if (scenes.size == plan.count) tr("Joue dans toutes les parties") else tr("Joue dans %s", plan.sections(scenes)),
            style = MaterialTheme.typography.bodyMedium, color = sceneColor(scenes.min()), modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun sceneColor(index: Int) = when (index) {
    0 -> Neon.Cyan
    1 -> androidx.compose.ui.graphics.Color(0xFF5BB8F5)  // sky blue, apart from the accent and the "playing" green
    2 -> Neon.Magenta
    else -> Neon.Amber  // the Break
}

/** The scene's letter in a small square of its colour, as on the Morceau tab. */
@Composable
private fun SceneLetter(index: Int) {
    Box(Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(sceneColor(index).copy(alpha = 0.22f)).border(1.dp, sceneColor(index), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center) {
        Text(StarterWizard.ScenePlan.NAMES[index], style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = sceneColor(index))
    }
}


/** The key's name, Do Ré Mi or C D E as the settings say. */
private fun keyName(root: Int) =
    com.pocketstudio.model.NoteNames.key(root, com.pocketstudio.app.settings.SettingsStore.state.value.englishNotes)

/** The feeling of the song: it picks the scale and the chords, so nothing can sound wrong. */
@Composable
private fun MoodStep(style: StarterWizard.Style, mood: StarterWizard.Mood?, packs: PacksUi, actions: WizardActions) {
    Text(
        tr("Quelle ambiance pour la mélodie ? Je choisis la gamme et les accords qui vont avec, et je verrouille le clavier sur cette gamme : impossible de jouer une fausse note, pas besoin de solfège."),
        style = MaterialTheme.typography.bodyMedium,
    )
    StylePacks(style, packs, actions)
    Text(tr("Touche une ambiance pour l'entendre : ses quatre accords, joués sur le rythme du style."),
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    var details by rememberSaveable { mutableStateOf(false) }
    StarterWizard.Mood.entries.forEach { m ->
        val selected = m == mood
        NeonCard(accent = if (selected) Neon.Cyan else Neon.Outline, modifier = Modifier.clickable { actions.setMood(m) }.testTag("wizard_mood_${m.id}")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListenButton(SoundRef.MoodDemo(m), { actions.preview(SoundRef.MoodDemo(m)) }, Modifier.testTag("wizard_mood_listen_${m.id}"))
                Column(Modifier.weight(1f)) {
                    Text(tr(m.label), style = MaterialTheme.typography.titleMedium, color = if (selected) Neon.Cyan else Neon.TextPrimary)
                    Text(tr(m.description), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    // Lot 2: the scale and the chords' numerals only for whoever asks (« Gamme Fa phrygienne · accords i–i–i–II » scared beginners).
                    if (selected && details) {
                        Text(tr("Gamme %s %s · accords %s", keyName(style.melody.root), tr(StarterWizard.progressionOf(style, m).first.label).lowercase(), StarterWizard.chordsLabel(style, m)),
                            style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("wizard_mood_theory"))
                    }
                }
                RadioButton(selected = selected, onClick = { actions.setMood(m) })
            }
        }
    }
    NeonCard(modifier = Modifier.clickable { actions.setMood(null) }.testTag("wizard_mood_none")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Pas de mélodie pour l'instant"), style = MaterialTheme.typography.titleMedium, color = if (mood == null) Neon.Cyan else Neon.TextPrimary)
                Text(tr("Juste la batterie : tu ajouteras les notes plus tard."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            RadioButton(selected = mood == null, onClick = { actions.setMood(null) })
        }
    }
    if (style.melody.mood == mood) Text(tr("C'est l'ambiance conseillée pour %s.", tr(style.name)), style = MaterialTheme.typography.bodySmall, color = Neon.Acid)
    if (mood != null) {
        TextButton(onClick = { details = !details }, modifier = Modifier.testTag("wizard_mood_details")) {
            Text(if (details) tr("Cacher la gamme et les accords") else tr("Pour les curieux : la gamme et les accords"))
        }
    }
}

/** One melodic part: what it does, use it or not, and its sound from the library (heard on its first bar). */
@Composable
private fun PartStep(style: StarterWizard.Style, ui: WizardUi, packs: PacksUi, part: StarterWizard.Part, playing: SoundRef?, actions: WizardActions) {
    val row = ui.parts[part]
    if (ui.mood == null) {
        NeonCard {
            SectionLabel(tr(part.title))
            Text(tr("Tu as choisi « pas de mélodie » : cette piste ne sera pas créée. Reviens à l'étape Ambiance pour l'ajouter."),
                style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    NeonCard(modifier = Modifier.testTag("wizard_part_${part.id}")) {
        SectionLabel(tr(part.title), color = Neon.Magenta)
        Text(tr(part.explanation), style = MaterialTheme.typography.bodyLarge)
        partNotes(style, part, ui.plan).forEach { note ->
            Text(note, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Neon.TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Créer cette piste"), modifier = Modifier.weight(1f))
            Switch(checked = row?.enabled ?: false, onCheckedChange = { actions.setPartEnabled(part, it) }, modifier = Modifier.testTag("wizard_part_enabled"))
        }
        ui.plan.parts[part]?.let { SceneBadge(it, ui.plan) }
        if (row != null && !row.enabled && part !in StarterWizard.defaultParts(style)) {
            Text(tr("Ne fait pas partie du style au départ : active-la si tu veux l'essayer."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("wizard_part_not_in_recipe"))
        }
    }
    if (row == null || !row.enabled) return
    val look = SoundLooks.of(part.categories.first())
    NeonCard {
        SectionLabel(tr("Choisis le son"), foldKey = "Choisis le son : ${part.id}")
        val sounds = StarterWizard.soundsFor(part, style, packs.installed)
        val listening = LocalListening.current
        TileGrid(sounds.size) { i, m ->
            val sound = sounds[i]
            val ref = SoundRef.Melodic(part, sound)
            SoundTile(
                title = tr(sound.name), icon = if (listening.playing == ref) Icons.Filled.GraphicEq else look.icon, accent = look.color,
                selected = sound == row.sound, onClick = { actions.choosePartSound(part, sound) },
                subtitle = when {
                    listening.preparing == ref -> tr("Préparation…")
                    listening.playing == ref -> tr("En écoute…")
                    i == 0 && sound.name == style.melody.sound(part) -> tr("Conseillé")
                    sound.sampled != null -> tr("Sample d'un pack")
                    sound.plaits != null -> SoundLooks.describe(sound.plaits!!)
                    else -> SoundLooks.describe(sound.synth)
                },
                modifier = m.testTag("wizard_part_sound_$i"),
            )
        }
        Text(
            tr("Touche un son pour l'entendre avec la batterie et les pistes déjà choisies.") + " " +
                tr("Les sons des packs téléchargés sont en bas de la liste."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
    }
    if (part == StarterWizard.Part.Bass) BassGridCard(style, ui, actions)
}

/**
 * « Motifs de basse » (docs/hits/basses-qui-tapent.md § 4): the rolling basslines the bass can play
 * instead of the style's own line, the style's own grids first (« Conseillé »), each heard at once.
 */
@Composable
private fun BassGridCard(style: StarterWizard.Style, ui: WizardUi, actions: WizardActions) {
    // Without the power chord where the bass glides (it would slide root -> octave).
    val grids = com.pocketstudio.model.BassPatterns.offered(style)
    val advised = com.pocketstudio.model.BassPatterns.advised(style.id)
    NeonCard(modifier = Modifier.testTag("wizard_bass_grids")) {
        SectionLabel(tr("Motifs de basse"), foldKey = "Motifs de basse")
        TileGrid(grids.size + 1) { i, m ->
            val grid = grids.getOrNull(i - 1)
            SoundTile(
                title = if (grid == null) tr("Celui du style") else tr(grid.name),
                icon = Icons.Filled.GraphicEq, accent = Neon.Magenta,
                selected = ui.bassGrid == grid?.id, onClick = { actions.chooseBassGrid(grid?.id) },
                // A few plain words (one line on a phone's two columns); the advised ones are named below.
                subtitle = if (grid == null) tr("La recette") else tr(grid.description),
                modifier = m.testTag("wizard_bass_grid_$i"),
            )
        }
        Text(
            tr("Des lignes de basse « roulantes » : des notes courtes entre les coups de kick, qui galopent. Touche un motif pour l'entendre."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        val first = grids.filter { it.id in advised }
        if (first.isNotEmpty()) {
            Text(
                tr("Conseillés pour ce style : %s.", first.joinToString(", ") { tr(it.name) }),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
    }
}

/**
 * What the part does in [style], under its general explanation: the recipe's own word, else how
 * the lead plays (an arpeggio, only when it is one; else a written riff); and whether it is the
 * style's signature, heard in every section.
 */
internal fun partNotes(style: StarterWizard.Style, part: StarterWizard.Part, plan: StarterWizard.ScenePlan? = null): List<String> {
    val own = style.melody.tips[part]?.takeIf { it.isNotBlank() }?.let { styleTip(style, it) }
    val general = when {
        own != null -> own
        part == StarterWizard.Part.Lead && style.melody.leadPattern.isBlank() ->
            tr("Ici, c'est l'arpégiateur qui joue les notes des accords une par une : tu pourras changer sa vitesse ou l'arrêter dans Synthé.")
        part == StarterWizard.Part.Lead -> tr("Un petit riff écrit pour ce style : tu pourras changer ses notes dans Synthé.")
        else -> null
    }
    // « Every section » only when the song's plan really says so (a signature bass waits for the Groove, or the Drop).
    val everywhere = plan?.parts?.get(part)?.let { it.size == plan.count }
        ?: (part != StarterWizard.Part.Bass && part !in style.drop.parts)
    val signature = when {
        style.melody.signature != part -> null
        everywhere -> tr("C'est le son qui fait le style : il joue dans toutes les parties.")
        else -> tr("C'est le son qui fait le style.")
    }
    return listOfNotNull(general, signature)
}
