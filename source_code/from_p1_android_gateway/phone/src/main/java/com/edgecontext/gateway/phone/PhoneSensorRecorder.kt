package com.edgecontext.gateway.phone

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PhoneSensorRecorder(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val dao = AppDatabase.get(context).sensorSampleDao()
    private val writer: ExecutorService = Executors.newSingleThreadExecutor()
    private val latestGyro = FloatArray(3)
    private val epochOffsetMillis = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    @Volatile private var metadata: CollectionMetadata? = null
    @Volatile var running: Boolean = false
        private set

    fun start(value: CollectionMetadata) {
        stop()
        metadata = value
        running = true
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    fun stop() {
        if (running) sensorManager.unregisterListener(this)
        running = false
        metadata = null
    }

    fun close() {
        stop()
        writer.shutdown()
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
        val sample = SensorSampleEntity(
            // Sensor events may be delivered in batches. Preserve acquisition time
            // instead of assigning the same wall-clock millisecond to a whole batch.
            timestamp = epochOffsetMillis + (event.timestamp / 1_000_000L),
            subjectId = current.subjectId,
            sessionId = current.sessionId,
            deviceId = "phone",
            placement = current.placement,
            label = current.label,
            accX = event.values[0],
            accY = event.values[1],
            accZ = event.values[2],
            gyroX = latestGyro[0],
            gyroY = latestGyro[1],
            gyroZ = latestGyro[2],
            deviceModel = "${Build.MANUFACTURER}_${Build.MODEL}",
        )
        writer.execute { dao.insert(sample) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

