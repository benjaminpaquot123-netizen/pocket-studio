package com.pocketstudio.app.bench

import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Records the engine calls of each export as a `.pscmd` file (format: engine/bench/ps_render.cpp,
 * docs/plan/son.md § 2.2), which `ps_render` replays on Linux through the engine core.
 *
 * Sounds go to [samplesDir] as raw float32, named by their content (the same kit voice is stored
 * once). The offline renders the app asks for while it builds a song (a kit voice rendered as a
 * pad sound) are run by [renderer] (the `ps_render` program): the app then gets the real sound.
 * Commands to the live engine (target 0) are only counted.
 */
class RecordingSink(private val samplesDir: File, private val renderer: File?) : CommandSink {
    private class Sound(val channels: Int, val rate: Int, val frames: Long, val file: String)

    private val sounds = HashMap<Long, Sound>()
    private val exports = HashMap<Long, StringBuilder>()
    // Per export: the number each sound has in its file (1, 2…: by first use, so that the file does
    // not depend on the order sounds were loaded in).
    private val declared = HashMap<Long, MutableMap<Long, Int>>()
    private val renders = HashMap<String, FloatArray>()  // offline renders already run, by their commands
    private var next = 1L

    /** Commands sent to the live engine (not recorded). */
    var liveCommands = 0
        private set

    /**
     * While set, the live engine's commands (target 0) are also written there in the same format
     * (with `WAIT` lines the test adds): what the app sends the phone's engine, replayed by `ps_live`.
     */
    var live: StringBuilder? = null

    /** The scene swaps the live engine reports ([NativeEngine.sceneSwaps]): the test moves it on. */
    @Volatile var swaps = 0

    override fun sceneSwaps(): Int = swaps

    /** A line in the live recording (a `WAIT` the replay honours, a comment). */
    @Synchronized
    fun mark(line: String) {
        live?.append(line)?.append('\n')
    }

    /** Lines written after the header of the next [exportRender] (META, SONG, SECTION), then cleared. */
    var nextHeader: List<String> = emptyList()

    @Synchronized
    override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
        val out = exports[target] ?: run {
            liveCommands++
            live?.append("C ")?.append(type)?.append(' ')?.append(value.toString())?.append(' ')?.append(a)?.append(' ')?.append(b)?.append('\n')
            return
        }
        out.append("C ").append(type).append(' ').append(value.toString()).append(' ').append(a).append(' ').append(b).append('\n')
    }

    @Synchronized
    override fun scheduleFrom(target: Long, frame: Long) {
        exports[target]?.append("AT ")?.append(frame)?.append('\n')
    }

    @Synchronized
    override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) {
        val out = exports[target] ?: run {
            liveCommands++
            live ?: return
        }
        var local = 0
        if (sample != 0L) {
            val numbers = declared.getOrPut(target) { HashMap() }
            local = numbers[sample] ?: (numbers.size + 1).also { n ->
                numbers[sample] = n
                val s = checkNotNull(sounds[sample]) { "unknown sample $sample" }
                out.append("SAMPLE $n ${s.channels} ${s.rate} ${samplesDir.name}/${s.file}\n")
            }
        }
        val line = when (kind) {
            CommandSink.Install.PadSample -> "PAD $index $local"
            CommandSink.Install.TrackSample -> "TRACKSAMPLE $index $local"
            CommandSink.Install.AudioClip -> "AUDIOCLIP $index $local"
            CommandSink.Install.Stems -> "STEMS $local"
            CommandSink.Install.Instrument -> "INSTRUMENT $index 0"
            // Live only (the JNI never sends them to an export engine).
            CommandSink.Install.LiveClip, CommandSink.Install.PreviewStems -> return
        }
        out.append(line).append('\n')
    }

    @Synchronized
    override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long {
        if (channels < 1 || data.size < channels) return 0L
        val bytes = ByteBuffer.allocate(data.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        bytes.asFloatBuffer().put(data)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("$channels/$sampleRate/".toByteArray())
        digest.update(bytes.array())
        val name = digest.digest().take(10).joinToString("") { "%02x".format(it) } + ".f32"
        samplesDir.mkdirs()
        val file = File(samplesDir, name)
        if (!file.exists()) file.writeBytes(bytes.array())
        val frames = data.size / channels
        // The engine's frames at 48 kHz (Sample::create resamples).
        val at48 = if (sampleRate == 48_000) frames.toLong() else Math.round(frames * 48_000.0 / sampleRate)
        val id = next++
        sounds[id] = Sound(channels, sampleRate, at48, name)
        return id
    }

    @Synchronized
    override fun sampleFrames(handle: Long): Long = sounds[handle]?.frames ?: 0L

    override fun sampleRelease(handle: Long) = Unit  // kept: a later export may still name it

    @Synchronized
    override fun exportCreate(): Long {
        val id = next++
        exports[id] = StringBuilder()
        return id
    }

    @Synchronized
    override fun exportDestroy(target: Long) {
        exports.remove(target)
        declared.remove(target)
    }

    private fun document(
        target: Long, header: List<String>, fade: Long, autoTail: Boolean, floatWav: Boolean, kind: String, frames: Long, tail: Long,
        sampleRate: Int = 48_000, bits: Int = 32,
    ): String {
        val body = checkNotNull(exports[target]) { "unknown export $target" }
        return buildString {
            append("PSCMD 1\nRATE 48000\n")
            header.forEach { append(it).append('\n') }
            append(body)
            append("AT -1\n")
            append("FADE $fade\n")
            append("OPTIONS ${if (autoTail) 1 else 0} ${if (floatWav) 1 else 0}\n")
            // The file's format as the app asked it (ps_render --as-app; the bench keeps float 48 kHz).
            append("FORMAT $sampleRate $bits\n")
            append("RENDER $kind $frames $tail\n")
        }
    }

    @Synchronized
    override fun exportRender(
        target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
        sampleRate: Int, bits: Int,
    ): ExportResult {
        val text = document(target, nextHeader, fadeOutFrames, autoTail, floatWav, "wav", frames, tailFrames, sampleRate, bits)
        nextHeader = emptyList()
        File(path).writeText(text)
        return ExportResult.Ok
    }

    override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray {
        val text = synchronized(this) { document(target, emptyList(), 0, false, false, "float", frames, tailFrames) }
        synchronized(renders) { renders[text]?.let { return it.copyOf() } }
        val program = checkNotNull(renderer) { "an offline render needs ps_render (-Pbench.render)" }
        val dir = samplesDir.parentFile
        val cmd = File.createTempFile("render-", ".pscmd", dir)
        val raw = File(dir, cmd.name.removeSuffix(".pscmd") + ".f32")
        try {
            cmd.writeText(text)
            val process = ProcessBuilder(program.absolutePath, cmd.absolutePath, raw.absolutePath).redirectErrorStream(true).start()
            val log = process.inputStream.bufferedReader().readText()
            check(process.waitFor() == 0) { "ps_render failed: $log" }
            val bytes = raw.readBytes()
            val out = FloatArray(bytes.size / 4)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(out)
            synchronized(renders) { renders[text] = out.copyOf() }
            return out
        } finally {
            cmd.delete()
            raw.delete()
        }
    }
}
