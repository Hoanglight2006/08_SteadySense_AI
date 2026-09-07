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

/**
 * State machine thuần Kotlin cho hàng đợi watch → phone.
 *
 * Bản production sẽ lưu các entry bằng Room trên đồng hồ. Lớp này cố ý tách
 * quyết định ACK/dedup khỏi API Android để có thể unit test trước khi nối
 * Wearable Data Layer.
 */
class ReliableOutbox {
    private val pending = linkedMapOf<Pair<String, Long>, TransportEnvelope>()
    private val acknowledged = mutableSetOf<Pair<String, Long>>()

    fun enqueue(envelope: TransportEnvelope): Boolean {
        val key = envelope.sessionId to envelope.sequenceId
        if (key in acknowledged || key in pending) return false
        pending[key] = envelope
        return true
    }

    fun acknowledge(sessionId: String, sequenceId: Long): Boolean {
        val key = sessionId to sequenceId
        val removed = pending.remove(key) ?: return false
        acknowledged += removed.sessionId to removed.sequenceId
        return true
    }

    fun pendingBatch(limit: Int): List<TransportEnvelope> =
        pending.values.take(limit.coerceAtLeast(0))

    fun pendingCount(): Int = pending.size
}
