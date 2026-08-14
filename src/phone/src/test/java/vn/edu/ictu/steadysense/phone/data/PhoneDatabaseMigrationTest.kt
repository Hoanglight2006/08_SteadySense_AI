package vn.edu.ictu.steadysense.phone.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Xác nhận Migration(1, 2) tạo đúng 4 bảng Research Mode và KHÔNG xóa dữ liệu
 * imu_windows đã có từ v1 (không destructive migration — bắt buộc theo
 * docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md mục 3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34]) // Robolectric 4.13 chưa hỗ trợ API 35 (targetSdk thật của app)
class PhoneDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PhoneDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsResearchTablesAndKeepsExistingImuWindows() {
        val dbName = "phone-migration-test.db"

        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO imu_windows (sessionId, sequenceId, capturedAtEpochNanos, " +
                    "receivedAtEpochMillis, frameCount, payload) VALUES " +
                    "('session-1', 1, 1000, 2000, 40, X'0011')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, PhoneDatabase.MIGRATION_1_2)

        migrated.query("SELECT COUNT(*) FROM imu_windows").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        val expectedTables = setOf(
            "research_participants",
            "research_sessions",
            "research_events",
            "device_snapshots",
        )
        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN " +
                "('research_participants', 'research_sessions', 'research_events', 'device_snapshots')",
        ).use { cursor ->
            val found = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                found.add(cursor.getString(0))
            }
            assertEquals(expectedTables, found)
        }
    }
}
