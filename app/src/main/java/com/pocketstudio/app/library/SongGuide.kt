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

/** "Intro → Couplet → Pré-refrain → Refrain → … → Intro pour finir : environ 3 min 10." (a variant of a section counts as it). */
fun songSummary(style: StarterWizard.Style, plan: StarterWizard.ScenePlan): String {
    val seconds = StarterWizard.songSeconds(style, plan)
    return tr("%s pour finir : environ %s.", StarterWizard.songSectionTitles(style, plan).joinToString(" → "), duration(seconds))
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
 * The guide that opens once the assistant has made the song, short pages: its sections
 * (Intro, Couplet, Refrain; or Intro, Groove, Drop and Break) and what each adds; then how to hear
 * it (the whole song, or one section in a loop) and what to try next, the voice on top included.
 * It ends on "▶ Écouter mon morceau". [song]: the song's summary ([songSummary]). [inKey]: a mood
 * was chosen, so the song's key is locked and the keyboard plays no wrong note. [acid]: the song
 * has the style's 303 line; [acidMoves]: its cutoff follows a song curve (acid styles), so the
 * guide says it moves on its own and how to take it over. [sweeps]: some sounds' filters open and
 * shut along the song (techno stabs, the pad of a Montée). The riser track ([StarterWizard.ScenePlan.riser])
 * gets a line too: it is only heard when the whole song plays.
 */
fun songGuide(
    styleName: String, plan: StarterWizard.ScenePlan, inKey: Boolean, song: String = "", acid: Boolean = false, acidMoves: Boolean = false,
    sweeps: Boolean = false,
): List<GuidePage> {
    fun scene(index: Int): String {
        val what = plan.contents(index).joinToString(", ") { it.substringBefore(" (").substringBefore(" / ") }
        return tr("%s · %s, %s : %s.", StarterWizard.ScenePlan.NAMES[index], plan.title(index), plan.role(index), what.ifEmpty { tr("rien pour l'instant") })
    }
    val scenes = (0 until plan.count).map(::scene)
    val intro = when {
        plan.form == StarterWizard.SongForm.Build && plan.hasBreak ->
            tr("Il a quatre parties, comme un vrai morceau de club : la Montée fait monter la tension sans kick, tout tombe au Drop, puis le Break fait une pause avant la dernière Montée :")
        plan.form == StarterWizard.SongForm.Build ->
            tr("Il a trois parties, comme un vrai morceau de club : la Montée fait monter la tension sans kick, puis tout tombe au Drop :")
        plan.form.sung && plan.hasBreak ->
            tr("Il a quatre parties, comme une vraie chanson : chacune ajoute des sons à celle d'avant, puis le Pont fait une pause sans kick ni basse avant le dernier Refrain :")
        plan.hasBreak -> tr("Il a quatre parties, comme un vrai morceau de club : les sons s'empilent, puis le Break fait une pause avant le dernier Drop :")
        else -> tr("Il a trois parties, comme une vraie chanson. Chacune ajoute des sons à celle d'avant :")
    }
    return listOf(
        GuidePage(
            tr("Ton morceau %s est prêt", tr(styleName)),
            listOf(intro) + scenes +
                tr("Pas besoin de sauvegarder : tout est gardé tout seul."),
        ),
        // Never more than three or four short lines a page, so that none of them scrolls on a small phone.
        GuidePage(
            tr("Écouter et changer"),
            listOfNotNull(
                tr("▶ joue tout le morceau, de l'Intro à la fin, sur tous les écrans. Pour écouter en boucle la partie affichée, touche « ⟳ Partie », juste sous ▶."),
                song.takeIf { it.isNotBlank() },
                tr("Onglet Morceau : touche une partie (Intro, Couplet ou Refrain) pour l'afficher, la renommer ou la dupliquer pour en faire un Pont ou une Fin."),
            ),
        ),
        GuidePage(
            tr("Changer les sons"),
            listOfNotNull(
                tr("Rythme : touche les cases pour ajouter ou enlever des coups."),
                if (inKey) tr("Synthé : joue au clavier, il reste dans la gamme du morceau : aucune fausse note possible.")
                else tr("Synthé : joue au clavier pour essayer des notes par-dessus le rythme."),
                // The 303 line made on its step: where it plays, and where to change it.
                if (!acid || plan.acid.isEmpty()) null
                else if (acidMoves) tr("Acide : ta ligne 303 joue dans %s. Dans Sons › Basse acide, touche un pas pour la changer. La coupure s'ouvre toute seule le long du morceau : pour la tourner toi-même, garde le doigt sur « Coupure ».", plan.sections(plan.acid))
                else tr("Acide : ta ligne 303 joue dans %s. Dans Sons › Basse acide, touche un pas pour la changer et tourne « Coupure » pour la faire crier.", plan.sections(plan.acid)),
            ),
        ),
        GuidePage(
            tr("Les petits plus"),
            listOfNotNull(
                tr("Rythme : chaque partie finit ses 4 mesures par un petit roulement, qui annonce la suite.").takeIf { plan.parts.isNotEmpty() },
                if (!plan.riser) null
                else if (plan.form.sung) tr("Souffle montant : il monte juste avant chaque Refrain et claque sur son premier temps. On ne l'entend qu'en jouant tout le morceau ; baisse la piste « Souffle montant » dans Mix s'il te gêne.")
                else tr("Souffle montant : il monte avant chaque Drop et claque sur son premier temps. On ne l'entend qu'en jouant tout le morceau ; baisse la piste « Souffle montant » dans Mix s'il te gêne."),
                tr("Contre-chant : une petite réponse aux accords arrive au milieu du premier %s, puis joue dans tout le suivant : le deuxième sonne plus plein.", plan.title(2)).takeIf { plan.counter },
                // The arrangement's drop (S2): the kick and the bass stop before it, a beat of silence, then everything lands.
                (if (plan.form.sung) tr("Avant chaque Refrain, le kick et la basse se coupent un instant, puis un temps de silence : le Refrain tombe d'un coup.")
                else tr("Montée : avant chaque Drop, le kick et la basse se coupent 2 mesures, la caisse claire roule, puis un temps de silence : tout tombe d'un coup.")).takeIf { plan.variants.isNotEmpty() },
            ),
        ),
        GuidePage(
            tr("Pour aller plus loin"),
            listOfNotNull(
                tr("Filtres : certains sons s'ouvrent et se ferment tout seuls le long du morceau (Mixeur, Automation).").takeIf { sweeps },
                tr("Audio : enregistre ta voix par-dessus le morceau (mets des écouteurs, sinon le micro entend la musique)."),
            ),
        ),
    // A page left empty by this song (no roll, riser or counter-melody) is dropped.
    ).filter { it.lines.isNotEmpty() }
}
