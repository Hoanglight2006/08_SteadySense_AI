package com.edgecontext.gateway.phone

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SensorSampleDao {
    @Insert
    fun insert(sample: SensorSampleEntity)

    @Insert
    fun insertAll(samples: List<SensorSampleEntity>)

    @Query("SELECT * FROM sensor_samples ORDER BY timestamp, id")
    fun getAll(): List<SensorSampleEntity>

    @Query("SELECT COUNT(*) FROM sensor_samples")
    fun countAll(): Int

    @Query("SELECT COUNT(*) FROM sensor_samples WHERE sessionId = :sessionId AND deviceId = :deviceId")
    fun countForTrial(sessionId: String, deviceId: String): Int

    @Query("DELETE FROM sensor_samples")
    fun clear()
}

