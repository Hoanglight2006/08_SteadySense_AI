package vn.edu.ictu.steadysense.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object TransportPaths {
    const val IMU_WINDOW = "/steadysense/imu-window/v1"
    const val ACK = "/steadysense/ack/v1"
    const val RESEARCH_CONFIG = "/steadysense/research-config/v1"
    const val RESEARCH_CONTROL = "/steadysense/research-control/v1"
    const val RESEARCH_EVENT = "/steadysense/research-event/v1"
}

data class SensorVector(
    val timestampNanos: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)

data class ImuFrame(
    val timestampEpochNanos: Long,
    val accelX: Float,
    val accelY: Float,
    val accelZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
)

data class ImuWindow(val frames: List<ImuFrame>)

/**
 * Ghép mẫu theo timestamp cảm biến và resample trên nhịp mục tiêu.
 * Timestamp đầu vào dùng cùng timebase elapsed realtime của Android.
 */
class ImuWindowAssembler(
    private val epochOffsetNanos: Long,
    private val targetIntervalNanos: Long = 50_000_000L,
    private val maxSensorSkewNanos: Long = 30_000_000L,
    private val windowSize: Int = 40,
) {
    init {
        require(targetIntervalNanos > 0)
        require(maxSensorSkewNanos >= 0)
        require(windowSize > 0)
    }

    private val accelerometerQueue = ArrayDeque<SensorVector>()
    private val gyroscopeQueue = ArrayDeque<SensorVector>()
    private var lastAcceptedTimestamp = Long.MIN_VALUE
    private val frames = ArrayList<ImuFrame>(windowSize)
    private val minStepNanos = (targetIntervalNanos * 0.70).toLong()

    fun onGyroscope(sample: SensorVector): ImuWindow? {
        gyroscopeQueue.addLast(sample)
        trim(gyroscopeQueue)
        return drainOnePair()
    }

    fun onAccelerometer(sample: SensorVector): ImuWindow? {
        accelerometerQueue.addLast(sample)
        trim(accelerometerQueue)
        return drainOnePair()
    }

    private fun drainOnePair(): ImuWindow? {
        while (accelerometerQueue.isNotEmpty() && gyroscopeQueue.isNotEmpty()) {
            val accel = accelerometerQueue.first()
            val gyro = gyroscopeQueue.first()
            val skew = accel.timestampNanos - gyro.timestampNanos
            if (kotlin.math.abs(skew) > maxSensorSkewNanos) {
                if (skew < 0) accelerometerQueue.removeFirst() else gyroscopeQueue.removeFirst()
                continue
            }
            accelerometerQueue.removeFirst()
            gyroscopeQueue.removeFirst()
            if (lastAcceptedTimestamp != Long.MIN_VALUE &&
                accel.timestampNanos - lastAcceptedTimestamp < minStepNanos
            ) continue

            lastAcceptedTimestamp = accel.timestampNanos
            frames += ImuFrame(
                timestampEpochNanos = epochOffsetNanos + accel.timestampNanos,
                accelX = accel.x,
                accelY = accel.y,
                accelZ = accel.z,
                gyroX = gyro.x,
                gyroY = gyro.y,
                gyroZ = gyro.z,
            )
            if (frames.size == windowSize) {
                return ImuWindow(frames.toList()).also { frames.clear() }
            }
        }
        return null
    }

    private fun trim(queue: ArrayDeque<SensorVector>) {
        while (queue.size > MAX_BUFFERED_SAMPLES) queue.removeFirst()
    }

    companion object {
        private const val MAX_BUFFERED_SAMPLES = 256
    }
}

object ImuPayloadCodec {
    private const val MAGIC = 0x5353494D // SSIM
    private const val VERSION = 1
    private const val MAX_FRAMES = 1_000

    fun encode(window: ImuWindow): ByteArray {
        require(window.frames.isNotEmpty() && window.frames.size <= MAX_FRAMES)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(VERSION)
                output.writeInt(window.frames.size)
                window.frames.forEach { frame ->
                    output.writeLong(frame.timestampEpochNanos)
                    output.writeFloat(frame.accelX)
                    output.writeFloat(frame.accelY)
                    output.writeFloat(frame.accelZ)
                    output.writeFloat(frame.gyroX)
                    output.writeFloat(frame.gyroY)
                    output.writeFloat(frame.gyroZ)
                }
            }
            bytes.toByteArray()
        }
    }

    fun decode(payload: ByteArray): ImuWindow = DataInputStream(ByteArrayInputStream(payload)).use { input ->
        require(input.readInt() == MAGIC) { "Invalid IMU payload magic" }
        require(input.readInt() == VERSION) { "Unsupported IMU payload version" }
        val count = input.readInt()
        require(count in 1..MAX_FRAMES) { "Invalid IMU frame count" }
        ImuWindow(
            List(count) {
                ImuFrame(
                    timestampEpochNanos = input.readLong(),
                    accelX = input.readFloat(),
                    accelY = input.readFloat(),
                    accelZ = input.readFloat(),
                    gyroX = input.readFloat(),
                    gyroY = input.readFloat(),
                    gyroZ = input.readFloat(),
                )
            },
        )
    }
}

object TransportEnvelopeCodec {
    private const val MAGIC = 0x5353454E // SSEN
    private const val VERSION = 1
    private const val MAX_PAYLOAD_BYTES = 90_000

    fun encode(envelope: TransportEnvelope): ByteArray {
        require(envelope.payload.size <= MAX_PAYLOAD_BYTES)
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(MAGIC)
                output.writeInt(VERSION)
                output.writeUTF(envelope.sessionId)
                output.writeLong(envelope.sequenceId)
                output.writeLong(envelope.capturedAtEpochNanos)
                output.writeInt(envelope.payload.size)
                output.write(envelope.payload)
            }
            bytes.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): TransportEnvelope = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == MAGIC) { "Invalid envelope magic" }
        require(input.readInt() == VERSION) { "Unsupported envelope version" }
        val sessionId = input.readUTF()
        val sequenceId = input.readLong()
        val capturedAt = input.readLong()
        val size = input.readInt()
        require(size in 0..MAX_PAYLOAD_BYTES && size <= input.available()) { "Invalid envelope payload size" }
        TransportEnvelope(sessionId, sequenceId, capturedAt, ByteArray(size).also(input::readFully))
    }
}

data class TransportAck(val sessionId: String, val sequenceId: Long)

object TransportAckCodec {
    fun encode(ack: TransportAck): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeUTF(ack.sessionId)
            output.writeLong(ack.sequenceId)
        }
        bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): TransportAck = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        TransportAck(input.readUTF(), input.readLong())
    }
}
