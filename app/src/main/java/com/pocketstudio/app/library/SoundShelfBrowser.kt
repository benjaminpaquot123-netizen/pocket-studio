package com.pocketstudio.app.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PartySounds
import com.pocketstudio.model.SoundCharacter
import com.pocketstudio.model.SoundMarks
import com.pocketstudio.model.SoundShelves
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.SoundTile
import com.pocketstudio.ui.TileGrid
import com.pocketstudio.ui.pressScale

typealias SoundShelf = SoundShelves.Shelf<SoundOption>

/** The built-in sounds' folder (always first or second, however small). */
internal const val KIT_FAMILY = "Sons intégrés"

/** The project's style packs (its machine, its own pack): first, however small. */
internal const val STYLE_FAMILY = "Pour ce style"

/** The browser's own folders at the top: « Récents », « Favoris », « Ma bibliothèque ». */
private const val RECENTS = "@recents"
private const val FAVORITES = "@favorites"
/** « Ma bibliothèque »'s folder id (a chooser can open on it). */
internal const val MINE = "@mine"

/** A sound's key for « Récents » and « Favoris » ([SoundMarks]); null: a sound that is not kept (a demo). */
fun keyOf(ref: SoundRef): String? = when (ref) {
    is SoundRef.Kit -> "kit:${ref.voice}"
    is SoundRef.Pack -> "pack:${ref.packId}/${ref.sound.file}"
    is SoundRef.Party -> "party:${ref.id}"
    is SoundRef.Mine -> "mine:${ref.id}"
    else -> null
}?.takeIf(SoundMarks::isKey)

/** The party sounds as options (shipped in the app), and the downloaded packs' that belong to « Fête ». */
internal fun partyItems(installed: List<PackManifest>): List<SoundShelves.Item<SoundOption>> =
    PartySounds.ALL.map { SoundShelves.Item(SoundOption(SoundRef.Party(it.id), it.name), PartySounds.FAMILY, it.group) } +
        installed.flatMap { m ->
            m.sounds.mapNotNull { s ->
                PartySounds.fromPack(m.id, s)?.let { group ->
                    SoundShelves.Item(SoundOption(SoundRef.Pack(m.id, s), "${s.name} · ${m.name}"), PartySounds.FAMILY, group, m.id)
                }
            }
        }

/**
 * A pack sound's second level: the machine inside « Machines », the library's own groups (Conga,
 * Cartes à jouer…) in « Bruitages » and « Acoustique », else its measured character.
 */
internal fun subOf(m: PackManifest, file: String, name: String): String? = when (m.family) {
    "Machines" -> m.name
    "Bruitages", "Acoustique" -> SoundShelves.groupOfName(name)
    else -> SoundCharacter.of(m.id, file)
}

/**
 * The folders of a sound chooser, built from the categories its caller already lists (in their
 * order: the line's kit voice, the style's machine and own pack, the kit, the other packs): the
 * kit's voices in « Sons intégrés », the style's packs in « Pour ce style » (also filed in their
 * family), each pack's sounds in its family, and « Fête » when [party].
 */
fun shelvesOf(categories: List<SoundCategory>, installed: List<PackManifest>, styleId: String?, party: Boolean = false): List<SoundShelf> {
    val style = styleId?.let(StarterWizard::style)
    val stylePacks = (style?.kit?.values.orEmpty() + style?.packs.orEmpty() + listOfNotNull(styleId)).toSet()
    val items = ArrayList<SoundShelves.Item<SoundOption>>()
    val pinned = ArrayList<String>()
    fun pin(family: String) { if (family !in pinned) pinned += family }
    for (c in categories) {
        when {
            c.id == KIT_CATEGORY || c.id == KIT_OTHER_CATEGORY -> {
                pin(KIT_FAMILY)
                c.options.forEach { items += SoundShelves.Item(it, KIT_FAMILY) }
            }
            c.id.startsWith("pack:") -> {
                val id = c.id.removePrefix("pack:")
                val m = installed.firstOrNull { it.id == id } ?: continue
                if (id in stylePacks) {
                    pin(STYLE_FAMILY)
                    c.options.forEach { items += SoundShelves.Item(it, STYLE_FAMILY, pack = id) }
                }
                c.options.forEach { o ->
                    val s = (o.ref as? SoundRef.Pack)?.sound
                    items += SoundShelves.Item(o, m.family.ifBlank { SoundShelves.OTHERS }, s?.let { subOf(m, it.file, it.name) }, id)
                }
            }
        }
    }
    if (party) {
        pin(PartySounds.FAMILY)
        items += partyItems(installed)
    }
    return SoundShelves.build(items, pinned)
}

