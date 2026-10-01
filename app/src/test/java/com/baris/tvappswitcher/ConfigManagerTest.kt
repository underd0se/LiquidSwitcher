package com.baris.tvappswitcher

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigManagerTest {
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

    // Note: getSwitchDelayLabel() and getSwitchDelayShortLabel() now require
    // a Context parameter for localized string resolution. These tests need
    // an instrumented test or Robolectric to provide a mock Context.

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
        assertTrue(ConfigManager.isSystemExcludedApp("com.mediatek.tv.service"))
        assertTrue(ConfigManager.isSystemExcludedApp("com.mediatek.wwtv.webview"))
        assertTrue(ConfigManager.isSystemExcludedApp("mediatek.factorymenu.ui"))
        assertTrue(ConfigManager.isSystemExcludedApp("com.google.android.backdrop"))
        assertTrue(ConfigManager.isSystemExcludedApp("com.android.tv.settings"))

        // Keyword based exclusions
        assertTrue(ConfigManager.isSystemExcludedApp("com.oem.netflixlauncher"))
        assertTrue(ConfigManager.isSystemExcludedApp("com.oem.disclaimercustomization"))
        assertTrue(ConfigManager.isSystemExcludedApp("com.mediatek.turnkeyui"))

        // Standard user-facing TV apps should NOT be excluded
        assertFalse(ConfigManager.isSystemExcludedApp("com.netflix.ninja", "NetflixActivity", "Netflix"))
        assertFalse(ConfigManager.isSystemExcludedApp("com.google.android.youtube.tv", "MainActivity", "YouTube"))
        assertFalse(ConfigManager.isSystemExcludedApp("com.spotify.tv.android", "SpotifyActivity", "Spotify"))
        assertFalse(ConfigManager.isSystemExcludedApp("com.amazon.amazonvideo.livingroom", "MainActivity", "Prime Video"))
        assertFalse(ConfigManager.isSystemExcludedApp("tv.twitch.android.app", "TwitchActivity", "Twitch"))
    }
}
