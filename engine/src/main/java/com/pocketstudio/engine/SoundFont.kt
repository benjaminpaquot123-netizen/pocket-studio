package com.pocketstudio.engine

import java.io.File

/** One preset of a SoundFont: [bank] 128 holds drum kits in General MIDI. */
data class Preset(val index: Int, val bank: Int, val program: Int, val name: String)

/**
 * A loaded SoundFont bank (shared sample data). Each engine plays its own light copy, made when
 * an instrument is chosen, so [close] only frees this master. Main thread, except [load].
 */
class SoundFont private constructor(internal val handle: Long) : AutoCloseable {
    val presets: List<Preset> = NativeEngine.soundFontPresets(handle).mapIndexed { i, line ->
        val (bank, program, name) = line.split("|", limit = 3)
        Preset(i, bank.toInt(), program.toInt(), name.trim())
    }

    fun indexOf(bank: Int, program: Int): Int = presets.firstOrNull { it.bank == bank && it.program == program }?.index ?: -1

    /** Main thread. One note of [preset] as mono 48 kHz audio (trailing silence trimmed). */
    fun renderNote(preset: Preset, note: Int, velocity: Float = 0.9f, holdSeconds: Float = 0.3f, maxSeconds: Float = 2.5f): FloatArray {
        check(!closed)
        return NativeEngine.soundFontRenderNote(handle, preset.index, note, velocity, (holdSeconds * 48_000).toInt(), (maxSeconds * 48_000).toInt())
    }

    private var closed = false

    override fun close() {
        if (closed) return
        closed = true
        NativeEngine.soundFontClose(handle)
    }

    companion object {
        /** Blocking (worker thread). Only for verified files: a corrupt bank is not safe to parse. */
        fun load(file: File): SoundFont? {
            if (NativeEngine.sink != null) return null  // banks are not recorded by a sink
            val h = NativeEngine.soundFontLoad(file.absolutePath)
            return if (h == 0L) null else SoundFont(h)
        }
    }
}
