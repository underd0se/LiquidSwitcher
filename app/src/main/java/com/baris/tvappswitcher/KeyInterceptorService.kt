package com.baris.tvappswitcher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class KeyInterceptorService : AccessibilityService() {

    private lateinit var overlay: AppSwitcherOverlay
    private var currentForegroundPackage: String? = null
    private var previousForegroundPackage: String? = null

    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.baris.tvappswitcher.TRIGGER_OVERLAY",
                "com.baris.tvappswitcher.TRIGGER_NEXT" -> {
                    if (overlay.isShowing()) {
                        overlay.next()
                        playNavSound()
                    } else {
                        triggerSwitcher(isNext = true)
                    }
                }
                "com.baris.tvappswitcher.TRIGGER_PREV" -> {
                    if (overlay.isShowing()) {
                        overlay.prev()
                        playNavSound()
                    } else {
                        triggerSwitcher(isNext = false)
                    }
                }
                "com.baris.tvappswitcher.TRIGGER_CONFIRM" -> {
                    if (overlay.isShowing()) {
                        overlay.confirmAndLaunch()
                    }
                }
                "com.baris.tvappswitcher.TRIGGER_DISMISS",
                @Suppress("DEPRECATION")
                Intent.ACTION_CLOSE_SYSTEM_DIALOGS,
                Intent.ACTION_SCREEN_OFF -> {
                    if (overlay.isShowing()) {
                        overlay.dismiss()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        SoundCueHelper.init(this)
        overlay = AppSwitcherOverlay(this)
        AppRepository.init(this)

        val filter = IntentFilter().apply {
            addAction("com.baris.tvappswitcher.TRIGGER_OVERLAY")
            addAction("com.baris.tvappswitcher.TRIGGER_NEXT")
            addAction("com.baris.tvappswitcher.TRIGGER_PREV")
            addAction("com.baris.tvappswitcher.TRIGGER_CONFIRM")
            addAction("com.baris.tvappswitcher.TRIGGER_DISMISS")
            @Suppress("DEPRECATION")
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this,
            controlReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundCueHelper.release()
        try {
            if (this::overlay.isInitialized) {
                overlay.destroy()
            }
        } catch (e: Exception) {
            // Ignore
        }
        try {
            unregisterReceiver(controlReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            info.feedbackType = 0
            serviceInfo = info
        } catch (e: Exception) {
            Log.e("KeyInterceptorService", "Error configuring service info", e)
        }
        Log.d("KeyInterceptorService", "Liquid Switcher Service Connected & Filter Flags Set")
        // Pre-warm the app cache so the first overlay trigger doesn't block the main thread
        AppRepository.refreshAppsAsync(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            if (pkg != null &&
                pkg != packageName &&
                !pkg.contains("systemui", ignoreCase = true) &&
                !pkg.contains("launcher", ignoreCase = true) &&
                !pkg.contains("tv.settings", ignoreCase = true) &&
                !pkg.contains("katniss", ignoreCase = true) &&
                !pkg.contains("inputmethod", ignoreCase = true) &&
                !pkg.contains("backdrop", ignoreCase = true) &&
                !pkg.contains("googlequicksearchbox", ignoreCase = true)
            ) {
                if (pkg != currentForegroundPackage) {
                    previousForegroundPackage = currentForegroundPackage
                }
                currentForegroundPackage = pkg
                ConfigManager.recordAppUsed(this, pkg)
            }
        }
    }

    override fun onInterrupt() {
        // Required callback
    }

    private var lastKeyPressTime = 0L
    private val keyRepeatThrottleMs = 140L

    @Suppress("DEPRECATION")
    private val actionCloseDialogs = Intent.ACTION_CLOSE_SYSTEM_DIALOGS

    private fun isKeyThrottled(event: KeyEvent): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastKeyPressTime < keyRepeatThrottleMs) {
            return true
        }
        lastKeyPressTime = now
        return false
    }

    private fun isStandardNavKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_HOME -> true
        else -> false
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        // Instant microsecond bypass for standard navigation keys when overlay is not showing
        if ((!this::overlay.isInitialized || !overlay.isShowing()) && !ConfigManager.isListening(this)) {
            if (isStandardNavKey(keyCode)) return false
        }

        return try {
            handleKeyEventInternal(event)
        } catch (t: Throwable) {
            Log.e("KeyInterceptorService", "Safely caught unexpected exception in onKeyEvent", t)
            false
        }
    }

    private fun handleKeyEventInternal(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        // 1. Key Configuration Mode (When user is in Settings assigning a key)
        if (ConfigManager.isListening(this)) {
            if (isStandardNavKey(keyCode)) {
                return super.onKeyEvent(event)
            }

            if (action == KeyEvent.ACTION_DOWN) {
                val intent = Intent("com.baris.tvappswitcher.KEY_CAPTURED")
                intent.setPackage(packageName)
                intent.putExtra("keyCode", keyCode)
                intent.putExtra("target", ConfigManager.getListeningTarget(this))
                sendBroadcast(intent)
                return true
            }
            if (action == KeyEvent.ACTION_UP) {
                return true
            }
        }

        val nextKey = ConfigManager.getNextKey(this)
        val prevKey = ConfigManager.getPrevKey(this)
        val customMappings = ConfigManager.getCustomMappings(this)
        val singleKeyMode = ConfigManager.isSingleKeyMode(this)

        // 2. If Overlay is Currently Showing on Screen
        if (overlay.isShowing()) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (isKeyThrottled(event)) {
                    return true
                }
                when (keyCode) {
                    nextKey, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        overlay.next()
                        playNavSound()
                        return true
                    }
                    prevKey, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> {
                        if (singleKeyMode && keyCode == prevKey) {
                            overlay.next()
                        } else {
                            overlay.prev()
                        }
                        playNavSound()
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        overlay.confirmAndLaunch()
                        return true
                    }
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                        overlay.dismiss()
                        return true
                    }
                    else -> {
                        overlay.dismiss()
                    }
                }
            } else if (action == KeyEvent.ACTION_UP) {
                val handledKeys = listOf(
                    nextKey, prevKey,
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT,
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE
                )
                if (handledKeys.contains(keyCode)) {
                    return true
                }
            }
        }

        // 3. Switcher Trigger Keys (Program Up / Program Down or Single Key)
        if (keyCode == nextKey || (!singleKeyMode && keyCode == prevKey)) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (!isKeyThrottled(event)) {
                    triggerSwitcher(isNext = (keyCode == nextKey || singleKeyMode))
                }
            }
            return true
        }

        // 4. Custom Direct Shortcuts (e.g. Color Buttons mapped to specific apps)
        if (customMappings.containsKey(keyCode)) {
            if (action == KeyEvent.ACTION_DOWN) {
                val targetPkg = customMappings[keyCode]
                if (targetPkg != null) {
                    launchApp(targetPkg)
                }
            }
            return true
        }


        return super.onKeyEvent(event)
    }

    private fun triggerSwitcher(isNext: Boolean) {
        val apps = AppRepository.getFilteredAndSortedApps(this)
        if (apps.isEmpty()) {
            android.widget.Toast.makeText(this, "No apps available in switcher", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val currentIndex = apps.indexOfFirst { it.packageName == currentForegroundPackage }
            .takeIf { it >= 0 } ?: 0

        val targetIndex = if (isNext) {
            (currentIndex + 1) % apps.size
        } else {
            if (currentIndex - 1 < 0) apps.size - 1 else currentIndex - 1
        }

        playNavSound()

        overlay.show(apps, targetIndex) { selectedApp ->
            launchApp(selectedApp.packageName)
        }
    }

    private fun playNavSound() {
        SoundCueHelper.playTick(this)
    }

    private fun launchApp(targetPackage: String) {
        try {
            currentForegroundPackage = targetPackage
            ConfigManager.recordAppUsed(this, targetPackage)
            val pm = packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(targetPackage)
                ?: pm.getLaunchIntentForPackage(targetPackage)

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                startActivity(intent)
            } else {
                // Fallback for non-standard or sideloaded TV apps without default launch intent
                val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    setPackage(targetPackage)
                }
                val list = pm.compatQueryIntentActivities(leanbackIntent, 0)
                val targetInfo = list.firstOrNull() ?: pm.compatQueryIntentActivities(
                    Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setPackage(targetPackage)
                    }, 0
                ).firstOrNull()

                if (targetInfo != null) {
                    val explicitIntent = Intent().apply {
                        setClassName(targetInfo.activityInfo.packageName, targetInfo.activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    startActivity(explicitIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("KeyInterceptorService", "Failed to launch $targetPackage", e)
            try {
                android.widget.Toast.makeText(this, "App not available", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }
}
