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

package vn.edu.ictu.steadysense.wear.transport

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
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

object WearConnectionState {
    var isPhoneConnected by mutableStateOf(false)
        private set

    fun setConnected(connected: Boolean) {
        Handler(Looper.getMainLooper()).post {
            isPhoneConnected = connected
        }
    }

    fun updateFromNodes(nodes: List<Node>) {
        val hasNearby = nodes.any { it.isNearby }
        setConnected(hasNearby)
    }
}

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

    fun pending(limit: Int = 40): List<TransportEnvelope> = dao.pending(limit)
        .mapNotNull { entry ->
            runCatching { TransportEnvelopeCodec.decode(entry.encodedEnvelope) }.getOrNull()
        }

    fun count(): Int = dao.count()

    fun purgeStale(cutoffEpochMillis: Long): Int = dao.deleteOlderThan(cutoffEpochMillis)
}

object WearSender {
    // Lưu thời điểm gửi (ms) để phát hiện timeout khi ACK bị rớt.
    // Nếu sau IN_FLIGHT_TIMEOUT_MS không có ACK, gỡ khỏi inFlight để gửi lại.
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private const val IN_FLIGHT_TIMEOUT_MS = 5_000L

    private fun key(sessionId: String, sequenceId: Long): String = "$sessionId:$sequenceId"

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
        retryScheduled.set(false)
        retryDelaySeconds = 1L
        io.execute {
            inFlight.clear()
            val store = WearOutboxStore(appContext)
            WearTransferState.update(store.count())
            sendPending(appContext, store)
        }
    }

    fun acknowledgeAndRetry(context: Context, sessionId: String, sequenceId: Long) {
        val appContext = context.applicationContext
        io.execute {
            inFlight.remove(key(sessionId, sequenceId))
            val store = WearOutboxStore(appContext)
            val removed = store.acknowledge(sessionId, sequenceId)
            if (removed) retryDelaySeconds = 1L
            WearTransferState.update(store.count(), if (removed) 1 else 0)
            Log.i(TAG, "ACK received session=$sessionId sequence=$sequenceId removed=$removed")
            sendPending(appContext, store)
        }
    }

    private fun sendPending(appContext: Context, store: WearOutboxStore) {
        val now = System.currentTimeMillis()
        // Dọn dẹp các gói IMU cũ tồn đọng > 4 giây khi bị gián đoạn mạng
        store.purgeStale(now - 4_000L)

        // Giải phóng các gói đã timeout (ACK bị rớt, không thể về đồng hồ)
        inFlight.entries.removeIf { (_, sentAt) -> now - sentAt > IN_FLIGHT_TIMEOUT_MS }

        val candidates = store.pending(40)
        val toSend = candidates.filter { !inFlight.containsKey(it.key()) }
        if (toSend.isEmpty()) return
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                val node = nodes.firstOrNull { it.isNearby } ?: run {
                    inFlight.clear()
                    return@addOnSuccessListener scheduleRetry(appContext)
                }
                toSend.forEach { envelope ->
                    val k = envelope.key()
                    inFlight[k] = System.currentTimeMillis()
                    Wearable.getMessageClient(appContext)
                        .sendMessage(node.id, TransportPaths.IMU_WINDOW, TransportEnvelopeCodec.encode(envelope))
                        .addOnSuccessListener {
                            Log.i(TAG, "Envelope delivered; awaiting ACK session=${envelope.sessionId} sequence=${envelope.sequenceId}")
                        }
                        .addOnFailureListener { error ->
                            Log.e(TAG, "Envelope delivery failed", error)
                            inFlight.remove(k)
                            scheduleRetry(appContext)
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Node discovery failed", error)
                inFlight.clear()
                scheduleRetry(appContext)
            }
    }

    private fun TransportEnvelope.key() = "${sessionId}:${sequenceId}"

    private fun scheduleRetry(appContext: Context) {
        if (!retryScheduled.compareAndSet(false, true)) return
        val delay = retryDelaySeconds
        retryDelaySeconds = (retryDelaySeconds * 2).coerceAtMost(3L)
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

object WearExerciseAlertState {
    var warningMessage by mutableStateOf<String?>(null)
        private set
    var sessionActiveByPhone by mutableStateOf(false)
        private set
    /** Chỉ dẫn nhịp tập từ Phone ("GẬP TAY!", "ĐẠT CHUẨN ✓", ...) */
    var cueMessage by mutableStateOf<String?>(null)
        private set
    var isMassageMode by mutableStateOf(false)
        private set
    var massageMinutes by mutableStateOf(5)
        private set
    var massageSecondsRemaining by mutableIntStateOf(300)
        private set
    var isMassagePaused by mutableStateOf(false)
        private set

    fun setMassageMode(active: Boolean, minutes: Int = 5, secondsRemaining: Int = minutes * 60) {
        Handler(Looper.getMainLooper()).post {
            isMassageMode = active
            massageMinutes = minutes
            massageSecondsRemaining = if (secondsRemaining > 0) secondsRemaining else minutes * 60
            isMassagePaused = false
        }
    }

    fun pauseMassage(secondsRemaining: Int? = null) {
        Handler(Looper.getMainLooper()).post {
            isMassagePaused = true
            if (secondsRemaining != null && secondsRemaining > 0) {
                massageSecondsRemaining = secondsRemaining
            }
        }
    }

    fun resumeMassage(secondsRemaining: Int? = null) {
        Handler(Looper.getMainLooper()).post {
            isMassagePaused = false
            if (secondsRemaining != null && secondsRemaining > 0) {
                massageSecondsRemaining = secondsRemaining
            }
        }
    }

    fun syncMassage(secondsRemaining: Int) {
        Handler(Looper.getMainLooper()).post {
            if (secondsRemaining >= 0) {
                massageSecondsRemaining = secondsRemaining
            }
        }
    }

    fun tickMassageCountdown() {
        Handler(Looper.getMainLooper()).post {
            if (isMassageMode && !isMassagePaused && massageSecondsRemaining > 0) {
                massageSecondsRemaining--
            }
        }
    }

    fun triggerWarning(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            warningMessage = message
        }
        vibrateStrong(context)
    }

    fun triggerWarningWithoutVibrate(message: String) {
        Handler(Looper.getMainLooper()).post {
            warningMessage = message
        }
    }

    fun clearWarning() {
        Handler(Looper.getMainLooper()).post {
            warningMessage = null
        }
    }

    /** Ra hiệu nhịp tập — rung nhẹ 1 tick + hiện chỉ dẫn trên màn hình đồng hồ. */
    fun triggerCue(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            cueMessage = message
        }
        vibrateLight(context)
    }

    fun triggerCueWithoutVibrate(message: String) {
        Handler(Looper.getMainLooper()).post {
            cueMessage = message
        }
    }

    fun clearCue() {
        Handler(Looper.getMainLooper()).post {
            cueMessage = null
        }
    }

    fun setSessionActive(active: Boolean) {
        Handler(Looper.getMainLooper()).post {
            sessionActiveByPhone = active
            if (!active) {
                cueMessage = null
                isMassageMode = false
                isMassagePaused = false
            }
        }
    }

    /** Rung nhẹ 1 nhịp — báo hiệu gập tay. */
    fun vibrateLight(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(80)
        }
    }

    fun vibrateStrong(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        // 3 nhịp rung dồn dập rất mạnh: 400ms rung, 150ms nghỉ, 400ms rung, 150ms nghỉ, 500ms rung
        val timings = longArrayOf(0, 400, 150, 400, 150, 500)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (vibrator.hasAmplitudeControl()) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
}

