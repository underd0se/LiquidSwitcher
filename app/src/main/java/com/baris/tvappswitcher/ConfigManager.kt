package com.baris.tvappswitcher

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ConfigManager {
    private const val PREFS_NAME = "tv_switcher_prefs"
    private const val KEY_NEXT = "key_switch_next"
    private const val KEY_PREV = "key_switch_prev"
    private const val KEY_SWITCH_DELAY = "switch_delay_seconds"
    private const val KEY_CUSTOM_MAPPINGS = "custom_mappings"
    private const val KEY_ENABLED_PACKAGES = "enabled_packages"
    private const val KEY_IS_LISTENING = "is_listening"
    private const val KEY_LISTENING_TARGET = "listening_target"
    private const val KEY_SORT_MODE = "app_sort_mode"
    private const val KEY_CUSTOM_APP_ORDER = "custom_app_order"
    private const val KEY_APP_RECENCY = "app_recency_timestamps"
    private const val KEY_DOCK_SCALE = "dock_scale_percent"
    private const val KEY_DOCK_TRANSPARENCY = "dock_transparency_percent"
    private const val KEY_SOUND_EFFECTS_ENABLED = "sound_effects_enabled"
    private const val KEY_DOCK_BOTTOM_MARGIN = "dock_bottom_margin_dp"
    private const val KEY_DOCK_BACKGROUND_DIM = "dock_background_dim_percent"
    private const val KEY_SINGLE_KEY_MODE = "single_key_mode"
    private const val KEY_SHOW_COUNTDOWN_BAR = "show_countdown_bar"

    enum class SortMode(val key: String, val label: String) {
        NAME("NAME", "Alphabetical (A - Z)"),
        RECENCY("RECENCY", "Recency (Most Recent First)"),
        CUSTOM("CUSTOM", "Custom Order");

        companion object {
            fun fromKey(key: String?): SortMode {
                return values().find { it.key.equals(key, ignoreCase = true) } ?: NAME
            }
        }
    }

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getNextKey(context: Context): Int {
        return getPrefs(context).getInt(KEY_NEXT, KeyEvent.KEYCODE_CHANNEL_UP)
    }

    fun setNextKey(context: Context, keyCode: Int) {
        getPrefs(context).edit().putInt(KEY_NEXT, keyCode).apply()
    }

    fun getPrevKey(context: Context): Int {
        return getPrefs(context).getInt(KEY_PREV, KeyEvent.KEYCODE_CHANNEL_DOWN)
    }

    fun setPrevKey(context: Context, keyCode: Int) {
        getPrefs(context).edit().putInt(KEY_PREV, keyCode).apply()
    }

    fun getSwitchDelaySeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_SWITCH_DELAY, 2)
    }

    fun setSwitchDelaySeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_SWITCH_DELAY, seconds).apply()
    }

    fun getSwitchDelayLabel(seconds: Int): String {
        return when (seconds) {
            0 -> "Manual (Press OK to Switch)"
            1 -> "1 second"
            else -> "$seconds seconds"
        }
    }

    fun getSwitchDelayShortLabel(seconds: Int): String {
        return when (seconds) {
            0 -> "Manual"
            else -> "${seconds}s"
        }
    }

    fun getSortMode(context: Context): SortMode {
        val key = getPrefs(context).getString(KEY_SORT_MODE, SortMode.NAME.key)
        return SortMode.fromKey(key)
    }

    fun setSortMode(context: Context, mode: SortMode) {
        getPrefs(context).edit().putString(KEY_SORT_MODE, mode.key).apply()
    }

    fun getCustomAppOrder(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_CUSTOM_APP_ORDER, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setCustomAppOrder(context: Context, order: List<String>) {
        getPrefs(context).edit().putString(KEY_CUSTOM_APP_ORDER, Gson().toJson(order)).apply()
    }

    fun getAppRecency(context: Context): Map<String, Long> {
        val json = getPrefs(context).getString(KEY_APP_RECENCY, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun recordAppUsed(context: Context, packageName: String) {
        val map = getAppRecency(context).toMutableMap()
        map[packageName] = System.currentTimeMillis()
        getPrefs(context).edit().putString(KEY_APP_RECENCY, Gson().toJson(map)).apply()
    }

    fun sortApps(context: Context, apps: List<AppInfo>): List<AppInfo> {
        return when (getSortMode(context)) {
            SortMode.NAME -> {
                apps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            }
            SortMode.RECENCY -> {
                val recencyMap = getAppRecency(context)
                apps.sortedWith(
                    compareByDescending<AppInfo> { recencyMap[it.packageName] ?: 0L }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                )
            }
            SortMode.CUSTOM -> {
                val customOrder = getCustomAppOrder(context)
                apps.sortedWith(
                    compareBy<AppInfo> {
                        val idx = customOrder.indexOf(it.packageName)
                        if (idx >= 0) idx else Int.MAX_VALUE
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                )
            }
        }
    }

    fun getCustomMappings(context: Context): Map<Int, String> {
        val json = getPrefs(context).getString(KEY_CUSTOM_MAPPINGS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<Int, String>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveCustomMapping(context: Context, keyCode: Int, packageName: String) {
        val mappings = getCustomMappings(context).toMutableMap()
        mappings[keyCode] = packageName
        getPrefs(context).edit().putString(KEY_CUSTOM_MAPPINGS, Gson().toJson(mappings)).apply()
    }

    fun removeCustomMapping(context: Context, keyCode: Int) {
        val mappings = getCustomMappings(context).toMutableMap()
        mappings.remove(keyCode)
        getPrefs(context).edit().putString(KEY_CUSTOM_MAPPINGS, Gson().toJson(mappings)).apply()
    }

    fun getEnabledPackages(context: Context): Set<String>? {
        return getPrefs(context).getStringSet(KEY_ENABLED_PACKAGES, null)
    }

    fun setEnabledPackages(context: Context, packages: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_ENABLED_PACKAGES, packages).apply()
    }

    fun setListening(context: Context, listening: Boolean, target: String = "") {
        getPrefs(context).edit()
            .putBoolean(KEY_IS_LISTENING, listening)
            .putString(KEY_LISTENING_TARGET, target)
            .apply()
    }

    fun isListening(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LISTENING, false)
    }

    fun getListeningTarget(context: Context): String {
        return getPrefs(context).getString(KEY_LISTENING_TARGET, "") ?: ""
    }

    fun getKeyLabel(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP -> "Program Up (CH+)"
            KeyEvent.KEYCODE_CHANNEL_DOWN -> "Program Down (CH-)"
            KeyEvent.KEYCODE_PROG_RED -> "Red Button"
            KeyEvent.KEYCODE_PROG_GREEN -> "Green Button"
            KeyEvent.KEYCODE_PROG_YELLOW -> "Yellow Button"
            KeyEvent.KEYCODE_PROG_BLUE -> "Blue Button"
            KeyEvent.KEYCODE_0 -> "Number 0"
            KeyEvent.KEYCODE_1 -> "Number 1"
            KeyEvent.KEYCODE_2 -> "Number 2"
            KeyEvent.KEYCODE_3 -> "Number 3"
            KeyEvent.KEYCODE_4 -> "Number 4"
            KeyEvent.KEYCODE_5 -> "Number 5"
            KeyEvent.KEYCODE_6 -> "Number 6"
            KeyEvent.KEYCODE_7 -> "Number 7"
            KeyEvent.KEYCODE_8 -> "Number 8"
            KeyEvent.KEYCODE_9 -> "Number 9"
            KeyEvent.KEYCODE_GUIDE -> "TV Guide / EPG"
            KeyEvent.KEYCODE_INFO -> "Info"
            KeyEvent.KEYCODE_CAPTIONS -> "Subtitles / CC"
            KeyEvent.KEYCODE_BOOKMARK -> "Bookmark / Fav"
            KeyEvent.KEYCODE_TV_INPUT -> "Source / Input"
            KeyEvent.KEYCODE_DVR -> "DVR / Recordings"
            KeyEvent.KEYCODE_SETTINGS -> "Settings"
            KeyEvent.KEYCODE_HELP -> "Help"
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "Play / Pause"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "Play"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "Pause"
            KeyEvent.KEYCODE_MEDIA_STOP -> "Stop"
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "Fast Forward"
            KeyEvent.KEYCODE_MEDIA_REWIND -> "Rewind"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "Media Next"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "Media Prev"
            KeyEvent.KEYCODE_LAST_CHANNEL -> "Last Channel / Recall"
            KeyEvent.KEYCODE_TV -> "Live TV"
            KeyEvent.KEYCODE_MENU -> "Menu"
            KeyEvent.KEYCODE_MUTE -> "Mute"
            KeyEvent.KEYCODE_VOLUME_UP -> "Volume Up"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "Volume Down"
            KeyEvent.KEYCODE_PAGE_UP -> "Page Up"
            KeyEvent.KEYCODE_PAGE_DOWN -> "Page Down"
            KeyEvent.KEYCODE_WINDOW -> "Multi-Window / Overview"
            else -> {
                val name = KeyEvent.keyCodeToString(keyCode).replace("KEYCODE_", "")
                "$name ($keyCode)"
            }
        }
    }

    fun getDockScalePercent(context: Context): Int {
        return getPrefs(context).getInt(KEY_DOCK_SCALE, 100).coerceIn(60, 140)
    }

    fun setDockScalePercent(context: Context, percent: Int) {
        getPrefs(context).edit().putInt(KEY_DOCK_SCALE, percent.coerceIn(60, 140)).apply()
    }

    fun getDockTransparencyPercent(context: Context): Int {
        return getPrefs(context).getInt(KEY_DOCK_TRANSPARENCY, 35).coerceIn(0, 90)
    }

    fun setDockTransparencyPercent(context: Context, percent: Int) {
        getPrefs(context).edit().putInt(KEY_DOCK_TRANSPARENCY, percent.coerceIn(0, 90)).apply()
    }

    fun isSoundEffectsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_EFFECTS_ENABLED, true)
    }

    fun setSoundEffectsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_EFFECTS_ENABLED, enabled).apply()
    }

    fun getDockBottomMarginDp(context: Context): Int {
        return getPrefs(context).getInt(KEY_DOCK_BOTTOM_MARGIN, 24).coerceIn(0, 72)
    }

    fun setDockBottomMarginDp(context: Context, marginDp: Int) {
        getPrefs(context).edit().putInt(KEY_DOCK_BOTTOM_MARGIN, marginDp.coerceIn(0, 72)).apply()
    }

    fun getDockBackgroundDimPercent(context: Context): Int {
        return getPrefs(context).getInt(KEY_DOCK_BACKGROUND_DIM, 0).coerceIn(0, 80)
    }

    fun setDockBackgroundDimPercent(context: Context, percent: Int) {
        getPrefs(context).edit().putInt(KEY_DOCK_BACKGROUND_DIM, percent.coerceIn(0, 80)).apply()
    }

    fun isSingleKeyMode(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SINGLE_KEY_MODE, false)
    }

    fun setSingleKeyMode(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SINGLE_KEY_MODE, enabled).apply()
    }

    fun isCountdownBarEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_COUNTDOWN_BAR, true)
    }

    fun setCountdownBarEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_COUNTDOWN_BAR, enabled).apply()
    }

    /**
     * Determines whether an application or component is an internal OEM service/stub
     * (e.g. NetflixLauncher, WebView, DisclaimerCustomization, factorymenu) that should be
     * excluded from the dock switcher.
     */
    fun isSystemExcludedApp(pkg: String, activityName: String = "", label: String = ""): Boolean {
        val lowerPkg = pkg.lowercase()
        val lowerAct = activityName.lowercase()
        val lowerLabel = label.lowercase()

        // 1. Explicit packages to exclude
        val blacklistedPackages = setOf(
            "com.mediatek.tv.service",
            "com.mediatek.wwtv.webview",
            "com.mstar.android.tv.disclaimercustomization",
            "mediatek.factorymenu.ui",
            "com.android.documentsui",
            "com.google.android.backdrop",
            "com.google.android.inputmethod.latin",
            "com.android.tv.settings",
            "com.android.settings",
            "com.mediatek.wwtv.tvcenter",
            "com.mediatek.wwtv.mediaplayer"
        )
        if (blacklistedPackages.contains(lowerPkg)) return true

        // 2. Keywords in package name, activity name, or label
        val blacklistedKeywords = listOf(
            "netflixobserver",
            "netflixlauncher",
            "disclaimercustomization",
            "factorymenu",
            "designmenu",
            "wwtv.webview",
            "tv.service",
            "documentsui",
            "turnkeyui"
        )
        for (kw in blacklistedKeywords) {
            if (lowerPkg.contains(kw) || lowerAct.contains(kw) || lowerLabel.contains(kw)) {
                return true
            }
        }

        // 3. Exclude if label specifically matches system utility stubs
        val blacklistedLabels = setOf(
            "webview",
            "netflixlauncher",
            "disclaimercustomization",
            "designmenu",
            "factorymenu",
            "tv center",
            "mediaplayer"
        )
        if (blacklistedLabels.contains(lowerLabel)) {
            return true
        }

        return false
    }
}
