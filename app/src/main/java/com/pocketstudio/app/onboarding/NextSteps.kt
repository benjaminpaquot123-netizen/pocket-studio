package com.pocketstudio.app.onboarding

import com.pocketstudio.model.i18n.tr
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Channel
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.RoleMix
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks

/** The steps from a first beat to a song worth sharing, in the order a beginner takes them. */
enum class StepGoal {
    Rhythm,
    Bass,
    Melody,
    Sections,
    Song,
    /** A song made for the user (the assistant, a style): hear it whole first. */
    Listen,
    /** …then make it theirs: one sound changed. */
    Sound,
    Volumes,
    /** An option, after the mix: not everyone sings. */
    Voice,
    Share;

    val title: String get() = when (this) {
        Rhythm -> tr("Rythme")
        Bass -> tr("Basse")
        Melody -> tr("Mélodie")
        Sections -> tr("Parties")
        Song -> tr("Morceau")
        Listen -> tr("Écoute")
        Sound -> tr("Un son")
        Voice -> tr("Voix")
        Volumes -> tr("Volumes")
        Share -> tr("Partager")
    }

    /** What to do, in a few words: the one-line « Prochaine étape » strip says it (« 3/8 · Ajoute une mélodie »). */
    val action: String get() = when (this) {
        Rhythm -> tr("Pose un rythme")
        Bass -> tr("Ajoute une basse")
        Melody -> tr("Ajoute une mélodie")
        Sections -> tr("Fais une deuxième partie")
        Song -> tr("Enchaîne tes parties")
        Listen -> tr("Écoute ton morceau en entier")
        Sound -> tr("Change un son")
        Voice -> tr("Pose ta voix (si tu veux)")
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
    /** Its place in the list (from 1): [nextSteps] sets it. */
    val number: Int = goal.ordinal + 1,
)

/** Notes below this pitch (C3) make a track a bass line. */
private const val BASS_BELOW = 48

/**
 * Where [project] stands on the way to a finished song, computed from what it holds: drum hits,
 * a bass line (the acid line, an 808 or a low instrument track), a melody (a higher instrument
 * track or a recording), two named parts, a song of three parts or more, a voice recorded on
 * top, the mixer touched. [marks]: the steps the user ticked by hand (the voice, the volumes, the sharing).
 *
 * The steps come in the order the project takes them: what it already has first. A song made
 * whole (the assistant, a style: rhythm, bass, melody and a song at once) goes on with « Écoute
 * ton morceau en entier », « Change un son », the volumes, then the voice as an option, and the sharing.
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
    // An assistant's song pans its tracks itself (G0, RoleMix): those places are not the user's mix.
    val assistantPans = if (project.style != null) RoleMix.PANS else emptySet()
    val mixed = Mixer.TRACK_STRIPS.mapNotNull { project.mixer.channels.getOrNull(it) }
        .any { it.volume != default.volume || (it.pan != default.pan && it.pan !in assistantPans) }
    // Made whole in one go (the assistant, a style): the way on is to listen, make it one's own, then share.
    val madeWhole = rhythm && (bass || melody) && song
    val steps = listOf(
        NextStep(StepGoal.Rhythm, rhythm, tr("Touche les cases de la grille pour poser un rythme (le kick sur chaque temps, pour commencer)."), AppTab.Rythme, tr("Rythme")),
        NextStep(StepGoal.Bass, bass, tr("Ajoute une basse : génère une ligne dans Sons › Basse acide, ou joue des notes graves sur une piste de Sons."), AppTab.Acide, tr("Aller à la basse acide")),
        NextStep(StepGoal.Melody, melody, tr("Ajoute une mélodie : joue quelques notes au clavier d'une piste de Sons, il reste dans la gamme."), AppTab.Synthe, tr("Aller au clavier")),
        NextStep(
            StepGoal.Sections, sections,
            if (filled.size < 2) tr("Fais une deuxième partie : duplique ta partie dans Structure › Parties, puis change-la (un couplet, un refrain).")
            else tr("Donne un nom à chaque partie (Intro, Couplet, Refrain…) dans Structure › Parties pour t'y retrouver."),
            AppTab.Morceau, tr("Aller à Structure"),
            action = if (filled.size < 2) StepGoal.Sections.action else tr("Nomme tes parties"),
        ),
        NextStep(StepGoal.Song, song, tr("Enchaîne tes parties dans le morceau : au moins trois, avec un début et une fin."), AppTab.Morceau, tr("Aller à Structure")),
    ) + listOfNotNull(
        if (madeWhole) NextStep(StepGoal.Listen, StepGoal.Listen in marks, tr("Écoute ton morceau en entier : ▶ en haut. Touche une partie dans Structure pour l'écouter à partir d'elle."), AppTab.Morceau, tr("Aller à Structure"), manual = true) else null,
        if (madeWhole) NextStep(StepGoal.Sound, StepGoal.Sound in marks, tr("Fais-le tien : dans Structure, touche le nom d'une piste pour changer son son, ou change un pad dans Sons › Batterie."), AppTab.Synthe, tr("Aller au clavier"), manual = true) else null,
    ) + listOf(
        NextStep(StepGoal.Volumes, mixed || StepGoal.Volumes in marks, tr("Écoute le morceau en entier et règle le volume de chaque piste dans Mix."), AppTab.Mixeur, tr("Aller à Mix"), manual = true),
        NextStep(
            StepGoal.Voice, voice || StepGoal.Voice in marks,
            tr("Si tu veux, pose ta voix : Sons › Voix, touche ● pour chanter ou rapper sur le morceau (avec des écouteurs)."), AppTab.Audio, tr("Aller à la voix"), manual = true,
        ),
        NextStep(StepGoal.Share, StepGoal.Share in marks, tr("Exporte ton morceau et fais-le écouter à tes amis !"), AppTab.Mixeur, tr("Partager"), manual = true),
    )
    // Done first, in order; what is left after, in order: the numbers follow.
    return steps.sortedBy { if (it.done) 0 else 1 }.mapIndexed { i, s -> s.copy(number = i + 1) }
}

/** The first step still to do, or null when the song is done. */
fun List<NextStep>.next(): NextStep? = firstOrNull { !it.done }
