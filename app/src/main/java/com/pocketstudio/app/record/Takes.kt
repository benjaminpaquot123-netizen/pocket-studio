package com.pocketstudio.app.record

import com.pocketstudio.app.ui.partName
import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.partSteps
import com.pocketstudio.model.sceneSteps
import com.pocketstudio.model.songStepOf

/** Where « ● Enregistrer » was touched: what the take writes. */
enum class TakeSource {
    /** The keyboard (Clavier, Notes): notes on the track chosen. */
    KEYS,
    /** The drum pads (Rythme): hits on the nearest sixteenth. */
    PADS,
    /** The 303 (Acide): its knobs' moves, as curves. */
    ACID,
    /** « Garder ce que je viens de jouer »: the last bars played, kept afterwards. */
    CAPTURE,
}

enum class TakePhase { COUNT_IN, RECORDING, DONE }

/**
 * A take from an editor, as the record bar shows it: counting in ([beatsLeft]), recording, then
 * done with what it wrote ([notes], [hits], [moves]: knob moves) and how it was put in time
 * ([strength]: null as played, else 0.5..1 of the way onto the sixteenths).
 */
data class NoteTakeUi(
    val source: TakeSource,
    val phase: TakePhase,
    val beatsLeft: Int = 0,
    val notes: Int = 0,
    val hits: Int = 0,
    val moves: Boolean = false,
    val strength: Float? = null,
    /** « Remettre en rythme » was touched once: its strengths and « Comme joué » stay under it. */
    val tuned: Boolean = false,
) {
    val running: Boolean get() = phase != TakePhase.DONE

    /** « Remettre en rythme » has something to move (notes; hits are on the sixteenths already). */
    val canPutInTime: Boolean get() = notes > 0

    /** What it wrote, said in a few words. */
    fun summary(): String = when {
        notes > 0 && hits > 0 -> tr("Ta prise : %1\$d notes et %2\$d coups", notes, hits)
        notes > 1 -> tr("Ta prise : %d notes", notes)
        notes == 1 -> tr("Ta prise : 1 note")
        hits > 1 -> tr("Ta prise : %d coups", hits)
        hits == 1 -> tr("Ta prise : 1 coup")
        moves -> tr("Ta prise : les mouvements des boutons")
        else -> tr("Rien n'a été enregistré")
    }
}

/** Strengths offered by « Remettre en rythme » (pros: a little, most, all the way). */
val IN_TIME_STRENGTHS = listOf(0.5f, 0.75f, 1f)

/**
 * The take's counter: the part and the bar inside it, « Couplet · mes. 5/16 ». Playing the song
 * ([songIndex] ≥ 0): part [songIndex], [bar] being the song's (from 0); playing a part alone: the
 * shown part's loop, [bar] its own. Null before the first bar is known.
 */
fun takeCounter(p: Project, songIndex: Int, bar: Int): String? {
    if (bar < 0) return null
    val spb = p.stepsPerBar.coerceAtLeast(1)
    val part = p.song.getOrNull(songIndex)
    return if (part != null) {
        val first = p.songStepOf(songIndex) / spb
        val bars = ((p.partSteps(part) + spb - 1) / spb).coerceAtLeast(1)
        tr("%1\$s · mes. %2\$d/%3\$d", partName(p, part.scene), (bar - first).coerceIn(0, bars - 1) + 1, bars)
    } else {
        val bars = ((p.sceneSteps(p.scene) + spb - 1) / spb).coerceAtLeast(1)
        tr("%1\$s · mes. %2\$d/%3\$d", partName(p, p.scene), bar % bars + 1, bars)
    }
}
