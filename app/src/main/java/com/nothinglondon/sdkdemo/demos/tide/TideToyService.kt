package com.nothinglondon.sdkdemo.demos.tide

import android.content.Context
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothinglondon.sdkdemo.demos.GlyphMatrixService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TideToyService : GlyphMatrixService("Tide") {

    override val deviceTarget: String = Glyph.DEVICE_25111p

    private val scope = CoroutineScope(Dispatchers.IO)

    private val matrixLength: Int
        get() = runCatching { Common.getDeviceMatrixLength() }.getOrDefault(0).let { if (it <= 0) FALLBACK_LENGTH else it }

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        renderNow(glyphMatrixManager)
        // Smooth slow sweep while actively viewed. Slow on purpose: no flashing.
        scope.launch {
            while (isActive) {
                delay(SWEEP_INTERVAL_MILLIS)
                renderNow(glyphMatrixManager)
            }
        }
    }

    // AOD: one tick per minute. Redraw the current level on each tick.
    override fun onAodUpdate() {
        glyphMatrixManager?.let { renderNow(it) }
    }

    override fun performOnServiceDisconnected(context: Context) {
        scope.cancel()
    }

    private fun renderNow(gmm: GlyphMatrixManager) {
        val frame = TideEngine.renderFrame(TideEngine.level(now()), matrixLength)
        scope.launch { withContext(Dispatchers.Main) { gmm.setMatrixFrame(frame) } }
    }

    private fun now(): Long = System.currentTimeMillis()

    private companion object {
        private const val FALLBACK_LENGTH = 13
        private const val SWEEP_INTERVAL_MILLIS = 1000L
    }
}
