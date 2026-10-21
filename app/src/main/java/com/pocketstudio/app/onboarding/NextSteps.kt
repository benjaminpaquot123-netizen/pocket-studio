package com.pocketstudio.app.onboarding

import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Channel
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks

/** The steps from a first beat to a song worth sharing, in the order a beginner takes them. */
enum class StepGoal(val title: String) {
    Rhythm("Rythme"),
    Bass("Basse"),
    Melody("Mélodie"),
    Sections("Sections"),
    Song("Morceau"),
    Volumes("Volumes"),
    Share("Partager"),
}

/**
 * One step of the "Prochaine étape" card: [done] when the project shows it, [hint] what to do in
 * plain words, [tab] where to do it and [button] the words on the button that goes there.
 * [manual]: the project cannot show it, the user ticks it ("C'est fait").
 */
data class NextStep(
    val goal: StepGoal,
    val done: Boolean,
    val hint: String,
    val tab: AppTab,
    val button: String,
    val manual: Boolean = false,
) {
    val number: Int get() = goal.ordinal + 1
}

/** Notes below this pitch (C3) make a track a bass line. */
private const val BASS_BELOW = 48

/**
 * Where [project] stands on the way to a finished song, computed from what it holds: drum hits,
 * a bass line (the acid line, an 808 or a low instrument track), a melody (a higher instrument
 * track or a recording), two named sections, a song of three parts or more, the mixer touched.
 * [marks]: the steps the user ticked by hand (the volumes, the sharing).
 */
fun nextSteps(project: Project, marks: Set<StepGoal> = emptySet()): List<NextStep> {
    val scenes = project.allScenes
    val rhythm = scenes.any { s -> s.drums.lanes.any { lane -> lane.hits.keys.any { it < s.drums.steps } } }
    val acid = scenes.any { s -> s.acid.steps.take(s.acid.length).any { it.gate } }
    // Each instrument track's notes in every scene: a low one is a bass, a higher one a melody.
    val tracks = project.instrumentTracks.map { t ->
        val notes = scenes.flatMap { s -> (if (t.id == 0) s.synthNotes else s.trackNotes[t.id])?.notes.orEmpty() }
        t.name to notes
    }.filter { it.second.isNotEmpty() }
    fun isBass(name: String, pitches: List<Int>): Boolean {
        val lower = name.lowercase()
        if ("bass" in lower || "808" in lower) return true
        val sorted = pitches.sorted()
        return sorted[sorted.size / 2] < BASS_BELOW
    }
    val (bassTracks, melodyTracks) = tracks.partition { (name, notes) -> isBass(name, notes.map { it.pitch }) }
    val bass = acid || bassTracks.isNotEmpty()
    val melody = melodyTracks.isNotEmpty() || project.audioTracks.any { it.clip != null }
    val filled = scenes.indices.filter { !scenes[it].isEmpty }
    val named = filled.count { scenes[it].label.isNotBlank() }
    val sections = filled.size >= 2 && named == filled.size
    val song = project.song.size >= 3
    val default = Channel()
    val mixed = project.mixer.channels.any { it.volume != default.volume || it.pan != default.pan }
    return listOf(
        NextStep(StepGoal.Rhythm, rhythm, "Touche les cases de la grille pour poser un rythme (le kick sur chaque temps, pour commencer).", AppTab.Rythme, "Rythme"),
        NextStep(StepGoal.Bass, bass, "Ajoute une basse : génère une ligne dans l'onglet Acide, ou joue des notes graves dans Synthé.", AppTab.Acide, "Aller à Acide"),
        NextStep(StepGoal.Melody, melody, "Ajoute une mélodie : joue quelques notes au clavier de Synthé, il reste dans la gamme.", AppTab.Synthe, "Aller à Synthé"),
        NextStep(
            StepGoal.Sections, sections,
            if (filled.size < 2) "Fais une deuxième section : duplique ta scène dans l'onglet Morceau, puis change-la (un couplet, un refrain)."
            else "Donne un nom à chaque section (Intro, Couplet, Refrain…) dans l'onglet Morceau pour t'y retrouver.",
            AppTab.Morceau, "Aller à Morceau",
        ),
        NextStep(StepGoal.Song, song, "Enchaîne tes sections dans le morceau : au moins trois parties, avec un début et une fin.", AppTab.Morceau, "Aller à Morceau"),
        NextStep(StepGoal.Volumes, mixed || StepGoal.Volumes in marks, "Écoute le morceau en entier et règle le volume de chaque piste dans le Mixeur.", AppTab.Mixeur, "Aller au Mixeur", manual = true),
        NextStep(StepGoal.Share, StepGoal.Share in marks, "Exporte ton morceau et fais-le écouter à tes amis !", AppTab.Mixeur, "Partager", manual = true),
    )
}

/** The first step still to do, or null when the song is done. */
fun List<NextStep>.next(): NextStep? = firstOrNull { !it.done }
