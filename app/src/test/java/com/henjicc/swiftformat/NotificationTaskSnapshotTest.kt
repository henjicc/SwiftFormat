package com.henjicc.swiftformat

import com.henjicc.swiftformat.conversion.ConversionTask
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import com.henjicc.swiftformat.service.notificationSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class NotificationTaskSnapshotTest {

    @Test
    fun completedScopedTask_isNotActiveWork() {
        val snapshot = notificationSnapshot(
            tasks = mapOf("done" to task("done", ConversionStatus.COMPLETED)),
            progressTaskIds = setOf("done"),
        )

        assertFalse(snapshot.hasActiveWork)
        assertEquals(0, snapshot.activeCount)
    }

    @Test
    fun missingScopedTask_countsAsPendingPlaceholder() {
        val snapshot = notificationSnapshot(
            tasks = emptyMap(),
            progressTaskIds = setOf("pending"),
        )

        assertTrue(snapshot.hasActiveWork)
        assertEquals(1, snapshot.activeCount)
        assertEquals(1, snapshot.pendingPlaceholderCount)
    }

    @Test
    fun emptyScope_usesOnlyActuallyActiveTasks() {
        val snapshot = notificationSnapshot(
            tasks = mapOf(
                "active" to task("active", ConversionStatus.CONVERTING),
                "done" to task("done", ConversionStatus.COMPLETED),
            ),
            progressTaskIds = emptySet(),
        )

        assertTrue(snapshot.hasActiveWork)
        assertEquals(1, snapshot.activeCount)
        assertEquals(listOf("active"), snapshot.tasks.map { it.request.id })
    }

    private fun task(id: String, status: ConversionStatus) = ConversionTask(
        request = ConversionRequest(
            id = id,
            input = InputFile(
                id = "input-$id",
                uri = mock(),
                displayName = "$id.jpg",
                mimeType = "image/jpeg",
                extension = "jpg",
                sizeBytes = 10L,
                mediaType = MediaType.IMAGE,
            ),
            outputFormat = "WEBP",
            targetMediaType = MediaType.IMAGE,
            quality = null,
            size = null,
            destination = OutputDestination.ResolvedUri(mock()),
        ),
        historyId = 1L,
        status = status,
    )
}
