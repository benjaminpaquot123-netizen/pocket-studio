@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pocketstudio.app.settings

import com.pocketstudio.model.i18n.tr
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketstudio.app.help.Glossary
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.pocketstudio.ui.ProSlider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.pillColors
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

class SettingsActions(
    val update: ((AppSettings) -> AppSettings) -> Unit = {},
    val reset: () -> Unit = {},
    val openGuide: () -> Unit = {},
)

/** Réglages: look, notes, feel, sound; the same for every project. */
@Composable
fun SettingsScreen(s: AppSettings, version: String, actions: SettingsActions) {
    val chip = pillColors()
    // On a tablet: look and feel on the left, sound and help on the right.
    com.pocketstudio.ui.AdaptivePanes(Modifier.testTag("settings"), spacing = 12.dp, first = {
        NeonCard {
            SectionLabel(tr("Affichage"))
            // Phase 36: the Mode simple or every tool; the project stays the same either way.
            Text(tr("Façon de travailler"), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = s.simpleMode, onClick = { actions.update { it.copy(simpleMode = true) } }, label = { Text(tr("Simple : 4 onglets")) },
                    colors = chip, modifier = Modifier.testTag("mode_simple"))
                FilterChip(selected = !s.simpleMode, onClick = { actions.update { it.copy(simpleMode = false) } }, label = { Text(tr("Complet : tous les outils")) },
                    colors = chip, modifier = Modifier.testTag("mode_full"))
            }
            Text(tr("Changer de façon de travailler ne change rien à tes morceaux."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            Text(tr("Langue"), style = MaterialTheme.typography.bodyMedium)
            LanguagePicker()
            Text(tr("Thème"), style = MaterialTheme.typography.bodyMedium)
            AppTheme.entries.forEach { t ->
                FilterChip(selected = s.theme == t, onClick = { actions.update { it.copy(theme = t) } }, label = { Text(t.label) },
                    colors = chip, modifier = Modifier.testTag("theme_${t.name}"))
            }
            Text(tr("Taille de l'interface"), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppSettings.UI_SCALES.forEach { scale ->
                    FilterChip(selected = s.uiScale == scale, onClick = { actions.update { it.copy(uiScale = scale) } },
                        label = { Text(com.pocketstudio.model.i18n.percent((scale * 100).roundToInt())) }, colors = chip, modifier = Modifier.testTag("scale_${(scale * 100).roundToInt()}"))
                }
            }
            Text(tr("Noms des notes"), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = !s.englishNotes, onClick = { actions.update { it.copy(englishNotes = false) } }, label = { Text("Do Ré Mi") }, colors = chip)
                FilterChip(selected = s.englishNotes, onClick = { actions.update { it.copy(englishNotes = true) } }, label = { Text("C D E") }, colors = chip)
            }
        }
        NeonCard {
            SectionLabel(tr("Toucher"))
            Toggle(tr("Vibrations"), tr("Une petite vibration sur les pads et les appuis longs."), s.haptics, "haptics") { v -> actions.update { it.copy(haptics = v) } }
            Toggle(tr("Battre la mesure en Live"), tr("Une petite vibration à chaque temps pendant que la page Live joue."), s.liveBeatHaptics, "live_beat_haptics") { v ->
                actions.update { it.copy(liveBeatHaptics = v) }
            }
            Toggle(tr("Écran toujours allumé pendant la lecture"), tr("L'écran ne s'éteint pas tant que la musique joue."), s.keepScreenOn, "keep_screen") { v ->
                actions.update { it.copy(keepScreenOn = v) }
            }
        }
    }, second = {
        NeonCard {
            SectionLabel(tr("Son"))
            Text(tr("Volume du métronome : %d %%", (s.metronomeLevel * 100).roundToInt()), style = MaterialTheme.typography.bodyMedium)
            ProSlider(value = s.metronomeLevel, onValueChange = { v -> actions.update { it.copy(metronomeLevel = v) } }, modifier = Modifier.testTag("metronome_level"))
            Text(tr("Décompte avant d'enregistrer"), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to tr("Aucun"), 1 to tr("1 mesure"), 2 to tr("2 mesures")).forEach { (bars, label) ->
                    FilterChip(selected = s.recordCountIn == bars, onClick = { actions.update { it.copy(recordCountIn = bars) } }, label = { Text(label) }, colors = chip)
                }
            }
            if (s.recordCountIn == 0) {
                Text(tr("Pour le micro, le décompte dure quand même 1 mesure : ta prise commence pile sur le temps."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            FilterChip(
                selected = s.metronomeInTakes, onClick = { actions.update { it.copy(metronomeInTakes = !it.metronomeInTakes) } },
                label = { Text(tr("Métronome pendant les prises")) }, colors = chip, modifier = Modifier.testTag("metronome_in_takes"),
            )
            Text(tr("Mode du son"), style = MaterialTheme.typography.bodyMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppSettings.BUFFERS.forEach { (bursts, label) ->
                    FilterChip(selected = s.bufferBursts == bursts, onClick = { actions.update { it.copy(bufferBursts = bursts) } }, label = { Text(label) },
                        colors = chip, modifier = Modifier.testTag("buffer_$bursts"))
                }
            }
            Text(
                tr("« Rapide » répond au doigt sans délai. Si le son craque, passe à « Sûr » : un tout petit peu plus de retard, mais fini les craquements."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
        }
        NeonCard {
            SectionLabel(tr("Aide"))
            OutlinedButton(onClick = actions.openGuide, modifier = Modifier.fillMaxWidth().testTag("open_guide")) { Text(tr("Revoir le guide des premiers pas")) }
            GlossaryButton("settings_glossary")
            var lexicon by rememberSaveable { mutableStateOf(false) }
            OutlinedButton(onClick = { lexicon = !lexicon }, modifier = Modifier.fillMaxWidth().testTag("open_lexicon")) {
                Text(if (lexicon) tr("Fermer le lexique") else tr("Lexique : les mots de la musique expliqués"))
            }
            if (lexicon) {
                Text(tr("Partout dans l'appli, le petit « ? » à côté d'un mot l'explique aussi."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                Glossary.ALL.forEach { h ->
                    Column(Modifier.testTag("lexicon_${h.term}")) {
                        Text(h.term, style = MaterialTheme.typography.titleSmall, color = Neon.Cyan)
                        Text(h.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            TextButton(onClick = actions.reset, modifier = Modifier.fillMaxWidth().testTag("settings_reset")) { Text(tr("Remettre les réglages par défaut")) }
            Text("BenStudio $version", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    })
}

/** « Les mots de la musique, à écouter »: the glossary with a sound for each word (phase 39). */
@Composable
private fun GlossaryButton(tag: String) {
    val learn = com.pocketstudio.app.learn.LocalLearn.current
    androidx.compose.material3.Button(onClick = { learn.open("") }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag)) {
        androidx.compose.material3.Icon(Icons.Filled.LibraryMusic, contentDescription = null)
        Text("  " + tr("Les mots de la musique, à écouter"))
    }
}

@Composable
private fun Toggle(title: String, hint: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}

class HelpActions(
    val openGuide: () -> Unit = {},
    /** G5: opens the Guide's panel on the song (it took over the « Prochaine étape » card). */
    val showSteps: () -> Unit = {},
    /** « Mon premier morceau » (the old Missions, now the Guide's first cards; null: not offered). */
    val missions: (() -> Unit)? = null,
)

/** Outils › Aide, the first page of Outils: the guide, the song's steps, and every word explained. */
@Composable
fun HelpPage(actions: HelpActions) {
    com.pocketstudio.ui.ReadingWidth {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp).testTag("help_page"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeonCard {
                SectionLabel(tr("Aide"))
                actions.missions?.let { start ->
                    androidx.compose.material3.Button(onClick = start, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("help_missions")) {
                        Text(tr("Mon premier morceau, avec le Guide"))
                    }
                }
                OutlinedButton(onClick = actions.openGuide, modifier = Modifier.fillMaxWidth().testTag("help_guide")) { Text(tr("Revoir le guide des premiers pas")) }
                OutlinedButton(onClick = actions.showSteps, modifier = Modifier.fillMaxWidth().testTag("help_steps")) { Text(tr("Ouvrir le Guide de mon morceau")) }
                GlossaryButton("help_glossary")
                Text(tr("Le « ? » au bout du bandeau sous la barre du haut étiquette l'écran : un toucher pour voir, un autre pour cacher."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                Text(tr("Partout dans l'appli, le petit « ? » à côté d'un mot l'explique aussi."),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            NeonCard {
                SectionLabel(tr("Lexique"))
                Glossary.ALL.forEach { h ->
                    Column {
                        Text(h.term, style = MaterialTheme.typography.titleSmall, color = Neon.Cyan)
                        Text(h.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
