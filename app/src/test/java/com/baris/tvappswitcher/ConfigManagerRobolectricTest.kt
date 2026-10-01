package com.baris.tvappswitcher

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigManagerRobolectricTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ConfigManager
            .getPrefs(context)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testSwitchDelayLabelsWithContext() {
        assertEquals("Manual (Press OK to Switch)", ConfigManager.getSwitchDelayLabel(context, 0))
        assertEquals("1 second", ConfigManager.getSwitchDelayLabel(context, 1))
        assertEquals("3 seconds", ConfigManager.getSwitchDelayLabel(context, 3))

        assertEquals("Manual", ConfigManager.getSwitchDelayShortLabel(context, 0))
        assertEquals("1s", ConfigManager.getSwitchDelayShortLabel(context, 1))
        assertEquals("4s", ConfigManager.getSwitchDelayShortLabel(context, 4))
    }

    @Test
    fun testKeyPreferencesPersistence() {
        assertEquals(KeyEvent.KEYCODE_CHANNEL_UP, ConfigManager.getNextKey(context))
        assertEquals(KeyEvent.KEYCODE_CHANNEL_DOWN, ConfigManager.getPrevKey(context))

        ConfigManager.setNextKey(context, KeyEvent.KEYCODE_PROG_RED)
        ConfigManager.setPrevKey(context, KeyEvent.KEYCODE_PROG_BLUE)

        assertEquals(KeyEvent.KEYCODE_PROG_RED, ConfigManager.getNextKey(context))
        assertEquals(KeyEvent.KEYCODE_PROG_BLUE, ConfigManager.getPrevKey(context))
    }

    @Test
    fun testDockGeometryAndVisualPreferences() {
        assertEquals(100, ConfigManager.getDockScalePercent(context))
        assertEquals(35, ConfigManager.getDockTransparencyPercent(context))
        assertEquals(24, ConfigManager.getDockBottomMarginDp(context))
        assertEquals(0, ConfigManager.getDockBackgroundDimPercent(context))

        ConfigManager.setDockScalePercent(context, 125)
        ConfigManager.setDockTransparencyPercent(context, 50)
        ConfigManager.setDockBottomMarginDp(context, 40)
        ConfigManager.setDockBackgroundDimPercent(context, 20)

        assertEquals(125, ConfigManager.getDockScalePercent(context))
        assertEquals(50, ConfigManager.getDockTransparencyPercent(context))
        assertEquals(40, ConfigManager.getDockBottomMarginDp(context))
        assertEquals(20, ConfigManager.getDockBackgroundDimPercent(context))

        // Clamping boundaries
        ConfigManager.setDockScalePercent(context, 250)
        assertEquals(140, ConfigManager.getDockScalePercent(context))

        ConfigManager.setDockScalePercent(context, 10)
        assertEquals(60, ConfigManager.getDockScalePercent(context))
    }

    @Test
    fun testCustomKeyMappingsPersistence() {
        assertTrue(ConfigManager.getCustomMappings(context).isEmpty())

        ConfigManager.saveCustomMapping(context, KeyEvent.KEYCODE_PROG_YELLOW, "com.google.android.youtube.tv")
        ConfigManager.saveCustomMapping(context, KeyEvent.KEYCODE_PROG_GREEN, "com.spotify.tv.android")

        val mappings = ConfigManager.getCustomMappings(context)
        assertEquals(2, mappings.size)
        assertEquals("com.google.android.youtube.tv", mappings[KeyEvent.KEYCODE_PROG_YELLOW])
        assertEquals("com.spotify.tv.android", mappings[KeyEvent.KEYCODE_PROG_GREEN])

        ConfigManager.removeCustomMapping(context, KeyEvent.KEYCODE_PROG_YELLOW)
        val afterRemove = ConfigManager.getCustomMappings(context)
        assertEquals(1, afterRemove.size)
        assertFalse(afterRemove.containsKey(KeyEvent.KEYCODE_PROG_YELLOW))
        assertTrue(afterRemove.containsKey(KeyEvent.KEYCODE_PROG_GREEN))
    }

    @Test
    fun testCustomAppOrderPersistence() {
        val testOrder = listOf("com.netflix.ninja", "com.google.android.youtube.tv", "com.spotify.tv.android")
        ConfigManager.setCustomAppOrder(context, testOrder)

        val retrieved = ConfigManager.getCustomAppOrder(context)
        assertEquals(testOrder, retrieved)
    }

    @Test
    fun testEnabledPackagesPersistence() {
        val enabledSet = setOf("com.netflix.ninja", "com.google.android.youtube.tv")
        ConfigManager.setEnabledPackages(context, enabledSet)

        val retrieved = ConfigManager.getEnabledPackages(context)
        assertEquals(enabledSet, retrieved)
    }

    @Test
    fun testRecencyRecording() {
        assertTrue(ConfigManager.getAppRecency(context).isEmpty())

        ConfigManager.recordAppUsed(context, "com.netflix.ninja")
        val recency = ConfigManager.getAppRecency(context)
        assertEquals(1, recency.size)
        assertTrue(recency.containsKey("com.netflix.ninja"))
        assertTrue(recency["com.netflix.ninja"]!! > 0L)
    }

    @Test
    fun testTogglesPersistence() {
        assertTrue(ConfigManager.isSoundEffectsEnabled(context))
        assertTrue(ConfigManager.isCountdownBarEnabled(context))
        assertTrue(ConfigManager.isShowAppNameEnabled(context))
        assertFalse(ConfigManager.isSingleKeyMode(context))

        ConfigManager.setSoundEffectsEnabled(context, false)
        ConfigManager.setCountdownBarEnabled(context, false)
        ConfigManager.setShowAppNameEnabled(context, false)
        ConfigManager.setSingleKeyMode(context, true)

        assertFalse(ConfigManager.isSoundEffectsEnabled(context))
        assertFalse(ConfigManager.isCountdownBarEnabled(context))
        assertFalse(ConfigManager.isShowAppNameEnabled(context))
        assertTrue(ConfigManager.isSingleKeyMode(context))
    }
}
