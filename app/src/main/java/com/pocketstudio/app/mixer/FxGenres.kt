package com.pocketstudio.app.mixer

import com.pocketstudio.model.FxPreset
import com.pocketstudio.model.FxPresets
import com.pocketstudio.model.i18n.tr

/**
 * The music styles the ready-made effects suit, for the « Style » chips of the effects box. Kept
 * with the screen (the presets themselves stay as they are): a preset added later for a style
 * gets its line here; an id that no longer exists is simply never shown.
 */
object FxGenres {
    const val ACID = "acid"
    const val TECHNO = "techno"
    const val DNB = "dnb"
    const val DUBSTEP = "dubstep"
    const val HOUSE = "house"
    const val LOFI = "lofi"

    /** In the order of the chips. */
    val ALL = listOf(ACID, TECHNO, DNB, DUBSTEP, HOUSE, LOFI)

    private val BY_PRESET: Map<String, Set<String>> = mapOf(
        "dub_deep" to setOf(TECHNO, DUBSTEP, DNB),
        "dub_echo" to setOf(TECHNO, DUBSTEP),
        "rumble" to setOf(TECHNO),
        "chopper" to setOf(TECHNO, DNB),
        "pump" to setOf(HOUSE, TECHNO),
        "edm_pump" to setOf(HOUSE, DUBSTEP),
        "punch" to setOf(DNB, HOUSE, TECHNO),
        "fat_bass" to setOf(DNB, DUBSTEP, ACID),
        "scream_lead" to setOf(DUBSTEP, ACID),
        "crunch" to setOf(ACID, TECHNO),
        "distortion" to setOf(ACID, DUBSTEP),
        "jet" to setOf(ACID, HOUSE),
        "robot" to setOf(DUBSTEP),
        "wobble" to setOf(DUBSTEP),
        "wob_dubstep" to setOf(DUBSTEP),
        "reese_dnb" to setOf(DNB),
        "underwater" to setOf(DUBSTEP, LOFI),
        "space" to setOf(HOUSE, DNB),
        "cathedral" to setOf(DNB),
        "wide_alive" to setOf(HOUSE),
        "pluck_pingpong" to setOf(HOUSE, TECHNO),
        "lofi_tape" to setOf(LOFI),
        "warm_vinyl" to setOf(LOFI, HOUSE),
        "radio" to setOf(LOFI),
        "phone" to setOf(LOFI),
        "8bit" to setOf(LOFI),
        "dream" to setOf(LOFI),
    )

    /** The styles preset [id] suits. */
    fun of(id: String): Set<String> = BY_PRESET[id].orEmpty()

    /** The presets for style [genre], in their usual order. */
    fun presets(genre: String): List<FxPreset> = FxPresets.ALL.filter { genre in of(it.id) }

    /** A style's chip. */
    fun label(genre: String): String = when (genre) {
        ACID -> tr("Acide")
        TECHNO -> tr("Techno")
        DNB -> tr("DnB")
        DUBSTEP -> tr("Dubstep")
        HOUSE -> tr("House")
        LOFI -> tr("Lo-fi")
        else -> genre
    }
}
