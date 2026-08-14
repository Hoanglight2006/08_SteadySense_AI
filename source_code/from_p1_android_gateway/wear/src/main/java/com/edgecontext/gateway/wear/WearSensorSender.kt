package com.edgecontext.gateway.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class WearSensorSender(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val latestGyro = FloatArray(3)
    private val pending = mutableListOf<JSONObject>()
    private val epochOffsetMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    @Volatile private var metadata: WearMetadata? = null
    @Volatile var running: Boolean = false
        private set

    @Synchronized
    fun start(value: WearMetadata) {
        stop()
        metadata = value
        running = true
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    @Synchronized
    fun stop() {
        if (running) sensorManager.unregisterListener(this)
        running = false
        flush()
        metadata = null
    }

    fun close() {
        stop()
        scope.cancel()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running) return
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            latestGyro[0] = event.values[0]
            latestGyro[1] = event.values[1]
            latestGyro[2] = event.values[2]
            return
        }
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val current = metadata ?: return
        val now = System.currentTimeMillis()
        val sampleTime = epochOffsetMillis + (event.timestamp / 1_000_000L)
        val row = JSONObject()
            .put("timestamp", sampleTime)
            .put("subject_id", current.subjectId)
            .put("session_id", current.sessionId)
            .put("device_id", "watch")
            .put("placement", current.placement)
            .put("label", current.label)
            .put("acc_x", event.values[0].toDouble())
            .put("acc_y", event.values[1].toDouble())
            .put("acc_z", event.values[2].toDouble())
            .put("gyro_x", latestGyro[0].toDouble())
            .put("gyro_y", latestGyro[1].toDouble())
            .put("gyro_z", latestGyro[2].toDouble())
            .put("device_model", "${Build.MANUFACTURER}_${Build.MODEL}")
            .put("sent_at", now)
        synchronized(pending) {
            pending += row
            if (pending.size >= 32) flushLocked()
        }
    }

    private fun flush() = synchronized(pending) { flushLocked() }

    private fun flushLocked() {
        if (pending.isEmpty()) return
        val payload = JSONArray(pending.toList()).toString().toByteArray(Charsets.UTF_8)
        pending.clear()
        scope.launch {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            for (node in nodes) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/sensor_batch", payload)
                    .await()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

