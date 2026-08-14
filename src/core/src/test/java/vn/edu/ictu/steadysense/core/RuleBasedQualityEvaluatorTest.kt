package vn.edu.ictu.steadysense.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedQualityEvaluatorTest {
    private val evaluator = RuleBasedQualityEvaluator()

    @Test
    fun reliableWindow_isAccepted() {
        val result = evaluator.evaluate(
            SensorWindowQuality(0.98f, 0.95f, 0.75f, 0.01f, 0.90f),
        )
        assertTrue(result.reliable)
        assertEquals("Tín hiệu ổn định", result.reason)
    }

    @Test
    fun missingSamples_areRejectedWithReason() {
        val result = evaluator.evaluate(
            SensorWindowQuality(0.60f, 0.95f, 0.80f, 0f, 0.90f),
        )
        assertFalse(result.reliable)
        assertEquals("Thiếu mẫu cảm biến", result.reason)
    }

    @Test
    fun clipping_isRejectedIndependently() {
        val result = evaluator.evaluate(
            SensorWindowQuality(1f, 1f, 1f, 0.20f, 1f),
        )
        assertFalse(result.reliable)
        assertEquals("Tín hiệu bị bão hòa", result.reason)
    }
}
