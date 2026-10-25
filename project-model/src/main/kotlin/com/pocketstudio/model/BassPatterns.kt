package com.pocketstudio.model

/**
 * « Des basses qui tapent » (docs/hits/basses-qui-tapent.md § 4): the rolling basslines, as grids
 * the generator plays ([StarterWizard.Melody.bassGrid], [MelodyBuilder.bass]) and the assistant
 * offers where the bass is chosen (« Motifs de basse »). A grid is a [StepLine] line ([pattern]:
 * 16 steps, or 64 for four bars) on the bars' chords, played with its own [velocities] (one per
 * sixteenth of a beat, cycling: the « gallop »; null: the generator's) and [gate] (the part of each
 * note that sounds: the notes never touch, and end before the next kick). [triplets]: the beat's
 * three eighth-note triplets, the first one empty (grid 10); [power]: each note doubled at the fifth
 * and the octave (grid 11). [name] and [description] (a few words, under the name in « Motifs de
 * basse »): saved and shown in French, translated at display.
 */
data class BassGrid(
    val id: String,
    val name: String,
    val pattern: String,
    val velocities: List<Int>? = null,
    val gate: Float = 1f,
    val triplets: Boolean = false,
    val power: Boolean = false,
    val description: String = "",
)

object BassPatterns {
    /** Grids 1 to 11 of docs/hits/basses-qui-tapent.md § 4 (the chapters of « 10 Rolling Basslines »). */
    val ALL: List<BassGrid> = listOf(
        BassGrid("rolling_kbbb", "Sans le temps fort", ".rRr.rRr.rRr.rRr", listOf(0, 70, 100, 75), gate = 0.7f, description = "Entre les kicks"),
        BassGrid("rolling_full", "Avec le temps fort", "rrRrrrRrrrRrrrRr", listOf(45, 70, 100, 70), gate = 0.55f, description = "Sans un trou"),
        BassGrid("sixteenths", "Doubles-croches", "RRRRRRRRRRRRRRRR", listOf(100, 100, 100, 100), gate = 0.5f, description = "Toutes égales"),
        BassGrid("trance_rolling", "Trance roulante", ".rRr.rRr.rRr.rR5".repeat(3) + ".rRr.rRr.rRO.5R3", listOf(0, 65, 100, 75), gate = 0.7f, description = "Avec des quintes"),
        BassGrid("arp_rolling", "Arpège roulant", ".R5O.R5O.R5O.R53", listOf(0, 80, 100, 80), gate = 0.6f, description = "Trois hauteurs"),
        BassGrid("three_two", "3-2-2-3", "R--R-R-R--R--R--", gate = 0.8f, description = "Ça chaloupe"),
        BassGrid("octave_bounce", "Deux notes", ".rOr.rOr.rOr.rOr", listOf(0, 70, 100, 70), gate = 0.5f, description = "Rebond d'octave"),
        BassGrid("velocity_roll", "Vélocité et modulation", ".rRr.rRr.rRr.rRr", listOf(40, 70, 100, 70), gate = 0.6f, description = "Fort, doux : galop"),
        BassGrid("lengths", "Longueurs variées", ".R-r.Rr-.R--.rR-", gate = 0.9f, description = "Courtes et longues"),
        BassGrid("triplets", "Triolets", "R---R---R---R---", listOf(80, 100), gate = 0.6f, triplets = true, description = "Ça balance"),
        BassGrid("power", "Power chord", ".rRr.rRr.rRr.rRr", listOf(0, 70, 100, 75), gate = 0.6f, power = true, description = "Trois notes à la fois"),
    )

    fun byId(id: String?): BassGrid? = ALL.firstOrNull { it.id == id }

