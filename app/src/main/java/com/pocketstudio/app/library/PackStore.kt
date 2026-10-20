package com.pocketstudio.app.library

import android.content.Context
import com.pocketstudio.app.download.VerifiedDownload
import com.pocketstudio.app.download.VerifiedFile
import com.pocketstudio.model.PackEntry
import com.pocketstudio.model.PackIndex
import com.pocketstudio.model.PackManifest
import com.pocketstudio.model.PackSound
import com.pocketstudio.model.SoundPacks
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * The sound packs on the phone: `files/packs/<id>/` with its pack.json and FLAC files. Each one
 * is downloaded on demand from our Release (checked by SHA-256) and can be deleted at any time.
 * Blocking calls: background threads only.
 */
object PackStore {
    const val RELEASE = "sons-packs-2"
    private const val BASE = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/$RELEASE/"

    /** The list of packs, pinned once the Packs workflow has built them; null = none published yet. */
    val INDEX: VerifiedFile? = null

    fun dir(context: Context) = File(context.filesDir, "packs")

    fun installed(context: Context): List<PackManifest> =
        dir(context).listFiles()?.filter { it.isDirectory && PackManifest.safeName(it.name) }?.mapNotNull { d ->
            runCatching { SoundPacks.parseManifest(File(d, MANIFEST).readText()) }.getOrNull()?.takeIf { it.id == d.name }
        }?.sortedBy { it.name }.orEmpty()

    fun file(context: Context, packId: String, sound: PackSound) = File(File(dir(context), packId), sound.file)

    fun bytesOnDisk(context: Context, packId: String): Long =
        File(dir(context), packId).listFiles()?.sumOf { it.length() } ?: 0L

    fun delete(context: Context, packId: String) {
        if (PackManifest.safeName(packId)) File(dir(context), packId).deleteRecursively()
    }

    /** The list of packs on the Release, or null when none is published yet. */
    fun fetchIndex(context: Context): PackIndex? {
        val spec = INDEX ?: return null
        val target = File(context.cacheDir, "packs-index.json")
        if (!VerifiedDownload.verify(spec, target)) VerifiedDownload.fetch(spec, target, {})
        return SoundPacks.parseIndex(target.readText())
    }

    /** Downloads, checks and unpacks [entry]; a pack already there is replaced only when complete. */
    fun install(context: Context, entry: PackEntry, onProgress: (Float) -> Unit, isCancelled: () -> Boolean) {
        val zip = File(context.cacheDir, "pack-${entry.id}.zip")
        val spec = VerifiedFile(BASE + entry.file, zip.name, entry.sha256, entry.sizeBytes)
        try {
            VerifiedDownload.fetch(spec, zip, onProgress, isCancelled)
            val root = dir(context).apply { mkdirs() }
            val part = File(root, "${entry.id}.part").apply { deleteRecursively(); mkdirs() }
            try {
                unpack(zip, part)
                val manifest = SoundPacks.parseManifest(File(part, MANIFEST).readText())
                if (manifest.id != entry.id) throw IOException("pack inattendu")
                // Only the sounds the manifest lists are kept; it must list at least one that is there.
                val kept = manifest.copy(sounds = manifest.sounds.filter { File(part, it.file).isFile })
                if (kept.sounds.isEmpty()) throw IOException("pack vide")
                part.listFiles()?.filter { f -> f.name != MANIFEST && kept.sounds.none { it.file == f.name } }?.forEach { it.delete() }
                val final = File(root, entry.id)
                final.deleteRecursively()
                if (!part.renameTo(final)) throw IOException("rename failed")
            } finally {
                part.deleteRecursively()
            }
        } finally {
            zip.delete()
        }
    }

    // Flat: only plain file names, a size cap, no folders (the packs are built that way).
    private fun unpack(zip: File, into: File) {
        var total = 0L
        ZipInputStream(zip.inputStream().buffered()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val name = e.name.substringAfterLast('/')
                if (e.isDirectory || !PackManifest.safeName(name)) continue
                File(into, name).outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = z.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > PackEntry.MAX_BYTES * 2) throw IOException("pack trop gros")
                        out.write(buffer, 0, n)
                    }
                }
            }
        }
    }

    private const val MANIFEST = "pack.json"
}
