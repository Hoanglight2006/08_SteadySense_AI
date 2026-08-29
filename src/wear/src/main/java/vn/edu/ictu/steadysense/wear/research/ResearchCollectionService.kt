package vn.edu.ictu.steadysense.wear.research

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.Executors
import vn.edu.ictu.steadysense.core.ImuPayloadCodec
import vn.edu.ictu.steadysense.core.ImuWindowAssembler
import vn.edu.ictu.steadysense.core.ResearchCommand
import vn.edu.ictu.steadysense.core.ResearchConfigCodec
import vn.edu.ictu.steadysense.core.ResearchControl
import vn.edu.ictu.steadysense.core.ResearchControlCodec
import vn.edu.ictu.steadysense.core.SensorVector
import vn.edu.ictu.steadysense.core.TransportEnvelope
import vn.edu.ictu.steadysense.core.TransportPaths
import vn.edu.ictu.steadysense.wear.MainActivity
import vn.edu.ictu.steadysense.wear.BuildConfig
import vn.edu.ictu.steadysense.wear.data.ResearchSessionConfigEntity
import vn.edu.ictu.steadysense.wear.data.WearDatabase
import vn.edu.ictu.steadysense.wear.transport.WearSender

data class ResearchCollectionSnapshot(
    val active: Boolean = false,
    val sessionId: String? = null,
    val participantCode: String? = null,
    val samples: Int = 0,
    val windows: Int = 0,
    val markers: Int = 0,
)

object WearResearchState {
    var snapshot by mutableStateOf(ResearchCollectionSnapshot())
        internal set
}

class WearResearchMessageService : WearableListenerService() {
    private val io = Executors.newSingleThreadExecutor()

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            TransportPaths.RESEARCH_CONFIG -> {
                val config = runCatching { ResearchConfigCodec.decode(event.data) }.getOrNull() ?: return
                io.execute {
                    WearDatabase.get(this).researchSessionConfigDao().upsert(
                        ResearchSessionConfigEntity(config.sessionId, config.participantCode,
                            config.condition, config.wornSide, config.targetCycles, config.tempoBpm,
                            config.configVersion, System.currentTimeMillis())
                    )
                    Handler(Looper.getMainLooper()).post {
                        WearResearchState.snapshot = WearResearchState.snapshot.copy(
                            sessionId = config.sessionId, participantCode = config.participantCode)
                    }
                    val watchNow = System.currentTimeMillis() * 1_000_000L
                    val ack = ResearchControl(config.sessionId, ResearchCommand.MARK, watchNow,
                        "CLOCK_ACK;phoneSent=${config.phoneSentAtEpochNanos};offsetNanos=${watchNow-config.phoneSentAtEpochNanos}" +
                            ";manufacturer=${Build.MANUFACTURER};model=${Build.MODEL}" +
                            ";android=${Build.VERSION.RELEASE};app=${BuildConfig.VERSION_NAME}")
                    Wearable.getMessageClient(this).sendMessage(event.sourceNodeId,
                        TransportPaths.RESEARCH_EVENT, ResearchControlCodec.encode(ack))
                }
            }
            TransportPaths.RESEARCH_CONTROL -> {
                val control = runCatching { ResearchControlCodec.decode(event.data) }.getOrNull() ?: return
                when (control.command) {
                    ResearchCommand.START -> startCollection(this, control.sessionId)
                    ResearchCommand.STOP -> stopCollection(this)
                    ResearchCommand.MARK -> markCollection(this, "ECHO:${control.value}")
                }
            }
        }
    }

    override fun onDestroy() { io.shutdown(); super.onDestroy() }
}

