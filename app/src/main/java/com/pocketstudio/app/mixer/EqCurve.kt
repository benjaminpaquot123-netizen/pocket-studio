package com.pocketstudio.app.mixer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.pocketstudio.model.EffectType
import com.pocketstudio.model.Insert
import com.pocketstudio.ui.Neon
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** The same filters as the engine's Biquad (Effects.cpp), to draw what an EQ does. */
internal object EqMath {
    private const val RATE = 48000.0

    private class Coefs(val b0: Double, val b1: Double, val b2: Double, val a0: Double, val a1: Double, val a2: Double) {
        /** Gain in dB at [hz]. */
        fun db(hz: Double): Double {
            val w = 2 * PI * hz / RATE
            val c1 = cos(w); val s1 = sin(w); val c2 = cos(2 * w); val s2 = sin(2 * w)
            val nr = b0 + b1 * c1 + b2 * c2; val ni = -(b1 * s1 + b2 * s2)
            val dr = a0 + a1 * c1 + a2 * c2; val di = -(a1 * s1 + a2 * s2)
            return 10 * log10((nr * nr + ni * ni) / (dr * dr + di * di))
        }
    }

    private fun lowShelf(f: Double, g: Double): Coefs {
        val a = 10.0.pow(g / 40); val w = 2 * PI * f / RATE; val cs = cos(w); val sq = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        return Coefs(a * ((a + 1) - (a - 1) * cs + sq), 2 * a * ((a - 1) - (a + 1) * cs), a * ((a + 1) - (a - 1) * cs - sq),
            (a + 1) + (a - 1) * cs + sq, -2 * ((a - 1) + (a + 1) * cs), (a + 1) + (a - 1) * cs - sq)
    }

    private fun highShelf(f: Double, g: Double): Coefs {
        val a = 10.0.pow(g / 40); val w = 2 * PI * f / RATE; val cs = cos(w); val sq = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        return Coefs(a * ((a + 1) + (a - 1) * cs + sq), -2 * a * ((a - 1) + (a + 1) * cs), a * ((a + 1) + (a - 1) * cs - sq),
            (a + 1) - (a - 1) * cs + sq, 2 * ((a - 1) - (a + 1) * cs), (a + 1) - (a - 1) * cs - sq)
    }

    private fun peak(f: Double, q: Double, g: Double): Coefs {
        val a = 10.0.pow(g / 40); val w = 2 * PI * f / RATE; val cs = cos(w); val alpha = sin(w) / (2 * q)
        return Coefs(1 + alpha * a, -2 * cs, 1 - alpha * a, 1 + alpha / a, -2 * cs, 1 - alpha / a)
    }

    private fun highPass(f: Double): Coefs {
        val w = 2 * PI * f / RATE; val cs = cos(w); val alpha = sin(w) / (2 * 0.7071)
        return Coefs((1 + cs) / 2, -(1 + cs), (1 + cs) / 2, 1 + alpha, -2 * cs, 1 - alpha)
    }

    private fun expMap(v: Float, lo: Double, hi: Double) = lo * (hi / lo).pow(v.toDouble().coerceIn(0.0, 1.0))

    /** The insert's total gain in dB at [hz] (EQ or parametric EQ; 0 for anything else). */
    fun responseDb(insert: Insert, hz: Double): Double {
        val p = insert.params
        return when (insert.effect) {
            EffectType.EQ -> lowShelf(100.0, (p[0] - 0.5) * 24).db(hz) + peak(1000.0, 0.9, (p[1] - 0.5) * 24).db(hz) +
                highShelf(8000.0, (p[2] - 0.5) * 24).db(hz)
            EffectType.PARAM_EQ -> peak(minOf(expMap(p[0], 20.0, 20000.0), RATE * 0.45), expMap(p[2], 0.3, 10.0), (p[1] - 0.5) * 36).db(hz) +
                if (p[3] < 0.01f) 0.0 else highPass(expMap(p[3], 20.0, 500.0)).db(hz)
            else -> 0.0
        }
    }
}

/** What the EQ does, drawn from 20 Hz to 20 kHz (±18 dB). */
@Composable
internal fun EqCurve(insert: Insert) {
    val line = Neon.Cyan
    val grid = Neon.Outline.copy(alpha = 0.5f)
    Canvas(Modifier.fillMaxWidth().height(96.dp).testTag("eq_curve")) {
        val range = 18.0
        fun y(db: Double) = (size.height / 2 - db.coerceIn(-range, range) / range * size.height / 2).toFloat()
        // 100 Hz, 1 kHz and 10 kHz lines, and 0 dB.
        listOf(100.0, 1000.0, 10000.0).forEach { f ->
            val x = (ln(f / 20) / ln(1000.0) * size.width).toFloat()
            drawLine(grid, Offset(x, 0f), Offset(x, size.height))
        }
        drawLine(grid, Offset(0f, y(0.0)), Offset(size.width, y(0.0)))
        val path = Path()
        val points = 160
        for (i in 0..points) {
            val f = 20.0 * 1000.0.pow(i.toDouble() / points)
            val x = size.width * i / points
            val yy = y(EqMath.responseDb(insert, f))
            if (i == 0) path.moveTo(x, yy) else path.lineTo(x, yy)
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
    }
}
