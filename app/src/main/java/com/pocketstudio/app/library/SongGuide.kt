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

/** "Intro → Couplet → Refrain → Couplet → Refrain → Intro pour finir : environ 2 min 40." */
fun songSummary(style: StarterWizard.Style, plan: StarterWizard.ScenePlan): String {
    val song = StarterWizard.songOf(style, plan)
    val seconds = StarterWizard.seconds(song, StarterWizard.loopBars(style, plan), style.bpm)
    return song.joinToString(" → ") { plan.title(it.scene) } + " pour finir : environ ${duration(seconds)}."
}

/** "2 min 40", "1 min", "45 s": rounded to ten seconds. */
fun duration(seconds: Double): String {
    val total = (Math.round(seconds / 10) * 10).toInt()
    val (m, s) = total / 60 to total % 60
    return when {
        m == 0 -> "$s s"
        s == 0 -> "$m min"
        else -> "$m min $s"
    }
}

/**
 * The guide that opens once the assistant has made the song: its three sections (Intro, Couplet,
 * Refrain) and what each adds, how to play the whole song, how to hear one section, and what to
 * try next. [song]: the song's summary ([songSummary]). [inKey]: a mood was chosen, so the song's
 * key is locked and the keyboard plays no wrong note.
 */
fun songGuide(styleName: String, plan: StarterWizard.ScenePlan, inKey: Boolean, song: String = ""): List<GuidePage> {
    fun scene(index: Int): String {
        val what = plan.contents(index).joinToString(", ") { it.substringBefore(" (").substringBefore(" / ") }
        return "${StarterWizard.ScenePlan.NAMES[index]} · ${plan.title(index)}, ${plan.role(index)} : ${what.ifEmpty { "rien pour l'instant" }}."
    }
    val scenes = (0 until StarterWizard.ScenePlan.COUNT).map(::scene)
    return listOf(
        GuidePage(
            "Ton morceau $styleName est prêt",
            listOf("Il a trois sections, comme une vraie chanson. Chacune ajoute des sons à celle d'avant :") + scenes,
        ),
        GuidePage(
            "Jouer tout le morceau",
            listOfNotNull(
                "Onglet Morceau : la liste enchaîne les sections du début à la fin.",
                song.takeIf { it.isNotBlank() },
                "Touche « Jouer le morceau » pour l'entendre en entier. Les boutons + et − changent le nombre de fois qu'une section se répète.",
            ),
        ),
        GuidePage(
            "Écouter une section",
            listOf(
                "▶ en haut joue tout le morceau, de l'Intro à la fin. Pour écouter une seule section en boucle : Morceau › Écouter « la scène ».",
                "Onglet Morceau › Scènes : touche Couplet ou Refrain pour l'afficher et l'écouter. Pendant la lecture, elle prend le relais à la mesure suivante.",
                "Ce que tu vois dans Rythme, Acide et Synthé, c'est toujours la section affichée.",
            ),
        ),
        GuidePage(
            "Et maintenant ?",
            listOf(
                "Rythme : touche les cases pour ajouter ou enlever des coups.",
                if (inKey) "Synthé : joue au clavier, il reste dans la gamme du morceau : aucune fausse note possible."
                else "Synthé : joue au clavier pour essayer des notes par-dessus le rythme.",
                "Les noms Intro, Couplet et Refrain se changent dans l'onglet Morceau. Duplique une section pour en faire un Pont ou une Fin, puis ajoute-la au morceau.",
            ),
        ),
    )
}
