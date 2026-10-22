package com.pocketstudio.app.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.library.QuickUi
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.FamilyChip
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SectionLabel
import kotlin.math.roundToInt

/**
 * « Nouveau projet »: the guided song, then the assistant's styles (the same list, families and
 * order) each made whole in one touch or opened step by step, then the other starters (the
 * starting beat, the demos, the empty project) under their headings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewProjectPage(starters: List<ProjectStarter>, quick: QuickUi, actions: ProjectsActions, close: () -> Unit) {
    // While a song is being prepared, every other choice waits.
    val busy = quick.creating != null
    // One family at a time, or the best known (then all of them on demand).
    var family by rememberSaveable { mutableStateOf<String?>(null) }
    var allStyles by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(12.dp).testTag("new_project"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = close, enabled = !busy, modifier = Modifier.testTag("new_project_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
            }
            Text(tr("Nouveau projet"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("new_project_list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel(tr("Guidé"))
            Card(
                colors = CardDefaults.cardColors(containerColor = Neon.Cyan.copy(alpha = 0.18f)),
                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { close(); actions.wizard() }.testTag("starter_wizard"),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(tr("Nouveau morceau guidé"), style = MaterialTheme.typography.titleMedium, color = Neon.Cyan)
                    Text(tr("Idéal pour débuter : choisis un style, je t'explique chaque son et je prépare l'intro, le couplet et le refrain."),
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
            }
            SectionLabel(StarterGroup.Style.title, Modifier.padding(top = 8.dp))
            Text(
                tr("Touche un style : je prépare tout le morceau d'un coup (intro, montée, drop… environ 3 minutes), prêt à écouter."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
            )
            quick.error?.let {
                Text(it, color = Neon.Amber, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = actions.dismissQuickError).testTag("quick_error"))
            }
            // The assistant's families, laid out in a row a phone scrolls sideways.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("quick_families"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf<String?>(null) + StarterWizard.FAMILIES).forEach { f ->
                    FamilyChip(
                        if (f == null) tr("Tous") else tr(f), if (f == null) Icons.Filled.Apps else Icons.Filled.GraphicEq, Neon.Cyan,
                        selected = f == family, onClick = { family = f }, modifier = Modifier.testTag("quick_family_${f ?: "all"}"),
                    )
                }
            }
            val styles = when {
                family != null -> StarterWizard.STYLES.filter { it.family == family }
                allStyles -> StarterWizard.STYLES
                else -> StarterWizard.STYLES.take(StarterWizard.BEST_KNOWN)
            }
            styles.forEach { QuickStyleCard(it, quick, actions) }
            if (family == null) {
                val more = StarterWizard.STYLES.size - StarterWizard.BEST_KNOWN
                TextButton(onClick = { allStyles = !allStyles }, modifier = Modifier.fillMaxWidth().testTag("quick_more")) {
                    Text(if (allStyles) "▾ " + tr("Moins de styles") else "▸ " + tr("Voir les %d autres styles", more))
                }
            }
            // The other starters under their headings (the starting beat closes the styles' group).
            StarterGroup.entries.forEach { group ->
                val inGroup = starters.withIndex().filter { it.value.group == group }
                if (inGroup.isEmpty()) return@forEach
                if (group != StarterGroup.Style) SectionLabel(group.title, Modifier.padding(top = 8.dp))
                inGroup.forEach { (i, s) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { close(); actions.create(i) }.testTag("starter_${s.tag}"),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(tr(s.title), style = MaterialTheme.typography.titleMedium)
                            Text(tr(s.description), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One style: a touch makes its whole song with the advised choices; « Personnaliser pas à pas »
 * opens the assistant on it. While its song is prepared, a wheel says so in place of its description.
 */
@Composable
private fun QuickStyleCard(s: StarterWizard.Style, quick: QuickUi, actions: ProjectsActions) {
    val preparing = quick.creating == s.id
    val busy = quick.creating != null
    Card(
        colors = CardDefaults.cardColors(containerColor = Neon.SurfaceHigh),
        border = if (preparing) BorderStroke(1.5.dp, Neon.Cyan) else null,
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) { actions.quickStyle(s.id) }.testTag("quick_style_${s.id}"),
    ) {
        Column(Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 2.dp)) {
            Row(Modifier.padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr(s.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (preparing) Neon.Cyan else Neon.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                // Its family when it says something ("Techno" under Techno, "Autres" do not).
                val family = s.family.takeIf { it != s.name && it != StarterWizard.FAMILIES.last() }
                Text(listOfNotNull(family?.let { tr(it) }, "${s.bpm.roundToInt()} BPM").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            }
            if (preparing) {
                Row(Modifier.padding(vertical = 4.dp).testTag("quick_preparing"), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Neon.Cyan)
                    Text("  " + tr("Préparation du morceau…"), style = MaterialTheme.typography.bodyMedium, color = Neon.Cyan)
                }
            } else {
                Text(tr(s.description), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 8.dp))
            }
            // The touch on the card makes the song; the assistant is the second, quieter way.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val ink = if (busy) Neon.TextSecondary.copy(alpha = 0.5f) else Neon.Cyan
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
                Text(" " + tr("Créer le morceau"), style = MaterialTheme.typography.labelLarge, color = ink, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = { actions.customStyle(s.id) }, enabled = !busy, modifier = Modifier.testTag("quick_custom_${s.id}")) {
                    val quiet = if (busy) Neon.TextSecondary.copy(alpha = 0.5f) else Neon.TextSecondary
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = quiet)
                    Text(" " + tr("Personnaliser pas à pas"), color = quiet)
                }
            }
        }
    }
}
