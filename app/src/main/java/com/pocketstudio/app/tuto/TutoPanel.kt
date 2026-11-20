package com.pocketstudio.app.tuto

import androidx.compose.animation.animateContentSize
import com.pocketstudio.model.i18n.inSentence
import com.pocketstudio.model.tuto.TutoNeed
import com.pocketstudio.app.guide.guideEdge
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import com.pocketstudio.app.learn.spot
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pocketstudio.model.Scene
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoKit
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoMapBlock
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.ui.Neon

/** A text of the course (tutos/<style>.json, checked by TutoCourseTest) or a style's name, in the interface's language. */
internal fun tutoText(fr: String): String = tr(fr)

/**
 * Audit ultra round 3, n° 29 (third time): a step names a pad by the kit's name (« Kick », shown « Grosse caisse 1 »
 * in the Mode simple, « Basdrum 1 » in Dutch) while the pad shows the sound on it (« Kick studio », a pack's
 * sound): the step says the pad's own name, as its pad shows it, in every language and mode.
 */
fun tutoPadNames(text: String, project: com.pocketstudio.model.Project): String {
    val kit = com.pocketstudio.app.rhythm.DrumKitInfo.names
    val names = kit.indices.associate { lane -> kit[lane] to com.pocketstudio.app.rhythm.padName(project, lane) }
    return com.pocketstudio.model.i18n.QuotedNames.rename(text, names)
}

/**
 * Tour 6 (T2): [shown], the translation of the course's French [fr], with each pad [fr] names between quotes said
 * by the kit's own name in this language, so that [tutoPadNames] finds it.
 */
internal fun tutoKitQuotes(fr: String, shown: String): String {
    if (fr == shown) return shown
    val quoted = Regex("[$TUTO_OPEN][\\s  ]?([^$TUTO_OPEN$TUTO_CLOSE]+?)[\\s  ]?[$TUTO_CLOSE]")
    val theirs = quoted.findAll(fr).map { it.groupValues[1] }.toList()
    val ours = quoted.findAll(shown).toList()
    // Only when each sentence quotes one thing: a language orders its words its own way, so with several quotes the k-th of the
    // translation is not the k-th of the French (fresh review, grave: « Envoi réverbe » became « Clap » in Chinese).
    if (theirs.size != 1 || ours.size != 1) return shown
    val kit = com.pocketstudio.app.rhythm.DrumKitInfo.names.toSet()
    val out = StringBuilder(shown)
    // From the end, so that the places still to change stay where they are.
    for (k in ours.indices.reversed()) {
        // A pad of the kit the French names (« Kick »), as this language names it (its pad's name: [tutoPadNames]).
        val name = tr(theirs[k]).takeIf { it in kit } ?: continue
        val g = ours[k].groups[1] ?: continue
        if (g.value != name) out.replace(g.range.first, g.range.last + 1, name)
    }
    return out.toString()
}

/** The quotes a course's words use, in every language (as [com.pocketstudio.model.i18n.QuotedNames]). */
private const val TUTO_OPEN = "«“„「『\"‘"
private const val TUTO_CLOSE = "»”“」』\"’"

/** A step's words ([fr], the course's French) as the panel shows them: translated, each pad by its name on the screen ([names]). */
private fun tutoWords(fr: String, names: (String) -> String): String = names(tutoKitQuotes(fr, tutoText(fr)))

/**
 * What the tutorial draws on the screens: dotted cells of a drum step (pad, steps), dotted notes of
 * a melodic step (track, notes), the cells « Montre-moi » lit fading away (pad, steps, how much they
 * show), the send it asks for in an effects box (channel, bus); [focus] goes up when « Je le fais »
 * asks to see the next dotted note; [demo]: « Montre-moi » under way (its frames follow each other).
 */
object TutoHints {
    var cells by mutableStateOf<Pair<Int, Set<Int>>?>(null)
    /** Tour 9, F093: the dotted [cells] a tap does not make ([com.pocketstudio.model.tuto.TutoPlay.longPressCells]). */
    var longPress by mutableStateOf<Set<Int>>(emptySet())

    /** What TalkBack says of an empty cell [step] of pad [lane]: null when it is not dotted. */
    fun dottedState(lane: Int, step: Int): String? =
        when {
            cells?.let { (l, c) -> l == lane && step in c } != true -> null
            step in longPress -> tr("vide, en pointillé : appui long")
            else -> tr("vide, en pointillé : à toucher")
        }
    var notes by mutableStateOf<Pair<Int, List<com.pocketstudio.model.Note>>?>(null)
    /** The 303 steps a step asks for, by index (the Acide tab draws them dashed until written). */
    var acid by mutableStateOf<Map<Int, com.pocketstudio.model.AcidStep>?>(null)
    var fading by mutableStateOf<Triple<Int, Set<Int>, Float>?>(null)
    var send by mutableStateOf<Pair<Int, Int>?>(null)
    var focus by mutableIntStateOf(0)
    /** The [focus] the piano roll last answered (not state: read and written there only). */
    var focusSeen = 0
    /** The [focus] a touch on the roll's line asked (it may go to a hidden note); any other one comes from the step. */
    var focusTouched = -1
    var demo by mutableStateOf(false)
    /** The hits a step asks to take away, by pad ([TutoChecks.toErase]): drawn crossed out. */
    var erase by mutableStateOf<Map<Int, Set<Int>>?>(null)
    /** The synth knobs a step names ([TutoGo.knobs]): dashed, the advanced ones unfolded. */
    var knobs by mutableStateOf<Set<Int>>(emptySet())
    /** The mixer strip a step points at ([TutoGo.strip]): the simple Mix opens its drums when it is a drum sound's. */
    var strip by mutableStateOf<Int?>(null)
    /** A send step's halves (« Accords ✓ · Caisse : à régler ici »), over its dashed send. */
    var sendNote by mutableStateOf<String?>(null)
    /** The tutorial runs (its panel has the bottom of the screen). */
    var active by mutableStateOf(false)
    /** Goes up each time a step is ticked (a hit's menu closes). */
    var ticked by mutableIntStateOf(0)
    /**
     * What the screen just changed by itself for the tutorial (« Mesure 2 → », « → Charley »), said a
     * moment over the grid (testers, lot « tuto 4 »: the page turned without a word, the hits seemed gone).
     */
    var banner by mutableStateOf<String?>(null)
    /**
     * The drum grid with its tutorial line is on the screen (RhythmScreen): with dotted cells to touch on it, the panel
     * leaves their rows room (replay t6-ha19-cases-panneau: at 100 % the first row was cut under the open panel).
     */
    var gridInSight by mutableStateOf(false)
    /** « Régler le son de la piste ↓ » is on the screen (the notes page): a step on the track's sound reveals it, the panel leaves it room. */
    var soundButtonInSight by mutableStateOf(false)
    /** The step shown (its id): a screen places its view once per step, never again by itself (lot « tuto 5 »). */
    var step by mutableStateOf<String?>(null)
    /**
     * « Copier sur les mesures suivantes » when it is the next thing to do (the motif written by hand), offered
     * where the user looks (the grid's line) as well as in the panel; null: not now.
     */
    var copy by mutableStateOf<(() -> Unit)?>(null)
    /** « Je le fais » asked the drum grid for a page: (pad, bar from 0, when). */
    var page by mutableStateOf<Triple<Int, Int, Long>?>(null)
    /** [copy] is the drum grid's (the hats' strengths), not the piano roll's. */
    var copyOnGrid by mutableStateOf(false)

    /**
     * Every field back to its default. A test that drove a real screen (the piano roll, the grid) leaves these set;
     * the next test must not read them (Tour 6: `Tour6TutoPanelAlreadyTest` turned red after `Tour6TutoFlechesRollTest`,
     * which had cleared only [cells] and [notes]). The app itself never calls this.
     */
    @androidx.annotation.VisibleForTesting
    fun reset() {
        cells = null; longPress = emptySet(); notes = null; acid = null; fading = null; send = null; focus = 0; focusSeen = 0; focusTouched = -1; demo = false
        erase = null; knobs = emptySet(); strip = null; sendNote = null; active = false; ticked = 0; banner = null; gridInSight = false; soundButtonInSight = false
        step = null; copy = null; page = null; copyOnGrid = false
    }
}

/** The tutorial's line over the drum grid, as a spot (a reveal of the cells keeps it in sight). */
const val TUTO_GRID_LINE_SPOT = "tuto_grid_line"

/** How long a screen waits before turning by itself for the tutorial (the last touch seen first). */
const val TUTO_TURN_MS = 450L

/** The room of a step's words while its dotted cells are on the drum grid shown ([TutoHints.gridInSight]). */
private val GRID_WORDS_MAX = 120.dp

/**
 * Tour 9 (D02, D13, D14; seats t9-44, t9-51, t9-65): the screen's least room above the tuto panel, under its top bar (a
 * part's header, the grid's line and a whole row of cells, a pad row, the piano roll's notes), at 100 %; it grows with the
 * font (up to 200 %), as what it holds does. The panel's ceiling is what leaves it: past it, the folded panel shows its
 * compact form (the chapter's title and line give their room to the screen).
 */
val TUTO_SCREEN_LEAST = 200.dp

/** How long [TutoHints.banner] stays. */
const val TUTO_BANNER_MS = 1800L

/** « Mesure 2 → »: the grid turned to bar [bar] (from 0) by itself. */
fun tutoBarBanner(bar: Int, forward: Boolean): String = if (forward) tr("Mesure %d →", bar + 1) else tr("← Mesure %d", bar + 1)

/** A short banner over the grid, a moment after the tutorial turned the screen ([TutoHints.banner]); touches go through. */
@Composable
fun TutoBanner(modifier: Modifier = Modifier) {
    val text = TutoHints.banner ?: return
    androidx.compose.runtime.LaunchedEffect(text) { kotlinx.coroutines.delay(TUTO_BANNER_MS); if (TutoHints.banner == text) TutoHints.banner = null }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.OnAccent,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Neon.Amber).padding(horizontal = 16.dp, vertical = 8.dp).testTag("tuto_banner"),
        )
    }
}

/**
 * The tutorial's line over the drum grid (testers, lot « tuto 5 »), always there while the tutorial
 * runs, one height: « Mesure 1/2 · Caisse », then « encore 2 cases » on this page, and « Mesure 2 ·
 * 3 cases → » when they are on another page (a touch goes there: the page never turns by itself),
 * what the screen just changed (« → Charley »), and « Copier » when it is the next thing to do.
 * Nothing of it over the cells.
 */
@Composable
fun TutoGridLine(bar: Int, bars: Int, pad: String, here: Int, elsewhere: Pair<Int, Int>?, go: (Int) -> Unit, copy: (() -> Unit)?, erasing: Boolean = false) {
    val said = TutoHints.banner
    androidx.compose.runtime.LaunchedEffect(said) { if (said != null) { kotlinx.coroutines.delay(TUTO_BANNER_MS); if (TutoHints.banner == said) TutoHints.banner = null } }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).background(Neon.Amber.copy(alpha = 0.10f)).padding(start = 10.dp, end = 4.dp)
            .spot(TUTO_GRID_LINE_SPOT).testTag("tuto_grid_line"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                (if (bars > 1) tr("Mesure %1\$d/%2\$d", bar + 1, bars) + " · " else "") + pad,
                style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("tuto_grid_where"),
            )
            val status = when {
                said != null -> said
                // n° 34: a hit too many (crossed out) is said as such, not as a dotted cell to touch.
                here > 0 && erasing -> trCount(here, "%d coup en trop ici (barré)", "%d coups en trop ici (barrés)")
                here > 0 -> trCount(here, "Encore %d case en pointillé ici", "Encore %d cases en pointillé ici")
                else -> null
            }
            if (status != null) Text(status, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Amber, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("tuto_grid_status"))
        }
        when {
            copy != null -> Button(onClick = copy, modifier = Modifier.heightIn(min = 40.dp).testTag("tuto_grid_copy")) { Text(tr("Copier"), maxLines = 1) }
            // Tour 9 F046: the other bars' cells said from the start, not only once this page is done.
            elsewhere != null -> TextButton(onClick = { go(elsewhere.first) }, modifier = Modifier.heightIn(min = 48.dp).testTag("tuto_bar_go")) {
                Text(
                    trCount(elsewhere.second, "Mesure %2\$d · %1\$d case", "Mesure %2\$d · %1\$d cases", elsewhere.second, elsewhere.first + 1) + (if (elsewhere.first > bar) " →" else ""),
                    color = Neon.Amber, fontWeight = FontWeight.Bold, maxLines = 1,
                )
            }
        }
    }
}

/** Over the piano roll: the track and the bar in sight (« Basse · mesure 2/8 »). */
fun tutoRollWhere(track: String?, bar: Int, bars: Int): String =
    listOfNotNull(track?.takeIf { it.isNotBlank() }, tr("mesure %1\$d/%2\$d", bar, bars)).joinToString(" · ")

/** Over the piano roll, the notes to write by hand all written: « Copier » beside it does the rest. */
fun tutoMotifDone(): String = tr("Motif posé ! La suite se copie :")

/**
 * With « Accords » on, one touch writes a whole dotted chord: the line counts chords, said as such (control seats:
 * « Encore 9 notes » over three chords), the arrow of [way] around it.
 */
fun tutoChordsLeft(left: Int, way: com.pocketstudio.model.tuto.TutoWay?, also: com.pocketstudio.model.tuto.TutoWay? = null): String {
    if (left <= 0) return tutoNotesLeft(0, way)
    val said = tr("Accords en pointillé à toucher : %d", left)
    // The arrow of [also] leads the line: at its end « Touche ici » cut it off on a 360 dp phone (tour 7, end checks).
    if (way == null) return when (also) {
        com.pocketstudio.model.tuto.TutoWay.Higher -> "↑ $said"
        com.pocketstudio.model.tuto.TutoWay.Lower -> "↓ $said"
        else -> said
    }
    return when (way) {
        com.pocketstudio.model.tuto.TutoWay.Higher -> "$said ↑"
        com.pocketstudio.model.tuto.TutoWay.Lower -> "$said ↓"
        com.pocketstudio.model.tuto.TutoWay.Later -> "$said →"
        com.pocketstudio.model.tuto.TutoWay.Earlier -> "← $said"
        null -> said
    }
}

