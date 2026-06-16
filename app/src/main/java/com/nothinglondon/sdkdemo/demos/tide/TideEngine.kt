package com.nothinglondon.sdkdemo.demos.tide

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

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

    // Returns tide level in 0f..1f. 1f = high tide, 0f = low tide.
    fun level(epochMillis: Long, amplitude: Float = 1f): Float {
        val phase = 2.0 * PI * (epochMillis - REFERENCE_HIGH_TIDE_UTC_MILLIS).toDouble() / M2_PERIOD_MILLIS
        return (0.5 + 0.5 * amplitude * cos(phase)).toFloat().coerceIn(0f, 1f)
    }

    // Renders a circular waterline into a row-major IntArray of n*n brightness
    // values (0..255). Always draws a dim limb ring so the disk is never blank.
    fun renderFrame(level: Float, n: Int): IntArray {
        val out = IntArray(n * n)
        val center = (n - 1) / 2.0
        val radius = n / 2.0 - 0.4
        val rimInner = radius - 1.0
        val fillTopY = (n - 1) - level * (n - 1)
        for (y in 0 until n) {
            for (x in 0 until n) {
                val dx = x - center
                val dy = y - center
                val dist2 = dx * dx + dy * dy
                if (dist2 > radius * radius) continue
                val i = y * n + x
                out[i] = when {
                    y >= fillTopY -> 255
                    abs(y - fillTopY) < 0.6 -> 180
                    dist2 >= rimInner * rimInner -> 90
                    else -> 0
                }
            }
        }
        return out
    }
}
