package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ScheduledAppBlockRepository
import com.example.data.local.BeSissyDatabase
import com.example.data.local.ScheduledAppBlockTaskEntity
import com.example.model.UiState
import com.example.viewmodel.ScheduledAppBlockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScheduledAppBlockRoomTest {

    private lateinit var db: BeSissyDatabase
    private lateinit var repository: ScheduledAppBlockRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BeSissyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ScheduledAppBlockRepository(db.scheduledAppBlockDao())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `repository saves, queries, toggles, updates, and deletes scheduled app block tasks`() = runTest {
        val insertedId = repository.saveTask(
            packageName = "com.zhiliaoapp.musically",
            appName = "TikTok",
            taskTitle = "Night Wind-Down",
            startMinutes = ScheduledAppBlockTaskEntity.toMinutesOfDay(22, 0),
            endMinutes = ScheduledAppBlockTaskEntity.toMinutesOfDay(7, 0),
            daysOfWeekMask = ScheduledAppBlockTaskEntity.daysMaskFromList(listOf(1, 2, 3, 4, 5)),
            enabled = true
        ).getOrThrow()

        assertTrue(insertedId > 0L)

        val all = repository.allTasks.first()
        assertEquals(1, all.size)
        val task = all.first()
        assertEquals("com.zhiliaoapp.musically", task.packageName)
        assertEquals("TikTok", task.appName)
        assertEquals("22:00 - 07:00", task.formattedWindow())
        assertTrue(task.isActiveAt(minutesOfDay = 23 * 60 + 30, dayOfWeek = 1)) // Mon 23:30
        assertTrue(task.isActiveAt(minutesOfDay = 6 * 60 + 15, dayOfWeek = 5))  // Fri 06:15
        assertFalse(task.isActiveAt(minutesOfDay = 12 * 60, dayOfWeek = 1))     // Mon 12:00
        assertFalse(task.isActiveAt(minutesOfDay = 23 * 60, dayOfWeek = 6))     // Sat 23:00 (not in mask)

        // Toggle disabled
        repository.setTaskEnabled(insertedId, enabled = false).getOrThrow()
        assertTrue(repository.enabledTasks.first().isEmpty())

        // Update time window
        repository.saveTask(
            id = insertedId,
            packageName = "com.zhiliaoapp.musically",
            appName = "TikTok",
            taskTitle = "Work Focus",
            startMinutes = ScheduledAppBlockTaskEntity.toMinutesOfDay(9, 0),
            endMinutes = ScheduledAppBlockTaskEntity.toMinutesOfDay(17, 0),
            enabled = true
        ).getOrThrow()

        val updated = repository.getTaskById(insertedId)!!
        assertEquals("Work Focus", updated.taskTitle)
        assertEquals("09:00 - 17:00", updated.formattedWindow())
        assertTrue(updated.isActiveAt(minutesOfDay = 13 * 60, dayOfWeek = 3))

        // Delete
        repository.deleteTask(insertedId).getOrThrow()
        assertTrue(repository.allTasks.first().isEmpty())
    }

    @Test
    fun `viewmodel manages scheduled app block tasks and exposes reactive state`() = runTest {
        val viewModel = ScheduledAppBlockViewModel(repository)
        backgroundScope.launch { viewModel.tasks.collect { } }

        val initialState = viewModel.uiState.first { it is UiState.Empty }
        assertEquals(UiState.Empty, initialState)

        viewModel.saveTask(
            packageName = "com.instagram.android",
            appName = "Instagram",
            taskTitle = "Evening Detox",
            startHour = 20,
            startMinute = 30,
            endHour = 23,
            endMinute = 0,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7),
            enabled = true
        )

        val successState = viewModel.uiState.first { it is UiState.Success } as UiState.Success
        val savedTasks = successState.data
        assertEquals(1, savedTasks.size)
        assertEquals("Instagram", savedTasks.first().appName)
        assertEquals("20:30 - 23:00", savedTasks.first().formattedWindow())

        viewModel.tasks.first { it.size == 1 }
        assertTrue(viewModel.isAppBlockedAt("com.instagram.android", 21 * 60, dayOfWeek = 2))
        assertFalse(viewModel.isAppBlockedAt("com.instagram.android", 18 * 60, dayOfWeek = 2))

        val savedId = savedTasks.first().id
        viewModel.toggleTaskEnabled(savedId, enabled = false)
        viewModel.tasks.first { list -> list.firstOrNull()?.enabled == false }
        assertFalse(viewModel.isAppBlockedAt("com.instagram.android", 21 * 60, dayOfWeek = 2))

        viewModel.deleteTask(savedId)
        val afterDeleteState = viewModel.uiState.first { it is UiState.Empty }
        assertEquals(UiState.Empty, afterDeleteState)
    }
}
