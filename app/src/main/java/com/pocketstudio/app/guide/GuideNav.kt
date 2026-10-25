package com.pocketstudio.app.guide

import com.pocketstudio.app.mixer.Where
import com.pocketstudio.app.ui.AppTab
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.GuideRoles
import com.pocketstudio.model.guide.GuideTrack
import com.pocketstudio.model.guide.guideTracks

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

    /** The drum kit's sheet. */
    data object Kit : GuideGo()

    /** The 303's sounds. */
    data object AcidSound : GuideGo()

    /** Instrument track [track]'s notes (Sons › its piano roll). */
    data class Notes(val track: Int) : GuideGo()

    /** « Ce qui joue », for the whole song. */
    data object WhatPlays : GuideGo()

    /** « Prêt à partager ? ». */
    data object Share : GuideGo()

    data object Video : GuideGo()
}

object GuideNav {
    /** Every screen the parcours' options can open ([com.pocketstudio.model.guide.GuideAction.spot]). */
    val SPOTS = setOf(
        "tempo", "key", "song.parts", "sounds.track", "sounds.favorite", "sounds.layers", "rhythm.kit", "rhythm.pattern", "song.automation",
        "record.voice", "mixer.master", "mixer.whatplays", "notes.track", "mixer.strip", "share.check", "share.export", "share.video", "acid",
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
            "sounds.track", "sounds.favorite", "sounds.layers" -> soundOf(track) ?: structure
            "rhythm.kit" -> GuideGo.Kit
            "rhythm.pattern" -> GuideGo.Tab(AppTab.Rythme, listOf("step_cells"))
            "song.automation" -> if (simple) structure else GuideGo.Tab(AppTab.Mixeur, listOf("mixer_automation"), mixer = Where.Lane(-1))
            "record.voice" -> GuideGo.Tab(AppTab.Audio, listOf("record_start"), audioPage = 0)
            "mixer.master" -> if (simple) GuideGo.Tab(AppTab.Envoyer, listOf("share_go")) else GuideGo.Tab(AppTab.Mixeur, listOf("mixer_master"), mixer = Where.Master)
            "mixer.whatplays" -> GuideGo.WhatPlays
            "notes.track" -> track?.key?.removePrefix("inst:")?.takeIf { track.key.startsWith("inst:") }?.toIntOrNull()?.let { GuideGo.Notes(it) }
                ?: GuideGo.Tab(AppTab.Synthe)
            "mixer.strip" -> if (simple) GuideGo.Tab(AppTab.Morceau, listOf("simple_track_*")) else GuideGo.Tab(AppTab.Mixeur, listOf("console_fader"))
            "share.check", "share.export" -> if (simple) GuideGo.Tab(AppTab.Envoyer, listOf("share_go")) else GuideGo.Share
            "share.video" -> GuideGo.Video
            "acid" -> if (simple) GuideGo.AcidSound else GuideGo.Tab(AppTab.Acide, listOf("acid_generate"))
            else -> structure
        }
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
