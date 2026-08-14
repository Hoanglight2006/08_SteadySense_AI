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