/** « Encore 3 notes en pointillé » over the piano roll ([way]: none of them in sight, the next one is that way; [also]: one more out of the rows). */
fun tutoNotesLeft(left: Int, way: com.pocketstudio.model.tuto.TutoWay?, also: com.pocketstudio.model.tuto.TutoWay? = null): String = when {
    left <= 0 -> tr("Toutes les notes en pointillé sont posées")
    way == com.pocketstudio.model.tuto.TutoWay.Higher -> trCount(left, "Encore %d note en pointillé, plus haut ↑", "Encore %d notes en pointillé, plus haut ↑")
    way == com.pocketstudio.model.tuto.TutoWay.Lower -> trCount(left, "Encore %d note en pointillé, plus bas ↓", "Encore %d notes en pointillé, plus bas ↓")
    way == com.pocketstudio.model.tuto.TutoWay.Later -> trCount(left, "Encore %d note en pointillé, plus loin →", "Encore %d notes en pointillé, plus loin →")
    way == com.pocketstudio.model.tuto.TutoWay.Earlier -> trCount(left, "← Encore %d note en pointillé, plus tôt", "← Encore %d notes en pointillé, plus tôt")
    // [also]: some are in sight, one more is over or under the rows in sight (control « tuto2 », seat a): the arrow alone,
    // at the head of the line (at its end « Touche ici » cut it off on a 360 dp phone).
    else -> when (also) { com.pocketstudio.model.tuto.TutoWay.Higher -> "↑ "; com.pocketstudio.model.tuto.TutoWay.Lower -> "↓ "; else -> "" } +
        trCount(left, "Encore %d note en pointillé à toucher", "Encore %d notes en pointillé à toucher")
}

/** What the panel's buttons do ([TutoController]'s). */
class TutoActions(
    val listen: () -> Unit = {},
    val doIt: () -> Unit = {},
    val showMe: () -> Unit = {},
    val stopDemo: () -> Unit = {},
    val need: () -> Unit = {},
    val markDone: () -> Unit = {},
    val toggle: () -> Unit = {},
    val next: () -> Unit = {},
    val previous: () -> Unit = {},
    val chapters: () -> Unit = {},
    val quit: () -> Unit = {},
    /** « Recommencer le tuto »: a new empty project, the same style and level. */
    val restart: () -> Unit = {},
    /** « ▶ Écouter ton résultat » on a step done: the project's own music (again: it stops). */
    val playMine: () -> Unit = {},
    /** The end's list of what is left: that step, in one touch (chapter, step). */
    val goTo: (Int, Int) -> Unit = { _, _ -> },
    /** « Il te reste… » closed: back to the step it was on. */
    val leaveEnd: () -> Unit = {},
)

/**
 * Socle 2 (owner: every tutorial on its style's box of sounds): at the tutorial's start, when the
 * style's pack is not on the phone, « Télécharger le kit <style> (taille) » in one touch, or
 * « Plus tard » to go on with the built-in sounds. Audit ultra round 3, n° 28 (third time): it is a
 * block at the end of the step's words in [TutoPanel] ([TutoPanel]'s `kit`), never the panel's
 * stand-in: it came with the catalogue while the user read, and the touch meant for « Lire la suite »
 * or « Écouter l'exemple » seconds later landed on it. [progress]: the download under way (0..1),
 * [error]: why it did not come (the built-in sounds stay).
 */
