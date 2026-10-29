package com.pocketstudio.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.settings.ProjectModes
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.ScaledDialog

/** The Simple mode's colour (sky blue: green means « playing »); Pro takes the app's accent. */
internal val SIMPLE_COLOR = Color(0xFF5BB8F5)

/** The mode's name as the interface says it: « Simple » or « Pro » (inside, Pro is the full mode). */
fun modeName(simple: Boolean): String = if (simple) tr("Simple") else tr("Pro")

/**
 * The step every new project goes through (owner, 2026-10-29): « Simple ou Pro ? », two big
 * cards, none chosen beforehand; the last choice made carries a « Ton dernier choix » mark.
 * [tuto]: for « J'apprends », each card says the tutorial's level it gives (Simple: Facile).
 */
@Composable
fun ModeStep(choose: (simple: Boolean) -> Unit, back: () -> Unit, tuto: Boolean = false) {
    val context = LocalContext.current
    val last = remember { ProjectModes.last(context) }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("mode_step"), contentAlignment = Alignment.TopCenter) {
        val wide = maxWidth >= 600.dp
        com.pocketstudio.ui.ReadingWidth(maxWidth = 760.dp) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back, modifier = Modifier.testTag("mode_step_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Retour"))
                    }
                    Text(tr("Simple ou Pro ?"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Text(
                    if (tuto) tr("Le mode choisit aussi le niveau du tuto. Tu pourras changer de mode à tout moment avec la pastille en haut.")
                    else tr("Comment veux-tu voir ce projet ? Tu pourras changer à tout moment avec la pastille en haut."),
                    style = MaterialTheme.typography.bodyLarge, color = Neon.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp),
                )
                ModeCards(mark = last?.let { it to tr("Ton dernier choix") }, tuto = tuto, wide = wide, choose = choose)
            }
        }
    }
}

/**
 * The two cards: Simple then Pro. [mark]: one of them carries a small label (« Ton dernier
 * choix », « En ce moment »), it is never chosen for the user. Side by side when [wide].
 */
@Composable
fun ModeCards(mark: Pair<Boolean, String>?, tuto: Boolean = false, wide: Boolean = false, choose: (Boolean) -> Unit) {
    val simple: @Composable (Modifier) -> Unit = { m ->
        ModeCard(
            true, tr("Simple"), tr("Pour débuter : 4 onglets, de gros boutons et des mots de tous les jours."),
            if (tuto) tr("Avec le tuto Facile") else null, mark?.takeIf { it.first }?.second, m,
        ) { choose(true) }
    }
    val pro: @Composable (Modifier) -> Unit = { m ->
        ModeCard(
            false, tr("Pro"), tr("Pour aller loin : tous les outils, structure, sons, mix et live."),
            if (tuto) tr("Avec le tuto Pro") else null, mark?.takeIf { !it.first }?.second, m,
        ) { choose(false) }
    }
    if (wide) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            simple(Modifier.weight(1f).fillMaxHeight())
            pro(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        simple(Modifier.fillMaxWidth())
        pro(Modifier.fillMaxWidth())
    }
}

