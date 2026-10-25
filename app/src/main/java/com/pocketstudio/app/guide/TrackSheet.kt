package com.pocketstudio.app.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.app.learn.spot
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.sounds.SoundBrowser
import com.pocketstudio.app.sounds.SoundBrowserUi
import com.pocketstudio.model.CurveShape
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Favorites
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.LayerRole
import com.pocketstudio.model.LayerSound
import com.pocketstudio.model.Layers
import com.pocketstudio.model.PlaitsPatch
import com.pocketstudio.model.SoundCategories
import com.pocketstudio.model.SoundLibrary
import com.pocketstudio.app.sounds.SoundLooks
import com.pocketstudio.model.SynthPatch
import com.pocketstudio.model.TrackLayer
import com.pocketstudio.model.guide.GuideHistoryLine
import com.pocketstudio.model.guide.GuideSheet
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.guide.guideText
import com.pocketstudio.model.guide.guideTracks
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.layersOf
import com.pocketstudio.model.sceneLabel
import com.pocketstudio.model.sceneTitle
import com.pocketstudio.model.track
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.ProSlider
import com.pocketstudio.ui.ScaledBottomSheet
import com.pocketstudio.ui.ScaledDropdownMenu
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.pillColors
import kotlin.math.roundToInt

// --- La fiche de piste (docs/PLAN-GUIDE.md §3.3, G3) -------------------------------------------------

/** What a track's sheet does (each one a call of the ViewModels or the app's navigation; null: not offered). */
class TrackSheetActions(
    val close: () -> Unit = {},
    /** « Changer le son » (its sound chooser). */
    val changeSound: (() -> Unit)? = null,
    /** « Couches » (its [LayersEditor]): an instrument track's. */
    val layers: (() -> Unit)? = null,
    /** Its effects box. */
    val effects: (() -> Unit)? = null,
    /** It plays or stays silent in song part (index). */
    val togglePart: ((Int) -> Unit)? = null,
    /** « Garder ce son dans mes favoris ». */
    val favorite: (() -> Unit)? = null,
    /** « Réutiliser ce motif ailleurs »: Structure's parts. */
    val reuse: (() -> Unit)? = null,
    /** « Copier cette chaîne sur… » track (key). */
    val copyTo: (String) -> Unit = {},
    /** « Ramener » a line of the history (its group id). */
    val revert: (String) -> Unit = {},
    val dismissNotice: () -> Unit = {},
)

/** A track the chain can be copied to: its key and name. */
data class SheetTarget(val key: String, val name: String)

/**
 * A track's sheet in five columns (§3.3): « Son » (the sound, its layers), « Quand » (one box per
 * part, a touch makes it play or stay silent there, what the style advises), « Quel effet et
 * combien » (the chain in order, each effect's first setting and why, its sends), « Combien de
 * fois » (one chain for the song or not, the curves that move it), « Ailleurs ? » (favourite,
 * pattern, chain on another track), then its dated history, each line brought back in a gesture.
 * [sheet] null: the Guide is still reading the song.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSheet(
    name: String,
    sheet: GuideSheet?,
    partNames: List<String>,
    targets: List<SheetTarget>,
    notice: String?,
    actions: TrackSheetActions,
    now: Long = System.currentTimeMillis(),
    /** The Guide has not read the song yet. */
    loading: Boolean = sheet == null,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ScaledBottomSheet(onDismissRequest = actions.close, sheetState = state, containerColor = Neon.Background, modifier = Modifier.testTag("track_sheet")) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tr("Fiche : %s", name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("track_sheet_title"),
                        )
                        Text(tr("Son, quand, quels effets, combien de fois, ailleurs : tout sur cette piste."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
                    TextButton(onClick = actions.close, modifier = Modifier.heightIn(min = 48.dp).testTag("track_sheet_close")) { Text(tr("Fermer")) }
                }
                if (notice != null) SheetNotice(notice, actions.dismissNotice)
                if (sheet == null) {
                    Text(
                        if (loading) tr("Le Guide lit ton morceau…") else tr("Cette piste ne joue nulle part pour l'instant : sa fiche viendra quand elle jouera."),
                        color = Neon.TextSecondary, modifier = Modifier.testTag("track_sheet_loading"),
                    )
                } else {
                    SoundColumn(sheet, actions)
                    WhenColumn(sheet, partNames, actions)
                    ChainColumn(sheet, actions)
                    OftenColumn(sheet)
                    ElsewhereColumn(targets, actions)
                    HistoryColumn(sheet.lines, now, actions.revert)
                }
            }
        }
    }
}

