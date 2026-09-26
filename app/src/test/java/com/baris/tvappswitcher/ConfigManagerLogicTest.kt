package com.baris.tvappswitcher

import org.junit.Assert.assertEquals
import org.junit.Test
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ConfigManagerLogicTest {

    @Test
    fun testGsonSerializationRoundTrip() {
        // Custom Mappings
        val originalMappings = mapOf(
            183 to "com.netflix.ninja",
            184 to "com.google.android.youtube.tv"
        )
        val mappingsJson = Gson().toJson(originalMappings)
        val mappingsType = object : TypeToken<Map<Int, String>>() {}.type
        val decodedMappings: Map<Int, String> = Gson().fromJson(mappingsJson, mappingsType)
        
        assertEquals(originalMappings, decodedMappings)

        // Custom App Order
        val originalOrder = listOf("com.netflix.ninja", "com.amazon", "com.youtube")
        val orderJson = Gson().toJson(originalOrder)
        val orderType = object : TypeToken<List<String>>() {}.type
        val decodedOrder: List<String> = Gson().fromJson(orderJson, orderType)
        
        assertEquals(originalOrder, decodedOrder)

        // Recency map
        val originalRecency = mapOf("com.amazon" to 123456L, "com.youtube" to 98765L)
        val recencyJson = Gson().toJson(originalRecency)
        val recencyType = object : TypeToken<Map<String, Long>>() {}.type
        val decodedRecency: Map<String, Long> = Gson().fromJson(recencyJson, recencyType)
        
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
