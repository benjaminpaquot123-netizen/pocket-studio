package com.pocketstudio.app.bench

import com.pocketstudio.model.DefaultNames
import com.pocketstudio.model.InstrumentTrack
import com.pocketstudio.model.Project
import com.pocketstudio.model.StarterWizard
import com.pocketstudio.model.instrumentTracks
import com.pocketstudio.model.leadTrackId
import com.pocketstudio.model.padTrackIds

/**
 * The mixer channels the benches render alone (`ps_render --solo`, `ps_live` with the channel
 * soloed): the lead (S4), and since the leads chantier the chords or pad and the counter-melody,
 * so that each melodic voice's timbre is measured on its own. ps_render and ps_live skip META lines.
 */
internal fun benchVoiceMeta(p: Project): List<String> {
    val counter = p.instrumentTracks.firstOrNull { (DefaultNames.french(it.name) ?: it.name) == StarterWizard.COUNTER_TRACK }?.id
    return listOfNotNull(
        p.leadTrackId()?.let { "META lead ${InstrumentTrack.mixerChannel(it)}" },
        p.padTrackIds().firstOrNull()?.let { "META pad ${InstrumentTrack.mixerChannel(it)}" },
        counter?.let { "META counter ${InstrumentTrack.mixerChannel(it)}" },
    )
}
