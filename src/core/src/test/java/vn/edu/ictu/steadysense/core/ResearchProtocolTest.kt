package vn.edu.ictu.steadysense.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ResearchProtocolTest {
    @Test fun configRoundTrip() {
        val value = ResearchConfig("S1", "P001", "NORMAL_WEAR", "RIGHT", "1.0", 10, 60f)
        assertEquals(value, ResearchConfigCodec.decode(ResearchConfigCodec.encode(value)))
    }

    @Test fun controlRoundTrip() {
        val value = ResearchControl("S1", ResearchCommand.MARK, 123L, "cycle")
        assertEquals(value, ResearchControlCodec.decode(ResearchControlCodec.encode(value)))
    }
}
