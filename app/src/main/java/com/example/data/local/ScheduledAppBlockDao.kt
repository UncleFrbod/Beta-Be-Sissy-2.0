package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledAppBlockDao {

    @Query("SELECT * FROM scheduled_app_block_tasks ORDER BY startMinutes ASC, appName ASC")
    fun observeAllTasks(): Flow<List<ScheduledAppBlockTaskEntity>>

    @Query("SELECT * FROM scheduled_app_block_tasks WHERE enabled = 1 ORDER BY startMinutes ASC, appName ASC")
    fun observeEnabledTasks(): Flow<List<ScheduledAppBlockTaskEntity>>

    @Query("SELECT * FROM scheduled_app_block_tasks WHERE packageName = :packageName ORDER BY startMinutes ASC")
    fun observeTasksForPackage(packageName: String): Flow<List<ScheduledAppBlockTaskEntity>>

    @Query("SELECT * FROM scheduled_app_block_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: Long): ScheduledAppBlockTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: ScheduledAppBlockTaskEntity): Long

    @Update
    suspend fun updateTask(task: ScheduledAppBlockTaskEntity)

    @Query(
        "UPDATE scheduled_app_block_tasks " +
            "SET enabled = :enabled, updatedAtMillis = :updatedAtMillis " +
            "WHERE id = :taskId"
    )
    suspend fun setTaskEnabled(
        taskId: Long,
        enabled: Boolean,
        updatedAtMillis: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM scheduled_app_block_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("DELETE FROM scheduled_app_block_tasks")
    suspend fun clearAllTasks()
}
