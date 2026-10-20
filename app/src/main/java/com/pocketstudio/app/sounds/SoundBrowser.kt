package com.pocketstudio.app.sounds

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.NamedSound
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.ui.Neon

/** What a sound browser shows: built-in sounds by family, the user's favourites, and the current sound. */
class SoundBrowserUi(
    val categories: List<String>,
    val sounds: List<NamedSound<*>>,
    val favorites: List<NamedSound<*>>,
    /** Name of the built-in sound or favourite the instrument plays now, if any. */
    val current: String?,
    val pick: (Int) -> Unit = {},
    val pickFavorite: (Int) -> Unit = {},
    val deleteFavorite: (Int) -> Unit = {},
    val random: () -> Unit = {},
    val save: () -> Unit = {},
    /** Prefix of the test tags (several browsers live in the app). */
    val tag: String = "sounds",
)

/** Families of ready-made sounds, favourites, "Au hasard" and "Garder ce son". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SoundBrowser(ui: SoundBrowserUi) {
    val chipColors = FilterChipDefaults.filterChipColors(selectedContainerColor = Neon.Cyan.copy(alpha = 0.25f), selectedLabelColor = Neon.Cyan)
    val families = (if (ui.favorites.isNotEmpty()) listOf(SoundCategories.FAVORITES) else emptyList()) + ui.categories
    val start = ui.sounds.firstOrNull { it.name == ui.current }?.category
        ?: if (ui.favorites.any { it.name == ui.current }) SoundCategories.FAVORITES else ui.categories.first()
    var family by rememberSaveable(ui.tag) { mutableStateOf(start) }
    if (family !in families) family = ui.categories.first()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag(ui.tag)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            families.forEach { f ->
                FilterChip(
                    selected = family == f, onClick = { family = f },
                    label = { Text(if (f == SoundCategories.FAVORITES) "★ $f" else f) },
                    colors = chipColors,
                    modifier = Modifier.testTag("${ui.tag}_family_$f"),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (family == SoundCategories.FAVORITES) {
                ui.favorites.forEachIndexed { i, s ->
                    FilterChip(
                        selected = s.name == ui.current, onClick = { ui.pickFavorite(i) },
                        label = { Text(s.name) }, colors = chipColors,
                        trailingIcon = {
                            IconButton(onClick = { ui.deleteFavorite(i) }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Retirer ${s.name}", modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.testTag("${ui.tag}_fav_$i"),
                    )
                }
            } else {
                ui.sounds.forEachIndexed { i, s ->
                    if (s.category == family) {
                        FilterChip(
                            selected = s.name == ui.current, onClick = { ui.pick(i) },
                            label = { Text(s.name) }, colors = chipColors,
                            modifier = Modifier.testTag("${ui.tag}_$i"),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = ui.random, modifier = Modifier.weight(1f).testTag("${ui.tag}_random")) {
                Icon(Icons.Filled.Casino, contentDescription = null)
                Text("  Au hasard")
            }
            OutlinedButton(onClick = ui.save, modifier = Modifier.weight(1f).testTag("${ui.tag}_save")) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Text("  Garder ce son")
            }
        }
        if (ui.favorites.isEmpty()) {
            Text("« Garder ce son » le range dans tes favoris, pour tous tes projets.", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    }
}
