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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.School
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.Scene
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.tuto.TutoChecks
import com.pocketstudio.model.tuto.TutoCourse
import com.pocketstudio.model.tuto.TutoLevel
import com.pocketstudio.model.tuto.TutoMapBlock
import com.pocketstudio.model.tuto.TutoTarget
import com.pocketstudio.ui.Neon

/** A text of the course (tutos/<style>.json, checked by TutoCourseTest) or a style's name, in the interface's language. */
internal fun tutoText(fr: String): String = tr(fr)

/** What the tutorial draws on the screens: dotted cells of a drum step (pad, steps), dotted notes of a melodic step (track, notes), the cells « Montre-moi » lit fading away (pad, steps, how much they show). */
object TutoHints {
    var cells by mutableStateOf<Pair<Int, Set<Int>>?>(null)
    var notes by mutableStateOf<Pair<Int, List<com.pocketstudio.model.Note>>?>(null)
    var fading by mutableStateOf<Triple<Int, Set<Int>, Float>?>(null)
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
)

/** « Recommencer le tuto ». */
fun restartTutoLabel(): String = tr("Recommencer le tuto")

/** The words of a need's button, as the step's text names it. */
fun needLabel(need: TutoNeed): String = when (need) {
    is TutoNeed.Bars -> tr("Passer à %d mesures", need.bars)
    is TutoNeed.Track -> when (need.role) {
        "bass" -> tr("Créer la piste Basse")
        "pad" -> tr("Créer la piste Accords")
        "lead" -> tr("Créer la piste Mélodie")
        else -> tr("Créer la piste Sub")
    }
    is TutoNeed.Part -> tr("Créer la partie « %s »", Scene.shownLabel(need.label))
    is TutoNeed.Name -> tr("Nommer la partie « %s »", need.label)
    TutoNeed.Lengths -> tr("Donner les durées conseillées")
    is TutoNeed.Scene -> tr("Aller dans la partie « %s »", Scene.shownLabel(need.label))
}

/** The panel's background: the screen's, tinted with the tutorial's colour (never a page card's grey). */
@Composable
private fun panelColor(): Color = androidx.compose.ui.graphics.lerp(Neon.Background, Neon.Cyan, 0.07f)

/**
 * The tutorial over the bottom of the screen, apart from the page's cards (its own tint, an accent
 * edge that beats like the Guide's): chapter X/N and step x/n with a progress bar, then (unfolded)
 * « À toi : … », its « Pourquoi ? », the song's map in the structure chapters, then its action bar:
 * the step's main action as a big filled button (what it needs first, « Je le fais », « C'est
 * fait »), « ▶ Écouter l'exemple » and « Montre-moi » under it, « ← Précédent » and « Suivant → ».
 * A step done stays (« Bravo, c'est réussi ! »): « Étape suivante » is the big button, « ▶ Écouter
 * ton résultat » ([mine]: something to hear in the project, [minePlaying]: it plays) beside the example.
 * Folded, one line: the screen stays free to work on. A new step opens it again.
 */
