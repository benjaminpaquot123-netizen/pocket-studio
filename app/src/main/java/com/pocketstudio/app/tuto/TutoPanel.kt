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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Scene
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.i18n.trCount
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
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
    var notes by mutableStateOf<Pair<Int, List<com.pocketstudio.model.Note>>?>(null)
    /** The 303 steps a step asks for, by index (the Acide tab draws them dashed until written). */
    var acid by mutableStateOf<Map<Int, com.pocketstudio.model.AcidStep>?>(null)
    var fading by mutableStateOf<Triple<Int, Set<Int>, Float>?>(null)
    var send by mutableStateOf<Pair<Int, Int>?>(null)
    var focus by mutableIntStateOf(0)
    /** The [focus] the piano roll last answered (not state: read and written there only). */
    var focusSeen = 0
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
        cells = null; notes = null; acid = null; fading = null; send = null; focus = 0; focusSeen = 0; demo = false
        erase = null; knobs = emptySet(); strip = null; sendNote = null; active = false; ticked = 0; banner = null
        step = null; copy = null; page = null; copyOnGrid = false
    }
}

/** The tutorial's line over the drum grid, as a spot (a reveal of the cells keeps it in sight). */
const val TUTO_GRID_LINE_SPOT = "tuto_grid_line"

/** How long a screen waits before turning by itself for the tutorial (the last touch seen first). */
const val TUTO_TURN_MS = 450L

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
 * runs, one height: « Mesure 1/2 · Caisse », then « encore 2 cases » on this page, or « Mesure 2 ·
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
            elsewhere != null && here == 0 -> TextButton(onClick = { go(elsewhere.first) }, modifier = Modifier.heightIn(min = 48.dp).testTag("tuto_bar_go")) {
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
fun tutoChordsLeft(left: Int, way: com.pocketstudio.model.tuto.TutoWay?): String {
    if (left <= 0) return tutoNotesLeft(0, way)
    val said = tr("Accords en pointillé à toucher : %d", left)
    return when (way) {
        com.pocketstudio.model.tuto.TutoWay.Higher -> "$said ↑"
        com.pocketstudio.model.tuto.TutoWay.Lower -> "$said ↓"
        com.pocketstudio.model.tuto.TutoWay.Later -> "$said →"
        com.pocketstudio.model.tuto.TutoWay.Earlier -> "← $said"
        null -> said
    }
}

/** « Encore 3 notes en pointillé » over the piano roll ([way]: none of them in sight, the next one is that way). */
fun tutoNotesLeft(left: Int, way: com.pocketstudio.model.tuto.TutoWay?): String = when {
    left <= 0 -> tr("Toutes les notes en pointillé sont posées")
    way == com.pocketstudio.model.tuto.TutoWay.Higher -> trCount(left, "Encore %d note en pointillé, plus haut ↑", "Encore %d notes en pointillé, plus haut ↑")
    way == com.pocketstudio.model.tuto.TutoWay.Lower -> trCount(left, "Encore %d note en pointillé, plus bas ↓", "Encore %d notes en pointillé, plus bas ↓")
    way == com.pocketstudio.model.tuto.TutoWay.Later -> trCount(left, "Encore %d note en pointillé, plus loin →", "Encore %d notes en pointillé, plus loin →")
    way == com.pocketstudio.model.tuto.TutoWay.Earlier -> trCount(left, "← Encore %d note en pointillé, plus tôt", "← Encore %d notes en pointillé, plus tôt")
    else -> trCount(left, "Encore %d note en pointillé à toucher", "Encore %d notes en pointillé à toucher")
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
    // Round 3, n° 28: inside the panel, at the end of the step's words (the panel's own scroll): never in its place.
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
            ) {
                Text(
                    if (onPhone) tr("Mettre les sons du kit %s sur les pads", style) else tr("Télécharger le kit %1\$s (%2\$s)", style, size),
                    textAlign = TextAlign.Center,
                )
            }
        }
        TextButton(onClick = later, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("tuto_kit_later")) { Text(tr("Plus tard"), color = Neon.TextSecondary) }
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
) {
    val step = ui.current
    val chapter = ui.chapters[ui.chapter]
    val done = step.id in ui.progress.done
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val beat = com.pocketstudio.app.guide.rememberGuideBeat(animated = !reducedMotion)
    val density = LocalDensity.current
    // The unfolded panel's height, taken once for its step and then held (0: not taken yet).
    val end = ui.atEnd || ui.finished
    var held by remember(step.id, ui.open, ui.progress.level, end) { mutableIntStateOf(0) }
    // Testers (lot « tuto 4 »): a height taken while « Montre-moi »'s one-line caption showed cut the step's
    // words once it ended; the end's card takes the room it needs.
    val holding = ui.open && held > 0 && !end
    // Tour 6 (N31): « Lire la suite » lay over « ▸ Pourquoi ? » (a touch there read on). « Pourquoi ? » and « Lire la
    // suite » are under the scrolling words, never in them: neither under the other, « Pourquoi ? » never cut by the
    // words' room (Arabic at 130 %), the panel's height as before. The words' scroll is the step's own (the step
    // before's left its room in the height held for this one).
    val words = remember(step.id, ui.open) { androidx.compose.foundation.ScrollState(0) }
    // A new scroll's maxValue is Int.MAX_VALUE until its first measure: taken as « they scroll », « Lire la suite »'s room
    // went into the height held for the step, empty under words that fit. Only once they are measured longer than their room.
    val scrolls = words.maxValue in 1 until Int.MAX_VALUE
    val why = (step.why ?: chapter.why.takeIf { it.isNotEmpty() }).takeIf { ui.demo == null && !ui.bravo }
    var whyOpen by rememberSaveable(why) { mutableStateOf(false) }
    val reason = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    LaunchedEffect(whyOpen, why) { if (whyOpen && why != null) reason.bringIntoView() }
    Surface(
        color = panelColor(), shape = shape,
        modifier = modifier.fillMaxWidth().border(2.dp, Neon.Cyan.copy(alpha = 0.6f), shape).guideEdge(beat, Neon.Cyan).testTag("tuto_panel"),
    ) {
        Column(
            Modifier.then(if (holding) Modifier.height(with(density) { held.toDp() }) else Modifier)
                .onSizeChanged { if (ui.open && held == 0 && it.height > 0 && ui.demo == null && !end) held = it.height }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        (if (ui.chapterDoneNext() != null) tr("Chapitre %1\$d/%2\$d · %3\$s", ui.chapter + 1, ui.chapters.size, tr("Fait"))
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
                IconButton(onClick = actions.toggle, modifier = Modifier.testTag("tuto_fold")) {
                    Icon(if (ui.open) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = if (ui.open) tr("Replier") else tr("Déplier"))
                }
            }
            LinearProgressIndicator(
                progress = { ui.doneCount / ui.total.coerceAtLeast(1).toFloat() }, color = Neon.Acid, trackColor = Neon.SurfaceHigh,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).testTag("tuto_progress"),
            )
            if (!ui.open) {
                FoldedLine(ui, done, actions, names)
                return@Column
            }
            // Testers (lot « tuto 4 »): the end said nothing (or sent back to chapter 5): « Bravo » when all is
            // done, else the steps left, each one touch away; never « Je le fais » after the end.
            if (end) {
                EndCard(ui, actions, names, mine, minePlaying)
                return@Column
            }
            // The step's words, or in their place « Bravo » or « Montre-moi »'s caption: never one more block.
            // At least a line and a half of room (the « Bravo » banner fits where a one-line step was).
            val most = if (chapter.map && map.isNotEmpty()) 290.dp else 240.dp
            // Audit ultra (« le panneau tient »): words longer than the room scroll, and say so at the bottom.
            // The words take the room « Pourquoi ? » and « Lire la suite » leave them (measured first, no frame late).
            // A phone on its side leaves the words a line or two: « Pourquoi ? » goes back in them there (« Lire la suite »
            // whole under them, not squeezed); on a screen high enough it is pinned under them.
            val pinned = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp >= WHY_PINNED_SCREEN
            Column((if (holding) Modifier.weight(1f) else Modifier.heightIn(min = 52.dp, max = most)), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(words),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when {
                    ui.demo != null -> DemoCaption(ui.demo)
                    ui.bravo -> BravoBanner(mine, ui.made, nextChapter = ui.chapterDoneNext(), missing = ui.chapterLeft(), already = alreadyWords(ui))
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
                                Text(tutoWords(step.textFor(ui.progress.level), names), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("tuto_text"))
                            }
                        }
                        ui.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("tuto_note")) }
                        // n° 37: in the Mode simple, each word of the trade of the step (kick, Drop, phrygienne…) said in plain words under it.
                        if (alreadySaid == null) com.pocketstudio.app.library.PlainWordsLines(step.textFor(ui.progress.level))
                        if (why != null && !pinned) Why(whyOpen) { whyOpen = !whyOpen; held = 0 }
                        // « Pourquoi ? »'s reason, opened by the user, where it belongs in the words (brought into sight).
                        if (why != null && whyOpen) Text(tutoText(why), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary,
                            modifier = Modifier.bringIntoViewRequester(reason).testTag("tuto_why_text"))
                        if (chapter.map && map.isNotEmpty()) SongMap(map, focus = mapFocus(step.listen))
                        if (ui.failed) Text(tr("L'exemple ne s'entend pas : monte le volume du téléphone, puis touche encore « Écouter l'exemple »."), color = Neon.Amber,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("tuto_failed"))
                    }
                }
                // Tour 6 (T7): not on the step that says the kit is ready (« Tu n'as rien à régler » over « Télécharger le kit »).
                if (step.id != READY_STEP) kit?.invoke()
            }
            // « Pourquoi ? » opened by the user: the panel takes its new height (the user's own touch, on the panel).
            if (why != null && pinned) Why(whyOpen) { whyOpen = !whyOpen; held = 0 }
            // Its room kept while the words scroll at all (shown or not, nothing moves when the end is reached).
            if (scrolls) MoreWords(words.canScrollForward) {
                words.animateScrollTo(minOf(words.maxValue, words.value + words.viewportSize * 3 / 4))
            }
            }
            ActionBar(ui, canShow, actions, mine, minePlaying, mineFirst)
        }
    }
}

