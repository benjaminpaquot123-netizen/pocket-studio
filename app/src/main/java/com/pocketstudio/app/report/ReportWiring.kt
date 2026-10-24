package com.pocketstudio.app.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.pocketstudio.app.rhythm.RhythmViewModel
import com.pocketstudio.engine.NativeEngine
import com.pocketstudio.model.AutoTarget
import com.pocketstudio.model.AutomationLane
import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.Project
import com.pocketstudio.model.ProjectJson
import com.pocketstudio.model.hasPoints
import com.pocketstudio.model.songBars
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** The master volume's song lane ([AutoTarget.masterVolume]), if the project has one. */
fun masterLane(p: Project): AutomationLane? =
    p.automation.firstOrNull { it.target == AutoTarget.masterVolume() && it.mode == AutomationLane.SONG && it.hasPoints }

/**
 * The value the master volume's song lane sets at transport step [songStep] (as the engine is sent
 * it: over the song, at least [minBars] bars), null when there is no lane or the song is stopped.
 */
fun masterLaneValue(p: Project, songStep: Int, minBars: Int = 8): Float? {
    val lane = masterLane(p) ?: return null
    if (songStep < 0) return null
    val points = maxOf(minBars, p.songBars) * lane.pointsPerBar(p.beatsPerBar)
    return lane.songValueAtStep(songStep, points, p.stepsPerBar)
}

/** The open project as the report says it. */
fun reportProject(vm: RhythmViewModel): ReportProject {
    val p = vm.project.value
    val playing = vm.transport.value.playing
    val pos = com.pocketstudio.app.ui.playPosition(p, playing, vm.sessionUi.value.shownSongIndex, vm.playBar.value, vm.playFrom.value, vm.loopPart.value)
    return ReportProject(
        ProjectSummary(
            name = DefaultNames.shown(p.name),
            style = p.style,
            tempoBpm = p.tempoBpm,
            position = pos?.let { "${it.name} · mes. ${it.bar}/${it.bars}" + if (it.loop) " (en boucle)" else "" },
            playing = playing,
            parts = p.song.size,
            songBars = p.songBars,
            tracks = p.tracks.size,
            masterDb = p.mixer.masterDb,
            target = p.mixer.target.name,
            masterLane = masterLane(p) != null,
        ),
        json = { ProjectJson.encode(p) },
    )
}

/**
 * Feeds the bug report's journal from the open project: the transport (play, stop), the takes,
 * the exports, the intentions done (the part sheet, a kit…); and lets the report and the health
 * readings see the project. [screen] names what is on screen (a space, a page, a sheet), read in
 * a snapshot: each change is one line of the journal.
 */
@Composable
fun JournalWatch(vm: RhythmViewModel, report: BugReportViewModel, screen: () -> String) {
    DisposableEffect(vm, report) {
        report.project = { reportProject(vm) }
        HealthMonitor.probe = {
            val p = vm.project.value
            val playing = vm.transport.value.playing
            HealthProbe(playing, if (playing && NativeEngine.isAvailable) masterLaneValue(p, NativeEngine.songStep()) else null)
        }
        onDispose {
            report.project = null
            HealthMonitor.probe = null
        }
    }
    // The newest lambda (it closes over plain values too, such as the mode), its state reads followed.
    val describe by rememberUpdatedState(screen)
    LaunchedEffect(Unit) { snapshotFlow { describe() }.collect { Journal.screen(it) } }
    LaunchedEffect(vm) {
        launch {
            vm.transport.map { it.playing }.distinctUntilChanged().drop(1).collect { playing ->
                val p = vm.project.value
                Journal.action(if (playing) "▶ Lecture (tempo ${p.tempoBpm.toInt()}, ${if (vm.loopPart.value || p.song.isEmpty()) "partie en boucle" else "morceau"})" else "■ Arrêt")
            }
        }
        launch { vm.recording.drop(1).collect { Journal.action(if (it) "● Enregistrement armé" else "Enregistrement fini") } }
        launch { vm.audioRecording.map { it != null }.distinctUntilChanged().drop(1).collect { Journal.action(if (it) "● Prise micro" else "Prise micro finie") } }
        launch { vm.noteTake.map { it != null }.distinctUntilChanged().drop(1).collect { Journal.action(if (it) "● Prise jouée" else "Prise jouée finie") } }
        launch { vm.done.collect { text -> if (text != null) Journal.action("Fait : $text") } }
        launch {
            vm.export.map { Triple(it.running, it.error, it.files.size) }.distinctUntilChanged().drop(1).collect { (running, error, files) ->
                val e = vm.export.value
                when {
                    running -> Journal.action("Export lancé : ${e.what.name}, ${e.format.name}")
                    error != null -> Journal.error("export", error)
                    files > 0 -> Journal.action("Export fini : $files fichier(s)" + (e.loudness?.let { ", ${String.format(java.util.Locale.ROOT, "%.1f", it)} LUFS" } ?: ""))
                }
            }
        }
    }
}
