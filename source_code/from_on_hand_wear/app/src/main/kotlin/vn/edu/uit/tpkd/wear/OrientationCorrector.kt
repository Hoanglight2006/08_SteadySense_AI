package vn.edu.uit.tpkd.wear

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Rotates each accelerometer/gyroscope sample so the device's gravity axis
 * lines up with training-time convention (+Y), without using any activity
 * label. Training-time preprocessing (`estimate_gravity_vector` /
 * `rotation_matrix_to_target` in preprocess/01_wisdm_6axis_win300.py)
 * estimated ONE gravity vector per subject from labeled sitting/standing
 * segments and rotated that subject's whole recording with it. On-device we
 * have no labels and no fixed per-subject calibration step, so instead this
 * estimates gravity continuously via a low-pass filter over the raw
 * accelerometer stream (gravity is the near-DC component; limb motion
 * during activities averages out over the filter's time constant) and
 * applies the identical Rodrigues rotation formula every sample. This is a
 * genuine domain-gap fix, not a cosmetic one: the deployed app previously
 * skipped this step entirely (see paper/AIMCS_2026 Limitations), so any
 * wrist-mounting angle that wasn't already close to the WISDM watch
 * convention fed the model rotated axes it never saw in training.
 */
class OrientationCorrector(
    private val timeConstantSeconds: Double = 1.5,
    private val targetGravity: FloatArray = floatArrayOf(0f, 9.8f, 0f)
) {
    private var gravityEstimate: FloatArray? = null
    private var lastTimestampNs: Long? = null

    fun reset() {
        gravityEstimate = null
        lastTimestampNs = null
    }

    /** Rotates one (acc, gyro) sample pair in place-equivalent (returns new arrays). */
    fun correct(timestampNs: Long, acc: FloatArray, gyro: FloatArray): Pair<FloatArray, FloatArray> {
        val g = updateGravityEstimate(timestampNs, acc)
        val r = rotationMatrixToTarget(g, targetGravity)
        return Pair(applyRotation(acc, r), applyRotation(gyro, r))
    }

    private fun updateGravityEstimate(timestampNs: Long, acc: FloatArray): FloatArray {
        val previous = gravityEstimate
        val previousTs = lastTimestampNs
        lastTimestampNs = timestampNs
        if (previous == null || previousTs == null) {
            val initial = acc.copyOf(3)
            gravityEstimate = initial
            return initial
        }
        val dtSeconds = ((timestampNs - previousTs).coerceAtLeast(0L)) / 1_000_000_000.0
        // Exponential moving average with a time-based alpha so the effective
        // cutoff frequency doesn't depend on the (device-variable) actual
        // sensor delivery rate. alpha -> 1 as dt grows large relative to tau.
        val alpha = (dtSeconds / (timeConstantSeconds + dtSeconds)).toFloat()
        val updated = FloatArray(3) { i -> previous[i] + alpha * (acc[i] - previous[i]) }
        gravityEstimate = updated
        return updated
    }

    companion object {
        private fun norm(v: FloatArray): Float = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])

        private fun normalize(v: FloatArray): FloatArray {
            val n = norm(v)
            if (n < 1e-6f) return floatArrayOf(0f, 1f, 0f)
            return FloatArray(3) { i -> v[i] / n }
        }

        private fun dot(a: FloatArray, b: FloatArray): Float = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

        private fun cross(a: FloatArray, b: FloatArray): FloatArray = floatArrayOf(
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        )

        /** Same construction as rotation_matrix_to_target() in the Python preprocessing script. */
        internal fun rotationMatrixToTarget(gCurrent: FloatArray, gTarget: FloatArray): Array<FloatArray> {
            val a = normalize(gCurrent)
            val b = normalize(gTarget)
            val d = dot(a, b).coerceIn(-1f, 1f)

            if (d > 0.9999f) return identity()

            val axis: FloatArray
            val angle: Double
            if (d < -0.9999f) {
                var perp = floatArrayOf(1f, 0f, 0f)
                if (abs(dot(a, perp)) > 0.9f) perp = floatArrayOf(0f, 1f, 0f)
                axis = normalize(cross(a, perp))
                angle = PI
            } else {
                axis = normalize(cross(a, b))
                angle = acos(d.toDouble())
            }
            return rodrigues(axis, angle)
        }

        private fun identity(): Array<FloatArray> = arrayOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )

        private fun rodrigues(axis: FloatArray, angle: Double): Array<FloatArray> {
            val s = sin(angle).toFloat()
            val c = cos(angle).toFloat()
            val oneMinusC = 1f - c
            val (x, y, z) = Triple(axis[0], axis[1], axis[2])
            // R = I + sin(angle) K + (1 - cos(angle)) K^2, K = skew-symmetric(axis)
            return arrayOf(
                floatArrayOf(
                    c + x * x * oneMinusC,
                    x * y * oneMinusC - z * s,
                    x * z * oneMinusC + y * s
                ),
                floatArrayOf(
                    y * x * oneMinusC + z * s,
                    c + y * y * oneMinusC,
                    y * z * oneMinusC - x * s
                ),
                floatArrayOf(
                    z * x * oneMinusC - y * s,
                    z * y * oneMinusC + x * s,
                    c + z * z * oneMinusC
                )
            )
        }

        internal fun applyRotation(v: FloatArray, r: Array<FloatArray>): FloatArray = FloatArray(3) { i ->
            r[i][0] * v[0] + r[i][1] * v[1] + r[i][2] * v[2]
        }
    }
}

