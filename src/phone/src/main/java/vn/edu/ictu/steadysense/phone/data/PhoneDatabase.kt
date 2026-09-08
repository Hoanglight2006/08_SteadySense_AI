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

package vn.edu.ictu.steadysense.phone.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "imu_windows", primaryKeys = ["sessionId", "sequenceId"])
data class ImuWindowEntity(
    val sessionId: String,
    val sequenceId: Long,
    val capturedAtEpochNanos: Long,
    val receivedAtEpochMillis: Long,
    val frameCount: Int,
    val payload: ByteArray,
)

@Dao
interface ImuWindowDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(window: ImuWindowEntity): Long

    @Query("SELECT COUNT(*) FROM imu_windows")
    fun count(): Int

    @Query("SELECT * FROM imu_windows ORDER BY receivedAtEpochMillis DESC LIMIT 1")
    fun latest(): ImuWindowEntity?

    @Query("SELECT * FROM imu_windows WHERE sessionId = :sessionId ORDER BY sequenceId")
    fun forSession(sessionId: String): List<ImuWindowEntity>

    @Query("SELECT * FROM imu_windows WHERE sessionId = :sessionId ORDER BY sequenceId DESC LIMIT :limit")
    fun latestBySession(sessionId: String, limit: Int): List<ImuWindowEntity>
}

// Schema Research Mode v2 — chỉ nền tảng (entity + DAO + migration test), CHƯA
// nối UI/export/validator thật (xem docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md
// mục 3 và mục 6 các bước tiếp theo). Không có trường tên/SĐT/bệnh án — đúng
// ràng buộc "không có trường định danh" của docs/06.

@Entity(tableName = "research_participants")
data class ResearchParticipantEntity(
    @PrimaryKey val code: String,
    val createdAt: Long,
    val consentVersion: String,
)

@Entity(tableName = "research_sessions")
data class ResearchSessionEntity(
    @PrimaryKey val id: String,
    val participantCode: String,
    val condition: String,
    val wornSide: String,
    val protocolVersion: String,
    val targetCycles: Int,
    val tempoBpm: Float,
    val startedAt: Long,
    val endedAt: Long,
    val status: String,
    val exclusionReason: String?,
)

@Entity(tableName = "research_events", primaryKeys = ["sessionId", "timestampNanos"])
data class ResearchEventEntity(
    val sessionId: String,
    val timestampNanos: Long,
    val type: String,
    val value: String,
)

@Entity(tableName = "device_snapshots")
data class DeviceSnapshotEntity(
    @PrimaryKey val sessionId: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val samplingConfig: String,
    val appVersion: String,
)

@Dao
interface ResearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertParticipant(participant: ResearchParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: ResearchSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEvent(event: ResearchEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeviceSnapshot(snapshot: DeviceSnapshotEntity)

    @Query("SELECT * FROM research_sessions WHERE id = :sessionId")
    fun sessionById(sessionId: String): ResearchSessionEntity?

    @Query("SELECT * FROM research_events WHERE sessionId = :sessionId ORDER BY timestampNanos")
    fun eventsForSession(sessionId: String): List<ResearchEventEntity>

    @Query("SELECT * FROM research_participants WHERE code = :code")
    fun participantByCode(code: String): ResearchParticipantEntity?

    @Query("SELECT * FROM device_snapshots WHERE sessionId = :sessionId")
    fun deviceSnapshot(sessionId: String): DeviceSnapshotEntity?

    @Query("UPDATE research_sessions SET endedAt = :endedAt, status = :status, exclusionReason = :reason WHERE id = :sessionId")
    fun finishSession(sessionId: String, endedAt: Long, status: String, reason: String?): Int

    @Query("SELECT * FROM research_sessions ORDER BY startedAt DESC")
    fun allSessions(): List<ResearchSessionEntity>
}

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long,
    val exerciseName: String,
    val selectedHand: String,
    val targetReps: Int,
    val completedReps: Int,
    val accuracyPercentage: Int,
    val steadyScore: Float,
    val sessionState: String, // "RELIABLE", "PARTIAL", "NEEDS_WORK"
    val durationSeconds: Int,
    val repDetailsJson: String,
    val scheduleId: String? = null,
)

