package com.pocketstudio.app.padlive

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.guide.GuideListenPlayer
import com.pocketstudio.app.report.AppLog
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.session.livePadColor
import com.pocketstudio.model.LiveClip
import com.pocketstudio.model.PadChallenge
import com.pocketstudio.model.PadLive
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.ui.Neon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pad Live's challenge (« refais ce rythme », lot B3), as its panel shows it. */
sealed interface ChallengeUi {
    /** Closed: the effects are there instead. */
    data object Off : ChallengeUi
    /** The page has no « coup » pad to play a rhythm on. */
    data object NoShots : ChallengeUi
    /** Ready: « Écouter » plays the rhythm. */
    data class Ready(val c: PadChallenge.Challenge) : ChallengeUi
    /** The rhythm plays: hit [at] sounds now (-1: between two). */
    data class Listening(val c: PadChallenge.Challenge, val at: Int) : ChallengeUi
    /** The player plays it back: [taps] touches so far. */
    data class YourTurn(val c: PadChallenge.Challenge, val taps: Int) : ChallengeUi
    /** Played back: how it went. */
    data class Done(val c: PadChallenge.Challenge, val result: PadChallenge.Result) : ChallengeUi
}

/**
 * The challenges' logic, held by [PadLiveController.challenge]: the rhythm of the level to play
 * made on the « coup » pads of the page shown, its example rendered from the pads' own sounds (as
 * the engine plays them) and played once, the touches timed, then scored ([PadChallenge.score]) and
 * the best stars kept with the project (not an edit: never undone). Nothing here keeps time for
 * the music: the example is one sound, rendered beforehand, and the touches only need their gaps.
 */
class ChallengeController(private val vm: RhythmViewModel) {
    private val _ui = MutableStateFlow<ChallengeUi>(ChallengeUi.Off)
    val ui: StateFlow<ChallengeUi> = _ui.asStateFlow()

    /** Plays the example (the engine's preview, set by the route; the bench gives its own). */
    var player: GuideListenPlayer? = null

    /** The clock the touches are timed on (milliseconds). */
    internal var now: () -> Long = { SystemClock.uptimeMillis() }

    /** Tests: the example's sound (no pad sounds on disk there). */
    internal var exampleForTest: ((PadChallenge.Challenge) -> Pcm?)? = null

    private val taps = ArrayList<PadChallenge.Tap>()
    private var job: Job? = null

    /** « Défi »: the panel opens on the next challenge to win (or [level]). */
    fun open(level: Int? = null) {
        stopExample()
        taps.clear()
        val p = vm.project.value
        val c = PadChallenge.make(level ?: p.padLive.nextChallenge, PadChallenge.shotsOf(p.live, p.padLive.shownPage), p.tempoBpm)
        _ui.value = if (c == null) ChallengeUi.NoShots else ChallengeUi.Ready(c)
        AppLog.add(if (c == null) "Défi : aucun pad « coup » sur la page" else "Défi ${c.level} prêt")
    }

    /** Audit ultra round 1, n° 9: « il faut des pads coup » stayed after a pack brought them: asked again as the pads change. */
    fun refresh() {
        if (_ui.value == ChallengeUi.NoShots) open()
    }

    fun close() {
        stopExample()
        taps.clear()
        _ui.value = ChallengeUi.Off
    }

    val open: Boolean get() = _ui.value != ChallengeUi.Off

    /** The challenge shown (null: none). */
    private fun current(): PadChallenge.Challenge? = when (val u = _ui.value) {
        is ChallengeUi.Ready -> u.c
        is ChallengeUi.Listening -> u.c
        is ChallengeUi.YourTurn -> u.c
        is ChallengeUi.Done -> u.c
        else -> null
    }

