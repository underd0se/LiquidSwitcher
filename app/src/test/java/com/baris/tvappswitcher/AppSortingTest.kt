package com.baris.tvappswitcher

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSortingTest {

    @Test
    fun testNameSorting() {
        val app1 = AppInfo("YouTube", "com.youtube", null)
        val app2 = AppInfo("Netflix", "com.netflix", null)
        val app3 = AppInfo("Amazon", "com.amazon", null)

        val unsorted = listOf(app1, app2, app3)
        // Simulate ConfigManager.sortApps() for NAME
        val sorted = unsorted.sortedBy { it.name.lowercase() }
        
        assertEquals("Amazon", sorted[0].name)
        assertEquals("Netflix", sorted[1].name)
        assertEquals("YouTube", sorted[2].name)
    }

    @Test
    fun testRecencySorting() {
        val app1 = AppInfo("YouTube", "com.youtube", null)
        val app2 = AppInfo("Netflix", "com.netflix", null)
        val app3 = AppInfo("Amazon", "com.amazon", null)

        val recencyMap = mapOf(
            "com.netflix" to 3000L,
            "com.amazon" to 2000L,
            "com.youtube" to 1000L
        )

        val unsorted = listOf(app1, app2, app3)
        // Simulate ConfigManager.sortApps() for RECENCY
        val sorted = unsorted.sortedWith(compareByDescending<AppInfo> { recencyMap[it.packageName] ?: 0L }
            .thenBy { it.name.lowercase() })

        assertEquals("Netflix", sorted[0].name)
        assertEquals("Amazon", sorted[1].name)
        assertEquals("YouTube", sorted[2].name)
    }

    @Test
    fun testCustomSorting() {
        val app1 = AppInfo("YouTube", "com.youtube", null)
        val app2 = AppInfo("Netflix", "com.netflix", null)
        val app3 = AppInfo("Amazon", "com.amazon", null)

        val customOrder = listOf("com.youtube", "com.amazon", "com.netflix")

        val unsorted = listOf(app2, app3, app1)
        // Simulate ConfigManager.sortApps() for CUSTOM
        val sorted = unsorted.sortedWith(compareBy<AppInfo> {
            val idx = customOrder.indexOf(it.packageName)
            if (idx != -1) idx else Int.MAX_VALUE
        }.thenBy { it.name.lowercase() })

        assertEquals("YouTube", sorted[0].name)
        assertEquals("Amazon", sorted[1].name)
        assertEquals("Netflix", sorted[2].name)
    }
}
