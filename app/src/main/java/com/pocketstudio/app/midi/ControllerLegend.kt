package com.pocketstudio.app.midi

import com.pocketstudio.model.Project
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.midi.ControlButton
import com.pocketstudio.model.midi.ControllerProfile

/** One line of the legend: a control of the controller and what it does on the screen. */
data class LegendRow(val control: String, val does: String, val tag: String)

/**
 * What the controller does on each screen, in words (the small legend on screen, and the list in
 * « Contrôleur MIDI »): keys, pads (both banks), knobs, faders, the main encoder and its click, the
 * transport buttons when the controller has them. [faders]: what its faders move (empty: it has none).
 */
object ControllerLegend {
    fun rows(screen: KnobScreen, knobs: List<KnobSlot>, faders: List<KnobSlot>, profile: ControllerProfile?, p: Project, learnt: Set<ControlButton> = emptySet()): List<LegendRow> {
        val rows = ArrayList<LegendRow>()
        rows += LegendRow(tr("Clavier"), tr("Joue la piste choisie, avec la force de frappe ; bandes ou molettes : hauteur et modulation ; pédale : tenue"), "keys")
        val columns = com.pocketstudio.app.live.liveColumnsOf(p).take(8).joinToString(" · ") { it.name }
        val (a, b) = when (screen) {
            KnobScreen.Live -> tr("Lancent les lignes A à H, calées comme sur la grille") to tr("Coupent ou rallument : %s", columns)
            KnobScreen.LiveAcide -> tr("Lancent la ligne 303 de A à H") to tr("Coupent ou rallument : %s", columns)
            KnobScreen.LivePads -> tr("Jouent les pads live 1 à 8") to tr("Jouent les pads live 9 à 16")
            else -> tr("Jouent les pads de batterie 1 à 8") to tr("Jouent les pads de batterie 9 à 16")
        }
        rows += LegendRow(tr("Pads 1 à 8"), a, "pads_a")
        rows += LegendRow(tr("Pads 9 à 16 (banque B)"), b, "pads_b")
        rows += LegendRow(tr("Potards 1 à 8"), knobs.joinToString(" · ") { it.label }, "knobs")
        if (faders.isNotEmpty()) {
            rows += LegendRow(tr("Faders"), tr("Volumes : %s", faders.joinToString(" · ") { it.label }), "faders")
        }
        val buttons = profile?.buttons.orEmpty().keys + learnt
        if (ControlButton.ENCODER in buttons) {
            rows += LegendRow(
                tr("Gros bouton"),
                if (screen.isLive) tr("Tempo") else tr("Change de piste (avec Shift : tempo)"),
                "encoder",
            )
        }
        if (ControlButton.CLICK in buttons) rows += LegendRow(tr("Clic du gros bouton"), tr("Lecture / arrêt (avec Shift : arrêt)"), "click")
        if (ControlButton.PLAY in buttons) {
            rows += LegendRow(
                tr("Lecture, Arrêt, Enregistrer"),
                if (screen.isLive) tr("Lecture / arrêt ; Enregistrer lance la prise du Live") else tr("Lecture / arrêt"),
                "transport",
            )
        }
        return rows
    }
}
