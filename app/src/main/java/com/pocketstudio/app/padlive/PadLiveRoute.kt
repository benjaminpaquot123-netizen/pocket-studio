package com.pocketstudio.app.padlive

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pocketstudio.app.audio.Pcm
import com.pocketstudio.app.live.HideSystemBars
import com.pocketstudio.app.live.RecordButton
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.app.ui.LiveStateHost
import com.pocketstudio.app.ui.TempoActions
import com.pocketstudio.app.ui.TempoSheet
import com.pocketstudio.app.ui.liveActionsOf
import com.pocketstudio.model.AutoTarget

/**
 * Pad Live as the app opens it (home screen card → the « Live » project): the Live page's state
 * (pads' lights, the take, the effects) and every gesture going to the ViewModel. [welcome]: opened
 * from the card just now, the first hit sounds as soon as it is in.
 */
@Composable
fun PadLiveRoute(
    vm: RhythmViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    close: () -> Unit,
    /** The sound browser for pad (any page). */
    browse: (Int) -> Unit,
    openLive: () -> Unit,
    openEffects: (Int) -> Unit,
    /** The ready-to-play set (« Fête ») as the sound library makes it. */
    fete: suspend (Double) -> Map<Int, Pair<Pcm, Int>>,
    welcome: Boolean,
    welcomed: () -> Unit,
) = LiveStateHost(vm, lifecycleOwner, liveEditing = null) { live, _ ->
    HideSystemBars(true)
    androidx.activity.compose.BackHandler(onBack = close)
    // An empty « Live » project: the ready-to-play set on page A (once, as the Live page does).
    LaunchedEffect(Unit) { vm.fillLiveStarter(fete) }
    LaunchedEffect(welcome) {
        if (welcome) {
            vm.padLive.welcome()
            welcomed()
        }
    }
    val busy by vm.padLive.busy.collectAsStateWithLifecycle()
    val starting by vm.liveStarting.collectAsStateWithLifecycle()
    val states by vm.liveStates.collectAsStateWithLifecycle()
    val canUndo by vm.session.canUndo.collectAsStateWithLifecycle()
    val played by vm.padLive.played.ui.collectAsStateWithLifecycle()
    val message by vm.padLive.played.message.collectAsStateWithLifecycle()
    // Lot B2: a controller's pads play Pad Live's pads while it shows.
    androidx.compose.runtime.DisposableEffect(Unit) {
        vm.padLive.shown = true
        onDispose { vm.padLive.shown = false }
    }
    val actions = liveActionsOf(vm, back = close, openEffects = openEffects)
    var tempoOpen by rememberSaveable { mutableStateOf(false) }
    val p = live.project
    val ui = PadLiveUi(
        pads = p.live, states = states, page = p.padLive.shownPage,
        pack = p.padLive.pack.ifEmpty { if (p.live.starterOffered) LiveStarterPack else "" },
        playing = live.playing, tempoBpm = p.tempoBpm, beatsPerBar = p.beatsPerBar,
        busy = busy ?: if (starting) com.pocketstudio.model.i18n.tr("Préparation des pads prêts à jouer…") else null,
        effectHeld = live.fxOf().values.isNotEmpty(),
        canUndo = canUndo,
        chop = p.liveFx.chop,
        riserBars = p.liveFx.riserBars,
        played = played,
        emptyPad = if (played is PlayedTakeUi.Done) vm.padLive.played.emptyPad()?.let { padName(it) } else null,
        message = message,
    )
    Box(
        Modifier.fillMaxSize()
            // Nothing under this page takes a touch meant for it.
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
    ) {
        PadLiveScreen(
            ui,
            PadLiveActions(
                back = close,
                togglePlay = vm::togglePlay,
                nudgeTempo = vm::nudgeTempo,
                openTempo = { tempoOpen = true },
                touch = vm.padLive::touch,
                setPage = vm.padLive::setPage,
                choosePack = { vm.padLive.loadPack(it, fete) },
                browse = browse,
                setMode = vm.padLive::setMode,
                setGroup = vm.padLive::setGroup,
                setBeats = vm::setLiveBeats,
                setGain = vm::setLiveGain,
                clear = vm::clearLive,
                stopAll = vm::stopLive,
                filter = { x -> vm.livePerformance(AutoTarget.LIVE_FILTER, x ?: AutoTarget.liveRest(AutoTarget.LIVE_FILTER)) },
                echo = { on -> vm.liveHold(AutoTarget.LIVE_ECHO_OUT, on) },
                resonance = { r -> vm.livePerformance(AutoTarget.LIVE_RESONANCE, r ?: AutoTarget.liveRest(AutoTarget.LIVE_RESONANCE)) },
                roll = vm::liveRoll,
                riser = { vm.liveRiser() },
                brake = { on -> vm.liveHold(AutoTarget.LIVE_TAPE_STOP, on) },
                stutter = { on -> vm.liveHold(AutoTarget.LIVE_CHOP, on) },
                undo = vm::undo,
                openLive = openLive,
                effects = { openEffects(EFFECTS_CHANNEL) },
                launchRow = { row -> vm.padLive.launchRow(p.padLive.shownPage, row) },
                setPitch = { pad, v -> vm.padLive.setSound(pad, pitch = v) },
                setReverse = { pad, v -> vm.padLive.setSound(pad, reverse = v) },
                setAttack = { pad, v -> vm.padLive.setSound(pad, attack = v) },
                setRelease = { pad, v -> vm.padLive.setSound(pad, release = v) },
                played = vm.padLive.played::press,
                placeOnPad = { vm.padLive.played.placeOnPad() },
                placeInBrick = { vm.padLive.played.placeInBrick() },
                dropTake = vm.padLive.played::cancel,
                messageShown = vm.padLive.played::messageShown,
            ),
            clockOf = live.clockOf,
            record = { RecordButton(live.take, actions.record) },
        )
    }
    if (tempoOpen) {
        LaunchedEffect(Unit) { vm.beginTempoSheet() }
        TempoSheet(p.tempoBpm, TempoActions(set = vm::setSheetTempo, tap = vm::tapSheetTempo, lead = p.tempoLead, backToLead = vm::backToLeadTempo), onClose = { tempoOpen = false })
    }
}

/** « A12 »: pad [pad]'s page and place. */
internal fun padName(pad: Int): String =
    com.pocketstudio.model.PadLive.PAGE_NAMES[com.pocketstudio.model.PadLive.pageOf(pad)] + (pad % com.pocketstudio.model.PadLive.PAGE_SIZE + 1)

/** The live pads' strip (shared with the remix): the pads' « Effets ». */
private const val EFFECTS_CHANNEL = 3

/** A « Live » project's page A holding the ready-to-play set, no pack chosen yet: its pack is « Fête ». */
private val LiveStarterPack: String get() = com.pocketstudio.model.PadLive.DEFAULT_PACK.id
