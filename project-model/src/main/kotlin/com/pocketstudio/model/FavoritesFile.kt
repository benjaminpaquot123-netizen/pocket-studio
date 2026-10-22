package com.pocketstudio.model

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * The favourites' file (favorites.json, shared by every project). A file that cannot be read is
 * never written over: it is set aside first (`favorites-illisible-<time>.json`), as an unreadable
 * project is, so an update that misreads it or a damaged copy loses nothing for good.
 */
class FavoritesFile(private val file: File) {
    /** What [load] found: the favourites, and the name a file it could not read was set aside under. */
    data class Loaded(val favorites: Favorites, val setAside: String? = null)

    /** False after an unreadable file could not be set aside: [save] then leaves it alone. */
    var writable = true
        private set

    fun load(now: Long = System.currentTimeMillis()): Loaded {
        if (!file.exists()) return Loaded(Favorites())
        return try {
            Loaded(ProjectJson.decodeFavorites(file.readText(Charsets.UTF_8)))
        } catch (e: Exception) {
            val aside = File(file.parentFile, "favorites-illisible-$now.json")
            if (file.renameTo(aside)) Loaded(Favorites(), aside.name)
            else {
                writable = false
                Loaded(Favorites())
            }
        }
    }

    /** Writes [favorites] through a flushed .tmp, then renames it into place. */
    @Throws(IOException::class)
    fun save(favorites: Favorites) {
        if (!writable) throw IOException("favorites.json illisible et pas mis de côté : pas réécrit")
        val tmp = File(file.path + ".tmp")
        FileOutputStream(tmp).use { out ->
            out.write(ProjectJson.encodeFavorites(favorites).toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (!tmp.renameTo(file)) throw IOException("renommage impossible")
    }
}
