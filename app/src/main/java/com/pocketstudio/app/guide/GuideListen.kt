package com.pocketstudio.app.guide

import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.audio.PcmPlayer
import com.pocketstudio.app.learn.LearnViewModel
import com.pocketstudio.engine.OfflineExport
import com.pocketstudio.model.Project
import com.pocketstudio.model.guide.GuideActions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What the Guide is playing (« ▶ » of an option): [preparing] while it renders, then [take] 0 for
 * « Sans » and 1 for « Avec » as the sound goes; [failed]: it could not be heard here.
 */
data class GuideListenUi(val card: String, val option: String, val preparing: Boolean = true, val take: Int = -1, val failed: Boolean = false)

/** Plays what [GuideListen] rendered; [PcmPlayer] in the app, a recorder in the tests. */
interface GuideListenPlayer {
    /** Starts [pcm]; false when it cannot. [ended] is called once it has played to its end (or was stopped). */
    fun play(pcm: Pcm, ended: () -> Unit): Boolean
    fun stop()
}

/** The app's player: the library's previews' [PcmPlayer]. */
class PcmListenPlayer : GuideListenPlayer {
    private var ended: (() -> Unit)? = null
    private val player = PcmPlayer { ended?.invoke() }

    override fun play(pcm: Pcm, ended: () -> Unit): Boolean {
        this.ended = ended
        return player.play(pcm)
    }

    override fun stop() {
        ended = null
        player.stop()
    }
}

/**
 * « Écouter avant d'appliquer » (docs/PLAN-GUIDE.md §4.3): an option of a card heard without,
 * then with, the change it would make, at the same loudness, in the part it is about, before
 * anything changes in the song. The change is worked out as « Appliquer » would make it
 * ([GuideActions.tryApply]) on a copy of the project; [render] makes the two takes (the app: a
 * private engine, [com.pocketstudio.app.rhythm.RhythmViewModel.renderGuideAB]); [player] plays them
 * one after the other. The last one is kept: a second ▶ is immediate. [pauseMusic]: the song
 * playing stops first (two sounds at once would say nothing).
 */
class GuideListen(
    private val scope: CoroutineScope,
    private val project: () -> Project,
    private val render: suspend (with: Project, without: Project) -> Pair<FloatArray, FloatArray>?,
    private val player: GuideListenPlayer,
    private val pauseMusic: () -> Unit = {},
    private val worker: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow<GuideListenUi?>(null)
    val state: StateFlow<GuideListenUi?> = _state.asStateFlow()

    private var job: Job? = null
    private var follow: Job? = null

    /** The last takes rendered: for that card, option and project (as it was then). */
    private var cached: Triple<String, Project, Rendered>? = null

    private class Rendered(val pcm: Pcm, val starts: List<Int>)

    /** How many renders were asked for (tests). */
    @Volatile var renders = 0
        private set

    /** ▶ of option [option] of card [card]: heard without then with it; touched again while it plays, it stops. */
    fun toggle(card: String, option: String) {
        val now = _state.value
        if (now != null && now.card == card && now.option == option && !now.failed) {
            stop()
            return
        }
        stop()
        pauseMusic()
        _state.value = GuideListenUi(card, option)
        val key = "$card/$option"
        job = scope.launch {
            val p = project()
            val hit = cached?.takeIf { it.first == key && it.second === p }?.third
            val rendered = hit ?: run {
                val with = withContext(worker) { GuideActions.tryApply(p, card, option, clock()).project }
                // Nothing would change (it opens a screen, it keeps): nothing to compare.
                if (with.copy(guide = p.guide) == p) {
                    _state.value = null
                    return@launch
                }
                renders++
                val ab = render(with, p)
                if (ab == null) {
                    _state.value = GuideListenUi(card, option, preparing = false, failed = true)
                    return@launch
                }
                // « Sans » first, then « Avec »: the ear hears what the change brings.
                val (data, starts) = LearnViewModel.join(listOf(ab.second, ab.first), GAP_FRAMES)
                Rendered(Pcm(data, 2, OfflineExport.SAMPLE_RATE), starts).also { cached = Triple(key, p, it) }
            }
            if (_state.value?.let { it.card == card && it.option == option } != true) return@launch
            val started = player.play(rendered.pcm) { scope.launch { if (_state.value?.let { it.card == card && it.option == option } == true) clear() } }
            if (!started) {
                _state.value = GuideListenUi(card, option, preparing = false, failed = true)
                return@launch
            }
            _state.value = GuideListenUi(card, option, preparing = false, take = 0)
            follow = scope.launch {
                val t0 = clock()
                rendered.starts.forEachIndexed { i, frame ->
                    val wait = frame * 1000L / OfflineExport.SAMPLE_RATE - (clock() - t0)
                    if (wait > 0) delay(wait)
                    val s = _state.value ?: return@launch
                    if (s.card != card || s.option != option) return@launch
                    _state.value = s.copy(take = i)
                }
            }
        }
    }

    /** Stops what plays (the panel folded, another option heard). */
    fun stop() {
        job?.cancel()
        job = null
        player.stop()
        clear()
    }

    private fun clear() {
        follow?.cancel()
        follow = null
        _state.value = null
    }

    companion object {
        /** Silence between « Sans » and « Avec ». */
        const val GAP_FRAMES = OfflineExport.SAMPLE_RATE / 3
    }
}
