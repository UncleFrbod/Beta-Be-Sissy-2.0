package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageHistoryDao {

    @Query(
        "SELECT * FROM historical_app_usage " +
            "WHERE userId = :userId AND timestampMillis >= :minTimestampMillis " +
            "ORDER BY timestampMillis DESC"
    )
    fun observeUsageSince(
        userId: String,
        minTimestampMillis: Long
    ): Flow<List<HistoricalAppUsageEntity>>

    @Query(
        "SELECT * FROM historical_app_usage " +
            "WHERE userId = :userId " +
            "ORDER BY timestampMillis DESC"
    )
    fun observeAllUsage(userId: String): Flow<List<HistoricalAppUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsageEntries(entries: List<HistoricalAppUsageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsageEntry(entry: HistoricalAppUsageEntity)

    @Query(
        "SELECT * FROM restriction_session_logs " +
            "WHERE userId = :userId AND startedAtMillis >= :minTimestampMillis " +
            "ORDER BY startedAtMillis DESC"
    )
    fun observeSessionLogsSince(
        userId: String,
        minTimestampMillis: Long
    ): Flow<List<RestrictionSessionLogEntity>>

    @Query(
        "SELECT * FROM restriction_session_logs " +
            "WHERE userId = :userId " +
            "ORDER BY startedAtMillis DESC"
    )
    fun observeAllSessionLogs(userId: String): Flow<List<RestrictionSessionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionLog(log: RestrictionSessionLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionLogs(logs: List<RestrictionSessionLogEntity>)

    @Query("SELECT COUNT(*) FROM historical_app_usage WHERE userId = :userId")
    suspend fun countUsageRows(userId: String): Int

    @Query("DELETE FROM historical_app_usage WHERE userId = :userId")
    suspend fun clearUsageForUser(userId: String)
}
