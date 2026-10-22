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

    fun decode(text: String): Project = json.decodeFromString(Project.serializer(), text).sanitized()

    /** [decode] read from [input] as it goes (UTF-8): a big file never sits whole in memory as text. */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    fun decode(input: java.io.InputStream): Project = json.decodeFromStream(Project.serializer(), input).sanitized()

    fun encodeFavorites(f: Favorites): String = json.encodeToString(Favorites.serializer(), f)

    fun decodeFavorites(text: String): Favorites = json.decodeFromString(Favorites.serializer(), text).sanitized()
}
