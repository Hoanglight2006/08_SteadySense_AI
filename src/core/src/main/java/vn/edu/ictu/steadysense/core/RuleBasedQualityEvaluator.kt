/*
 * Copyright 2026 SteadySense AI Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
