package com.pocketstudio.app.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.model.DefaultNames

/**
 * Right under the top bar, on every tab, from the open project: the rename bar a long press on the
 * project's name asks for ([renaming]), the play strip (tempo, position, « ⟳ Partie ») and, while
 * a track is soloed, the solo banner (landscape, where every strip costs a share of a short
 * screen: a « Solo » chip in the play strip instead); and the tempo sheet the tempo opens
 * ([tempoOpen]). [rename]: what renaming does (the app also saves at once and refreshes « Mes projets »).
 */
@Composable
fun UnderTopBar(
    vm: RhythmViewModel,
    tempoOpen: Boolean,
    setTempoOpen: (Boolean) -> Unit,
    renaming: Boolean,
    closeRename: () -> Unit,
    rename: (String) -> Unit = vm::renameProject,
    /** False on the Live page and the timeline: the tempo and the position only (their ▶ has its own meaning there). */
    controls: Boolean = true,
    /** Phase 36: the « ? » that labels the screen (null: none), and what it does. */
    helpOn: Boolean? = null,
    toggleHelp: () -> Unit = {},
    /** A long press on the « ? »: « Signaler un bug » (null: no shortcut). */
    reportBug: (() -> Unit)? = null,
) {
    val project by vm.project.collectAsStateWithLifecycle()
    val transport by vm.transport.collectAsStateWithLifecycle()
    val sessionUi by vm.sessionUi.collectAsStateWithLifecycle()
    val loopPart by vm.loopPart.collectAsStateWithLifecycle()
    val playFrom by vm.playFrom.collectAsStateWithLifecycle()
    // Changes once a bar at most, and is read only by the position's text.
    val bar = vm.playBar.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(transport.playing) {
        if (transport.playing) lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.pollPosition() }
    }
    val playing = transport.playing
    val songIndex = sessionUi.shownSongIndex
    val position = remember(project, playing, songIndex, playFrom, loopPart, bar) {
        { playPosition(project, playing, songIndex, bar.value, playFrom, loopPart) }
    }
    val parts = project.song.map { partName(project, it.scene) }
    val soloed = soloedTracks(project)
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column {
        if (renaming) {
            RenameProjectBar(
                DefaultNames.shown(project.name),
                onDone = { name ->
                    closeRename()
                    if (name.isNotBlank() && name.trim() != DefaultNames.shown(project.name)) rename(name)
                },
                onDismiss = closeRename,
            )
        }
        PlayStrip(
            PlayStripUi(
                project.tempoBpm, hasSong = controls && project.song.isNotEmpty(), loopPart = loopPart, playing = playing, position = position, parts = parts,
                soloed = if (landscape || !controls) soloed else emptyList(), helpOn = helpOn,
            ),
            PlayStripActions(openTempo = { setTempoOpen(true) }, setLoopPart = vm::setLoopPart, playFrom = vm::playSongPart, clearSolos = vm::clearSolos, toggleHelp = toggleHelp, reportBug = reportBug),
        )
        if (!landscape && controls) SoloBanner(soloed, vm::clearSolos)
    }
    if (tempoOpen) {
        // Whatever the sheet changes until it closes is one undo step.
        LaunchedEffect(Unit) { vm.beginTempoSheet() }
        TempoSheet(project.tempoBpm, TempoActions(set = vm::setSheetTempo, tap = vm::tapSheetTempo), onClose = { setTempoOpen(false) })
    }
}

/**
 * The strip on the Live page and the timeline, under their own bar: the tempo (a touch opens its
 * sheet) and where the music is (« C · Drop · mes. 9/60 », or the scene looping): no « ⟳ Partie »
 * nor « Jouer depuis… » there, since ▶ on those pages has its own meaning (the Live page plays the
 * scene shown; the timeline the song).
 */
@Composable
fun PositionStrip(vm: RhythmViewModel, tempoOpen: Boolean, setTempoOpen: (Boolean) -> Unit) =
    UnderTopBar(vm, tempoOpen, setTempoOpen, renaming = false, closeRename = {}, controls = false)
