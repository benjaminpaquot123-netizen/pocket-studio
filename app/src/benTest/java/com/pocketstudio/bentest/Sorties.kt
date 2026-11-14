package com.pocketstudio.bentest

/**
 * The app went to the background while the robot worked (an external app opened by something the robot
 * did not foresee): noted, brought back to the front, the step's timing marked as skewed (« faussé »).
 */
class Sorties(
    private val carnet: Carnet,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ramener: () -> Unit,
) {
    /** The step running (null: no run). */
    @Volatile
    var etape: String? = null

    /** The robot itself put the app in the background (a question asked). */
    @Volatile
    var voulue = false

    private class Sortie(val etape: String, val depuis: Long)

    @Volatile
    private var sortie: Sortie? = null

    /** The studio's ON_STOP. Nothing for what is expected: a turn of the phone, the screen off, a question, the screen-off step. */
    fun arret(ecranAllume: Boolean = true, rotation: Boolean = false) {
        val step = etape ?: return
        if (rotation || !ecranAllume || voulue || step == "ecran_eteint" || sortie != null) return
        sortie = Sortie(step, clock())
        runCatching { ramener() }
    }

    /** The studio is in front again. */
    fun retour() {
        val s = sortie ?: return
        sortie = null
        val secondes = (clock() - s.depuis) / 1000
        carnet.add(Carnet.NOTE, s.etape, "sortie de l'appli à l'étape ${s.etape}, revenue après $secondes s")
        carnet.add(Carnet.FAUSSE, s.etape, "sortie de l'appli ($secondes s)")
    }
}
