package com.pocketstudio.app.library

import com.pocketstudio.model.i18n.tr
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
    return tr("%s pour finir : environ %s.", song.joinToString(" → ") { plan.title(it.scene) }, duration(seconds))
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
 * The guide that opens once the assistant has made the song, two short pages: its three sections
 * (Intro, Couplet, Refrain) and what each adds; then how to hear it (the whole song, or one section
 * in a loop) and what to try next. It ends on "▶ Écouter mon morceau". [song]: the song's summary
 * ([songSummary]). [inKey]: a mood was chosen, so the song's key is locked and the keyboard plays
 * no wrong note.
 */
fun songGuide(styleName: String, plan: StarterWizard.ScenePlan, inKey: Boolean, song: String = ""): List<GuidePage> {
    fun scene(index: Int): String {
        val what = plan.contents(index).joinToString(", ") { it.substringBefore(" (").substringBefore(" / ") }
        return tr("%s · %s, %s : %s.", StarterWizard.ScenePlan.NAMES[index], plan.title(index), plan.role(index), what.ifEmpty { tr("rien pour l'instant") })
    }
    val scenes = (0 until StarterWizard.ScenePlan.COUNT).map(::scene)
    return listOf(
        GuidePage(
            tr("Ton morceau %s est prêt", tr(styleName)),
            listOf(tr("Il a trois sections, comme une vraie chanson. Chacune ajoute des sons à celle d'avant :")) + scenes +
                tr("Pas besoin de sauvegarder : tout est gardé tout seul."),
        ),
        GuidePage(
            tr("Écouter et changer"),
            listOfNotNull(
                tr("▶ Jouer fait entendre tout le morceau, de l'Intro à la fin. Pour écouter en boucle la section affichée, touche « Écouter » à côté de Jouer (onglet Rythme) : « cette section »."),
                song.takeIf { it.isNotBlank() },
                tr("Rythme : touche les cases pour ajouter ou enlever des coups."),
                if (inKey) tr("Synthé : joue au clavier, il reste dans la gamme du morceau : aucune fausse note possible.")
                else tr("Synthé : joue au clavier pour essayer des notes par-dessus le rythme."),
                tr("Onglet Morceau : touche Intro, Couplet ou Refrain pour l'afficher, change son nom, ou duplique-la pour en faire un Pont ou une Fin."),
            ),
        ),
    )
}
