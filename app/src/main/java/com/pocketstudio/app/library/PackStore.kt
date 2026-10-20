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
    const val RELEASE = "sons-packs-3"
    private const val BASE = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/$RELEASE/"

    /** The list of packs the Packs workflow published (51: styles, machines, current sounds); null would mean none published yet. */
    val INDEX: VerifiedFile? = VerifiedFile(
        BASE + "index.json", "packs-index.json", "6a58044515b6a430ececdc6b5852ab0660d6137639db2073ca7256830f135766", 10332,
    )

    fun dir(context: Context) = File(context.filesDir, "packs")

    fun installed(context: Context): List<PackManifest> =
        dir(context).listFiles()?.filter { it.isDirectory && PackManifest.safeName(it.name) }?.mapNotNull { d ->
            runCatching { SoundPacks.parseManifest(File(d, MANIFEST).readText()) }.getOrNull()?.takeIf { it.id == d.name }
        }?.sortedBy { it.name }.orEmpty()

    fun file(context: Context, packId: String, sound: PackSound) = File(File(dir(context), packId), sound.file)

    fun bytesOnDisk(context: Context, packId: String): Long =
        File(dir(context), packId).listFiles()?.sumOf { it.length() } ?: 0L

    /** Half-installed packs left by a killed app (never while a download runs). */
    fun cleanLeftovers(context: Context) {
        dir(context).listFiles()?.filter { it.isDirectory && (it.name.endsWith(".part") || it.name.endsWith(".old")) }?.forEach { f ->
            // An old copy whose new pack never arrived is put back.
            val id = f.name.substringBeforeLast('.')
            if (f.name.endsWith(".old") && PackManifest.safeName(id) && !File(dir(context), id).exists()) f.renameTo(File(dir(context), id))
            else f.deleteRecursively()
        }
    }

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
                // The pack already there stays until the new one is in place.
                val final = File(root, entry.id)
                val old = File(root, "${entry.id}.old").apply { deleteRecursively() }
                if (final.exists() && !final.renameTo(old)) throw IOException("rename failed")
                if (!part.renameTo(final)) {
                    old.renameTo(final)
                    throw IOException("rename failed")
                }
                old.deleteRecursively()
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
