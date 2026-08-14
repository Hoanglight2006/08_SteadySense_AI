package vn.edu.ictu.steadysense.wear.data

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

@Entity(tableName = "transport_outbox", primaryKeys = ["sessionId", "sequenceId"])
data class OutboxEntity(
    val sessionId: String,
    val sequenceId: Long,
    val createdAtEpochMillis: Long,
    val encodedEnvelope: ByteArray,
)

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(entry: OutboxEntity): Long

    @Query("DELETE FROM transport_outbox WHERE sessionId = :sessionId AND sequenceId = :sequenceId")
    fun acknowledge(sessionId: String, sequenceId: Long): Int

    @Query("SELECT * FROM transport_outbox ORDER BY createdAtEpochMillis, sequenceId LIMIT :limit")
    fun pending(limit: Int): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM transport_outbox")
    fun count(): Int

    @Query("SELECT COALESCE(MAX(sequenceId), 0) FROM transport_outbox WHERE sessionId = :sessionId")
    fun maxSequence(sessionId: String): Long

    @Query("DELETE FROM transport_outbox WHERE createdAtEpochMillis < :cutoffEpochMillis")
    fun deleteOlderThan(cutoffEpochMillis: Long): Int
}

// Bảng nền tảng cho Research Mode (docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md
// mục 3–4): Wear giữ cấu hình phiên nghiên cứu đang hoạt động để "khôi phục
// sau restart"; service Research Mode đọc bảng này khi tiến trình được tạo lại.
@Entity(tableName = "research_session_config")
data class ResearchSessionConfigEntity(
    @PrimaryKey val sessionId: String,
    val participantCode: String,
    val condition: String,
    val wornSide: String,
    val targetCycles: Int,
    val tempoBpm: Float,
    val configVersion: Int,
    val receivedAtEpochMillis: Long,
)

@Dao
interface ResearchSessionConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(config: ResearchSessionConfigEntity)

    @Query("SELECT * FROM research_session_config ORDER BY receivedAtEpochMillis DESC LIMIT 1")
    fun latest(): ResearchSessionConfigEntity?

    @Query("DELETE FROM research_session_config WHERE sessionId = :sessionId")
    fun clear(sessionId: String)
}

@Database(
    entities = [OutboxEntity::class, ResearchSessionConfigEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class WearDatabase : RoomDatabase() {
    abstract fun outboxDao(): OutboxDao
    abstract fun researchSessionConfigDao(): ResearchSessionConfigDao

    companion object {
        @Volatile private var instance: WearDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `research_session_config` (" +
                        "`sessionId` TEXT NOT NULL, `participantCode` TEXT NOT NULL, " +
                        "`condition` TEXT NOT NULL, `wornSide` TEXT NOT NULL, " +
                        "`targetCycles` INTEGER NOT NULL, `tempoBpm` REAL NOT NULL, " +
                        "`configVersion` INTEGER NOT NULL, `receivedAtEpochMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`sessionId`))",
                )
            }
        }

        fun get(context: Context): WearDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WearDatabase::class.java,
                "steadysense-wear.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
