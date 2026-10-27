package com.pocketstudio.app.guide

import com.pocketstudio.app.mixer.Where
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.GuideRoles
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.guide.guideTracks
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks

/**
 * Where « Voir le réglage » goes (an option of a card that opens a screen,
 * [com.pocketstudio.model.guide.GuideAction.OPEN]): a screen of the app, then the control lit
 * ([spots], ids of [com.pocketstudio.app.learn.spot], the first one on the screen), or a sheet
 * that is the control itself (the tempo, a track's sounds).
 */
sealed class GuideGo {
    /** Space [tab]; [partsView]: Structure's list of parts; [audioPage]: the Audio tab's page; [mixer]: the mixer's card. */
    data class Tab(
        val tab: AppTab,
        val spots: List<String> = emptyList(),
        val partsView: Boolean = false,
        val audioPage: Int? = null,
        val mixer: Where? = null,
    ) : GuideGo()

    data object Tempo : GuideGo()

    /** « Accords du morceau » (the key is changed there). */
    data object Chords : GuideGo()

    /** « Changer le son » of instrument track [track]. */
    data class Sound(val track: Int) : GuideGo()

    /** G3: the layers of instrument track [track] (its [LayersEditor]). */
    data class Layers(val track: Int) : GuideGo()

    /** The drum kit's sheet. */
    data object Kit : GuideGo()

    /** Audit final: the sheet of drum pad [lane] (its sound only: the rest of the kit stays). */
    data class Pad(val lane: Int) : GuideGo()

    /** Audit final: a 303 line made in the song's key (when it has none), then its sounds (« Générer une basse acide »). */
    data object AcidGenerate : GuideGo()

    /** The 303's sounds. */
    data object AcidSound : GuideGo()

    /** Instrument track [track]'s notes (Sons › its piano roll). */
    data class Notes(val track: Int) : GuideGo()

    /** « Ce qui joue », for the whole song. */
    data object WhatPlays : GuideGo()

    /** « Prêt à partager ? ». */
    data object Share : GuideGo()

    data object Video : GuideGo()

    /** G5: the song plays, from about 8 bars before its first Drop ([fromDrop]) or from its start. */
    data class Play(val fromDrop: Boolean) : GuideGo()

    /** G5: the guide the assistant wrote for this song (its pages). */
    data object SongGuide : GuideGo()

    /** G5: the assistant, to choose a style (« Mon premier morceau »'s first step). */
    data object NewStyle : GuideGo()

    /** G5: the Remix (Sons › Remix). */
    data object Remix : GuideGo()

    /** G5: instrument track [track] on the keyboard, « ● Enregistrer ma mélodie » lit ([simple]: the Mode simple's Clavier). */
    data class Record(val track: Int) : GuideGo()
}

object GuideNav {
    /** Every screen the parcours' options can open ([com.pocketstudio.model.guide.GuideAction.spot]). */
    val SPOTS = setOf(
        "tempo", "key", "song.parts", "sounds.track", "sounds.favorite", "sounds.layers", "rhythm.kit", "rhythm.pattern", "song.automation",
        "record.voice", "mixer.master", "mixer.whatplays", "notes.track", "mixer.strip", "share.check", "share.export", "share.video", "acid", "acid.generate",
        // G5: the common cards (« Pour commencer », the Remix) and « Écouter ».
        "new.style", "song.guide", "sounds.main", "record.melody", "remix", GuideViewModel.PLAY,
    )

