package com.example.history

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: String,
    val gestureKey: String,
    val gestureLabel: String,
    val actionKey: String,
    val actionLabel: String,
    val succeeded: Boolean,
    val detailMessage: String,
    val confidencePercent: Int,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs WHERE userId = :userId ORDER BY timestampMillis DESC LIMIT 100")
    fun observeLogsForUser(userId: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Query(
        "DELETE FROM activity_logs WHERE userId = :userId AND id NOT IN " +
            "(SELECT id FROM activity_logs WHERE userId = :userId ORDER BY timestampMillis DESC LIMIT 100)"
    )
    suspend fun trimToLatest100(userId: String)

    @Query("DELETE FROM activity_logs WHERE userId = :userId")
    suspend fun clearLogsForUser(userId: String)
}

@Database(entities = [ActivityLogEntity::class], version = 1, exportSchema = false)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: HistoryDatabase? = null

        fun getInstance(context: Context): HistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HistoryDatabase::class.java,
                    "negm_gesture_history.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class HistoryLocalRepository(private val dao: ActivityLogDao) {
    fun observeUserHistory(userId: String): Flow<List<ActivityLogEntity>> =
        dao.observeLogsForUser(userId)

    suspend fun recordLog(log: ActivityLogEntity) {
        dao.insertLog(log)
        dao.trimToLatest100(log.userId)
    }

    suspend fun clearUserHistory(userId: String) {
        dao.clearLogsForUser(userId)
    }
}
