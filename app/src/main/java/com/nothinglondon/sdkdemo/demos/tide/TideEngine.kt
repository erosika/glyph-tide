package com.nothinglondon.sdkdemo.demos.tide

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

// Semidiurnal tide model. The PERIOD is real (principal lunar M2 = 12h25m);
// absolute phase + amplitude must be calibrated to a NOAA station before the
// level can be trusted as a true reading. Until then the motion is correct,
// the offset is nominal. Calibration hook: REFERENCE_HIGH_TIDE_UTC_MILLIS.
object TideEngine {

    private const val M2_PERIOD_MILLIS = 44712_000L // 12.4206 h

    // Calibrated to NYC Harbor high tide 2026-06-15 21:14 EDT (01:14 UTC 06-16).
    // M2-only model drifts slowly and ignores spring/neap amplitude; good enough
    // for a glyph toy, recalibrate or wire NOAA per-station for true precision.
    private const val REFERENCE_HIGH_TIDE_UTC_MILLIS = 1781572440000L

    private const val WATER_BASE = 95     // dim fill, shows the true level
    private const val CREST_EDGE = 140    // slight emphasis on the top water line
    private const val RIM = 90            // always-on disk outline
    private const val BLOOM_PEAK = 160.0  // brightness of the gliding bloom band
    private const val BLOOM_SIGMA = 1.4   // band softness (px)
    private const val CYCLE_MILLIS = 6000.0 // one bloom pass through the water

    // Returns tide level in 0f..1f. 1f = high tide, 0f = low tide.
    fun level(epochMillis: Long, amplitude: Float = 1f): Float {
        val phase = 2.0 * PI * (epochMillis - REFERENCE_HIGH_TIDE_UTC_MILLIS).toDouble() / M2_PERIOD_MILLIS
        return (0.5 + 0.5 * amplitude * cos(phase)).toFloat().coerceIn(0f, 1f)
    }

    // True while the tide is flooding (level increasing).
    fun isRising(epochMillis: Long): Boolean {
        val phase = 2.0 * PI * (epochMillis - REFERENCE_HIGH_TIDE_UTC_MILLIS).toDouble() / M2_PERIOD_MILLIS
        return sin(phase) < 0 // d/dt cos = -sin
    }

    // Renders the tide into a row-major IntArray of n*n brightness values (0..255).
    // The disk fills to the true level (dim water, so height = how high the tide is),
    // and a soft bloom band glides through the water in the tide's direction — UP toward
    // the fill point while rising, DOWN while ebbing — looping seamlessly. Slow + soft on
    // purpose (~0.16 Hz, gaussian edges): legible motion, no strobe. Rim is always lit.
    fun renderFrame(epochMillis: Long, n: Int): IntArray {
        val out = IntArray(n * n)
        val center = (n - 1) / 2.0
        val radius = n / 2.0 - 0.4
        val rimInner = radius - 1.0

        val level = level(epochMillis)
        val rising = isRising(epochMillis)
        val crestRow = Math.round((n - 1) - level * (n - 1)).toInt().coerceIn(0, n - 1)

        // Bloom band travels a touch beyond both ends so it fades out before wrapping.
        val cyclePos = (epochMillis % CYCLE_MILLIS.toLong()) / CYCLE_MILLIS
        val top = crestRow - 1.0
        val bot = (n - 1) + 1.0
        val bandY = if (rising) bot - cyclePos * (bot - top) else top + cyclePos * (bot - top)

        for (y in 0 until n) {
            for (x in 0 until n) {
                val dx = x - center
                val dy = y - center
                val dist2 = dx * dx + dy * dy
                if (dist2 > radius * radius) continue
                val i = y * n + x
                if (y < crestRow) {
                    out[i] = if (dist2 >= rimInner * rimInner) RIM else 0
                } else {
                    val base = if (y == crestRow) CREST_EDGE else WATER_BASE
                    val d = y - bandY
                    val bloom = BLOOM_PEAK * exp(-(d * d) / (2 * BLOOM_SIGMA * BLOOM_SIGMA))
                    out[i] = (base + bloom).toInt().coerceAtMost(255)
                }
            }
        }
        return out
    }
}
