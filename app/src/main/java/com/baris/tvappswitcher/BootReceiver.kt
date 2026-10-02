package com.baris.tvappswitcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        val action = intent?.action ?: ""
        val isRelevantAction = action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (isRelevantAction) {
            Log.d("BootReceiver", "Broadcast received ($action). Pre-warming AppRepository...")
            context?.let { ctx ->
                AppRepository.init(ctx.applicationContext)
                // On Android 14+, ensure accessibility service is re-enabled after update/boot
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ensureAccessibilityServiceEnabled(ctx, action)
                }
            }
        }
    }

    private fun ensureAccessibilityServiceEnabled(context: Context, action: String) {
        try {
            val serviceComponent = "${context.packageName}/com.baris.tvappswitcher.KeyInterceptorService"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: ""

            if (!enabledServices.contains(serviceComponent)) {
                Log.w("BootReceiver", "Accessibility service not enabled after $action, cannot auto-enable due to Android restrictions")
            }
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error checking accessibility service state", e)
        }
    }
}
