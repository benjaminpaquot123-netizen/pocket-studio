package com.pocketstudio.app.tuto

import androidx.compose.animation.animateContentSize
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

/** The dotted cells the Rythme tab shows on a pad while a drum step is shown: (pad, steps). */
object TutoHints {
    var cells by mutableStateOf<Pair<Int, Set<Int>>?>(null)
}

/** What the panel's buttons do ([TutoController]'s). */
class TutoActions(
    val listen: () -> Unit = {},
    val doIt: () -> Unit = {},
    val showMe: () -> Unit = {},
    val markDone: () -> Unit = {},
    val toggle: () -> Unit = {},
    val next: () -> Unit = {},
    val previous: () -> Unit = {},
    val chapters: () -> Unit = {},
    val quit: () -> Unit = {},
)

/**
 * The tutorial over the bottom of the screen: chapter X/N and step x/n with a progress bar, then
 * (unfolded) « À toi : … », its « Pourquoi ? », the song's map in the structure chapters, and
 * three buttons: « ▶ Écouter l'exemple », « Je le fais », « Montre-moi » (« C'est fait » for a
 * step only the user can tick). Folded, one line: the screen stays free to work on.
 */
@Composable
fun TutoPanel(ui: TutoUi, canShow: Boolean, actions: TutoActions, modifier: Modifier = Modifier, map: List<TutoMapBlock> = emptyList()) {
    val step = ui.current
    val chapter = ui.chapters[ui.chapter]
    val done = step.id in ui.progress.done
    Surface(
        color = Neon.Surface, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = modifier.fillMaxWidth().border(1.dp, Neon.Cyan.copy(alpha = 0.6f), RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).testTag("tuto_panel"),
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
            } else if (ui.bravo) {
                Text(tr("Bravo, c'est bon ! Étape suivante :"), color = Neon.Acid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("tuto_bravo"))
            }
            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (done) Icon(Icons.Filled.CheckCircle, contentDescription = tr("Fait"), tint = Neon.Acid, modifier = Modifier.padding(end = 6.dp).size(20.dp).testTag("tuto_step_done"))
                    Text(tutoText(step.textFor(ui.progress.level)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("tuto_text"))
                }
                Why(step.why ?: chapter.why.takeIf { it.isNotEmpty() })
                if (chapter.map && map.isNotEmpty()) SongMap(map)
                if (ui.failed) Text(tr("L'exemple ne s'écoute pas ici."), color = Neon.Amber, style = MaterialTheme.typography.bodySmall)
            }
            Buttons(ui, canShow, actions)
        }
    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Buttons(ui: TutoUi, canShow: Boolean, actions: TutoActions) {
    val step = ui.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        if (step.listen.isNotEmpty()) {
            val busy = ui.preparing || ui.playing
            OutlinedButton(onClick = actions.listen, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_listen")) {
                when {
                    ui.preparing -> CircularProgressIndicator(Modifier.size(16.dp).semantics { contentDescription = tr("Préparation de l'écoute") }, strokeWidth = 2.dp)
                    busy -> Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    else -> Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(" " + if (busy) tr("Arrêter l'écoute") else tr("Écouter l'exemple"), maxLines = 1)
            }
        }
        if (step.spot.isNotEmpty()) {
            Button(onClick = actions.doIt, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_do"),
                colors = ButtonDefaults.buttonColors(containerColor = Neon.Cyan, contentColor = Neon.OnAccent)) {
                Icon(Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + tr("Je le fais"), maxLines = 1)
            }
        }
        if (step.manual) {
            OutlinedButton(onClick = actions.markDone, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_mark")) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + if (step.spot.isEmpty()) tr("J'ai compris") else tr("C'est fait"), maxLines = 1)
            }
        } else if (canShow) {
            OutlinedButton(onClick = actions.showMe, modifier = Modifier.heightIn(min = 44.dp).testTag("tuto_show")) {
                Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(" " + tr("Montre-moi"), maxLines = 1)
            }
        }
        Row {
            IconButton(onClick = actions.previous, modifier = Modifier.testTag("tuto_prev")) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Étape précédente")) }
            IconButton(onClick = actions.next, modifier = Modifier.testTag("tuto_next")) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = tr("Étape suivante")) }
        }
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
fun TutoChapterList(ui: TutoUi, goTo: (Int) -> Unit, setLevel: (TutoLevel) -> Unit, quit: () -> Unit, close: () -> Unit) {
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

/** A step cell the tutorial asks for, still empty: a dotted outline in the pad's [color]. */
@Composable
fun TutoHintCell(color: Color) {
    Canvas(Modifier.fillMaxSize().padding(3.dp).testTag("tuto_hint")) {
        val r = 6.dp.toPx()
        drawRoundRect(
            color.copy(alpha = 0.9f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
            style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
    }
}
