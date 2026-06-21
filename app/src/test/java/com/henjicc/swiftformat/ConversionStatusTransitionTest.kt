package com.henjicc.swiftformat

import com.henjicc.swiftformat.conversion.ConversionStatusTransition
import com.henjicc.swiftformat.core.model.ConversionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionStatusTransitionTest {

    @Test
    fun lowProgress_fromPending_movesToConverting() {
        assertEquals(ConversionStatus.CONVERTING, ConversionStatusTransition.fromProgress(ConversionStatus.PENDING, 0f))
    }

    @Test
    fun lowProgress_fromPreparing_movesToConverting() {
        assertEquals(ConversionStatus.CONVERTING, ConversionStatusTransition.fromProgress(ConversionStatus.PREPARING, 0.3f))
    }

    @Test
    fun highProgress_movesToSaving() {
        assertEquals(ConversionStatus.SAVING, ConversionStatusTransition.fromProgress(ConversionStatus.CONVERTING, 0.95f))
        assertEquals(ConversionStatus.SAVING, ConversionStatusTransition.fromProgress(ConversionStatus.PENDING, 0.9f))
    }

    @Test
    fun terminalStatuses_areUnaffectedByProgress() {
        assertEquals(ConversionStatus.COMPLETED, ConversionStatusTransition.fromProgress(ConversionStatus.COMPLETED, 0.1f))
        assertEquals(ConversionStatus.FAILED, ConversionStatusTransition.fromProgress(ConversionStatus.FAILED, 1f))
        assertEquals(ConversionStatus.CANCELLED, ConversionStatusTransition.fromProgress(ConversionStatus.CANCELLED, 1f))
    }

    @Test
    fun saving_staysSavingOnFurtherProgress() {
        assertEquals(ConversionStatus.SAVING, ConversionStatusTransition.fromProgress(ConversionStatus.SAVING, 1f))
    }
}
