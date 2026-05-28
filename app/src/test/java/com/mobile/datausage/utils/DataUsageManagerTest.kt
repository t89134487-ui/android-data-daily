package com.mobile.datausage.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class DataUsageManagerTest {

    @Test
    fun testFormatDataUsage_MB() {
        val bytes = 500L * 1024 * 1024
        val formatted = DataUsageManager.formatDataUsage(bytes)
        assertEquals("500.0 MB", formatted)
    }

    @Test
    fun testFormatDataUsage_GB() {
        val bytes = 1500L * 1024 * 1024
        val formatted = DataUsageManager.formatDataUsage(bytes)
        assertEquals("1.5 GB", formatted)
    }

    @Test
    fun testFormatDataUsage_Precision() {
        val bytes = (102.4 * 1024 * 1024).toLong()
        val formatted = DataUsageManager.formatDataUsage(bytes)
        assertEquals("102.4 MB", formatted)
    }
}
