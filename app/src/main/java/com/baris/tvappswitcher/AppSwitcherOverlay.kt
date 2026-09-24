package com.baris.tvappswitcher

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class AppSwitcherOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isVisible = false
    private var isDismissing = false
    private var currentApps: List<AppInfo> = emptyList()
    private var selectedIndex = 0
    private var onAppChosen: ((AppInfo) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable {
        confirmAndLaunch()
    }

    private var llDockTooltipContainer: LinearLayout? = null
    private var tvDockTooltip: TextView? = null
    private var vDockCountdown: View? = null
    private var hsvOverlay: HorizontalScrollView? = null
    private var llApps: LinearLayout? = null
    private val itemViews = mutableListOf<View>()
    private var isFirstTooltipPlacement = true

    fun isShowing(): Boolean = isVisible && !isDismissing

    fun show(apps: List<AppInfo>, initialIndex: Int, onSelected: (AppInfo) -> Unit) {
        if (apps.isEmpty()) return
        currentApps = apps
        selectedIndex = initialIndex.coerceIn(0, apps.size - 1)
        onAppChosen = onSelected

        mainHandler.post {
            if (isDismissing) {
                overlayView?.animate()?.cancel()
                isDismissing = false
            }
            if (!isVisible || overlayView?.isAttachedToWindow != true) {
                createAndAddView()
            }
            populateApps()
            updateSelection()
            resetAutoDismissTimer()
        }
    }

    fun next() {
        if (!isVisible || isDismissing || currentApps.isEmpty()) return
        mainHandler.post {
            selectedIndex = (selectedIndex + 1) % currentApps.size
            updateSelection()
            resetAutoDismissTimer()
        }
    }

    fun prev() {
        if (!isVisible || isDismissing || currentApps.isEmpty()) return
        mainHandler.post {
            selectedIndex = if (selectedIndex - 1 < 0) currentApps.size - 1 else selectedIndex - 1
            updateSelection()
            resetAutoDismissTimer()
        }
    }

    fun confirmAndLaunch() {
        mainHandler.post {
            if (!isVisible || isDismissing) return@post
            val chosen = currentApps.getOrNull(selectedIndex)
            dismissInternal {
                chosen?.let { onAppChosen?.invoke(it) }
            }
        }
    }

    fun dismiss() {
        mainHandler.post {
            dismissInternal()
        }
    }

    private fun dismissInternal(onComplete: (() -> Unit)? = null) {
        mainHandler.removeCallbacks(autoDismissRunnable)
        vDockCountdown?.animate()?.cancel()
        val view = overlayView
        if (!isVisible || view == null || isDismissing) {
            onComplete?.invoke()
            return
        }
        isDismissing = true
        val density = context.resources.displayMetrics.density

        view.animate()
            .translationY(42f * density)
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(120)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                try {
                    if (view.isAttachedToWindow) {
                        windowManager.removeView(view)
                    }
                } catch (e: Exception) {
                    Log.w("AppSwitcherOverlay", "Error detaching overlay view: ${e.message}")
                }
                isVisible = false
                isDismissing = false
                isFirstTooltipPlacement = true
                onComplete?.invoke()
            }
            .start()
    }

    @Suppress("DEPRECATION")
    private fun createAndAddView() {
        val density = context.resources.displayMetrics.density
        val scale = ConfigManager.getDockScalePercent(context) / 100f
        val bottomMarginDp = ConfigManager.getDockBottomMarginDp(context)
        val dimPercent = ConfigManager.getDockBackgroundDimPercent(context)

        val view = overlayView ?: LayoutInflater.from(context).inflate(R.layout.layout_switcher_overlay, null).also {
            overlayView = it
            llDockTooltipContainer = it.findViewById(R.id.llDockTooltipContainer)
            tvDockTooltip = it.findViewById(R.id.tvDockTooltip)
            vDockCountdown = it.findViewById(R.id.vDockCountdown)
            hsvOverlay = it.findViewById(R.id.hsvOverlay)
            llApps = it.findViewById(R.id.llOverlayApps)

            // API 21+ compatible scroll listener
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                hsvOverlay?.setOnScrollChangeListener { _, _, _, _, _ ->
                    syncTooltipPosition(animate = false)
                }
            } else {
                hsvOverlay?.viewTreeObserver?.addOnScrollChangedListener {
                    syncTooltipPosition(animate = false)
                }
            }
        }

        val flDockPill = view.findViewById<LiquidDockView>(R.id.flDockPill)
        flDockPill?.refreshCustomization()

        val cornerPad = (24f * density * scale).toInt()
        hsvOverlay?.setPadding(cornerPad, 0, cornerPad, 0)

        flDockPill?.setPadding(
            (26f * density * scale).toInt(),
            (12f * density * scale).toInt(),
            (26f * density * scale).toInt(),
            (10f * density * scale).toInt()
        )
        val tooltipParams = llDockTooltipContainer?.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        tooltipParams?.bottomMargin = (20f * density * scale).toInt()
        llDockTooltipContainer?.layoutParams = tooltipParams

        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            (bottomMarginDp * density).toInt()
        )

        val dimFlag = if (dimPercent > 0) WindowManager.LayoutParams.FLAG_DIM_BEHIND else 0
        @Suppress("InlinedApi")
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    dimFlag,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            dimAmount = (dimPercent / 100f).coerceIn(0f, 0.8f)
            y = 0
        }

        view.translationY = 48f * density
        view.alpha = 0f
        view.scaleX = 0.96f
        view.scaleY = 0.96f

        try {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, params)
            } else {
                windowManager.addView(view, params)
            }
            isVisible = true
            isDismissing = false
            isFirstTooltipPlacement = true

            view.animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160)
                .setInterpolator(DecelerateInterpolator(1.6f))
                .start()

            Log.d("AppSwitcherOverlay", "Dock overlay view added successfully with entrance animation")
        } catch (e: Exception) {
            Log.e("AppSwitcherOverlay", "Failed to add dock overlay view", e)
            isVisible = false
            isDismissing = false
        }
    }

    private fun populateApps() {
        val container = llApps ?: return

        val scale = ConfigManager.getDockScalePercent(context) / 100f
        val density = context.resources.displayMetrics.density
        val iconSize = (66f * density * scale).toInt()
        val padH = (8f * density * scale).toInt()
        val padV = (8f * density * scale).toInt()

        val canReuse = itemViews.size == currentApps.size && currentApps.indices.all { i ->
            (itemViews[i].tag as? String) == currentApps[i].packageName
        }

        if (canReuse) {
            for (i in currentApps.indices) {
                val itemView = itemViews[i]
                itemView.setPadding(padH, padV, padH, padV)
                val flIcon = itemView.findViewById<View>(R.id.flIconContainer)
                val flParams = flIcon.layoutParams
                if (flParams.width != iconSize || flParams.height != iconSize) {
                    flParams.width = iconSize
                    flParams.height = iconSize
                    flIcon.layoutParams = flParams
                }
            }
            return
        }

        container.removeAllViews()
        itemViews.clear()

        val inflater = LayoutInflater.from(context)
        for (i in currentApps.indices) {
            val app = currentApps[i]
            val itemView = inflater.inflate(R.layout.item_overlay_app, container, false)
            itemView.tag = app.packageName
            itemView.setPadding(padH, padV, padH, padV)
            itemView.alpha = 1.0f

            val flIcon = itemView.findViewById<View>(R.id.flIconContainer)
            val flParams = flIcon.layoutParams
            flParams.width = iconSize
            flParams.height = iconSize
            flIcon.layoutParams = flParams
            flIcon.alpha = 1.0f

            val ivIcon = itemView.findViewById<ImageView>(R.id.ivOverlayAppIcon)
            ivIcon.setImageDrawable(app.icon)
            ivIcon.alpha = 1.0f

            container.addView(itemView)
            itemViews.add(itemView)
        }
    }

    private fun updateSelection() {
        val currentApp = currentApps.getOrNull(selectedIndex)
        val tooltip = tvDockTooltip
        val tooltipContainer = llDockTooltipContainer
        val selectedView = itemViews.getOrNull(selectedIndex)
        val density = context.resources.displayMetrics.density
        val scale = ConfigManager.getDockScalePercent(context) / 100f

        // Update floating dock tooltip text & position
        if (currentApp != null && tooltip != null && tooltipContainer != null) {
            tooltip.text = currentApp.name
            tooltipContainer.visibility = View.VISIBLE
            tooltipContainer.alpha = 1.0f

            selectedView?.post {
                syncTooltipPosition(animate = !isFirstTooltipPlacement)
                isFirstTooltipPlacement = false
            }
        }

        // Animate dock items: fluid magnification bell-curve & lift effect
        for (i in itemViews.indices) {
            val view = itemViews[i]
            val dist = kotlin.math.abs(i - selectedIndex)
            val flIcon = view.findViewById<View>(R.id.flIconContainer)

            val targetScale: Float
            val targetLiftY: Float
            val targetElevation: Float

            when (dist) {
                0 -> {
                    targetScale = 1.38f
                    targetLiftY = -16f * density * scale
                    targetElevation = 20f
                }
                1 -> {
                    targetScale = 1.18f
                    targetLiftY = -7f * density * scale
                    targetElevation = 14f
                }
                2 -> {
                    targetScale = 1.06f
                    targetLiftY = -2.5f * density * scale
                    targetElevation = 8f
                }
                else -> {
                    targetScale = 1.0f
                    targetLiftY = 0f
                    targetElevation = 2f
                }
            }

            view.elevation = targetElevation
            view.alpha = 1.0f

            flIcon?.let { iconView ->
                iconView.alpha = 1.0f

                if (iconView.width > 0 && iconView.height > 0) {
                    iconView.pivotX = iconView.width / 2f
                    iconView.pivotY = iconView.height * 0.95f
                } else {
                    iconView.post {
                        iconView.pivotX = iconView.width / 2f
                        iconView.pivotY = iconView.height * 0.95f
                    }
                }

                // Explicit Interpolator interface type ensures 100% API 21+ backwards-compatibility
                val interpolator: android.view.animation.Interpolator = if (dist == 0) {
                    OvershootInterpolator(1.15f)
                } else {
                    DecelerateInterpolator(1.5f)
                }

                iconView.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationY(targetLiftY)
                    .setDuration(180)
                    .setInterpolator(interpolator)
                    .start()
            }

            if (dist == 0) {
                hsvOverlay?.post {
                    val hsv = hsvOverlay ?: return@post
                    val scrollX = view.left - (hsv.width / 2) + (view.width / 2)
                    hsv.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
                }
            }
        }
    }

    private fun syncTooltipPosition(animate: Boolean) {
        val container = llDockTooltipContainer ?: return
        val root = overlayView ?: return
        val selectedView = itemViews.getOrNull(selectedIndex) ?: return

        container.post {
            val itemLoc = IntArray(2)
            selectedView.getLocationInWindow(itemLoc)
            val rootLoc = IntArray(2)
            root.getLocationInWindow(rootLoc)

            val rootWidth = if (root.width > 0) root.width.toFloat() else context.resources.displayMetrics.widthPixels.toFloat()
            val rootCenterX = rootWidth / 2f
            val itemCenterX = (itemLoc[0] - rootLoc[0]) + (selectedView.width / 2f)
            val targetTranslationX = itemCenterX - rootCenterX

            if (animate) {
                container.animate()
                    .translationX(targetTranslationX)
                    .alpha(1f)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            } else {
                container.translationX = targetTranslationX
            }
        }
    }

    private fun resetAutoDismissTimer() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        val delaySec = ConfigManager.getSwitchDelaySeconds(context)
        val countdownView = vDockCountdown

        if (delaySec > 0) {
            mainHandler.postDelayed(autoDismissRunnable, delaySec * 1000L)
            countdownView?.let { cv ->
                cv.visibility = View.VISIBLE
                cv.animate()?.cancel()
                cv.scaleX = 1f
                cv.pivotX = 0f
                cv.animate()
                    .scaleX(0f)
                    .setDuration(delaySec * 1000L)
                    .setInterpolator(android.view.animation.LinearInterpolator())
                    .start()
            }
        } else {
            countdownView?.visibility = View.GONE
        }
    }
}
