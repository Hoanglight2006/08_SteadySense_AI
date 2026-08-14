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

@Database(
    entities = [
        ImuWindowEntity::class,
        ResearchParticipantEntity::class,
        ResearchSessionEntity::class,
        ResearchEventEntity::class,
        DeviceSnapshotEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class PhoneDatabase : RoomDatabase() {
    abstract fun imuWindowDao(): ImuWindowDao
    abstract fun researchDao(): ResearchDao

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

        fun get(context: Context): PhoneDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PhoneDatabase::class.java,
                "steadysense.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
