package com.baris.tvappswitcher

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce

class AppSwitcherOverlay(
    private val context: Context,
) {
    private class IconSprings(
        private val targetView: View,
    ) {
        private fun createSpring(property: DynamicAnimation.ViewProperty) = SpringAnimation(targetView, property).apply {
            spring =
                SpringForce().apply {
                    dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                    stiffness = 900f
                }
        }

        private val scaleX = createSpring(DynamicAnimation.SCALE_X)
        private val scaleY = createSpring(DynamicAnimation.SCALE_Y)
        private val transX = createSpring(DynamicAnimation.TRANSLATION_X)
        private val transY = createSpring(DynamicAnimation.TRANSLATION_Y)

        fun animateTo(
            targetScale: Float,
            targetX: Float,
            targetY: Float,
        ) {
            scaleX.animateToFinalPosition(targetScale)
            scaleY.animateToFinalPosition(targetScale)
            transX.animateToFinalPosition(targetX)
            transY.animateToFinalPosition(targetY)
        }

        fun cancel() {
            scaleX.cancel()
            scaleY.cancel()
            transX.cancel()
            transY.cancel()
        }
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var isVisible = false
    private var isDismissing = false
    private var currentApps: List<AppInfo> = emptyList()
    private var selectedIndex = 0
    private var onAppChosen: ((AppInfo) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable =
        Runnable {
            confirmAndLaunch()
        }

    private var llDockTooltipContainer: LinearLayout? = null
    private var tvDockTooltip: TextView? = null
    private var vDockCountdown: View? = null
    private var hsvOverlay: HorizontalScrollView? = null
    private var llApps: LinearLayout? = null
    private val itemViews = mutableListOf<View>()
    private val iconSprings = mutableListOf<IconSprings>()
    private var isFirstTooltipPlacement = true

    fun isShowing(): Boolean = isVisible && !isDismissing

    fun isVisible(): Boolean = isVisible

    fun isDismissing(): Boolean = isDismissing

    fun show(
        apps: List<AppInfo>,
        initialIndex: Int,
        onSelected: (AppInfo) -> Unit,
    ) {
        // Handle empty state
        if (apps.isEmpty()) {
            showEmptyState()
            return
        }

        currentApps = apps
        selectedIndex = initialIndex.coerceIn(0, apps.size - 1)
        onAppChosen = onSelected

        mainHandler.post {
            if (isDismissing) {
                overlayView?.animate()?.cancel()
                try {
                    if (overlayView?.isAttachedToWindow == true) {
                        windowManager.removeView(overlayView)
                    }
                } catch (e: Exception) {
                    Log.w("AppSwitcherOverlay", "Error detaching view during show interrupt: ${e.message}")
                }
                isVisible = false
                isDismissing = false
                isFirstTooltipPlacement = true
                iconSprings.forEach { it.cancel() }
            }
            if (!isVisible || overlayView?.isAttachedToWindow != true) {
                createAndAddView()
            }
            populateApps()
            updateSelection()
            resetAutoDismissTimer()
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        mainHandler.post {
            if (!isVisible || overlayView?.isAttachedToWindow != true) {
                createAndAddView()
            }
            val emptyState = overlayView?.findViewById<LinearLayout>(R.id.llEmptyState)
            val dockPill = overlayView?.findViewById<View>(R.id.flDockPill)
            val tooltipContainer = overlayView?.findViewById<View>(R.id.llDockTooltipContainer)

            emptyState?.visibility = View.VISIBLE
            dockPill?.visibility = View.GONE
            tooltipContainer?.visibility = View.GONE
            isVisible = true
            isDismissing = false
        }
    }

    private fun hideEmptyState() {
        val emptyState = overlayView?.findViewById<LinearLayout>(R.id.llEmptyState)
        val dockPill = overlayView?.findViewById<View>(R.id.flDockPill)
        emptyState?.visibility = View.GONE
        dockPill?.visibility = View.VISIBLE
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

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        overlayView?.animate()?.cancel()
        vDockCountdown?.animate()?.cancel()
        llDockTooltipContainer?.animate()?.cancel()
        iconSprings.forEach { it.cancel() }
        iconSprings.clear()
        itemViews.clear()
        try {
            if (overlayView?.isAttachedToWindow == true) {
                windowManager.removeView(overlayView)
            }
        } catch (_: Exception) {
        }
        overlayView = null
        llApps = null
        hsvOverlay = null
        tvDockTooltip = null
        llDockTooltipContainer = null
        vDockCountdown = null
        isVisible = false
        isDismissing = false
    }

    private fun dismissInternal(onComplete: (() -> Unit)? = null) {
        mainHandler.removeCallbacks(autoDismissRunnable)
        vDockCountdown?.animate()?.cancel()
        vDockCountdown?.visibility = View.GONE
        llDockTooltipContainer?.animate()?.cancel()
        iconSprings.forEach { it.cancel() }
        val view = overlayView
        if (!isVisible || view == null || isDismissing) {
            onComplete?.invoke()
            return
        }
        isDismissing = true
        val density = context.resources.displayMetrics.density

        view
            .animate()
            .translationY(42f * density)
            .alpha(0f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(120)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                view.visibility = View.GONE
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
            }.start()
    }

    @Suppress("DEPRECATION")
    private fun createAndAddView() {
        val density = context.resources.displayMetrics.density
        val scale = ConfigManager.getDockScalePercent(context) / 100f
        val bottomMarginDp = ConfigManager.getDockBottomMarginDp(context)
        val dimPercent = ConfigManager.getDockBackgroundDimPercent(context)

        val view =
            overlayView ?: LayoutInflater.from(context).inflate(R.layout.layout_switcher_overlay, null).also {
                overlayView = it
                llDockTooltipContainer = it.findViewById(R.id.llDockTooltipContainer)
                tvDockTooltip = it.findViewById(R.id.tvDockTooltip)
                vDockCountdown = it.findViewById(R.id.vDockCountdown)
                hsvOverlay = it.findViewById(R.id.hsvOverlay)
                llApps = it.findViewById(R.id.llOverlayApps)

                hsvOverlay?.setOnScrollChangeListener { _, _, _, _, _ ->
                    if (ConfigManager.isShowAppNameEnabled(context)) {
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
            (10f * density * scale).toInt(),
        )
        val showAppName = ConfigManager.isShowAppNameEnabled(context)
        if (showAppName) {
            val tooltipParams = llDockTooltipContainer?.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            tooltipParams?.bottomMargin = (20f * density * scale).toInt()
            llDockTooltipContainer?.layoutParams = tooltipParams
        } else {
            llDockTooltipContainer?.visibility = View.GONE
        }

        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            (bottomMarginDp * density).toInt(),
        )

        val dimFlag = if (dimPercent > 0) WindowManager.LayoutParams.FLAG_DIM_BEHIND else 0
        val blurFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            } else {
                0
            }

        @Suppress("InlinedApi")
        val overlayType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        // Get primary display for correct window placement on multi-display devices
        val primaryDisplay = getPrimaryDisplay()

        val params =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    overlayType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        dimFlag or
                        blurFlag,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    dimAmount = (dimPercent / 100f).coerceIn(0f, 0.8f)
                    y = 0
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        blurBehindRadius = (22f * density).toInt()
                    }
                    // Target primary display for multi-display support (e.g., external monitor, foldables)
                    if (primaryDisplay != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        windowAnimations = 0
                        // Note: setDisplayId is hidden API, use windowManager.addView with display context instead
                    }
                }

        view.visibility = View.VISIBLE
        view.translationY = 48f * density
        view.alpha = 0f
        view.scaleX = 0.96f
        view.scaleY = 0.96f

        try {
            // Use display context for primary display awareness
            val targetWindowManager = primaryDisplay?.let { display ->
                context.createDisplayContext(display).getSystemService(Context.WINDOW_SERVICE) as WindowManager
            } ?: windowManager

            if (view.isAttachedToWindow) {
                targetWindowManager.updateViewLayout(view, params)
            } else {
                targetWindowManager.addView(view, params)
            }
            isVisible = true
            isDismissing = false
            isFirstTooltipPlacement = true

            view
                .animate()
                .translationY(0f)
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator(1.6f))
                .start()

            Log.d("AppSwitcherOverlay", "Dock overlay view added successfully with entrance animation")
        } catch (e: Exception) {
            Log.e("AppSwitcherOverlay", "Failed to add dock overlay view", e)
            isVisible = false
            isDismissing = false
        }
    }

    private fun getPrimaryDisplay(): Display? {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        // DISPLAY_CATEGORY_PRIMARY ("primary") available from API 33
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val displays = displayManager.getDisplays("primary")
            return displays.firstOrNull()
        }
        // Fallback: return default display
        return displayManager.getDisplay(Display.DEFAULT_DISPLAY)
    }

    private fun populateApps() {
        val container = llApps ?: return

        val scale = ConfigManager.getDockScalePercent(context) / 100f
        val density = context.resources.displayMetrics.density
        val iconSize = (66f * density * scale).toInt()
        val padH = (12f * density * scale).toInt()
        val padV = (8f * density * scale).toInt()

        val canReuse =
            itemViews.size == currentApps.size &&
                container.childCount == itemViews.size &&
                currentApps.indices.all { i ->
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
        iconSprings.forEach { it.cancel() }
        iconSprings.clear()

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
            flIcon.outlineProvider =
                object : android.view.ViewOutlineProvider() {
                    override fun getOutline(
                        view: View,
                        outline: android.graphics.Outline,
                    ) {
                        val r = 0.22f * view.width
                        outline.setRoundRect(0, 0, view.width, view.height, r)
                    }
                }
            flIcon.clipToOutline = true

            flIcon.post {
                flIcon.pivotX = flIcon.width / 2f
                flIcon.pivotY = flIcon.height * 0.95f
            }

            val ivIcon = itemView.findViewById<ImageView>(R.id.ivOverlayAppIcon)
            ivIcon.setImageDrawable(AppRepository.getAppIcon(context, app))
            ivIcon.contentDescription = app.name
            ivIcon.alpha = 1.0f

            container.addView(itemView)
            itemViews.add(itemView)
            iconSprings.add(IconSprings(flIcon))
        }
    }

    private fun updateSelection() {
        val currentApp = currentApps.getOrNull(selectedIndex)
        android.util.Log.i("AppSwitcherOverlay", "SELECTED: index=$selectedIndex name=${currentApp?.name} pkg=${currentApp?.packageName}")
        val tooltip = tvDockTooltip
        val tooltipContainer = llDockTooltipContainer
        val selectedView = itemViews.getOrNull(selectedIndex)
        val density = context.resources.displayMetrics.density
        val scale = ConfigManager.getDockScalePercent(context) / 100f
        val showAppName = ConfigManager.isShowAppNameEnabled(context)

        // Update floating dock tooltip text & position
        if (showAppName && currentApp != null && tooltip != null && tooltipContainer != null) {
            tooltip.text = currentApp.name
            tooltipContainer.visibility = View.VISIBLE
            tooltipContainer.alpha = 1.0f

            selectedView?.post {
                syncTooltipPosition(animate = !isFirstTooltipPlacement)
                isFirstTooltipPlacement = false
            }
        } else {
            tooltipContainer?.visibility = View.GONE
        }

        // Animate dock items: fluid magnification bell-curve & lift effect
        for (i in itemViews.indices) {
            val view = itemViews[i]
            val dist = kotlin.math.abs(i - selectedIndex)
            val flIcon = view.findViewById<View>(R.id.flIconContainer)

            val targetScale: Float
            val targetLiftY: Float
            val targetElevation: Float
            val targetShiftX: Float

            when (dist) {
                0 -> {
                    targetScale = 1.32f
                    targetLiftY = -15f * density * scale
                    targetElevation = 20f
                    targetShiftX = 0f
                }
                1 -> {
                    targetScale = 1.12f
                    targetLiftY = -6f * density * scale
                    targetElevation = 14f
                    val dir = if (i > selectedIndex) 1f else -1f
                    targetShiftX = dir * 6f * density * scale
                }
                2 -> {
                    targetScale = 1.04f
                    targetLiftY = -2f * density * scale
                    targetElevation = 8f
                    val dir = if (i > selectedIndex) 1f else -1f
                    targetShiftX = dir * 2f * density * scale
                }
                else -> {
                    targetScale = 1.0f
                    targetLiftY = 0f
                    targetElevation = 2f
                    targetShiftX = 0f
                }
            }

            view.elevation = targetElevation
            view.alpha = 1.0f

            flIcon?.let { iconView ->
                iconView.alpha = 1.0f

                if (iconView.width > 0 && iconView.height > 0) {
                    iconView.pivotX = iconView.width / 2f
                    iconView.pivotY = iconView.height * 0.95f
                }

                iconSprings.getOrNull(i)?.animateTo(targetScale, targetShiftX, targetLiftY)
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
        if (!ConfigManager.isShowAppNameEnabled(context)) return
        val container = llDockTooltipContainer ?: return
        val root = overlayView ?: return
        val selectedView = itemViews.getOrNull(selectedIndex) ?: return

        container.post {
            val itemLoc = IntArray(2)
            selectedView.getLocationInWindow(itemLoc)
            val rootLoc = IntArray(2)
            root.getLocationInWindow(rootLoc)

            val rootWidth =
                if (root.width > 0) {
                    root.width.toFloat()
                } else {
                    context.resources.displayMetrics.widthPixels
                        .toFloat()
                }
            val rootCenterX = rootWidth / 2f
            val itemCenterX = (itemLoc[0] - rootLoc[0]) + (selectedView.width / 2f)
            val targetTranslationX = itemCenterX - rootCenterX

            if (animate) {
                container
                    .animate()
                    .translationX(targetTranslationX)
                    .alpha(1f)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator())
                    .withLayer()
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
        val showCountdown = ConfigManager.isCountdownBarEnabled(context)
        val showAppName = ConfigManager.isShowAppNameEnabled(context)

        if (delaySec > 0) {
            mainHandler.postDelayed(autoDismissRunnable, delaySec * 1000L)
            if (showAppName && showCountdown && countdownView != null) {
                countdownView.visibility = View.VISIBLE
                countdownView.animate()?.cancel()
                countdownView.scaleX = 1f
                countdownView.pivotX = 0f
                countdownView
                    .animate()
                    .scaleX(0f)
                    .setDuration(delaySec * 1000L)
                    .setInterpolator(android.view.animation.LinearInterpolator())
                    .start()
            } else {
                countdownView?.visibility = View.GONE
            }
        } else {
            countdownView?.visibility = View.GONE
        }
    }
}
