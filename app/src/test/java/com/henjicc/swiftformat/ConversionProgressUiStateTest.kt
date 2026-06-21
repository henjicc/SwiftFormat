package com.henjicc.swiftformat

import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.feature.progress.ConversionProgressUiState
import com.henjicc.swiftformat.feature.progress.ConversionTaskUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class ConversionProgressUiStateTest {

    private fun itemWith(status: ConversionStatus, progress: Float = 0f, id: String = "1") = ConversionTaskUiItem(
        taskId = id,
        displayName = "a",
        originalFormat = "mov",
        outputFormat = "MP4",
        inputUri = mock(),
        outputUri = null,
        status = status,
        progress = progress,
        failureReason = null,
    )

    @Test
    fun isActive_trueOnlyForNonTerminalStatuses() {
        assertTrue(itemWith(ConversionStatus.PENDING).isActive)
        assertTrue(itemWith(ConversionStatus.PREPARING).isActive)
        assertTrue(itemWith(ConversionStatus.CONVERTING).isActive)
        assertTrue(itemWith(ConversionStatus.SAVING).isActive)
        assertFalse(itemWith(ConversionStatus.COMPLETED).isActive)
        assertFalse(itemWith(ConversionStatus.FAILED).isActive)
        assertFalse(itemWith(ConversionStatus.CANCELLED).isActive)
    }

    @Test
    fun canRetry_onlyWhenFailed() {
        assertTrue(itemWith(ConversionStatus.FAILED).canRetry)
        assertFalse(itemWith(ConversionStatus.CANCELLED).canRetry)
        assertFalse(itemWith(ConversionStatus.CONVERTING).canRetry)
    }

    @Test
    fun overallFraction_countsTerminalTasksAsComplete() {
        val state = ConversionProgressUiState(
            items = listOf(
                itemWith(ConversionStatus.COMPLETED, id = "1"),
                itemWith(ConversionStatus.CONVERTING, progress = 0.5f, id = "2"),
            ),
        )
        assertEquals(0.75f, state.overallFraction, 0.001f)
    }

    @Test
    fun overallFraction_emptyList_isZero() {
        assertEquals(0f, ConversionProgressUiState().overallFraction, 0.001f)
    }

    @Test
    fun hasActiveTasks_falseWhenAllTerminal() {
        val state = ConversionProgressUiState(
            items = listOf(itemWith(ConversionStatus.COMPLETED, id = "1"), itemWith(ConversionStatus.FAILED, id = "2")),
        )
        assertFalse(state.hasActiveTasks)
        assertNull(state.currentItem)
    }

    @Test
    fun currentItem_returnsFirstActiveTask() {
        val active = itemWith(ConversionStatus.CONVERTING, id = "2")
        val state = ConversionProgressUiState(items = listOf(itemWith(ConversionStatus.COMPLETED, id = "1"), active))
        assertEquals(active, state.currentItem)
    }

    @Test
    fun completedAndTotal_countCorrectly() {
        val state = ConversionProgressUiState(
            items = listOf(
                itemWith(ConversionStatus.COMPLETED, id = "1"),
                itemWith(ConversionStatus.COMPLETED, id = "2"),
                itemWith(ConversionStatus.FAILED, id = "3"),
                itemWith(ConversionStatus.CANCELLED, id = "4"),
            ),
        )
        assertEquals(4, state.total)
        assertEquals(2, state.completed)
        assertEquals(1, state.failed)
        assertEquals(1, state.cancelled)
    }

    @Test
    fun canConvertAgain_onlyWhenCompleted() {
        assertTrue(itemWith(ConversionStatus.COMPLETED).canConvertAgain)
        assertFalse(itemWith(ConversionStatus.FAILED).canConvertAgain)
    }
}
