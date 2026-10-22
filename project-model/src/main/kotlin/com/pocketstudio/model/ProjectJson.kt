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

    fun decode(text: String): Project = json.decodeFromString(Project.serializer(), text).sanitized().quietUnusedAcid()

    /** [decode] read from [input] as it goes (UTF-8): a big file never sits whole in memory as text. */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    fun decode(input: java.io.InputStream): Project = json.decodeFromStream(Project.serializer(), input).sanitized().quietUnusedAcid()

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

    fun encodeFavorites(f: Favorites): String = json.encodeToString(Favorites.serializer(), f)

    fun decodeFavorites(text: String): Favorites = json.decodeFromString(Favorites.serializer(), text).sanitized()
}
