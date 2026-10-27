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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pocketstudio.app.learn.LocalLearn
import com.pocketstudio.app.learn.spot
import com.pocketstudio.model.guide.GuideStages
import com.pocketstudio.model.guide.GuideStatus
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.learn.SoundGlossary
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.Neon

/** What the Guide's bar and panel do (each one a call of [GuideViewModel], [GuideListen] or the app's navigation). */
class GuidePanelActions(
    /** Opens or folds the panel. */
    val toggle: () -> Unit = {},
    /** Option [option] of card [card]: applied, or its screen opened (« Voir le réglage »). */
    val apply: (card: String, option: String) -> Unit = { _, _ -> },
    /** « Voir le réglage »: option [option] of card [card] opens its screen; the card stays the one shown. */
    val open: (card: String, option: String) -> Unit = apply,
    /** ▶ of an option: heard without then with it. */
    val listen: (card: String, option: String) -> Unit = { _, _ -> },
    val later: (card: String) -> Unit = {},
    val skip: (card: String) -> Unit = {},
    /** « Ne plus proposer »: refused for good, remembered in the project. */
    val refuse: (card: String) -> Unit = {},
    /** « Annuler mon choix ». */
    val undo: (card: String) -> Unit = {},
    /** A card of the plan shown (null: back to the next one). */
    val select: (card: String?) -> Unit = {},
    val dismissNotice: () -> Unit = {},
    /** G3: « La fiche de la piste » of track (its Guide key). */
    val sheet: (track: String) -> Unit = {},
    /** G4: a card of the levels shows: the song is measured (when the music is stopped). */
    val measure: () -> Unit = {},
    /** G4: another reference to compare with (« À côté d'un morceau que tu aimes »). */
    val pickReference: () -> Unit = {},
    /** G5 review: the « Sans / Avec » stops (a word's example starts: one sound at a time). */
    val stopListen: () -> Unit = {},
)

/** The symbol of a card's status in the plan (« faite ✓, sautée ⟶, à revoir ↺ »). */
fun guideMark(status: GuideStatus): String = when (status) {
    GuideStatus.DONE, GuideStatus.DONE_ELSEWHERE -> "✓"
    GuideStatus.SKIPPED -> "⟶"
    GuideStatus.LATER -> "↺"
    GuideStatus.TODO -> "○"
}

/** Where the Guide is, in a few words: « Étape 12/34 : la basse au Drop 2 », an alert, or all done. */
fun guideStepLine(ui: GuideUi): String {
    val next = ui.next
    return when {
        ui.loading -> ""
        next == null -> tr("Tout est fait")
        next.alert || ui.step == 0 -> tr("À corriger : %s", next.title)
        else -> tr("Étape %1\$d/%2\$d : %3\$s", ui.step, ui.total, next.title)
    }
}

/**
 * The Guide's bar (docs/PLAN-GUIDE.md §3.1), right above the spaces on every screen where the song
 * is made: « 🧭 Guide · Étape 12/34 : la basse au Drop 2 ». A touch opens the panel (the bar is
 * then its title), another folds it. Never over what is edited: it takes its own line.
 */