    /**
     * Where [open] goes in [p], in the Mode simple ([simple]: its four tabs, no mixer: the tracks'
     * volumes are on « Mon morceau », the master on « Envoyer ») or in the full app.
     */
    fun go(open: GuideOpen, p: Project, simple: Boolean): GuideGo {
        val track = trackOf(p, open.target)
        val structure = GuideGo.Tab(AppTab.Morceau, if (simple) listOf("simple_track_*") else listOf("timeline_name_*"))
        return when (open.spot) {
            "tempo" -> GuideGo.Tempo
            "key" -> GuideGo.Chords
            "song.parts" -> GuideGo.Tab(AppTab.Morceau, listOf("part_play_0", "structure_switch"), partsView = !simple)
            "sounds.track", "sounds.favorite" -> soundOf(track) ?: structure
            // G3: an instrument track's layers in their editor; another track's sound as before.
            "sounds.layers" -> track?.key?.takeIf { it.startsWith("inst:") }?.removePrefix("inst:")?.toIntOrNull()?.let { GuideGo.Layers(it) } ?: soundOf(track) ?: structure
            // Audit final: a drum role's card changes that pad's sound, not the whole kit.
            "rhythm.kit" -> track?.key?.takeIf { it.startsWith("pad:") }?.removePrefix("pad:")?.toIntOrNull()?.let { GuideGo.Pad(it) } ?: GuideGo.Kit
            "rhythm.pattern" -> GuideGo.Tab(AppTab.Rythme, listOf("step_cells"))
            "song.automation" -> if (simple) structure else GuideGo.Tab(AppTab.Mixeur, listOf("mixer_automation"), mixer = Where.Lane(-1))
            "record.voice" -> GuideGo.Tab(AppTab.Audio, listOf("record_start"), audioPage = 0)
            "mixer.master" -> if (simple) GuideGo.Tab(AppTab.Envoyer, listOf("share_go")) else GuideGo.Tab(AppTab.Mixeur, listOf("mixer_master"), mixer = Where.Master)
            "mixer.whatplays" -> GuideGo.WhatPlays
            "notes.track" -> track?.key?.removePrefix("inst:")?.takeIf { track.key.startsWith("inst:") }?.toIntOrNull()?.let { GuideGo.Notes(it) }
                ?: GuideGo.Tab(AppTab.Synthe)
            // Audit final: the volume's button lit (not the line that changes the sound).
            "mixer.strip" -> if (simple) GuideGo.Tab(AppTab.Morceau, listOf("simple_volume_*")) else GuideGo.Tab(AppTab.Mixeur, listOf("console_fader"))
            "share.check", "share.export" -> if (simple) GuideGo.Tab(AppTab.Envoyer, listOf("share_go")) else GuideGo.Share
            "share.video" -> GuideGo.Video
            "acid" -> if (simple) GuideGo.AcidSound else GuideGo.Tab(AppTab.Acide, listOf("acid_generate"))
            "acid.generate" -> if (simple) GuideGo.AcidGenerate else GuideGo.Tab(AppTab.Acide, listOf("acid_generate"))
            GuideViewModel.PLAY -> GuideGo.Play(fromDrop = open.target == "drop")
            "song.guide" -> GuideGo.SongGuide
            "new.style" -> GuideGo.NewStyle
            "remix" -> GuideGo.Remix
            "sounds.main" -> mainTrackOf(p)?.let { GuideGo.Sound(it) } ?: structure
            "record.melody" -> GuideGo.Record(mainTrackOf(p) ?: p.instrumentTracks.firstOrNull()?.id ?: 0)
            else -> structure
        }
    }

    /** Notes below this pitch (C3) make a track a bass line. */
    private const val BASS_BELOW = 48

    /** The default names of the tracks that carry the tune (the assistant's lead, its counter-melody). */
    private val TUNE_NAMES = setOf("Lead", "Mélodie", "Contre-chant")

    /**
     * The track « Changer un son » opens (from « Ton morceau est prêt », now a card): the instrument
     * track that carries the tune. The one named for it (« Lead », « Mélodie »…) first; else, among
     * the tracks that are not a bass line, the one playing highest; a bass line only if nothing else
     * plays; null when no track has notes.
     */
    fun mainTrackOf(project: Project): Int? {
        val scenes = project.allScenes
        val tracks = project.instrumentTracks.map { t ->
            val notes = scenes.flatMap { s -> (if (t.id == 0) s.synthNotes else s.trackNotes[t.id])?.notes.orEmpty() }
            Triple(t.id, t.name, notes)
        }.filter { it.third.isNotEmpty() }
        if (tracks.isEmpty()) return null
        tracks.firstOrNull { (_, name, _) -> (com.pocketstudio.model.DefaultNames.french(name) ?: name) in TUNE_NAMES }?.let { return it.first }
        fun median(notes: List<com.pocketstudio.model.Note>) = notes.map { it.pitch }.sorted()[notes.size / 2]
        val tunes = tracks.filter { (_, _, notes) -> median(notes) >= BASS_BELOW }
        if (tunes.isEmpty()) return tracks.maxByOrNull { it.third.size }?.first
        return tunes.maxByOrNull { (_, _, notes) -> median(notes) }?.first
    }

    /** The track [role] is about ([GuideRoles]): the first one of that role, null for the whole song or none. */
    fun trackOf(p: Project, role: String): GuideTrack? =
        if (role.isEmpty() || role == GuideRoles.SONG) null else p.guideTracks().firstOrNull { it.role == role }

    /** The sheet that changes [track]'s sound. */
    private fun soundOf(track: GuideTrack?): GuideGo? {
        val key = track?.key ?: return null
        return when {
            key.startsWith("inst:") -> key.removePrefix("inst:").toIntOrNull()?.let { GuideGo.Sound(it) }
            key == GuideTrack.DRUMS || key.startsWith("pad:") -> GuideGo.Kit
            key == GuideTrack.ACID -> GuideGo.AcidSound
            key.startsWith("audio:") -> GuideGo.Tab(AppTab.Audio, listOf("record_start"), audioPage = 0)
            else -> null
        }
    }
}
