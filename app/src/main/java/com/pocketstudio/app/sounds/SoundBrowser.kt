package com.pocketstudio.app.sounds

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.SoundShelves
import com.pocketstudio.app.library.MINE
import com.pocketstudio.app.library.ShelfBrowser
import com.pocketstudio.app.library.ShelfBrowserActions
import com.pocketstudio.app.library.ShelfBrowserUi
import com.pocketstudio.app.library.SoundOption
import com.pocketstudio.app.library.SoundRef
import com.pocketstudio.app.library.SoundShelf
import com.pocketstudio.app.library.shelfSearch
import androidx.compose.runtime.remember

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
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
    /** « Au hasard » and « Garder ce son » under the tiles (false: a sound only picked, a layer's). */
    val extras: Boolean = true,
    /** The family to open on when the sound playing is none of these (n° 35: a lead track opened on « Basses »). */
    val family: String? = null,
    /**
     * Audit ultra round 1, n° 35 (seat D c): « Garder ce son » asks for a name ([nextName] offered) and keeps it under
     * it; null: kept at once under « Mon son N » ([save]).
     */
    val saveNamed: ((String) -> Unit)? = null,
    val nextName: String = "",
    /** The kept sound that is this sound now (null: none): « Garder ce son » says it is there already. */
    val keptAs: String? = null,
    /** « Renommer » a kept sound (its index in [favorites], the new name); null: not offered. */
    val rename: ((Int, String) -> Unit)? = null,
)

/**
 * The sound family a track's name asks for (« Lead », « Mélodie » → Leads; « Basse » → Basses; « Accords »
 * → Nappes…), null when its name says none (audit ultra round 1, n° 35).
 */
fun familyOfTrack(name: String): String? {
    val n = name.lowercase()
    return when {
        "808" in n || "bass" in n -> "Basses"
        "accord" in n || "nappe" in n || "chord" in n || n.startsWith("pad") -> "Nappes"
        "arp" in n || "pluck" in n -> "Pluck"
        "lead" in n || "mélod" in n || "melod" in n || "riff" in n || "contre" in n -> "Leads"
        "piano" in n || "clavier" in n || "orgue" in n || "keys" in n -> "Claviers"
        else -> null
    }
}

/**
 * Ready-made sounds (settings, not files) in the app's one sound browser ([ShelfBrowser], 7d):
 * search, the kept ones as « Favoris », the families as folders, opened on the sound playing now;
 * a touch puts the sound on (and plays it). « Au hasard » and « Garder ce son » under it.
 */
