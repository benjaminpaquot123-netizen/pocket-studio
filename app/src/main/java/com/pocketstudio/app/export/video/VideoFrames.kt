package com.pocketstudio.app.export.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import com.pocketstudio.model.i18n.tr
import com.pocketstudio.model.video.VideoLevels
import com.pocketstudio.model.video.VideoPlan
import com.pocketstudio.ui.Palette
import com.pocketstudio.ui.Palettes
import kotlin.math.max
import kotlin.math.min

/** The video's colours (ARGB), from the app's palette: the same orange, green and greys as on screen. */
data class VideoColors(
    val top: Int, val bottom: Int, val surface: Int, val accent: Int, val playing: Int, val text: Int, val textSecondary: Int,
) {
    companion object {
        fun of(p: Palette = Palettes.Studio) = VideoColors(
            top = p.surface.toArgb(), bottom = 0xFF14161A.toInt(), surface = p.surfaceHigh.toArgb(), accent = p.accent.toArgb(),
            playing = p.playing.toArgb(), text = p.textPrimary.toArgb(), textSecondary = p.textSecondary.toArgb(),
        )
    }
}

/**
 * Draws the images of the « vidéo 9:16 » (720 × 1280): the app's name and the song's title on
 * top, a waveform that scrolls under a playhead with a glow that breathes with the level, the
 * part playing in big letters (« Drop »), the parts of the 30 seconds as a strip with the
 * playhead, the time, and « Fait avec BenStudio ». The still parts are drawn once.
 * [bold] and [regular]: Inter when the app's fonts load, the system's otherwise.
 */