/**
 * The first sounds a quick list offers (a pad's sheet): the built-in ones, then the packs taking
 * turns, best first, so a dozen sounds are the best of every pack rather than one pack's first.
 */
fun quickPicks(kind: String, installed: List<PackManifest>, count: Int): List<SoundOption> {
    val categories = libraryCategories(kind, installed)
    val kit = categories.filter { it.id == KIT_CATEGORY }.flatMap { it.options }
    val packs = SoundShelves.turns(categories.filter { it.id != KIT_CATEGORY }.flatMap { c -> c.options.map { SoundShelves.Item(it, "", pack = c.id) } })
    return (kit + packs).take(count)
}

/** A folder's name as shown. */
internal fun shelfTitle(title: String): String = when (title) {
    KIT_FAMILY -> tr("Sons intégrés")
    STYLE_FAMILY -> tr("Pour ce style")
    PartySounds.FAMILY -> tr("Fête")
    else -> tr(title)
}

/** A folder's icon and colour: the style families as in the style chooser, the others their own. */
@Composable
internal fun shelfLook(family: String): Pair<ImageVector, Color> = when (family) {
    KIT_FAMILY -> Icons.Filled.Apps to Color(0xFF81C784)
    STYLE_FAMILY -> Icons.Filled.Star to Neon.Cyan
    PartySounds.FAMILY -> Icons.Filled.Celebration to Color(0xFFFF6E9C)
    "Machines" -> Icons.Filled.Memory to Color(0xFFFF8A65)
    "Sons actuels" -> Icons.Filled.NewReleases to Color(0xFF4DD0E1)
    "Acoustique" -> Icons.Filled.Forest to Color(0xFFA1887F)
    "Bruitages" -> Icons.Filled.Vibration to Color(0xFFF06292)
    RECENTS -> Icons.Filled.History to Color(0xFF90A4AE)
    FAVORITES -> Icons.Filled.Star to Color(0xFFFFD54F)
    MINE -> Icons.Filled.Person to Color(0xFF9575CD)
    else -> familyLook(family)
}

/** What the shared sound browser shows. */
class ShelfBrowserUi(
    val shelves: List<SoundShelf>,
    /** The kind of sound chosen (kick, stab…): the tiles' icon and colour. */
    val kind: String,
    /** « Récents » and « Favoris », by key ([keyOf]); only the ones this chooser offers are shown. */
    val recents: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    /** The sound heard now, and the one chosen (the assistant's line). */
    val playing: SoundRef? = null,
    val chosen: SoundRef? = null,
    /** Prefix of the test tags. */
    val tag: String = "shelf",
    /** The words of the button that puts the sound in place, translated (« Poser »). */
    val use: String = "",
    /** A touch chooses the sound (and plays it) instead of only playing it: the assistant's lines. */
    val pickOnTap: Boolean = false,
    /** « Ma bibliothèque »: its sounds (null: not offered here) and what to show inside it. */
    val mine: List<SoundOption>? = null,
    val mineContent: (@Composable () -> Unit)? = null,
    /** Sounds found by the search field (null: no search field). */
    val search: ((String) -> List<SoundOption>)? = null,
    /** Opened on this folder ([SoundShelves.Shelf.id]; null: the families). */
    val start: String? = null,
    /** A kind for one sound's colour in mixed lists (search, « Fête »): null, the browser's [kind]. */
    val kindOf: (SoundOption) -> String? = { null },
    /** What the open folder is remembered by (the assistant: one per line). */
    val stateKey: String = tag,
    /** Whether a sound is heard now, when [playing] alone cannot tell (the assistant's line excerpts). */
    val heard: ((SoundOption) -> Boolean)? = null,
)

/** « Récents » and « Favoris » for the browsers deep in a screen (the assistant), and ☆. */
class SoundMarksUi(val recents: List<String> = emptyList(), val favorites: List<String> = emptyList(), val toggle: (SoundRef) -> Unit = {})

