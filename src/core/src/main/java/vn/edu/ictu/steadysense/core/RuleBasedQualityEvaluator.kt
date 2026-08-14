package vn.edu.ictu.steadysense.core

import kotlin.math.round

/**
 * Baseline phục vụ kiểm thử phần mềm. Các trọng số/ngưỡng chưa được xác nhận
 * cho người sau đột quỵ và không được diễn giải như một quyết định lâm sàng.
 */
class RuleBasedQualityEvaluator(
    private val reliableThreshold: Float = 0.72f,
) {
    fun evaluate(input: SensorWindowQuality): QualityDecision {
        if (input.sampleCoverage < 0.75f) {
            return QualityDecision(input.sampleCoverage, false, "Thiếu mẫu cảm biến")
        }
        if (input.clippingRatio > 0.08f) {
            return QualityDecision(1f - input.clippingRatio, false, "Tín hiệu bị bão hòa")
        }

        val score = (
            input.sampleCoverage * 0.30f +
                input.timingStability * 0.25f +
                input.motionEnergy.coerceIn(0f, 1f) * 0.15f +
                (1f - input.clippingRatio).coerceIn(0f, 1f) * 0.10f +
                input.sensorAgreement * 0.20f
            ).coerceIn(0f, 1f)
        val normalized = round(score * 1000f) / 1000f
        return if (normalized >= reliableThreshold) {
            QualityDecision(normalized, true, "Tín hiệu ổn định")
        } else {
            QualityDecision(normalized, false, "Hãy chỉnh lại đồng hồ")
        }
    }
}