class ResearchCollectionService : Service(), SensorEventListener {
    private lateinit var manager: SensorManager
    private var assembler: ImuWindowAssembler? = null
    private var sessionId = ""
    private var sequence = 0L
    private var samples = 0
    private var windows = 0
    private var markers = 0
    private var beatThread: Thread? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        manager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if (intent?.action == ACTION_MARK) { sendMarker(intent.getStringExtra(EXTRA_VALUE) ?: "MARK"); return START_STICKY }
        startForeground(NOTIFICATION_ID, notification("đang chuẩn bị"))
        val requestedSession = intent?.getStringExtra(EXTRA_SESSION_ID)
        io.execute {
            val config = WearDatabase.get(this).researchSessionConfigDao().latest()
            Handler(Looper.getMainLooper()).post {
                if (config == null || (requestedSession != null && requestedSession != config.sessionId)) {
                    stopSelf()
                } else beginCollection(config)
            }
        }
        return START_STICKY
    }

    private fun beginCollection(config: ResearchSessionConfigEntity) {
        sessionId = config.sessionId
        sequence = getSharedPreferences("research_sequence", MODE_PRIVATE).getLong(sessionId, 0L)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID,
            notification(config.participantCode))
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SteadySense:ResearchWakeLock").apply {
                setReferenceCounted(false)
                acquire(45 * 60 * 1000L)
            }
        }
        val offset = System.currentTimeMillis() * 1_000_000L - SystemClock.elapsedRealtimeNanos()
        assembler = ImuWindowAssembler(offset)
        manager.unregisterListener(this)
        manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
        manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME)
        beatThread?.interrupt()
        beatThread = null
        if (config.condition !in listOf("REST", "DAILY_ACTIVITY_DISTRACTOR") && config.targetCycles > 0) {
            startMetronome(config.tempoBpm)
        }
        WearResearchState.snapshot = ResearchCollectionSnapshot(true, sessionId, config.participantCode)
        // Xóa gói tồn đọng của các phiên cũ trên IO thread (Room không cho phép truy vấn DB
        // trên Main thread). Sau khi xóa mới gọi retryPending để đồng hồ chỉ gửi gói của
        // phiên hiện tại, tránh nghẽn HOL (Head-of-Line Blocking).
        io.execute {
            WearDatabase.get(this).outboxDao().deleteOtherSessions(sessionId)
            WearSender.retryPending(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        samples++
        val sample = SensorVector(event.timestamp, event.values[0], event.values[1], event.values[2])
        val window = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> assembler?.onAccelerometer(sample)
            Sensor.TYPE_GYROSCOPE -> assembler?.onGyroscope(sample)
            else -> null
        } ?: return publish()
        if (vn.edu.ictu.steadysense.wear.transport.WearTransferState.snapshot.pending >= MAX_PENDING_WINDOWS) {
            WearResearchState.snapshot = WearResearchState.snapshot.copy(active = false)
            stopSelf()
            return
        }
        sequence++; windows++
        getSharedPreferences("research_sequence", MODE_PRIVATE).edit().putLong(sessionId, sequence).apply()
        WearSender.enqueueAndSend(this, TransportEnvelope(sessionId, sequence,
            window.frames.first().timestampEpochNanos, ImuPayloadCodec.encode(window)))
        publish()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
        manager.unregisterListener(this)
        beatThread?.interrupt()
        io.shutdown()
        Handler(Looper.getMainLooper()).post {
            WearResearchState.snapshot = WearResearchState.snapshot.copy(active = false)
        }
        super.onDestroy()
    }

    private fun publish() {
        if (samples % 10 == 0 || windows > WearResearchState.snapshot.windows) {
            Handler(Looper.getMainLooper()).post {
                WearResearchState.snapshot = WearResearchState.snapshot.copy(
                    active = true, sessionId = sessionId, samples = samples, windows = windows, markers = markers)
            }
        }
    }

    private fun sendMarker(value: String) {
        if (sessionId.isBlank()) return
        markers++
        val control = ResearchControl(sessionId, ResearchCommand.MARK,
            System.currentTimeMillis() * 1_000_000L, value)
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.firstOrNull()?.let { node -> Wearable.getMessageClient(this)
                .sendMessage(node.id, TransportPaths.RESEARCH_EVENT, ResearchControlCodec.encode(control)) }
        }
        publish()
    }

    private fun startMetronome(tempoBpm: Float) {
        val period = (60_000f / tempoBpm.coerceIn(20f, 180f)).toLong().coerceAtLeast(300L)
        beatThread = Thread {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            try { while (!Thread.currentThread().isInterrupted) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                Thread.sleep(period)
            } } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }.also { it.name = "SteadySenseMetronome"; it.start() }
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Thu dữ liệu nghiên cứu", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notification(participant: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(vn.edu.ictu.steadysense.wear.R.drawable.ic_steadysense)
        .setContentTitle("SteadySense đang thu IMU")
        .setContentText("Mã ẩn danh: $participant")
        .setOngoing(true)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        .build()

    companion object {
        const val ACTION_START = "steadysense.research.START"
        const val ACTION_STOP = "steadysense.research.STOP"
        const val ACTION_MARK = "steadysense.research.MARK"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_VALUE = "value"
        private const val CHANNEL_ID = "research_collection"
        private const val NOTIFICATION_ID = 41
        private const val MAX_PENDING_WINDOWS = 10_000
    }
}

fun startCollection(context: Context, sessionId: String) {
    ContextCompat.startForegroundService(context, Intent(context, ResearchCollectionService::class.java)
        .setAction(ResearchCollectionService.ACTION_START)
        .putExtra(ResearchCollectionService.EXTRA_SESSION_ID, sessionId))
}

fun stopCollection(context: Context) {
    context.startService(Intent(context, ResearchCollectionService::class.java)
        .setAction(ResearchCollectionService.ACTION_STOP))
}

fun markCollection(context: Context, value: String = "MARK") {
    context.startService(Intent(context, ResearchCollectionService::class.java)
        .setAction(ResearchCollectionService.ACTION_MARK)
        .putExtra(ResearchCollectionService.EXTRA_VALUE, value))
}