@Entity(tableName = "workout_schedules")
data class WorkoutScheduleEntity(
    @PrimaryKey val id: String,
    val exerciseName: String,
    val targetReps: Int,
    val scheduledTime: String,
    val dayOfWeek: Int, // 1 = T2, ..., 7 = CN
    val repeatType: String, // "DAILY", "WEEKLY", "ONCE"
    val isCompleted: Boolean,
    val targetSets: Int = 1,
    val restSeconds: Int = 60,
    val specificDate: String? = null, // "yyyy-MM-dd" cho lịch ngày cụ thể
)

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun allSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun sessionsFlow(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun latestSessions(limit: Int): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE scheduleId = :scheduleId AND startedAt BETWEEN :startMillis AND :endMillis LIMIT 1")
    fun findSessionByScheduleAndDay(scheduleId: String, startMillis: Long, endMillis: Long): WorkoutSessionEntity?

    @Query("SELECT COUNT(*) FROM workout_sessions")
    fun countSessions(): Int

    @Query("SELECT SUM(completedReps) FROM workout_sessions")
    fun totalReps(): Int?

    @Query("DELETE FROM workout_sessions")
    fun clearAllSessions()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSchedule(schedule: WorkoutScheduleEntity)

    @Query("SELECT * FROM workout_schedules ORDER BY scheduledTime ASC")
    fun allSchedules(): List<WorkoutScheduleEntity>

    @Query("SELECT * FROM workout_schedules ORDER BY scheduledTime ASC")
    fun schedulesFlow(): Flow<List<WorkoutScheduleEntity>>

    @Query("DELETE FROM workout_schedules WHERE id = :id")
    fun deleteSchedule(id: String)
}

@Database(
    entities = [
        ImuWindowEntity::class,
        ResearchParticipantEntity::class,
        ResearchSessionEntity::class,
        ResearchEventEntity::class,
        DeviceSnapshotEntity::class,
        WorkoutSessionEntity::class,
        WorkoutScheduleEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class PhoneDatabase : RoomDatabase() {
    abstract fun imuWindowDao(): ImuWindowDao
    abstract fun researchDao(): ResearchDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile private var instance: PhoneDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `research_participants` (" +
                        "`code` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`consentVersion` TEXT NOT NULL, PRIMARY KEY(`code`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `research_sessions` (" +
                        "`id` TEXT NOT NULL, `participantCode` TEXT NOT NULL, `condition` TEXT NOT NULL, " +
                        "`wornSide` TEXT NOT NULL, `protocolVersion` TEXT NOT NULL, " +
                        "`targetCycles` INTEGER NOT NULL, `tempoBpm` REAL NOT NULL, " +
                        "`startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `status` TEXT NOT NULL, " +
                        "`exclusionReason` TEXT, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `research_events` (" +
                        "`sessionId` TEXT NOT NULL, `timestampNanos` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                        "`value` TEXT NOT NULL, PRIMARY KEY(`sessionId`, `timestampNanos`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `device_snapshots` (" +
                        "`sessionId` TEXT NOT NULL, `manufacturer` TEXT NOT NULL, `model` TEXT NOT NULL, " +
                        "`androidVersion` TEXT NOT NULL, `samplingConfig` TEXT NOT NULL, " +
                        "`appVersion` TEXT NOT NULL, PRIMARY KEY(`sessionId`))",
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
                        "`id` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, " +
                        "`exerciseName` TEXT NOT NULL, `selectedHand` TEXT NOT NULL, " +
                        "`targetReps` INTEGER NOT NULL, `completedReps` INTEGER NOT NULL, `accuracyPercentage` INTEGER NOT NULL, " +
                        "`steadyScore` REAL NOT NULL, `sessionState` TEXT NOT NULL, `durationSeconds` INTEGER NOT NULL, " +
                        "`repDetailsJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `workout_schedules` (" +
                        "`id` TEXT NOT NULL, `exerciseName` TEXT NOT NULL, `targetReps` INTEGER NOT NULL, " +
                        "`scheduledTime` TEXT NOT NULL, `dayOfWeek` INTEGER NOT NULL, `repeatType` TEXT NOT NULL, " +
                        "`isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_schedules` ADD COLUMN `targetSets` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_schedules` ADD COLUMN `restSeconds` INTEGER NOT NULL DEFAULT 60")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `scheduleId` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `workout_schedules` ADD COLUMN `specificDate` TEXT DEFAULT NULL")
            }
        }

        fun get(context: Context): PhoneDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PhoneDatabase::class.java,
                "steadysense.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build().also { instance = it }
        }
    }
}