@Composable
fun TutoKitCard(
    style: String, size: String, progress: Float?, error: String?, download: () -> Unit, later: () -> Unit, modifier: Modifier = Modifier,
    /** The kit is on the phone already, the course under way: « Mettre ces sons » ([download]) rather than a download. */
    onPhone: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    val pinned = LocalKitPinned.current
    // Tour 9 F016 (t9-28): the size never breaks between its figure and its unit.
    val label = if (onPhone) tr("Mettre les sons du kit %s sur les pads", style) else tr("Télécharger le kit %1\$s (%2\$s)", style, size.replace(' ', '\u00A0'))
    // Round 3, n° 28: inside the panel, never in its place. Tour 7 D3 end: pinned under the words, one row, nothing more
    // (the long sentence pushed its buttons below « Lire la suite »).
    if (pinned) {
        // Fresh review of « fable-repar » (A): its words grow up to 130 % only (as « Chapitres » does): at 200 % the row
        // was three lines high and took the room of the step's instruction.
        val by = minOf(1f, 1.3f / LocalDensity.current.fontScale.coerceAtLeast(0.1f))
        val compact = MaterialTheme.typography.labelLarge.let { it.copy(fontSize = it.fontSize * by, lineHeight = it.lineHeight * by) }
        Column(
            modifier.fillMaxWidth().padding(top = 4.dp).clip(shape).background(Neon.Amber.copy(alpha = 0.08f)).border(1.dp, Neon.Amber, shape)
                .padding(horizontal = 6.dp, vertical = 4.dp).testTag("tuto_kit"),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Red, maxLines = 2, modifier = Modifier.testTag("tuto_kit_error")) }
            if (progress != null) {
                Text(tr("Téléchargement du kit… %d %%", (progress * 100).toInt()), style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("tuto_kit_progress"))
                LinearProgressIndicator(progress = { progress }, color = Neon.Amber, trackColor = Neon.SurfaceHigh, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (progress == null) {
                    Button(
                        onClick = download, colors = ButtonDefaults.buttonColors(containerColor = Neon.Amber, contentColor = Neon.Background),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_kit_download"),
                    ) { Text(label, textAlign = TextAlign.Center, style = compact) }
                } else Spacer(Modifier.weight(1f))
                // Tour 9 (F017): the download under way, only its progress (« Plus tard » went on beside it).
                if (progress == null) TextButton(
                    onClick = later, contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("tuto_kit_later"),
                ) { Text(tr("Plus tard"), color = Neon.TextSecondary, maxLines = 2, textAlign = TextAlign.Center, style = compact) }
            }
        }
        return
    }
    Column(
        modifier.fillMaxWidth().padding(top = 4.dp).clip(shape).background(Neon.Amber.copy(alpha = 0.08f)).border(1.dp, Neon.Amber, shape)
            .padding(10.dp).testTag("tuto_kit"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(tr("Le kit %s", style), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            if (onPhone) tr("Le kit est sur ton téléphone. Ses sons remplaceront ceux de tes pads (« Annuler » les remet).")
            else tr("Les vrais sons de batterie du style, pour ton morceau et pour les exemples. Sans eux, le tuto marche avec les sons intégrés."),
            style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.Red, modifier = Modifier.testTag("tuto_kit_error")) }
        if (progress != null) {
            Text(tr("Téléchargement du kit… %d %%", (progress * 100).toInt()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("tuto_kit_progress"))
            LinearProgressIndicator(progress = { progress }, color = Neon.Amber, trackColor = Neon.SurfaceHigh, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
        } else {
            Button(
                onClick = download, colors = ButtonDefaults.buttonColors(containerColor = Neon.Amber, contentColor = Neon.Background),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_kit_download"),
            ) { Text(label, textAlign = TextAlign.Center) }
        }
        if (progress == null) TextButton(onClick = later, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("tuto_kit_later")) { Text(tr("Plus tard"), color = Neon.TextSecondary) }
    }
}

/** « Écouter mon morceau » at the course's end (round 3, n° 30). */
@Composable
private fun PlayMineButton(actions: TutoActions, playing: Boolean) {
    Button(onClick = actions.playMine, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_end_play")) {
        Icon(if (playing) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(" " + if (playing) tr("Arrêter") else tr("Écouter mon morceau"), maxLines = 1)
    }
}

/** « Recommencer le tuto ». */
fun restartTutoLabel(): String = tr("Recommencer le tuto")

/** Tour 9 F019: every step of [p]'s course settled (done, or in Pro an example): nothing left to resume, only to restart. */
fun tutoCourseFinished(p: com.pocketstudio.model.tuto.TutoProgress?): Boolean =
    p != null && TutoCourse.of(p.style)?.chaptersFor(p.level)?.flatMap { it.steps }
        ?.all { it.id in p.done || (p.level == TutoLevel.Pro && it.listening) } == true

/** The words of a need's button, as the step's text names it. */
fun needLabel(need: TutoNeed, style: String? = null): String = when (need) {
    is TutoNeed.Bars -> tr("Passer à %d mesures", need.bars)
    is TutoNeed.Track -> when (need.role) {
        // Trap, drill…: the bass is an 808 track (« + Piste › Basse 808 »).
        "bass" -> if (style?.let { com.pocketstudio.model.tuto.TutoTarget.of(it)?.bass808 } == true) tr("Créer la piste 808") else tr("Créer la piste Basse")
        "pad" -> tr("Créer la piste Accords")
        "lead" -> tr("Créer la piste Mélodie")
        com.pocketstudio.model.tuto.TutoTarget.PAD2 -> tr("Créer la piste Nappe")
        com.pocketstudio.model.tuto.TutoTarget.COUNTER -> tr("Créer la piste Contre-chant")
        // Lot « tuto 4 »: the Sub is the bass an octave down, written by the same touch.
        else -> tr("Copier la basse une octave plus bas")
    }
    is TutoNeed.Part -> tr("Créer la partie « %s »", Scene.shownLabel(need.label))
    is TutoNeed.Name -> tr("Nommer la partie « %s »", Scene.shownLabel(need.label))
    TutoNeed.Lengths -> tr("Donner les durées conseillées")
    is TutoNeed.Scene -> tr("Aller dans la partie « %s »", Scene.shownLabel(need.label))
    is TutoNeed.Copy -> tr("Copier sur les mesures suivantes")
    is TutoNeed.Course -> tr("Poser ce qui manque")
}

/**
 * Tour 7, P7 (plan § 2): one moment on screen. In Facile, while the engine asks for a preparation button first (the
 * step's first unmet need), the words say that button alone; the step's own text comes once it is done. Null: the
 * step's text. Pro keeps its whole text (its buttons are named in it).
 */
internal fun momentWords(ui: TutoUi): String? {
    val need = ui.need ?: return null
    if (ui.progress.level != com.pocketstudio.model.tuto.TutoLevel.Facile) return null
    // Tour 9, F058: the button's own quotes (« Créer la partie « Intro » ») dropped inside the line's quotes.
    val label = needLabel(need, ui.progress.style).filterNot { it in TUTO_OPEN || it in TUTO_CLOSE }.replace(Regex("\\s{2,}"), " ").trim()
    return tr("À toi : touche « %s ».", label)
}

/** The panel's background: the screen's, tinted with the tutorial's colour (never a page card's grey). */
@Composable
private fun panelColor(): Color = androidx.compose.ui.graphics.lerp(Neon.Background, Neon.Cyan, 0.07f)

/**
 * The tutorial over the bottom of the screen, apart from the page's cards (its own tint, an accent
 * edge that beats like the Guide's): the chapter's title, chapter X/N and step x/n with a progress
 * bar, then (unfolded) « À toi : … », its « Pourquoi ? », the song's map in the structure chapters,
 * then its action bar: the step's main action as a big filled button (what it needs first, « Je le
 * fais », « C'est fait »), « ▶ Écouter l'exemple » and « Montre-moi » under it, « ← Précédent » and
 * « Suivant → ». A step done stays (« Bravo, c'est réussi ! » in the text's place): « Étape
 * suivante » is the big button, « ▶ Écouter ton résultat » ([mine]: something to hear in the
 * project, [minePlaying]: it plays) beside the example; a listening step about the user's own music
 * ([mineFirst]) offers it first. Folded, one line of the same height whatever it says (the step,
 * « ✓ Réussi · Étape suivante → », « Montre-moi »'s caption): the screen stays free to work on.
 * Testers (lot « tuto 3 »): it never changes height by itself under a finger; unfolded, it keeps
 * its height while its step stays (only the user's own « Pourquoi ? » measures it again).
 */
@Composable
fun TutoPanel(
    ui: TutoUi, canShow: Boolean, actions: TutoActions, modifier: Modifier = Modifier, map: List<TutoMapBlock> = emptyList(),
    mine: Boolean = false, minePlaying: Boolean = false, mineFirst: Boolean = false,
    reducedMotion: Boolean = com.pocketstudio.app.onboarding.animationsOff(androidx.compose.ui.platform.LocalContext.current),
    /** The style's kit offer ([TutoKitCard]), at the end of the step's words (round 3, n° 28: never in the panel's place). */
    kit: (@Composable () -> Unit)? = null,
    /** Round 3, n° 29: the step's words with each pad named as it shows on the screen ([tutoPadNames]). */
    names: (String) -> String = { it },
    /**
     * Tour 9 (D02): the most height the panel may take and leave the screen its least room ([TUTO_SCREEN_LEAST]).
     * Folded over it, the chapter's head gives its room (a progress bar and one row: the folded line, « Déplier »).
     */
    ceiling: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Infinity,
) {
    val step = ui.current
    val chapter = ui.chapters[ui.chapter]
    val done = step.id in ui.progress.done
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val beat = com.pocketstudio.app.guide.rememberGuideBeat(animated = !reducedMotion)
    val density = LocalDensity.current
    // The unfolded panel's height, taken once for its step and then held (0: not taken yet).
    val end = ui.atEnd || ui.finished
    // Replay t6-ha19-cases-panneau (100 %, panel open on the kick step): the cells to touch on the grid shown keep
    // their two rows above the panel; the words scroll (« Lire la suite ») in a lower room (the height taken again).
    // The same for a step on the track's sound (knobs named) with « Régler le son de la piste ↓ » on the screen (H5, H7).
    // The hits to cross out too (a gap step: « → <pad> » and its crosses were under the panel in a 4-bar Montée).
    // Control seat Da-10 (ch. 3, Japanese): in that lower room « Lire la suite » moved a long instruction by a line in
    // a window of two and a half: no effect to the eye. There, one touch on it gives the words their usual room back
    // (the whole instruction, as before the lower room), for this step, until the panel is folded.
    var readAll by remember(step.id, ui.open, ui.progress.level) { mutableStateOf(false) }
    // The words' whole height, measured while « Lire la suite » unfolds them (0: not yet).
    var wholeWords by remember(step.id, ui.open, ui.progress.level, readAll) { mutableIntStateOf(0) }
    val lowRoom = (TutoHints.gridInSight && (TutoHints.cells?.second?.isNotEmpty() == true || TutoHints.erase?.values?.any { it.isNotEmpty() } == true)) ||
        (TutoHints.soundButtonInSight && TutoHints.knobs.isNotEmpty())
    // Tour 9 F012 (t9-40, t9-52, t9-68): « Bravo » in the words' low room over the cells was cut under « Lire la suite »:
    // the step done has no cells left to touch, its banner takes its whole height (the panel's height taken again).
    val cellsToTouch = lowRoom && !readAll && !ui.bravo
    // Tour 9 (D02; seat t9-65, 360 dp, 200 %, Japanese): folded, the chapter's title and line (three lines each) and two
    // lines of words made 262 dp: the rhythm grid kept 23 dp, no cell in sight. The panel's whole height, measured with its
    // head; folded over the ceiling it goes compact, for the step, until it is unfolded.
    val ceilingPx = if (ceiling == androidx.compose.ui.unit.Dp.Infinity) Int.MAX_VALUE else with(density) { ceiling.roundToPx() }
    var fullHeight by remember(step.id, ui.open, ui.progress.level, end, ui.demo != null, done) { mutableIntStateOf(0) }
    val compact = !ui.open && fullHeight > ceilingPx
    // Tour 9 (D14; seat t9-44, Dutch at 200 % on 392 dp: the step's instruction is 378 dp high, the open panel had 517 dp
    // with its head and four rows of buttons; unfolded with no bound, it went past the screen's top and its buttons out
    // of it). Unfolded by « Lire la suite » (upright), the panel reads: it may go up to the status bar, over the app's top
    // bar, the time to read (« Réduire » or another step gives it back); its buttons are measured before the words, never
    // cut. Where the whole instruction still does not fit, the panel lets go, one at a time ([shed]): its head (the
    // chapter's title, « Chapitres », the fold), then « Pourquoi ? » and the kit offer, then the row of the example and
    // « Montre-moi »; all come back with « Réduire ».
    var bottomPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBars.getTop(density)
    val reading = readAll && ui.open && !end && ceiling != androidx.compose.ui.unit.Dp.Infinity && bottomPx > statusTop + 1f
    val reach = if (reading) with(density) { (bottomPx - statusTop).toDp() } else androidx.compose.ui.unit.Dp.Infinity
    var shed by remember(step.id, ui.open, ui.progress.level, readAll) { mutableIntStateOf(0) }
    // Tour 9 F009: « Pourquoi ? »'s reason opened where the panel is short (200 %, a phone on its side), the panel lets
    // go the same way (its head, the kit offer, then the row of the example) until the reason has two lines under its
    // button; all come back when it is closed (« ▾ Pourquoi ? » stays).
    var whyShed by remember(step.id, ui.open, ui.progress.level) { mutableIntStateOf(0) }
    val shedAt = if (reading) shed else whyShed
    var held by remember(step.id, ui.open, ui.progress.level, end, cellsToTouch, readAll, shedAt) { mutableIntStateOf(0) }
    // Testers (lot « tuto 4 »): a height taken while « Montre-moi »'s one-line caption showed cut the step's
    // words once it ended; the end's card takes the room it needs.
    val holding = ui.open && held > 0 && !end
    // How many times the height held was taken again for the words' least room ([WordsRoom]).
    var regrown by remember(step.id, ui.open, ui.progress.level, end, cellsToTouch, readAll, shedAt) { mutableIntStateOf(0) }
    // Tour 6 (N31): « Lire la suite » lay over « ▸ Pourquoi ? » (a touch there read on). « Pourquoi ? » and « Lire la
    // suite » are under the scrolling words, never in them: neither under the other, « Pourquoi ? » never cut by the
    // words' room (Arabic at 130 %), the panel's height as before. The words' scroll is the step's own (the step
    // before's left its room in the height held for this one).
    // Tour 7 P6/P7 bloc 0b: the examples folded (below), the words are measured again from the start: « Lire la suite »'s
    // room, taken while the examples overflowed, is not kept under words that now fit.
    var examplesHeight by remember(step.id, ui.open, ui.progress.level) { mutableIntStateOf(0) }
    var examplesFolded by remember(step.id, ui.open, ui.progress.level) { mutableStateOf(false) }
    val words = remember(step.id, ui.open, examplesFolded) { androidx.compose.foundation.ScrollState(0) }
    // Tour 7 D3 (seat lotA/a, ohat_do above the grid): the room's bottom fell inside a line of the words (half a line seen
    // above « Pourquoi ? »), and inside the kit offer's buttons. The step's lines and the offer's buttons (top, bottom), in
    // the scrolled content's own pixels: the room ends on a whole line, above a whole button.
    var rows by remember(step.id, ui.open) { mutableStateOf<Map<String, List<Pair<Float, Float>>>>(emptyMap()) }
    val lines = rows.values.flatten().sortedBy { it.first }
    val content = remember(step.id, ui.open) { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
    val laid = remember(step.id, ui.open) { arrayOfNulls<androidx.compose.ui.text.TextLayoutResult>(1) }
    val put: (String, androidx.compose.ui.layout.LayoutCoordinates, (Float) -> List<Pair<Float, Float>>) -> Unit = { key, c, of ->
        content[0]?.takeIf { it.isAttached && c.isAttached }?.localPositionOf(c, Offset.Zero)?.y?.let { base ->
            val now = of(base)
            if (rows[key] != now) rows = rows + (key to now)
        }
    }
    // A new scroll's maxValue is Int.MAX_VALUE until its first measure: taken as « they scroll », « Lire la suite »'s room
    // went into the height held for the step, empty under words that fit. Only once they are measured longer than their room.
    val scrolls = words.maxValue in 1 until Int.MAX_VALUE
    // Tour 7 P6/P7 bloc 0b (docs/audits/ultra/tour-7-plan-p6-p7.md § 1.4): the blocks fold in [TutoReadability.layout]'s
    // order before the words are cut. The kit offer and « Pourquoi ? » are pinned under the words and stay (cut anyway,
    // folding them spares nothing; the kit offer is only there when the step needs it, « ▸ Pourquoi ? » is already its
    // folded form). The examples (Mode simple, under the words) fold first: words that scroll fold them, for the step.
    val lineHeight = wordsLineHeight()
    val gapPx = with(density) { 6.dp.roundToPx() }
    var moreHeight by remember { mutableIntStateOf(0) }
    if (scrolls && !examplesFolded && examplesHeight > 0) SideEffect {
        val more = if (moreHeight > 0) moreHeight else with(density) { TutoReadability.MORE_DP.dp.roundToPx() }
        val room = words.viewportSize + more + gapPx
        val rest = (words.maxValue + words.viewportSize - examplesHeight - gapPx).coerceAtLeast(0)
        val restLines = maxOf(1, kotlin.math.ceil(rest / lineHeight).toInt())
        val keep = TutoReadability.layout(room, rest.toFloat() / restLines, restLines, 0, 0, examplesHeight + gapPx, more)
        if (!keep.examples) examplesFolded = true
    }
    // Tour 9 F009: kept at the step's validation (« Bravo »): a reason opened stays open until the user closes it.
    val why = (step.why ?: chapter.why.takeIf { it.isNotEmpty() }).takeIf { ui.demo == null }
    var whyOpen by rememberSaveable(why, step.id) { mutableStateOf(false) }
    if (!whyOpen && whyShed > 0) SideEffect { whyShed = 0 }
    // Opened from the words (a phone on its side), the reason goes pinned under them with its button: the words go back
    // to their start, the instruction is read first (it was scrolled away to show the reason).
    LaunchedEffect(whyOpen, why) { if (whyOpen && why != null) words.scrollTo(0) }
    Surface(
        color = panelColor(), shape = shape,
        // Tour 9 F003 (seats t9-11, t9-29, t9-41, t9-56): at 200 % the unfolded panel covered the whole work area (the
        // piano under it gone). Upright, it takes at most [PANEL_MAX_SHARE] of the screen's height; the words scroll in
        // what is left (their room is weighted below), the buttons stay whole.
        modifier = modifier.fillMaxWidth().then(panelCap(whyOpen && why != null)).border(2.dp, Neon.Cyan.copy(alpha = 0.6f), shape).guideEdge(beat, Neon.Cyan).testTag("tuto_panel")
            .onGloballyPositioned { val b = it.boundsInWindow().bottom; if (b != bottomPx) bottomPx = b },
    ) {
        Column(
            Modifier.then(if (reading) Modifier.heightIn(max = reach) else Modifier)
                .then(if (holding) Modifier.height(with(density) { held.toDp() }) else Modifier)
                .onSizeChanged {
                    if (!compact && it.height > 0) fullHeight = it.height
                    // Reading (D14), the panel takes the words' height as it comes: it is held again once « Réduire »d.
                    if (ui.open && held == 0 && it.height > 0 && ui.demo == null && !end && !reading) held = it.height
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val fold: @Composable () -> Unit = {
                IconButton(onClick = actions.toggle, modifier = Modifier.testTag("tuto_fold")) {
                    Icon(if (ui.open) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = if (ui.open) tr("Replier") else tr("Déplier"))
                }
            }
            val progress: @Composable (Modifier) -> Unit = { m ->
                LinearProgressIndicator(
                    progress = { ui.doneCount / ui.total.coerceAtLeast(1).toFloat() }, color = Neon.Acid, trackColor = Neon.SurfaceHigh,
                    modifier = m.height(6.dp).clip(RoundedCornerShape(3.dp)).testTag("tuto_progress"),
                )
            }
            if (compact && !ui.open) {
                // Compact and folded: the progress, then the folded line and « Déplier » in one row.
                progress(Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("tuto_compact")) {
                    Box(Modifier.weight(1f)) { FoldedLine(ui, done, actions, names) }
                    fold()
                }
                return@Column
            }
            if (shedAt < 1) Row(verticalAlignment = Alignment.CenterVertically) {
                // Tour 6 (N22 fallout): the school icon (22 dp and 8 dp, decorative) gave its room to « Chapitres »: the title
                // and the chapter's line keep the room they had beside the bare list icon.
                Column(Modifier.weight(1f).clickable(onClick = actions.toggle).testTag("tuto_toggle")) {
                    // The chapter's whole title (testers: « Le tempo et la ton… »), on two lines when it needs them.
                    // n° 38 (200 %): three lines at most, a little smaller rather than cut.
                    com.pocketstudio.ui.WholeWordsText(
                        tutoText(chapter.title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 3,
                        modifier = Modifier.testTag("tuto_chapter_title"),
                    )
                    // Tour 6 (N23): the chapter's « Bravo » shown, « Chapitre 7/15 · Fait » (« Étape 1/2 » said a step left).
                    Text(
                        // Tour 9 F019 (t9-10, t9-18): the end, no step counter (« Étape 3/3 » under a finished tutorial).
                        (if (end) (if (ui.finished) tr("Tuto fini : bravo !") else tr("Presque fini !"))
                        else if (ui.chapterDoneNext() != null) tr("Chapitre %1\$d/%2\$d · %3\$s", ui.chapter + 1, ui.chapters.size, tr("Fait"))
                        else tr("Chapitre %1\$d/%2\$d · Étape %3\$d/%4\$d", ui.chapter + 1, ui.chapters.size, ui.step + 1, chapter.steps.size)) + " · " +
                            if (ui.progress.level == TutoLevel.Pro) tr("Pro") else tr("Facile"),
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                    )
                }
                // Tour 6 (N22): a bare list icon was not read as the chapters (seat ja took the ← arrow): its name under it,
                // whole on one line (never cut), folded or not. At most [CHAPTERS_WIDTH] wide (the list icon's 48 dp and the
                // school icon's 30): a longer word (« Hoofdstukken », 130 %) a little smaller, never wider.
                Column(
                    Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp, max = CHAPTERS_WIDTH).clip(RoundedCornerShape(10.dp))
                        .clickable(onClickLabel = tr("Chapitres"), role = androidx.compose.ui.semantics.Role.Button, onClick = actions.chapters)
                        .padding(horizontal = 2.dp).testTag("tuto_chapters"),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(20.dp))
                    // Its words grow up to 130 % only: at 200 % the panel's head stays a finger high (the cells above it in sight).
                    val label = MaterialTheme.typography.labelSmall
                    val cap = minOf(1f, 1.3f / LocalDensity.current.fontScale.coerceAtLeast(0.1f))
                    val name = tr("Chapitres")
                    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
                    val room = with(LocalDensity.current) { (CHAPTERS_WIDTH - 4.dp).toPx() }
                    var fit = label.copy(fontSize = label.fontSize * cap, lineHeight = label.lineHeight * cap)
                    for (k in 0 until 6) {
                        val w = measurer.measure(name, fit, maxLines = 1, softWrap = false).size.width
                        if (w <= room) break
                        val by = room / w * 0.97f
                        fit = fit.copy(fontSize = fit.fontSize * by, lineHeight = fit.lineHeight * by)
                    }
                    Text(name, style = fit, maxLines = 1, softWrap = false)
                }
                fold()
            }
            if (shedAt < 1) progress(Modifier.fillMaxWidth())
            if (!ui.open) {
                FoldedLine(ui, done, actions, names, kitPending = kit != null)
                return@Column
            }
            // Testers (lot « tuto 4 »): the end said nothing (or sent back to chapter 5): « Bravo » when all is
            // done, else the steps left, each one touch away; never « Je le fais » after the end.
            if (end) {
                EndCard(ui, actions, names, mine, minePlaying, Modifier.weight(1f, fill = false))
                return@Column
            }
            // The step's words, or in their place « Bravo » or « Montre-moi »'s caption: never one more block.
            // At least a line and a half of room (the « Bravo » banner fits where a one-line step was).
            val most = if (chapter.map && map.isNotEmpty()) 290.dp else if (cellsToTouch) GRID_WORDS_MAX else 240.dp
            // Audit ultra (« le panneau tient »): words longer than the room scroll, and say so at the bottom.
            // The words take the room « Pourquoi ? » and « Lire la suite » leave them (measured first, no frame late).
            // A phone on its side leaves the words a line or two: « Pourquoi ? » goes back in them there (« Lire la suite »
            // whole under them, not squeezed); on a screen high enough it is pinned under them.
            val pinned = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp >= WHY_PINNED_SCREEN
            // Fresh review of « fable-repar » (A): at 200 % the kit offer, « Pourquoi ? » and « Lire la suite » took the
            // whole room and the step's instruction had no height left. The words keep a line and a half at least: the
            // room grows by what is missing, and a height held too low for it is taken again (twice at most).
            // Only where « Pourquoi ? » is pinned: a phone on its side has its panel as before (its buttons were cut).
            // Tour 9 F009: « Pourquoi ? » opened is pinned too, its reason right under it (on a phone on its side as well).
            val whyPinned = pinned || (whyOpen && why != null)
            val lineAndHalf = if (whyPinned) with(density) { (MaterialTheme.typography.bodyLarge.lineHeight * 1.5f).roundToPx() } else 0
            // Tour 7 P6/P7 bloc 0b (CI 353): « Lire la suite » over the cells to touch gives back the whole instruction, the
            // kit offer pinned under it or not (its card came once the packs' index was read and took the words' room):
            // there, the words' least room is their whole height, as far as the screen lets it.
            // Kept once measured (words that then fit scroll no more: their whole height is not read again).
            // Tour 9 (D14; seats t9-05, t9-44, t9-68, t9-69: at 200 % a touch moved the words by a line or three, or by
            // nothing to the eye): on every step, « Lire la suite » unfolds the whole instruction the same way.
            val whole = if (readAll && words.maxValue in 1 until Int.MAX_VALUE) words.maxValue + words.viewportSize else 0
            if (whole > wholeWords) SideEffect { wholeWords = maxOf(wholeWords, whole) }
            val least = maxOf(lineAndHalf, if (readAll) wholeWords else 0)
            WordsRoom(
                if (holding) Modifier.weight(1f) else Modifier.weight(1f, fill = false), holding, most, least,
                mapRoom = chapter.map && map.isNotEmpty(),
                starved = { if (regrown < 2) { regrown++; held = 0 } },
                short = if (reading) { { if (shed == shedAt && shed < 3) shed++ } } else null,
                // « Pourquoi ? » opened leaves the instruction a line at least (its first one, in sight over the reason).
                whyLeaves = if (whyOpen) with(density) { MaterialTheme.typography.bodyLarge.lineHeight.roundToPx() } else 0,
                whyMin = with(density) { MaterialTheme.typography.labelLarge.lineHeight.roundToPx() + (MaterialTheme.typography.bodyMedium.lineHeight * 2).roundToPx() },
                whyShort = if (!reading && whyOpen && why != null) { { if (whyShed < 3) whyShed++ } } else null,
            ) {
            Box {
            Column(
                Modifier.wholeLines(words) { rows.values.flatten() }.verticalScroll(words)
                    .onGloballyPositioned { content[0] = it },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when {
                    ui.demo != null -> DemoCaption(ui.demo)
                    ui.bravo -> {
                        BravoBanner(mine, ui.made, nextChapter = ui.chapterDoneNext(), missing = ui.chapterLeft(), already = alreadyWords(ui))
                    }
                    else -> {
                        ui.made?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.Acid, modifier = Modifier.testTag("tuto_made")) }
                        // Tour 6 (N20): reached done, never touched: said, not left to look like a mistake.
                        val alreadySaid = alreadyWords(ui)
                        alreadySaid?.let { words ->
                            Column(Modifier.testTag("tuto_already")) {
                                Text(tr("Déjà fait !"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(words, style = MaterialTheme.typography.bodySmall, color = Neon.TextPrimary)
                            }
                        }
                        // Control seat Da-5 (tour 6, chapter 12): « déjà fait » under « À toi : … efface » : a step the song held
                        // already asks for nothing, its words are not shown.
                        if (alreadySaid == null) {
                            Row(verticalAlignment = Alignment.Top) {
                                if (done) Icon(Icons.Filled.CheckCircle, contentDescription = tr("Fait"), tint = Neon.Acid, modifier = Modifier.padding(end = 6.dp).size(20.dp).testTag("tuto_step_done"))
                                Text(momentWords(ui) ?: tutoWords(shownText(ui, step.id, kit != null), names), style = MaterialTheme.typography.bodyLarge, onTextLayout = { laid[0] = it },
                                    modifier = Modifier.testTag("tuto_text").onGloballyPositioned { c ->
                                        val t = laid[0] ?: return@onGloballyPositioned
                                        put("text", c) { base -> (0 until t.lineCount).map { base + t.getLineTop(it) to base + t.getLineBottom(it) } }
                                    })
                            }
                        }
                        ui.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("tuto_note")) }
                        // n° 37: in the Mode simple, each word of the trade of the step (kick, Drop, phrygienne…) said in plain words under it.
                        if (alreadySaid == null && momentWords(ui) == null && !examplesFolded) com.pocketstudio.app.library.PlainWordsLines(
                            shownText(ui, step.id, kit != null), modifier = Modifier.onSizeChanged { if (it.height > 0) examplesHeight = it.height })
                        // Closed, on a phone on its side: « ▸ Pourquoi ? » in the words (opened, it is pinned under them).
                        if (why != null && !whyPinned) Why(why, false) { whyOpen = true; held = 0 }
                        if (chapter.map && map.isNotEmpty()) SongMap(map, focus = mapFocus(step.listen))
                        if (ui.failed) Text(tr("L'exemple ne s'entend pas : monte le volume du téléphone, puis touche encore « Écouter l'exemple »."), color = Neon.Amber,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("tuto_failed"))
                    }
                }
            }
            // Control of « fable-repar » (seat c, V4; Korean at 130 %, Japanese unfolded): the words stopped at the end of
            // a line, in the middle of a sentence, with nothing to say they go on. « … » at the end of the last line in sight
            // while more is under it (silent: « Lire la suite » is what TalkBack reads).
            if (scrolls && words.canScrollForward) Text(
                "…", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan, maxLines = 1,
                modifier = Modifier.align(Alignment.BottomEnd).background(panelColor()).padding(start = 4.dp).clearAndSetSemantics { testTag = "tuto_text_cut" },
            )
            }
            // Tour 6 (T7): not on the step that says the kit is ready (« Tu n'as rien à régler » over « Télécharger le kit »).
            // Tour 7 D3 end (seats 1a #1, 2a #2, 2d #3): at the end of the words, its buttons came only after « Lire la suite ».
            // Pinned under the words, one row (« Télécharger le kit House (4 Mo) », « Plus tard »): always whole in sight.
            // It takes its room from the words above it, never from the buttons below (round 3, n° 28: an offer coming
            // late never moves the panel's buttons under a finger).
            if (step.id != READY_STEP && kit != null && ui.demo == null && !ui.bravo && shedAt < 2) {
                // Tour 9 F016: one child, known to [WordsRoom] (on a map step it never takes the map's room).
                Box(Modifier.layoutId(KIT_ROOM)) { CompositionLocalProvider(LocalKitPinned provides true) { kit() } }
            }
            // « Pourquoi ? » opened by the user: the panel takes its new height (the user's own touch, on the panel).
            // Tour 9 F009: its reason opens right under it (not up in the words), and stays at « Bravo » until closed.
            if (why != null && whyPinned && (shedAt < 2 || whyOpen) && (!ui.bravo || whyOpen)) Why(why, whyOpen, Modifier.layoutId(WHY_ROOM)) { whyOpen = !whyOpen; held = 0 }
            // Its room kept while the words scroll at all (shown or not, nothing moves when the end is reached).
            // Reading (D14), only once the panel has let all it could go and the words still do not fit: they page on.
            if (scrolls && (!reading || shedAt >= 3)) MoreWords(words.canScrollForward, Modifier.onSizeChanged { moreHeight = it.height }) {
                // The first touch unfolds the whole instruction; where the screen is too short for it, the next ones page on.
                if (!readAll) readAll = true
                else {
                    // A page on, from the first line not wholly seen (never half a line at the top).
                    val next = lines.firstOrNull { it.second > words.value + words.viewportSize + 0.5f }?.first?.toInt()
                    val to = next?.takeIf { it > words.value } ?: (words.value + words.viewportSize * 3 / 4)
                    words.animateScrollTo(minOf(words.maxValue, to))
                }
            }
            // Control « tuto2 »: unfolded by « Lire la suite », the panel covers the cells to touch: « Réduire » gives them back.
            if (readAll) LessWords { readAll = false }
            }
            ActionBar(ui, canShow, actions, mine, minePlaying, mineFirst, few = shedAt >= 3)
        }
    }
}

/**
 * The room of the step's words (the first child of [content]) over what is pinned under them (the others: the kit offer,
 * « Pourquoi ? », « Lire la suite »), 6 dp apart. The pinned ones are measured first and whole; the words take what is
 * left of [most] (of the height given, when [holding]). [least] px (0: none) is the words' least room: the room grows
 * past [most] by what is missing, as far as the height given lets it; when [holding], [starved] is told (the height
 * held is too low).
 * Tour 9 F016 (t9-16, t9-22): on a step of the song's map ([mapRoom]) the kit offer ([KIT_ROOM]) adds its own height to
 * [most]: the map of parts, in the words, is no longer cut by the banner (as far as the height given lets it).
 */
@Composable
private fun WordsRoom(
    modifier: Modifier, holding: Boolean, most: androidx.compose.ui.unit.Dp, least: Int, mapRoom: Boolean = false, starved: () -> Unit,
    short: (() -> Unit)? = null, whyLeaves: Int = 0, whyMin: Int = 0, whyShort: (() -> Unit)? = null, content: @Composable () -> Unit,
) {
    androidx.compose.ui.layout.Layout(content, modifier) { ms, c ->
        val gap = 6.dp.roundToPx()
        val wide = androidx.compose.ui.unit.Constraints(maxWidth = c.maxWidth)
        // Each pinned one within what the height given leaves (as a column would), never past it.
        // Tour 9 F009: « Pourquoi ? » ([WHY_ROOM]) is measured last, in what the others and the words' least room leave
        // (its reason scrolls under its button there): opened, it never takes the instruction's line and a half.
        var fixed = 0
        val bounded = c.hasBoundedHeight
        val rest = ms.drop(1)
        val sized = arrayOfNulls<androidx.compose.ui.layout.Placeable>(rest.size)
        val order = rest.indices.sortedBy { if (rest[it].layoutId == WHY_ROOM) 1 else 0 }
        for (i in order) {
            val left = if (bounded) maxOf(c.maxHeight - fixed - (if (rest[i].layoutId == WHY_ROOM && least > 0) minOf(least, whyLeaves) else 0), 0) else androidx.compose.ui.unit.Constraints.Infinity
            sized[i] = rest[i].measure(wide.copy(maxHeight = left)).also { fixed += it.height + gap }
            if (rest[i].layoutId == WHY_ROOM && bounded && left < whyMin) whyShort?.invoke()
        }
        val pinned = sized.map { it!! }
        val kitRoom = if (mapRoom) ms.drop(1).zip(pinned).filter { it.first.layoutId == KIT_ROOM }.sumOf { it.second.height + gap } else 0
        val cap = if (holding && c.hasBoundedHeight) c.maxHeight else minOf(c.maxHeight, maxOf(most.roundToPx() + kitRoom, if (least > 0) fixed + least else 0))
        if (holding && least > 0 && cap - fixed < least) starved()
        // Not held, the most the panel may take does not hold the words' least room: [short] is told.
        if (!holding && short != null && least > 0 && c.hasBoundedHeight && c.maxHeight - fixed < least) short()
        val room = maxOf(cap - fixed, 0)
        val words = ms.firstOrNull()?.measure(wide.copy(maxHeight = room))
        val height = ((words?.height ?: 0) + fixed).coerceIn(if (holding) c.minHeight else minOf(52.dp.roundToPx(), cap), maxOf(cap, c.minHeight))
        val width = (listOfNotNull(words) + pinned).maxOfOrNull { it.width }?.coerceIn(c.minWidth, c.maxWidth) ?: c.minWidth
        layout(width, height) {
            var y = 0
            words?.let { it.placeRelative(0, 0); y = it.height + gap }
            for (q in pinned) { q.placeRelative(0, y); y += q.height + gap }
        }
    }
}

/** Tour 9 F016: the kit offer's place among [WordsRoom]'s pinned children. */
private const val KIT_ROOM = "kit"

/** Tour 9 F009: « Pourquoi ? »'s place among [WordsRoom]'s pinned children (measured last). */
private const val WHY_ROOM = "why"

/** Tour 7 D3 end: the kit offer pinned under the panel's words ([TutoKitCard] in one row); false outside the panel. */
private val LocalKitPinned = androidx.compose.runtime.staticCompositionLocalOf { false }

/**
 * Tour 7 D3: the words' room ends on a whole line. When the room's bottom ([words]' value plus its most) falls inside
 * one of [lines] (top, bottom in the scrolled content), the room stops at that line's top; the line is read on.
 */
private fun Modifier.wholeLines(words: androidx.compose.foundation.ScrollState, lines: () -> List<Pair<Float, Float>>): Modifier =
    this.layout { m, c ->
        var most = c.maxHeight
        if (c.hasBoundedHeight) {
            val s = words.value.toFloat()
            val bottom = s + most
            val cut = lines().firstOrNull { it.first < bottom - 0.5f && it.second > bottom + 0.5f }
            val room = cut?.let { (it.first - s).toInt() }
            if (room != null && room in maxOf(1, c.minHeight)..most) most = room
        }
        val p = m.measure(c.copy(maxHeight = most))
        layout(p.width, p.height) { p.place(0, 0) }
    }

/** The panel's way to the chapters, at its widest: the bare list icon's 48 dp and the school icon's 22 + 8 it replaced. */
private val CHAPTERS_WIDTH = 78.dp

/** The screen's height (dp) from which « Pourquoi ? » is pinned under the words (a phone upright, a tablet; not on its side). */
private const val WHY_PINNED_SCREEN = 480

/** The courses' first step, « ton kit est déjà celui du style… Tu n'as rien à régler »: no kit offer under it. */
private const val READY_STEP = "ready"

/**
 * Tour 9, F061: the current step's words as shown. The ready step does not speak of the style's kit as on the
 * pads while its card offers it ([kitPending]): « le kit du disco est chargé » was said over the studio's sounds.
 */
private fun shownText(ui: TutoUi, stepId: String, kitPending: Boolean): String {
    val text = ui.current.textFor(ui.progress.level)
    return if (stepId == READY_STEP) TutoKit.readyText(text, kitPending) else text
}

/**
 * Under the step's words when they scroll: « Lire la suite », a touch scrolls down ([shown]; at their end the same room
 * stays, empty, so that nothing moves).
 */
@Composable
private fun MoreWords(shown: Boolean, modifier: Modifier = Modifier, more: suspend () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Row(
        modifier.fillMaxWidth()
            .then(if (shown) Modifier.clickable(onClickLabel = tr("Lire la suite"), role = androidx.compose.ui.semantics.Role.Button) { scope.launch { more() } }.testTag("tuto_text_more")
            // Its room kept, empty and silent: nothing left for TalkBack to read (fresh review, moyen).
            else Modifier.alpha(0f).clearAndSetSemantics {})
            .heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr("Lire la suite"), style = MaterialTheme.typography.labelMedium, color = Neon.Cyan, fontWeight = FontWeight.Bold)
        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
    }
}

