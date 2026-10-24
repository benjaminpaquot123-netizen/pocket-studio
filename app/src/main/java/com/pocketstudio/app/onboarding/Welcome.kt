package com.pocketstudio.app.onboarding

import com.pocketstudio.model.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketstudio.ui.Neon

/** The three ways to start offered at the first launch, and "later" (stay on the first project). */
class WelcomeActions(
    val guided: () -> Unit = {},
    val demo: () -> Unit = {},
    val empty: () -> Unit = {},
    val later: () -> Unit = {},
    /** Phase 36: the Mode simple (true) or every tool (false), chosen before starting. */
    val chooseMode: (Boolean) -> Unit = {},
)

/**
 * The first launch: one welcome line and three big choices (the guided song, a demo to listen
 * to, an empty project) instead of pages of text. Shown while [show]; once a demo is chosen and
 * opened (the activity rebuilds), a short dialog offers to play it ([playDemo]).
 */
@Composable
fun FirstLaunch(show: Boolean, actions: WelcomeActions, playDemo: () -> Unit, simple: Boolean = false) {
    // Kept through the rebuild that opening the demo does.
    var demoReady by rememberSaveable(key = "demo_ready") { mutableStateOf(false) }
    // A project is being opened (the activity rebuilds then): the choices stay until it is.
    var leaving by remember { mutableStateOf(false) }
    if (show || leaving) {
        Dialog(onDismissRequest = { leaving = false; demoReady = false; actions.later() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
            Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                WelcomeScreen(
                    WelcomeActions(
                        guided = actions.guided,
                        // A second quick touch while the first project opens makes no second one.
                        demo = { if (!leaving) { demoReady = true; leaving = true; actions.demo() } },
                        empty = { if (!leaving) { leaving = true; actions.empty() } },
                        later = { leaving = false; demoReady = false; actions.later() },
                        chooseMode = actions.chooseMode,
                    ),
                    simple = simple,
                )
            }
        }
    } else if (demoReady) {
        AlertDialog(
            onDismissRequest = { demoReady = false },
            title = { Text(tr("Ta démo est prête")) },
            text = {
                Text(tr("Écoute-la en entier, puis va voir dans Structure comment elle est construite : tu peux tout changer."))
            },
            confirmButton = {
                Button(onClick = { demoReady = false; playDemo() }, modifier = Modifier.testTag("demo_play")) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(" " + tr("Écouter"))
                }
            },
            dismissButton = { TextButton(onClick = { demoReady = false }) { Text(tr("Plus tard")) } },
            modifier = Modifier.testTag("demo_ready"),
        )
    }
}

/** The choice screen itself: the guided song first and biggest, then the demo, then the empty project. */
@Composable
fun WelcomeScreen(actions: WelcomeActions, simple: Boolean = true) {
    // Steps to read: centred at a comfortable width on a tablet.
    com.pocketstudio.ui.ReadingWidth(maxWidth = 600.dp) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp).testTag("welcome"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Pocket Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Neon.Cyan)
            // The language first: French is ticked, the other ones are one tap away.
            com.pocketstudio.app.settings.LanguagePicker()
            // Phase 36: how the app is laid out, before anything (Réglages changes it later, nothing is lost).
            ModeChoice(simple, actions.chooseMode)
            Text(tr("Bienvenue ! Fais ta musique sur ton appareil. Par quoi on commence ?"), style = MaterialTheme.typography.bodyLarge)
            Choice(
                Icons.Filled.AutoAwesome, tr("Créer mon premier morceau (guidé)"),
                tr("Choisis un style, je t'aide pas à pas : intro, couplet, refrain."),
                Neon.Cyan, primary = true, onClick = actions.guided, tag = "welcome_guided",
            )
            Choice(
                Icons.Filled.Headphones, tr("Écouter une démo"),
                tr("Un morceau déjà prêt, pour découvrir ce qu'on peut faire."),
                Color(0xFF5BB8F5), onClick = actions.demo, tag = "welcome_demo",  // sky blue: green means "playing"
            )
            Choice(
                Icons.Filled.Add, tr("Partir de zéro"),
                tr("Un projet vide : à toi de jouer."),
                Neon.TextSecondary, onClick = actions.empty, tag = "welcome_empty",
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = actions.later, modifier = Modifier.heightIn(min = 48.dp).testTag("welcome_later")) {
                    Text(tr("Plus tard : voir le rythme de départ"), color = Neon.TextSecondary)
                }
            }
            Text(tr("Le guide des premiers pas reste dans Plus › Aide."), style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** A big choice: a round icon, a title and one line; [primary] filled with its colour. */
@Composable
private fun Choice(icon: ImageVector, title: String, line: String, color: Color, primary: Boolean = false, onClick: () -> Unit, tag: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (primary) 104.dp else 84.dp).clip(shape)
            .background(if (primary) color.copy(alpha = 0.2f) else Neon.Surface)
            .border(if (primary) 2.dp else 1.dp, if (primary) color else Neon.Outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(color.copy(alpha = if (primary) 1f else 0.18f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (primary) Neon.OnAccent else color, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (primary) color else Neon.TextPrimary)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        }
    }
}

/**
 * « Comment veux-tu travailler ? »: the Mode simple (four tabs, everyday words) or every tool
 * (the five spaces). Two cards side by side, the chosen one lit; Réglages changes it later.
 */
@Composable
private fun ModeChoice(simple: Boolean, choose: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("welcome_mode")) {
        Text(tr("Comment veux-tu travailler ?"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
            ModeCard(tr("Simple"), tr("4 onglets et des mots de tous les jours : idéal pour commencer."), simple, "welcome_mode_simple", Modifier.weight(1f)) { choose(true) }
            ModeCard(tr("Complet"), tr("Tous les outils : structure, sons, mix et live."), !simple, "welcome_mode_full", Modifier.weight(1f)) { choose(false) }
        }
    }
}

@Composable
private fun ModeCard(title: String, line: String, chosen: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.fillMaxHeight().clip(shape)
            .background(if (chosen) Neon.Cyan.copy(alpha = 0.18f) else Neon.Surface)
            .border(if (chosen) 2.dp else 1.dp, if (chosen) Neon.Cyan else Neon.Outline, shape)
            .selectable(selected = chosen, role = Role.RadioButton, onClick = onClick)
            .padding(12.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (chosen) Neon.Cyan else Neon.TextPrimary)
        Text(line, style = MaterialTheme.typography.bodySmall, color = Neon.TextSecondary)
    }
}
