package com.pocketstudio.app.library

import com.pocketstudio.model.i18n.tr
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
 * is downloaded on demand from our Releases (checked by SHA-256) and can be deleted at any time.
 * The list comes from the fixed `sons-packs` catalogue release (new packs and updates arrive
 * without a new app), else from its last copy, else from the index pinned in the app.
 * Blocking calls: background threads only.
 */
object PackStore {
    const val RELEASE = "sons-packs-4"
    private const val RELEASES = "https://github.com/benjaminpaquot123-netizen/pocket-studio/releases/download/"
    private const val BASE = "$RELEASES$RELEASE/"
    const val CATALOGUE = RELEASES + "sons-packs/index.json"
    /** Where the packs are downloaded from (a test points it at a stand-in server). */
    internal var releasesBase = RELEASES
    private const val CATALOGUE_MAX = 1_000_000

    /** The list of packs the Packs workflow published (53: styles, machines, current sounds); null would mean none published yet. */
    val INDEX: VerifiedFile? = VerifiedFile(
        BASE + "index.json", "packs-index.json", "4038b2e60892451140870bf6147c4ec20f8d78b3739c4fdaecd3e0d490ef515f", 10744,
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

    /** The list of packs: the catalogue, its last copy, or the pinned index; null when none is published yet. */
    fun fetchIndex(context: Context): PackIndex? {
        // Next to the packs (left out of backups like them).
        val saved = File(dir(context), "catalogue.json")
        runCatching { fetchCatalogue() }.getOrNull()?.let { (text, index) ->
            runCatching { saved.parentFile?.mkdirs(); saved.writeText(text) }
            return index
        }
        runCatching { SoundPacks.parseIndex(saved.readText()) }.getOrNull()?.takeIf { it.packs.isNotEmpty() }?.let { return it }
        val spec = INDEX ?: return null
        val target = File(context.cacheDir, "packs-index.json")
        if (!VerifiedDownload.verify(spec, target)) VerifiedDownload.fetch(spec, target, {})
        return SoundPacks.parseIndex(target.readText())
    }

    /** The catalogue (HTTPS from our own release, size capped; each pack in it is checked by its SHA-256). */
    private fun fetchCatalogue(): Pair<String, PackIndex>? {
        val c = (java.net.URL(CATALOGUE).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
        }
        try {
            if (c.responseCode != 200) return null
            val out = java.io.ByteArrayOutputStream()
            c.inputStream.use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    if (out.size() > CATALOGUE_MAX) return null
                }
            }
            val bytes = out.toByteArray()
            val text = bytes.toString(Charsets.UTF_8)
            return SoundPacks.parseIndex(text).takeIf { it.packs.isNotEmpty() }?.let { text to it }
        } finally {
            c.disconnect()
        }
    }

    /** Where [entry]'s zip is: its own release, or the pinned index's. */
    fun url(entry: PackEntry) = releasesBase + entry.release.ifEmpty { RELEASE } + "/" + entry.file

    /**
     * An instrument's preview, heard before its pack is downloaded (checked by SHA-256, kept in
     * the cache); null when the catalogue has none for it. Blocking: a worker thread.
     */
    fun preview(context: Context, entry: PackEntry, isCancelled: () -> Boolean = { false }): File? {
        if (entry.preview.isEmpty()) return null
        val spec = VerifiedFile(RELEASES + entry.release.ifEmpty { RELEASE } + "/" + entry.preview, entry.preview, entry.previewSha256, entry.previewBytes)
        val target = File(context.cacheDir, "apercu-${entry.previewSha256.take(12)}.flac")
        if (!VerifiedDownload.verify(spec, target)) VerifiedDownload.fetch(spec, target, {}, isCancelled)
        return target
    }

    /**
     * Downloads, checks and unpacks [entry]; a pack already there is replaced only when complete.
     * A cut download resumes where it stopped on the next try (the same file only: its SHA-256 is in the name).
     */
    fun install(context: Context, entry: PackEntry, onProgress: (Float) -> Unit, isCancelled: () -> Boolean) {
        val zip = File(context.cacheDir, "pack-${entry.id}-${entry.sha256.take(12)}.zip")
        val spec = VerifiedFile(url(entry), zip.name, entry.sha256, entry.sizeBytes)
        // The zip plus its unpacked sounds: refused before downloading when the phone is full.
        val needed = entry.sizeBytes * 3
        if (context.filesDir.usableSpace < needed) {
            throw IOException(tr("Stockage plein : il faut %d Mo libres.", needed / 1_000_000))
        }
        try {
            VerifiedDownload.fetch(spec, zip, onProgress, isCancelled, resume = true)
            val root = dir(context).apply { mkdirs() }
            val part = File(root, "${entry.id}.part").apply { deleteRecursively(); mkdirs() }
            try {
                unpack(zip, part)
                val manifest = SoundPacks.parseManifest(File(part, MANIFEST).readText())
                if (manifest.id != entry.id) throw IOException(tr("pack inattendu"))
                // Only the sounds the manifest lists are kept; it must list at least one that is there.
                // An instrument's zones must all be there (a missing one would leave a hole in the keyboard).
                val kept = manifest.copy(sounds = manifest.sounds.filter { File(part, it.file).isFile })
                if (kept.sounds.isEmpty()) throw IOException(tr("pack vide"))
                if (kept.instrument?.zones?.any { !File(part, it.file).isFile } == true) throw IOException(tr("pack incomplet"))
                val files = kept.files
                part.listFiles()?.filter { f -> f.name != MANIFEST && f.name !in files }?.forEach { it.delete() }
                // The version the index announced is the one installed: no update offered again for it.
                File(part, MANIFEST).writeText(json.encodeToString(PackManifest.serializer(), kept.copy(version = maxOf(kept.version, entry.version))))
                // The pack already there stays until the new one is in place.
                val final = File(root, entry.id)
                val old = File(root, "${entry.id}.old").apply { deleteRecursively() }
                if (final.exists() && !final.renameTo(old)) throw IOException(tr("écriture impossible"))
                if (!part.renameTo(final)) {
                    old.renameTo(final)
                    throw IOException(tr("écriture impossible"))
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
                        if (total > PackEntry.MAX_BYTES * 2) throw IOException(tr("pack trop gros"))
                        out.write(buffer, 0, n)
                    }
                }
            }
        }
    }

    private const val MANIFEST = "pack.json"
    private val json = kotlinx.serialization.json.Json { encodeDefaults = true }
}
