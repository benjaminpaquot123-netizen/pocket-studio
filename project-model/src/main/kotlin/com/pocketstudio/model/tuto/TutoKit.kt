package com.pocketstudio.model.tuto

import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.Project

/**
 * The style's drum kit for the tutorial (owner: every tutorial starts on its style's box of sounds):
 * the packs its drum lines come from, as the assistant takes them (each line's machine, such as the
 * TR-909, else the style's own pack: `soundCategories` of the app), and the sound each pad takes
 * once they are on the phone. Without them, the built-in kit ([TutoTarget.blank]). The examples
 * are played with the user's drum sounds ([TutoChecks.withKitOf]): the same sounds both ways.
 */
object TutoKit {
    /** The packs of the catalogue [index] that [target]'s drum lines are taken from (none: the built-in kit is the style's). */
    fun packs(target: TutoTarget, index: List<PackEntry>): List<PackEntry> {
        val style = target.style
        val ids = (style.lines.filter { it.voice == null }.mapNotNull { style.kit[it.role] } + style.id).distinct()
        // An instrument (it has a preview to hear) is no drum kit.
        return ids.mapNotNull { id -> index.firstOrNull { it.id == id } }.filter { it.preview.isEmpty() }
    }

    /** Those of [packs] that are not on the phone yet ([installed]). */
    fun missing(target: TutoTarget, index: List<PackEntry>, installed: List<PackManifest>): List<PackEntry> =
        packs(target, index).filter { e -> installed.none { it.id == e.id } }

    /**
     * The pack sound each pad takes from [installed] (pad → pack id, sound), as the assistant
     * chooses it: the line's machine when it is there and has that kind of sound, else the style's
     * own pack; a line that asks for a kit voice (the afro house shaker) keeps it. Pads not listed
     * keep the built-in kit's sound.
     */
    fun sounds(target: TutoTarget, installed: List<PackManifest>): Map<Int, Pair<String, PackSound>> {
        val style = target.style
        val out = LinkedHashMap<Int, Pair<String, PackSound>>()
        for (line in style.lines) {
            if (line.voice != null) continue
            val kind = line.role.packKind
            val from = listOfNotNull(style.kit[line.role], style.id).distinct()
                .firstNotNullOfOrNull { id -> installed.firstOrNull { it.id == id }?.takeIf { m -> m.ofKind(kind).isNotEmpty() } } ?: continue
            out[line.role.lane] = from.id to from.ofKind(kind).first()
        }
        return out
    }

    /** What the tutorial does about its kit right now ([offer]). */
    sealed interface Offer {
        /** Nothing: decided already, offline, or the catalogue still coming. */
        data object Wait : Offer

        /** The style has no pack: the built-in kit is its own, decided at once. */
        data object BuiltIn : Offer

        /** « Télécharger le kit » / « Plus tard »: [missing] are not on the phone yet. */
        data class Download(val missing: List<PackEntry>) : Offer

        /** The pack is here and the course has not begun (or the user asked for it): its sounds go on the pads. */
        data object Apply : Offer

        /**
         * The pack is here but the course is under way (a tutorial begun with an earlier version,
         * or offline): its sounds would replace the pads' own, so « Mettre ces sons » / « Plus tard ».
         */
        data object Ask : Offer
    }

    /**
     * Review (2026-10-29, MOYEN 1): the pack's sounds go on the pads by themselves only on a course
     * not begun ([fresh]) or when the user asked for them ([asked]: « Télécharger » or « Mettre ces
     * sons »); otherwise they are offered, never put in silence over the user's sounds.
     * [index] null: no catalogue (offline), nothing to do.
     */
    fun offer(target: TutoTarget, progress: TutoProgress, project: Project, index: List<PackEntry>?, installed: List<PackManifest>, asked: Boolean): Offer {
        if (progress.kit.isNotEmpty() || index == null) return Offer.Wait
        if (packs(target, index).isEmpty()) return Offer.BuiltIn
        val missing = missing(target, index, installed)
        if (missing.isNotEmpty()) return Offer.Download(missing)
        if (sounds(target, installed).isEmpty()) return Offer.BuiltIn
        return if (asked || fresh(progress, project)) Offer.Apply else Offer.Ask
    }

    /** A course not begun: no step done, and no sound of the user's on a pad. */
    fun fresh(progress: TutoProgress, project: Project): Boolean =
        progress.done.isEmpty() && project.drums.lanes.none { it.sample != null }
}
