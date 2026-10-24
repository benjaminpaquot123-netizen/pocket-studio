package com.pocketstudio.app.export.video

/**
 * One picture as H.264 encoders take it: Y at full size, U and V at half size each way (4:2:0),
 * BT.601 limited range (16..235), the default of every phone's encoder and player.
 */
class Yuv420(val width: Int, val height: Int) {
    val y = ByteArray(width * height)
    val u = ByteArray((width / 2) * (height / 2))
    val v = ByteArray((width / 2) * (height / 2))

    /** Fills the planes from [argb] (Android's Bitmap pixels, row by row); each chroma sample is the average of its 2×2 pixels. */
    fun fill(argb: IntArray) {
        val w = width
        for (row in 0 until height) {
            val base = row * w
            for (col in 0 until w) {
                val c = argb[base + col]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                y[base + col] = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).toByte()
            }
        }
        val cw = w / 2
        for (row in 0 until height / 2) {
            val top = 2 * row * w
            val bottom = top + w
            for (col in 0 until cw) {
                val x = 2 * col
                val p0 = argb[top + x]; val p1 = argb[top + x + 1]; val p2 = argb[bottom + x]; val p3 = argb[bottom + x + 1]
                val r = (((p0 shr 16) and 0xFF) + ((p1 shr 16) and 0xFF) + ((p2 shr 16) and 0xFF) + ((p3 shr 16) and 0xFF) + 2) shr 2
                val g = (((p0 shr 8) and 0xFF) + ((p1 shr 8) and 0xFF) + ((p2 shr 8) and 0xFF) + ((p3 shr 8) and 0xFF) + 2) shr 2
                val b = ((p0 and 0xFF) + (p1 and 0xFF) + (p2 and 0xFF) + (p3 and 0xFF) + 2) shr 2
                u[row * cw + col] = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).toByte()
                v[row * cw + col] = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).toByte()
            }
        }
    }

    /** I420 (Y, then all of U, then all of V) into [out] from its position: what a planar encoder buffer holds. */
    fun putPlanar(out: java.nio.ByteBuffer) {
        out.put(y); out.put(u); out.put(v)
    }

    /** NV12 (Y, then U and V interleaved) into [out] from its position: what a semi-planar encoder buffer holds. */
    fun putSemiPlanar(out: java.nio.ByteBuffer) {
        out.put(y)
        val uv = ByteArray(u.size * 2)
        for (i in u.indices) { uv[2 * i] = u[i]; uv[2 * i + 1] = v[i] }
        out.put(uv)
    }

    /**
     * Into an encoder's [android.media.Image] whatever its layout (planar or interleaved, padded
     * rows): each plane written with its own row and pixel strides.
     */
    fun putImage(image: android.media.Image) {
        val planes = image.planes
        writePlane(planes[0], y, width, height)
        writePlane(planes[1], u, width / 2, height / 2)
        writePlane(planes[2], v, width / 2, height / 2)
    }

    private fun writePlane(plane: android.media.Image.Plane, data: ByteArray, w: Int, h: Int) {
        val buf = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        if (pixelStride == 1) {
            for (row in 0 until h) {
                buf.position(row * rowStride)
                buf.put(data, row * w, w)
            }
        } else {
            for (row in 0 until h) {
                val base = row * rowStride
                for (col in 0 until w) buf.put(base + col * pixelStride, data[row * w + col])
            }
        }
    }

    companion object {
        /** The Y value [argb] gives (tests, and a check on the picture). */
        fun lumaOf(argb: Int): Int {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr 8) and 0xFF
            val b = argb and 0xFF
            return ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
        }
    }
}
