package com.pocketstudio.app.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.pocketstudio.app.settings.GuidePage
import com.pocketstudio.model.StarterWizard

/** The guide waiting to be read, kept through the activity's rebuild (opening a project recreates it). */
@Composable
fun rememberSongGuide(): MutableState<List<GuidePage>?> =
    rememberSaveable(key = "song_guide", stateSaver = GuidePagesSaver) { mutableStateOf(null) }

/** Pages as title, lines, title, lines… (a line never holds a line break). */
private val GuidePagesSaver = Saver<List<GuidePage>?, ArrayList<String>>(
    save = { pages -> ArrayList(pages.orEmpty().flatMap { listOf(it.title, it.lines.joinToString("\n")) }) },
    restore = { saved -> saved.chunked(2).filter { it.size == 2 }.map { (title, lines) -> GuidePage(title, lines.split("\n")) }.ifEmpty { null } },
)

/**
 * The guide that opens once the assistant has made the song: its three scenes, how to hear each
 * one, how to play them in a row, and what to try next. [inKey]: a mood was chosen, so the
 * song's key is locked and the keyboard plays no wrong note.
 */
fun songGuide(styleName: String, plan: StarterWizard.ScenePlan, inKey: Boolean): List<GuidePage> {
    fun scene(index: Int): String {
        val what = plan.contents(index).joinToString(", ") { it.substringBefore(" (").substringBefore(" / ") }
        return "Scène ${StarterWizard.ScenePlan.NAMES[index]} · ${plan.title(index)} : ${what.ifEmpty { "rien pour l'instant" }}."
    }
    val scenes = (0 until StarterWizard.ScenePlan.COUNT).map(::scene)
    return listOf(
        GuidePage(
            "Ton morceau $styleName est prêt",
            listOf("Je l'ai rangé en trois scènes. Une scène, c'est une boucle qui tourne en rond, comme un couplet ou un refrain.") + scenes,
        ),
        GuidePage(
            "Écouter une scène",
            listOf(
                "▶ en haut joue la scène affichée, en boucle. Tu commences sur la scène A.",
                "Onglet Morceau › Scènes : touche B ou C pour l'afficher et l'écouter. Pendant la lecture, elle prend le relais à la mesure suivante.",
                "Ce que tu vois dans Rythme, Acide et Synthé, c'est toujours la scène affichée.",
            ),
        ),
        GuidePage(
            "Jouer tout le morceau",
            listOf(
                "Onglet Morceau › Morceau : la scène A joue 2 fois, puis la B 2 fois, puis la C 2 fois, et ça recommence.",
                "Touche « Jouer le morceau » pour l'entendre. Les boutons + et − changent le nombre de fois.",
            ),
        ),
        GuidePage(
            "Et maintenant ?",
            listOf(
                "Rythme : touche les cases pour ajouter ou enlever des coups.",
                if (inKey) "Synthé : joue au clavier, il reste dans la gamme du morceau : aucune fausse note possible."
                else "Synthé : joue au clavier pour essayer des notes par-dessus le rythme.",
                // The assistant fills A, B and C: the first copy lands in D.
                "Morceau › « Dupliquer la scène » : une scène D pour une variante (un refrain, une fin), puis « + D » pour l'ajouter au morceau.",
            ),
        ),
    )
}
