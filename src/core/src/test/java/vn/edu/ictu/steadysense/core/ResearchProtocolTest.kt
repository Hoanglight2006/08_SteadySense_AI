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