/** One mode: its little picture, its name, who it is for and what is seen, a chevron-less big touch. */
@Composable
private fun ModeCard(simple: Boolean, title: String, line: String, tuto: String?, mark: String?, modifier: Modifier, onClick: () -> Unit) {
    val color = if (simple) SIMPLE_COLOR else Neon.Cyan
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.heightIn(min = 120.dp)
            .showcasePanel(shape, accent = color.copy(alpha = 0.85f), edge = if (mark != null) 2.dp else 1.5.dp, wash = 0.15f)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .testTag(if (simple) "mode_card_simple" else "mode_card_pro"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModePicture(simple, color)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
                if (mark != null) {
                    Text(
                        mark, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp).testTag("mode_mark"),
                    )
                }
            }
            Text(line, style = MaterialTheme.typography.bodyLarge, color = Neon.TextPrimary.copy(alpha = 0.88f))
            if (tuto != null) Text(tuto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

/**
 * A little phone screen: Simple, four big pads and four big tabs; Pro, a grid of steps, a row of
 * faders and more, smaller tabs. Drawn, so it follows the theme and the size of the text.
 */
@Composable
private fun ModePicture(simple: Boolean, color: Color) {
    val ink = Neon.TextPrimary.copy(alpha = 0.55f)
    val frame = Neon.Outline
    Box(
        Modifier.size(width = 58.dp, height = 84.dp).clip(RoundedCornerShape(10.dp)).background(Neon.Background)
            .border(1.5.dp, frame, RoundedCornerShape(10.dp)),
    ) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val w = size.width
            val h = size.height
            val r = CornerRadius(4.dp.toPx())
            if (simple) {
                val gap = 4.dp.toPx()
                val cell = (w - gap) / 2
                for (i in 0 until 4) {
                    val x = (i % 2) * (cell + gap)
                    val y = (i / 2) * (cell + gap)
                    drawRoundRect(if (i == 0) color else color.copy(alpha = 0.45f), Offset(x, y), Size(cell, cell), r)
                }
                val tabW = (w - 3 * 3.dp.toPx()) / 4
                for (i in 0 until 4) drawRoundRect(ink, Offset(i * (tabW + 3.dp.toPx()), h - 9.dp.toPx()), Size(tabW, 9.dp.toPx()), r)
            } else {
                val gap = 2.dp.toPx()
                val cols = 6
                val step = (w - (cols - 1) * gap) / cols
                for (row in 0 until 3) for (c in 0 until cols) {
                    val on = (row + c) % 3 == 0
                    drawRoundRect(if (on) color else ink.copy(alpha = 0.3f), Offset(c * (step + gap), row * (step + gap)), Size(step, step), CornerRadius(2.dp.toPx()))
                }
                val top = 3 * (step + gap) + 4.dp.toPx()
                val fadersH = h - top - 10.dp.toPx()
                listOf(0.6f, 0.9f, 0.4f, 0.75f, 0.5f, 0.85f).forEachIndexed { i, f ->
                    val x = i * (step + gap) + step / 2
                    drawLine(ink.copy(alpha = 0.4f), Offset(x, top), Offset(x, top + fadersH), 1.5.dp.toPx())
                    drawRoundRect(color, Offset(x - step / 2.5f, top + fadersH * (1 - f)), Size(step / 1.25f, 3.dp.toPx()), CornerRadius(1.dp.toPx()))
                }
                val tabW = (w - 5 * 2.dp.toPx()) / 6
                for (i in 0 until 6) drawRoundRect(ink, Offset(i * (tabW + 2.dp.toPx()), h - 6.dp.toPx()), Size(tabW, 6.dp.toPx()), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

/** [ModeStep] over the whole screen (the starts that are not on a page of their own). */
@Composable
fun ModeStepDialog(choose: (Boolean) -> Unit, back: () -> Unit, tuto: Boolean = false) {
    ScaledDialog(onDismissRequest = back, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) { ModeStep(choose, back, tuto) }
    }
}

/**
 * The top bar's chip: « Simple » or « Pro », readable and quiet (a thin outline in the mode's
 * colour); a touch opens [ModeSwitchDialog]. A 30 dp chip inside a 48 dp touch target.
 */
@Composable
fun ModeChip(simple: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = if (simple) SIMPLE_COLOR else Neon.Cyan
    val name = modeName(simple)
    Box(
        modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = tr("Changer de mode"), onClick = onClick)
            .semantics { contentDescription = tr("Mode %s. Toucher pour changer de mode.", name) }
            .testTag("top_mode"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color, maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = modeChipMax(simple)).clip(RoundedCornerShape(15.dp)).background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(15.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp).testTag("top_mode_text"),
        )
    }
}

/**
 * The widest the top bar's mode chip gets (its budget in [topBarLayout]): « Pro » is short in every
 * language, which leaves the scene its letter on a usual phone.
 */
internal fun modeChipMax(simple: Boolean) = if (simple) 72.dp else 56.dp

/**
 * « Mode de ce projet »: the two cards, the one shown marked « En ce moment », and what changes
 * (the screens and the words, never the song). A touch on the other one switches and closes.
 */
@Composable
fun ModeSwitchDialog(simple: Boolean, choose: (Boolean) -> Unit, close: () -> Unit) {
    ScaledDialog(onDismissRequest = close, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Neon.Background, shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(16.dp).widthIn(max = 560.dp)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp).testTag("mode_switch"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(tr("Mode de ce projet"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    tr("Ton morceau ne change pas : seuls les écrans et les mots changent. Ce projet garde le mode choisi."),
                    style = MaterialTheme.typography.bodyLarge, color = Neon.TextSecondary,
                )
                ModeCards(mark = simple to tr("En ce moment"), choose = { v -> if (v != simple) choose(v); close() })
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = close, modifier = Modifier.heightIn(min = 48.dp).testTag("mode_switch_close")) { Text(tr("Fermer")) }
                }
            }
        }
    }
}
