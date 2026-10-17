package io.github.nikoapps.clock.shake

import io.github.nikoapps.clock.settings.ShakeSensitivity
import kotlin.math.sqrt

/**
 * Decides when accelerometer samples add up to a deliberate shake.
 *
 * Gravity is removed with a low-pass filter, leaving the phone's own movement. A "peak" is a
 * burst of movement above the threshold in one direction. A shake is a chain of peaks that
 * keep reversing direction quickly: back and forth, several times, within about a second.
 *
 * What this rules out:
 * - Walking: steps reverse about every 250 ms, slower than [Thresholds.maxGapMs], and in a
 *   pocket they rarely reach the threshold.
 * - A bump or a phone put down hard: one or two peaks, not enough to make a chain.
 * - Impact ringing: reversals closer than [Thresholds.minGapMs] don't count.
 *
 * After a shake, [Thresholds.cooldownMs] passes before another can fire, so one shake speaks once.
 * Pure Kotlin, so it can be tested on the JVM with made-up signals.
 */
class ShakeDetector(sensitivity: ShakeSensitivity = ShakeSensitivity.Medium) {

    /** Tuning for one sensitivity. Accelerations in m/s², without gravity. */
    data class Thresholds(
        val minAcceleration: Float,
        val minPeaks: Int,
        val minGapMs: Long = 50,
        val maxGapMs: Long = 220,
        val windowMs: Long = 1_000,
        val cooldownMs: Long = 3_000,
    )

    var sensitivity: ShakeSensitivity = sensitivity
        set(value) {
            if (field != value) {
                field = value
                thresholds = thresholdsFor(value)
                chain = 0
            }
        }

    private var thresholds = thresholdsFor(sensitivity)

    private var lastTimeNs = Long.MIN_VALUE
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f

    /** Direction of the latest peak in the chain, as a unit vector. */
    private var peakX = 0f
    private var peakY = 0f
    private var peakZ = 0f
    private var lastPeakNs = 0L
    private var firstPeakNs = 0L
    private var chain = 0

    private var lastFireNs = Long.MIN_VALUE

    /**
     * Feeds one accelerometer sample: [x], [y], [z] in m/s² including gravity, and
     * [timestampNs] on a monotonic clock. Returns true once for each deliberate shake.
     */
    fun onSample(x: Float, y: Float, z: Float, timestampNs: Long): Boolean {
        val dtNs = timestampNs - lastTimeNs
        if (lastTimeNs == Long.MIN_VALUE || dtNs <= 0 || dtNs > GAP_RESET_NS) {
            // First sample, or events stopped for a while: start again from rest.
            gx = x; gy = y; gz = z
            lastTimeNs = timestampNs
            chain = 0
            return false
        }
        lastTimeNs = timestampNs

        // Low-pass filter tracks gravity whatever the sample rate; the rest is movement.
        val dt = dtNs / 1e9f
        val alpha = GRAVITY_TIME_CONSTANT_S / (GRAVITY_TIME_CONSTANT_S + dt)
        gx = alpha * gx + (1 - alpha) * x
        gy = alpha * gy + (1 - alpha) * y
        gz = alpha * gz + (1 - alpha) * z
        val lx = x - gx
        val ly = y - gy
        val lz = z - gz

        if (lastFireNs != Long.MIN_VALUE && timestampNs - lastFireNs < thresholds.cooldownMs * MS) {
            return false
        }

        val magnitude = sqrt(lx * lx + ly * ly + lz * lz)
        if (magnitude < thresholds.minAcceleration) return false

        val ux = lx / magnitude
        val uy = ly / magnitude
        val uz = lz / magnitude

        if (chain == 0) {
            startChain(ux, uy, uz, timestampNs)
            return false
        }

        // Gaps are measured between the starts of bursts. For a back-and-forth movement that
        // is half its period whatever the strength, so a hard step is still a slow step.
        val sinceLastPeak = timestampNs - lastPeakNs
        val reversed = ux * peakX + uy * peakY + uz * peakZ < 0
        when {
            !reversed -> Unit // same burst, or the same direction again
            sinceLastPeak > thresholds.maxGapMs * MS -> startChain(ux, uy, uz, timestampNs)
            sinceLastPeak < thresholds.minGapMs * MS -> Unit // ringing from an impact
            else -> {
                chain++
                peakX = ux; peakY = uy; peakZ = uz
                lastPeakNs = timestampNs
                if (timestampNs - firstPeakNs > thresholds.windowMs * MS) {
                    startChain(ux, uy, uz, timestampNs)
                } else if (chain >= thresholds.minPeaks) {
                    lastFireNs = timestampNs
                    chain = 0
                    return true
                }
            }
        }
        return false
    }

    /** Forgets any movement in progress, for example when the sensor is registered again. */
    fun reset() {
        lastTimeNs = Long.MIN_VALUE
        chain = 0
    }

    private fun startChain(ux: Float, uy: Float, uz: Float, timestampNs: Long) {
        peakX = ux; peakY = uy; peakZ = uz
        firstPeakNs = timestampNs
        lastPeakNs = timestampNs
        chain = 1
    }

    companion object {
        private const val MS = 1_000_000L
        private const val GAP_RESET_NS = 1_000L * MS
        private const val GRAVITY_TIME_CONSTANT_S = 0.25f

        /**
         * High fires on a gentle shake, Low needs a firm one. Medium is above what a phone
         * in a pocket feels when walking. To be tuned with the first tester.
         */
        fun thresholdsFor(sensitivity: ShakeSensitivity): Thresholds = when (sensitivity) {
            ShakeSensitivity.High -> Thresholds(minAcceleration = 10f, minPeaks = 3)
            ShakeSensitivity.Medium -> Thresholds(minAcceleration = 13f, minPeaks = 4)
            ShakeSensitivity.Low -> Thresholds(minAcceleration = 17f, minPeaks = 4)
        }
    }
}
