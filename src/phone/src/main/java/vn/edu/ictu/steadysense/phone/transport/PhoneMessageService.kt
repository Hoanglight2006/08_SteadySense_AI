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

package vn.edu.ictu.steadysense.phone.transport

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
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
    var watchBatteryPercent by mutableIntStateOf(-1)
        private set
    var isWatchConnected by mutableStateOf(false)
        private set
    var watchDeviceName by mutableStateOf("Chưa tìm thấy thiết bị")
        private set
    var isCheckingConnection by mutableStateOf(false)
        private set
    var lastSeenEpochMillis by mutableLongStateOf(0L)
        private set

    @Volatile
    private var pingAckCompleter: CompletableDeferred<Int>? = null

    fun publishStoredCount(value: Int) {
        Handler(Looper.getMainLooper()).post { storedWindows = value }
    }

    fun publishWatchBattery(value: Int) {
        Handler(Looper.getMainLooper()).post {
            watchBatteryPercent = value
            if (value >= 0) {
                isWatchConnected = true
                lastSeenEpochMillis = System.currentTimeMillis()
            }
        }
        pingAckCompleter?.complete(value)
    }

    fun setConnected(connected: Boolean) {
        Handler(Looper.getMainLooper()).post {
            isWatchConnected = connected
            if (connected) {
                lastSeenEpochMillis = System.currentTimeMillis()
            } else {
                watchBatteryPercent = -1
                watchDeviceName = "Chưa tìm thấy thiết bị"
            }
        }
    }

    fun onPeerConnected(node: Node) {
        Handler(Looper.getMainLooper()).post {
            if (node.isNearby) {
                watchDeviceName = node.displayName
            }
        }
    }

    fun onPeerDisconnected(node: Node) {
        Handler(Looper.getMainLooper()).post {
            isWatchConnected = false
            watchBatteryPercent = -1
            watchDeviceName = "Chưa tìm thấy thiết bị"
        }
        pingAckCompleter?.complete(-1)
    }

    suspend fun performPingPongCheck(context: Context, timeoutMs: Long = 1800L): Boolean {
        Handler(Looper.getMainLooper()).post { isCheckingConnection = true }
        val deferred = CompletableDeferred<Int>()
        pingAckCompleter = deferred

        return try {
            val nodesTask = Wearable.getNodeClient(context).connectedNodes
            val nodes = suspendCancellableCoroutine<List<Node>> { cont ->
                nodesTask.addOnSuccessListener { cont.resume(it) }
                nodesTask.addOnFailureListener { cont.resumeWithException(it) }
                nodesTask.addOnCanceledListener { cont.cancel() }
            }

            // Chỉ chấp nhận node kết nối trực tiếp tầm gần (Bluetooth)
            val nearbyNodes = nodes.filter { it.isNearby }
            if (nearbyNodes.isEmpty()) {
                Handler(Looper.getMainLooper()).post {
                    isWatchConnected = false
                    watchBatteryPercent = -1
                    watchDeviceName = if (nodes.isEmpty()) {
                        "Chưa tìm thấy thiết bị"
                    } else {
                        "Chỉ có Wi-Fi (Cần Bluetooth)"
                    }
                    isCheckingConnection = false
                }
                return false
            }

            val targetNode = nearbyNodes.first()
            Handler(Looper.getMainLooper()).post {
                watchDeviceName = targetNode.displayName
            }

            // Gửi PING tới đồng hồ
            Wearable.getMessageClient(context)
                .sendMessage(targetNode.id, TransportPaths.PING, ByteArray(0))

            // Đợi đồng hồ thực sự phản hồi WATCH_STATUS trong timeoutMs (Bắt tay 2 chiều)
            val batteryResult = withTimeoutOrNull(timeoutMs) {
                deferred.await()
            }

            val success = batteryResult != null && batteryResult >= 0
            Handler(Looper.getMainLooper()).post {
                isWatchConnected = success
                if (success) {
                    watchBatteryPercent = batteryResult
                    lastSeenEpochMillis = System.currentTimeMillis()
                } else {
                    watchBatteryPercent = -1
                }
                isCheckingConnection = false
            }
            success
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                isWatchConnected = false
                watchBatteryPercent = -1
                isCheckingConnection = false
            }
            false
        } finally {
            if (pingAckCompleter === deferred) {
                pingAckCompleter = null
            }
        }
    }
}

class PhoneMessageService : WearableListenerService() {
    private val database by lazy { PhoneDatabase.get(this) }
    private val io = Executors.newSingleThreadExecutor()

    override fun onPeerConnected(peer: Node) {
        Log.i(TAG, "Peer connected: ${peer.displayName} (isNearby=${peer.isNearby})")
        PhoneTransferState.onPeerConnected(peer)
        if (peer.isNearby) {
            Wearable.getMessageClient(this).sendMessage(peer.id, TransportPaths.PING, ByteArray(0))
        }
    }

    override fun onPeerDisconnected(peer: Node) {
        Log.w(TAG, "Peer disconnected: ${peer.displayName}")
        PhoneTransferState.onPeerDisconnected(peer)
    }

    override fun onMessageReceived(event: MessageEvent) {
        PhoneTransferState.setConnected(true)
        if (event.path == TransportPaths.WATCH_STATUS) {
            val bat = String(event.data, Charsets.UTF_8).toIntOrNull() ?: -1
            PhoneTransferState.publishWatchBattery(bat)
            Log.i(TAG, "Received watch status battery=$bat%")
            return
        }
        if (event.path == TransportPaths.EXERCISE_SESSION) {
            val cmd = String(event.data, Charsets.UTF_8)
            Log.i(TAG, "Received EXERCISE_SESSION from watch: $cmd")
            if (cmd.startsWith("TOGGLE_PAUSE")) {
                ExerciseDataBridge.emitMassageControl("TOGGLE_PAUSE")
            }
            return
        }
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

        // Forward IMU window real-time cho màn hình tập luyện (nếu đang mở)
        ExerciseDataBridge.emit(window)

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