@Composable
fun GuideBar(ui: GuideUi, open: Boolean, toggle: () -> Unit, modifier: Modifier = Modifier) {
    val line = guideStepLine(ui)
    val said = if (open) tr("Replier le Guide") else tr("Ouvrir le Guide")
    Row(
        modifier.fillMaxWidth()
            .background(lerp(Neon.Background, Color.Black, 0.1f))
            .drawBehind { drawLine(Color.White.copy(alpha = 0.08f), Offset(0f, 0f), Offset(size.width, 0f), 1f) }
            .clickable(role = Role.Button, onClickLabel = said, onClick = toggle)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .heightIn(min = 48.dp)
            .padding(start = 12.dp, end = 8.dp)
            .semantics { stateDescription = if (open) tr("ouvert") else tr("replié") }
            .spot("guide_bar", tr("Le Guide : où en est ton morceau"))
            .testTag("guide_bar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Explore, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(20.dp))
        Text(
            tr("Guide"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan, maxLines = 1,
            modifier = Modifier.padding(start = 6.dp),
        )
        Text(
            if (line.isEmpty()) "" else " · $line", style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("guide_bar_step"),
        )
        if (!ui.loading && ui.total > 0) {
            Text(
                "${ui.percent} %", style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary, maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        Icon(if (open) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.size(22.dp))
    }
}

/**
 * The Guide's panel (§3.1): its bar on top, then « Où on en est » (the song, its progress, and the
 * three lines that welcome a return), the card (the next decision, or the one picked in the plan)
 * and the plan, folded by stage; in the Mode simple, the other stages under « Aller plus loin ».
 * [height]: about a third of the screen on a phone (40 %), half on a tablet or lying down; it scrolls inside. Nothing in it
 * is modal: the music and the screen above go on.
 */
@Composable
fun GuidePanel(
    ui: GuideUi,
    selected: String?,
    listen: GuideListenUi?,
    notice: String?,
    actions: GuidePanelActions,
    /** Null: as high as [modifier] lets it (lying down, on the side of the screen). */
    height: Dp?,
    modifier: Modifier = Modifier,
    /** G4: the reference's file name, when one is picked. */
    reference: String? = null,
) {
    val sized = if (height != null) modifier.fillMaxWidth().height(height) else modifier
    Column(sized.background(Neon.Background).testTag("guide_panel")) {
        GuideBar(ui, open = true, toggle = actions.toggle)
        val scroll = rememberScrollState()
        // Another card shown (decided, picked in the plan): back to the top, where the card is.
        val shownId = if (ui.loading) null else (selected?.let { ui.cards[it] } ?: ui.alerts.firstOrNull() ?: ui.next)?.id
        androidx.compose.runtime.LaunchedEffect(shownId) { scroll.animateScrollTo(0) }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight().verticalScroll(scroll)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (notice != null) NoticeLine(notice, actions.dismissNotice)
                if (ui.loading) {
                    Text(tr("Le Guide lit ton morceau…"), color = Neon.TextSecondary, modifier = Modifier.testTag("guide_loading"))
                } else {
                    PanelBody(ui, selected, listen, actions, reference)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PanelBody(ui: GuideUi, selected: String?, listen: GuideListenUi?, actions: GuidePanelActions, reference: String?) {
                // The bar says the step and how far; « Où on en est » in full comes first only to welcome a
                // return after days, else under the card (the decision stays in sight on a phone).
                if (ui.back) WhereWeAre(ui)
                val shown = selected?.let { ui.cards[it] } ?: ui.alerts.firstOrNull() ?: ui.next
                if (shown != null) {
                    val picked = selected != null && shown.id != (ui.alerts.firstOrNull() ?: ui.next)?.id
                    // G4: a card of the levels needs the song's measures (each track alone, mono, phone).
                    androidx.compose.runtime.LaunchedEffect(shown.id, ui.measured) { if (shown.stage == 6 && !ui.measured) actions.measure() }
                    GuideCard(shown, listen, actions, picked = picked, measuring = ui.measuring, reference = reference)
                } else {
                    Text(ui.summary.firstOrNull() ?: tr("Tout est fait"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("guide_all_done"))
                }
                if (!ui.back) WhereWeAre(ui)
                GuidePlan(ui.plan, shown?.id, actions.select, tag = "guide_plan")
                if (ui.further.isNotEmpty()) FurtherPlan(ui.further, shown?.id, actions.select)
}

/** A thing the Guide could not do, and why, in a line that goes with ✕. */
@Composable
private fun NoticeLine(text: String, dismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.Amber.copy(alpha = 0.18f)).border(1.dp, Neon.Amber, RoundedCornerShape(10.dp))
            .padding(start = 10.dp).testTag("guide_notice"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
        Box(
            Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = tr("Fermer"), onClick = dismiss).testTag("guide_notice_close"),
            contentAlignment = Alignment.Center,
        ) { Text("✕", color = Neon.TextSecondary) }
    }
}

/**
 * « Où on en est » (§3.1, §3.6): the song in a line, how far it is, and the three lines (« Tu en
 * étais à… », « Fait : … », « Reste : … ») folded under it; open by themselves when the Guide comes
 * back after a day or more.
 */
@Composable
private fun WhereWeAre(ui: GuideUi) {
    var open by rememberSaveable(ui.back) { mutableStateOf(ui.back) }
    Column(Modifier.fillMaxWidth().testTag("guide_where"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // One line: how far the song is; a touch says the rest (the card stays in sight under it).
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClickLabel = if (open) tr("Replier") else tr("Où on en est")) { open = !open }
                .heightIn(min = 48.dp).padding(horizontal = 4.dp).testTag("guide_where_toggle"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(tr("Où on en est"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan, maxLines = 1)
            Box(Modifier.weight(1f)) { Progress(ui.percent) }
            Text(
                tr("%d à décider", ui.toDecide), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary,
                maxLines = 1, modifier = Modifier.testTag("guide_progress"),
            )
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
        }
        if (open) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.Surface).padding(10.dp).testTag("guide_summary"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(ui.header, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
                ui.summary.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary) }
            }
        }
    }
}

@Composable
private fun Progress(percent: Int) {
    val f = (percent / 100f).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Neon.SurfaceHigh)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Neon.Acid))
    }
}