    /** Each style's grids (docs/hits/basses-qui-tapent.md § 4 and § 9), the best first; the others follow them in the list. */
    private val BY_STYLE: Map<String, List<String>> = mapOf(
        "techno" to listOf("rolling_kbbb", "velocity_roll"),
        "retro_techno" to listOf("rolling_kbbb", "arp_rolling"),
        "hard_techno" to listOf("rolling_full", "power", "rolling_kbbb"),
        "industrial_techno" to listOf("rolling_kbbb", "power"),
        "acid_techno" to listOf("trance_rolling", "rolling_kbbb"),
        "gabber" to listOf("rolling_full", "power"),
        "acidcore" to listOf("rolling_full", "power"),
        "trance" to listOf("trance_rolling", "arp_rolling", "rolling_kbbb"),
        "tech_house" to listOf("velocity_roll", "three_two", "rolling_full", "lengths"),
        "minimal" to listOf("lengths", "sixteenths", "velocity_roll"),
        "acid_house" to listOf("sixteenths", "octave_bounce"),
        "house" to listOf("octave_bounce", "three_two"),
        "deep_house" to listOf("lengths", "three_two"),
        "afro_house" to listOf("triplets", "three_two"),
        "amapiano" to listOf("triplets", "lengths"),
        "disco" to listOf("octave_bounce"),
        "speed_garage" to listOf("octave_bounce"),
        "synthwave" to listOf("octave_bounce", "arp_rolling"),
    )

    /** The grids the analysis gives [styleId] (« Conseillé »). */
    fun advised(styleId: String?): Set<String> = BY_STYLE[styleId].orEmpty().toSet()

    /** [styleId]'s grids first (those the analysis gives it), then every other one. */
    /**
     * The grids offered to [style]: [forStyle]'s, without the power chord when its bass glides (tech
     * house, speed garage, dnb, jump up…: a sliding bass plays one note at a time).
     */
    fun offered(style: StarterWizard.Style): List<BassGrid> = forStyle(style.id).filter { !(it.power && style.melody.bassGlide) }

    fun forStyle(styleId: String?): List<BassGrid> {
        val own = BY_STYLE[styleId].orEmpty().mapNotNull(::byId)
        return own + (ALL - own.toSet())
    }

    /** [style] playing [grid] as its bass line (null: the style as it is). */
    fun styled(style: StarterWizard.Style, grid: BassGrid?): StarterWizard.Style =
        if (grid == null) style else style.copy(melody = style.melody.copy(bassPattern = grid.pattern, bassVariation = "", bassGrid = grid.id))

    /** The grid [style] plays its bass on ([styled]), or null: its recipe's own line. */
    fun of(style: StarterWizard.Style): BassGrid? = byId(style.melody.bassGrid.ifBlank { null })

    /** A grid's 100 (MelodyBuilder's bass velocity: a grid plays as loud as the recipe's line). */
    const val TOP_VELOCITY = 0.9f

    /** Ticks of a beat, and of an eighth-note triplet. */
    private const val BEAT = 4 * NoteClip.TICKS_PER_STEP
    private const val TRIPLET = BEAT / 3

    /**
     * The notes of a bass line written on [grid]'s pattern made into the grid: its velocities (one
     * per sixteenth of the beat, cycling), its gate (no note touches the next: a slide keeps its
     * overlap), its triplets (each beat's note becomes the 2nd and 3rd eighth-note triplets of the
     * beat) or its power chords (the fifth and the octave over each note, softer).
     */
    fun played(clip: NoteClip, grid: BassGrid, top: Float = TOP_VELOCITY): NoteClip {
        val t = NoteClip.TICKS_PER_STEP
        var notes = clip.notes.sortedBy { it.start }
        if (grid.triplets) {
            notes = notes.flatMap { n ->
                val beat = n.start - Math.floorMod(n.start, BEAT)
                listOf(1, 2).map { k ->
                    val start = beat + k * TRIPLET
                    n.copy(start = start, length = (TRIPLET * grid.gate).toInt().coerceAtLeast(6))
                }
            }.distinctBy { it.start }
        }
        notes = notes.map { n ->
            val step = n.start / t
            val index = if (grid.triplets) Math.floorMod(n.start, BEAT) / TRIPLET - 1 else step % 4
            val v = grid.velocities?.let { vs -> vs[Math.floorMod(index, vs.size)].takeIf { it > 0 }?.let { it / 100f * top } } ?: n.velocity
            val slide = !grid.triplets && n.length % t != 0
            val length = if (slide || grid.triplets) n.length else (n.length * grid.gate).toInt().coerceAtLeast(6)
            n.copy(velocity = v.coerceIn(0.05f, 1f), length = length)
        }
        if (grid.power) {
            notes = notes + notes.flatMap { n -> listOf(7, 12).map { n.copy(pitch = n.pitch + it, velocity = (n.velocity * 0.6f).coerceAtLeast(0.05f)) } }
        }
        return clip.copy(notes = notes.sortedWith(compareBy({ it.start }, { it.pitch }))).sanitized()
    }
}