/** Under the step's words unfolded over the cells to touch: « Réduire », a touch gives the words their low room back. */
@Composable
private fun LessWords(less: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = tr("Réduire"), role = androidx.compose.ui.semantics.Role.Button, onClick = less)
            .testTag("tuto_text_less").heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tr("Réduire"), style = MaterialTheme.typography.labelMedium, color = Neon.Cyan, fontWeight = FontWeight.Bold)
        Icon(Icons.Filled.ExpandLess, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
    }
}

/** « Bravo, c'est réussi ! » in the step's text's place ([made]: what the step's button made, said instead of the hint). */
@Composable
private fun BravoBanner(mine: Boolean, made: String? = null, nextChapter: String? = null, missing: Int? = null, already: String? = null) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp).testTag("tuto_bravo"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 10.dp)) {
            // n° 34 (seat A b): a chapter's last step went straight on to the next chapter; now « Bravo » for the chapter,
            // its music to hear, then the next chapter by its name.
            if (nextChapter != null) {
                Text(tr("Bravo, chapitre fini !"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("tuto_chapter_done"))
                already?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextPrimary, modifier = Modifier.testTag("tuto_already")) }
                Text(
                    made ?: if (mine) tr("Écoute ton morceau tel qu'il est maintenant, puis passe au chapitre suivant : « %s ».", nextChapter)
                    else tr("Chapitre suivant : « %s ».", nextChapter),
                    style = MaterialTheme.typography.bodySmall, color = Neon.TextPrimary, modifier = if (made != null) Modifier.testTag("tuto_made") else Modifier,
                )
                return@Column
            }
            // N20: ticked with no gesture (the project held it): « Déjà fait ! » and why, not « Bravo » for a touch never made.
            Text(if (already != null) tr("Déjà fait !") else tr("Bravo, c'est réussi !"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            already?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextPrimary, modifier = Modifier.testTag("tuto_already")) }
            Text(
                made ?: if (mine) tr("Écoute ton résultat, puis touche « Étape suivante » quand tu veux.") else tr("Touche « Étape suivante » quand tu veux."),
                style = MaterialTheme.typography.bodySmall, color = Neon.TextPrimary, modifier = if (made != null) Modifier.testTag("tuto_made") else Modifier,
            )
            // n° 30 (round 3): the chapter's last step done, an earlier one not: said, not « chapitre fini ».
            if (missing != null) Text(
                tr("Ce chapitre n'est pas fini : il reste l'étape %d. Touche « Chapitres » pour y revenir.", missing),
                style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("tuto_chapter_left"),
            )
        }
    }
}

