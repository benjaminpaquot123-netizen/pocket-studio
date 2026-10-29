package com.pocketstudio.app.onboarding

import com.pocketstudio.ui.ScaledAlertDialog
import com.pocketstudio.ui.ScaledDialog

import com.pocketstudio.model.i18n.tr
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketstudio.ui.Neon
import com.pocketstudio.app.ui.IconMedallion
import com.pocketstudio.app.ui.showcasePanel

/** The three ways to start offered at the first launch, and "later" (stay on the first project). */
class WelcomeActions(
    val guided: () -> Unit = {},
    /** « J'apprends »: the tutorial's chooser (Facile or Pro, then the style). */
    val learn: () -> Unit = {},
    val demo: () -> Unit = {},
    val empty: () -> Unit = {},
    val later: () -> Unit = {},
    /** Remix testers (Sarah): « Extraire la voix d'une chanson », the Remix on a project of its own. */
    val remix: () -> Unit = {},
    /** The mode of the project about to start: Simple (true) or Pro (false), chosen at « Simple ou Pro ? ». */
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
    // Owner (2026-10-29): each way to start (but « Plus tard ») asks « Simple ou Pro ? » first; its
    // Retour comes back to the choices. The way touched waits here (kept through a rotation).
    var asking by rememberSaveable(key = "welcome_asking") { mutableStateOf<String?>(null) }
    if (show || leaving) {
        ScaledDialog(
            onDismissRequest = { if (asking != null) asking = null else { leaving = false; demoReady = false; actions.later() } },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            Surface(color = Neon.Background, modifier = Modifier.fillMaxSize()) {
                val start: (String) -> Unit = { way ->
                    when (way) {
                        "guided" -> actions.guided()
                        "learn" -> actions.learn()
                        // A second quick touch while the first project opens makes no second one.
                        "demo" -> if (!leaving) { demoReady = true; leaving = true; actions.demo() }
                        "empty" -> if (!leaving) { leaving = true; actions.empty() }
                        "remix" -> if (!leaving) { leaving = true; actions.remix() }
                    }
                }
                val way = asking
                if (way != null) {
                    com.pocketstudio.app.ui.ModeStep(
                        choose = { v -> asking = null; actions.chooseMode(v); start(way) },
                        back = { asking = null },
                        tuto = way == "learn",
                    )
                } else WelcomeScreen(
                    WelcomeActions(
                        guided = { asking = "guided" },
                        learn = { asking = "learn" },
                        demo = { if (!leaving) asking = "demo" },
                        empty = { if (!leaving) asking = "empty" },
                        remix = { if (!leaving) asking = "remix" },
                        later = { leaving = false; demoReady = false; actions.later() },
                    ),
                    simple = simple,
                )
            }
        }
    } else if (demoReady) {
        ScaledAlertDialog(
            onDismissRequest = { demoReady = false },
            title = { Text(tr("Ta démo est prête")) },
            text = {
                Text(
                    if (simple) tr("Écoute-la en entier, puis va voir dans Mon morceau comment elle est construite : tu peux tout changer.")
                    else tr("Écoute-la en entier, puis va voir dans Structure comment elle est construite : tu peux tout changer."),
                )
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
            // The language on top (the phone's is already chosen), one touch away.
            com.pocketstudio.app.settings.LanguagePicker(Modifier.align(Alignment.End))
            // Who we are: the mark, the name, one line.
            Row(Modifier.fillMaxWidth().testTag("welcome_brand"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // The pads take half of the mark's box: the box overflows a smaller slot.
                Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) { BenStudioMark(120.dp, Modifier.requiredSize(120.dp)) }
                Column(Modifier.weight(1f)) {
                    BenStudioWordmark(sizeSp = 28)
                    Text(tr("Ton studio de musique, dans ta poche."), style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
                }
            }
            Text(tr("Bienvenue ! Fais ta musique sur ton appareil. Par quoi on commence ?"), style = MaterialTheme.typography.bodyLarge)
            Choice(
                Icons.Filled.AutoAwesome, tr("Créer mon premier morceau (guidé)"),
                tr("Choisis un style, je t'aide pas à pas, du début à la fin du morceau."),
                Neon.Cyan, primary = true, onClick = actions.guided, tag = "welcome_guided",
            )
            Choice(
                Icons.Filled.School, tr("J'apprends"),
                tr("Tu fais tout le morceau toi-même, étape par étape : je t'explique et je vérifie."),
                Neon.Acid, onClick = actions.learn, tag = "welcome_learn",
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
            // Remix testers (Sarah installed the app for this and found it nowhere): one way in, under the others.
            Choice(
                Icons.Filled.RecordVoiceOver, tr("Extraire la voix d'une chanson"),
                tr("Choisis une chanson du téléphone : l'appli sépare la voix, la batterie, la basse et le reste."),
                com.pocketstudio.app.ui.REMIX_COLOR, onClick = actions.remix, tag = "welcome_remix",
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = actions.later, modifier = Modifier.heightIn(min = 48.dp).testTag("welcome_later")) {
                    Text(tr("Plus tard : voir le rythme de départ"), color = Neon.TextSecondary)
                }
            }
            Text(if (simple) tr("Le guide des premiers pas reste dans ⋮ › Aide et premiers pas.") else tr("Le guide des premiers pas reste dans Plus › Aide."), style = MaterialTheme.typography.bodySmall,
                color = Neon.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** A big choice: a round icon, a title and one line; [primary] filled with its colour. */
@Composable
private fun Choice(icon: ImageVector, title: String, line: String, color: Color, primary: Boolean = false, onClick: () -> Unit, tag: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = if (primary) 104.dp else 84.dp)
            .showcasePanel(shape, accent = color.takeIf { primary }, edge = if (primary) 2.dp else 1.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconMedallion(icon, color, 52.dp, 28.dp, filled = primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (primary) color else Neon.TextPrimary)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = Neon.TextSecondary)
        }
    }
}
