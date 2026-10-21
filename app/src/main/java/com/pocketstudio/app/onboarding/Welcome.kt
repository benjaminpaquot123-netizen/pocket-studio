package com.pocketstudio.app.onboarding

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
)

/**
 * The first launch: one welcome line and three big choices (the guided song, a demo to listen
 * to, an empty project) instead of pages of text. Shown while [show]; once a demo is chosen and
 * opened (the activity rebuilds), a short dialog offers to play it ([playDemo]).
 */
@Composable
fun FirstLaunch(show: Boolean, actions: WelcomeActions, playDemo: () -> Unit) {
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
                        demo = { demoReady = true; leaving = true; actions.demo() },
                        empty = { leaving = true; actions.empty() },
                        later = { leaving = false; demoReady = false; actions.later() },
                    ),
                )
            }
        }
    } else if (demoReady) {
        AlertDialog(
            onDismissRequest = { demoReady = false },
            title = { Text("Ta démo est prête") },
            text = {
                Text("Écoute-la en entier, puis va voir dans l'onglet Morceau comment elle est construite : tu peux tout changer.")
            },
            confirmButton = {
                Button(onClick = { demoReady = false; playDemo() }, modifier = Modifier.testTag("demo_play")) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(" Écouter")
                }
            },
            dismissButton = { TextButton(onClick = { demoReady = false }) { Text("Plus tard") } },
            modifier = Modifier.testTag("demo_ready"),
        )
    }
}

/** The choice screen itself: the guided song first and biggest, then the demo, then the empty project. */
@Composable
fun WelcomeScreen(actions: WelcomeActions) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 28.dp).testTag("welcome"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Pocket Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Neon.Cyan)
        Text("Bienvenue ! Fais ta musique sur ton téléphone. Par quoi on commence ?", style = MaterialTheme.typography.bodyLarge)
        Choice(
            Icons.Filled.AutoAwesome, "Créer mon premier morceau (guidé)",
            "Choisis un style, je t'aide pas à pas : intro, couplet, refrain.",
            Neon.Cyan, primary = true, onClick = actions.guided, tag = "welcome_guided",
        )
        Choice(
            Icons.Filled.Headphones, "Écouter une démo",
            "Un morceau déjà prêt, pour découvrir ce qu'on peut faire.",
            Color(0xFF5BB8F5), onClick = actions.demo, tag = "welcome_demo",  // sky blue: green means "playing"
        )
        Choice(
            Icons.Filled.Add, "Partir de zéro",
            "Un projet vide : à toi de jouer.",
            Neon.TextSecondary, onClick = actions.empty, tag = "welcome_empty",
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = actions.later, modifier = Modifier.heightIn(min = 48.dp).testTag("welcome_later")) {
                Text("Plus tard : voir le rythme de départ", color = Neon.TextSecondary)
            }
        }
        Text("Le guide des premiers pas reste dans Outils › Réglages.", style = MaterialTheme.typography.bodySmall,
            color = Neon.TextSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
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