/** N20: why the step shown is done already ([TutoUi.already]), null when it is not said. */
private fun alreadyWords(ui: TutoUi): String? {
    if (!ui.already || ui.current.id !in ui.progress.done) return null
    return if (ui.current.manual || ui.current.listening) tr("Tu as déjà fait cette étape.")
    else tr("Ton morceau avait déjà ce que demande cette étape : rien à toucher.")
}

/** The next chapter's title when the step shown is its chapter's last one and it is done (null: not a chapter's end). */
@Composable
private fun TutoUi.chapterDoneNext(): String? {
    val c = chapters[chapter]
    if (current.id !in progress.done || chapter >= chapters.lastIndex || finished || atEnd) return null
    // Tour 6 (N23): its « Bravo » on a step before the last one, the others done: the chapter's end all the same.
    if (step != c.steps.lastIndex && !chapterEndEarly) return null
    // Audit ultra round 3, n° 30 (seats A c, E b): « Bravo, chapitre fini ! » while the chapters' list said 2/3 (the
    // example left before it was heard): the chapter is finished only when each of its steps is.
    if (chapterLeft() != null) return null
    return tutoText(chapters[chapter + 1].title)
}

/** The first step of the shown chapter still to do (its number from 1), the last one shown done (null: none). */
internal fun TutoUi.chapterLeft(): Int? {
    val c = chapters[chapter]
    if (step != c.steps.lastIndex || current.id !in progress.done) return null
    val leftIds = left.map { it.id }.toSet()
    return c.steps.indexOfFirst { it.id in leftIds }.takeIf { it >= 0 }?.plus(1)
}