    /**
     * « Écouter »: the music stops (the rhythm is heard alone), the example plays once with its pads
     * lit as it goes, then it is the player's turn.
     */
    fun listen() {
        val c = current() ?: return
        stopExample()
        taps.clear()
        if (vm.transport.value.playing) vm.togglePlay()
        if (vm.padsSoundingNow().isNotEmpty()) vm.stopLive()
        job = vm.viewModelScope.launch {
            val pcm = try {
                exampleForTest?.invoke(c) ?: withContext(Dispatchers.Default) { example(c) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.add("Défi : exemple impossible (${e.message ?: e.javaClass.simpleName})")
                null
            }
            val play = player
            if (pcm == null || play == null) {
                // Not heard: the player can still play it back from the pads' names.
                _ui.value = ChallengeUi.YourTurn(c, 0)
                return@launch
            }
            _ui.value = ChallengeUi.Listening(c, -1)
            val ended = java.util.concurrent.atomic.AtomicBoolean(false)
            if (!play.play(pcm) { ended.set(true) }) {
                _ui.value = ChallengeUi.YourTurn(c, 0)
                return@launch
            }
            // The pads light where the example is heard (read from what really went out).
            val rate = pcm.sampleRate
            val starts = c.hits.indices.map { PadChallenge.frameOf(c, it, rate).toLong() }
            while (!ended.get()) {
                val at = litHit(starts, play.heard(), rate)
                if ((_ui.value as? ChallengeUi.Listening)?.at != at) _ui.value = ChallengeUi.Listening(c, at)
                delay(FRAME_MS)
            }
            if (_ui.value is ChallengeUi.Listening) _ui.value = ChallengeUi.YourTurn(c, 0)
        }
    }

    /** « À toi » straight away (the example skipped or heard already). */
    fun yourTurn() {
        val c = current() ?: return
        stopExample()
        taps.clear()
        _ui.value = ChallengeUi.YourTurn(c, 0)
    }

    /** A finger down on pad [pad] (the screen or a controller): counted while it is the player's turn. */
    fun tapped(pad: Int) {
        val u = _ui.value as? ChallengeUi.YourTurn ?: return
        taps += PadChallenge.Tap(pad, now())
        // As many touches as the rhythm has hits: scored at once (« J'ai fini » before that).
        if (taps.size >= u.c.hits.size) finish() else _ui.value = u.copy(taps = taps.size)
    }

    /** « J'ai fini » (or every hit played): scored, the best stars kept. */
    fun finish() {
        val c = (_ui.value as? ChallengeUi.YourTurn)?.c ?: return
        val result = PadChallenge.score(c, taps)
        taps.clear()
        _ui.value = ChallengeUi.Done(c, result)
        vm.session.editQuietly { it.copy(padLive = it.padLive.withStars(c.level, result.stars)) }
        AppLog.add("Défi ${c.level} : ${result.right}/${result.total}, ${result.stars} étoile(s)")
    }

    /** « Rejouer » the same challenge. */
    fun again() {
        val c = current() ?: return
        open(c.level)
    }

    /** « Défi suivant ». */
    fun next() {
        val c = current() ?: return
        open((c.level + 1).coerceAtMost(PadChallenge.LEVELS.size))
    }

    private fun stopExample() {
        job?.cancel()
        job = null
        player?.stop()
    }

    /** The pads that sound in the example now (lit as if they played). */
    fun litPad(): Int? {
        val u = _ui.value as? ChallengeUi.Listening ?: return null
        return u.c.hits.getOrNull(u.at)?.pad
    }

    /**
     * The example: each hit's pad sound (its pitch, reverse and attack, at its volume) where the
     * rhythm puts it, stereo at the engine's rate. Off the main thread.
     */
    private fun example(c: PadChallenge.Challenge): Pcm? {
        val rate = ResampleSource.RATE
        val clips = vm.project.value.live
        val sounds = HashMap<Int, Pcm>()
        for (pad in c.hits.map { it.pad }.distinct()) {
            val clip: LiveClip = clips.at(pad) ?: continue
            val src = runCatching { PadSound.shaped(vm.padSamples.read(clip.file), clip) }.getOrNull() ?: continue
            if (src.sampleRate != rate) continue
            sounds[pad] = src
        }
        if (sounds.isEmpty()) return null
        val frames = PadChallenge.exampleFrames(c, rate)
        val out = FloatArray(frames * 2)
        for ((i, h) in c.hits.withIndex()) {
            val src = sounds[h.pad] ?: continue
            val gain = clips.at(h.pad)?.gain ?: 1f
            val at = PadChallenge.frameOf(c, i, rate)
            // Each hit until the next one on its own pad (a hit tapped again starts over, as on the engine).
            val nextSame = c.hits.withIndex().firstOrNull { it.index > i && it.value.pad == h.pad }?.let { PadChallenge.frameOf(c, it.index, rate) } ?: frames
            val n = minOf(src.frames, nextSame - at, frames - at)
            for (k in 0 until n) {
                val o = (at + k) * 2
                if (src.channels == 2) {
                    out[o] += src.data[k * 2] * gain
                    out[o + 1] += src.data[k * 2 + 1] * gain
                } else {
                    val v = src.data[k] * gain
                    out[o] += v
                    out[o + 1] += v
                }
            }
        }
        // Never louder than full scale (several hits on top of one another).
        val peak = out.maxOfOrNull { kotlin.math.abs(it) } ?: 0f
        if (peak > 0.98f) for (k in out.indices) out[k] *= 0.98f / peak
        return Pcm(out, 2, rate)
    }

    companion object {
        /** How long a pad stays lit for a hit of the example, at least. */
        const val LIT_MS = 180
        /** The dark gap before the next hit, so that a pad hit twice in a row flashes twice. */
        const val GAP_MS = 70
        private const val FRAME_MS = 30L

        /**
         * The hit of the example lit when [heard] frames went out ([starts]: each hit's frame), -1
         * before the first, past the last's [LIT_MS], or in the short gap before the next. Audit ultra
         * round 1, n° 9: lit for 180 ms only, the pads were dark most of the example (« regarde les
         * pads s'allumer », none was): a hit's pad now stays lit until just before the next one.
         */
        fun litHit(starts: List<Long>, heard: Long, rate: Int): Int {
            if (heard < 0) return -1
            val at = starts.indexOfLast { it <= heard }.takeIf { it >= 0 } ?: return -1
            val minLit = rate.toLong() * LIT_MS / 1000
            val gap = rate.toLong() * GAP_MS / 1000
            val until = starts.getOrNull(at + 1)?.let { maxOf(it - gap, starts[at] + minLit) } ?: (starts[at] + minLit)
            return if (heard < until) at else -1
        }
    }
}

/** A challenge's name as shown (« Défi 2 · Ping-pong »). */
internal fun challengeTitle(c: PadChallenge.Challenge): String = tr("Défi %1\$d · %2\$s", c.level, levelName(c.level))

/** A level's name in the language shown ([PadChallenge.LEVELS]' French names, in order). */
internal fun levelName(level: Int): String = when (level) {
    1 -> tr("Boum boum")
    2 -> tr("Ping-pong")
    3 -> tr("Le galop")
    4 -> tr("Le contretemps")
    5 -> tr("Le roulement")
    else -> tr("Le boss")
}

/**
 * The challenge's panel, in place of the scene effects while it is open: what to do now in one
 * line, the rhythm as dots (one per hit, its pad's colour; lit as the example plays, filled as the
 * player plays, green or red once scored), and at most two buttons.
 */
@Composable
internal fun ChallengePanel(
    ui: ChallengeUi, pads: com.pocketstudio.model.LivePads, actions: ChallengeActions,
    /** The best stars of each level ([com.pocketstudio.model.PadLiveSet.stars]). */
    best: List<Int> = emptyList(),
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Neon.Surface).border(1.dp, Neon.Amber.copy(alpha = 0.45f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp).testTag("padlive_challenge"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val c = when (ui) {
            is ChallengeUi.Ready -> ui.c
            is ChallengeUi.Listening -> ui.c
            is ChallengeUi.YourTurn -> ui.c
            is ChallengeUi.Done -> ui.c
            else -> null
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                c?.let { challengeTitle(it) } ?: tr("Défi"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                color = Neon.Amber, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("padlive_challenge_title"),
            )
            com.pocketstudio.ui.HelpDot(com.pocketstudio.app.help.ScreenHelp.PADLIVE_CHALLENGE, about = listOf("padlive_challenge_listen", "padlive_challenge_go", "padlive_challenge_finish", "padlive_challenge_again", "padlive_challenge_next") + (1..PadChallenge.LEVELS.size).map { "padlive_challenge_level_$it" })
            IconButton(onClick = actions.close, modifier = Modifier.size(48.dp).testTag("padlive_challenge_close")) {
                Icon(Icons.Filled.Close, contentDescription = tr("Fermer le défi"), tint = Neon.TextSecondary)
            }
        }
        val line = when (ui) {
            ChallengeUi.NoShots -> tr("Il faut des pads « coup » sur cette page : choisis un pack ou une autre page.")
            is ChallengeUi.Ready -> tr("Écoute le rythme, puis refais-le sur les pads.")
            is ChallengeUi.Listening -> tr("Écoute bien… regarde les pads qui s'allument.")
            is ChallengeUi.YourTurn -> if (ui.taps == 0) tr("À toi ! Joue-le sur les pads.") else tr("À toi ! %1\$d sur %2\$d", ui.taps, ui.c.hits.size)
            is ChallengeUi.Done -> resultWords(ui.result)
            ChallengeUi.Off -> ""
        }
        Text(
            line, style = MaterialTheme.typography.bodyMedium, color = Neon.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.testTag("padlive_challenge_line"),
        )
        // Audit ultra round 1, n° 9: every challenge with its best stars, one touch to play it (it went back
        // to challenge 1 with no list, and a try worse than the best seemed to wipe it).
        if (c != null && (ui is ChallengeUi.Ready || ui is ChallengeUi.Done)) LevelList(c.level, best, actions.pick)
        if (c != null) {
            // The rhythm's dots, said as one (TalkBack had a row of nameless dots).
            val said = when (ui) {
                is ChallengeUi.Done -> tr("Rythme · coups : %1\$d · réussis : %2\$d", c.hits.size, ui.result.right)
                is ChallengeUi.YourTurn -> tr("Rythme · coups : %1\$d · joués : %2\$d", c.hits.size, ui.taps)
                else -> tr("Rythme · coups : %d", c.hits.size)
            }
            Row(
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = said }.testTag("padlive_challenge_dots"),
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ui is ChallengeUi.Done) Stars(ui.result.stars)
                c.hits.forEachIndexed { i, h ->
                    val color = pads.at(h.pad)?.let { livePadColor(it.name, h.pad % PadLive.PAGE_SIZE) } ?: Neon.TextSecondary
                    val fill = when (ui) {
                        is ChallengeUi.Listening -> if (i == ui.at) color else color.copy(alpha = 0.25f)
                        is ChallengeUi.YourTurn -> if (i < ui.taps) color else Color.Transparent
                        is ChallengeUi.Done -> if (ui.result.matched.getOrElse(i) { false }) Neon.Acid else Neon.Red
                        else -> color.copy(alpha = 0.25f)
                    }
                    Box(Modifier.size(14.dp).clip(CircleShape).background(fill).border(1.5.dp, color, CircleShape))
                }
            }
        }
        // The best stays, said when this try did less (on its own line: the buttons keep their words).
        if (ui is ChallengeUi.Done) {
            val record = best.getOrElse(ui.c.level - 1) { 0 }
            if (record > ui.result.stars) {
                Text(tr("Ton record : %d sur 3", record), style = MaterialTheme.typography.labelMedium, color = Neon.Amber, modifier = Modifier.testTag("padlive_challenge_record"))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (ui) {
                is ChallengeUi.Ready -> {
                    ChallengeButton(Icons.AutoMirrored.Filled.VolumeUp, tr("Écouter"), "padlive_challenge_listen", Modifier.weight(1f), main = true, onClick = actions.listen)
                    ChallengeButton(androidx.compose.material.icons.Icons.Filled.TouchApp, tr("À moi !"), "padlive_challenge_go", Modifier.weight(1f), onClick = actions.yourTurn)
                }
                is ChallengeUi.Listening -> ChallengeButton(androidx.compose.material.icons.Icons.Filled.TouchApp, tr("À moi !"), "padlive_challenge_go", Modifier.weight(1f), onClick = actions.yourTurn)
                is ChallengeUi.YourTurn -> {
                    ChallengeButton(Icons.AutoMirrored.Filled.VolumeUp, tr("Réécouter"), "padlive_challenge_listen", Modifier.weight(1f), onClick = actions.listen)
                    ChallengeButton(androidx.compose.material.icons.Icons.Filled.Check, tr("J'ai fini"), "padlive_challenge_finish", Modifier.weight(1f), main = true, onClick = actions.finish)
                }
                is ChallengeUi.Done -> {
                    ChallengeButton(Icons.Filled.Refresh, tr("Rejouer"), "padlive_challenge_again", Modifier.weight(1f), main = !ui.result.won, onClick = actions.again)
                    if (ui.c.level < PadChallenge.LEVELS.size) {
                        ChallengeButton(Icons.AutoMirrored.Filled.ArrowForward, tr("Défi suivant"), "padlive_challenge_next", Modifier.weight(1f), main = ui.result.won, onClick = actions.next)
                    }
                }
                else -> Unit
            }
        }
    }
}

