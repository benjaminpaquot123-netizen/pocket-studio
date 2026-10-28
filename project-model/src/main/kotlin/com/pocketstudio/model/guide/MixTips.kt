package com.pocketstudio.model.guide

import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.LoudnessTargets
import com.pocketstudio.model.Mixer
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One tip of the Mode simple's « Mix » tab: what is wrong ([text]) and the button that fixes it
 * ([fix]). [ch] and [db]: the strip moved and by how much (dB); the master by [db] for "clip".
 */
data class MixTip(val id: String, val text: String, val fix: String, val ch: Int = -1, val db: Float = 0f)

/**
 * The Mode simple's mix tips, the most urgent first (only the first one shows): the output
 * clipping, a solo left on, a track measured out of its place by the Guide (the bass against the
 * kick first), then, unmeasured, a bass pushed over the assistant's level or a fader pushed near the top.
 */
object MixTips {
    /** A fader this high (dB) is « presque à fond » (the top is about +3.9 dB). */
    const val HOT_DB = 3f

    /** The bass this much over the assistant's level (dB) gets in the kick's way. */
    const val BASS_OVER_DB = 2f

    /** What the « Ça sature » tip takes off the master (dB). */
    const val CLIP_STEP_DB = 3f

    fun of(p: Project, clipping: Boolean = false): List<MixTip> {
        val tips = ArrayList<MixTip>()
        if (clipping && p.mixer.masterDb > Mixer.MASTER_MIN_DB) {
            tips += MixTip("clip", tr("Ça sature : le son grésille."), tr("Baisser la sortie de %s dB", num(CLIP_STEP_DB)), Mixer.MASTER, -CLIP_STEP_DB)
        }
        val facts = GuideFacts.of(p)
        val solos = facts.tracks.filter { it.channel != Mixer.MASTER && p.mixer.channels.getOrNull(it.channel)?.solo == true }
        if (solos.isNotEmpty()) tips += MixTip("solo", tr("Un solo est resté allumé : on n'entend plus que %s.", names(solos)), tr("Tout réentendre"))
        // The acid line is the bass of an acid song.
        val bassRoles = setOf(GuideRoles.BASS, GuideRoles.SUB, GuideRoles.BASS2, GuideRoles.ACID)
        val kickPlays = facts.playing(GuideRoles.KICK).isNotEmpty()
        // Measured by the Guide: the track furthest from its place, the bass first when it covers the kick.
        facts.fresh?.let { m ->
            val off = GuideLevels.offsets(facts, m).filter { abs(it.value) > GuideLevels.TOLERANCE_DB }
            off.entries.sortedWith(compareByDescending<Map.Entry<String, Float>> { e -> facts.trackOf(e.key)?.role in bassRoles && e.value > 0f }.thenByDescending { abs(it.value) })
                .firstOrNull()?.let { (key, offset) ->
                    val t = facts.trackOf(key) ?: return@let
                    val db = -offset.coerceIn(-GuideLevels.MAX_MOVE_DB, GuideLevels.MAX_MOVE_DB)
                    tips += tip("level:$key", t, db, kickPlays && t.role in bassRoles)
                }
        }
        // Not measured: what the faders say.
        val heard = facts.tracks.filter { it.channel != Mixer.MASTER && it.channel in 0 until Mixer.CHANNELS && !it.key.startsWith("bus:") }
            .distinctBy { it.channel }
        if (kickPlays) {
            heard.filter { it.role in bassRoles }.firstOrNull { t -> overStart(p, t.channel) >= BASS_OVER_DB }?.let { t ->
                tips += tip("bass:${t.key}", t, -BASS_OVER_DB, bass = true)
            }
        }
        // Testers (lot « tuto 5 »): only a track that plays (« Rim couvre les autres » for a pad never touched).
        heard.firstOrNull { t -> facts.song.plays(t.key) && db(p.mixer.channels[t.channel].volume) >= HOT_DB && overStart(p, t.channel) > 0.5f }?.let { t -> tips += tip("hot:${t.key}", t, -HOT_DB, bass = false) }
        return tips.distinctBy { it.ch.takeIf { ch -> ch >= 0 } ?: it.id }
    }

    /** [p] with [tip]'s fix (one undo step for whoever applies it). */
    fun apply(p: Project, tip: MixTip): Project = when {
        tip.id == "clip" -> p.copy(mixer = p.mixer.copy(masterDb = (p.mixer.masterDb + tip.db).coerceIn(Mixer.MASTER_MIN_DB, Mixer.MASTER_MAX_DB)))
        tip.id == "solo" -> p.copy(mixer = p.mixer.copy(channels = p.mixer.channels.map { if (it.solo) it.copy(solo = false) else it }))
        tip.ch in p.mixer.channels.indices -> {
            val channels = p.mixer.channels.toMutableList()
            val c = channels[tip.ch]
            channels[tip.ch] = c.copy(volume = GuideLevels.faderOf(db(c.volume) + tip.db))
            p.copy(mixer = p.mixer.copy(channels = channels))
        }
        else -> p
    }

    private fun tip(id: String, t: GuideTrack, db: Float, bass: Boolean): MixTip {
        val name = DefaultNames.shown(t.name)
        val amount = num(abs(db))
        return when {
            bass && db < 0f -> MixTip(id, tr("La basse et le kick se gênent."), tr("Baisser la basse de %s dB", amount), t.channel, db)
            db < 0f -> MixTip(id, tr("« %s » couvre les autres.", name), tr("Le baisser de %s dB", amount), t.channel, db)
            else -> MixTip(id, tr("« %s » se perd dans le mix.", name), tr("Le monter de %s dB", amount), t.channel, db)
        }
    }

    private fun overStart(p: Project, ch: Int): Float = db(p.mixer.channels[ch].volume) - db(p.mixer.startVolume(ch))

    private fun db(v: Float): Float = GuideLevels.faderDb(v)

    /** A move in dB as said: to the half dB, « 2 » or « 2,5 ». */
    private fun num(db: Float): String = LoudnessTargets.formatTarget((db * 2f).roundToInt() / 2f)

    private fun names(ts: List<GuideTrack>): String = ts.joinToString(", ") { DefaultNames.shown(it.name) }
}