/**
 * One card, one decision (§3.2): its stage, its title, its why; each option to choose (▶ to hear
 * it without then with it first, at the same loudness, when it changes the song); « Voir le
 * réglage » for the options that open a screen; then « Plus tard », « Sauter », « En savoir plus »,
 * « Annuler mon choix » once applied, « Ne plus proposer ».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuideCard(
    card: GuideCardUi, listen: GuideListenUi?, actions: GuidePanelActions, modifier: Modifier = Modifier, picked: Boolean = false,
    measuring: Boolean = false, reference: String? = null,
) {
    val learn = LocalLearn.current
    NeonCard(modifier.testTag("guide_card_${card.id}"), accent = if (card.alert) Neon.Amber else Neon.Cyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (card.alert) tr("À corriger") else if (card.stage == GuideStages.START) GuideStages.title(card.stage)
                // Audit final: a stage of the plan is a « chapitre » (« Étape n/N » counts the cards).
                else tr("Chapitre %1\$d · %2\$s", card.stage, GuideStages.title(card.stage))).uppercase(),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (card.alert) Neon.Amber else Neon.Cyan,
                modifier = Modifier.weight(1f),
            )
            if (card.status != GuideStatus.TODO) {
                Text(
                    guideMark(card.status) + " " + statusWord(card.status), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary,
                    modifier = Modifier.testTag("guide_card_status"),
                )
            }
        }
        Text(card.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary)
        if (card.why.isNotBlank()) Text(card.why, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        // G4: what the Guide hears in the song about this card (its measures, its analysis).
        if (measuring && card.stage == 6) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("guide_measuring")) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Neon.Cyan)
                Text(tr("Le Guide écoute chaque piste…"), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(start = 8.dp))
            }
        }
        card.heard.forEach { line ->
            Row(Modifier.testTag("guide_heard"), verticalAlignment = Alignment.Top) {
                Text("•", color = Neon.Cyan, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(14.dp))
                Text(line, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary)
            }
        }
        val choices = card.options.filter { it.opens == null }
        val opens = (card.options + card.elsewhere).filter { it.opens != null }
        choices.forEachIndexed { i, o -> OptionRow(card.id, o, primary = i == 0, listen, actions) }
        // Under the decision (it stays two gestures away), effect by effect.
        if (card.effects.isNotEmpty()) EffectsList(card.id, card.effects, listen, actions)
        if (reference != null && card.options.any { it.listen == "reference" }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Morceau : %s", reference), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                SmallAction(tr("Changer"), "guide_reference_change") { actions.pickReference() }
            }
        }
        val elsewhere = card.elsewhere.filter { it.opens == null && !it.refused }
        if (elsewhere.isNotEmpty()) {
            Text(tr("Ailleurs ?").uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Neon.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            elsewhere.forEach { o -> OptionRow(card.id, o, primary = false, listen, actions) }
        }
        if (opens.isNotEmpty() || card.sheet != null) {
            Text(tr("Voir le réglage").uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Neon.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            // G3: the track's whole sheet (its sound and layers, when, effects, history), one touch away.
            card.sheet?.let { key ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = tr("Voir le réglage")) { actions.sheet(key) }
                        .heightIn(min = 48.dp).padding(horizontal = 4.dp)
                        .testTag("guide_sheet"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("↗", color = Neon.Cyan, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp))
                    Text(tr("La fiche de la piste « %s »", card.sheetName), style = MaterialTheme.typography.bodyLarge, color = Neon.Cyan, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
            opens.forEach { o ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClickLabel = tr("Voir le réglage")) { actions.open(card.id, o.id) }
                        .heightIn(min = 48.dp).padding(horizontal = 4.dp)
                        .semantics(mergeDescendants = true) { contentDescription = tr("Voir le réglage : %s", o.label) }
                        .testTag("guide_open_${o.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("↗", color = Neon.Cyan, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(24.dp))
                    Text(o.label, style = MaterialTheme.typography.bodyLarge, color = Neon.Cyan, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
            }
        }
        val word = SoundGlossary.ALL.firstOrNull { it.key == card.lexicon }
        // G5: the card's word, heard and said in a line before the decision (« pré-entraînement »); the glossary one touch further.
        if (word != null) WordLine(word, learn, actions.stopListen)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 4.dp)) {
            // A card picked in the plan: the way back to the one in progress.
            if (picked) SmallAction("← " + tr("Revenir à l'étape en cours"), "guide_back_to_next") { actions.select(null) }
            if (!card.status.settled && card.status != GuideStatus.LATER) SmallAction(tr("Plus tard"), "guide_later") { actions.later(card.id) }
            if (!card.status.settled) SmallAction(tr("Sauter"), "guide_skip") { actions.skip(card.id) }
            if (word != null) SmallAction(tr("En savoir plus"), "guide_learn") { learn.open(word.term) }
            if (card.undoable) SmallAction(tr("Annuler mon choix"), "guide_undo") { actions.undo(card.id) }
            if (card.status != GuideStatus.SKIPPED) SmallAction(tr("Ne plus proposer"), "guide_refuse") { actions.refuse(card.id) }
        }
    }
}

/**
 * The word a card is about (« Sidechain (pompe) »), what it means in a line, and ▶ its example (the
 * glossary's): heard before deciding, not after. A touch on the line opens the glossary on it.
 */
