package vn.edu.ictu.steadysense.wear.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Xác nhận Migration(1, 2) tạo bảng research_session_config và KHÔNG xóa gói
 * đang chờ ACK trong transport_outbox — không destructive migration (đúng
 * docs/06_KE_HOACH_CONG_CU_THU_DU_LIEU.md mục 3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34]) // Robolectric 4.13 chưa hỗ trợ API 35 (targetSdk thật của app)
class WearDatabaseMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WearDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsResearchSessionConfigAndKeepsPendingOutbox() {
        val dbName = "wear-migration-test.db"

        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO transport_outbox (sessionId, sequenceId, createdAtEpochMillis, " +
                    "encodedEnvelope) VALUES ('session-1', 1, 1000, X'0011')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(dbName, 2, true, WearDatabase.MIGRATION_1_2)

        migrated.query("SELECT COUNT(*) FROM transport_outbox").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'research_session_config'",
        ).use { cursor ->
            assertEquals(1, cursor.count)
        }
    }
}
