package io.github.nikoapps.clock.shake

import io.github.nikoapps.clock.settings.ShakeSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class ShakeDetectorTest {

    private val gravity = 9.81f

    /** One sample: time in ms and movement on each axis, gravity added on z. */
    private data class Sample(val ms: Double, val x: Double = 0.0, val y: Double = 0.0, val z: Double = 0.0)

    /** Phone lying still for [ms], then whatever [movement] adds, sampled at [hz]. */
    private fun signal(
        durationMs: Double,
        hz: Double = 50.0,
        startMs: Double = 0.0,
        movement: (Double) -> Sample,
    ): List<Sample> {
        val step = 1000.0 / hz
        return generateSequence(0.0) { it + step }
            .takeWhile { it < durationMs }
            .map { t -> movement(t).copy(ms = startMs + t) }
            .toList()
    }

    private fun still(durationMs: Double, startMs: Double = 0.0, hz: Double = 50.0) =
        signal(durationMs, hz, startMs) { Sample(it) }

    /** Side-to-side shake: [amplitude] m/s² at [frequency] Hz. */
    private fun shake(durationMs: Double, amplitude: Double, frequency: Double = 4.0, startMs: Double = 0.0, hz: Double = 50.0) =
        signal(durationMs, hz, startMs) { t -> Sample(t, x = amplitude * sin(2 * PI * frequency * t / 1000)) }

    /** Walking with the phone in a pocket: up and down once per step, plus a little sway. */
    private fun walking(durationMs: Double, amplitude: Double, stepsPerSecond: Double = 2.0, startMs: Double = 0.0) =
        signal(durationMs, startMs = startMs) { t ->
            val s = t / 1000
            Sample(
                t,
                x = amplitude * 0.3 * sin(2 * PI * stepsPerSecond / 2 * s),
                z = amplitude * sin(2 * PI * stepsPerSecond * s) + amplitude * 0.4 * sin(4 * PI * stepsPerSecond * s + 1),
            )
        }

    private fun fires(samples: List<Sample>, sensitivity: ShakeSensitivity = ShakeSensitivity.Medium): Int {
        val detector = ShakeDetector(sensitivity)
        return samples.count { s ->
            detector.onSample(s.x.toFloat(), s.y.toFloat(), (gravity + s.z).toFloat(), (s.ms * 1_000_000).toLong())
        }
    }

    @Test fun `a firm shake speaks once`() {
        val samples = still(500.0) + shake(1500.0, amplitude = 20.0, startMs = 500.0)
        assertEquals(1, fires(samples))
    }

    @Test fun `a shake is caught at a slower sample rate too`() {
        val samples = still(500.0, hz = 16.0) + shake(1500.0, amplitude = 20.0, startMs = 500.0, hz = 16.0)
        assertEquals(1, fires(samples))
    }

    @Test fun `a fast and a slower shake both count`() {
        assertEquals(1, fires(still(500.0) + shake(1200.0, amplitude = 20.0, frequency = 6.0, startMs = 500.0)))
        assertEquals(1, fires(still(500.0) + shake(1500.0, amplitude = 20.0, frequency = 3.0, startMs = 500.0)))
    }

    @Test fun `lying still never fires`() {
        for (sensitivity in ShakeSensitivity.entries) {
            assertEquals(0, fires(still(10_000.0), sensitivity))
        }
    }

    @Test fun `walking for a minute doesn't fire at medium`() {
        assertEquals(0, fires(walking(60_000.0, amplitude = 8.0)))
    }

    @Test fun `hard steps are still too slow to count as a shake`() {
        // Steps strong enough to cross every threshold, but at walking pace.
        for (sensitivity in ShakeSensitivity.entries) {
            assertEquals(sensitivity.name, 0, fires(walking(60_000.0, amplitude = 20.0), sensitivity))
        }
    }

    @Test fun `a single bump doesn't fire`() {
        // One push and the stop that follows it.
        val bump = signal(200.0, startMs = 500.0) { t -> Sample(t, x = 25 * sin(2 * PI * t / 200)) }
        val samples = still(500.0) + bump + still(1000.0, startMs = 700.0)
        for (sensitivity in ShakeSensitivity.entries) {
            assertEquals(sensitivity.name, 0, fires(samples, sensitivity))
        }
    }

    @Test fun `dropping the phone on a table doesn't fire`() {
        // Free fall, a hard knock, then ringing that dies away quickly.
        val fall = signal(300.0, startMs = 500.0) { t -> Sample(t, z = -gravity.toDouble()) }
        val knock = signal(300.0, startMs = 800.0) { t ->
            Sample(t, z = 60 * exp(-t / 30) * sin(2 * PI * 25 * t / 1000 + PI / 2))
        }
        val samples = still(500.0) + fall + knock + still(1000.0, startMs = 1100.0)
        for (sensitivity in ShakeSensitivity.entries) {
            assertEquals(sensitivity.name, 0, fires(samples, sensitivity))
        }
    }

    @Test fun `shaking on and on speaks again only after the cooldown`() {
        // 5 s of shaking: once at the start, once more after the 3 s cooldown.
        assertEquals(2, fires(still(500.0) + shake(5000.0, amplitude = 20.0, startMs = 500.0)))
    }

    @Test fun `a second shake after a pause speaks again`() {
        val samples = still(500.0) +
            shake(1000.0, amplitude = 20.0, startMs = 500.0) +
            still(3000.0, startMs = 1500.0) +
            shake(1000.0, amplitude = 20.0, startMs = 4500.0)
        assertEquals(2, fires(samples))
    }

    @Test fun `higher sensitivity needs a gentler shake`() {
        fun firesAt(amplitude: Double, sensitivity: ShakeSensitivity) =
            fires(still(500.0) + shake(1500.0, amplitude, startMs = 500.0), sensitivity) == 1

        // Gentle: only High hears it.
        assertTrue(firesAt(11.5, ShakeSensitivity.High))
        assertEquals(false, firesAt(11.5, ShakeSensitivity.Medium))
        // Moderate: High and Medium.
        assertTrue(firesAt(15.0, ShakeSensitivity.Medium))
        assertEquals(false, firesAt(15.0, ShakeSensitivity.Low))
        // Firm: all three.
        assertTrue(firesAt(20.0, ShakeSensitivity.Low))

        val thresholds = ShakeSensitivity.entries.map { ShakeDetector.thresholdsFor(it).minAcceleration }
        assertEquals(thresholds.sortedDescending(), thresholds)
    }
}
