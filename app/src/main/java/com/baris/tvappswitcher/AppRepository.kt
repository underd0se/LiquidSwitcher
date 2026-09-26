package com.baris.tvappswitcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance in-memory repository for installed TV applications.
 *
 * Pre-fetches and caches app metadata and icons asynchronously off the UI thread
 * so that opening the dock overlay or triggering app switching happens in 0ms (zero jank).
 * Automatically monitors package addition, removal, and updates via broadcast.
 */
object AppRepository {

    private const val TAG = "AppRepository"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var cachedAllApps: List<AppInfo> = emptyList()

    private val iconCache = android.util.LruCache<String, android.graphics.drawable.Drawable>(30)

    private val isInitialized = AtomicBoolean(false)
    private val isFetching = AtomicBoolean(false)

    private val listeners = mutableListOf<() -> Unit>()

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Package change detected: ${intent?.action} for ${intent?.data?.schemeSpecificPart}")
            context?.let { refreshAppsAsync(it.applicationContext) }
        }
    }

    fun init(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            ContextCompat.registerReceiver(
                context.applicationContext,
                packageChangeReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            refreshAppsAsync(context.applicationContext)
        }
    }

    fun addListener(listener: () -> Unit) {
        synchronized(listeners) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: () -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    fun getAllApps(context: Context): List<AppInfo> {
        if (!isInitialized.get()) {
            init(context)
        }
        val cached = cachedAllApps
        if (cached.isEmpty()) {
            // Synchronous fallback only if cache has never been populated
            val loaded = loadAppsFromPackageManager(context.applicationContext)
            cachedAllApps = loaded
            return loaded
        }
        return cached
    }

    fun getFilteredAndSortedApps(context: Context): List<AppInfo> {
        val allApps = getAllApps(context)
        val enabledFilter = ConfigManager.getEnabledPackages(context)
        val filtered = if (enabledFilter != null && enabledFilter.isNotEmpty()) {
            allApps.filter { enabledFilter.contains(it.packageName) }
        } else {
            allApps
        }
        return ConfigManager.sortApps(context, filtered)
    }

    fun refreshAppsAsync(context: Context, onComplete: (() -> Unit)? = null) {
        if (isFetching.getAndSet(true)) {
            onComplete?.let { mainHandler.post(it) }
            return
        }

        executor.execute {
            try {
                val loaded = loadAppsFromPackageManager(context.applicationContext)
                cachedAllApps = loaded
                pruneUninstalledPackages(context.applicationContext, loaded.map { it.packageName }.toSet())
                Log.d(TAG, "Cached ${loaded.size} TV apps successfully in background")

                mainHandler.post {
                    onComplete?.invoke()
                    synchronized(listeners) {
                        listeners.forEach { it.invoke() }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading installed TV apps in background", e)
            } finally {
                isFetching.set(false)
            }
        }
    }

    private fun pruneUninstalledPackages(context: Context, installedPackages: Set<String>) {
        if (installedPackages.isEmpty()) return
        try {
            val currentOrder = ConfigManager.getCustomAppOrder(context)
            if (currentOrder.isNotEmpty()) {
                val prunedOrder = currentOrder.filter { installedPackages.contains(it) }
                if (prunedOrder.size != currentOrder.size) {
                    ConfigManager.setCustomAppOrder(context, prunedOrder)
                }
            }

            val enabledPackages = ConfigManager.getEnabledPackages(context)
            if (enabledPackages != null) {
                val prunedEnabled = enabledPackages.filter { installedPackages.contains(it) }.toSet()
                if (prunedEnabled.size != enabledPackages.size) {
                    ConfigManager.setEnabledPackages(context, prunedEnabled)
                }
            }

            val customMappings = ConfigManager.getCustomMappings(context)
            if (customMappings.isNotEmpty()) {
                val invalidKeys = customMappings.filter { !installedPackages.contains(it.value) }.keys
                invalidKeys.forEach { ConfigManager.removeCustomMapping(context, it) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pruning uninstalled packages", e)
        }
    }

    fun getAppIcon(context: Context, app: AppInfo): android.graphics.drawable.Drawable? {
        app.icon?.let { return it }
        val cached = iconCache.get(app.packageName)
        if (cached != null) {
            app.icon = cached
            return cached
        }
        
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            val drawable = if (intent != null) {
                val ri = pm.resolveActivity(intent, 0)
                ri?.activityInfo?.loadIcon(pm) ?: pm.getApplicationIcon(app.packageName)
            } else {
                pm.getApplicationIcon(app.packageName)
            }
            iconCache.put(app.packageName, drawable)
            app.icon = drawable
            drawable
        } catch (e: Exception) {
            null
        }
    }

    private fun loadAppsFromPackageManager(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val appsMap = mutableMapOf<String, AppInfo>()
        val selfPkg = context.packageName

        // 1. Query Leanback Launcher apps (primary TV interface apps)
        val leanbackIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val leanbackList = pm.queryIntentActivities(leanbackIntent, 0)
        for (ri in leanbackList) {
            val pkg = ri.activityInfo.packageName
            if (pkg == selfPkg) continue
            try {
                val rawLabel = try { ri.loadLabel(pm).toString() } catch (e: Exception) { "" }
                val appLabel = try { pm.getApplicationLabel(ri.activityInfo.applicationInfo).toString() } catch (e: Exception) { "" }
                val label = when {
                    appLabel.isNotBlank() && !appLabel.contains('.') -> appLabel
                    rawLabel.isNotBlank() && !rawLabel.contains('.') -> rawLabel
                    rawLabel.isNotBlank() -> rawLabel.substringAfterLast('.')
                    else -> pkg.substringAfterLast('.')
                }
                val actName = ri.activityInfo.name ?: ""
                if (ConfigManager.isSystemExcludedApp(pkg, actName, label)) continue

                appsMap[pkg] = AppInfo(label, pkg, null)
            } catch (e: Exception) {
                // Ignore single package resolution failure
            }
        }

        // 2. Query Standard Launcher apps as fallback (sideloaded or non-leanback apps)
        val standardIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val standardList = pm.queryIntentActivities(standardIntent, 0)
        for (ri in standardList) {
            val pkg = ri.activityInfo.packageName
            if (pkg == selfPkg || appsMap.containsKey(pkg)) continue
            try {
                val rawLabel = try { ri.loadLabel(pm).toString() } catch (e: Exception) { "" }
                val appLabel = try { pm.getApplicationLabel(ri.activityInfo.applicationInfo).toString() } catch (e: Exception) { "" }
                val label = when {
                    appLabel.isNotBlank() && !appLabel.contains('.') -> appLabel
                    rawLabel.isNotBlank() && !rawLabel.contains('.') -> rawLabel
                    rawLabel.isNotBlank() -> rawLabel.substringAfterLast('.')
                    else -> pkg.substringAfterLast('.')
                }
                val actName = ri.activityInfo.name ?: ""
                if (ConfigManager.isSystemExcludedApp(pkg, actName, label)) continue

                appsMap[pkg] = AppInfo(label, pkg, null)
            } catch (e: Exception) {
                // Ignore single package resolution failure
            }
        }

        return appsMap.values.toList()
    }
}
