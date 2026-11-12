package com.pocketstudio.model.guide

import com.pocketstudio.model.AudioTrack
import com.pocketstudio.model.Channel
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.SongPart
import com.pocketstudio.model.SoundKind
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.TrackRoles
import com.pocketstudio.model.allScenes
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.soundKind

/**
 * The roles a card is about ([GuideCard.target]) and how a project's tracks take them: the
 * assistant's tracks by their (French) names, a track made by hand by its sound's kind, the drum
 * lines by their pads (the assistant's lanes, [StarterWizard.Role.lane]).
 */
object GuideRoles {
    const val SONG = "song"
    const val KICK = "kick"
    const val SNARE = "snare"
    const val HATS = "hats"
    const val PERC = "perc"
    const val RUMBLE = "rumble"
    const val DRUMS = "drums"
    const val BASS = "bass"
    const val SUB = "sub"
    const val BASS2 = "bass2"
    const val SUB_DROP = "subdrop"
    const val ACID = "acid"
    const val PAD = "pad"
    const val LEAD = "lead"
    const val COUNTER = "counter"
    const val VOCAL = "vocal"
    const val RISER = "riser"
    const val MASTER = "master"
    const val SENDS = "sends"
    const val AUDIO = "audio"

    /** The pads of each drum role: the kick, the snare and its clap, the hats and cymbals, the percussions. */
    val PAD_LANES: Map<String, List<Int>> = mapOf(
        KICK to listOf(StarterWizard.Role.Kick.lane),
        SNARE to listOf(StarterWizard.Role.Snare.lane, StarterWizard.Role.Clap.lane),
        HATS to listOf(StarterWizard.Role.Hat.lane, StarterWizard.Role.OpenHat.lane, StarterWizard.Role.Ride.lane),
        PERC to listOf(StarterWizard.Role.Perc.lane, StarterWizard.Role.Perc2.lane, StarterWizard.Role.Tom.lane, 7, 9, 11, 14, 15),
        RUMBLE to listOf(StarterWizard.Role.Rumble.lane),
    )

    /** The 808 played by a pad (the trap songs' bass when no 808 sample is on the phone). */
    val BASS_808_LANE = StarterWizard.Role.Bass808.lane

    /** Roles whose low end belongs in the centre, without a low cut. */
    val LOW = setOf(KICK, BASS, SUB, BASS2, SUB_DROP, RUMBLE)

    /** The pro order of the tracks (docs/guide/recherche.md §2.2): the sheets are listed in it. */
    val ORDER = listOf(KICK, SNARE, HATS, PERC, RUMBLE, DRUMS, BASS, SUB, BASS2, SUB_DROP, ACID, PAD, LEAD, COUNTER, VOCAL, RISER, AUDIO, SENDS, MASTER)

    /** The role of instrument track [t]. */
    fun of(t: InstrumentTrack): String {
        val fr = DefaultNames.french(t.name) ?: t.name
        return when {
            fr == StarterWizard.SUB_TRACK -> SUB
            fr == StarterWizard.BASS_SWITCH_TRACK -> BASS2
            fr == StarterWizard.SUB_DROP_TRACK -> SUB_DROP
            TrackRoles.isRiserName(fr) -> RISER
            fr == StarterWizard.VOCAL_TRACK -> VOCAL
            fr == StarterWizard.COUNTER_TRACK -> COUNTER
            fr == "Lead" -> LEAD
            TrackRoles.isPadName(fr) -> PAD
            TrackRoles.isBassName(fr) -> BASS
            else -> when (t.soundKind()) {
                SoundKind.BASS -> BASS
                SoundKind.PAD, SoundKind.KEYS -> PAD
                else -> LEAD
            }
        }
    }
}

/**
 * The tracks of a project as the Guide sees them: each with a key that stays the same while the
 * project changes ("inst:3", "pad:0", "drums", "acid", "audio:1", "master", "bus:0"), its role,
 * its name and its mixer strip.
 */
data class GuideTrack(val key: String, val role: String, val name: String, val channel: Int) {
    companion object {
        fun inst(id: Int) = "inst:$id"
        fun pad(lane: Int) = "pad:$lane"
        const val DRUMS = "drums"
        const val ACID = "acid"
        const val MASTER = "master"
        fun bus(n: Int) = "bus:$n"
        fun audio(id: Int) = "audio:$id"

        /** The mixer strip of track key [key] ([Mixer.MASTER] for the master), or null. */
        fun channelOf(key: String): Int? = when {
            key == DRUMS -> 0
            key == ACID -> 1
            key == MASTER -> Mixer.MASTER
            key.startsWith("inst:") -> key.removePrefix("inst:").toIntOrNull()?.let(InstrumentTrack::mixerChannel)
            key.startsWith("pad:") -> key.removePrefix("pad:").toIntOrNull()?.takeIf { it in 0 until Mixer.PADS }?.let(Mixer::padChannel)
            key.startsWith("audio:") -> key.removePrefix("audio:").toIntOrNull()?.let(AudioTrack::mixerChannel)
            else -> null
        }

        /** The song part key ([SongPart.plays]) of track key [key] ("drums" for a pad), or null. */
        fun partKey(key: String): String? = when {
            key == DRUMS || key.startsWith("pad:") -> SongPart.DRUMS
            key == ACID -> SongPart.ACID
            key.startsWith("inst:") -> key.removePrefix("inst:").toIntOrNull()?.let(SongPart::inst)
            key.startsWith("audio:") -> key.removePrefix("audio:").toIntOrNull()?.let(SongPart::audio)
            else -> null
        }
    }
}

/** Every track of [p] the Guide can talk about, in the pro order: the pads with hits, the drums, the 303 when it plays, the instruments, the audio tracks. */
fun Project.guideTracks(): List<GuideTrack> {
    val out = ArrayList<GuideTrack>()
    val scenes = allScenes
    for (lane in 0 until Mixer.PADS) {
        val hits = scenes.any { s -> s.drums.lanes.getOrNull(lane)?.hits?.keys?.any { it < s.drums.steps } == true }
        if (!hits) continue
        val role = GuideRoles.PAD_LANES.entries.firstOrNull { lane in it.value }?.key
            ?: if (lane == GuideRoles.BASS_808_LANE) GuideRoles.BASS else GuideRoles.PERC
        out += GuideTrack(GuideTrack.pad(lane), role, DefaultNames.KIT_PADS.getOrElse(lane) { "Pad ${lane + 1}" }, Mixer.padChannel(lane))
    }
    if (out.isNotEmpty()) out += GuideTrack(GuideTrack.DRUMS, GuideRoles.DRUMS, "Batterie", 0)
    if (scenes.any { s -> s.acid.steps.take(s.acid.length.coerceAtLeast(0)).any { it.gate } }) out += GuideTrack(GuideTrack.ACID, GuideRoles.ACID, DefaultNames.ACID, 1)
    for (t in instrumentTracks) out += GuideTrack(GuideTrack.inst(t.id), GuideRoles.of(t), t.name, InstrumentTrack.mixerChannel(t.id))
    for (a in audioTracks) out += GuideTrack(GuideTrack.audio(a.id), GuideRoles.AUDIO, a.name, AudioTrack.mixerChannel(a.id))
    return out.sortedBy { GuideRoles.ORDER.indexOf(it.role).let { i -> if (i < 0) GuideRoles.ORDER.size else i } }
}

/** The strip of [t] in this project (the master's has no strip: null). */
fun Project.stripOf(t: GuideTrack): Channel? = mixer.channels.getOrNull(t.channel)?.takeIf { t.channel != Mixer.MASTER }
