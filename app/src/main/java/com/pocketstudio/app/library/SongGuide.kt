package com.pocketstudio.app.library

import com.pocketstudio.app.settings.GuidePage
import com.pocketstudio.model.StarterWizard

/**
 * The guide that opens once the assistant has made the song: its two scenes, how to hear each
 * one, how to play them in a row, and what to try next.
 */
fun songGuide(styleName: String, plan: StarterWizard.ScenePlan): List<GuidePage> {
    fun scene(index: Int): String {
        val name = if (index == 0) "A" else "B"
        val what = plan.contents(index).joinToString(", ") { it.substringBefore(" (").substringBefore(" / ") }
        return "Scène $name · ${plan.title(index)} : ${what.ifEmpty { "rien pour l'instant" }}."
    }
    val scenes = if (plan.copy) {
        listOf(
            "Scène A · tout ton morceau : ${plan.contents(0).joinToString(", ") { it.substringBefore(" (").substringBefore(" / ") }}.",
            "Scène B · sa copie : change-la (ajoute des coups, enlève une ligne) pour faire une variante.",
        )
    } else {
        listOf(scene(0), scene(1))
    }
    return listOf(
        GuidePage(
            "Ton morceau $styleName est prêt",
            listOf("Je l'ai rangé en deux scènes. Une scène, c'est une boucle qui tourne en rond, comme un couplet ou un refrain.") + scenes,
        ),
        GuidePage(
            "Écouter une scène",
            listOf(
                "▶ en haut joue la scène affichée, en boucle. Tu commences sur la scène A.",
                "Onglet Morceau › Scènes : touche B pour l'afficher et l'écouter. Pendant la lecture, elle prend le relais à la mesure suivante.",
                "Ce que tu vois dans Rythme, Acide et Synthé, c'est toujours la scène affichée.",
            ),
        ),
        GuidePage(
            "Jouer tout le morceau",
            listOf(
                "Onglet Morceau › Morceau : la scène A joue 2 fois, puis la B 2 fois, et ça recommence.",
                "Touche « Jouer le morceau » pour l'entendre. Les boutons + et − changent le nombre de fois.",
            ),
        ),
        GuidePage(
            "Et maintenant ?",
            listOf(
                "Rythme : touche les cases pour ajouter ou enlever des coups.",
                "Synthé : joue au clavier, il reste dans la gamme du morceau : aucune fausse note possible.",
                "Morceau › « Dupliquer la scène » : une scène C pour une variante (un refrain, une fin), puis « + C » pour l'ajouter au morceau.",
            ),
        ),
    )
}
