package vn.edu.ictu.steadysense.phone.transport

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors
import vn.edu.ictu.steadysense.core.ImuPayloadCodec
import vn.edu.ictu.steadysense.core.TransportAck
import vn.edu.ictu.steadysense.core.TransportAckCodec
import vn.edu.ictu.steadysense.core.TransportEnvelopeCodec
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.core.ResearchControlCodec
import vn.edu.ictu.steadysense.phone.data.ResearchEventEntity
import vn.edu.ictu.steadysense.phone.data.DeviceSnapshotEntity
import vn.edu.ictu.steadysense.phone.BuildConfig
import vn.edu.ictu.steadysense.phone.data.ImuWindowEntity
import vn.edu.ictu.steadysense.phone.data.PhoneDatabase

object PhoneTransferState {
    var storedWindows by mutableIntStateOf(0)
        private set

    fun publishStoredCount(value: Int) {
        Handler(Looper.getMainLooper()).post { storedWindows = value }
    }
}

class PhoneMessageService : WearableListenerService() {
    private val database by lazy { PhoneDatabase.get(this) }
    private val io = Executors.newSingleThreadExecutor()

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == TransportPaths.RESEARCH_EVENT) {
            val marker = runCatching { ResearchControlCodec.decode(event.data) }.getOrNull() ?: return
            io.execute {
                database.researchDao().insertEvent(ResearchEventEntity(marker.sessionId,
                    marker.timestampEpochNanos, when {
                        marker.value.startsWith("CLOCK_ACK") -> "CLOCK_ACK"
                        marker.value == "WEAR_BUTTON" -> "REP"
                        else -> "WEAR_MARK"
                    }, marker.value))
                if (marker.value.startsWith("CLOCK_ACK")) {
                    val fields = marker.value.split(';').drop(1).mapNotNull {
                        val parts = it.split('=', limit = 2); if (parts.size == 2) parts[0] to parts[1] else null
                    }.toMap()
                    database.researchDao().insertDeviceSnapshot(DeviceSnapshotEntity(marker.sessionId,
                        fields["manufacturer"] ?: "UNKNOWN", fields["model"] ?: "UNKNOWN",
                        fields["android"] ?: "UNKNOWN", "accel+gyro@20Hz",
                        fields["app"] ?: BuildConfig.VERSION_NAME))
                }
            }
            return
        }
        if (event.path != TransportPaths.IMU_WINDOW) return
        val envelope = runCatching { TransportEnvelopeCodec.decode(event.data) }
            .getOrElse {
                Log.e(TAG, "Rejected malformed envelope", it)
                return
            }
        val window = runCatching { ImuPayloadCodec.decode(envelope.payload) }
            .getOrElse {
                Log.e(TAG, "Rejected malformed IMU payload", it)
                return
            }

        io.execute {
            val dao = database.imuWindowDao()
            val rowId = dao.insert(
                ImuWindowEntity(
                    sessionId = envelope.sessionId,
                    sequenceId = envelope.sequenceId,
                    capturedAtEpochNanos = envelope.capturedAtEpochNanos,
                    receivedAtEpochMillis = System.currentTimeMillis(),
                    frameCount = window.frames.size,
                    payload = envelope.payload,
                ),
            )
            PhoneTransferState.publishStoredCount(dao.count())
            val ack = TransportAckCodec.encode(TransportAck(envelope.sessionId, envelope.sequenceId))
            Wearable.getMessageClient(this)
                .sendMessage(event.sourceNodeId, TransportPaths.ACK, ack)
                .addOnSuccessListener {
                    Log.i(TAG, "ACK sent session=${envelope.sessionId} sequence=${envelope.sequenceId} inserted=${rowId != -1L}")
                }
                .addOnFailureListener { error -> Log.e(TAG, "ACK send failed", error) }
        }
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SteadySensePhone"
    }
}
