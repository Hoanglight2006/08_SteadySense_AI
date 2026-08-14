package vn.edu.ictu.steadysense.core

import java.util.UUID

enum class ExerciseCode {
    ELBOW_FLEX_EXTEND,
    FOREARM_ROTATION,
    TABLE_SLIDE,
}

enum class PlanStatus { DRAFT, PENDING_APPROVAL, ACTIVE, PAUSED, COMPLETED, SUPERSEDED }

enum class SessionStatus {
    COMPLETED_RELIABLE,
    PARTIALLY_COMPLETED,
    NOT_COMPLETED,
    INSUFFICIENT_SIGNAL,
    USER_REPORTED,
}

data class ExerciseDefinition(
    val code: ExerciseCode,
    val name: String,
    val instruction: String,
    val targetRepetitions: Int,
    val reviewedBySpecialist: Boolean = false,
)

data class ExercisePlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val exercises: List<ExerciseDefinition>,
    val reminderHour: Int,
    val reminderMinute: Int,
    val status: PlanStatus = PlanStatus.PENDING_APPROVAL,
    val version: Int = 1,
)

data class SensorWindowQuality(
    val sampleCoverage: Float,
    val timingStability: Float,
    val motionEnergy: Float,
    val clippingRatio: Float,
    val sensorAgreement: Float,
)

data class QualityDecision(
    val score: Float,
    val reliable: Boolean,
    val reason: String,
)

data class TransportEnvelope(
    val sessionId: String,
    val sequenceId: Long,
    val capturedAtEpochNanos: Long,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        other is TransportEnvelope &&
            sessionId == other.sessionId &&
            sequenceId == other.sequenceId &&
            capturedAtEpochNanos == other.capturedAtEpochNanos &&
            payload.contentEquals(other.payload)

    override fun hashCode(): Int =
        31 * (31 * (31 * sessionId.hashCode() + sequenceId.hashCode()) +
            capturedAtEpochNanos.hashCode()) + payload.contentHashCode()
}
