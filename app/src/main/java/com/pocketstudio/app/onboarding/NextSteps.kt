package com.pocketstudio.app.onboarding

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Channel
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks

/** The steps from a first beat to a song worth sharing, in the order a beginner takes them. */
enum class StepGoal {
    Rhythm,
    Bass,
    Melody,
    Sections,
    Song,
    Voice,
    Volumes,
    Share;

    val title: String get() = when (this) {
        Rhythm -> tr("Rythme")
        Bass -> tr("Basse")
        Melody -> tr("Mélodie")
        Sections -> tr("Sections")
        Song -> tr("Morceau")
        Voice -> tr("Voix")
        Volumes -> tr("Volumes")
        Share -> tr("Partager")
    }

    /** What to do, in a few words: the one-line « Prochaine étape » strip says it (« 3/8 · Ajoute une mélodie »). */
    val action: String get() = when (this) {
        Rhythm -> tr("Pose un rythme")
        Bass -> tr("Ajoute une basse")
        Melody -> tr("Ajoute une mélodie")
        Sections -> tr("Fais une deuxième section")
        Song -> tr("Enchaîne tes sections")
        Voice -> tr("Pose ta voix")
        Volumes -> tr("Règle les volumes")
        Share -> tr("Partage ton morceau")
    }
}

/**
 * One step of the "Prochaine étape" card: [done] when the project shows it, [hint] what to do in
 * plain words, [tab] where to do it and [button] the words on the button that goes there.
 * [manual]: the project cannot show it, the user ticks it ("C'est fait"). [action]: the hint in
 * a few words, for the one-line strip.
 */
data class NextStep(
    val goal: StepGoal,
    val done: Boolean,
    val hint: String,
    val tab: AppTab,
    val button: String,
    val manual: Boolean = false,
    val action: String = goal.action,
) {
    val number: Int get() = goal.ordinal + 1
}

/** Notes below this pitch (C3) make a track a bass line. */
private const val BASS_BELOW = 48

/**
 * Where [project] stands on the way to a finished song, computed from what it holds: drum hits,
 * a bass line (the acid line, an 808 or a low instrument track), a melody (a higher instrument
 * track or a recording), two named sections, a song of three parts or more, a voice recorded on
 * top, the mixer touched. [marks]: the steps the user ticked by hand (the voice, the volumes, the sharing).
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
        // A default name in any language reads as its French ("Bas" → "Basse"), so the language does not matter.
        val lower = (com.pocketstudio.model.DefaultNames.french(name) ?: name).lowercase()
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
    // A recording on an audio track: the voice is there.
    val voice = project.audioTracks.any { it.clip != null }
    val default = Channel()
    // The tracks' faders (the pads' own strips hold the assistant's balance of the kit, not the user's mix).
    val mixed = project.mixer.channels.take(Mixer.FIRST_PAD).any { it.volume != default.volume || it.pan != default.pan }
    return listOf(
        NextStep(StepGoal.Rhythm, rhythm, tr("Touche les cases de la grille pour poser un rythme (le kick sur chaque temps, pour commencer)."), AppTab.Rythme, tr("Rythme")),
        NextStep(StepGoal.Bass, bass, tr("Ajoute une basse : génère une ligne dans l'onglet Acide, ou joue des notes graves dans Synthé."), AppTab.Acide, tr("Aller à Acide")),
        NextStep(StepGoal.Melody, melody, tr("Ajoute une mélodie : joue quelques notes au clavier de Synthé, il reste dans la gamme."), AppTab.Synthe, tr("Aller à Synthé")),
        NextStep(
            StepGoal.Sections, sections,
            if (filled.size < 2) tr("Fais une deuxième section : duplique ta scène dans l'onglet Morceau, puis change-la (un couplet, un refrain).")
            else tr("Donne un nom à chaque section (Intro, Couplet, Refrain…) dans l'onglet Morceau pour t'y retrouver."),
            AppTab.Morceau, tr("Aller à Morceau"),
            action = if (filled.size < 2) StepGoal.Sections.action else tr("Nomme tes sections"),
        ),
        NextStep(StepGoal.Song, song, tr("Enchaîne tes sections dans le morceau : au moins trois parties, avec un début et une fin."), AppTab.Morceau, tr("Aller à Morceau")),
        NextStep(
            StepGoal.Voice, voice || StepGoal.Voice in marks,
            tr("Pose ta voix : onglet Audio, touche ● pour chanter ou rapper sur le morceau (avec des écouteurs)."), AppTab.Audio, tr("Aller à Audio"), manual = true,
        ),
        NextStep(StepGoal.Volumes, mixed || StepGoal.Volumes in marks, tr("Écoute le morceau en entier et règle le volume de chaque piste dans le Mixeur."), AppTab.Mixeur, tr("Aller au Mixeur"), manual = true),
        NextStep(StepGoal.Share, StepGoal.Share in marks, tr("Exporte ton morceau et fais-le écouter à tes amis !"), AppTab.Mixeur, tr("Partager"), manual = true),
    )
}

/** The first step still to do, or null when the song is done. */
fun List<NextStep>.next(): NextStep? = firstOrNull { !it.done }
