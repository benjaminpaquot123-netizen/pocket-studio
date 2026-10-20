package com.pocketstudio.app.settings

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

val GUIDE_PAGES = listOf(
    GuidePage(
        "Bienvenue dans Pocket Studio",
        listOf(
            "Fais de la musique avec ton téléphone : des rythmes, une basse acide, un synthé, et même le remix de tes morceaux préférés.",
            "Tout est enregistré tout seul, à chaque geste.",
        ),
    ),
    GuidePage(
        "La barre du haut",
        listOf(
            "📁 Le nom du projet : touche-le pour ouvrir, créer ou envoyer tes projets.",
            "↶ ↷ Annuler et rétablir : on peut toujours revenir en arrière.",
            "● Enregistrer : ce que tu joues (clavier, pads, clavier MIDI) s'écrit en rythme.",
            "▶ Jouer et arrêter, sur tous les écrans.",
        ),
    ),
    GuidePage(
        "Les onglets du bas",
        listOf(
            "Rythme : les 16 pads et leurs pas.",
            "Acide : la ligne de basse façon 303, avec « Génère-moi un pattern ».",
            "Synthé : le clavier, les notes (piano roll) et les sons.",
            "Mixeur : volumes, effets, et l'export de ta musique.",
            "Audio : le micro, les sons importés et le Remix (sépare un morceau en batterie, basse, voix et le reste).",
            "Morceau : les scènes, les pads live et les effets en direct.",
            "Outils : les réglages, le clavier MIDI, la recette.",
        ),
    ),
    GuidePage(
        "Trois gestes à connaître",
        listOf(
            "Appui long sur un pas ou une note : plus fort ou plus doux.",
            "Sur un potard : glisse vers le haut ou le bas ; deux touchers le remettent à zéro.",
            "Une erreur ? ↶ répare tout, autant de fois que tu veux.",
        ),
    ),
    GuidePage(
        "Et ensuite ?",
        listOf(
            "Pour commencer : touche ▶, puis les pads de l'onglet Rythme.",
            "Pour partager : Mixeur › Exporter (WAV, M4A ou MIDI).",
            "Ce guide se retrouve dans Outils › Réglages.",
        ),
    ),
)

/** A guide of a few [pages] (the first steps by default), "Passer" at any time, [done] on the last page. */
@Composable
fun GuideDialog(onClose: () -> Unit, pages: List<GuidePage> = GUIDE_PAGES, tag: String = "guide", done: String = "C'est parti !") {
    var page by rememberSaveable(tag) { mutableIntStateOf(0) }
    val p = pages[page.coerceIn(0, pages.lastIndex)]
    val last = page >= pages.lastIndex
    // A tap beside it does not close it for good (Retour does); its text scrolls, its buttons stay.
    Dialog(onDismissRequest = onClose, properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false)) {
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
                    TextButton(onClick = onClose, modifier = Modifier.testTag("guide_skip")) { Text(if (last) "" else "Passer") }
                    Box(Modifier.weight(1f))
                    if (page > 0) TextButton(onClick = { page-- }) { Text("Retour") }
                    Button(onClick = { if (last) onClose() else page++ }, modifier = Modifier.testTag("guide_next")) {
                        Text(if (last) done else "Suivant")
                    }
                }
            }
        }
    }
}