@Composable
fun SoundBrowser(ui: SoundBrowserUi) {
    val shelves = remember(ui.categories, ui.sounds) { presetShelves(ui) }
    val mine = remember(ui.favorites) { ui.favorites.mapIndexed { i, s -> presetOption(SoundRef.Preset(i, saved = true), s, ui) } }
    val current = ui.sounds.indexOfFirst { it.name == ui.current }.takeIf { it >= 0 }?.let { SoundRef.Preset(it) }
        ?: ui.favorites.indexOfFirst { it.name == ui.current }.takeIf { it >= 0 }?.let { SoundRef.Preset(it, saved = true) }
    // Opened on the family of the sound playing now, whenever it changes (another track shown, a
    // sound chosen elsewhere): never the folder left open for the previous track.
    val start = when (current) {
        is SoundRef.Preset -> if (current.saved) SAVED else ui.sounds[current.index].category
        // A sound of its own (not one of these): the track's family (n° 35), else the first one, its sounds one touch away.
        else -> ui.family?.takeIf { it in ui.categories } ?: ui.categories.firstOrNull()
    }
    var naming by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = NameAsk.Saver) { mutableStateOf<NameAsk?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(ui.tag)) {
        ShelfBrowser(
            ShelfBrowserUi(
                shelves = shelves, kind = ui.categories.firstOrNull().orEmpty(), chosen = current, tag = ui.tag, pickOnTap = true,
                mine = mine.takeIf { it.isNotEmpty() }, mineTitle = tr("Favoris"),
                search = { q -> shelfSearch(shelves + listOf(SoundShelves.Shelf(SAVED, SAVED, emptyList(), mine)), q) },
                start = start, stateKey = "${ui.tag}:$start",
                kindOf = { o -> (o.ref as? SoundRef.Preset)?.let { r -> (if (r.saved) ui.favorites else ui.sounds).getOrNull(r.index)?.category } },
                tileTag = { o -> (o.ref as? SoundRef.Preset)?.let { r -> if (r.saved) "${ui.tag}_fav_${r.index}" else "${ui.tag}_${r.index}" }.orEmpty() },
            ),
            ShelfBrowserActions(
                choose = { o -> (o.ref as? SoundRef.Preset)?.let { r -> if (r.saved) ui.pickFavorite(r.index) else ui.pick(r.index) } },
                remove = { o -> (o.ref as? SoundRef.Preset)?.takeIf { it.saved }?.let { ui.deleteFavorite(it.index) } },
            ),
        )
        // A narrow pane (a tablet's side, a big interface size): the words go on two lines and the
        // buttons grow together, never « Garder ce » cut.
        if (ui.extras) Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val tight = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            OutlinedButton(onClick = ui.random, contentPadding = tight, modifier = Modifier.weight(1f).fillMaxHeight().testTag("${ui.tag}_random")) {
                Icon(Icons.Filled.Casino, contentDescription = null)
                com.pocketstudio.ui.WholeWordsText(tr("Au hasard"), Modifier.padding(start = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            OutlinedButton(
                // Kept already: said (the save says so), never a second « Mon son 2 » the same.
                onClick = { if (ui.saveNamed != null && ui.keptAs == null) naming = NameAsk(-1, ui.nextName) else ui.save() },
                contentPadding = tight, modifier = Modifier.weight(1f).fillMaxHeight().testTag("${ui.tag}_save"),
            ) {
                Icon(Icons.Filled.Star, contentDescription = null)
                com.pocketstudio.ui.WholeWordsText(
                    if (ui.keptAs != null) tr("Déjà dans tes favoris") else tr("Garder ce son"), Modifier.padding(start = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        // The kept sound playing now: « Renommer » it, its name in the button.
        val keptIndex = ui.favorites.indexOfFirst { it.name == ui.current }
        if (ui.extras && ui.rename != null && keptIndex >= 0) {
            androidx.compose.material3.TextButton(
                onClick = { naming = NameAsk(keptIndex, ui.favorites[keptIndex].name) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("${ui.tag}_rename"),
            ) { Text(tr("Renommer « %s »", ui.favorites[keptIndex].name), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        naming?.let { ask ->
            KeptNameDialog(
                title = if (ask.index < 0) tr("Garder ce son") else tr("Renommer le son"),
                start = ask.name, tag = ui.tag,
                done = { name ->
                    naming = null
                    if (ask.index < 0) ui.saveNamed?.invoke(name) else ui.rename?.invoke(ask.index, name)
                },
                cancel = { naming = null },
            )
        }
        if (ui.extras && ui.favorites.isEmpty()) {
            Text(tr("« Garder ce son » le range dans tes favoris, pour tous tes projets."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        }
    }
}

/** The name being asked for a kept sound: [index] -1 for a new one, else the one renamed. */
private data class NameAsk(val index: Int, val name: String) {
    companion object {
        val Saver = androidx.compose.runtime.saveable.Saver<NameAsk?, List<Any>>(
            save = { a -> a?.let { listOf(it.index, it.name) } ?: emptyList() },
            restore = { l -> if (l.size == 2) NameAsk(l[0] as Int, l[1] as String) else null },
        )
    }
}

/** « Garder ce son » / « Renommer le son »: its name, typed (n° 35), « Garder » or « Annuler ». */
@Composable
private fun KeptNameDialog(title: String, start: String, tag: String, done: (String) -> Unit, cancel: () -> Unit) {
    var text by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(start) }
    // At the interface's size, as every window (ScaledWindowsTest).
    com.pocketstudio.ui.ScaledAlertDialog(
        onDismissRequest = cancel,
        title = { Text(title) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text, onValueChange = { text = it.take(com.pocketstudio.model.Favorites.NAME_MAX) }, singleLine = true,
                label = { Text(tr("Nom du son")) }, modifier = Modifier.fillMaxWidth().testTag("${tag}_name_field"),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { done(text) }, enabled = com.pocketstudio.model.Favorites.cleanName(text) != null,
                modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_name_ok"),
            ) { Text(tr("Garder")) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = cancel, modifier = Modifier.heightIn(min = 48.dp).testTag("${tag}_name_cancel")) { Text(tr("Annuler")) }
        },
        modifier = Modifier.testTag("${tag}_name_dialog"),
    )
}

/** The kept sounds' folder (« Favoris » here). */
private const val SAVED = MINE

/** A ready-made sound as a browser option: its name, and its few words in small. */
private fun presetOption(ref: SoundRef, s: NamedSound<*>, ui: SoundBrowserUi): SoundOption =
    SoundOption(ref, ui.describe(s)?.takeIf { it.isNotBlank() }?.let { "${s.name} · $it" } ?: s.name, SoundLooks.waveOf(s.sound))

/** One folder per family, in the browser's order, its sounds in the library's order. */
internal fun presetShelves(ui: SoundBrowserUi): List<SoundShelf> = ui.categories.mapNotNull { c ->
    val items = ui.sounds.withIndex().filter { it.value.category == c }.map { (i, s) -> presetOption(SoundRef.Preset(i), s, ui) }
    if (items.isEmpty()) null else SoundShelves.Shelf(c, c, emptyList(), items)
}
