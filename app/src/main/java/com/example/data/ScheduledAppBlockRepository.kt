package com.example.data

import com.example.data.local.ScheduledAppBlockDao
import com.example.data.local.ScheduledAppBlockTaskEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class ScheduledAppBlockRepository(
    private val dao: ScheduledAppBlockDao
) {
    val allTasks: Flow<List<ScheduledAppBlockTaskEntity>> = dao.observeAllTasks()

    val enabledTasks: Flow<List<ScheduledAppBlockTaskEntity>> = dao.observeEnabledTasks()

    fun observeTasksForPackage(packageName: String): Flow<List<ScheduledAppBlockTaskEntity>> =
        dao.observeTasksForPackage(packageName.trim())

    suspend fun getTaskById(taskId: Long): ScheduledAppBlockTaskEntity? =
        dao.getTaskById(taskId)

    suspend fun saveTask(
        id: Long = 0L,
        packageName: String,
        appName: String,
        taskTitle: String = "",
        startMinutes: Int,
        endMinutes: Int,
        daysOfWeekMask: Int = ScheduledAppBlockTaskEntity.ALL_DAYS_MASK,
        enabled: Boolean = true
    ): Result<Long> {
        return try {
            val cleanPackage = packageName.trim()
            require(cleanPackage.isNotEmpty()) { "Application package name cannot be empty." }
            val cleanAppName = appName.trim().ifEmpty { cleanPackage.substringAfterLast('.') }
            val cleanTitle = taskTitle.trim().ifEmpty { "$cleanAppName Focus Block" }
            val safeStart = startMinutes.coerceIn(0, ScheduledAppBlockTaskEntity.MAX_MINUTE_OF_DAY)
            val safeEnd = endMinutes.coerceIn(0, ScheduledAppBlockTaskEntity.MAX_MINUTE_OF_DAY)
            val safeMask = if (daysOfWeekMask and ScheduledAppBlockTaskEntity.ALL_DAYS_MASK == 0) {
                ScheduledAppBlockTaskEntity.ALL_DAYS_MASK
            } else {
                daysOfWeekMask and ScheduledAppBlockTaskEntity.ALL_DAYS_MASK
            }

            val now = System.currentTimeMillis()
            val existing = if (id > 0L) dao.getTaskById(id) else null
            val entity = ScheduledAppBlockTaskEntity(
                id = if (existing != null) existing.id else 0L,
                packageName = cleanPackage,
                appName = cleanAppName,
                taskTitle = cleanTitle,
                startMinutes = safeStart,
                endMinutes = safeEnd,
                daysOfWeekMask = safeMask,
                enabled = enabled,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            val rowId = dao.upsertTask(entity)
            Result.success(if (existing != null) existing.id else rowId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setTaskEnabled(taskId: Long, enabled: Boolean): Result<Unit> {
        return try {
            require(taskId > 0L) { "Invalid task ID." }
            dao.setTaskEnabled(taskId = taskId, enabled = enabled)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: Long): Result<Unit> {
        return try {
            require(taskId > 0L) { "Invalid task ID." }
            dao.deleteTaskById(taskId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