/** What the panel's buttons do. */
class ChallengeActions(
    val listen: () -> Unit = {},
    val yourTurn: () -> Unit = {},
    val finish: () -> Unit = {},
    val again: () -> Unit = {},
    val next: () -> Unit = {},
    val close: () -> Unit = {},
    /** Audit ultra round 1, n° 9: a challenge of the list (its level, 1-based). */
    val pick: (Int) -> Unit = {},
)

/** Every challenge, its number and its best stars; the one shown is outlined. */
@Composable
private fun LevelList(shown: Int, best: List<Int>, pick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("padlive_challenge_levels"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (level in 1..PadChallenge.LEVELS.size) {
            val stars = best.getOrElse(level - 1) { 0 }
            val here = level == shown
            val said = tr("%1\$s · étoiles : %2\$d sur 3", tr("Défi %1\$d · %2\$s", level, levelName(level)), stars)
            Column(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (here) Neon.Amber.copy(alpha = 0.22f) else Neon.SurfaceHigh)
                    .border(1.dp, if (here) Neon.Amber else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { pick(level) }
                    .semantics(mergeDescendants = true) { contentDescription = said }
                    .padding(vertical = 4.dp).testTag("padlive_challenge_level_$level"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("$level", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (here) Neon.Amber else Neon.TextPrimary)
                Row {
                    for (i in 0 until 3) {
                        Icon(if (i < stars) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null,
                            tint = if (i < stars) Neon.Amber else Neon.TextSecondary, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}

/** The score in plain words (said by TalkBack as it comes). */
internal fun resultWords(r: PadChallenge.Result): String {
    val praise = when (r.stars) {
        3 -> tr("Parfait !")
        2 -> tr("Bravo !")
        1 -> tr("Presque !")
        else -> tr("Encore un essai !")
    }
    val pace = when (r.tendency) {
        -1 -> " " + tr("Un peu trop vite.")
        1 -> " " + tr("Un peu trop lent.")
        else -> ""
    }
    return tr("%1\$s %2\$d sur %3\$d.", praise, r.right, r.total) + pace
}

@Composable
private fun Stars(stars: Int) {
    Row(Modifier.semantics { contentDescription = tr("Étoiles : %d sur 3", stars) }.testTag("padlive_challenge_stars")) {
        for (i in 0 until 3) {
            Icon(if (i < stars) Icons.Filled.Star else Icons.Filled.StarBorder, contentDescription = null,
                tint = if (i < stars) Neon.Amber else Neon.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ChallengeButton(icon: ImageVector?, label: String, tag: String, modifier: Modifier, main: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .background(if (main) Neon.Amber else Neon.SurfaceHigh)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 10.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (main) Color.Black else Neon.TextPrimary
        // Audit ultra round 2, zone 1, n° 3: each button has its picture (ear, hand, tick, arrow), big enough to read without words.
        icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp).padding(end = 4.dp)) }
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