val LocalSoundMarks = androidx.compose.runtime.staticCompositionLocalOf { SoundMarksUi() }

/** The sounds of [shelves] whose name or pack holds [query] (as written or as shown). */
internal fun shelfSearch(shelves: List<SoundShelf>, query: String): List<SoundOption> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    return shelves.flatMap { it.all }.distinct().filter { o ->
        val name = o.name.substringBefore(" · ")
        val pack = o.name.substringAfter(" · ", "")
        listOf(name, tr(name), pack).any { q in it.lowercase() }
    }
}

class ShelfBrowserActions(
    val preview: (SoundOption) -> Unit = {},
    val choose: (SoundOption) -> Unit = {},
    val toggleFavorite: (SoundOption) -> Unit = {},
)

/** How many tiles a folder shows before « Voir plus ». */
private const val PAGE = 30

/**
 * The one sound browser of the app (Benjamin's list n° 13–14): search and « Récents », « Favoris »,
 * « Ma bibliothèque » at the top, then the families as folders; inside, the sub-folders and the
 * sounds, best first, each with its pack in small. A touch plays a sound, its button puts it in
 * place; the back gesture goes up one folder.
 */
@Composable
fun ShelfBrowser(ui: ShelfBrowserUi, actions: ShelfBrowserActions, modifier: Modifier = Modifier) {
    var at by rememberSaveable(ui.stateKey) { mutableStateOf(ui.start.orEmpty()) }
    var query by rememberSaveable(ui.stateKey) { mutableStateOf("") }
    val all = remember(ui.shelves, ui.mine) {
        val map = LinkedHashMap<String, SoundOption>()
        (ui.shelves.flatMap { it.all } + ui.mine.orEmpty()).forEach { o -> keyOf(o.ref)?.let { map.putIfAbsent(it, o) } }
        map
    }
    val recents = ui.recents.mapNotNull(all::get)
    val favorites = ui.favorites.mapNotNull(all::get)
    val favoriteKeys = ui.favorites.toSet()
    // A folder that no longer exists (a pack deleted): back to the families.
    val folder = ui.shelves.firstNotNullOfOrNull { it.find(at) }
    val gone = when (at) {
        RECENTS -> recents.isEmpty()
        FAVORITES -> favorites.isEmpty()
        MINE -> ui.mine.isNullOrEmpty()
        else -> at.isNotEmpty() && folder == null
    }
    if (gone) at = ""
    val parent = ui.shelves.firstOrNull { s -> s.children.any { it.id == at } }?.id.orEmpty()
    BackHandler(enabled = at.isNotEmpty()) { at = parent }
    Column(modifier.fillMaxWidth().testTag("${ui.tag}_browser"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (at.isEmpty() && ui.search != null) {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(60) }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = tr("Effacer")) } },
                placeholder = { Text(tr("Chercher un son (doigts, verre, conga…)")) },
                modifier = Modifier.fillMaxWidth().testTag("${ui.tag}_search"),
            )
        }
        AnimatedContent(at to query.isNotBlank(), transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "shelf") { (here, searching) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    here.isEmpty() && searching && ui.search != null -> {
                        val hits = remember(query, ui.search) { ui.search.invoke(query.trim()).take(60) }
                        if (hits.isEmpty()) Text(tr("Aucun son trouvé : essaie un autre mot, ou télécharge d'autres packs ci-dessous."),
                            style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
                        Sounds(hits, ui, actions, favoriteKeys, "${ui.tag}_result")
                    }
                    here.isEmpty() -> {
                        if (recents.isNotEmpty()) FolderRow(RECENTS, tr("Récents"), recents.size, "${ui.tag}_recents") { at = RECENTS }
                        if (favorites.isNotEmpty()) FolderRow(FAVORITES, tr("Favoris"), favorites.size, "${ui.tag}_favorites") { at = FAVORITES }
                        if (!ui.mine.isNullOrEmpty()) FolderRow(MINE, tr("Ma bibliothèque"), ui.mine.size, "${ui.tag}_shelf_mine") { at = MINE }
                        ui.shelves.forEach { s -> FolderRow(s.title, shelfTitle(s.title), s.count, "${ui.tag}_folder_${s.id}") { at = s.id } }
                        if (favorites.isEmpty()) Text(tr("Touche ☆ sur un son pour le retrouver dans « Favoris »."),
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
                    else -> {
                        val title = when (here) {
                            RECENTS -> tr("Récents")
                            FAVORITES -> tr("Favoris")
                            MINE -> tr("Ma bibliothèque")
                            else -> listOfNotNull(
                                ui.shelves.firstOrNull { s -> s.children.any { it.id == here } }?.title?.let(::shelfTitle),
                                ui.shelves.firstNotNullOfOrNull { it.find(here) }?.title?.let(::shelfTitle),
                            ).joinToString(" › ")
                        }
                        Header(title, "${ui.tag}_back") { at = parent }
                        when (here) {
                            RECENTS -> Sounds(recents, ui, actions, favoriteKeys, "${ui.tag}_sound")
                            FAVORITES -> Sounds(favorites, ui, actions, favoriteKeys, "${ui.tag}_sound")
                            MINE -> ui.mineContent?.invoke() ?: Sounds(ui.mine.orEmpty(), ui, actions, favoriteKeys, "${ui.tag}_sound")
                            else -> ui.shelves.firstNotNullOfOrNull { it.find(here) }?.let { f ->
                                f.children.forEach { c -> FolderRow(f.title, shelfTitle(c.title), c.count, "${ui.tag}_folder_${c.id}") { at = c.id } }
                                Sounds(f.items, ui, actions, favoriteKeys, "${ui.tag}_sound")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A folder: its icon, its name, how many sounds, the whole width a target. */
@Composable
private fun FolderRow(family: String, title: String, count: Int, tag: String, open: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val (icon, color) = shelfLook(family)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(Neon.SurfaceHigh).border(1.dp, Neon.Outline, shape)
            .clickable(role = Role.Button, onClick = open).padding(horizontal = 12.dp, vertical = 8.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Neon.TextPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Text(trCount(count, "1 son", "%d sons"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1,
            modifier = Modifier.padding(start = 8.dp, end = 4.dp))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Neon.TextSecondary)
    }
}

/** Where we are, and the way back up. */
@Composable
private fun Header(title: String, tag: String, back: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back, modifier = Modifier.testTag(tag)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
    }
}

/** The sounds of a folder as tiles, [PAGE] at a time. */
@Composable
private fun Sounds(options: List<SoundOption>, ui: ShelfBrowserUi, actions: ShelfBrowserActions, favorites: Set<String>, tag: String) {
    var shown by remember(options) { mutableIntStateOf(PAGE) }
    val visible = options.take(shown)
    TileGrid(visible.size, minTileWidth = 260.dp) { i, m ->
        val option = visible[i]
        val look = SoundLooks.of(ui.kindOf(option) ?: ui.kind)
        val playing = ui.heard?.invoke(option) ?: (ui.playing == option.ref)
        val chosen = ui.chosen == option.ref
        val key = keyOf(option.ref)
        val source = option.name.substringAfter(" · ", "").takeIf { it.isNotBlank() }?.let { pack -> tr(pack) }
        SoundTile(
            title = tr(option.name.substringBefore(" · ")), icon = if (playing) Icons.Filled.GraphicEq else look.icon,
            accent = look.color, selected = playing || chosen,
            onClick = { if (ui.pickOnTap) actions.choose(option) else actions.preview(option) },
            subtitle = if (playing) tr("En écoute…") else source,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (key != null) {
                        val fav = key in favorites
                        IconButton(onClick = { actions.toggleFavorite(option) }, modifier = Modifier.testTag("${tag}_fav_$i")) {
                            Icon(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = if (fav) tr("Retirer des favoris") else tr("Ajouter aux favoris"),
                                tint = if (fav) Color(0xFFFFD54F) else Neon.TextSecondary)
                        }
                    }
                    if (!ui.pickOnTap) {
                        FilledTonalButton(onClick = { actions.choose(option) }, modifier = Modifier.pressScale().testTag("${tag}_use_$i")) { Text(ui.use.ifEmpty { tr("Poser") }) }
                    }
                }
            },
            modifier = m.testTag("${tag}_$i"),
        )
    }
    if (options.size > shown) {
        TextButton(onClick = { shown += PAGE }, modifier = Modifier.fillMaxWidth().testTag("${tag}_more")) {
            Text(tr("Voir plus (%d)", options.size - shown))
        }
    }
}
