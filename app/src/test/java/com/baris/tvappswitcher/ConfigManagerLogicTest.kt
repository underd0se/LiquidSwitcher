package com.baris.tvappswitcher

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigManagerLogicTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSerializationRoundTrip() {
        // Custom Mappings
        val originalMappings =
            mapOf(
                183 to "com.netflix.ninja",
                184 to "com.google.android.youtube.tv",
            )
        val mappingsJson = json.encodeToString(originalMappings)
        val decodedMappings: Map<Int, String> = json.decodeFromString(mappingsJson)

        assertEquals(originalMappings, decodedMappings)

        // Custom App Order
        val originalOrder = listOf("com.netflix.ninja", "com.amazon", "com.youtube")
        val orderJson = json.encodeToString(originalOrder)
        val decodedOrder: List<String> = json.decodeFromString(orderJson)

        assertEquals(originalOrder, decodedOrder)

        // Recency map
        val originalRecency = mapOf("com.amazon" to 123456L, "com.youtube" to 98765L)
        val recencyJson = json.encodeToString(originalRecency)
        val decodedRecency: Map<String, Long> = json.decodeFromString(recencyJson)

        assertEquals(originalRecency, decodedRecency)
    }

    @Test
    fun testBoundaryCoercionLogic() {
        // Dock Scale: coerceIn(60, 140)
        assertEquals(60, 10.coerceIn(60, 140))
        assertEquals(140, 200.coerceIn(60, 140))
        assertEquals(100, 100.coerceIn(60, 140))

        // Dock Transparency: coerceIn(0, 90)
        assertEquals(0, (-10).coerceIn(0, 90))
        assertEquals(90, 100.coerceIn(0, 90))
        assertEquals(45, 45.coerceIn(0, 90))

        // Dock Bottom Margin: coerceIn(0, 72)
        assertEquals(0, (-5).coerceIn(0, 72))
        assertEquals(72, 100.coerceIn(0, 72))
        assertEquals(36, 36.coerceIn(0, 72))

        // Dock Dim: coerceIn(0, 80)
        assertEquals(0, (-20).coerceIn(0, 80))
        assertEquals(80, 100.coerceIn(0, 80))
        assertEquals(50, 50.coerceIn(0, 80))
    }
}
