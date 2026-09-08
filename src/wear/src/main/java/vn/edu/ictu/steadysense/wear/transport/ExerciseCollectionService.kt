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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import vn.edu.ictu.steadysense.core.ImuPayloadCodec
import vn.edu.ictu.steadysense.core.ImuWindowAssembler
import vn.edu.ictu.steadysense.core.SensorVector
import vn.edu.ictu.steadysense.core.TransportEnvelope
import vn.edu.ictu.steadysense.wear.MainActivity
import vn.edu.ictu.steadysense.wear.R
import vn.edu.ictu.steadysense.wear.data.WearDatabase

/**
 * Foreground Service chịu trách nhiệm thu thập cảm biến IMU (Gia tốc kế + Con quay hồi chuyển)
 * trong suốt buổi tập luyện của người dùng trên đồng hồ.
 *
 * Dữ liệu được ghép thành các cửa sổ 40 frame (~2.0s tại 20Hz) qua [ImuWindowAssembler]
 * rồi gửi ngay sang điện thoại qua [WearSender] để AI phân tích.
 */
class ExerciseCollectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var assembler: ImuWindowAssembler? = null
    private var sessionId = ""
    private var sequence = 0L
    private var wakeLock: PowerManager.WakeLock? = null
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        createChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()
        beginExerciseCollection()
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_steadysense)
            .setContentTitle("SteadySense đang theo dõi bài tập")
            .setContentText("Cảm biến IMU đang hoạt động theo thời gian thực")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun beginExerciseCollection() {
        sessionId = "exercise-${System.currentTimeMillis()}"
        sequence = 0L

        // Giữ WakeLock để đồng hồ không tắt cảm biến khi người dùng đang gập tay
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SteadySense:ExerciseWakeLock",
            ).apply {
                setReferenceCounted(false)
                acquire(30 * 60 * 1000L) // Tối đa 30 phút cho 1 buổi tập
            }
        }

        val offset = System.currentTimeMillis() * 1_000_000L - SystemClock.elapsedRealtimeNanos()
        assembler = ImuWindowAssembler(offset)

        // Hủy đăng ký cũ nếu có để tránh trùng lặp
        sensorManager.unregisterListener(this)

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val accelOk = sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        val gyroOk = sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_GAME)
        Log.i(TAG, "Exercise IMU sensors registered: accel=$accelOk, gyro=$gyroOk, session=$sessionId")

        // Xóa outbox cũ để chỉ gửi dữ liệu phiên hiện tại, tránh nghẽn hàng đợi
        io.execute {
            WearDatabase.get(this).outboxDao().deleteOtherSessions(sessionId)
            WearSender.retryPending(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sample = SensorVector(event.timestamp, event.values[0], event.values[1], event.values[2])
        val window = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> assembler?.onAccelerometer(sample)
            Sensor.TYPE_GYROSCOPE -> assembler?.onGyroscope(sample)
            else -> null
        } ?: return

        sequence++
        val payload = ImuPayloadCodec.encode(window)
        val envelope = TransportEnvelope(
            sessionId = sessionId,
            sequenceId = sequence,
            capturedAtEpochNanos = window.frames.first().timestampEpochNanos,
            payload = payload,
        )

        WearSender.enqueueAndSend(this, envelope)
        if (sequence % 5L == 0L) {
            Log.d(TAG, "Streaming IMU window seq=$sequence frames=${window.frames.size}")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.i(TAG, "Stopping ExerciseCollectionService")
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null

        sensorManager.unregisterListener(this)
        assembler = null
        io.shutdown()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Theo dõi bài tập SteadySense",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ExerciseCollectionSvc"
        private const val CHANNEL_ID = "exercise_collection_channel"
        private const val NOTIFICATION_ID = 51

        const val ACTION_START = "vn.edu.ictu.steadysense.exercise.START"
        const val ACTION_STOP = "vn.edu.ictu.steadysense.exercise.STOP"

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ExerciseCollectionService::class.java).setAction(ACTION_START),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ExerciseCollectionService", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.startService(
                    Intent(context, ExerciseCollectionService::class.java).setAction(ACTION_STOP),
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop ExerciseCollectionService", e)
            }
        }
    }
}
