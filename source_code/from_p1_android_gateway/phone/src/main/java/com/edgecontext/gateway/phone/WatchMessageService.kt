package com.edgecontext.gateway.phone

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray
import java.util.concurrent.Executors

class WatchMessageService : WearableListenerService() {
    private val writer = Executors.newSingleThreadExecutor()

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != "/sensor_batch") return
        val receivedAt = System.currentTimeMillis()
        val array = JSONArray(String(event.data, Charsets.UTF_8))
        val rows = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SensorSampleEntity(
                        timestamp = item.getLong("timestamp"),
                        subjectId = item.getString("subject_id"),
                        sessionId = item.getString("session_id"),
                        deviceId = "watch",
                        placement = item.optString("placement", "wrist"),
                        label = item.getString("label"),
                        accX = item.getDouble("acc_x").toFloat(),
                        accY = item.getDouble("acc_y").toFloat(),
                        accZ = item.getDouble("acc_z").toFloat(),
                        gyroX = item.getDouble("gyro_x").toFloat(),
                        gyroY = item.getDouble("gyro_y").toFloat(),
                        gyroZ = item.getDouble("gyro_z").toFloat(),
                        deviceModel = item.optString("device_model", "wear_os_watch"),
                        latencyMs = receivedAt - item.optLong("sent_at", receivedAt),
                    )
                )
            }
        }
        writer.execute { AppDatabase.get(applicationContext).sensorSampleDao().insertAll(rows) }
    }

    override fun onDestroy() {
        writer.shutdown()
        super.onDestroy()
    }
}