@Composable
fun TutoPanel(
    ui: TutoUi, canShow: Boolean, actions: TutoActions, modifier: Modifier = Modifier, map: List<TutoMapBlock> = emptyList(),
    mine: Boolean = false, minePlaying: Boolean = false,
    reducedMotion: Boolean = com.pocketstudio.app.onboarding.animationsOff(androidx.compose.ui.platform.LocalContext.current),
) {
    val step = ui.current
    val chapter = ui.chapters[ui.chapter]
    val done = step.id in ui.progress.done
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val beat = com.pocketstudio.app.guide.rememberGuideBeat(animated = !reducedMotion)
    Surface(
        color = panelColor(), shape = shape,
        modifier = modifier.fillMaxWidth().border(2.dp, Neon.Cyan.copy(alpha = 0.6f), shape).guideEdge(beat, Neon.Cyan).testTag("tuto_panel"),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.School, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(start = 8.dp).clickable(onClick = actions.toggle).testTag("tuto_toggle")) {
                    Text(
                        tr("Chapitre %1\$d/%2\$d · %3\$s", ui.chapter + 1, ui.chapters.size, tutoText(chapter.title)),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        tr("Étape %1\$d/%2\$d", ui.step + 1, chapter.steps.size) + " · " + if (ui.progress.level == TutoLevel.Pro) tr("Pro") else tr("Facile"),
                        style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                    )
                }
                IconButton(onClick = actions.chapters, modifier = Modifier.testTag("tuto_chapters")) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = tr("Chapitres"))
                }
                IconButton(onClick = actions.toggle) {
                    Icon(if (ui.open) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = if (ui.open) tr("Replier") else tr("Déplier"))
                }
            }
            LinearProgressIndicator(
                progress = { ui.doneCount / ui.total.coerceAtLeast(1).toFloat() }, color = Neon.Acid, trackColor = Neon.SurfaceHigh,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).testTag("tuto_progress"),
            )
            if (!ui.open) {
                Text(tutoText(step.textFor(ui.progress.level)), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = actions.toggle))
                return@Column
            }
            if (ui.finished) {
                Text(tr("Bravo, ton morceau est fini ! Tu peux tout réécouter et continuer à le changer."), color = Neon.Acid,
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("tuto_finished"))
                OutlinedButton(onClick = actions.restart, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tuto_restart_finished")) {
                    Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" " + restartTutoLabel(), maxLines = 1)
                }
            } else if (ui.bravo) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Acid.copy(alpha = 0.14f)).border(1.dp, Neon.Acid, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp).testTag("tuto_bravo"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(24.dp))
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(tr("Bravo, c'est réussi !"), color = Neon.Acid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            if (mine) tr("Écoute ton résultat, puis touche « Étape suivante » quand tu veux.") else tr("Touche « Étape suivante » quand tu veux."),
                            style = MaterialTheme.typography.bodySmall, color = Neon.TextPrimary,
                        )
                    }
                }
            }
            Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (done) Icon(Icons.Filled.CheckCircle, contentDescription = tr("Fait"), tint = Neon.Acid, modifier = Modifier.padding(end = 6.dp).size(20.dp).testTag("tuto_step_done"))
                    Text(tutoText(step.textFor(ui.progress.level)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("tuto_text"))
                }
                Why(step.why ?: chapter.why.takeIf { it.isNotEmpty() })
                if (chapter.map && map.isNotEmpty()) SongMap(map)
                if (ui.failed) Text(tr("L'exemple ne s'entend pas : monte le volume du téléphone, puis touche encore « Écouter l'exemple »."), color = Neon.Amber,
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("tuto_failed"))
            }
            ui.demo?.let { phase ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Neon.Amber.copy(alpha = 0.16f)).border(1.dp, Neon.Amber, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp).testTag("tuto_demo_caption"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, tint = Neon.Amber, modifier = Modifier.size(20.dp))
                    Text(
                        if (phase == TutoDemoPhase.SHOWING) tr("Regarde, je le fais pour toi…") else tr("Voilà ce qu'il fallait faire — à toi maintenant !"),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary, modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            ActionBar(ui, canShow, actions, mine, minePlaying)
        }
    }
}

/** What the big button of a step does, and its words. */
private enum class Primary { NEXT, NEED, DO, MARK, LISTEN, NONE }

