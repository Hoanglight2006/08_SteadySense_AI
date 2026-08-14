package vn.edu.uit.tpkd.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class OrientationCorrectorTest {

    @Test
    fun `leaves an already-aligned gravity vector unchanged`() {
        val corrector = OrientationCorrector(timeConstantSeconds = 1.0)
        var lastAcc = floatArrayOf(0f, 0f, 0f)
        for (i in 0..200) {
            val ts = i * 20_000_000L // 20 ms steps
            val (acc, _) = corrector.correct(ts, floatArrayOf(0f, 9.8f, 0f), floatArrayOf(1f, 2f, 3f))
            lastAcc = acc
        }
        assertEquals(0f, lastAcc[0], 0.05f)
        assertEquals(9.8f, lastAcc[1], 0.05f)
        assertEquals(0f, lastAcc[2], 0.05f)
    }

    @Test
    fun `rotates a sideways-worn watch so gravity converges onto +Y`() {
        // Gravity reads on the X axis: as if the watch face were rotated 90 degrees
        // relative to the training-time convention. The rotation that fixes this
        // pivots around Z, so use a gyro reading NOT aligned with Z (which would be
        // invariant under that specific rotation and prove nothing).
        val corrector = OrientationCorrector(timeConstantSeconds = 1.0)
        var lastAcc = floatArrayOf(0f, 0f, 0f)
        var lastGyro = floatArrayOf(0f, 0f, 0f)
        for (i in 0..500) {
            val ts = i * 20_000_000L
            val (acc, gyro) = corrector.correct(ts, floatArrayOf(9.8f, 0f, 0f), floatArrayOf(1f, 0f, 0f))
            lastAcc = acc
            lastGyro = gyro
        }
        // After the filter settles (well beyond a few time constants), corrected
        // acceleration should point along +Y with the same magnitude as gravity.
        assertEquals(0f, lastAcc[0], 0.1f)
        assertEquals(9.8f, lastAcc[1], 0.1f)
        assertEquals(0f, lastAcc[2], 0.1f)
        // The gyro reading must be rotated by the SAME matrix, not left untouched:
        // raw [1,0,0] should end up along +Y too (same rotation as gravity above).
        val gyroMag = sqrt(lastGyro[0] * lastGyro[0] + lastGyro[1] * lastGyro[1] + lastGyro[2] * lastGyro[2])
        assertEquals(1f, gyroMag, 0.05f)
        assertTrue("gyro should have been rotated away from its raw [1,0,0] axis",
            kotlin.math.abs(lastGyro[0] - 1f) > 0.5f)
        assertEquals(1f, lastGyro[1], 0.1f)
    }

    @Test
    fun `rotation matrix maps current gravity exactly onto the target`() {
        val gCurrent = floatArrayOf(3f, 4f, 0f) // arbitrary, |g|=5
        val gTarget = floatArrayOf(0f, 9.8f, 0f)
        val r = OrientationCorrector.rotationMatrixToTarget(gCurrent, gTarget)
        val rotated = OrientationCorrector.applyRotation(gCurrent, r)
        val targetDir = floatArrayOf(0f, 1f, 0f)
        val rotatedNorm = sqrt(rotated[0] * rotated[0] + rotated[1] * rotated[1] + rotated[2] * rotated[2])
        assertEquals(targetDir[0], rotated[0] / rotatedNorm, 1e-3f)
        assertEquals(targetDir[1], rotated[1] / rotatedNorm, 1e-3f)
        assertEquals(targetDir[2], rotated[2] / rotatedNorm, 1e-3f)
    }

    @Test
    fun `handles the anti-parallel case without producing NaNs`() {
        val gCurrent = floatArrayOf(0f, -9.8f, 0f)
        val gTarget = floatArrayOf(0f, 9.8f, 0f)
        val r = OrientationCorrector.rotationMatrixToTarget(gCurrent, gTarget)
        val rotated = OrientationCorrector.applyRotation(gCurrent, r)
        assertTrue(rotated.all { !it.isNaN() })
        assertEquals(0f, rotated[0], 0.05f)
        assertEquals(9.8f, rotated[1], 0.05f)
        assertEquals(0f, rotated[2], 0.05f)
    }
}