@Composable
private fun WordLine(word: com.pocketstudio.model.learn.SoundWord, learn: com.pocketstudio.app.learn.LearnHost, stopListen: () -> Unit) {
    val playing by learn.playing.collectAsStateWithLifecycle()
    val mine = playing?.takeIf { it.key == word.key }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.SurfaceHigh.copy(alpha = 0.6f)).testTag("guide_word"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = if (mine != null) tr("Arrêter") else tr("Écouter un exemple")) { stopListen(); learn.toggle(word.key) }
                .semantics { contentDescription = tr("Écouter un exemple : %s", word.term) }
                .testTag("guide_word_listen"),
            contentAlignment = Alignment.Center,
        ) {
            when {
                mine?.preparing == true -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Neon.Cyan)
                mine != null -> Icon(Icons.Filled.Stop, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(20.dp))
                else -> Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.Cyan, modifier = Modifier.size(22.dp))
            }
        }
        Column(
            Modifier.weight(1f).clickable(role = Role.Button, onClickLabel = tr("En savoir plus")) { learn.open(word.term) }
                .padding(end = 10.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(tr("Le mot : %s", word.term), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Neon.Cyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(word.text, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun statusWord(status: GuideStatus): String = when (status) {
    GuideStatus.DONE -> tr("fait")
    GuideStatus.DONE_ELSEWHERE -> tr("déjà en place")
    GuideStatus.SKIPPED -> tr("sauté")
    GuideStatus.LATER -> tr("à revoir")
    GuideStatus.TODO -> ""
}

/** An option: ▶ (heard without then with it) when it changes the song, its button, its why and starting value. */
@Composable
private fun OptionRow(card: String, o: GuideOptionUi, primary: Boolean, listen: GuideListenUi?, actions: GuidePanelActions) {
    val mine = listen?.takeIf { it.card == card && it.option == o.id }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (o.heard || o.listen != null) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = if (mine != null) tr("Arrêter") else tr("Écouter sans puis avec")) { actions.listen(card, o.id) }
                        .semantics { contentDescription = tr("Écouter « %s » : sans, puis avec", o.label) }
                        .testTag("guide_listen_${o.id}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(if (mine != null && !mine.failed) Neon.Acid else Neon.SurfaceHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            mine?.preparing == true -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Neon.TextPrimary)
                            mine != null && !mine.failed -> Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            else -> Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
            }
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(shape)
                    .background(if (primary) Neon.Cyan else Color.Transparent)
                    .border(1.dp, if (primary) Neon.Cyan else Neon.Outline, shape)
                    // A check is heard, not applied: the option itself plays it.
                    .clickable(role = Role.Button) { if (o.listen != null) actions.listen(card, o.id) else actions.apply(card, o.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("guide_option_${o.id}"),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    o.label + (o.value?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                    color = if (primary) Neon.OnAccent else Neon.TextPrimary,
                )
            }
        }
        when {
            mine?.failed == true -> Text(
                tr("Écoute impossible ici : applique, écoute, et « Annuler mon choix » si ça ne va pas."),
                style = MaterialTheme.typography.bodySmall, color = Neon.Amber, modifier = Modifier.testTag("guide_listen_failed"),
            )
            mine != null && !mine.preparing -> Takes(mine)
            o.why.isNotBlank() -> Text(o.why, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.padding(start = if (o.heard || o.listen != null) 54.dp else 4.dp))
        }
    }
}

/** The two takes as they play (« Sans ▸ Avec », « Stéréo ▸ Mono »…), the one heard lit, and what the Guide says of them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Takes(mine: GuideListenUi) {
    Column(Modifier.testTag("guide_listen_takes"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(mine.labels[0], style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (mine.take == 0) Neon.Acid else Neon.TextSecondary)
            Text("▸", color = Neon.TextSecondary)
            Text(mine.labels[1], style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (mine.take == 1) Neon.Acid else Neon.TextSecondary)
            Text(if (mine.matched) tr("au même volume") else tr("tel quel"), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary)
        }
        mine.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, modifier = Modifier.testTag("guide_listen_note")) }
    }
}

/**
 * G4: « Voilà ce que j'ai mis », effect by effect (docs/PLAN-GUIDE.md §3.5, stage 3): each track's
 * effects under its name, each with its why, ▶ to hear it without (or with, when set aside) at the
 * same loudness, and « Retirer » / « Le remettre » (its settings kept, « Annuler mon choix » too).
 */
@Composable
private fun EffectsList(card: String, effects: List<GuideFxUi>, listen: GuideListenUi?, actions: GuidePanelActions) {
    val groups = effects.groupBy { it.track }
    // Audit final: several tracks (« Voilà ce que j'ai mis »): each one folded on a line, so the card's choice
    // (« Tout garder ») stays in sight; one track (its own card): its effects shown.
    val folds = groups.size > 1
    var openTrack by rememberSaveable(card) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(top = 2.dp).testTag("guide_effects"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        groups.forEach { (track, list) ->
            if (folds) {
                val open = openTrack == track
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClickLabel = if (open) tr("Replier") else tr("Déplier")) { openTrack = if (open) null else track }
                        .heightIn(min = 48.dp).padding(horizontal = 4.dp).testTag("guide_fx_track_$track"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(list.first().trackName.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Neon.Cyan)
                        Text(
                            list.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
                }
                if (!open) return@forEach
            } else {
                Text(list.first().trackName.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Neon.Cyan, modifier = Modifier.padding(top = 4.dp))
            }
            list.forEach { fx ->
                val mine = listen?.takeIf { it.card == card && it.option == fx.option }
                Row(Modifier.fillMaxWidth().testTag("guide_fx_${fx.option}"), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = if (mine != null) tr("Arrêter") else tr("Écouter sans puis avec")) { actions.listen(card, fx.option) }
                            .semantics { contentDescription = tr("Écouter « %s » : sans, puis avec", fx.name) }
                            .testTag("guide_fx_listen_${fx.option}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if (mine != null && !mine.failed) Neon.Acid else Neon.SurfaceHigh), contentAlignment = Alignment.Center) {
                            when {
                                mine?.preparing == true -> CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Neon.TextPrimary)
                                mine != null && !mine.failed -> Icon(Icons.Filled.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                else -> Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Neon.TextPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            fx.name + if (fx.off) " · " + tr("retiré") else "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
                            color = if (fx.off) Neon.TextSecondary else Neon.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        when {
                            mine?.failed == true -> Text(tr("Écoute impossible ici."), style = MaterialTheme.typography.bodySmall, color = Neon.Amber)
                            mine != null && !mine.preparing -> Takes(mine)
                            fx.why.isNotBlank() -> Text(fx.why, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    SmallAction(if (fx.off) tr("Le remettre") else tr("Retirer"), "guide_fx_toggle_${fx.option}") { actions.apply(card, fx.option) }
                }
            }
        }
    }
}

@Composable
private fun SmallAction(text: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, style = MaterialTheme.typography.labelLarge, color = Neon.TextPrimary,
            modifier = Modifier.border(1.dp, Neon.Outline, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

/** The plan (§3.1): each stage on a line (✓ once settled, else how many are done), opened to its cards by a touch. */
@Composable
private fun GuidePlan(stages: List<GuideStageUi>, current: String?, select: (String) -> Unit, tag: String) {
    var openStage by rememberSaveable(tag) { mutableStateOf<Int?>(null) }
    val currentStage = stages.firstOrNull { s -> s.items.any { it.id == current } }?.stage
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (tag == "guide_plan") {
            Text(tr("Le plan").uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Neon.Cyan, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
        }
        stages.forEach { s ->
            val open = openStage == s.stage
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClickLabel = if (open) tr("Replier") else tr("Déplier")) { openStage = if (open) null else s.stage }
                    .heightIn(min = 48.dp).padding(horizontal = 4.dp)
                    .testTag("guide_stage_${s.stage}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (s.settled) "✓" else "${s.done}/${s.items.size}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = if (s.settled) Neon.Acid else if (s.stage == currentStage) Neon.Cyan else Neon.TextSecondary, modifier = Modifier.width(44.dp),
                )
                Text(
                    s.title, style = MaterialTheme.typography.bodyLarge, color = if (s.stage == currentStage) Neon.Cyan else Neon.TextPrimary,
                    fontWeight = if (s.stage == currentStage) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f),
                )
                Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.TextSecondary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
            }
            if (open) s.items.forEach { item ->
                val here = item.id == current
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { select(item.id) }
                        .heightIn(min = 44.dp).padding(start = 44.dp, end = 4.dp)
                        .testTag("guide_item_${item.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (here) "●" else guideMark(item.status), color = if (item.status.done) Neon.Acid else if (here) Neon.Cyan else Neon.TextSecondary,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        item.title + if (item.auto) " · " + tr("déjà en place") else "", style = MaterialTheme.typography.bodyMedium,
                        color = if (here) Neon.Cyan else if (item.status.settled) Neon.TextSecondary else Neon.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The Mode simple's « Aller plus loin » (§3.7): the stages it leaves out, folded, each card still reachable. */
@Composable
private fun FurtherPlan(stages: List<GuideStageUi>, current: String?, select: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().testTag("guide_further")) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { open = !open }
                .heightIn(min = 48.dp).padding(horizontal = 4.dp).testTag("guide_further_toggle"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(tr("Aller plus loin"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Neon.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                tr("%d étapes de plus", stages.sumOf { it.items.size }), style = MaterialTheme.typography.labelMedium, color = Neon.TextSecondary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
        }
        if (open) GuidePlan(stages, current, select, tag = "guide_further_plan")
    }
}

/** « Voir le réglage »: next to the control lit, what it is (the Guide's card waits in its bar). */
@Composable
fun GuideSpotCard(close: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.padding(16.dp).widthIn(max = 480.dp)) {
        NeonCard(Modifier.testTag("guide_spot"), accent = Neon.Amber) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Explore, contentDescription = null, tint = Neon.Amber, modifier = Modifier.size(20.dp))
                Text(
                    tr("Le réglage est ici. Le Guide t'attend dans sa barre, en bas."), style = MaterialTheme.typography.bodyLarge, color = Neon.TextPrimary,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }
            Box(
                Modifier.align(Alignment.End).heightIn(min = 48.dp).clip(RoundedCornerShape(20.dp)).background(Neon.Amber)
                    .clickable(role = Role.Button, onClick = close).padding(horizontal = 18.dp).testTag("guide_spot_close"),
                contentAlignment = Alignment.Center,
            ) { Text(tr("Compris"), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.Black) }
        }
    }
}
