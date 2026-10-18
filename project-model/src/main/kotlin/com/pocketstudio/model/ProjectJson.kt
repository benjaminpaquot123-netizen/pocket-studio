package com.pocketstudio.model

import kotlinx.serialization.json.Json

/** JSON encoding of projects, tolerant to files written by older and newer versions. */
object ProjectJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        coerceInputValues = true
    }

    fun encode(project: Project): String = json.encodeToString(Project.serializer(), project)

    fun decode(text: String): Project = json.decodeFromString(Project.serializer(), text).sanitized()
}