class WearAckService : WearableListenerService() {
    override fun onPeerConnected(peer: Node) {
        Log.i(TAG, "Phone peer connected: ${peer.displayName} isNearby=${peer.isNearby}")
        if (peer.isNearby) {
            WearConnectionState.setConnected(true)
            WearSender.retryPending(this)
        }
    }

    override fun onPeerDisconnected(peer: Node) {
        Log.w(TAG, "Phone peer disconnected: ${peer.displayName}")
        WearConnectionState.setConnected(false)
    }

    override fun onMessageReceived(event: MessageEvent) {
        WearConnectionState.setConnected(true)
        if (event.path == TransportPaths.PING || event.path == TransportPaths.EXERCISE_SESSION) {
            WearSender.retryPending(this)
        }
        when (event.path) {
            TransportPaths.ACK -> {
                val ack = runCatching { TransportAckCodec.decode(event.data) }
                    .getOrElse {
                        Log.e(TAG, "Rejected malformed ACK", it)
                        return
                    }
                WearSender.acknowledgeAndRetry(this, ack.sessionId, ack.sequenceId)
            }
            TransportPaths.EXERCISE_WARNING -> {
                val raw = String(event.data, Charsets.UTF_8).ifBlank { "VUNG QUÁ NHANH!" }
                Log.w(TAG, "Received EXERCISE_WARNING: $raw")
                if (raw.startsWith("SILENT:")) {
                    WearExerciseAlertState.triggerWarningWithoutVibrate(raw.removePrefix("SILENT:"))
                } else {
                    WearExerciseAlertState.triggerWarning(this, raw)
                }
            }
            TransportPaths.PING -> {
                Log.i(TAG, "Received PING, sending watch status")
                sendWatchStatus(this)
            }
            TransportPaths.EXERCISE_SESSION -> {
                val cmd = String(event.data, Charsets.UTF_8)
                Log.i(TAG, "Received EXERCISE_SESSION: $cmd")
                when {
                    cmd.startsWith("START:MASSAGE") -> {
                        val mins = cmd.substringAfterLast(":", "5").toIntOrNull() ?: 5
                        WearExerciseAlertState.setMassageMode(true, mins, mins * 60)
                        WearExerciseAlertState.setSessionActive(true)
                        ExerciseCollectionService.stop(this)
                    }
                    cmd.startsWith("PAUSE:MASSAGE") -> {
                        val sec = cmd.substringAfterLast(":", "-1").toIntOrNull()
                        WearExerciseAlertState.pauseMassage(sec)
                        WearExerciseAlertState.setSessionActive(true)
                    }
                    cmd.startsWith("RESUME:MASSAGE") -> {
                        val sec = cmd.substringAfterLast(":", "-1").toIntOrNull()
                        WearExerciseAlertState.resumeMassage(sec)
                        WearExerciseAlertState.setSessionActive(true)
                    }
                    cmd.startsWith("SYNC:MASSAGE") -> {
                        val sec = cmd.substringAfterLast(":", "-1").toIntOrNull()
                        if (sec != null && sec >= 0) {
                            WearExerciseAlertState.syncMassage(sec)
                        }
                    }
                    cmd.startsWith("START") -> {
                        WearExerciseAlertState.setMassageMode(false)
                        WearExerciseAlertState.setSessionActive(true)
                        ExerciseCollectionService.start(this)
                    }
                    else -> {
                        WearExerciseAlertState.setMassageMode(false)
                        WearExerciseAlertState.setSessionActive(false)
                        ExerciseCollectionService.stop(this)
                    }
                }
                sendWatchStatus(this)
            }
            TransportPaths.EXERCISE_CUE -> {
                val raw = String(event.data, Charsets.UTF_8)
                Log.i(TAG, "Received EXERCISE_CUE: $raw")
                if (raw == "TEST_VIBRATE") {
                    WearExerciseAlertState.vibrateLight(this)
                } else if (raw.startsWith("SILENT:")) {
                    WearExerciseAlertState.triggerCueWithoutVibrate(raw.removePrefix("SILENT:"))
                } else {
                    WearExerciseAlertState.triggerCue(this, raw)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SteadySenseWear"

        fun getBatteryPercentage(context: Context): Int {
            return try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                val capacity = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                if (capacity in 0..100) {
                    capacity
                } else {
                    val ifilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
                    val batteryStatus = context.registerReceiver(null, ifilter)
                    val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                    if (level >= 0 && scale > 0) {
                        (level * 100 / scale.toFloat()).toInt()
                    } else -1
                }
            } catch (e: Exception) {
                -1
            }
        }

        fun sendWatchStatus(context: Context) {
            val batLevel = getBatteryPercentage(context)
            val payload = "$batLevel".toByteArray(Charsets.UTF_8)
            runCatching {
                Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                    nodes.forEach { node ->
                        Wearable.getMessageClient(context).sendMessage(node.id, TransportPaths.WATCH_STATUS, payload)
                    }
                }
            }
        }
    }
}