/** The panel's way to the chapters, at its widest: the bare list icon's 48 dp and the school icon's 22 + 8 it replaced. */
private val CHAPTERS_WIDTH = 78.dp

/** The screen's height (dp) from which « Pourquoi ? » is pinned under the words (a phone upright, a tablet; not on its side). */
private const val WHY_PINNED_SCREEN = 480

/** The courses' first step, « ton kit est déjà celui du style… Tu n'as rien à régler »: no kit offer under it. */
private const val READY_STEP = "ready"

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
private fun FoldedLine(ui: TutoUi, done: Boolean, actions: TutoActions, names: (String) -> String = { it }) {
    val shape = RoundedCornerShape(12.dp)
    val line = Modifier.fillMaxWidth().height(foldedLineHeight()).clip(shape)
    when {
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
        done && ui.chapterDoneNext() != null -> Box(line.background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, shape).testTag("tuto_bravo")) {
            Row(
                Modifier.fillMaxSize().clickable(onClick = actions.toggle).padding(horizontal = 10.dp).testTag("tuto_chapter_end_folded"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(20.dp))
                Text(tr("Bravo, chapitre fini !"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 6.dp))
                Icon(Icons.Filled.ExpandLess, contentDescription = tr("Déplier"), tint = Neon.Acid, modifier = Modifier.size(20.dp))
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
        ui.need != null -> Row(
            line.background(Neon.Amber.copy(alpha = 0.14f)).border(1.dp, Neon.Amber, shape).clickable(onClick = actions.need).padding(horizontal = 10.dp)
                .testTag("tuto_need_folded"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(needLabel(ui.need, ui.progress.style), color = Neon.Amber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Neon.Amber, modifier = Modifier.size(18.dp))
        }
        // Audit ultra round 1, n° 34 (seat A b, 200 %): « À toi : sur le pad « Clap », t… » said nothing; two lines of
        // the step, and « Lire » (a touch unfolds the rest).
        else -> Row(
            line.clickable(onClickLabel = tr("Lire la suite"), onClick = actions.toggle).testTag("tuto_folded_text"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tutoWords(ui.current.textFor(ui.progress.level), names), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f))
            Row(Modifier.padding(start = 6.dp).testTag("tuto_folded_more"), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Lire"), style = MaterialTheme.typography.labelMedium, color = Neon.Cyan, fontWeight = FontWeight.Bold, maxLines = 1)
                Icon(Icons.Filled.ExpandLess, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** The folded panel's line: a finger's height at least, whatever it says. */
private val FOLDED_LINE = 48.dp

/**
 * The folded line's height: [FOLDED_LINE], or two lines of the step's words when the font is big (n° 34: at
 * 200 % one line held « À toi : sur le pad « Clap », t… »). The same for every state of the line.
 */
@Composable
private fun foldedLineHeight(): androidx.compose.ui.unit.Dp {
    // Two lines as the text lays them out (its line height and the font's own room above and below), and a margin.
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = MaterialTheme.typography.bodyMedium
    val two = with(LocalDensity.current) { measurer.measure("Ag\nAg", style).size.height.toDp() } + 8.dp
    return maxOf(FOLDED_LINE, two)
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
private fun ActionBar(ui: TutoUi, canShow: Boolean, actions: TutoActions, mine: Boolean, minePlaying: Boolean, mineFirst: Boolean) {
    val step = ui.current
    val primary = primaryOf(ui, mineFirst)
    val busy = ui.preparing || ui.playing
    val big = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    val filled = ButtonDefaults.buttonColors(containerColor = Neon.Cyan, contentColor = Neon.OnAccent)
    val chapterEnd = ui.chapterDoneNext() != null
    when (primary) {
        Primary.NEXT -> Button(onClick = actions.next, colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Neon.OnAccent), modifier = big.testTag("tuto_next_big")) {
            Text((if (chapterEnd) tr("Chapitre suivant") else tr("Étape suivante")) + "  ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Primary.NEED -> Button(onClick = actions.need, enabled = ui.demo == null, colors = filled, modifier = big.testTag("tuto_need")) {
            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + needLabel(ui.need!!, ui.progress.style), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
    val secondaries = buildList<@Composable RowScope.() -> Unit> {
        // A step done: the user's own result first, then the example to compare.
        if (primary == Primary.NEXT && mine && (!step.listening || mineFirst || chapterEnd)) {
            val label = if (minePlaying) tr("Arrêter") else if (chapterEnd) tr("Écouter mon morceau") else tr("Écouter ton résultat")
            words += label
            add {
                OutlinedButton(onClick = actions.playMine, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_play_mine")) {
                    Icon(if (minePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    com.pocketstudio.ui.WholeWordsText(" $label", textAlign = TextAlign.Center)
                }
            }
        }
        if (step.listen.isNotEmpty() && primary != Primary.LISTEN) {
            words += if (busy) tr("Arrêter l'écoute") else tr("Écouter l'exemple")
            add {
                OutlinedButton(onClick = actions.listen, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_listen")) { ListenContent(ui, busy) }
            }
        }
        if (step.manual && primary != Primary.MARK) {
            words += tr("C'est fait")
            add {
                OutlinedButton(onClick = actions.markDone, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_mark")) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    com.pocketstudio.ui.WholeWordsText(" " + tr("C'est fait"), textAlign = TextAlign.Center)
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
                    com.pocketstudio.ui.WholeWordsText(" " + if (showing) tr("Arrêter") else tr("Montre-moi"), textAlign = TextAlign.Center)
                }
            }
        }
    }
    // Testers (lot « tuto 4 »): a step done had one more row of buttons than when it began, in the same held
    // height: its words went under the green button. Done, its other buttons share « Précédent »'s row…
    // Audit ultra round 2, n° 23 (seat A a, 200 %): … when their words fit there whole; else (a big font, a long
    // language) they get a row of their own, « ← Précédent » and « Suivant → » in words under them (it was « Éco… »
    // twice and an arrow).
    val beside = primary == Primary.NEXT && besideFits(words)
    if (secondaries.isNotEmpty() && !beside) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        secondaries.forEach { it() }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (beside) 6.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
        val back = ui.chapter > 0 || ui.step > 0
        if (beside && secondaries.isNotEmpty()) {
            // Only the arrow (its words for TalkBack): the row is the other buttons'.
            IconButton(onClick = actions.previous, enabled = back, modifier = Modifier.testTag("tuto_prev")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Précédent"), modifier = Modifier.size(20.dp))
            }
            secondaries.forEach { it() }
            // Testers (lot « tuto 5 »): « Suivant → » stays where it was (a quick touch there replayed the example).
            TextButton(onClick = actions.next, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_next")) {
                Text(tr("Suivant") + " ", maxLines = 1)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            return@Row
        }
        TextButton(onClick = actions.previous, enabled = back, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_prev")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" " + tr("Précédent"), maxLines = 1)
        }
        Box(Modifier.weight(1f))
        // Testers: two ways on (« Étape suivante » and « Suivant → ») puzzled them; the step done has only the big one
        // (n° 23: « Suivant → » stays where it was when its row is the secondary buttons' own on a big font).
        if (primary != Primary.NEXT || secondaries.isNotEmpty()) TextButton(onClick = actions.next, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_next")) {
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
private fun EndCard(ui: TutoUi, actions: TutoActions, names: (String) -> String = { it }, mine: Boolean = false, minePlaying: Boolean = false) {
    val left = ui.left
    Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()).testTag("tuto_end"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        left.take(END_LIST).forEach { s ->
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
        if (left.size > END_LIST) Text(trCount(left.size - END_LIST, "… et %d autre (liste ≡)", "… et %d autres (liste ≡)"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
        // N13: framed too (the end's card, one look for its buttons).
        if (ui.atEnd) OutlinedButton(onClick = actions.leaveEnd, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_end_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" " + tr("Revenir à l'étape"), maxLines = 1)
        }
    }
}

/**
 * Whether « ← », the secondary buttons ([words]) and « Suivant → » fit on one row with each button's
 * longest word whole at the smallest size [com.pocketstudio.ui.WholeWordsText] would shrink it to.
 */
@Composable
private fun besideFits(words: List<String>): Boolean {
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
            val next = px(tr("Suivant") + " ") + (18.dp + 12.dp * 2).toPx()
            // The panel's padding (14 dp each side), « ← » (48 dp), the gaps (6 dp).
            val room = (width - 28.dp - 48.dp - 6.dp * (words.size + 1)).toPx()
            buttons + next <= room
        }
    }
}

/** The steps the end lists by name (the others: the chapters' list). */
private const val END_LIST = 4

/** The secondary buttons' inner room: their words on one line on a phone, two at 130 %. */
private val SECONDARY_PADDING = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)

/** « ▶ Écouter l'exemple », a wheel while it is prepared, « ■ Arrêter l'écoute » while it plays. */
@Composable
private fun ListenContent(ui: TutoUi, busy: Boolean) {
    when {
        ui.preparing -> CircularProgressIndicator(Modifier.size(16.dp).semantics { contentDescription = tr("Préparation de l'écoute") }, strokeWidth = 2.dp)
        busy -> Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
        else -> Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
    }
    com.pocketstudio.ui.WholeWordsText(" " + if (busy) tr("Arrêter l'écoute") else tr("Écouter l'exemple"), textAlign = TextAlign.Center)
}

/** « Pourquoi ? »: its reason (one or two sentences, in the step's words) folded until touched ([toggle]). */
@Composable
private fun Why(open: Boolean, toggle: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = toggle).testTag("tuto_why")) {
        Text((if (open) "▾ " else "▸ ") + tr("Pourquoi ?"), style = MaterialTheme.typography.labelLarge, color = Neon.Cyan)
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
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            blocks.forEachIndexed { i, b ->
                val c = partColor(b.label)
                val on = lit(b)
                androidx.compose.foundation.layout.BoxWithConstraints(
                    Modifier.weight(b.bars.toFloat()).fillMaxSize().clip(RoundedCornerShape(6.dp))
                        .background(c.copy(alpha = if (focus != null && on) 0.6f else if (on) 0.3f else 0.12f))
                        .border(if (focus != null && on) 2.5.dp else 1.dp, if (on) c else c.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .then(if (focus != null && on) Modifier.testTag("tuto_map_lit_$i") else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    // A name only where it fits whole (the names are written under the map anyway). Tour 6 (N29, N30): measured
                    // as drawn, in its language and font size (« Breal », « ブレイク » cut, « 16 » out of its block in Arabic
                    // at 130 %): the name and its bars when both fit, else the bars alone, else nothing.
                    val name = Scene.shownLabel(b.label)
                    val bars = "${b.bars}"
                    val style = MaterialTheme.typography.labelSmall
                    val n = measurer.measure(name, style, softWrap = false, maxLines = 1).size
                    val k = measurer.measure(bars, style, softWrap = false, maxLines = 1).size
                    val w = with(LocalDensity.current) { (maxWidth - 2.dp).toPx() }
                    val h = with(LocalDensity.current) { (maxHeight - 2.dp).toPx() }
                    when {
                        n.width <= w && k.width <= w && n.height + k.height <= h -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(name, style = style, maxLines = 1, softWrap = false, color = if (on) Neon.TextPrimary else Neon.TextSecondary)
                            Text(bars, style = style, color = Neon.TextSecondary, maxLines = 1, softWrap = false)
                        }
                        k.width <= w && k.height <= h -> Text(bars, style = style, color = Neon.TextSecondary, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
        // Every part's name in full, in the song's order, with its colour (the one the step is about in bold).
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.testTag("tuto_map_names")) {
            blocks.map { it.label }.distinct().forEach { label ->
                val c = partColor(label)
                val on = focus == label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(if (focus == null || on) c else c.copy(alpha = 0.4f)))
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

/** A cell of the step's target still empty, dotted. */
@Composable
fun TutoHintCell(color: Color) {
    Canvas(Modifier.fillMaxSize().padding(3.dp).testTag("tuto_hint")) {
        val r = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        drawRoundRect(color.copy(alpha = 0.28f), cornerRadius = r)
        drawRoundRect(
            Color.White.copy(alpha = 0.9f), cornerRadius = r,
            style = Stroke(width = 2.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
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
                KeyChip(tr(sc.label), harmony.scale == sc, sc == wanted.scale, "tuto_scale_${sc.name}") {
                    set(harmony.copy(scale = sc, lock = true))
                }
            }
        }
        Text(tr("Tonique"), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            (0..11).forEach { i ->
                KeyChip(com.pocketstudio.model.NoteNames.key(i, english), harmony.root == i && harmony.scale != com.pocketstudio.model.Scale.Off, i == wanted.root, "tuto_root_$i") {
                    set(harmony.copy(root = i, scale = if (harmony.scale == com.pocketstudio.model.Scale.Off) wanted.scale else harmony.scale, lock = true))
                }
            }
        }
        // Audit ultra round 2, n° 24 (seat A a, screen 22): « Phrygienne » then « Fa » touched, the sheet stayed as it was and
        // « Fermer » had to be guessed: the key asked for is said reached, and the way back to the course is one big button.
        if (harmony.scale == wanted.scale && harmony.root == wanted.root) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp).testTag("tuto_key_done"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(22.dp))
                Text(tr("Réussi ! La couleur du morceau est choisie."), color = Neon.Acid, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = close, colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Neon.OnAccent),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("tuto_key_continue"),
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

/** A choice of the key sheet: filled when chosen, dotted when it is the one the step asks for. */
@Composable
private fun KeyChip(label: String, chosen: Boolean, wanted: Boolean, tag: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val amber = Neon.Amber
    Box(
        Modifier.heightIn(min = 48.dp).widthIn(min = 52.dp).clip(shape)
            .background(if (chosen) Neon.Cyan else if (wanted) Neon.Amber.copy(alpha = 0.18f) else Neon.SurfaceHigh)
            .then(if (wanted && !chosen) Modifier.drawDashed(amber) else Modifier.border(1.dp, if (chosen) Neon.Cyan else Neon.Outline, shape))
            .clickable(onClick = onClick).padding(horizontal = 12.dp).testTag(tag),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (chosen) Neon.OnAccent else Neon.TextPrimary, fontWeight = if (chosen || wanted) FontWeight.Bold else FontWeight.Normal) }
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
