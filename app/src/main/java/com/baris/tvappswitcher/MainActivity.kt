package com.baris.tvappswitcher

import android.accessibilityservice.AccessibilityServiceInfo
import androidx.appcompat.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var tvServiceStatus: AppCompatButton
    private lateinit var tvNextKey: TextView
    private lateinit var tvPrevKey: TextView
    private lateinit var tvSwitchDelay: TextView
    private lateinit var tvSortOrder: TextView
    private lateinit var btnChangeNext: AppCompatButton
    private lateinit var btnChangePrev: AppCompatButton
    private lateinit var btnChangeDelay: AppCompatButton
    private lateinit var btnChangeSortOrder: AppCompatButton
    private lateinit var btnCustomizeOrder: AppCompatButton
    private lateinit var btnManageVisibility: AppCompatButton
    private lateinit var switchSingleKey: IosSwitchView
    private lateinit var switchSound: IosSwitchView
    private lateinit var switchCountdown: IosSwitchView
    private lateinit var btnPreviewDock: AppCompatButton
    private lateinit var btnAddDirectShortcut: AppCompatButton
    private lateinit var btnTestKey: AppCompatButton
    private lateinit var tvNoShortcutsHint: TextView
    private lateinit var rvShortcuts: RecyclerView

    private lateinit var tvDockSizeLabel: TextView
    private lateinit var sbDockSize: SeekBar
    private lateinit var btnDockSizeMinus: AppCompatButton
    private lateinit var btnDockSizePlus: AppCompatButton

    private lateinit var tvDockTransparencyLabel: TextView
    private lateinit var sbDockTransparency: SeekBar
    private lateinit var btnDockTransMinus: AppCompatButton
    private lateinit var btnDockTransPlus: AppCompatButton

    private lateinit var tvDockMarginLabel: TextView
    private lateinit var sbDockMargin: SeekBar
    private lateinit var btnDockMarginMinus: AppCompatButton
    private lateinit var btnDockMarginPlus: AppCompatButton

    private lateinit var tvDockDimLabel: TextView
    private lateinit var sbDockDim: SeekBar
    private lateinit var btnDockDimMinus: AppCompatButton
    private lateinit var btnDockDimPlus: AppCompatButton

    private var waitingDialog: AlertDialog? = null
    private var testKeyDialog: AlertDialog? = null
    private var testDialogTextView: TextView? = null

    private val keyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.baris.tvappswitcher.KEY_CAPTURED") {
                val keyCode = intent.getIntExtra("keyCode", -1)
                val target = intent.getStringExtra("target") ?: ""

                if (target == "TEST") {
                    if (keyCode != -1) {
                        testDialogTextView?.text = getString(
                            R.string.dialog_test_key_detected,
                            ConfigManager.getKeyLabel(keyCode),
                            keyCode
                        )
                    }
                    return
                }

                ConfigManager.setListening(this@MainActivity, false)
                waitingDialog?.dismiss()

                if (keyCode != -1) {
                    handleKeyCaptured(keyCode, target)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppRepository.init(this)
        SoundCueHelper.init(this)

        initViews()
        setupListeners()
        setupFocusEffects()
        updateUi()

        ConfigManager.setListening(this, false)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ConfigManager.isListening(this)) {
            val navKeys = listOf(
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME
            )
            if (navKeys.contains(event.keyCode)) {
                return super.dispatchKeyEvent(event)
            }

            if (event.action == KeyEvent.ACTION_DOWN) {
                val target = ConfigManager.getListeningTarget(this)
                if (target == "TEST") {
                    testDialogTextView?.text = getString(
                        R.string.dialog_test_key_detected,
                        ConfigManager.getKeyLabel(event.keyCode),
                        event.keyCode
                    )
                    return true
                } else if (target.isNotEmpty()) {
                    ConfigManager.setListening(this, false)
                    waitingDialog?.dismiss()
                    handleKeyCaptured(event.keyCode, target)
                    return true
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun initViews() {
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        tvNextKey = findViewById(R.id.tvNextKey)
        tvPrevKey = findViewById(R.id.tvPrevKey)
        tvSwitchDelay = findViewById(R.id.tvSwitchDelay)
        tvSortOrder = findViewById(R.id.tvSortOrder)
        btnChangeNext = findViewById(R.id.btnChangeNext)
        btnChangePrev = findViewById(R.id.btnChangePrev)
        btnChangeDelay = findViewById(R.id.btnChangeDelay)
        btnChangeSortOrder = findViewById(R.id.btnChangeSortOrder)
        btnCustomizeOrder = findViewById(R.id.btnCustomizeOrder)
        btnManageVisibility = findViewById(R.id.btnManageVisibility)
        switchSingleKey = findViewById(R.id.switchSingleKey)
        switchSound = findViewById(R.id.switchSound)
        switchCountdown = findViewById(R.id.switchCountdown)
        btnPreviewDock = findViewById(R.id.btnPreviewDock)
        btnAddDirectShortcut = findViewById(R.id.btnAddDirectShortcut)
        btnTestKey = findViewById(R.id.btnTestKey)
        tvNoShortcutsHint = findViewById(R.id.tvNoShortcutsHint)
        rvShortcuts = findViewById(R.id.rvShortcuts)

        tvDockSizeLabel = findViewById(R.id.tvDockSizeLabel)
        sbDockSize = findViewById(R.id.sbDockSize)
        btnDockSizeMinus = findViewById(R.id.btnDockSizeMinus)
        btnDockSizePlus = findViewById(R.id.btnDockSizePlus)

        tvDockTransparencyLabel = findViewById(R.id.tvDockTransparencyLabel)
        sbDockTransparency = findViewById(R.id.sbDockTransparency)
        btnDockTransMinus = findViewById(R.id.btnDockTransMinus)
        btnDockTransPlus = findViewById(R.id.btnDockTransPlus)

        tvDockMarginLabel = findViewById(R.id.tvDockMarginLabel)
        sbDockMargin = findViewById(R.id.sbDockMargin)
        btnDockMarginMinus = findViewById(R.id.btnDockMarginMinus)
        btnDockMarginPlus = findViewById(R.id.btnDockMarginPlus)

        tvDockDimLabel = findViewById(R.id.tvDockDimLabel)
        sbDockDim = findViewById(R.id.sbDockDim)
        btnDockDimMinus = findViewById(R.id.btnDockDimMinus)
        btnDockDimPlus = findViewById(R.id.btnDockDimPlus)

        sbDockSize.keyProgressIncrement = 5
        sbDockTransparency.keyProgressIncrement = 5
        sbDockMargin.keyProgressIncrement = 4
        sbDockDim.keyProgressIncrement = 5

        rvShortcuts.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        tvServiceStatus.setOnClickListener {
            if (isServiceCurrentlyEnabled()) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle(R.string.service_active_title)
                    .setMessage(R.string.service_active_message)
                    .setPositiveButton(R.string.open_settings) { _, _ ->
                        launchTvSettings()
                    }
                    .setNeutralButton(R.string.btn_show_adb_command) { _, _ ->
                        showAdbSetupDialog()
                    }
                    .setNegativeButton(R.string.close, null)
                    .show()

                dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.parseColor("#E2E4F0"))
            } else {
                openAccessibilitySettings()
            }
        }

        btnPreviewDock.setOnClickListener {
            val apps = AppRepository.getFilteredAndSortedApps(this)
            if (apps.isNotEmpty()) {
                if (isServiceCurrentlyEnabled()) {
                    sendBroadcast(Intent("com.baris.tvappswitcher.TRIGGER_OVERLAY").setPackage(packageName))
                } else {
                    val previewOverlay = AppSwitcherOverlay(this)
                    previewOverlay.show(apps, 0) {}
                }
            } else {
                Toast.makeText(this, getString(R.string.no_apps_found), Toast.LENGTH_SHORT).show()
            }
        }

        btnChangeNext.setOnClickListener {
            startListeningForKey("NEXT", getString(R.string.dialog_title_next_key))
        }

        btnChangePrev.setOnClickListener {
            startListeningForKey("PREV", getString(R.string.dialog_title_prev_key))
        }

        btnChangeDelay.setOnClickListener {
            showDelaySelectionDialog()
        }

        btnChangeSortOrder.setOnClickListener {
            showSortOrderSelectionDialog()
        }

        btnCustomizeOrder.setOnClickListener {
            showReorderAppsDialog()
        }

        btnManageVisibility.setOnClickListener {
            showAppVisibilityDialog()
        }

        switchSingleKey.setOnCheckedChangeListener { _, isChecked ->
            ConfigManager.setSingleKeyMode(this, isChecked)
            btnChangePrev.isEnabled = !isChecked
            btnChangePrev.alpha = if (isChecked) 0.4f else 1.0f
        }

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            ConfigManager.setSoundEffectsEnabled(this, isChecked)
            if (isChecked) {
                SoundCueHelper.playTick(this)
            }
        }

        switchCountdown.setOnCheckedChangeListener { _, isChecked ->
            ConfigManager.setCountdownBarEnabled(this, isChecked)
        }

        sbDockSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = 60 + progress
                ConfigManager.setDockScalePercent(this@MainActivity, percent)
                tvDockSizeLabel.text = getDockSizeLabel(percent)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDockSizeMinus.setOnClickListener {
            val current = sbDockSize.progress
            sbDockSize.progress = (current - 5).coerceAtLeast(0)
        }

        btnDockSizePlus.setOnClickListener {
            val current = sbDockSize.progress
            sbDockSize.progress = (current + 5).coerceAtMost(80)
        }

        sbDockTransparency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = progress
                ConfigManager.setDockTransparencyPercent(this@MainActivity, percent)
                tvDockTransparencyLabel.text = getDockTransparencyLabel(percent)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDockTransMinus.setOnClickListener {
            val current = sbDockTransparency.progress
            sbDockTransparency.progress = (current - 5).coerceAtLeast(0)
        }

        btnDockTransPlus.setOnClickListener {
            val current = sbDockTransparency.progress
            sbDockTransparency.progress = (current + 5).coerceAtMost(90)
        }

        sbDockMargin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ConfigManager.setDockBottomMarginDp(this@MainActivity, progress)
                tvDockMarginLabel.text = getDockMarginLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDockMarginMinus.setOnClickListener {
            val current = sbDockMargin.progress
            sbDockMargin.progress = (current - 4).coerceAtLeast(0)
        }

        btnDockMarginPlus.setOnClickListener {
            val current = sbDockMargin.progress
            sbDockMargin.progress = (current + 4).coerceAtMost(72)
        }

        sbDockDim.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ConfigManager.setDockBackgroundDimPercent(this@MainActivity, progress)
                tvDockDimLabel.text = getDockDimLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnDockDimMinus.setOnClickListener {
            val current = sbDockDim.progress
            sbDockDim.progress = (current - 5).coerceAtLeast(0)
        }

        btnDockDimPlus.setOnClickListener {
            val current = sbDockDim.progress
            sbDockDim.progress = (current + 5).coerceAtMost(50)
        }

        btnAddDirectShortcut.setOnClickListener {
            startListeningForKey("CUSTOM", getString(R.string.dialog_title_custom_key))
        }

        btnTestKey.setOnClickListener {
            showKeyTesterDialog()
        }
    }

    private fun setupFocusEffects() {
        val buttons = listOf(
            tvServiceStatus,
            btnChangeNext,
            btnChangePrev,
            btnChangeDelay,
            btnChangeSortOrder,
            btnCustomizeOrder,
            btnManageVisibility,
            btnPreviewDock,
            btnAddDirectShortcut,
            btnTestKey
        )

        for (btn in buttons) {
            btn.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .translationZ(8f)
                        .setDuration(140)
                        .start()
                } else {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(140)
                        .start()
                }
            }
        }

        val seekBars = listOf(sbDockSize, sbDockTransparency, sbDockMargin, sbDockDim)
        for (sb in seekBars) {
            sb.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate()
                        .scaleX(1.03f)
                        .scaleY(1.15f)
                        .translationZ(8f)
                        .setDuration(150)
                        .start()
                } else {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .translationZ(0f)
                        .setDuration(150)
                        .start()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateUi()

        val filter = IntentFilter("com.baris.tvappswitcher.KEY_CAPTURED")
        ContextCompat.registerReceiver(
            this,
            keyReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Ensure first button gets focus immediately so user can see where cursor is
        btnChangeNext.post {
            btnChangeNext.requestFocus()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(keyReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        ConfigManager.setListening(this, false)
        waitingDialog?.dismiss()
        testKeyDialog?.dismiss()
        testKeyDialog = null
        testDialogTextView = null
    }

    private fun getDockSizeLabel(percent: Int): String {
        val qualifier = when {
            percent < 80 -> "Small"
            percent < 95 -> "Compact"
            percent in 95..105 -> "Medium"
            percent in 106..120 -> "Large"
            else -> "Extra Large"
        }
        return "$percent% ($qualifier)"
    }

    private fun getDockTransparencyLabel(percent: Int): String {
        val qualifier = when {
            percent == 0 -> "Opaque Solid"
            percent <= 25 -> "Frosted Glass"
            percent <= 50 -> "Subtle Glass"
            percent <= 75 -> "Clear Glass"
            else -> "Ultra Transparent"
        }
        return "$percent% ($qualifier)"
    }

    private fun getDockMarginLabel(marginDp: Int): String {
        val qualifier = when {
            marginDp == 0 -> "Flush Bottom"
            marginDp in 1..16 -> "Low Margin"
            marginDp in 17..32 -> "Standard TV"
            marginDp in 33..48 -> "Elevated"
            else -> "High Lift"
        }
        return "$marginDp dp ($qualifier)"
    }

    private fun getDockDimLabel(percent: Int): String {
        val qualifier = when {
            percent == 0 -> "No Dim"
            percent <= 15 -> "Subtle"
            percent <= 30 -> "Medium"
            else -> "Cinema Dark"
        }
        return "$percent% ($qualifier)"
    }

    private fun getSortModeLabel(mode: ConfigManager.SortMode): String {
        return when (mode) {
            ConfigManager.SortMode.NAME -> getString(R.string.sort_alphabetical)
            ConfigManager.SortMode.RECENCY -> getString(R.string.sort_recency)
            ConfigManager.SortMode.CUSTOM -> getString(R.string.sort_custom)
        }
    }

    private fun updateUi() {
        val nextKey = ConfigManager.getNextKey(this)
        val prevKey = ConfigManager.getPrevKey(this)
        val delaySec = ConfigManager.getSwitchDelaySeconds(this)
        val sortMode = ConfigManager.getSortMode(this)

        tvNextKey.text = ConfigManager.getKeyLabel(nextKey)
        tvPrevKey.text = ConfigManager.getKeyLabel(prevKey)

        tvSwitchDelay.text = ConfigManager.getSwitchDelayLabel(delaySec)
        btnChangeDelay.text = getString(R.string.btn_change_delay_fmt, ConfigManager.getSwitchDelayShortLabel(delaySec))

        tvSortOrder.text = getSortModeLabel(sortMode)

        val allApps = AppRepository.getAllApps(this)
        val enabledFilter = ConfigManager.getEnabledPackages(this)
        val visibleCount = if (enabledFilter != null && enabledFilter.isNotEmpty()) {
            allApps.count { enabledFilter.contains(it.packageName) }
        } else {
            allApps.size
        }
        btnManageVisibility.text = if (visibleCount < allApps.size) {
            getString(R.string.btn_visible_count_fmt, visibleCount, allApps.size)
        } else {
            getString(R.string.btn_hide_apps)
        }

        val singleKey = ConfigManager.isSingleKeyMode(this)
        switchSingleKey.setChecked(singleKey, false)
        btnChangePrev.isEnabled = !singleKey
        btnChangePrev.alpha = if (singleKey) 0.4f else 1.0f

        val soundEnabled = ConfigManager.isSoundEffectsEnabled(this)
        switchSound.setChecked(soundEnabled, false)

        val countdownEnabled = ConfigManager.isCountdownBarEnabled(this)
        switchCountdown.setChecked(countdownEnabled, false)

        val dockScale = ConfigManager.getDockScalePercent(this)
        sbDockSize.progress = dockScale - 60
        tvDockSizeLabel.text = getDockSizeLabel(dockScale)

        val dockTrans = ConfigManager.getDockTransparencyPercent(this)
        sbDockTransparency.progress = dockTrans
        tvDockTransparencyLabel.text = getDockTransparencyLabel(dockTrans)

        val dockMargin = ConfigManager.getDockBottomMarginDp(this)
        sbDockMargin.progress = dockMargin
        tvDockMarginLabel.text = getDockMarginLabel(dockMargin)

        val dockDim = ConfigManager.getDockBackgroundDimPercent(this)
        sbDockDim.progress = dockDim
        tvDockDimLabel.text = getDockDimLabel(dockDim)

        updateShortcutsList()
        updateServiceStatus()
    }

    private fun showAppVisibilityDialog() {
        val allApps = AppRepository.getAllApps(this).sortedBy { it.name.lowercase() }
        if (allApps.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_apps_found), Toast.LENGTH_SHORT).show()
            return
        }

        val currentEnabled = ConfigManager.getEnabledPackages(this)?.toMutableSet()
            ?: allApps.map { it.packageName }.toMutableSet()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#151722"))
            setPadding(16, 12, 16, 8)
        }

        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 4, 8, 12)
        }

        val tvSummary = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            textSize = 14f
            setTextColor(Color.parseColor("#00E5FF"))
            text = getString(R.string.apps_saved_visible_toast, currentEnabled.size)
        }

        val btnSelectAll = AppCompatButton(this).apply {
            text = getString(R.string.select_all)
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.selector_tv_btn_bg)
            setPadding(20, 8, 20, 8)
            isFocusable = true
            isClickable = true
        }

        val btnDeselectAll = AppCompatButton(this).apply {
            text = getString(R.string.deselect_all)
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.selector_tv_btn_bg)
            setPadding(20, 8, 20, 8)
            isFocusable = true
            isClickable = true
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = 12
            layoutParams = lp
        }

        headerLayout.addView(tvSummary)
        headerLayout.addView(btnSelectAll)
        headerLayout.addView(btnDeselectAll)
        container.addView(headerLayout)

        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setPadding(4, 4, 4, 4)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.55).toInt()
            )
            layoutParams = lp
        }

        lateinit var adapter: AppVisibilityAdapter
        fun updateSummary() {
            tvSummary.text = getString(R.string.apps_saved_visible_toast, currentEnabled.size)
        }

        adapter = AppVisibilityAdapter(
            apps = allApps,
            enabledPackages = currentEnabled,
            onCountChanged = { updateSummary() }
        )
        rv.adapter = adapter
        container.addView(rv)

        btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }
        btnDeselectAll.setOnClickListener {
            adapter.deselectAll()
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_manage_visibility_title)
            .setView(container)
            .setPositiveButton(R.string.save) { d, _ ->
                ConfigManager.setEnabledPackages(this, currentEnabled)
                AppRepository.refreshAppsAsync(this)
                updateUi()
                d.dismiss()
                Toast.makeText(this, getString(R.string.apps_saved_visible_toast, currentEnabled.size), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSortOrderSelectionDialog() {
        val modes = ConfigManager.SortMode.values()
        val labels = modes.map { getSortModeLabel(it) }.toTypedArray()
        val currentMode = ConfigManager.getSortMode(this)
        val currentIndex = modes.indexOf(currentMode).coerceAtLeast(0)

        val adapter = object : android.widget.ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_single_choice,
            labels
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as android.widget.CheckedTextView
                view.setTextColor(Color.WHITE)
                view.textSize = 16f
                view.setPadding(32, 24, 32, 24)
                return view
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_select_sort_order)
            .setSingleChoiceItems(adapter, currentIndex) { dialog, which ->
                val selectedMode = modes[which]
                ConfigManager.setSortMode(this, selectedMode)
                updateUi()
                dialog.dismiss()
                Toast.makeText(
                    this,
                    getString(R.string.sort_order_set_toast, getSortModeLabel(selectedMode)),
                    Toast.LENGTH_SHORT
                ).show()

                if (selectedMode == ConfigManager.SortMode.CUSTOM) {
                    showReorderAppsDialog()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showReorderAppsDialog() {
        val appsList = getAllInstalledApps().toMutableList()
        if (appsList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_apps_to_reorder), Toast.LENGTH_SHORT).show()
            return
        }

        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setPadding(20, 16, 20, 16)
            setBackgroundColor(Color.parseColor("#151722"))
        }

        var adapter: ReorderAppAdapter? = null

        adapter = ReorderAppAdapter(
            apps = appsList,
            onOrderChanged = { updatedList ->
                val packageOrder = updatedList.map { it.packageName }
                ConfigManager.setCustomAppOrder(this@MainActivity, packageOrder)
                ConfigManager.setSortMode(this@MainActivity, ConfigManager.SortMode.CUSTOM)
            },
            onItemClick = { app, position ->
                showItemPositionMenu(app, position, appsList, adapter!!)
            }
        )

        rv.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_customize_order_title)
            .setView(rv)
            .setPositiveButton(R.string.done) { d, _ ->
                ConfigManager.setCustomAppOrder(this, appsList.map { it.packageName })
                ConfigManager.setSortMode(this, ConfigManager.SortMode.CUSTOM)
                updateUi()
                d.dismiss()
            }
            .setNeutralButton(R.string.reset_to_az) { _, _ ->
                val aToZ = appsList.sortedBy { it.name.lowercase() }
                appsList.clear()
                appsList.addAll(aToZ)
                ConfigManager.setCustomAppOrder(this, aToZ.map { it.packageName })
                adapter.notifyDataSetChanged()
                ConfigManager.setSortMode(this, ConfigManager.SortMode.CUSTOM)
                updateUi()
                Toast.makeText(this, getString(R.string.order_reset_toast), Toast.LENGTH_SHORT).show()
            }
            .setOnDismissListener {
                updateUi()
            }
            .show()
    }

    private fun showItemPositionMenu(
        app: AppInfo,
        position: Int,
        appsList: MutableList<AppInfo>,
        adapter: ReorderAppAdapter
    ) {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (position > 0) {
            options.add(getString(R.string.move_to_top))
            actions.add { adapter.moveItem(position, 0) }

            options.add(getString(R.string.move_up))
            actions.add { adapter.moveItem(position, position - 1) }
        }

        if (position < appsList.size - 1) {
            options.add(getString(R.string.move_down))
            actions.add { adapter.moveItem(position, position + 1) }

            options.add(getString(R.string.move_to_bottom))
            actions.add { adapter.moveItem(position, appsList.size - 1) }
        }

        if (options.isEmpty()) return

        val adapter = object : android.widget.ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            options
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent) as android.widget.TextView
                view.setTextColor(android.graphics.Color.WHITE)
                view.textSize = 16f
                view.setPadding(32, 24, 32, 24)
                return view
            }
        }

        AlertDialog.Builder(this)
            .setTitle("${app.name} (#${position + 1})")
            .setAdapter(adapter) { _, which ->
                actions[which].invoke()
            }
            .show()
    }

    private fun getAllInstalledApps(): List<AppInfo> {
        val all = AppRepository.getAllApps(this)
        val customOrder = ConfigManager.getCustomAppOrder(this)
        return all.sortedWith(
            compareBy<AppInfo> {
                val idx = customOrder.indexOf(it.packageName)
                if (idx >= 0) idx else Int.MAX_VALUE
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
    }

    private fun showDelaySelectionDialog() {
        val options = arrayOf(
            "1 second (Fast)",
            "2 seconds (Recommended)",
            "3 seconds",
            "4 seconds",
            "5 seconds (Slow)",
            "Manual Only (Press OK to switch)"
        )
        val values = intArrayOf(1, 2, 3, 4, 5, 0)
        val currentDelay = ConfigManager.getSwitchDelaySeconds(this)
        val checkedIndex = values.indexOf(currentDelay).coerceAtLeast(0)

        val adapter = object : android.widget.ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_single_choice,
            options
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = super.getView(position, convertView, parent) as android.widget.CheckedTextView
                view.setTextColor(Color.WHITE)
                view.textSize = 16f
                view.setPadding(32, 24, 32, 24)
                return view
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_select_countdown_title)
            .setSingleChoiceItems(adapter, checkedIndex) { dialog, which ->
                val selectedSeconds = values[which]
                ConfigManager.setSwitchDelaySeconds(this, selectedSeconds)
                updateUi()
                dialog.dismiss()
                Toast.makeText(
                    this,
                    getString(R.string.countdown_set_toast, ConfigManager.getSwitchDelayLabel(selectedSeconds)),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateShortcutsList() {
        val mappings = ConfigManager.getCustomMappings(this)
        if (mappings.isEmpty()) {
            tvNoShortcutsHint.visibility = View.VISIBLE
            rvShortcuts.visibility = View.GONE
        } else {
            tvNoShortcutsHint.visibility = View.GONE
            rvShortcuts.visibility = View.VISIBLE
            rvShortcuts.adapter = MappingAdapter(this, mappings) { keyCode ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.dialog_remove_shortcut_title)
                    .setMessage(getString(R.string.dialog_remove_shortcut_msg, ConfigManager.getKeyLabel(keyCode)))
                    .setPositiveButton(R.string.delete) { _, _ ->
                        ConfigManager.removeCustomMapping(this, keyCode)
                        updateShortcutsList()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun isServiceCurrentlyEnabled(): Boolean {
        // Check 1: Settings.Secure
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledServices != null && enabledServices.contains("KeyInterceptorService")) {
            return true
        }

        // Check 2: AccessibilityManager
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val runningList = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in runningList) {
            if (service.resolveInfo?.serviceInfo?.packageName == packageName) {
                return true
            }
        }
        return false
    }

    private fun updateServiceStatus() {
        val isEnabled = isServiceCurrentlyEnabled()
        if (isEnabled) {
            tvServiceStatus.text = getString(R.string.service_status_active)
        } else {
            tvServiceStatus.text = getString(R.string.service_status_inactive)
        }
    }

    private fun startListeningForKey(target: String, title: String) {
        ConfigManager.setListening(this, true, target)

        waitingDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.dialog_press_remote_key)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel) { _, _ ->
                ConfigManager.setListening(this, false)
            }
            .setOnCancelListener {
                ConfigManager.setListening(this, false)
            }
            .setOnKeyListener { _, keyCode, event ->
                val navKeys = listOf(
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME
                )
                if (navKeys.contains(keyCode)) {
                    return@setOnKeyListener false
                }
                if (event.action == KeyEvent.ACTION_DOWN) {
                    ConfigManager.setListening(this, false)
                    waitingDialog?.dismiss()
                    handleKeyCaptured(keyCode, target)
                    return@setOnKeyListener true
                }
                false
            }
            .show()
    }

    private fun showKeyTesterDialog() {
        ConfigManager.setListening(this, true, "TEST")

        val tv = TextView(this).apply {
            setPadding(48, 36, 48, 28)
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 16f
            text = getString(R.string.dialog_test_key_msg)
            setLineSpacing(0f, 1.3f)
        }
        testDialogTextView = tv

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_test_key_title)
            .setView(tv)
            .setCancelable(true)
            .setPositiveButton(R.string.done) { d, _ ->
                ConfigManager.setListening(this, false)
                d.dismiss()
            }
            .setOnDismissListener {
                ConfigManager.setListening(this, false)
                testKeyDialog = null
                testDialogTextView = null
            }
            .setOnKeyListener { _, keyCode, event ->
                val navKeys = listOf(
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME
                )
                if (navKeys.contains(keyCode)) {
                    return@setOnKeyListener false
                }
                if (event.action == KeyEvent.ACTION_DOWN) {
                    testDialogTextView?.text = getString(
                        R.string.dialog_test_key_detected,
                        ConfigManager.getKeyLabel(keyCode),
                        keyCode
                    )
                    return@setOnKeyListener true
                }
                false
            }
            .create()

        testKeyDialog = dialog
        dialog.show()
    }

    private fun handleKeyCaptured(keyCode: Int, target: String) {
        when (target) {
            "NEXT" -> {
                ConfigManager.setNextKey(this, keyCode)
                Toast.makeText(this, getString(R.string.key_next_set_toast, ConfigManager.getKeyLabel(keyCode)), Toast.LENGTH_SHORT).show()
                updateUi()
            }
            "PREV" -> {
                ConfigManager.setPrevKey(this, keyCode)
                Toast.makeText(this, getString(R.string.key_prev_set_toast, ConfigManager.getKeyLabel(keyCode)), Toast.LENGTH_SHORT).show()
                updateUi()
            }
            "CUSTOM" -> {
                showAppSelectionDialog { chosenApp ->
                    ConfigManager.saveCustomMapping(this, keyCode, chosenApp.packageName)
                    Toast.makeText(this, getString(R.string.key_mapped_toast, ConfigManager.getKeyLabel(keyCode), chosenApp.name), Toast.LENGTH_SHORT).show()
                    updateUi()
                }
            }
        }
    }

    private fun showAppSelectionDialog(onChosen: (AppInfo) -> Unit) {
        val apps = getAllInstalledApps().sortedBy { it.name.lowercase() }

        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#1C1C28"))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_select_app_title)
            .setView(rv)
            .show()

        rv.adapter = AppListAdapter(apps) { app ->
            dialog.dismiss()
            onChosen(app)
        }
    }

    private fun showAdbSetupDialog() {
        val cmd1 = "adb shell settings put secure enabled_accessibility_services com.baris.tvappswitcher/com.baris.tvappswitcher.KeyInterceptorService"
        val cmd2 = "adb shell settings put secure accessibility_enabled 1"
        val message = getString(R.string.adb_setup_msg, cmd1, cmd2)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.adb_setup_title)
            .setMessage(message)
            .setPositiveButton(R.string.close, null)
            .show()

        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.parseColor("#E2E4F0"))
    }

    private fun openAccessibilitySettings() {
        if (tryEnableServiceProgrammatically()) {
            Toast.makeText(this, getString(R.string.accessibility_service_enabled_toast), Toast.LENGTH_SHORT).show()
            updateServiceStatus()
            return
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.dialog_enable_service_title)
            .setMessage(R.string.dialog_enable_service_msg)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                launchTvSettings()
            }
            .setNeutralButton(R.string.btn_show_adb_command) { _, _ ->
                showAdbSetupDialog()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.parseColor("#E2E4F0"))
    }

    private fun tryEnableServiceProgrammatically(): Boolean {
        return try {
            val serviceComponent = "${packageName}/${KeyInterceptorService::class.java.name}"
            val currentServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val newServices = if (!currentServices.contains(serviceComponent)) {
                if (currentServices.isBlank()) serviceComponent else "$currentServices:$serviceComponent"
            } else {
                currentServices
            }

            val setServices = Settings.Secure.putString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newServices
            )
            val setEnabled = Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                1
            )
            setServices && setEnabled
        } catch (e: Exception) {
            false
        }
    }

    private fun launchTvSettings() {
        val intentsToTry = listOf(
            Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings"),
            Intent(Settings.ACTION_SETTINGS),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.device.apps.AppsActivity")
        )

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            } catch (e: Exception) {
                // Try next
            }
        }
        Toast.makeText(this, getString(R.string.open_accessibility_manual_toast), Toast.LENGTH_LONG).show()
    }
}
