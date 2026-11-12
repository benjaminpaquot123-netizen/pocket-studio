package com.pocketstudio.app.audio

import android.media.AudioDeviceInfo
import com.pocketstudio.engine.CommandSink
import com.pocketstudio.engine.ExportResult
import com.pocketstudio.engine.NativeEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tour 5, lot « kick » (docs/audits/ultra/tour-5-kick-rapport.md): on the phone's own speaker the
 * engine plays the kick for a small speaker (SpeakerKick.h); on headphones and Bluetooth, as mixed.
 * The route Android reports reaches the LIVE engine (command 210), never an export.
 */
class SpeakerKickRouteTest {
    private class Posts : CommandSink {
        val posts = ArrayList<FloatArray>()  // target, type, value
        override fun post(target: Long, type: Int, value: Float, a: Int, b: Int) {
            posts += floatArrayOf(target.toFloat(), type.toFloat(), value)
        }
        override fun scheduleFrom(target: Long, frame: Long) = Unit
        override fun install(target: Long, kind: CommandSink.Install, index: Int, sample: Long) = Unit
        override fun sampleCreate(data: FloatArray, channels: Int, sampleRate: Int): Long = 1L
        override fun sampleFrames(handle: Long): Long = 0L
        override fun sampleRelease(handle: Long) = Unit
        override fun exportCreate(): Long = 7L
        override fun exportRender(
            target: Long, path: String, frames: Long, tailFrames: Long, fadeOutFrames: Long, autoTail: Boolean, floatWav: Boolean,
            sampleRate: Int, bits: Int,
        ): ExportResult = ExportResult.Ok
        override fun exportRenderFloat(target: Long, frames: Long, tailFrames: Long): FloatArray = FloatArray(0)
        override fun exportDestroy(target: Long) = Unit
        fun speaker(): List<FloatArray> = posts.filter { it[1] == 210f }
    }

    @After
    fun tearDown() {
        NativeEngine.sink = null
    }

    @Test
    fun onlyThePhoneSpeakerGetsTheSpeakerKick() {
        assertTrue(Headphones.speakerKick(Listening.Speaker))
        assertFalse(Headphones.speakerKick(Listening.Wired))
        assertFalse(Headphones.speakerKick(Listening.Bluetooth))
    }

    @Test
    fun theRouteReachesTheLiveEngine() {
        val sink = Posts()
        NativeEngine.sink = sink
        assertEquals(Listening.Speaker, Headphones.route(listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)))
        assertEquals(Listening.Bluetooth, Headphones.route(listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)))
        assertEquals(Listening.Wired, Headphones.route(listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_WIRED_HEADPHONES)))
        assertEquals(Listening.Speaker, Headphones.route(listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)))
        val sent = sink.speaker()
        assertEquals(listOf(1f, 0f, 0f, 1f), sent.map { it[2] })
        // The live engine (target 0), never an offline export.
        assertTrue(sent.all { it[0] == 0f })
        assertEquals(Listening.Speaker, Headphones.state.value)
    }
}
