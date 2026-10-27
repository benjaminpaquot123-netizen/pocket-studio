package com.pocketstudio.app.midi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.midi.Control
import com.pocketstudio.model.midi.ControlButton
import com.pocketstudio.ui.MonoStyle
import com.pocketstudio.ui.Neon
import com.pocketstudio.ui.NeonCard
import com.pocketstudio.ui.ScaledPopup
import com.pocketstudio.ui.SectionLabel
import com.pocketstudio.ui.pillColors

/** What « Contrôleur MIDI » shows. */
class ControllerCardUi(
    /** The controller connected (null: none). */
    val controller: ControllerInfo? = null,
    val monitor: List<String> = emptyList(),
    /** The screen whose legend is shown, and its rows. */
    val screen: KnobScreen = KnobScreen.Rythme,
    val legend: List<LegendRow> = emptyList(),
    val legendOn: Boolean = true,
    /** The user changed something for this controller (its own profile). */
    val customized: Boolean = false,
    /** « Apprendre mes pads »: pads heard so far (null: not learning). */
    val learningPads: Int? = null,
    /** « Apprendre » of a fader or a button waiting for it. */
    val learningControl: Control? = null,
)

class ControllerCardActions(
    val pickScreen: (KnobScreen) -> Unit = {},
    val setLegend: (Boolean) -> Unit = {},
    /** « Apprendre mes pads » (true) or « Terminé » (false). */
    val learnPads: (Boolean) -> Unit = {},
    /** « Apprendre » a fader or a button (null: cancel). */
    val learnControl: (Control?) -> Unit = {},
    val forget: () -> Unit = {},
    val openKnobs: () -> Unit = {},
)

/** The words for a control being learnt. */
fun controlName(c: Control): String = when (c) {
    is Control.Knob -> tr("Potard %d", c.index + 1)
    is Control.Fader -> tr("Fader %d", c.index + 1)
    is Control.Button -> when (c.button) {
        ControlButton.ENCODER -> tr("Gros bouton")
        ControlButton.ENCODER_SHIFT -> tr("Gros bouton avec Shift")
        ControlButton.CLICK -> tr("Clic du gros bouton")
        ControlButton.CLICK_SHIFT -> tr("Clic avec Shift")
        ControlButton.SHIFT -> tr("Shift")
        ControlButton.PLAY -> tr("Lecture")
        ControlButton.STOP -> tr("Arrêt")
        ControlButton.RECORD -> tr("Enregistrer")
    }
}

/**
 * « Contrôleur MIDI » (Plus › Clavier MIDI): the controller found and the app's profile for it
 * (« prêt à jouer »), what each control does on each screen, « Apprendre » for the pads, the
 * faders, the main encoder and the transport, the monitor of what arrives, and the switch of the
 * small legend on screen.
 */
