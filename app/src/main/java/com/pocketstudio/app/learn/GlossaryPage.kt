package com.pocketstudio.app.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.learn.SoundGlossary
import com.pocketstudio.model.learn.SoundWord
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.SectionLabel

class GlossaryActions(
    val search: (String) -> Unit = {},
    /** ▶ / ■ on a word. */
    val toggle: (SoundWord) -> Unit = {},
    val close: () -> Unit = {},
)

/**
 * « Les mots de la musique, à écouter » (phase 39): a search field, then every word under its
 * heading, each with one or two sentences and ▶ to hear it. [playing]: the word heard (its
 * « Sans / Avec » light up in turn). Its own page over the app (Aide, Réglages, a « ? »).
 */
@Composable
fun GlossaryPage(query: String, playing: GlossaryPlay?, actions: GlossaryActions, modifier: Modifier = Modifier) {
    val found = SoundGlossary.search(query)
    val groups = SoundGlossary.GROUPS
    com.pocketstudio.ui.ReadingWidth {
        Column(modifier.fillMaxSize().background(Neon.Background).statusBarsPadding().testTag("glossary")) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = actions.close, modifier = Modifier.testTag("glossary_close")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
                }
                Column(Modifier.weight(1f)) {
                    Text(tr("Les mots de la musique"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(tr("Touche ▶ pour entendre chaque mot."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
            }
            OutlinedTextField(
                value = query, onValueChange = actions.search, singleLine = true,
                placeholder = { Text(tr("Chercher un mot (réverbe, drop…)")) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isEmpty()) null else {
                    { IconButton(onClick = { actions.search("") }) { Icon(Icons.Filled.Close, contentDescription = tr("Effacer")) } }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag("glossary_search"),
            )
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (found.isEmpty()) {
                    item { Text(tr("Aucun mot ne correspond. Essaie « kick » ou « écho »."), color = Neon.TextSecondary, modifier = Modifier.padding(12.dp).testTag("glossary_none")) }
                }
                if (query.isBlank()) {
                    groups.forEach { (title, keys) ->
                        item(key = "h_$title") { SectionLabel(title, modifier = Modifier.padding(top = 8.dp)) }
                        items(found.filter { it.key in keys }, key = { it.key }) { WordRow(it, playing, actions) }
                    }
                } else {
                    items(found, key = { it.key }) { WordRow(it, playing, actions) }
                }
            }
        }
    }
}

@Composable
private fun WordRow(word: SoundWord, playing: GlossaryPlay?, actions: GlossaryActions) {
    val mine = playing?.takeIf { it.key == word.key }
    NeonCard(modifier = Modifier.testTag("glossary_word_${word.key}")) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(word.term, style = MaterialTheme.typography.titleMedium, color = Neon.Cyan, fontWeight = FontWeight.SemiBold)
                Text(word.text, style = MaterialTheme.typography.bodyMedium)
                if (mine != null && !mine.preparing && mine.take >= 0) TakeChips(word, mine.take)
            }
            PlayButton(word, mine, actions)
        }
    }
}

/** « Sans · Avec » under a word whose example has two takes: the one heard is lit. */
@Composable
private fun TakeChips(word: SoundWord, take: Int) {
    val labels = word.example().labels
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("glossary_takes")) {
        labels.forEachIndexed { i, label ->
            val on = i == take
            Text(
                label, style = MaterialTheme.typography.labelMedium, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                color = if (on) Neon.OnAccent else Neon.TextSecondary,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) Neon.Acid else Neon.SurfaceHigh).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PlayButton(word: SoundWord, mine: GlossaryPlay?, actions: GlossaryActions) {
    val active = mine != null
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(if (active) Neon.Acid else Neon.SurfaceHigh)
            .clickable(role = Role.Button) { actions.toggle(word) }
            .semantics { contentDescription = if (active) tr("Arrêter « %s »", word.term) else tr("Écouter « %s »", word.term) }
            .testTag("glossary_play_${word.key}"),
        contentAlignment = Alignment.Center,
    ) {
        when {
            mine?.preparing == true -> CircularProgressIndicator(Modifier.size(24.dp), color = Neon.OnAccent, strokeWidth = 2.dp)
            active -> Icon(Icons.Filled.Stop, contentDescription = null, tint = Neon.OnAccent)
            else -> Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.Cyan)
        }
    }
}
