package com.pocketstudio.bentest

/** The app went to the background while the robot worked: noted, brought back, the step's timing marked as skewed. */
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

    fun arret(ecranAllume: Boolean = true, rotation: Boolean = false) = Unit

    fun retour() = Unit
}
