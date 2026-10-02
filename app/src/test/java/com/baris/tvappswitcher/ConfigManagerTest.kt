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
class ConfigManagerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSortModeFromKey() {
        assertEquals(ConfigManager.SortMode.NAME, ConfigManager.SortMode.fromKey("NAME"))
        assertEquals(ConfigManager.SortMode.NAME, ConfigManager.SortMode.fromKey("name"))
        assertEquals(ConfigManager.SortMode.RECENCY, ConfigManager.SortMode.fromKey("RECENCY"))
        assertEquals(ConfigManager.SortMode.RECENCY, ConfigManager.SortMode.fromKey("recency"))
        assertEquals(ConfigManager.SortMode.CUSTOM, ConfigManager.SortMode.fromKey("CUSTOM"))
        assertEquals(ConfigManager.SortMode.CUSTOM, ConfigManager.SortMode.fromKey("custom"))

        // Fallbacks
        assertEquals(ConfigManager.SortMode.NAME, ConfigManager.SortMode.fromKey(null))
        assertEquals(ConfigManager.SortMode.NAME, ConfigManager.SortMode.fromKey("UNKNOWN_KEY"))
    }

    @Test
    fun testKeyLabels() {
        assertEquals("Program Up (CH+)", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_CHANNEL_UP))
        assertEquals("Program Down (CH-)", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_CHANNEL_DOWN))
        assertEquals("Red Button", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_PROG_RED))
        assertEquals("Green Button", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_PROG_GREEN))
        assertEquals("Yellow Button", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_PROG_YELLOW))
        assertEquals("Blue Button", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_PROG_BLUE))
        assertEquals("Settings", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_SETTINGS))
        assertEquals("TV Guide / EPG", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_GUIDE))
        assertEquals("Play / Pause", ConfigManager.getKeyLabel(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
    }

    @Test
    fun testIsSystemExcludedApp() {
        // System and OEM blacklisted packages
        assertTrue(ConfigManager.isSystemExcludedApp(context, "com.mediatek.tv.service"))
        assertTrue(ConfigManager.isSystemExcludedApp(context, "com.mediatek.wwtv.webview"))
        assertTrue(ConfigManager.isSystemExcludedApp(context, "mediatek.factorymenu.ui"))
        assertTrue(ConfigManager.isSystemExcludedApp(context, "com.google.android.backdrop"))
        assertTrue(ConfigManager.isSystemExcludedApp(context, "com.android.tv.settings"))

        // Keyword based exclusions
        assertTrue(ConfigManager.isSystemExcludedApp(context, "com.oem.netflixlauncher"))
        assertTrue(ConfigManager.isSystemExcludedApp(context, "com.oem.disclaimercustomization"))
        assertTrue(ConfigManager.isSystemExcludedApp(context, "com.mediatek.turnkeyui"))

        // Standard user-facing TV apps should NOT be excluded
        assertFalse(ConfigManager.isSystemExcludedApp(context, "com.netflix.ninja", "NetflixActivity", "Netflix"))
        assertFalse(ConfigManager.isSystemExcludedApp(context, "com.google.android.youtube.tv", "MainActivity", "YouTube"))
        assertFalse(ConfigManager.isSystemExcludedApp(context, "com.spotify.tv.android", "SpotifyActivity", "Spotify"))
        assertFalse(ConfigManager.isSystemExcludedApp(context, "com.amazon.amazonvideo.livingroom", "MainActivity", "Prime Video"))
        assertFalse(ConfigManager.isSystemExcludedApp(context, "tv.twitch.android.app", "TwitchActivity", "Twitch"))
    }
}
