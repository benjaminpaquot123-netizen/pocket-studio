package com.pocketstudio.app.library

import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
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
    val choosePartSound: (StarterWizard.Part, StarterWizard.PartSound) -> Unit = { _, _ -> },
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
                Modifier.size(18.dp).semantics { contentDescription = "Préparation de l'écoute" }, strokeWidth = 2.dp, color = Neon.Cyan,
            )
            listening.playing -> Icon(Icons.Filled.Stop, contentDescription = "Arrêter l'écoute", tint = Neon.Cyan)
            else -> Icon(Icons.Filled.PlayArrow, contentDescription = "Écouter", tint = Neon.Cyan)
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
    Column(Modifier.fillMaxSize().background(Neon.Background).testTag("wizard")) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nouveau morceau, pas à pas", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (ui.step == 0) "Étape 1 : le style" else "Étape ${ui.step + 1} sur ${ui.lastStep + 1}${style?.let { " · ${it.name}" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                )
            }
            IconButton(onClick = actions.close, modifier = Modifier.testTag("wizard_close")) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
        }
        LinearProgressIndicator(
            progress = { if (style == null) 0.05f else (ui.step + 1f) / (ui.lastStep + 1f) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), color = Neon.Cyan,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (val current = ui.current) {
                StarterWizard.Step.Style -> StyleStep(ui.styleId, packs, actions)
                StarterWizard.Step.Mood -> MoodStep(style!!, ui.mood, packs, actions)
                is StarterWizard.Step.Drum -> LineStep(style!!, current.line, ui.rows[current.line.role], packs, playing, actions)
                is StarterWizard.Step.Melodic -> PartStep(style!!, ui, packs, current.part, playing, actions)
                StarterWizard.Step.Summary -> SummaryStep(ui, style!!)
            }
            ui.error?.let { Text(it, color = Neon.Amber, modifier = Modifier.testTag("wizard_error")) }
        }
        if (style != null && ui.step > 0) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = actions.back, modifier = Modifier.weight(1f).testTag("wizard_back")) { Text("Retour") }
                if (ui.step >= ui.lastStep) {
                    Button(onClick = actions.create, enabled = !ui.creating, modifier = Modifier.weight(1f).testTag("wizard_create")) {
                        Text(if (ui.creating) "Préparation…" else "Créer le morceau")
                    }
                } else {
                    Button(onClick = actions.next, modifier = Modifier.weight(1f).testTag("wizard_next")) { Text("Suivant") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleStep(chosen: String?, packs: PacksUi, actions: WizardActions) {
    Text(
        "Choisis un style. Ensuite, je te présente chaque ligne de la batterie (à quoi elle sert, quels sons " +
            "lui vont) et je place un rythme de départ dans la grille. Tu pourras tout changer après.",
        style = MaterialTheme.typography.bodyMedium,
    )
    // The best known first, then the others (still the better known first).
    val sections = listOf(
        "Les plus connus" to StarterWizard.STYLES.take(StarterWizard.BEST_KNOWN),
        "Les autres styles" to StarterWizard.STYLES.drop(StarterWizard.BEST_KNOWN),
    )
    sections.forEach { (title, styles) ->
        FoldSection("$title · ${styles.size}", foldKey = "Styles : $title") {
            styles.forEach { s ->
                val installed = packs.installed.any { it.id == s.id }
                NeonCard(modifier = Modifier.clickable { actions.chooseStyle(s.id) }.testTag("wizard_style_${s.id}")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium, color = if (s.id == chosen) Neon.Cyan else Neon.TextPrimary,
                            modifier = Modifier.weight(1f))
                        Text("${s.family} · ${s.bpm.roundToInt()} BPM", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
                    Text(s.description, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    if (s.references.isNotEmpty()) {
                        Text("Pour l'idée : " + s.references.take(3).joinToString(" · "), style = MaterialTheme.typography.labelMedium,
                            color = Neon.TextSecondary.copy(alpha = 0.85f), fontStyle = FontStyle.Italic)
                    }
                    if (installed) Text("Pack de sons téléchargé", style = MaterialTheme.typography.labelSmall, color = Neon.Acid)
                }
            }
        }
    }
    InstalledPacks(packs, actions)
}

/** The packs on the phone, each with what it takes and a way to delete it. */
@Composable
private fun InstalledPacks(packs: PacksUi, actions: WizardActions) {
    if (packs.installed.isEmpty()) return
    NeonCard(modifier = Modifier.testTag("wizard_installed_packs")) {
        SectionLabel("Packs téléchargés", help = Glossary.SOUND_PACK)
        packs.installed.forEach { m ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${m.name} · ${m.sounds.size} sons", modifier = Modifier.weight(1f))
                TextButton(onClick = { actions.deletePack(m.id) }, modifier = Modifier.testTag("wizard_delete_${m.id}")) { Text("Supprimer") }
            }
        }
        Text("Supprimer un pack libère la place ; tes morceaux gardent les sons qu'ils utilisent.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineStep(style: StarterWizard.Style, line: StarterWizard.Line, row: WizardRow?, packs: PacksUi, playing: SoundRef?, actions: WizardActions) {
    val role = line.role
    val color = DrumKitInfo.colors[role.lane]
    NeonCard(modifier = Modifier.testTag("wizard_line_${role.id}")) {
        SectionLabel(role.title, color = color)
        Text(role.explanation, style = MaterialTheme.typography.bodyLarge)
        Text("En ${style.name} : ${line.tip}", style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = Neon.TextSecondary)
        PatternPreview(line.pattern, color)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Utiliser cette ligne", modifier = Modifier.weight(1f))
            Switch(checked = row?.enabled ?: false, onCheckedChange = { actions.setEnabled(role, it) }, modifier = Modifier.testTag("wizard_enabled"))
        }
    }
    if (row == null || !row.enabled) {
        Text("Cette ligne restera vide : tu pourras l'ajouter plus tard en touchant les cases de la grille.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        return
    }
    val categories = soundCategories(role, style.id, packs.installed)
    val look = SoundLooks.of(role.id)
    NeonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Choisis le son", Modifier.weight(1f))
            row.sound?.let { sound ->
                val demo = SoundRef.LineDemo(role, sound)
                ListenButton(demo, { actions.preview(demo) }, Modifier.testTag("wizard_line_listen"), label = "Avec le rythme")
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
                title = option.name.substringBefore(" · "), icon = if (listening.playing == option.ref) Icons.Filled.GraphicEq else look.icon,
                accent = look.color, selected = option == row.sound, onClick = { actions.choose(role, option) },
                subtitle = if (listening.playing == option.ref) "En écoute…" else null,
                modifier = m.testTag("wizard_sound_$i"),
            )
        }
        Text("Touche un son pour l'écouter et le choisir ; « Avec le rythme » le fait entendre avec les lignes déjà choisies.",
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
        SectionLabel("Les vrais sons du style")
        Text(
            "Pour que ça sonne vraiment ${style.name} : " + missing.joinToString(", ") { it.name } +
                " (" + mb(missing.sumOf { it.sizeBytes }) + "). L'assistant prendra ses sons dedans.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (downloading.isNotEmpty()) {
            LinearProgressIndicator(progress = { downloading.average().toFloat() }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
        } else {
            Button(onClick = { missing.forEach { actions.download(it.id) } }, modifier = Modifier.fillMaxWidth().testTag("wizard_style_packs_download")) {
                Text("Télécharger les sons du style")
            }
        }
    }
}

private fun mb(bytes: Long) =
    if (bytes < 1_000_000) "${(bytes / 1000).coerceAtLeast(1)} ko" else String.format(java.util.Locale.FRANCE, "%.1f Mo", bytes / 1e6)

/** The style's own pack: to download (with its size), downloading, or there (and deletable). */
@Composable
private fun PackOffer(style: StarterWizard.Style, packs: PacksUi, actions: WizardActions) {
    val installed = packs.installed.firstOrNull { it.id == style.id }
    val entry: PackEntry? = (packs.index as? IndexState.Ready)?.index?.packs?.firstOrNull { it.id == style.id }
    val progress = packs.downloading[style.id]
    when {
        installed != null -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pack ${installed.name} : ${installed.sounds.size} sons sur le téléphone.", style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary, modifier = Modifier.weight(1f))
            TextButton(onClick = { actions.deletePack(style.id) }, modifier = Modifier.testTag("wizard_delete_pack")) { Text("Supprimer") }
        }
        progress != null -> NeonCard {
            Text("Téléchargement du pack ${style.name}… ${(progress * 100).roundToInt()} %")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Neon.Cyan)
            TextButton(onClick = { actions.cancelDownload(style.id) }) { Text("Annuler") }
        }
        entry != null -> NeonCard(modifier = Modifier.testTag("wizard_pack_offer")) {
            Text("Plus de sons ${style.name} : un pack de ${entry.sounds} sons (${entry.sizeLabel}), à télécharger seulement si tu veux.",
                style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { actions.download(style.id) }, modifier = Modifier.fillMaxWidth().testTag("wizard_download")) {
                Text("Télécharger le pack ${style.name} (${entry.sizeLabel})")
            }
        }
        packs.index is IndexState.Unavailable -> Text(
            "Les packs de sons par style arrivent bientôt. En attendant, les sons intégrés font très bien l'affaire.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        else -> Unit
    }
    packs.message?.let { Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodySmall) }
}

/** The line's groove, one square per step, filled as strong as the hit. */
@Composable
private fun PatternPreview(pattern: String, color: androidx.compose.ui.graphics.Color) {
    val hits = StarterWizard.hits(pattern)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        pattern.indices.forEach { step ->
            val v = hits[step]?.velocity
            Box(
                Modifier.weight(1f).aspectRatio(0.7f).clip(RoundedCornerShape(3.dp))
                    .background(if (v != null) color.copy(alpha = 0.3f + 0.7f * v) else if (step % 4 == 0) Neon.SurfaceHigh else Neon.Surface)
                    .border(1.dp, Neon.Outline, RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun SummaryStep(ui: WizardUi, style: StarterWizard.Style) {
    NeonCard(modifier = Modifier.testTag("wizard_summary")) {
        SectionLabel("Ton morceau ${style.name}", foldKey = "Ton morceau")
        Text("Tempo ${style.bpm.roundToInt()} BPM${if (style.swing > 0) ", avec du swing" else ""}.", style = MaterialTheme.typography.bodyMedium)
        style.lines.forEach { line ->
            val row = ui.rows[line.role]
            val used = row?.enabled == true && row.sound != null
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (used) DrumKitInfo.colors[line.role.lane] else Neon.Outline))
                Text(line.role.title, modifier = Modifier.weight(1f))
                Text(if (used) row!!.sound!!.name.substringBefore(" · ") else "pas utilisée", color = if (used) Neon.TextPrimary else Neon.TextSecondary,
                    style = MaterialTheme.typography.bodySmall)
            }
            if (used) PatternPreview(line.pattern, DrumKitInfo.colors[line.role.lane])
        }
        val mood = ui.mood
        if (mood == null) {
            Text("Mélodie : aucune pour l'instant (la batterie seule).", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        } else {
            Text("Ambiance : ${mood.label} · accords ${StarterWizard.chordsLabel(style, mood)}, gamme ${keyName(style.melody.root)} " +
                "${StarterWizard.progressionOf(style, mood).first.label.lowercase()} verrouillée.",
                style = MaterialTheme.typography.bodyMedium)
            StarterWizard.Part.entries.forEach { part ->
                val row = ui.parts[part]
                val used = row?.enabled == true
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(part.title, modifier = Modifier.weight(1f))
                    Text(if (used) row!!.sound.name else "pas utilisée", style = MaterialTheme.typography.bodySmall,
                        color = if (used) Neon.TextPrimary else Neon.TextSecondary)
                }
            }
        }
    }
    Text(
        "« Créer le morceau » ouvre un nouveau projet : chaque son est sur son pad et le rythme est dans la grille. " +
            "Appuie sur Lecture, puis change ce que tu veux : touche les cases, essaie d'autres sons, ajoute des pistes.",
        style = MaterialTheme.typography.bodyMedium,
    )
}

private val KEY_NAMES = listOf("Do", "Do♯", "Ré", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "La♭", "La", "Si♭", "Si")

private fun keyName(root: Int) = KEY_NAMES[Math.floorMod(root, 12)]

/** The feeling of the song: it picks the scale and the chords, so nothing can sound wrong. */
@Composable
private fun MoodStep(style: StarterWizard.Style, mood: StarterWizard.Mood?, packs: PacksUi, actions: WizardActions) {
    Text(
        "Quelle ambiance pour la mélodie ? Je choisis la gamme et les accords qui vont avec, et je verrouille le " +
            "clavier sur cette gamme : impossible de jouer une fausse note, pas besoin de solfège.",
        style = MaterialTheme.typography.bodyMedium,
    )
    StylePacks(style, packs, actions)
    Text("Touche une ambiance pour l'entendre : ses quatre accords, joués sur le rythme du style.",
        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    StarterWizard.Mood.entries.forEach { m ->
        val selected = m == mood
        NeonCard(accent = if (selected) Neon.Cyan else Neon.Outline, modifier = Modifier.clickable { actions.setMood(m) }.testTag("wizard_mood_${m.id}")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListenButton(SoundRef.MoodDemo(m), { actions.preview(SoundRef.MoodDemo(m)) }, Modifier.testTag("wizard_mood_listen_${m.id}"))
                Column(Modifier.weight(1f)) {
                    Text(m.label, style = MaterialTheme.typography.titleMedium, color = if (selected) Neon.Cyan else Neon.TextPrimary)
                    Text(m.description, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    Text("Gamme ${keyName(style.melody.root)} ${StarterWizard.progressionOf(style, m).first.label.lowercase()} · accords ${StarterWizard.chordsLabel(style, m)}",
                        style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
                }
                RadioButton(selected = selected, onClick = { actions.setMood(m) })
            }
        }
    }
    NeonCard(modifier = Modifier.clickable { actions.setMood(null) }.testTag("wizard_mood_none")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pas de mélodie pour l'instant", style = MaterialTheme.typography.titleMedium, color = if (mood == null) Neon.Cyan else Neon.TextPrimary)
                Text("Juste la batterie : tu ajouteras les notes plus tard.", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            RadioButton(selected = mood == null, onClick = { actions.setMood(null) })
        }
    }
    if (style.melody.mood == mood) Text("C'est l'ambiance la plus courante en ${style.name}.", style = MaterialTheme.typography.bodySmall, color = Neon.Acid)
}

/** One melodic part: what it does, use it or not, and its sound from the library (heard on its first bar). */
@Composable
private fun PartStep(style: StarterWizard.Style, ui: WizardUi, packs: PacksUi, part: StarterWizard.Part, playing: SoundRef?, actions: WizardActions) {
    val row = ui.parts[part]
    if (ui.mood == null) {
        NeonCard {
            SectionLabel(part.title)
            Text("Tu as choisi « pas de mélodie » : cette piste ne sera pas créée. Reviens à l'étape Ambiance pour l'ajouter.",
                style = MaterialTheme.typography.bodyMedium)
        }
        return
    }
    NeonCard(modifier = Modifier.testTag("wizard_part_${part.id}")) {
        SectionLabel(part.title, color = Neon.Magenta)
        Text(part.explanation, style = MaterialTheme.typography.bodyLarge)
        if (part == StarterWizard.Part.Lead) {
            Text("C'est l'arpégiateur qui fait bouger les notes : tu pourras changer sa vitesse ou l'arrêter dans Synthé.",
                style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = Neon.TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Créer cette piste", modifier = Modifier.weight(1f))
            Switch(checked = row?.enabled ?: false, onCheckedChange = { actions.setPartEnabled(part, it) }, modifier = Modifier.testTag("wizard_part_enabled"))
        }
    }
    if (row == null || !row.enabled) return
    val look = SoundLooks.of(part.categories.first())
    NeonCard {
        SectionLabel("Choisis le son")
        val sounds = StarterWizard.soundsFor(part, style, packs.installed)
        val listening = LocalListening.current
        TileGrid(sounds.size) { i, m ->
            val sound = sounds[i]
            val ref = SoundRef.Melodic(part, sound)
            SoundTile(
                title = sound.name, icon = if (listening.playing == ref) Icons.Filled.GraphicEq else look.icon, accent = look.color,
                selected = sound == row.sound, onClick = { actions.choosePartSound(part, sound) },
                subtitle = when {
                    listening.preparing == ref -> "Préparation…"
                    listening.playing == ref -> "En écoute…"
                    i == 0 && sound.name == style.melody.sound(part) -> "Conseillé"
                    sound.sampled != null -> "Sample d'un pack"
                    sound.plaits != null -> SoundLooks.describe(sound.plaits!!)
                    else -> SoundLooks.describe(sound.synth)
                },
                modifier = m.testTag("wizard_part_sound_$i"),
            )
        }
        Text(
            "Touche un son pour l'entendre avec la batterie et les pistes déjà choisies." +
                " Les sons des packs téléchargés sont en bas de la liste.",
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
    }
}
