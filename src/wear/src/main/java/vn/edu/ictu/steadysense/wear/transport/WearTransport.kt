package vn.edu.ictu.steadysense.wear.transport

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import vn.edu.ictu.steadysense.core.TransportAckCodec
import vn.edu.ictu.steadysense.core.TransportEnvelope
import vn.edu.ictu.steadysense.core.TransportEnvelopeCodec
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.wear.data.OutboxEntity
import vn.edu.ictu.steadysense.wear.data.WearDatabase

data class TransferSnapshot(val pending: Int = 0, val acknowledged: Int = 0)

object WearTransferState {
    var snapshot by mutableStateOf(TransferSnapshot())
        private set

    fun update(pending: Int, acknowledgedDelta: Int = 0) {
        Handler(Looper.getMainLooper()).post {
            snapshot = snapshot.copy(
                pending = pending,
                acknowledged = snapshot.acknowledged + acknowledgedDelta,
            )
        }
    }
}

class WearOutboxStore(context: Context) {
    private val dao = WearDatabase.get(context).outboxDao()

    fun enqueue(envelope: TransportEnvelope): Boolean = dao.insert(
        OutboxEntity(
            sessionId = envelope.sessionId,
            sequenceId = envelope.sequenceId,
            createdAtEpochMillis = System.currentTimeMillis(),
            encodedEnvelope = TransportEnvelopeCodec.encode(envelope),
        ),
    ) != -1L

    fun acknowledge(sessionId: String, sequenceId: Long): Boolean =
        dao.acknowledge(sessionId, sequenceId) > 0

    fun pending(limit: Int = 4): List<TransportEnvelope> = dao.pending(limit)
        .mapNotNull { entry ->
            runCatching { TransportEnvelopeCodec.decode(entry.encodedEnvelope) }.getOrNull()
        }

    fun count(): Int = dao.count()
}

object WearSender {
    fun enqueueAndSend(context: Context, envelope: TransportEnvelope) {
        val appContext = context.applicationContext
        io.execute {
            val store = WearOutboxStore(appContext)
            if (!store.enqueue(envelope)) {
                Log.w(TAG, "Duplicate envelope session=${envelope.sessionId} sequence=${envelope.sequenceId}")
                return@execute
            }
            WearTransferState.update(store.count())
            sendPending(appContext, store)
        }
    }

    fun retryPending(context: Context) {
        val appContext = context.applicationContext
        io.execute {
            val store = WearOutboxStore(appContext)
            WearTransferState.update(store.count())
            sendPending(appContext, store)
        }
    }

    fun acknowledgeAndRetry(context: Context, sessionId: String, sequenceId: Long) {
        val appContext = context.applicationContext
        io.execute {
            val store = WearOutboxStore(appContext)
            val removed = store.acknowledge(sessionId, sequenceId)
            if (removed) retryDelaySeconds = 2L
            WearTransferState.update(store.count(), if (removed) 1 else 0)
            Log.i(TAG, "ACK received session=$sessionId sequence=$sequenceId removed=$removed")
            sendPending(appContext, store)
        }
    }

    private fun sendPending(appContext: Context, store: WearOutboxStore) {
        val pending = store.pending()
        if (pending.isEmpty()) return
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull() ?: return@addOnSuccessListener scheduleRetry(appContext)
                pending.forEach { envelope ->
                    Wearable.getMessageClient(appContext)
                        .sendMessage(node.id, TransportPaths.IMU_WINDOW, TransportEnvelopeCodec.encode(envelope))
                        .addOnSuccessListener {
                            Log.i(TAG, "Envelope delivered; awaiting ACK session=${envelope.sessionId} sequence=${envelope.sequenceId}")
                        }
                        .addOnFailureListener { error ->
                            Log.e(TAG, "Envelope delivery failed", error)
                            scheduleRetry(appContext)
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Node discovery failed", error)
                scheduleRetry(appContext)
            }
    }

    private fun scheduleRetry(appContext: Context) {
        if (!retryScheduled.compareAndSet(false, true)) return
        val delay = retryDelaySeconds
        retryDelaySeconds = (retryDelaySeconds * 2).coerceAtMost(60)
        scheduler.schedule({
            retryScheduled.set(false)
            retryPending(appContext)
        }, delay, TimeUnit.SECONDS)
    }

    private val io = Executors.newSingleThreadExecutor()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val retryScheduled = AtomicBoolean(false)
    @Volatile private var retryDelaySeconds = 2L
    private const val TAG = "SteadySenseWear"
}

class WearAckService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != TransportPaths.ACK) return
        val ack = runCatching { TransportAckCodec.decode(event.data) }
            .getOrElse {
                Log.e(TAG, "Rejected malformed ACK", it)
                return
            }
        WearSender.acknowledgeAndRetry(this, ack.sessionId, ack.sequenceId)
    }

    companion object {
        private const val TAG = "SteadySenseWear"
    }
}
