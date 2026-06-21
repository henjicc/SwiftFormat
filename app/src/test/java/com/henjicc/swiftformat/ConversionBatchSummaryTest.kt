package com.henjicc.swiftformat

import android.net.Uri
import com.henjicc.swiftformat.conversion.ConversionBatchSummary
import com.henjicc.swiftformat.conversion.ConversionTask
import com.henjicc.swiftformat.core.model.ConversionRequest
import com.henjicc.swiftformat.core.model.ConversionStatus
import com.henjicc.swiftformat.core.model.InputFile
import com.henjicc.swiftformat.core.model.MediaType
import com.henjicc.swiftformat.core.model.OutputDestination
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class ConversionBatchSummaryTest {

    private fun taskWith(status: ConversionStatus): ConversionTask {
        val uri = mock<Uri>()
        val request = ConversionRequest(
            id = "1",
            input = InputFile(
                id = "1",
                uri = uri,
                displayName = "a",
                mimeType = null,
                extension = null,
                sizeBytes = null,
                mediaType = MediaType.IMAGE,
            ),
            outputFormat = "JPG",
            quality = null,
            size = null,
            destination = OutputDestination.ResolvedUri(uri),
        )
        return ConversionTask(request, historyId = 1, status = status)
    }

    @Test
    fun countsEachStatusBucket() {
        val tasks = listOf(
            taskWith(ConversionStatus.COMPLETED),
            taskWith(ConversionStatus.COMPLETED),
            taskWith(ConversionStatus.FAILED),
            taskWith(ConversionStatus.CANCELLED),
            taskWith(ConversionStatus.CONVERTING),
        )
        val summary = ConversionBatchSummary.from(tasks)
        assertEquals(5, summary.total)
        assertEquals(2, summary.completed)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.cancelled)
        assertEquals(1, summary.inProgress)
    }

    @Test
    fun emptyList_yieldsZeroedSummary() {
        val summary = ConversionBatchSummary.from(emptyList())
        assertEquals(0, summary.total)
        assertEquals(0, summary.completed)
        assertEquals(0, summary.inProgress)
    }
}