class VideoFrameRenderer(
    private val plan: VideoPlan,
    private val levels: VideoLevels.Levels,
    private val colors: VideoColors = VideoColors.of(),
    private val bold: Typeface = Typeface.DEFAULT_BOLD,
    private val regular: Typeface = Typeface.DEFAULT,
    val width: Int = VideoPlan.WIDTH,
    val height: Int = VideoPlan.HEIGHT,
) {
    /** Everything is laid out for 720 px across, then scaled. */
    private val k = width / 720f
    private val still: Bitmap by lazy { drawStill() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    /** Image [i] on [canvas] (at least [width] × [height]). */
    fun draw(canvas: Canvas, i: Int) {
        val t = i.toDouble() / plan.fps
        canvas.drawBitmap(still, 0f, 0f, null)
        val level = shown(levels.rms, i)
        glow(canvas, level)
        waveform(canvas, i)
        partName(canvas, t)
        strip(canvas, t)
    }

    /** A new image of frame [i] (previews and tests). */
    fun frame(i: Int): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { draw(Canvas(it), i) }

    private fun shown(values: FloatArray, i: Int): Float = if (values.isEmpty()) 0f else VideoLevels.shown(values[i.coerceIn(0, values.lastIndex)])

    private fun drawStill(): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors.top, colors.bottom, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
        p.shader = null
        // The app's sign: three level bars in orange, then its name.
        val name = "POCKET STUDIO"
        text.typeface = bold
        text.textSize = 30f * k
        text.letterSpacing = 0.18f
        text.color = colors.accent
        val nameWidth = text.measureText(name)
        val markWidth = 34f * k
        val x0 = (width - nameWidth - markWidth - 14f * k) / 2f
        p.color = colors.accent
        listOf(0.55f, 1f, 0.75f).forEachIndexed { j, h ->
            val bx = x0 + j * 12f * k
            rect.set(bx, 128f * k - 34f * k * h, bx + 8f * k, 128f * k)
            c.drawRoundRect(rect, 3f * k, 3f * k, p)
        }
        c.drawText(name, x0 + markWidth + 14f * k, 128f * k, text)
        text.letterSpacing = 0f
        // The title, on two lines at most.
        text.typeface = bold
        text.color = colors.text
        text.textSize = 64f * k
        val title = plan.title.ifBlank { tr("Mon morceau") }
        val layout = StaticLayout.Builder.obtain(title, 0, title.length, text, (width - 96f * k).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build()
        c.save()
        c.translate(48f * k, 190f * k)
        layout.draw(c)
        c.restore()
        // The foot.
        text.typeface = regular
        text.textSize = 26f * k
        text.color = colors.textSecondary
        val foot = tr("Fait avec BenStudio")
        c.drawText(foot, (width - text.measureText(foot)) / 2f, height - 64f * k, text)
        return bmp
    }

    private fun glow(canvas: Canvas, level: Float) {
        val cx = width / 2f
        val cy = WAVE_Y * k
        val r = (260f + 200f * level) * k
        val alpha = (40 + 120 * level).toInt().coerceIn(0, 255)
        paint.shader = RadialGradient(cx, cy, r, (colors.accent and 0x00FFFFFF) or (alpha shl 24), colors.accent and 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null
    }

    /** [BARS] bars, two images each: the past on the left (orange), the playhead in the middle, what comes on the right (faint). */
    private fun waveform(canvas: Canvas, i: Int) {
        val barW = 9f * k
        val gap = 4.5f * k
        val total = BARS * barW + (BARS - 1) * gap
        val x0 = (width - total) / 2f
        val cy = WAVE_Y * k
        val mid = BARS / 2
        for (b in 0 until BARS) {
            val f = i + (b - mid) * 2
            val inside = f >= 0 && f < levels.rms.size
            val v = if (inside) 0.65f * shown(levels.rms, f) + 0.35f * shown(levels.peak, f) else 0f
            val h = (10f + 300f * v) * k
            paint.color = when {
                !inside -> colors.surface
                b < mid -> colors.accent
                b == mid -> colors.playing
                else -> (colors.textSecondary and 0x00FFFFFF) or (0x55 shl 24)
            }
            val x = x0 + b * (barW + gap)
            rect.set(x, cy - h / 2f, x + barW, cy + h / 2f)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, paint)
        }
    }

    private fun partName(canvas: Canvas, t: Double) {
        val s = plan.sectionAt(t)
        val section = plan.sections.getOrNull(s) ?: return
        // A new part comes in: it fades in and rises a little over a fifth of a second.
        val since = (t - section.start).toFloat()
        val inT = if (s == 0 && section.start <= 0.0) 1f else min(1f, max(0f, since / 0.2f))
        text.typeface = bold
        text.textSize = 96f * k
        text.color = colors.text
        text.alpha = (255 * inT).toInt()
        val label = TextUtils.ellipsize(section.label, text, width - 80f * k, TextUtils.TruncateAt.END).toString()
        canvas.drawText(label, (width - text.measureText(label)) / 2f, (1010f + 24f * (1f - inT)) * k, text)
        text.alpha = 255
        // The next part, in its last three seconds.
        val next = plan.sections.getOrNull(s + 1)
        if (next != null && section.end - t < 3.0) {
            text.typeface = regular
            text.textSize = 30f * k
            text.color = colors.textSecondary
            val then = tr("Ensuite : %s", next.label)
            canvas.drawText(then, (width - text.measureText(then)) / 2f, 1062f * k, text)
        }
    }

    private fun strip(canvas: Canvas, t: Double) {
        val left = 60f * k
        val right = width - 60f * k
        val top = 1110f * k
        val h = 34f * k
        val seconds = plan.seconds.coerceAtLeast(1e-6)
        val current = plan.sectionAt(t)
        plan.sections.forEachIndexed { j, s ->
            val a = left + ((s.start / seconds) * (right - left)).toFloat()
            val b = left + ((s.end / seconds) * (right - left)).toFloat()
            paint.color = if (j == current) colors.accent else colors.surface
            rect.set(a + 2f * k, top, b - 2f * k, top + h)
            canvas.drawRoundRect(rect, 8f * k, 8f * k, paint)
            text.typeface = bold
            text.textSize = 20f * k
            text.color = if (j == current) 0xFF1C1E22.toInt() else colors.textSecondary
            val room = b - a - 12f * k
            if (room > 24f * k) {
                val label = TextUtils.ellipsize(s.label, text, room, TextUtils.TruncateAt.END).toString()
                canvas.drawText(label, a + 8f * k, top + h / 2f + 7f * k, text)
            }
        }
        // The playhead.
        val x = left + ((t / seconds).coerceIn(0.0, 1.0) * (right - left)).toFloat()
        paint.color = colors.text
        rect.set(x - 2f * k, top - 8f * k, x + 2f * k, top + h + 8f * k)
        canvas.drawRoundRect(rect, 2f * k, 2f * k, paint)
        text.typeface = regular
        text.textSize = 24f * k
        text.color = colors.textSecondary
        canvas.drawText(clock(t), left, top + h + 40f * k, text)
        val end = clock(plan.seconds)
        canvas.drawText(end, right - text.measureText(end), top + h + 40f * k, text)
    }

    companion object {
        // Odd: the playhead bar sits in the middle of the picture.
        const val BARS = 47
        const val WAVE_Y = 640f

        fun clock(seconds: Double): String {
            val s = seconds.toInt().coerceAtLeast(0)
            return "%d:%02d".format(s / 60, s % 60)
        }

        /** Inter from the app's fonts (the system's when they cannot be read). */
        fun fonts(context: android.content.Context): Pair<Typeface, Typeface> {
            fun load(id: Int, fallback: Typeface) = runCatching { androidx.core.content.res.ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback
            return load(com.pocketstudio.ui.R.font.inter_bold, Typeface.DEFAULT_BOLD) to load(com.pocketstudio.ui.R.font.inter_medium, Typeface.DEFAULT)
        }
    }
}
