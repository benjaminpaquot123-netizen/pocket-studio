package com.pocketstudio.app.sounds

import com.pocketstudio.model.i18n.tr

import com.pocketstudio.ui.fadingHorizontalScroll
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
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
import com.pocketstudio.ui.FamilyChip
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.TileGrid

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
    /** A few words under a sound's name. */
    val describe: (NamedSound<*>) -> String? = { null },
)

/** Families of ready-made sounds as coloured tabs, their sounds as tiles, "Au hasard" and "Garder ce son". */
@Composable
fun SoundBrowser(ui: SoundBrowserUi) {
    val families = (if (ui.favorites.isNotEmpty()) listOf(SoundCategories.FAVORITES) else emptyList()) + ui.categories
    val start = ui.sounds.firstOrNull { it.name == ui.current }?.category
        ?: if (ui.favorites.any { it.name == ui.current }) SoundCategories.FAVORITES else ui.categories.first()
    var family by rememberSaveable(ui.tag) { mutableStateOf(start) }
    if (family !in families) family = ui.categories.first()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(ui.tag)) {
        Row(Modifier.fadingHorizontalScroll(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            families.forEach { f ->
                val look = SoundLooks.of(f)
                FamilyChip(
                    label = tr(f), icon = look.icon, accent = look.color, selected = family == f, onClick = { family = f },
                    modifier = Modifier.testTag("${ui.tag}_family_$f"),
                )
            }
        }
        AnimatedContent(family, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "family") { shown ->
            val look = SoundLooks.of(shown)
            if (shown == SoundCategories.FAVORITES) {
                TileGrid(ui.favorites.size) { i, m ->
                    val s = ui.favorites[i]
                    val own = SoundLooks.of(s.category)
                    SoundTile(
                        title = tr(s.name), icon = own.icon, accent = own.color, selected = s.name == ui.current,
                        onClick = { ui.pickFavorite(i) }, subtitle = ui.describe(s),
                        trailing = {
                            IconButton(onClick = { ui.deleteFavorite(i) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = tr("Retirer %s", tr(s.name)), tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        },
                        modifier = m.testTag("${ui.tag}_fav_$i"),
                    )
                }
            } else {
                val shownSounds = ui.sounds.withIndex().filter { it.value.category == shown }
                TileGrid(shownSounds.size) { k, m ->
                    val (i, s) = shownSounds[k]
                    SoundTile(
                        title = tr(s.name), icon = look.icon, accent = look.color, selected = s.name == ui.current,
                        onClick = { ui.pick(i) }, subtitle = ui.describe(s),
                        modifier = m.testTag("${ui.tag}_$i"),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val tight = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            OutlinedButton(onClick = ui.random, contentPadding = tight, modifier = Modifier.weight(1f).testTag("${ui.tag}_random")) {
                Icon(Icons.Filled.Casino, contentDescription = null)
                Text(tr("  Au hasard"), maxLines = 1)
            }
            OutlinedButton(onClick = ui.save, contentPadding = tight, modifier = Modifier.weight(1f).testTag("${ui.tag}_save")) {
                Icon(Icons.Filled.Star, contentDescription = null)
                Text(tr("  Garder ce son"), maxLines = 1)
            }
        }
        if (ui.favorites.isEmpty()) {
            Text(tr("« Garder ce son » le range dans tes favoris, pour tous tes projets."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    }
}
