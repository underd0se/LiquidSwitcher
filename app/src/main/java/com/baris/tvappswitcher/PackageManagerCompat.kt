package com.baris.tvappswitcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

/**
 * Backward-compatible extensions for PackageManager across Android 8.0 (API 26) through Android 15 (API 35).
 * Uses modern ResolveInfoFlags on Android 13+ (API 33) and legacy integer flags on earlier versions.
 */
fun PackageManager.compatQueryIntentActivities(intent: Intent, flags: Int = 0): List<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, flags)
    }
}

fun PackageManager.compatResolveActivity(intent: Intent, flags: Int = 0): ResolveInfo? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        resolveActivity(intent, flags)
    }
}
