package com.pocketstudio.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

/**
 * JSON encoding of projects, tolerant to files written by older and newer versions. Written
 * compact: an indented file of a busy project is four times bigger (it made exported .zips too big
 * to import back); older indented files still load.
 */
object ProjectJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun encode(project: Project): String = json.encodeToString(Project.serializer(), project)

    fun decode(text: String): Project = json.decodeFromString(Project.serializer(), text).sanitized().quietUnusedAcid().withAcidcoreSaturationLevelled()

    /** [decode] read from [input] as it goes (UTF-8): a big file never sits whole in memory as text. */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    fun decode(input: java.io.InputStream): Project = json.decodeFromStream(Project.serializer(), input).sanitized().quietUnusedAcid().withAcidcoreSaturationLevelled()

    /**
     * A 303 that plays nothing yet, saved at the level it had before its calibration (phase 30):
     * brought to the calibrated one, so that the riff written into it later does not cover the song.
     * Nothing changes to what is heard (it is silent); a 303 that plays keeps its volume.
     */
    internal fun Project.quietUnusedAcid(): Project {
        if (acid.steps.any { it.gate } || scenes.any { s -> s.acid.steps.any { it.gate } }) return this
        fun AcidPattern.quiet() = if (sound.level == AcidSound.OLD_LEVEL) copy(sound = sound.copy(level = AcidSound.DEFAULT_LEVEL)) else this
        return copy(acid = acid.quiet(), scenes = scenes.map { it.copy(acid = it.acid.quiet()) })
    }

    /**
     * An Acidcore song the assistant made at [Project.MIX_V4] (before Benjamin's « le gros kick disparaît », APK 233):
     * its 303's saturation as the style gave it (drive 0.45, its level not held) at a constant level now, as the new
     * songs have it, then [Project.MIX_V5], so that it is done once (a saturation set back by hand stays so).
     * Only that insert, only with the style's own settings: anything the user changed is left as it is.
     */
    internal fun Project.withAcidcoreSaturationLevelled(): Project {
        if (mixVersion != Project.MIX_V4 || style != "acidcore") return this
        val ch = AutoTarget.ACID_CHANNEL
        val strip = mixer.channels.getOrNull(ch) ?: return this
        val inserts = strip.inserts.map {
            if (it.effect == EffectType.SATURATION && it.params.take(EffectType.SAT_COMPENSATION) == OLD_ACIDCORE_SATURATION && it.params[EffectType.SAT_COMPENSATION] < 0.5f) {
                it.copy(params = it.params.toMutableList().also { v -> v[EffectType.SAT_COMPENSATION] = 1f })
            } else it
        }
        val channels = mixer.channels.toMutableList().also { it[ch] = strip.copy(inserts = inserts) }
        return copy(mixer = mixer.copy(channels = channels), mixVersion = Project.MIX_V5)
    }

    /** The Acidcore 303's saturation before [Project.MIX_V5]: drive, tone, mix, volume (StyleRecipes). */
    private val OLD_ACIDCORE_SATURATION = listOf(0.45f, 0.75f, 1.0f, 0.7f)

    fun encodeFavorites(f: Favorites): String = json.encodeToString(Favorites.serializer(), f)

    fun decodeFavorites(text: String): Favorites = json.decodeFromString(Favorites.serializer(), text).sanitized()
}
