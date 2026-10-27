package com.pocketstudio.app.settings

import com.pocketstudio.ui.ScaledDialog

import com.pocketstudio.model.i18n.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pocketstudio.ui.Neon

/** One page of the first-steps guide. */
class GuidePage(val title: String, val lines: List<String>)

/** The first-steps guide, in the interface's language. */
val GUIDE_PAGES: List<GuidePage> get() = listOf(
    GuidePage(
        tr("Bienvenue dans Pocket Studio"),
        listOf(
            tr("Fais de la musique avec ton appareil : des rythmes, une basse acide, un synthé, et même le remix de tes morceaux préférés."),
            tr("Tout est sauvegardé tout seul, à chaque geste."),
        ),
    ),
    GuidePage(
        tr("La barre du haut"),
        listOf(
            tr("📁 Le nom du projet : touche-le pour ouvrir, créer ou envoyer tes projets."),
            tr("↶ ↷ Annuler et rétablir : on peut toujours revenir en arrière."),
            tr("● Enregistrer : ce que tu joues (clavier, pads, clavier MIDI) s'écrit en rythme."),
            tr("▶ Jouer et arrêter, sur tous les écrans."),
            tr("▶ joue tout le morceau ; « ⟳ Partie », juste dessous, boucle la partie affichée. Touche le tempo pour le changer, garde le doigt sur le nom pour le renommer."),
        ),
    ),
    GuidePage(
        tr("Les espaces"),
        listOf(
            tr("En bas de l'écran, ou à gauche quand l'écran est couché :"),
            tr("Structure : les parties du morceau sur la ligne du temps ; deux touches sur un bloc l'ouvrent."),
            tr("Sons : la batterie, la basse acide, la voix et chaque piste d'instrument, en puces en haut."),
            tr("Mix : la console (les volumes côte à côte), les effets, l'automation et l'export."),
            tr("Live : jouer les parties en direct, avec les pads et les effets."),
            tr("Plus : tes projets, l'aide, les réglages, le clavier MIDI."),
            tr("Le Mode simple (Réglages) n'en garde que quatre : Mon morceau, Rythme, Clavier, Envoyer."),
        ),
    ),
    GuidePage(
        tr("Trois gestes à connaître"),
        listOf(
            tr("Appui long sur un pas ou une note : plus fort ou plus doux."),
            tr("Sur un potard : glisse vers le haut ou le bas ; deux touchers le remettent à zéro."),
            tr("Une erreur ? ↶ répare tout, autant de fois que tu veux."),
        ),
    ),
    GuidePage(
        tr("Et ensuite ?"),
        listOf(
            tr("Pour commencer : touche ▶, puis les pads dans Sons › Batterie."),
            tr("Pour partager : ⤴ tout en haut à droite, sur chaque écran."),
            tr("Ce guide se retrouve dans Plus › Aide."),
        ),
    ),
)

/** A guide of a few [pages] (the first steps by default), "Passer" at any time, [done] on the last page ([onDone], closing by default). */
@Composable
fun GuideDialog(onClose: () -> Unit, pages: List<GuidePage> = GUIDE_PAGES, tag: String = "guide", done: String = tr("C'est parti !"), onDone: () -> Unit = onClose) {
    var page by rememberSaveable(tag) { mutableIntStateOf(0) }
    val p = pages[page.coerceIn(0, pages.lastIndex)]
    val last = page >= pages.lastIndex
    // A tap beside it does not close it for good (Retour does); its text scrolls, its buttons stay.
    ScaledDialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false)) {
        Card(colors = CardDefaults.cardColors(containerColor = Neon.Surface), shape = RoundedCornerShape(14.dp), modifier = Modifier.testTag(tag)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(p.title, style = MaterialTheme.typography.titleLarge, color = Neon.Cyan)
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    p.lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    pages.indices.forEach { i ->
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == page) Neon.Cyan else Neon.Outline))
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // The last page has nothing left to skip: no empty button (TalkBack read « Bouton »).
                    if (!last) TextButton(onClick = onClose, modifier = Modifier.testTag("guide_skip")) { Text(tr("Passer")) }
                    Box(Modifier.weight(1f))
                    if (page > 0) TextButton(onClick = { page-- }) { Text(tr("Retour")) }
                    Button(onClick = { if (last) onDone() else page++ }, modifier = Modifier.testTag("guide_next")) {
                        Text(if (last) done else tr("Suivant"))
                    }
                }
            }
        }
    }
}
