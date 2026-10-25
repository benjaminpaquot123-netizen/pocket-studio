package com.pocketstudio.model.guide

import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Layers
import com.pocketstudio.model.TrackLayer
import com.pocketstudio.model.i18n.tr

/**
 * A line of a track's history (the sheet's « Historique », docs/PLAN-GUIDE.md §3.3): one group of
 * changes ([GuideChange.id]: what one option or one gesture changed), what it was in a few words
 * ([text], in the interface's language), when, by whom ([byGuide]: a card, [card] its title, else
 * by hand), and whether it was brought back since ([reverted]). « Ramener » takes [id].
 */
data class GuideHistoryLine(
    val id: String,
    val text: String,
    val at: Long,
    val byGuide: Boolean,
    val card: String?,
    val reverted: Boolean,
)

object GuideHistory {
    /** [changes] (a track's, newest first: [GuideState.historyOf]) as lines, one per group, newest first. [cardTitle]: a card's title, by id. */
    fun lines(changes: List<GuideChange>, cardTitle: (String) -> String? = { null }): List<GuideHistoryLine> =
        changes.groupBy { it.id.ifEmpty { "${it.at}:${it.field}" } }.map { (id, group) ->
            val first = group.first()
            // In the order they were made (the track's history comes newest first).
            val texts = group.asReversed().map(::describe).distinct()
            val text = texts.take(2).joinToString(" · ") + if (texts.size > 2) " …" else ""
            GuideHistoryLine(
                id = first.id, text = text, at = group.maxOf { it.at }, byGuide = first.origin == GuideChange.GUIDE,
                card = first.card.takeIf { first.origin == GuideChange.GUIDE && it.isNotEmpty() }?.let(cardTitle),
                reverted = group.all { it.reverted },
            )
        }

    /** What change [c] was, in a few words. */
    fun describe(c: GuideChange): String = when {
        c.field == GuideActions.LAYERS -> layers(GuideActions.layersOf(c.before), GuideActions.layersOf(c.after))
        c.field.startsWith(GuideActions.SLOT) -> effect(GuideActions.insertOf(c.before)?.effect, GuideActions.insertOf(c.after)?.effect)
        c.field.startsWith(GuideActions.SEND) -> if (c.field == GuideActions.SEND + "1") tr("Envoi vers l'écho") else tr("Envoi vers la réverbe")
        c.field == GuideActions.PAN || c.field == GuideActions.PAD_PAN -> tr("Place dans la stéréo")
        c.field == GuideActions.VOLUME -> tr("Volume")
        c.field == GuideActions.SOLO -> tr("Solo")
        c.field == GuideActions.TARGET -> tr("Cible de volume")
        c.field == GuideActions.DUCKING -> tr("La musique s'écarte du kick")
        c.field.startsWith(GuideActions.PARAM) -> tr("Réglage d'un retour d'effet")
        c.field.startsWith(GuideActions.MUTED) -> tr("Joue ou se tait dans une partie")
        c.kind == GuideChange.LANE -> tr("Une courbe")
        c.kind == GuideChange.SOUND -> tr("Son changé")
        else -> tr("Réglage changé")
    }

    private fun layers(before: List<TrackLayer>, after: List<TrackLayer>): String {
        val firstDiff = (0 until maxOf(before.size, after.size)).firstOrNull { before.getOrNull(it) != after.getOrNull(it) } ?: 0
        return when {
            after.size > before.size -> tr("Couche ajoutée : %s", Layers.shownTitle(after.getOrNull(firstDiff) ?: after.last()))
            after.size < before.size -> tr("Couche retirée : %s", Layers.shownTitle(before.getOrNull(firstDiff) ?: before.last()))
            after.isEmpty() -> tr("Couches")
            else -> {
                val b = before[firstDiff]
                val a = after[firstDiff]
                if (a.sound != b.sound) tr("Son de la couche « %s » changé", Layers.shownTitle(a)) else tr("Couche réglée : %s", Layers.shownTitle(a))
            }
        }
    }

    private fun effect(before: EffectType?, after: EffectType?): String {
        val was = before?.takeIf { it != EffectType.NONE }
        val now = after?.takeIf { it != EffectType.NONE }
        return when {
            was == null && now != null -> tr("Effet ajouté : %s", effectName(now))
            was != null && now == null -> tr("Effet retiré : %s", effectName(was))
            was != null && now != null && was != now -> tr("Effet changé : %1\$s → %2\$s", effectName(was), effectName(now))
            now != null -> tr("Effet réglé : %s", effectName(now))
            else -> tr("Effet")
        }
    }

    /** An effect's name in the interface's language. */
    private fun effectName(effect: EffectType): String = tr(effect.label)
}
