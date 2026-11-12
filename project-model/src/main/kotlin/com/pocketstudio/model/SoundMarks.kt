package com.pocketstudio.model

/**
 * « Récents » and « Favoris » of the sound browser: sounds by key (`kit:3`, `pack:tr909/kick_01.flac`,
 * `party:air_horn`, `mine:<id>`), kept on the phone for every project. Only well-formed keys are
 * read back (a damaged file gives fewer marks, never a crash), the lists are capped, and a sound
 * no longer on the phone (a pack deleted) is simply not shown.
 */
object SoundMarks {
    const val MAX_RECENTS = 24
    const val MAX_FAVORITES = 200
    private const val MAX_KEY = 160

    private val KEY = Regex("""(kit:\d{1,2}|pack:[A-Za-z0-9_.-]{1,64}/[A-Za-z0-9_.-]{1,80}|party:[a-z0-9_]{1,40}|mine:[A-Za-z0-9_-]{1,64})""")

    fun isKey(key: String): Boolean = key.length <= MAX_KEY && KEY.matches(key)

    /** [key] first, once, the list capped to [max]. */
    fun touched(list: List<String>, key: String, max: Int = MAX_RECENTS): List<String> =
        if (!isKey(key)) list else (listOf(key) + list.filter { it != key }).take(max)

    /** [key] added (first) or removed. */
    fun toggled(list: List<String>, key: String): List<String> = when {
        !isKey(key) -> list
        key in list -> list - key
        else -> (listOf(key) + list).take(MAX_FAVORITES)
    }

    /** One key per line. */
    fun write(list: List<String>): String = list.filter(::isKey).joinToString("\n")

    /** Back from [write]: unknown lines and duplicates dropped, capped to [max]. */
    fun read(text: String?, max: Int): List<String> =
        text.orEmpty().lineSequence().map { it.trim() }.filter(::isKey).distinct().take(max).toList()
}