@Composable
fun ControllerCard(ui: ControllerCardUi, actions: ControllerCardActions) {
    NeonCard(modifier = Modifier.testTag("controller_card")) {
        SectionLabel(tr("Contrôleur MIDI"))
        val c = ui.controller
        if (c == null) {
            Text(tr("Aucun contrôleur branché. Branche ton clavier en USB : il est reconnu tout seul."), style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.Acid, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val profile = c.profile
            Text(
                when {
                    profile != null && ui.customized -> tr("Profil : %s, avec tes changements", profile.name)
                    profile != null -> tr("Profil : %s — prêt à jouer, rien à régler", profile.name)
                    ui.customized -> tr("Profil : le tien (appris sur cet appareil)")
                    else -> tr("Profil : clavier générique — tes potards se rangent dans l'ordre où tu les tournes")
                },
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("controller_profile"),
            )
        }
        // The legend of a screen: what each control does there.
        Text(tr("Ce que fait chaque commande"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LEGEND_SCREENS.forEach { s ->
                FilterChip(
                    selected = s == ui.screen, onClick = { actions.pickScreen(s) }, label = { Text(s.label, maxLines = 1) },
                    colors = pillColors(), modifier = Modifier.testTag("controller_screen_${s.name}"),
                )
            }
        }
        ui.legend.forEach { row -> LegendLine(row) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Aide à l'écran"), style = MaterialTheme.typography.bodyLarge)
                Text(tr("Un petit rappel de ce que fait ton contrôleur sur l'écran affiché."), style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
            }
            Switch(checked = ui.legendOn, onCheckedChange = actions.setLegend, modifier = Modifier.testTag("controller_legend_switch"))
        }
        if (c != null) {
            // « Apprendre »: the pads, then a fader or a button.
            if (ui.learningPads != null) {
                Text(
                    tr("Tape tes pads l'un après l'autre (%d appris)…", ui.learningPads),
                    color = Neon.Amber, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("controller_learning_pads"),
                )
                OutlinedButton(onClick = { actions.learnPads(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("controller_pads_done")) { Text(tr("Terminé")) }
            } else {
                OutlinedButton(onClick = { actions.learnPads(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("controller_learn_pads")) {
                    Text(tr("Apprendre mes pads"))
                }
            }
            val learning = ui.learningControl
            if (learning != null) {
                Text(tr("Bouge maintenant « %s » sur ton contrôleur…", controlName(learning)), color = Neon.Amber, modifier = Modifier.testTag("controller_learning"))
                TextButton(onClick = { actions.learnControl(null) }) { Text(tr("Annuler")) }
            } else {
                Text(tr("Apprendre une autre commande :"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LEARNABLE.forEach { ctl ->
                        OutlinedButton(onClick = { actions.learnControl(ctl) }, modifier = Modifier.heightIn(min = 48.dp).testTag("controller_learn_${learnTag(ctl)}")) {
                            Text(controlName(ctl), maxLines = 1)
                        }
                    }
                }
            }
            OutlinedButton(onClick = actions.openKnobs, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("controller_open_knobs")) { Text(tr("Voir et changer les potards")) }
            if (ui.customized) {
                TextButton(onClick = actions.forget, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("controller_forget")) { Text(tr("Oublier mes réglages pour cet appareil")) }
            }
        }
        // The monitor: what arrives, newest first.
        Text(tr("Ce qui arrive"), style = MaterialTheme.typography.labelLarge, color = Neon.TextSecondary)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Neon.Background).padding(8.dp).testTag("controller_monitor"),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (ui.monitor.isEmpty()) Text(tr("Touche une note, un pad ou un potard…"), style = MonoStyle, color = Neon.TextSecondary)
            ui.monitor.forEachIndexed { i, line -> Text(line, style = MonoStyle, color = if (i == 0) Neon.Cyan else Neon.TextSecondary, maxLines = 1) }
        }
    }
}

@Composable
private fun LegendLine(row: LegendRow) {
    Row(Modifier.fillMaxWidth().testTag("legend_${row.tag}"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(row.control, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.width(LEGEND_LABEL))
        Text(row.does, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** The legend's first column: the same width on every line, so what each control does lines up. */
private val LEGEND_LABEL = 124.dp

/** The screens the legend can show, in the app's order. */
val LEGEND_SCREENS = listOf(
    KnobScreen.Rythme, KnobScreen.Acide, KnobScreen.Synthe, KnobScreen.Mixeur, KnobScreen.Live, KnobScreen.LiveAcide, KnobScreen.LivePads, KnobScreen.Piste,
)

/** The controls « Apprendre » offers besides the knobs (the knobs have their own list). */
val LEARNABLE: List<Control> = (0 until 4).map { Control.Fader(it) } + listOf(
    ControlButton.ENCODER, ControlButton.CLICK, ControlButton.PLAY, ControlButton.STOP, ControlButton.RECORD,
).map { Control.Button(it) }

private fun learnTag(c: Control): String = when (c) {
    is Control.Fader -> "fader_${c.index}"
    is Control.Button -> c.button.name.lowercase()
    is Control.Knob -> "knob_${c.index}"
}

/**
 * The small legend on screen while a controller is connected: a pill with its name; a touch opens
 * what each control does on this screen (another touch, or ✕, closes it). It never takes the
 * touches of what is under it but its own.
 */
@Composable
fun ControllerLegendOverlay(name: String, screen: KnobScreen, rows: List<LegendRow>, expanded: Boolean, toggle: () -> Unit) {
    ScaledPopup(alignment = Alignment.BottomStart, properties = PopupProperties(focusable = false)) {
        Box(Modifier.navigationBarsPadding().padding(start = 8.dp, bottom = 92.dp)) {
            if (!expanded) LegendPill(name, toggle) else LegendPanel(name, screen, rows, toggle)
        }
    }
}

/** The legend folded: the controller's name in a pill. */
@Composable
fun LegendPill(name: String, toggle: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(Neon.Surface).border(1.dp, Neon.Outline, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = toggle).heightIn(min = 32.dp).padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("controller_legend_pill"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Piano, contentDescription = null, tint = Neon.Acid, modifier = Modifier.size(16.dp))
        Text("  " + name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
    }
}

/** The legend open: what each control does on [screen]. */
@Composable
fun LegendPanel(name: String, screen: KnobScreen, rows: List<LegendRow>, toggle: () -> Unit) {
    Column(
        Modifier.widthIn(max = 420.dp).clip(RoundedCornerShape(16.dp)).background(Neon.Surface).border(1.dp, Neon.Outline, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = toggle).padding(12.dp).testTag("controller_legend"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("%1\$s · %2\$s", name, screen.label), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Filled.Close, contentDescription = tr("Fermer"), tint = Neon.TextSecondary, modifier = Modifier.size(18.dp))
        }
        rows.forEach { LegendLine(it) }
    }
}

/** What a button, the encoder or a pad just did (« Tempo 124 », « Ligne B »), for a moment. */
@Composable
fun ControllerFlash(text: String) {
    ScaledPopup(alignment = Alignment.TopCenter, properties = PopupProperties(focusable = false)) {
        Box(Modifier.padding(top = 72.dp)) {
            Text(
                text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Neon.TextPrimary,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Neon.SurfaceHigh).border(1.dp, Neon.Cyan, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp).testTag("controller_flash"),
            )
        }
    }
}