private fun primaryOf(ui: TutoUi): Primary {
    val s = ui.current
    return when {
        // A step done that stays shown (an example heard to its end): on to the next one.
        s.id in ui.progress.done && !ui.finished -> Primary.NEXT
        ui.need != null -> Primary.NEED
        s.spot.isNotEmpty() -> Primary.DO
        s.manual -> Primary.MARK
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
private fun ActionBar(ui: TutoUi, canShow: Boolean, actions: TutoActions, mine: Boolean, minePlaying: Boolean) {
    val step = ui.current
    val primary = primaryOf(ui)
    val busy = ui.preparing || ui.playing
    val big = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    val filled = ButtonDefaults.buttonColors(containerColor = Neon.Cyan, contentColor = Neon.OnAccent)
    when (primary) {
        Primary.NEXT -> Button(onClick = actions.next, colors = ButtonDefaults.buttonColors(containerColor = Neon.Acid, contentColor = Neon.OnAccent), modifier = big.testTag("tuto_next_big")) {
            Text(tr("Étape suivante") + "  ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Primary.NEED -> Button(onClick = actions.need, enabled = ui.demo == null, colors = filled, modifier = big.testTag("tuto_need")) {
            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + needLabel(ui.need!!), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Primary.DO -> Button(onClick = actions.doIt, enabled = ui.demo == null, colors = filled, modifier = big.testTag("tuto_do")) {
            Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + tr("Je le fais"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Primary.MARK -> Button(onClick = actions.markDone, colors = filled, modifier = big.testTag("tuto_mark")) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("  " + if (step.spot.isEmpty()) tr("J'ai compris") else tr("C'est fait"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Primary.LISTEN -> Button(onClick = actions.listen, colors = filled, modifier = big.testTag("tuto_listen")) {
            ListenContent(ui, busy)
        }
        Primary.NONE -> Unit
    }
    val secondaries = buildList<@Composable RowScope.() -> Unit> {
        // A step done: the user's own result first, then the example to compare.
        if (primary == Primary.NEXT && mine && !step.listening) add {
            OutlinedButton(onClick = actions.playMine, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_play_mine")) {
                Icon(if (minePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + if (minePlaying) tr("Arrêter") else tr("Écouter ton résultat"), maxLines = 2, textAlign = TextAlign.Center)
            }
        }
        if (step.listen.isNotEmpty() && primary != Primary.LISTEN) add {
            OutlinedButton(onClick = actions.listen, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_listen")) { ListenContent(ui, busy) }
        }
        if (step.manual && primary != Primary.MARK) add {
            OutlinedButton(onClick = actions.markDone, contentPadding = SECONDARY_PADDING, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_mark")) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + tr("C'est fait"), maxLines = 2, textAlign = TextAlign.Center)
            }
        } else if (canShow && primary != Primary.NEXT) add {
            val showing = ui.demo != null
            OutlinedButton(
                onClick = if (showing) actions.stopDemo else actions.showMe, contentPadding = SECONDARY_PADDING,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tuto_show"),
            ) {
                Icon(if (showing) Icons.Filled.Stop else Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + if (showing) tr("Arrêter") else tr("Montre-moi"), maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
    if (secondaries.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        secondaries.forEach { it() }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = actions.previous, enabled = ui.chapter > 0 || ui.step > 0, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_prev")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(" " + tr("Précédent"), maxLines = 1)
        }
        Box(Modifier.weight(1f))
        TextButton(onClick = actions.next, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_next")) {
            Text(tr("Suivant") + " ", maxLines = 1)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

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
    Text(" " + if (busy) tr("Arrêter l'écoute") else tr("Écouter l'exemple"), maxLines = 2, textAlign = TextAlign.Center)
}

/** « Pourquoi ? »: one or two sentences, folded until touched. */
@Composable
private fun Why(text: String?) {
    if (text == null) return
    var open by rememberSaveable(text) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = !open }.testTag("tuto_why")) {
        Text((if (open) "▾ " else "▸ ") + tr("Pourquoi ?"), style = MaterialTheme.typography.labelLarge, color = Neon.Cyan)
        if (open) Text(tutoText(text), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary, modifier = Modifier.testTag("tuto_why_text"))
    }
}

/** The colour of a part on the map (by its section's name). */
@Composable
private fun partColor(label: String): Color = when {
    label.startsWith("Drop") -> Neon.Magenta
    label == "Montée" -> Neon.Amber
    label == "Break" -> Color(0xFF5BB8F5)
    label == "Fin" -> Neon.TextSecondary
    else -> Neon.Cyan
}

/**
 * The song's map: one block per part, as wide as its bars (its name and bars in it), the energy
 * curve above (how many of its bricks play): the parts go up and down.
 */
@Composable
fun SongMap(blocks: List<TutoMapBlock>, modifier: Modifier = Modifier) {
    val total = blocks.sumOf { it.bars }.coerceAtLeast(1)
    val curve = Neon.Acid
    Column(modifier.fillMaxWidth().testTag("tuto_map"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(tr("Énergie"), style = MaterialTheme.typography.labelSmall, color = curve)
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val path = Path()
            var x = 0f
            blocks.forEachIndexed { i, b ->
                val w = size.width * b.bars / total
                val y = size.height * (1f - b.energy.coerceIn(0f, 1f) * 0.9f)
                if (i == 0) path.moveTo(0f, y) else path.lineTo(x, y)
                path.lineTo(x + w, y)
                x += w
            }
            drawPath(path, curve, style = Stroke(width = 3.dp.toPx()))
            drawLine(curve.copy(alpha = 0.25f), Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            blocks.forEach { b ->
                val c = partColor(b.label)
                Column(
                    Modifier.weight(b.bars.toFloat()).fillMaxSize().clip(RoundedCornerShape(6.dp)).background(c.copy(alpha = 0.3f)).border(1.dp, c, RoundedCornerShape(6.dp)).padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(Scene.shownLabel(b.label), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Clip)
                    Text("${b.bars}", style = MaterialTheme.typography.labelSmall, color = Neon.TextSecondary, maxLines = 1)
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
                val count = c.steps.count { it.id in ui.progress.done }
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
fun TutoChooser(start: (style: String, level: TutoLevel) -> Unit, close: () -> Unit, simple: Boolean = true) {
    var level by rememberSaveable { mutableStateOf(if (simple) TutoLevel.Facile else TutoLevel.Pro) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp).testTag("tuto_chooser"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = close, modifier = Modifier.testTag("tuto_chooser_back")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour")) }
            Text(tr("J'apprends"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Text(tr("Tu fais tout le morceau toi-même, étape par étape : je t'explique et je vérifie."), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
            Text(tr("Bientôt pour les autres styles"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary, modifier = Modifier.padding(top = 6.dp).testTag("tuto_soon"))
            Text(soon.joinToString(" · ") { tutoText(it.name) }, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary.copy(alpha = 0.7f))
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
            Text(tr("Tonalité"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = close, modifier = Modifier.testTag("tuto_key_close")) { Text(tr("Fermer")) }
        }
        val now = if (harmony.scale == com.pocketstudio.model.Scale.Off) tr("aucune") else "${com.pocketstudio.model.NoteNames.key(harmony.root, english)} ${tr(harmony.scale.label).inSentence()}"
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
    }
}

/** [TutoKeySheetContent] in a sheet over the screen. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TutoKeySheet(harmony: com.pocketstudio.model.Harmony, wanted: com.pocketstudio.model.Harmony, set: (com.pocketstudio.model.Harmony) -> Unit, close: () -> Unit) {
    com.pocketstudio.ui.ScaledBottomSheet(
        onDismissRequest = close, sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Neon.Surface,
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
