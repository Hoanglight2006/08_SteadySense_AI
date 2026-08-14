package com.edgecontext.gateway.phone

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_samples")
data class SensorSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val subjectId: String,
    val sessionId: String,
    val deviceId: String,
    val placement: String,
    val label: String,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyroX: Float,
    val gyroY: Float,
    val gyroZ: Float,
    val deviceModel: String,
    val batteryPct: Int? = null,
    val latencyMs: Long? = null,
)