/** « Montre-moi »'s caption: it shows, then « à toi maintenant ». */
@Composable
private fun DemoCaption(phase: TutoDemoPhase) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Amber.copy(alpha = 0.16f)).border(1.dp, Neon.Amber, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp).testTag("tuto_demo_caption"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Neon.Amber, modifier = Modifier.size(20.dp))
        Text(
            demoWords(phase), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// Testers (lot « tuto 4 »): « Voilà ce qu'il fallait faire — à toi ma… » was cut on the folded line.
// Audit ultra round 1, n° 34 (seat B d): « À toi maintenant » was said while the shown value (174 BPM) was still set;
// now said once it is put back as it was, « Je remets comme avant » meanwhile.
private fun demoWords(phase: TutoDemoPhase): String = when (phase) {
    TutoDemoPhase.SHOWING -> tr("Regarde, je le fais…")
    TutoDemoPhase.HOLDING -> tr("Voilà. Je remets comme avant…")
    TutoDemoPhase.FADING -> tr("Voilà ! À toi maintenant.")
}

/**
 * The folded panel's line, always [FOLDED_LINE] high: « Montre-moi »'s caption (« Arrêter » beside
 * it), a step done (« ✓ Réussi » and « Étape suivante → », one touch), else the step's words (a
 * touch unfolds them).
 */
@Composable
private fun FoldedLine(ui: TutoUi, done: Boolean, actions: TutoActions, names: (String) -> String = { it }, kitPending: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    val line = Modifier.fillMaxWidth().height(foldedLineHeight()).clip(shape)
    when {
        // Tour 9, F002 (seats t9-47, t9-60): « Voilà ! À toi maintenant. » and « Arrêter » were all the line said, the step's
        // button (« Créer la partie … ») unseen: once the demonstration hands over, its button is on the line.
        ui.demo == TutoDemoPhase.FADING && ui.need != null -> NeedLine(line, shape, ui.need, ui.progress.style) { actions.stopDemo(); actions.need() }
        ui.demo != null -> Row(
            line.background(Neon.Amber.copy(alpha = 0.16f)).border(1.dp, Neon.Amber, shape).padding(start = 10.dp).testTag("tuto_demo_caption"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = Neon.Amber, modifier = Modifier.size(20.dp))
            Text(demoWords(ui.demo), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp))
            TextButton(onClick = actions.stopDemo, modifier = Modifier.testTag("tuto_show")) { Text(tr("Arrêter"), maxLines = 1) }
        }
        ui.atEnd || ui.finished -> Row(
            line.background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, shape).clickable(onClick = actions.toggle).padding(horizontal = 10.dp).testTag("tuto_end_folded"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(20.dp))
            Text(
                if (ui.finished) tr("Tuto fini : bravo !") else trCount(ui.left.size, "Il te reste %d étape", "Il te reste %d étapes"),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp),
            )
        }
        // n° 34: a chapter's end folded: « Bravo » first, unfolded by a touch (its music, then the next chapter), never skipped.
        // Tour 9, F002 (seat t9-11): it said no « Chapitre suivant »; now beside « Bravo », one touch.
        done && ui.chapterDoneNext() != null -> Row(line.background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, shape).testTag("tuto_bravo"),
            verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).fillMaxHeight().clickable(onClickLabel = tr("Déplier"), onClick = actions.toggle).padding(start = 10.dp, end = 4.dp)
                    .testTag("tuto_chapter_end_folded"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(20.dp))
                Text(tr("Bravo, chapitre fini !"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
            }
            Row(
                Modifier.fillMaxHeight().clickable(onClick = actions.next).padding(horizontal = 10.dp).testTag("tuto_chapter_next_folded"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr("Chapitre suivant"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(max = 120.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(18.dp))
            }
        }
        done -> Box(line.background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, shape).testTag("tuto_bravo")) {
            Row(
                Modifier.fillMaxSize().clickable(onClick = actions.next).padding(horizontal = 10.dp).testTag("tuto_next_folded"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(20.dp))
                Text(if (ui.bravo) tr("Réussi") else tr("Fait"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    maxLines = 1, modifier = Modifier.padding(start = 6.dp))
                Box(Modifier.weight(1f))
                Text(tr("Étape suivante"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(18.dp))
            }
        }
        // Testers (lot « tuto 5 »): the step's next button (« Copier sur les mesures suivantes »…) on the folded line
        // itself, one touch; it was hidden until the panel was unfolded.
        ui.need != null -> NeedLine(line, shape, ui.need, ui.progress.style, actions.need)
        // Audit ultra round 1, n° 34 (seat A b, 200 %): « À toi : sur le pad « Clap », t… » said nothing; two lines of
        // the step, and « Lire » (a touch unfolds the rest).
        else -> Row(
            line.clickable(onClickLabel = tr("Lire la suite"), onClick = actions.toggle).testTag("tuto_folded_text"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tutoWords(shownText(ui, ui.current.id, kitPending), names), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Row(Modifier.padding(start = 6.dp).testTag("tuto_folded_more"), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Lire"), style = MaterialTheme.typography.labelMedium, color = Neon.Cyan, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(Icons.Filled.ExpandLess, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** The step's button on the folded line ([TutoUi.need]), one touch. */
@Composable
private fun NeedLine(line: Modifier, shape: androidx.compose.ui.graphics.Shape, need: TutoNeed, style: String, onClick: () -> Unit) {
    Row(
        line.background(Neon.Amber.copy(alpha = 0.14f)).border(1.dp, Neon.Amber, shape).clickable(onClick = onClick).padding(horizontal = 10.dp)
            .testTag("tuto_need_folded"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(needLabel(need, style), color = Neon.Amber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.Amber, modifier = Modifier.size(18.dp))
    }
}

/** The folded panel's line: a finger's height at least, whatever it says. */
private val FOLDED_LINE = 48.dp

/**
 * The folded line's height: [FOLDED_LINE], or two lines of the step's words when the font is big (n° 34: at
 * 200 % one line held « À toi : sur le pad « Clap », t… »). The same for every state of the line.
 */
@Composable
internal fun foldedLineHeight(): androidx.compose.ui.unit.Dp {
    // Two lines as the text lays them out (its line height and the font's own room above and below), and a margin.
    // Tour 7, P6: measured on a character of the language's own script (a CJK, Devanagari or Arabic line is higher).
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium
    val w = TutoReadability.witness(com.pocketstudio.model.i18n.I18n.lang)
    val two = with(LocalDensity.current) { measurer.measure("$w\n$w", style).size.height.toDp() } + 8.dp
    return maxOf(FOLDED_LINE, two)
}

/**
 * Tour 7, P6: the height (px) of one line of the words, what [TutoReadability.layout] counts in: the type's line height,
 * or more where a line of the language's own script ([TutoReadability.witness]) lays out higher (CJK, Devanagari, Arabic).
 */
@Composable
internal fun wordsLineHeight(): Float {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyLarge
    val w = TutoReadability.witness(com.pocketstudio.model.i18n.I18n.lang)
    val pitch = measurer.measure("$w\n$w", style).size.height - measurer.measure(w, style).size.height
    return maxOf(with(LocalDensity.current) { style.lineHeight.toPx() }, pitch.toFloat())
}

/** The part a structure step is about, lit on the map: « Écoute la Montée » → "Montée" (null: none). */
internal fun mapFocus(listen: String): String? = when {
    listen.startsWith("part:") -> listen.removePrefix("part:")
    listen.startsWith("vs:") -> listen.substringAfterLast(':')
    else -> null
}.takeIf { !it.isNullOrBlank() }

/** What the big button of a step does, and its words. */
private enum class Primary { NEXT, NEED, DO, MARK, MINE, LISTEN, NONE }

private fun primaryOf(ui: TutoUi, mineFirst: Boolean): Primary {
    val s = ui.current
    return when {
        // A step done that stays shown (an example heard to its end): on to the next one.
        s.id in ui.progress.done && !ui.finished -> Primary.NEXT
        ui.need != null -> Primary.NEED
        s.spot.isNotEmpty() -> Primary.DO
        s.manual -> Primary.MARK
        // Testers (lot « tuto 3 »): « Écoute toute ta batterie » plays the user's own drums first, then the example.
        s.listening && mineFirst -> Primary.MINE
        s.listen.isNotEmpty() -> Primary.LISTEN
        else -> Primary.NONE
    }
}

/**
 * The action bar (owner, 2026-10-28: « plus professionnelle, qu'on voie où toucher »): the step's
 * main action as a big filled button, the example and « Montre-moi » as two clear buttons with
 * their icon and their words, then « ← Précédent » and « Suivant → » with their words. Each row
 * shares its width (two lines of text at 130 %), the arrows turn round in a right-to-left language.
 */
@Composable
private fun ActionBar(ui: TutoUi, canShow: Boolean, actions: TutoActions, mine: Boolean, minePlaying: Boolean, mineFirst: Boolean, few: Boolean = false) {
    val step = ui.current
    val primary = primaryOf(ui, mineFirst)
    val busy = ui.preparing || ui.playing
    val big = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    val filled = ButtonDefaults.buttonColors(containerColor = Neon.Cyan, contentColor = Neon.OnAccent)
    val chapterEnd = ui.chapterDoneNext() != null
    when (primary) {
        // Tour 9 F004/F005 (seats t9-12, t9-74, t9-39, t9-40): the step done has one way on, in « Suivant → »'s own place
        // (bottom, at the end): the finger aimed at « Suivant » when the example ended now finds « Étape suivante → »,
        // not « Écouter l'exemple » come up in its place; never two « suivant » at once.
        Primary.NEXT -> Unit
        Primary.NEED -> {
            // Tour 7 (audit « Fable », R-C): a chapter opened from the list on a project that lacks what the chapters
            // before make. One touch lays it; the other way is to go and make it.
            val course = ui.need as? TutoNeed.Course
            if (course != null) {
                Text(tr("Il te manque ce que font les chapitres d'avant."), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("tuto_need_course"))
            }
            // Tour 7 (seat d): a step that works in a part the song has no more (« Montée » renamed): the button alone
            // said nothing of why; the step that makes the part needs no such word.
            // Tour 8 (seat d, V2): a part that is there, empty, is not said missing; its button fills it.
            val part = (ui.need as? TutoNeed.Part)?.takeUnless { step.check.startsWith("part:") || ui.partEmpty }
            if (part != null) {
                Text(tr("Il n'y a pas de partie « %s » dans ton morceau : cette étape se fait dedans.", Scene.shownLabel(part.label)),
                    style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("tuto_need_why"))
            }
            Button(onClick = actions.need, enabled = ui.demo == null, colors = filled, modifier = big.testTag("tuto_need")) {
                Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("  " + needLabel(ui.need!!, ui.progress.style), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            if (course != null) {
                TextButton(onClick = { actions.goTo(course.chapter - 1, 0) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_need_goto")) {
                    Text(tr("Aller au chapitre %d", course.chapter), textAlign = TextAlign.Center)
                }
            }
        }
        Primary.DO -> Button(onClick = actions.doIt, enabled = ui.demo == null, colors = filled, modifier = big.testTag("tuto_do")) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + tr("Je le fais"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Primary.MARK -> Button(onClick = actions.markDone, colors = filled, modifier = big.testTag("tuto_mark")) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + if (step.spot.isEmpty()) tr("J'ai compris") else tr("C'est fait"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Primary.MINE -> Button(onClick = actions.playMine, colors = filled, modifier = big.testTag("tuto_play_mine")) {
            Icon(if (minePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + if (minePlaying) tr("Arrêter") else tr("Écouter ton résultat"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Primary.LISTEN -> Button(onClick = actions.listen, colors = filled, modifier = big.testTag("tuto_listen")) {
            ListenContent(ui, busy)
        }
        Primary.NONE -> Unit
    }
    // The secondary buttons' words (what [besideFits] measures), each with its button.
    val words = ArrayList<String>()
    // Tour 9 (F014, t9-20, German at 200 %): « Dein Ergebnis anhören » shrank, « Beispiel anhören » beside it did not;
    // side by side, their words share one size (the smallest any of them needs).
    val shared = remember(step.id, primary, mine, chapterEnd, canShow, LocalDensity.current, androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp) { SharedLabelSize() }
    // [few]: the panel reads a long instruction (Tour 9, D14): the example's and « Montre-moi »'s row lets it the room.
    val secondaries = if (few) emptyList() else buildList<@Composable RowScope.() -> Unit> {
        // A step done: the user's own result first, then the example to compare.
        if (primary == Primary.NEXT && mine && (!step.listening || mineFirst || chapterEnd)) {
            val label = if (minePlaying) tr("Arrêter") else if (chapterEnd) tr("Écouter mon morceau") else tr("Écouter ton résultat")
            words += label
            add {
                OutlinedButton(onClick = actions.playMine, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_play_mine")) {
                    Icon(if (minePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    SecondaryLabel(" $label", shared, 0)
                }
            }
        }
        if (step.listen.isNotEmpty() && primary != Primary.LISTEN) {
            words += if (busy) tr("Arrêter l'écoute") else tr("Écouter l'exemple")
            add {
                OutlinedButton(onClick = actions.listen, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_listen")) { ListenContent(ui, busy, shared) }
            }
        }
        // F005: « C'est fait » on a step already done said nothing it does.
        if (step.manual && primary != Primary.MARK && primary != Primary.NEXT) {
            words += tr("C'est fait")
            add {
                OutlinedButton(onClick = actions.markDone, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_mark")) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    SecondaryLabel(" " + tr("C'est fait"), shared, 2)
                }
            }
        } else if (canShow && primary != Primary.NEXT) {
            val showing = ui.demo != null
            words += if (showing) tr("Arrêter") else tr("Montre-moi")
            add {
                OutlinedButton(
                    onClick = if (showing) actions.stopDemo else actions.showMe, contentPadding = SECONDARY_PADDING,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_show"),
                ) {
                    Icon(if (showing) Icons.Filled.Stop else Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    SecondaryLabel(" " + if (showing) tr("Arrêter") else tr("Montre-moi"), shared, 2)
                }
            }
        }
    }
    // Testers (lot « tuto 4 »): a step done had one more row of buttons than when it began, in the same held
    // height: its words went under the green button. Done, its other buttons share « Précédent »'s row…
    // Audit ultra round 2, n° 23 (seat A a, 200 %): … when their words fit there whole; else (a big font, a long
    // language) they get a row of their own, « ← Précédent » and « Suivant → » in words under them (it was « Éco… »
    // twice and an arrow).
    val nextWords = if (chapterEnd) tr("Chapitre suivant") else tr("Étape suivante")
    val beside = primary == Primary.NEXT && besideFits(words, nextWords)
    // The step done's one way on, filled, at the row's end where « Suivant → » stands while the step is on.
    val nextBig: @Composable (Modifier) -> Unit = { m ->
        Button(
            onClick = actions.next, colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Neon.OnAccent),
            contentPadding = SECONDARY_PADDING, modifier = m.heightIn(min = 48.dp).testTag("tuto_next_big"),
        ) {
            com.pocketstudio.ui.WholeWordsText("$nextWords ", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
    if (secondaries.isNotEmpty() && !beside) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        secondaries.forEach { it() }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (beside) 6.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
        val back = ui.chapter > 0 || ui.step > 0
        // Tour 9 (F013, t9-02): during « Montre-moi » the steps stay put: a finger aiming at « Arrêter » beside them
        // hit « Suivant » and got « Cette étape n'est pas encore faite ». They stay in their place, inert.
        val still = ui.demo == null
        if (beside && secondaries.isNotEmpty()) {
            // Only the arrow (its words for TalkBack): the row is the other buttons'.
            IconButton(onClick = actions.previous, enabled = back && still, modifier = Modifier.testTag("tuto_prev")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Précédent"), modifier = Modifier.size(20.dp))
            }
            secondaries.forEach { it() }
            // Tour 7 (D7) and tour 9 F004/F005: one way on only, the filled one, where « Suivant → » stood.
            // Testers (lot « tuto 5 »): the way on stays where « Suivant → » was (a quick touch there replayed the example).
            nextBig(Modifier.weight(1f, fill = false))
            return@Row
        }
        TextButton(onClick = actions.previous, enabled = back && still, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_prev")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" " + tr("Précédent"), maxLines = 1)
        }
        // Testers: two ways on (« Étape suivante » and « Suivant → ») puzzled them; the step done has only the filled one,
        // in « Suivant → »'s place (n° 23, F004).
        if (primary == Primary.NEXT) Box(Modifier.weight(1f).padding(start = 8.dp), contentAlignment = Alignment.CenterEnd) { nextBig(Modifier) }
        else Box(Modifier.weight(1f))
        if (primary != Primary.NEXT) TextButton(onClick = actions.next, enabled = still, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_next")) {
            Text(tr("Suivant") + " ", maxLines = 1)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * The end of the tutorial, in the step's place: « Bravo » and what to do now when every step is
 * done; else the steps left (in Pro an example not listened to is not one), each one touch away.
 */
@Composable
private fun EndCard(ui: TutoUi, actions: TutoActions, names: (String) -> String = { it }, mine: Boolean = false, minePlaying: Boolean = false, modifier: Modifier = Modifier) {
    val left = ui.left
    // Tour 9 F007 (seats t9-12, t9-28…30): « … et 6 autres » was words to read, not a way to them: one touch lists them all.
    var all by remember(left.size) { mutableStateOf(false) }
    // Tour 9 F006 (seats t9-05, t9-12, t9-42, t9-63): « Revenir à l'étape » at the bottom of the scrolling list was cut at
    // the panel's edge (200 %, a long list): pinned under the list, whole, the list scrolling above it.
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Column(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()).testTag("tuto_end"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (left.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(28.dp))
                Text(tr("Bravo, ton morceau est fini ! Tu peux tout réécouter et continuer à le changer."), color = Neon.Acid, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp).testTag("tuto_finished"))
            }
            if (mine) PlayMineButton(actions, minePlaying)
            OutlinedButton(onClick = actions.restart, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_restart_finished")) {
                Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + restartTutoLabel(), maxLines = 1)
            }
            // Tour 6 (N13): grey words with no frame under two framed buttons said nothing touchable: framed like them.
            OutlinedButton(onClick = actions.quit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_end_close")) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + tr("Fermer le tuto"), maxLines = 1)
            }
            return@Column
        }
        Text(tr("Presque fini !"), color = Neon.Amber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        // n° 30 (round 3, seat A c): the last chapter's end had no way to hear the song made.
        if (mine) PlayMineButton(actions, minePlaying)
        Text(trCount(left.size, "Il te reste %d étape. Touche-la pour y aller :", "Il te reste %d étapes. Touche-en une pour y aller :"), style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("tuto_left_count"))
        left.take(if (all) left.size else END_LIST).forEach { s ->
            val (c, i) = ui.placeOf(s)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp)).background(Neon.SurfaceHigh)
                    .clickable { actions.goTo(c, i) }.padding(horizontal = 10.dp, vertical = 6.dp).testTag("tuto_left_${s.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Chapitre %1\$d/%2\$d · %3\$s", c + 1, ui.chapters.size, tutoText(ui.chapters[c].title)), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(tutoWords(s.textFor(ui.progress.level), names), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
            }
        }
        if (left.size > END_LIST && !all) TextButton(onClick = { all = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("tuto_end_more")) {
            Text(trCount(left.size - END_LIST, "… et %d autre (liste ≡)", "… et %d autres (liste ≡)"), style = MaterialTheme.typography.bodyMedium, color = Neon.Cyan)
        }
    }
        // N13: framed too (the end's card, one look for its buttons).
        if (ui.atEnd && left.isNotEmpty()) OutlinedButton(onClick = actions.leaveEnd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_end_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            com.pocketstudio.ui.WholeWordsText(" " + tr("Revenir à l'étape"), textAlign = TextAlign.Center)
        }
    }
}

/**
 * Whether « ← », the secondary buttons ([words]) and the way on ([nextWords]) fit on one row with each button's
 * longest word whole at the smallest size [com.pocketstudio.ui.WholeWordsText] would shrink it to.
 */
@Composable
private fun besideFits(words: List<String>, nextWords: String): Boolean {
    if (words.isEmpty()) return true
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val width = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    return remember(words, style, density, width) {
        // The words' floor: 72 % of their size, as far as they would shrink at 130 % (WholeWords).
        val floor = 0.72f * minOf(1f, 1.3f / density.fontScale.coerceAtLeast(0.1f))
        fun px(text: String) = measurer.measure(text, style).size.width.toFloat()
        with(density) {
            val buttons = words.sumOf { label ->
                val longest = label.split(' ', '\u00A0').filter { it.isNotBlank() }.maxOfOrNull { px(it) } ?: 0f
                // Its icon, a space, its padding and border.
                (longest * floor + (18.dp + 6.dp + 10.dp * 2 + 2.dp).toPx()).toDouble()
            }.toFloat()
            // The filled way on (F004): its longest word, its arrow, its padding.
            val next = (nextWords.split(' ', '\u00A0').filter { it.isNotBlank() }.maxOfOrNull { px(it) } ?: 0f) * floor + (18.dp + 6.dp + 10.dp * 2).toPx()
            // The panel's padding (14 dp each side), « ← » (48 dp), the gaps (6 dp).
            val room = (width - 28.dp - 48.dp - 6.dp * (words.size + 1)).toPx()
            buttons + next <= room
        }
    }
}

/** The steps the end lists by name (the others: the chapters' list). */
private const val END_LIST = 4

/** Tour 9 F003: the most of the screen's height the unfolded panel takes, upright. */
internal const val PANEL_MAX_SHARE = 0.6f

/**
 * Tour 9 F009: « Pourquoi ? »'s reason opened by the user (upright), the panel may take this much of the screen's height,
 * until it is closed: at 200 % the reason, its button and the buttons left the instruction no room under [PANEL_MAX_SHARE].
 */
internal const val PANEL_REASON_SHARE = 0.85f

/** The panel's height cap: [PANEL_MAX_SHARE] of an upright screen (on its side, the panel is as before). */
@Composable
private fun panelCap(reasonOpen: Boolean = false): Modifier {
    val conf = androidx.compose.ui.platform.LocalConfiguration.current
    return if (conf.screenHeightDp >= conf.screenWidthDp) Modifier.heightIn(max = (conf.screenHeightDp * if (reasonOpen) PANEL_REASON_SHARE else PANEL_MAX_SHARE).dp) else Modifier
}

/** The secondary buttons' inner room: their words on one line on a phone, two at 130 %. */
private val SECONDARY_PADDING = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)

/** « ▶ Écouter l'exemple », a wheel while it is prepared, « ■ Arrêter l'écoute » while it plays. */
@Composable
private fun ListenContent(ui: TutoUi, busy: Boolean, shared: SharedLabelSize? = null) {
    when {
        ui.preparing -> CircularProgressIndicator(Modifier.size(16.dp).semantics { contentDescription = tr("Préparation de l'écoute") }, strokeWidth = 2.dp)
        busy -> Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
        else -> Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
    }
    val label = " " + if (busy) tr("Arrêter l'écoute") else tr("Écouter l'exemple")
    if (shared != null) SecondaryLabel(label, shared, 1) else com.pocketstudio.ui.WholeWordsText(label, textAlign = TextAlign.Center)
}

/** The sizes the secondary buttons' words were shrunk to, by button (0 its result, 1 the example, 2 the third). */
private class SharedLabelSize {
    val fitted = androidx.compose.runtime.mutableStateMapOf<Int, Float>()
}

/**
 * A secondary button's words, as [com.pocketstudio.ui.WholeWordsText] shrinks them (no word cut, two lines), never
 * larger than its neighbours' ([shared]): each reports the size it fits at, all take the smallest. The floor stays
 * the one of the words' own size.
 */
@Composable
private fun SecondaryLabel(text: String, shared: SharedLabelSize, slot: Int) {
    val merged = androidx.compose.material3.LocalTextStyle.current
    // As WholeWordsText: past 130 % a line height in sp would stay as tall when the words shrink.
    val big = LocalDensity.current.fontScale > 1.3f
    val style = if (big && merged.lineHeight.isSp && merged.fontSize.isSp && merged.fontSize.value > 0f) {
        merged.copy(lineHeight = (merged.lineHeight.value / merged.fontSize.value).em)
    } else merged
    if (!style.fontSize.isSp) {
        com.pocketstudio.ui.WholeWordsText(text, textAlign = TextAlign.Center)
        return
    }
    val own = style.fontSize.value
    val top = minOf(own, shared.fitted.values.minOrNull() ?: own)
    // A new ceiling is a new text node: the auto-size kept its first measure while the constraints stayed the same.
    androidx.compose.runtime.key(top) {
        Text(
            text, textAlign = TextAlign.Center, style = style, maxLines = 2, overflow = TextOverflow.Ellipsis,
            autoSize = SharedFit(top, MIN_SCALE * own) { size -> if (shared.fitted[slot] != size) shared.fitted[slot] = size },
        )
    }
}

/**
 * As [com.pocketstudio.ui.WholeWords] from [top] down by half points (no word cut, nothing past the lines), and no
 * first line of blanks either (F014: « Montre-moi » under an empty line, the space before it alone above), never
 * under [floor] (as far as at 130 % past it); each size chosen told to [report].
 */
private class SharedFit(private val top: Float, private val floor: Float, private val report: (Float) -> Unit) :
    androidx.compose.foundation.text.TextAutoSize {
    override fun androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope.getFontSize(
        constraints: androidx.compose.ui.unit.Constraints,
        text: androidx.compose.ui.text.AnnotatedString,
    ): androidx.compose.ui.unit.TextUnit {
        val min = floor / (fontScale / 1.3f).coerceAtLeast(1f)
        var size = top
        while (size > min) {
            val layout = performLayout(constraints, text, size.sp)
            val blankFirst = layout.lineCount > 1 && text.text.substring(0, layout.getLineEnd(0)).isBlank()
            if (!layout.hasVisualOverflow && !com.pocketstudio.ui.splitsAWord(layout, text.text) && !blankFirst) break
            size -= 0.5f
        }
        val chosen = size.coerceAtLeast(min)
        report(chosen)
        return chosen.sp
    }

    override fun equals(other: Any?) = other is SharedFit && other.top == top && other.floor == floor
    override fun hashCode() = top.hashCode() * 31 + floor.hashCode()
}

/** WholeWordsText's floor: 72 % of the words' size. */
private const val MIN_SCALE = 0.72f

/** « Pourquoi ? »: its reason ([why], one or two sentences, in the step's words) folded until touched ([toggle]), right under it. */
@Composable
private fun Why(why: String, open: Boolean, modifier: Modifier = Modifier, toggle: () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = toggle).testTag("tuto_why")) {
            Text((if (open) "▾ " else "▸ ") + tr("Pourquoi ?"), style = MaterialTheme.typography.labelLarge, color = Neon.Cyan)
        }
        // Where the panel is short (200 %, a phone on its side), the reason scrolls in the room left under its button.
        if (open) Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).testTag("tuto_why_room")) {
            Text(tutoText(why), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("tuto_why_text"))
        }
    }
}

/** The colour of a part on the map (by its section's name; the Intro and the Fin in a quiet grey-blue, not the accent). */
@Composable
private fun partColor(label: String): Color = when {
    label.startsWith("Drop") || label.startsWith("Refrain") -> Neon.Magenta
    label.startsWith("Montée") || label.startsWith("Pré-refrain") -> Neon.Amber
    label.startsWith("Break") || label.startsWith("Pont") -> Color(0xFF5BB8F5)
    else -> Color(0xFF9AA4B2)
}

/** Tour 9 F011: a colour for each part not yet coloured by its kind (« Drop 2 » after « Drop », « Groove » after « Intro »). */
private val MORE_PART_COLOURS = listOf(
    Color(0xFF7CD992), Color(0xFFB98CFF), Color(0xFFFF8A3D), Color(0xFF3FD0C9), Color(0xFFF2E25B), Color(0xFFE05C9E), Color(0xFF8C6E4F), Color(0xFFEDEDED),
)

/**
 * Each distinct part of [labels] its own colour (Tour 9 F011: the Intro, the Groove and the Fin, or the Drop and the Drop 2,
 * had one colour in the legend): its kind's colour ([partColor]) when no part before has one close to it, else the first
 * of [MORE_PART_COLOURS] apart from all the colours given.
 */
@Composable
private fun partColors(labels: List<String>): Map<String, Color> {
    val out = LinkedHashMap<String, Color>()
    fun apart(a: Color, b: Color) = kotlin.math.abs(a.red - b.red) + kotlin.math.abs(a.green - b.green) + kotlin.math.abs(a.blue - b.blue) >= 0.3f
    for (label in labels.distinct()) {
        val own = partColor(label)
        out[label] = if (out.values.all { apart(it, own) }) own
            else MORE_PART_COLOURS.firstOrNull { c -> out.values.all { apart(it, c) } } ?: own
    }
    return out
}

/**
 * The song's map: one block per part, as wide as its bars (its name and bars in it when they fit,
 * never cut to « M » or « Br »), the energy curve above (how many of its bricks play), every part's
 * name under it. [focus]: the part the step is about, lit (the others dimmed); null: all alike.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SongMap(blocks: List<TutoMapBlock>, modifier: Modifier = Modifier, focus: String? = null) {
    val total = blocks.sumOf { it.bars }.coerceAtLeast(1)
    val curve = Neon.Acid
    fun lit(b: TutoMapBlock) = focus == null || b.label == focus
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    Column(modifier.fillMaxWidth().testTag("tuto_map"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(tr("Énergie"), style = MaterialTheme.typography.labelSmall, color = curve)
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val path = Path()
            var x = 0f
            blocks.forEachIndexed { i, b ->
                val w = size.width * b.bars / total
                val y = size.height * (1f - b.energy.coerceIn(0f, 1f) * 0.9f)
                if (i == 0) path.moveTo(0f, y) else path.lineTo(x, y)
                // The Montée climbs over its bars (lot « tuto 4 »).
                path.lineTo(x + w, size.height * (1f - b.energyEnd.coerceIn(0f, 1f) * 0.9f))
                x += w
            }
            drawPath(path, curve, style = Stroke(width = 3.dp.toPx()))
            drawLine(curve.copy(alpha = 0.25f), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        }
        // Tour 9 F011 (16 seats): every block shows its name and its bars whole, even at 200 % (a narrow block showed its
        // bars alone, or nothing). Each block is as wide as its bars at least, wider when its words need it; the blocks
        // go on to a next line, in the song's order, when they no longer fit on one.
        val colours = partColors(blocks.map { it.label })
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 2.dp
            val room = maxWidth
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                blocks.forEachIndexed { i, b ->
                    val c = colours.getValue(b.label)
                    val on = lit(b)
                    val share = ((room - gap * (blocks.size - 1)) * (b.bars.toFloat() / total)).coerceAtLeast(0.dp)
                    Column(
                        Modifier.widthIn(min = minOf(share, room), max = room).heightIn(min = 44.dp).clip(RoundedCornerShape(6.dp))
                            .background(c.copy(alpha = if (focus != null && on) 0.6f else if (on) 0.3f else 0.12f))
                            .border(if (focus != null && on) 2.5.dp else 1.dp, if (on) c else c.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .then(if (focus != null && on) Modifier.testTag("tuto_map_lit_$i") else Modifier)
                            .testTag("tuto_map_block_$i").padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        val style = MaterialTheme.typography.labelSmall
                        Text(Scene.shownLabel(b.label), style = style, textAlign = TextAlign.Center, color = if (on) Neon.TextPrimary else Neon.TextSecondary,
                            modifier = Modifier.testTag("tuto_map_name_$i"))
                        Text("${b.bars}", style = style, color = Neon.TextSecondary, maxLines = 1, softWrap = false, modifier = Modifier.testTag("tuto_map_bars_$i"))
                    }
                }
            }
        }
        // Every part's name in full, in the song's order, with its colour (the one the step is about in bold).
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.testTag("tuto_map_names")) {
            blocks.map { it.label }.distinct().forEach { label ->
                val c = colours.getValue(label)
                val on = focus == label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(if (focus == null || on) c else c.copy(alpha = 0.4f)).testTag("tuto_map_swatch_$label"))
                    Text(" " + Scene.shownLabel(label), style = MaterialTheme.typography.labelMedium, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = if (focus == null || on) Neon.TextPrimary else Neon.TextSecondary)
                }
            }
        }
        Text(tr("Chaque nombre : la durée de la partie en mesures."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

/** The chapters, each with its steps done; one touch goes there. « Passer en Pro », « Quitter le tuto ». */
@Composable
fun TutoChapterList(ui: TutoUi, goTo: (Int) -> Unit, setLevel: (TutoLevel) -> Unit, quit: () -> Unit, close: () -> Unit, restart: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(16.dp).testTag("tuto_chapter_list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Tuto de A à Z"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = close) { Icon(Icons.Filled.Close, contentDescription = tr("Fermer")) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ui.chapters.forEachIndexed { i, c ->
                // Tour 6 (T9): counted as the panel counts them (in Pro an example not heard holds nothing back).
                val count = c.steps.count(ui::settled)
                val whole = count == c.steps.size
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (i == ui.chapter) Neon.Cyan.copy(alpha = 0.16f) else Neon.SurfaceHigh)
                        .clickable { goTo(i) }.padding(12.dp).testTag("tuto_chapter_$i"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = Neon.Cyan, modifier = Modifier.width(32.dp))
                    Text(tutoText(c.title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (whole) Icon(Icons.Filled.CheckCircle, contentDescription = tr("Fait"), tint = Neon.Acid)
                    else Text("$count/${c.steps.size}", color = Neon.TextSecondary)
                }
            }
        }
        if (ui.progress.level == TutoLevel.Facile) {
            Button(onClick = { setLevel(TutoLevel.Pro) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_to_pro")) {
                Text(tr("Continuer en Pro (tous les outils)"))
            }
        } else {
            OutlinedButton(onClick = { setLevel(TutoLevel.Facile) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_to_easy")) {
                Text(tr("Revenir en Facile"))
            }
        }
        // Owner (2026-10-28, 2A): start again from an empty project, the same style and level (this one is kept).
        OutlinedButton(onClick = restart, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_restart")) {
            Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" " + tr("Recommencer le tuto (nouveau projet vide)"))
        }
        TextButton(onClick = quit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_quit")) {
            Text(tr("Quitter le tuto (tu pourras le reprendre)"), color = Neon.TextSecondary)
        }
    }
}

/**
 * « J'apprends »: Facile or Pro, then the style. Jump up has its course; the other styles say
 * « Bientôt pour les autres styles ».
 */
@Composable
fun TutoChooser(start: (style: String, level: TutoLevel) -> Unit, close: () -> Unit, simple: Boolean = true, fixedLevel: TutoLevel? = null) {
    // [fixedLevel]: given by « Simple ou Pro ? » just before (owner, 2026-10-29): not asked twice.
    var level by rememberSaveable { mutableStateOf(fixedLevel ?: if (simple) TutoLevel.Facile else TutoLevel.Pro) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).testTag("tuto_chooser"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = close, modifier = Modifier.testTag("tuto_chooser_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour")) }
            Text(tr("J'apprends"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(tr("Tu fais tout le morceau toi-même, étape par étape : je t'explique et je vérifie."), style = MaterialTheme.typography.bodyLarge)
        if (fixedLevel == null) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LevelCard(tr("Facile"), tr("Une quinzaine de chapitres, des mots simples, les écrans du Mode simple."), level == TutoLevel.Facile, "tuto_level_easy", Modifier.weight(1f)) { level = TutoLevel.Facile }
            LevelCard(tr("Pro"), tr("Tout le vocabulaire : piano roll, synthé, courbes, envois d'effets, mix et master."), level == TutoLevel.Pro, "tuto_level_pro", Modifier.weight(1f)) { level = TutoLevel.Pro }
        }
        Text(tr("Choisis ton style"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val (ready, soon) = StarterWizard.STYLES.partition { it.id in TutoCourse.STYLES }
            ready.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Neon.Cyan.copy(alpha = 0.18f)).border(1.5.dp, Neon.Cyan, RoundedCornerShape(14.dp))
                        .clickable { start(s.id, level) }.padding(14.dp).testTag("tuto_style_${s.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tutoText(s.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${s.bpm.toInt()} BPM", style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.Cyan)
                }
            }
            // Every style with its course: no « Bientôt » line.
            if (soon.isNotEmpty()) {
                Text(tr("Bientôt pour les autres styles"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, modifier = Modifier.padding(top = 6.dp).testTag("tuto_soon"))
                Text(soon.joinToString(" · ") { tutoText(it.name) }, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun LevelCard(title: String, line: String, chosen: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape).background(if (chosen) Neon.Cyan.copy(alpha = 0.18f) else Neon.Surface)
            .border(if (chosen) 2.dp else 1.dp, if (chosen) Neon.Cyan else Neon.Outline, shape)
            .clickable(onClick = onClick).padding(12.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (chosen) Neon.Cyan else Neon.TextPrimary)
        Text(line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}

/** The map of [style]'s song (cached by the target). */
fun tutoMap(style: String): List<TutoMapBlock> = TutoTarget.of(style)?.let(TutoChecks::map).orEmpty()

/**
 * A step cell the tutorial asks for, still empty: a soft fill of the pad's [color] and a dashed
 * outline that stands out on the dark grid, there for the whole step (not only while lit).
 */
@Composable
fun TutoEraseCell() {
    val red = Neon.Red
    Canvas(Modifier.fillMaxSize().padding(3.dp).testTag("tuto_erase")) {
        val r = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        val w = 2.5.dp.toPx()
        drawRoundRect(red, cornerRadius = r, style = Stroke(width = w, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
        val m = size.minDimension * 0.28f
        val c = center
        drawLine(red, c - androidx.compose.ui.geometry.Offset(m, m), c + androidx.compose.ui.geometry.Offset(m, m), strokeWidth = w)
        drawLine(red, c + androidx.compose.ui.geometry.Offset(-m, m), c + androidx.compose.ui.geometry.Offset(m, -m), strokeWidth = w)
    }
}

/**
 * A cell of the step's target still empty, dotted. [ring]: the one to touch next, ringed with the pulsing amber of
 * « Touche ici » ([TutoSpotRing]; tour 8, seat a, ch.3: no cell said where to start).
 */
@Composable
fun TutoHintCell(color: Color, ring: Boolean = false) {
    Canvas(Modifier.fillMaxSize().padding(3.dp).testTag("tuto_hint")) {
        val r = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        drawRoundRect(color.copy(alpha = 0.28f), cornerRadius = r)
        drawRoundRect(
            Color.White.copy(alpha = 0.9f), cornerRadius = r,
            style = Stroke(width = 2.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
    }
    if (ring) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "tuto_cell_ring")
        val pulse = transition.animateFloat(
            0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse), label = "tuto_cell_ring_pulse",
        )
        val amber = Neon.Amber
        Canvas(Modifier.fillMaxSize().testTag("tuto_hint_ring")) {
            val w = 3.dp.toPx()
            val r = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx())
            drawRoundRect(amber.copy(alpha = 0.22f * (1f - pulse.value)), cornerRadius = r)
            drawRoundRect(
                amber.copy(alpha = 0.95f - 0.4f * pulse.value), Offset(w / 2, w / 2),
                androidx.compose.ui.geometry.Size(size.width - w, size.height - w), r, style = Stroke(w),
            )
        }
    }
}

/**
 * The key step's own sheet: « Gamme » then « Tonique », the words the piano roll's « Gamme et
 * accords » uses; the ones the step asks for drawn dotted until chosen. A touch sets the song's key
 * (one undo step, the scale locked as the assistant does).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TutoKeySheetContent(harmony: com.pocketstudio.model.Harmony, wanted: com.pocketstudio.model.Harmony, set: (com.pocketstudio.model.Harmony) -> Unit, close: () -> Unit) {
    val english = com.pocketstudio.app.settings.englishNotes()
    // Tour 9, F018 (t9-11: « G » touched first, « Dorian » was chosen by itself): on a song without a key, the scale and
    // the root are chosen one by one, each shown chosen at once, and the key is set only once both are.
    val off = harmony.scale == com.pocketstudio.model.Scale.Off
    var scale by remember(harmony) { mutableStateOf(if (off) null else harmony.scale) }
    var root by remember(harmony) { mutableStateOf(if (off) null else harmony.root) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).testTag("tuto_key_sheet"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Tonalité du morceau"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = close, modifier = Modifier.testTag("tuto_key_close")) { Text(tr("Fermer")) }
        }
        val now = if (harmony.scale == com.pocketstudio.model.Scale.Off) tr("aucune") else "${com.pocketstudio.model.NoteNames.key(harmony.root, english)} · ${tr(harmony.scale.label)}"
        Text(tr("Tonalité du morceau : %s", now), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("tuto_key_now"))
        Text(tr("Gamme"), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            com.pocketstudio.model.Scale.entries.filter { it != com.pocketstudio.model.Scale.Off }.forEach { sc ->
                KeyChip(tr(sc.label), scale == sc, sc == wanted.scale, "tuto_scale_${sc.name}") {
                    scale = sc
                    root?.let { set(harmony.copy(root = it, scale = sc, lock = true)) }
                }
            }
        }
        Text(tr("Tonique"), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..11).forEach { i ->
                KeyChip(com.pocketstudio.model.NoteNames.key(i, english), root == i, i == wanted.root, "tuto_root_$i") {
                    root = i
                    scale?.let { set(harmony.copy(root = i, scale = it, lock = true)) }
                }
            }
        }
        // Audit ultra round 2, n° 24 (seat A a, screen 22): « Phrygienne » then « Fa » touched, the sheet stayed as it was and
        // « Fermer » had to be guessed: the key asked for is said reached, and the way back to the course is one big button.
        // Tour 9, F018 (t9-11: « la feuille a sauté vers le haut après « G »): the place of « Réussi ! » is kept from the
        // start (drawn only once reached, nothing to read or touch before), so the chips stay under the finger.
        val reached = harmony.scale == wanted.scale && harmony.root == wanted.root
        Column(
            Modifier.fillMaxWidth().alpha(if (reached) 1f else 0f).then(if (reached) Modifier else Modifier.clearAndSetSemantics {}),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp).then(if (reached) Modifier.testTag("tuto_key_done") else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(22.dp))
                Text(tr("Réussi ! La couleur du morceau est choisie."), color = Neon.Acid, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = close, enabled = reached, colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Neon.OnAccent),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).then(if (reached) Modifier.testTag("tuto_key_continue") else Modifier),
            ) { Text(tr("Revenir au tuto"), fontWeight = FontWeight.Bold) }
        }
    }
}

/** [TutoKeySheetContent] in a sheet over the screen. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TutoKeySheet(harmony: com.pocketstudio.model.Harmony, wanted: com.pocketstudio.model.Harmony, set: (com.pocketstudio.model.Harmony) -> Unit, close: () -> Unit) {
    com.pocketstudio.ui.ScaledBottomSheet(
        onDismissRequest = close, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Neon.Surface, scrolls = true,
    ) { TutoKeySheetContent(harmony, wanted, set, close) }
}

/**
 * A choice of the key sheet: filled and ticked when chosen (and said « sélectionné »), dotted when it is the one the step
 * asks for. Tour 9, F018 (t9-15, t9-54: « rien de marqué sur les puces »): the asked one was tinted like a choice.
 */
@Composable
private fun KeyChip(label: String, chosen: Boolean, wanted: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val amber = Neon.Amber
    Row(
        Modifier.heightIn(min = 48.dp).widthIn(min = 52.dp).clip(shape)
            .background(if (chosen) Neon.Cyan else Neon.SurfaceHigh)
            .then(if (wanted && !chosen) Modifier.drawDashed(amber) else Modifier.border(1.dp, if (chosen) Neon.Cyan else Neon.Outline, shape))
            .selectable(selected = chosen, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (chosen) Icon(Icons.Filled.Check, contentDescription = null, tint = Neon.OnAccent, modifier = Modifier.padding(end = 4.dp).size(16.dp))
        Text(label, color = if (chosen) Neon.OnAccent else Neon.TextPrimary, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal)
    }
}

/** The tutorial's dashed amber outline around a control it asks to touch (an effects box's send). */
@Composable
fun tutoDashed(): Modifier = Modifier.drawDashed(Neon.Amber)

/** A dashed outline around a control (what the tutorial asks to touch). */
fun Modifier.drawDashed(color: Color): Modifier = drawBehind {
    val r = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
    drawRoundRect(color, cornerRadius = r, style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
}

/**
 * « Je le fais »: the control of the step ringed a moment (a ring that pulses, no card, nothing to
 * close), over the screen without taking its touches; it goes by itself ([TutoController]'s panel
 * stays as it is). [target]: the control's place in the window (null: not found, nothing drawn).
 */
@Composable
fun TutoSpotRing(target: androidx.compose.ui.geometry.Rect?, modifier: Modifier = Modifier) {
    if (target == null) return
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "tuto_ring")
    val pulse = transition.animateFloat(
        0f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse), label = "tuto_ring_pulse",
    )
    val amber = Neon.Amber
    Canvas(modifier.fillMaxSize().testTag("tuto_spot_ring")) {
        val grow = 4.dp.toPx() + 6.dp.toPx() * pulse.value
        val r = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx())
        val tl = Offset(target.left - grow, target.top - grow)
        val sz = androidx.compose.ui.geometry.Size(target.width + 2 * grow, target.height + 2 * grow)
        drawRoundRect(amber.copy(alpha = 0.18f * (1f - pulse.value)), tl, sz, r)
        drawRoundRect(amber.copy(alpha = 0.95f - 0.4f * pulse.value), tl, sz, r, style = Stroke(3.dp.toPx()))
    }
}

/** n° 20: a gesture of « Montre-moi » that slides (a knob turned up, a fader lowered), not a touch. */
enum class TutoDrag { RIGHT, DOWN }

/**
 * n° 20 (« Montre-moi » that shows): a finger on the place of each gesture, a halo spreading from
 * it as it touches ([gesture] counts them: each new one plays again). On a pad's grid ([cell] of a
 * bar of [perBar] steps, two rows) the very cell, else the middle of [target]. A [drag] (a knob, a
 * fader): the finger presses, slides along [dragPath] leaving a trail, then lifts.
 */
@Composable
fun TutoFinger(target: androidx.compose.ui.geometry.Rect?, gesture: Int, cell: Int?, perBar: Int, modifier: Modifier = Modifier, drag: TutoDrag? = null) {
    if (target == null || gesture <= 0) return
    val touch = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(gesture, drag) {
        touch.snapTo(0f)
        touch.animateTo(1f, androidx.compose.animation.core.tween(if (drag != null) FINGER_DRAG_MS else FINGER_TOUCH_MS, easing = androidx.compose.animation.core.LinearEasing))
    }
    val path = drag?.let { dragPath(target, it) }
    val amber = Neon.Amber
    val finger = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Filled.TouchApp)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val size = with(density) { FINGER_SIZE.toPx() }
    // Where the finger is now (read in the draw and layout phases only: the animation recomposes nothing).
    fun tip(t: Float): Offset = path?.let { (a, b) -> androidx.compose.ui.geometry.lerp(a, b, dragProgress(t)) } ?: fingerAt(target, cell, perBar)
    Canvas(modifier.fillMaxSize().testTag("tuto_finger")) {
        val t = touch.value
        val at = tip(t)
        if (path != null) {
            // The way it went: a trail from where it pressed to where it is, a dot where it started.
            val (a, _) = path
            drawLine(amber.copy(alpha = 0.75f), a, at, strokeWidth = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawCircle(amber.copy(alpha = 0.6f), 6.dp.toPx(), a)
            val held = if (t < DRAG_PRESS) t / DRAG_PRESS else if (t > 1f - DRAG_LIFT) (1f - t) / DRAG_LIFT else 1f
            drawCircle(amber.copy(alpha = 0.35f * held), 18.dp.toPx(), at)
            drawCircle(amber.copy(alpha = 0.9f * held), 18.dp.toPx(), at, style = Stroke(3.dp.toPx()))
        } else {
            val r = 10.dp.toPx() + 26.dp.toPx() * t
            drawCircle(amber.copy(alpha = 0.35f * (1f - t)), r, at)
            drawCircle(amber.copy(alpha = 0.9f * (1f - t)), r, at, style = Stroke(3.dp.toPx()))
        }
    }
    // The finger itself, its tip on the place (a little lower while it presses).
    Box(
        Modifier.offset {
            val t = touch.value
            val at = tip(t)
            // A drag's tip stays on its path (the halo says it presses); a touch dips a little.
            val press = if (path != null) 0f else (if (t < 0.3f) t / 0.3f else 1f - (t - 0.3f) / 0.7f) * 4.dp.toPx()
            androidx.compose.ui.unit.IntOffset((at.x - size * 0.35f).roundToInt(), (at.y - size * 0.08f + press).roundToInt())
        }.size(FINGER_SIZE).testTag("tuto_finger_tip"),
    ) {
        androidx.compose.foundation.Image(finger, contentDescription = null, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White), modifier = Modifier.fillMaxSize())
    }
}

/** The way a [drag] goes over [target]: a knob's slider (in its lower half) to the right, a fader down. */
internal fun dragPath(target: androidx.compose.ui.geometry.Rect, drag: TutoDrag): Pair<Offset, Offset> = when (drag) {
    TutoDrag.RIGHT -> {
        val y = target.top + target.height * 0.7f
        Offset(target.left + target.width * 0.25f, y) to Offset(target.left + target.width * 0.75f, y)
    }
    TutoDrag.DOWN -> Offset(target.center.x, target.top + target.height * 0.3f) to Offset(target.center.x, target.top + target.height * 0.65f)
}

/** How far along its path a drag is at [t] (0..1 of its time): still while pressing, eased along, still while lifting. */
internal fun dragProgress(t: Float): Float =
    androidx.compose.animation.core.FastOutSlowInEasing.transform(((t - DRAG_PRESS) / (1f - DRAG_PRESS - DRAG_LIFT)).coerceIn(0f, 1f))

/** Where a gesture of « Montre-moi » lands in [target]: cell [cell] of a bar of [perBar] steps in two rows, else its middle. */
internal fun fingerAt(target: androidx.compose.ui.geometry.Rect, cell: Int?, perBar: Int): Offset {
    if (cell == null || perBar <= 0) return target.center
    val cols = (perBar / 2).coerceAtLeast(1)
    val k = cell % perBar
    return Offset(target.left + target.width * ((k % cols) + 0.5f) / cols, target.top + target.height * ((k / cols) + 0.5f) / 2f)
}

private const val FINGER_TOUCH_MS = 700
/** A drag of « Montre-moi »: under the gesture's second ([TutoController.FRAME_MS]), slow enough to follow. */
internal const val FINGER_DRAG_MS = 900
private const val DRAG_PRESS = 0.15f
private const val DRAG_LIFT = 0.15f
private val FINGER_SIZE = 40.dp

/**
 * Tour 9, F021 (t9-33: the echo send set in the effects window, « la feuille d'effets couvre la carte du tuto : aucune
 * confirmation visible »): a step ticked while a window covers the course's card is said a moment over that window,
 * in the card's own words. Drawn only inside a window (a dialog or a sheet): on the screen itself, the card says it.
 */
@Composable
fun TutoTickedOverWindow() {
    if (!TutoHints.active) return
    val view = androidx.compose.ui.platform.LocalView.current
    val windowed = remember(view) { generateSequence(view.parent) { it.parent }.any { it is androidx.compose.ui.window.DialogWindowProvider } }
    if (!windowed) return
    val ticked = TutoHints.ticked
    val start = remember { ticked }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(ticked) {
        if (ticked != start) {
            shown = true
            kotlinx.coroutines.delay(2_500)
            shown = false
        }
    }
    if (!shown) return
    androidx.compose.ui.window.Popup(
        popupPositionProvider = TopCenterPopup,
        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
    ) {
        Row(
            Modifier.padding(top = 12.dp).background(Neon.SurfaceHigh, RoundedCornerShape(12.dp))
                .border(1.dp, Neon.Acid, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }.testTag("tuto_ticked_toast"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(20.dp))
            Text(tr("Bravo, c'est réussi !"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** At the top of the window, across its middle. */
private object TopCenterPopup : androidx.compose.ui.window.PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect, windowSize: androidx.compose.ui.unit.IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection, popupContentSize: androidx.compose.ui.unit.IntSize,
    ) = androidx.compose.ui.unit.IntOffset((windowSize.width - popupContentSize.width) / 2, 0)
}