@Composable
private fun SheetNotice(text: String, dismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.Amber.copy(alpha = 0.18f)).border(1.dp, Neon.Amber, RoundedCornerShape(10.dp))
            .padding(start = 10.dp).testTag("track_sheet_notice"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        Box(
            Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = tr("Fermer"), onClick = dismiss),
            contentAlignment = Alignment.Center,
        ) { Text("✕", color = Neon.TextSecondary) }
    }
}

/** « Son »: what plays and its layers; « Changer le son », « Couches ». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SoundColumn(sheet: GuideSheet, actions: TrackSheetActions) {
    NeonCard(Modifier.testTag("track_sheet_sound")) {
        SectionLabel(tr("Son"), foldKey = "sheet.sound")
        Text(sheet.sound.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Neon.TextPrimary)
        if (sheet.sound.layers.isNotEmpty()) {
            Text(
                tr("Avec ses couches : %s", sheet.sound.layers.joinToString(" · ")), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                modifier = Modifier.testTag("track_sheet_layer_names"),
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            actions.changeSound?.let { OutlinedButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp).testTag("track_sheet_change_sound")) { Text(tr("Changer le son")) } }
            actions.layers?.let {
                OutlinedButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp).spot("track_sheet_layers", tr("Les couches : plusieurs sons sur les mêmes notes")).testTag("track_sheet_layers")) {
                    Text(tr("Couches (%1\$d/%2\$d sons)", sheet.sound.layers.size + 1, Layers.MAX + 1))
                }
            }
        }
    }
}

/** « Quand »: one box per part of the song (lit where it plays), a touch to change it; what the style advises. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WhenColumn(sheet: GuideSheet, partNames: List<String>, actions: TrackSheetActions) {
    NeonCard(Modifier.testTag("track_sheet_when")) {
        SectionLabel(tr("Quand"), foldKey = "sheet.when")
        if (sheet.parts.isEmpty()) {
            Text(tr("Pas encore de morceau : la piste joue dans la boucle."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        } else {
            Text(
                tr("Joue dans %1\$d parties sur %2\$d.", sheet.parts.count { it }, sheet.parts.size), style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary,
                modifier = Modifier.testTag("track_sheet_plays"),
            )
            // One box per part, in the song's order, on a line that scrolls (a long song has 40 or 50).
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("track_sheet_parts"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sheet.parts.forEachIndexed { i, on ->
                    val label = partNames.getOrNull(i).orEmpty()
                    val shape = RoundedCornerShape(8.dp)
                    Column(
                        Modifier.width(56.dp).heightIn(min = 56.dp).clip(shape)
                            .background(if (on) Neon.Cyan.copy(alpha = 0.85f) else Neon.SurfaceHigh.copy(alpha = 0.5f))
                            .border(1.dp, if (on) Neon.Cyan else Neon.Outline, shape)
                            .then(actions.togglePart?.let { t -> Modifier.clickable(role = Role.Checkbox) { t(i) } } ?: Modifier)
                            .semantics {
                                selected = on
                                contentDescription = if (on) tr("Partie %1\$d, %2\$s : elle joue", i + 1, label) else tr("Partie %1\$d, %2\$s : elle se tait", i + 1, label)
                            }
                            .padding(vertical = 6.dp)
                            .testTag("track_sheet_part_$i"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val ink = if (on) Neon.OnAccent else Neon.TextSecondary
                        Text("${i + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ink, maxLines = 1)
                        Text(label, style = MaterialTheme.typography.labelSmall, color = ink, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
        sheet.advice?.let { Text(tr("Le style conseille : %s", guideText(it)), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary) }
    }
}

/** « Quel effet et combien »: the chain in the order the sound goes through it, each effect's start value and why; the sends. */
@Composable
private fun ChainColumn(sheet: GuideSheet, actions: TrackSheetActions) {
    NeonCard(Modifier.testTag("track_sheet_chain")) {
        SectionLabel(tr("Quel effet et combien"), foldKey = "sheet.chain")
        if (sheet.chain.isEmpty()) Text(tr("Aucun effet : le son passe tel quel."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        sheet.chain.forEachIndexed { n, e ->
            Column(Modifier.fillMaxWidth().testTag("track_sheet_effect_$n"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${n + 1}.", style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, modifier = Modifier.width(24.dp))
                    Text(tr(e.type.label), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (e.bypassed) Neon.TextSecondary else Neon.TextPrimary, modifier = Modifier.weight(1f))
                    if (e.valueLabel.isNotBlank()) Text(tr(e.valueLabel) + " " + e.value, style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
                }
                val notes = listOfNotNull(
                    e.why.takeIf { it.isNotBlank() }?.let(::guideText),
                    tr("coupé pour l'instant").takeIf { e.bypassed },
                    tr("seulement dans certaines parties").takeIf { !e.everywhere },
                )
                if (notes.isNotEmpty()) Text(notes.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(start = 24.dp))
            }
        }
        val reverb = (sheet.sends.getOrElse(0) { 0f } * 100).roundToInt()
        val echo = (sheet.sends.getOrElse(1) { 0f } * 100).roundToInt()
        Text(tr("Envois : réverbe %1\$d %%, écho %2\$d %%", reverb, echo), style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, modifier = Modifier.testTag("track_sheet_sends"))
        actions.effects?.let { OutlinedButton(onClick = it, modifier = Modifier.heightIn(min = 48.dp).testTag("track_sheet_effects")) { Text(tr("Régler les effets")) } }
    }
}

/** « Combien de fois »: one chain for the song (the ready-made one it is), the curves that move the track, part by part. */
@Composable
private fun OftenColumn(sheet: GuideSheet) {
    NeonCard(Modifier.testTag("track_sheet_often")) {
        SectionLabel(tr("Combien de fois"), foldKey = "sheet.often")
        val o = sheet.often
        Text(
            when {
                o.preset != null && o.onceForSong -> tr("C'est la chaîne « %s », une fois pour tout le morceau.", tr(o.preset!!))
                o.onceForSong -> tr("Une seule chaîne pour tout le morceau.")
                else -> tr("Certains effets ne jouent que dans certaines parties.")
            },
            style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary,
        )
        if (o.lanes.isEmpty()) Text(tr("Aucune courbe ne la fait bouger."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        o.lanes.forEach { l ->
            val how = when (l.shape) {
                CurveShape.Kind.RISES -> tr("monte")
                CurveShape.Kind.FALLS -> tr("descend")
                CurveShape.Kind.UP_DOWN -> tr("monte puis redescend")
                CurveShape.Kind.DOWN_UP -> tr("descend puis remonte")
                CurveShape.Kind.STEADY -> tr("reste stable")
                else -> tr("bouge")
            }
            val where = if (l.sections.isEmpty()) "" else " (" + l.sections.joinToString(", ") { DefaultNames.shown(it) } + ")"
            Text("• " + tr(l.what) + " : " + how + where, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary)
        }
    }
}

/** « Ailleurs ? »: keep the sound, reuse the pattern, copy the chain onto another track. */
@Composable
private fun ElsewhereColumn(targets: List<SheetTarget>, actions: TrackSheetActions) {
    NeonCard(Modifier.testTag("track_sheet_elsewhere")) {
        SectionLabel(tr("Ailleurs ?"), foldKey = "sheet.elsewhere")
        actions.favorite?.let { SheetLink(tr("Garder ce son dans mes favoris"), "track_sheet_favorite", it) }
        actions.reuse?.let { SheetLink(tr("Réutiliser ce motif dans une autre partie"), "track_sheet_reuse", it) }
        if (targets.isNotEmpty()) {
            var open by remember { mutableStateOf(false) }
            Box {
                SheetLink(tr("Copier cette chaîne sur…"), "track_sheet_copy") { open = true }
                ScaledDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    targets.forEach { t ->
                        DropdownMenuItem(text = { Text(t.name) }, onClick = { open = false; actions.copyTo(t.key) }, modifier = Modifier.testTag("track_sheet_copy_${t.key}"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetLink(text: String, tag: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onClick).heightIn(min = 48.dp).padding(horizontal = 4.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("↗", color = Neon.Cyan, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Neon.Cyan, fontWeight = FontWeight.SemiBold)
    }
}

/** « Historique »: what changed on the track, newest first, each line « Ramener » in a gesture. */
@Composable
private fun HistoryColumn(lines: List<GuideHistoryLine>, now: Long, revert: (String) -> Unit) {
    NeonCard(Modifier.testTag("track_sheet_history")) {
        SectionLabel(tr("Historique"), foldKey = "sheet.history")
        if (lines.isEmpty()) {
            Text(tr("Rien encore : chaque choix fait ici ou par le Guide s'inscrit ici, et se ramène d'un geste."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        }
        lines.take(MAX_LINES).forEach { l ->
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("track_sheet_line_${l.id}"), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(l.text, style = MaterialTheme.typography.bodyMedium, color = if (l.reverted) Neon.TextSecondary else Neon.TextPrimary)
                    val who = when {
                        l.card != null -> tr("par le Guide : %s", l.card!!)
                        l.byGuide -> tr("par le Guide")
                        else -> tr("à la main")
                    }
                    Text(
                        listOfNotNull(ago(now - l.at), who, tr("ramené").takeIf { l.reverted }).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary,
                    )
                }
                if (!l.reverted && l.id.isNotEmpty()) {
                    TextButton(onClick = { revert(l.id) }, modifier = Modifier.heightIn(min = 48.dp).testTag("track_sheet_revert_${l.id}")) { Text(tr("Ramener")) }
                }
            }
        }
    }
}

/** How long ago, in words (« à l'instant », « il y a 5 min », « il y a 3 h », « il y a 2 jours »). */
internal fun ago(ms: Long): String {
    val min = ms / 60_000
    return when {
        min < 1 -> tr("à l'instant")
        min < 60 -> tr("il y a %d min", min.toInt())
        min < 24 * 60 -> tr("il y a %d h", (min / 60).toInt())
        else -> tr("il y a %d jours", (min / (24 * 60)).toInt())
    }
}

private const val MAX_LINES = 30

/**
 * A track's sheet in the app: its Guide sheet (the analysis's), its parts' names, the tracks its
 * chain can go to, and what its buttons do. [key]: the track's Guide key ("inst:3", "pad:0"…).
 */
@Composable
internal fun TrackSheetRoute(
    vm: RhythmViewModel,
    key: String,
    close: () -> Unit,
    changeSound: (Int) -> Unit,
    layers: (Int) -> Unit,
    effects: (Int) -> Unit,
    reuse: () -> Unit,
) {
    val project by vm.project.collectAsStateWithLifecycle()
    val guide by vm.guide.ui.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val tracks = remember(project) { project.guideTracks() }
    val track = tracks.firstOrNull { it.key == key }
    // The track went away (an undo, another song): the sheet goes with it.
    if (track == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { close() }
        return
    }
    val inst = key.removePrefix("inst:").toIntOrNull()?.takeIf { key.startsWith("inst:") }
    // Each part by its section's name (« Drop »), else its scene's letter.
    val partNames = remember(project) { project.song.map { project.sceneLabel(it.scene).takeIf(String::isNotBlank)?.let(com.pocketstudio.model.Scene::shownLabel) ?: project.sceneTitle(it.scene) } }
    // The chain goes to a track of the same family: an instrument, the 303 or a voice to the others; a pad to the other pads.
    val pad = key.startsWith("pad:")
    val targets = tracks.filter { it.key != key && it.channel != com.pocketstudio.model.Mixer.MASTER && it.key != GuideTrack.DRUMS && it.key.startsWith("pad:") == pad }
        .map { SheetTarget(it.key, DefaultNames.shown(it.name)) }
    val partKey = GuideTrack.partKey(key)
    TrackSheet(
        name = DefaultNames.shown(track.name),
        sheet = guide.sheets.firstOrNull { it.track.key == key },
        partNames = partNames,
        targets = targets,
        notice = notice,
        loading = guide.loading,
        actions = TrackSheetActions(
            close = close,
            changeSound = inst?.let { id -> { changeSound(id) } },
            layers = inst?.let { id -> { layers(id) } },
            effects = { effects(track.channel) },
            togglePart = partKey?.takeIf { project.song.isNotEmpty() }?.let { k -> { i: Int -> vm.togglePartTrack(i, k) } },
            favorite = inst?.takeIf { project.track(it).let { t -> t.instrument == null && t.engine <= InstrumentTrack.ENGINE_PLAITS } }?.let { id -> { vm.saveFavoriteOf(id) } },
            reuse = if (project.song.isNotEmpty() && partKey != null) reuse else null,
            copyTo = { to -> vm.copyChain(key, to) },
            revert = { id -> vm.guide.revert(id) },
            dismissNotice = vm::dismissNotice,
        ),
    )
}

// --- Les couches (§3.4, G3) ----------------------------------------------------------------------------

/** What the layers editor does. [listen]: only layer (index, [Layers.OWN] its own sound), or every sound (null). */
class LayersEditorActions(
    val close: () -> Unit = {},
    val play: () -> Unit = {},
    val listen: (Int?) -> Unit = {},
    val add: (role: String, sound: LayerSound, label: String) -> Unit = { _, _, _ -> },
    val setSound: (index: Int, sound: LayerSound, label: String) -> Unit = { _, _, _ -> },
    /** A level drag (dB): one undo step per drag. */
    val setGain: (index: Int, db: Float) -> Unit = { _, _ -> },
    val setPan: (index: Int, pan: Float) -> Unit = { _, _ -> },
    /** Its transposition, in semitones (whole octaves here). */
    val setTranspose: (index: Int, semitones: Int) -> Unit = { _, _ -> },
    val setMute: (index: Int, mute: Boolean) -> Unit = { _, _ -> },
    val remove: (index: Int) -> Unit = {},
)

/** What a layers editor shows: the track, its own sound, its layers, what is heard and whether the song plays. */
data class LayersEditorUi(
    val track: String,
    val ownSound: String,
    val layers: List<TrackLayer>,
    /** Heard alone: a layer's index, [Layers.OWN], or null (every sound). */
    val heard: Int? = null,
    val playing: Boolean = false,
    val favorites: Favorites = Favorites(),
)

/** A sound being chosen: for a new layer of [role] ([index] null), or for layer [index]. */
private data class Picking(val role: String?, val index: Int?)

/**
 * The layers of a track (§3.4): its own sound, then up to [Layers.MAX] more, each with its sound,
 * its level, its octave and its place in the stereo; each heard alone (▶ plays the song), muted,
 * removed (Annuler, or « Ramener » in the sheet, brings it back). « + Ajouter une couche » asks
 * what it should bring (sub, body, attack, air: one role each), then its sound.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersEditor(ui: LayersEditorUi, actions: LayersEditorActions) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var picking by remember { mutableStateOf<Picking?>(null) }
    ScaledBottomSheet(onDismissRequest = actions.close, sheetState = state, containerColor = Neon.Background, modifier = Modifier.testTag("layers_editor")) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Couches de : %s", ui.track), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("layers_title"))
                        Text(
                            tr("Jusqu'à %d sons qui jouent les mêmes notes, chacun à sa place.", Layers.MAX + 1),
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                        )
                    }
                    PlayButton(ui.playing, actions.play)
                    TextButton(onClick = actions.close, modifier = Modifier.heightIn(min = 48.dp).testTag("layers_close")) { Text(tr("Fermer")) }
                }
                val p = picking
                if (p != null) {
                    SoundPicker(p, ui.favorites, back = { picking = null }) { role, sound, label ->
                        if (p.index != null) actions.setSound(p.index, sound, label) else actions.add(role, sound, label)
                        picking = null
                    }
                } else {
                    OwnSoundCard(ui, actions)
                    ui.layers.forEachIndexed { i, l -> LayerCard(i, l, ui.heard == i, actions) { picking = Picking(l.role, i) } }
                    if (ui.layers.size < Layers.MAX) {
                        AddLayerCard(ui.layers.map { it.role }.toSet()) { role -> picking = Picking(role, null) }
                    } else {
                        Text(tr("Quatre sons au maximum : retire une couche pour en mettre une autre."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayButton(playing: Boolean, play: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(if (playing) Neon.Acid else Neon.SurfaceHigh)
            .clickable(role = Role.Button, onClickLabel = if (playing) tr("Arrêter") else tr("Jouer"), onClick = play).testTag("layers_play"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = if (playing) tr("Arrêter") else tr("Jouer"), tint = if (playing) Color.Black else Neon.TextPrimary)
    }
}

/** The track's own sound, the first of its four: heard alone or with the others. */
@Composable
private fun OwnSoundCard(ui: LayersEditorUi, actions: LayersEditorActions) {
    NeonCard(Modifier.testTag("layers_own")) {
        SectionLabel(tr("Son de la piste"), foldKey = "layers.own")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui.ownSound, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Neon.TextPrimary, modifier = Modifier.weight(1f))
            if (ui.layers.isNotEmpty()) AloneChip(ui.heard == Layers.OWN, "layers_alone_own") { actions.listen(if (ui.heard == Layers.OWN) null else Layers.OWN) }
        }
    }
}

@Composable
private fun AloneChip(on: Boolean, tag: String, toggle: () -> Unit) {
    FilterChip(
        selected = on, onClick = toggle, label = { Text(if (on) tr("Seule ✓") else tr("Écouter seule")) }, colors = pillColors(Neon.Acid),
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
    )
}

/** One layer: its sound, alone / muted / removed, its level, its octave, its place. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LayerCard(i: Int, l: TrackLayer, alone: Boolean, actions: LayersEditorActions, pickSound: () -> Unit) {
    NeonCard(Modifier.testTag("layer_$i"), accent = if (alone) Neon.Acid else Neon.Outline) {
        SectionLabel(tr("Couche %1\$d · %2\$s", i + 1, Layers.shownTitle(l)), foldKey = "layers.layer")
        Text(
            LayerRole.title(l.role)?.let { roleWhy(l.role) } ?: tr("Joue les mêmes notes que la piste"),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AloneChip(alone, "layer_alone_$i") { actions.listen(if (alone) null else i) }
            FilterChip(
                selected = l.mute, onClick = { actions.setMute(i, !l.mute) }, label = { Text(if (l.mute) tr("Coupée") else tr("Couper")) }, colors = pillColors(Neon.Amber),
                modifier = Modifier.heightIn(min = 48.dp).testTag("layer_mute_$i"),
            )
            OutlinedButton(onClick = pickSound, modifier = Modifier.heightIn(min = 48.dp).testTag("layer_sound_$i")) { Text(tr("Changer le son")) }
            OutlinedButton(onClick = { actions.remove(i) }, modifier = Modifier.heightIn(min = 48.dp).testTag("layer_remove_$i")) { Text(tr("Retirer")) }
        }
        LabeledSlider(tr("Volume"), formatDb(l.gainDb), "layer_gain_$i") {
            ProSlider(l.gainDb.coerceIn(GAIN_MIN, GAIN_MAX), { actions.setGain(i, (it * 2).roundToInt() / 2f) }, valueRange = GAIN_MIN..GAIN_MAX, modifier = Modifier.fillMaxWidth().testTag("layer_gain_slider_$i"))
        }
        LabeledSlider(tr("Place"), formatPan(l.pan), "layer_pan_$i") {
            ProSlider(l.pan, { actions.setPan(i, if (kotlin.math.abs(it) < 0.04f) 0f else (it * 100).roundToInt() / 100f) }, valueRange = -1f..1f, modifier = Modifier.fillMaxWidth().testTag("layer_pan_slider_$i"))
        }
        val octave = (l.transpose / 12f).roundToInt()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tr("Octave"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, modifier = Modifier.width(72.dp))
            OutlinedButton(
                onClick = { actions.setTranspose(i, ((octave - 1) * 12).coerceAtLeast(-24)) }, enabled = octave > -2,
                modifier = Modifier.heightIn(min = 48.dp).testTag("layer_octave_down_$i"),
            ) { Text("−") }
            Text(Layers.signedSemitones(octave), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("layer_octave_$i"))
            OutlinedButton(
                onClick = { actions.setTranspose(i, ((octave + 1) * 12).coerceAtMost(24)) }, enabled = octave < 2,
                modifier = Modifier.heightIn(min = 48.dp).testTag("layer_octave_up_$i"),
            ) { Text("+") }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, tag: String, slider: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag(tag)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary)
        }
        slider()
    }
}

/** « + Ajouter une couche »: what it should bring (one role each: the ones there are greyed). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddLayerCard(taken: Set<String>, choose: (String) -> Unit) {
    NeonCard(Modifier.testTag("layers_add"), accent = Neon.Cyan) {
        SectionLabel(tr("Ajouter une couche"), foldKey = "layers.add")
        Text(tr("Qu'est-ce qui manque ?"), style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary)
        LayerRole.ALL.forEach { role ->
            val there = role in taken
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .then(if (there) Modifier else Modifier.clickable(role = Role.Button) { choose(role) })
                    .heightIn(min = 48.dp).padding(horizontal = 4.dp).testTag("layers_add_$role"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+", color = if (there) Neon.TextSecondary else Neon.Cyan, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp))
                Column(Modifier.weight(1f)) {
                    Text(LayerRole.title(role).orEmpty(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = if (there) Neon.TextSecondary else Neon.Cyan)
                    Text(if (there) tr("déjà là : un seul son par rôle") else roleWhy(role), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                }
            }
        }
    }
}

/** What a role brings, in everyday words. */
private fun roleWhy(role: String): String = when (role) {
    LayerRole.SUB -> tr("Un grave propre et centré, sous le reste")
    LayerRole.BODY -> tr("Plus de corps, dans le médium")
    LayerRole.ATTACK -> tr("Plus de claquant au début des notes")
    LayerRole.AIR -> tr("Plus d'air, une octave au-dessus")
    else -> ""
}

/** The sounds a layer can take (the synth's, Plaits', the favourites; the 808 for a sub). */
@Composable
private fun SoundPicker(p: Picking, favorites: Favorites, back: () -> Unit, pick: (role: String, sound: LayerSound, label: String) -> Unit) {
    val role = p.role.orEmpty()
    var plaits by rememberSaveable { mutableStateOf(false) }
    NeonCard(Modifier.testTag("layer_picker"), accent = Neon.Cyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = back, modifier = Modifier.heightIn(min = 48.dp).testTag("layer_picker_back")) { Text("← " + tr("Retour")) }
            Text(
                LayerRole.title(role)?.let { tr("Le son de la couche « %s »", it) } ?: tr("Le son de la couche"),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !plaits, onClick = { plaits = false }, label = { Text(tr("Synthé")) }, colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("layer_picker_synth"))
            FilterChip(selected = plaits, onClick = { plaits = true }, label = { Text("Plaits") }, colors = pillColors(), modifier = Modifier.heightIn(min = 48.dp).testTag("layer_picker_plaits"))
            if (role == LayerRole.SUB || role == "") {
                OutlinedButton(onClick = { pick(role, LayerSound.bass808(), "808") }, modifier = Modifier.heightIn(min = 48.dp).testTag("layer_picker_808")) { Text("808") }
            }
        }
        val ui = if (plaits) SoundBrowserUi(
            SoundCategories.PLAITS, SoundLibrary.PLAITS, favorites.plaits, current = null,
            pick = { i -> SoundLibrary.PLAITS[i].let { pick(role, LayerSound.plaits(it.sound), it.name) } },
            pickFavorite = { i -> favorites.plaits[i].let { pick(role, LayerSound.plaits(it.sound), it.name) } },
            tag = "layer_plaits_sounds", describe = { (it.sound as? PlaitsPatch)?.let(SoundLooks::describe) }, extras = false,
        ) else SoundBrowserUi(
            SoundCategories.SYNTH, SoundLibrary.SYNTH, favorites.synth, current = null,
            pick = { i -> SoundLibrary.SYNTH[i].let { pick(role, LayerSound.synth(it.sound), it.name) } },
            pickFavorite = { i -> favorites.synth[i].let { pick(role, LayerSound.synth(it.sound), it.name) } },
            tag = "layer_synth_sounds", describe = { (it.sound as? SynthPatch)?.let(SoundLooks::describe) }, extras = false,
        )
        SoundBrowser(ui)
    }
}

private const val GAIN_MIN = -30f
private const val GAIN_MAX = 6f

private fun formatDb(db: Float): String = if (db <= Layers.MIN_GAIN_DB) tr("muet") else (if (db > 0f) "+" else if (db < 0f) "−" else "") + "%.1f dB".format(kotlin.math.abs(db))

private fun formatPan(pan: Float): String = when {
    kotlin.math.abs(pan) < 0.005f -> tr("au centre")
    pan < 0f -> tr("gauche %d", (-pan * 100).roundToInt())
    else -> tr("droite %d", (pan * 100).roundToInt())
}

/** The layers editor of instrument track [track] in the app: one undo step per gesture (a drag one), each kept in its sheet's history. */
@Composable
internal fun LayersEditorRoute(vm: RhythmViewModel, track: Int, close: () -> Unit) {
    val project by vm.project.collectAsStateWithLifecycle()
    val transport by vm.transport.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    if (track != 0 && project.tracks.none { it.id == track }) {
        androidx.compose.runtime.LaunchedEffect(Unit) { close() }
        return
    }
    var heard by remember(track) { mutableStateOf<Int?>(null) }
    val layers = project.layersOf(track)
    // A layer heard alone that went away (removed, undone): every sound again.
    if (heard != null && heard != Layers.OWN && heard!! !in layers.indices) heard = null
    // Leaving: every sound heard again (listening is live only, saved nowhere).
    DisposableEffect(track) { onDispose { vm.soloLayer(track, null) } }
    val t = project.track(track)
    LayersEditor(
        LayersEditorUi(DefaultNames.shown(t.name), com.pocketstudio.model.guide.GuideSheets.soundName(t), layers, heard, transport.playing, favorites),
        LayersEditorActions(
            close = close,
            play = vm::togglePlay,
            listen = { only -> heard = only; vm.soloLayer(track, only) },
            add = { role, sound, label -> vm.addLayer(track, sound, role, label) },
            setSound = { i, sound, label -> vm.editLayer(track, i) { it.copy(sound = sound, label = label) } },
            setGain = { i, db -> vm.setLayer(track, i, key = "gain", gainDb = db) },
            setPan = { i, pan -> vm.setLayer(track, i, key = "pan", pan = pan) },
            setTranspose = { i, st -> vm.setLayer(track, i, transpose = st) },
            setMute = { i, m -> vm.setLayer(track, i, mute = m) },
            remove = { i -> heard = null; vm.removeLayer(track, i) },
        ),
    )
}

/** The Guide track key of instrument track [id] (a track's sheet from the track bar). */
fun sheetKeyOf(id: Int): String = GuideTrack.inst(id)
