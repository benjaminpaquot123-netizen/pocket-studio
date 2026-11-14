package com.pocketstudio.model

/**
 * « Fête » (Benjamin's list n° 12): air horns, sirens, lasers, risers and drops synthesized by us
 * (scripts/party/synth_party.py, ours: no licence to manage), and a few shouts of the Kenney
 * Voiceover Pack (CC0), shipped in the app (assets `party/<id>.ogg`, about 470 kB in all). The
 * downloaded packs add theirs (risers and impacts, fireworks, sci-fi lasers): [fromPack].
 */
object PartySounds {
    /** The family's name, as the sound browser shows it (translated). */
    const val FAMILY = "Fête"

    /** Second-level groups (merged into « Fête » while they hold fewer than [SoundShelves.MIN_FOLDER] sounds). */
    const val HORNS = "Klaxons et sifflets"
    const val SIRENS = "Sirènes"
    const val LASERS = "Lasers"
    const val RISERS = "Montées et chutes"
    const val VOICES = "Cris et voix"
    const val FIREWORKS = "Pétards et feux d'artifice"

    /** One party sound of the app: [id] is its asset's name, [kind] a [SoundPacks.KIND_LABELS] key. */
    data class Sound(val id: String, val name: String, val group: String, val kind: String) {
        val asset: String get() = "party/$id.ogg"
    }

    val ALL: List<Sound> = listOf(
        Sound("air_horn", "Klaxon de DJ", HORNS, "fx"),
        Sound("air_horn_dancehall", "Klaxon dancehall", HORNS, "fx"),
        Sound("air_horn_long", "Klaxon long", HORNS, "fx"),
        Sound("whistle", "Sifflet", HORNS, "fx"),
        Sound("siren_dub", "Sirène dub", SIRENS, "fx"),
        Sound("siren_wail", "Sirène d'alarme", SIRENS, "fx"),
        Sound("siren_yelp", "Sirène de police", SIRENS, "fx"),
        Sound("laser", "Laser", LASERS, "fx"),
        Sound("laser_triple", "Triple laser", LASERS, "fx"),
        Sound("zap", "Zap électrique", LASERS, "fx"),
        Sound("riser", "Montée", RISERS, "fx"),
        Sound("riser_short", "Montée courte", RISERS, "fx"),
        Sound("drop", "Chute", RISERS, "fx"),
        Sound("impact", "Impact", RISERS, "fx"),
        Sound("voice_go_go_go", "« Go go go! »", VOICES, "vocal"),
        Sound("voice_go", "« Go! »", VOICES, "vocal"),
        Sound("voice_ready", "« Ready »", VOICES, "vocal"),
        Sound("voice_level_up", "« Level up! »", VOICES, "vocal"),
        Sound("voice_power_up", "« Power up! »", VOICES, "vocal"),
        Sound("voice_fire", "« Fire in the hole! »", VOICES, "vocal"),
        Sound("voice_you_win", "« You win! »", VOICES, "vocal"),
        Sound("voice_hurry_up", "« Hurry up! »", VOICES, "vocal"),
    )

    fun byId(id: String): Sound? = ALL.firstOrNull { it.id == id }

    /**
     * The group of « Fête » a downloaded pack's sound also belongs to, if any: the risers and
     * impacts of « Montées et impacts », the bangs and fireworks of « Bruitages : eau, feu et
     * ambiances », the sci-fi sounds (lasers) of « Clics, bips et jeux vidéo ».
     */
    fun fromPack(packId: String, sound: PackSound): String? = when {
        packId == "fx" && sound.kind == "fx" -> RISERS
        packId == "bruitages_nature" && sound.file.startsWith("fx_explosions") -> FIREWORKS
        packId == "sons_jeux" && sound.file.startsWith("fx_scifi") -> LASERS
        else -> null
    }
}
