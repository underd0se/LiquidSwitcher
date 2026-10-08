package com.baris.tvappswitcher

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.baris.tvappswitcher.databinding.ActivityMainBinding
import com.baris.tvappswitcher.databinding.ViewSettingsRowActionBinding
import com.baris.tvappswitcher.databinding.ViewSettingsRowSliderBinding
import com.baris.tvappswitcher.databinding.ViewSettingsRowToggleBinding
import com.baris.tvappswitcher.dialogs.AdbSetupDialog
import com.baris.tvappswitcher.dialogs.AppSelectionDialog
import com.baris.tvappswitcher.dialogs.AppVisibilityDialog
import com.baris.tvappswitcher.dialogs.DelaySelectionDialog
import com.baris.tvappswitcher.dialogs.KeyCaptureDialog
import com.baris.tvappswitcher.dialogs.KeyTesterDialog
import com.baris.tvappswitcher.dialogs.ReorderAppsDialog
import com.baris.tvappswitcher.dialogs.SortOrderDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var waitingDialog: KeyCaptureDialog? = null
    private var testKeyDialog: AlertDialog? = null
    private var testDialogTextView: TextView? = null

    private var activeTab = 0

    private val tabButtons: List<AppCompatButton>
        get() = listOf(
            binding.tabSwitching,
            binding.tabApps,
            binding.tabDockSizing,
            binding.tabDockAppearance,
            binding.tabShortcuts,
        )

    private val paneRoots: List<View>
        get() = listOf(
            binding.paneSwitching.root,
            binding.paneApps.root,
            binding.paneDockSizing.root,
            binding.paneDockAppearance.root,
            binding.paneShortcuts.root,
        )

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
                            keyCode,
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppRepository.init(this)
        SoundCueHelper.init(this)

        initRows()
        setupListeners()
        setupTabRail()
        setupFocusEffects()
        selectTab(savedInstanceState?.getInt(KEY_ACTIVE_TAB) ?: 0)
        updateUi()

        ConfigManager.setListening(this, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_ACTIVE_TAB, activeTab)
    }

    private fun isStandardNavKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_HOME,
        -> true
        else -> false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ConfigManager.isListening(this)) {
            if (isStandardNavKey(event.keyCode)) {
                return super.dispatchKeyEvent(event)
            }

            if (event.action == KeyEvent.ACTION_DOWN) {
                val target = ConfigManager.getListeningTarget(this)
                if (target == "TEST") {
                    testDialogTextView?.text = getString(
                        R.string.dialog_test_key_detected,
                        ConfigManager.getKeyLabel(event.keyCode),
                        event.keyCode,
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

        if (event.action == KeyEvent.ACTION_DOWN && isInActivePane()) {
            // Up is the spine: it walks the pane and leaves through the top
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP && isOnFirstPaneItem()) {
                tabs(activeTab)?.requestFocus()
                return true
            }
            // Left only means "go back" for rows that have no value to adjust
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && !isOnHorizontalControl()) {
                tabs(activeTab)?.requestFocus()
                return true
            }
        }

        return super.dispatchKeyEvent(event)
    }

    private fun initRows() {
        with(binding) {
            paneSwitching.rowNextKey.configure(R.drawable.ic_arrow_forward, R.string.label_switch_next)
            paneSwitching.rowPrevKey.configure(R.drawable.ic_arrow_back, R.string.label_switch_prev)
            paneSwitching.rowDelay.configure(R.drawable.ic_timer, R.string.label_countdown_delay)
            paneSwitching.rowSingleKey.configure(
                R.drawable.ic_remote,
                R.string.label_single_key_mode,
                R.string.single_key_disabled,
            )
            paneSwitching.rowAudioCues.configure(
                R.drawable.ic_volume,
                R.string.label_audio_cues,
                R.string.audio_cues_off,
            )

            paneApps.rowVisibleApps.configure(R.drawable.ic_apps_grid, R.string.label_visible_apps)
            paneApps.rowSortOrder.configure(R.drawable.ic_sort, R.string.label_app_sort_order)
            paneApps.rowCustomOrder.configure(
                R.drawable.ic_reorder,
                R.string.label_custom_order,
                R.string.desc_custom_order,
            )
            paneApps.rowCustomOrder.hideValue()

            paneDockSizing.rowDockSize.configure(R.drawable.ic_dock_size, R.string.label_dock_size)
            paneDockSizing.rowDockTransparency.configure(R.drawable.ic_layers, R.string.label_dock_transparency)
            paneDockSizing.rowDockMargin.configure(R.drawable.ic_ruler, R.string.label_dock_margin)
            paneDockSizing.rowDockDim.configure(R.drawable.ic_contrast, R.string.label_dock_dim)

            paneDockAppearance.rowAppName.configure(
                R.drawable.ic_text_badge,
                R.string.label_show_app_name,
                R.string.desc_show_app_name,
            )
            paneDockAppearance.rowCountdown.configure(
                R.drawable.ic_bar_chart,
                R.string.label_show_countdown,
                R.string.desc_show_countdown,
            )

            paneShortcuts.rowAddShortcut.configure(R.drawable.ic_bolt, R.string.btn_add_shortcut)
            paneShortcuts.rowAddShortcut.hideValue()
            paneShortcuts.rowTestKey.configure(R.drawable.ic_gamepad, R.string.btn_test_key)
            paneShortcuts.rowTestKey.hideValue()

            paneShortcuts.rvShortcuts.layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupListeners() {
        with(binding) {
            tvServiceStatus.setOnClickListener {
                if (isServiceCurrentlyEnabled()) {
                    val dialog = AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.service_active_title)
                        .setMessage(R.string.service_active_message)
                        .setPositiveButton(R.string.open_settings) { _, _ ->
                            launchTvSettings()
                        }
                        .setNeutralButton(R.string.btn_show_adb_command) { _, _ ->
                            AdbSetupDialog(this@MainActivity) {}.show()
                        }
                        .setNegativeButton(R.string.close, null)
                        .show()

                    dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                } else {
                    openAccessibilitySettings()
                }
            }

            paneSwitching.rowNextKey.root.setOnClickListener {
                waitingDialog = KeyCaptureDialog(this@MainActivity, "NEXT", getString(R.string.dialog_title_next_key), { keyCode ->
                    ConfigManager.setNextKey(this@MainActivity, keyCode)
                    Toast.makeText(this@MainActivity, getString(R.string.key_next_set_toast, ConfigManager.getKeyLabel(keyCode)), Toast.LENGTH_SHORT).show()
                    updateUi()
                }) {}.also { it.show() }
            }

            paneSwitching.rowPrevKey.root.setOnClickListener {
                waitingDialog = KeyCaptureDialog(this@MainActivity, "PREV", getString(R.string.dialog_title_prev_key), { keyCode ->
                    ConfigManager.setPrevKey(this@MainActivity, keyCode)
                    Toast.makeText(this@MainActivity, getString(R.string.key_prev_set_toast, ConfigManager.getKeyLabel(keyCode)), Toast.LENGTH_SHORT).show()
                    updateUi()
                }) {}.also { it.show() }
            }

            paneSwitching.rowDelay.root.setOnClickListener {
                DelaySelectionDialog(this@MainActivity) { updateUi() }.show()
            }

            paneSwitching.rowSingleKey.rowSwitch.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setSingleKeyMode(this@MainActivity, isChecked)
                updatePrevKeyAvailability(!isChecked)
            }
            paneSwitching.rowSingleKey.root.setOnClickListener {
                paneSwitching.rowSingleKey.rowSwitch.toggle()
            }

            paneSwitching.rowAudioCues.rowSwitch.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setSoundEffectsEnabled(this@MainActivity, isChecked)
                paneSwitching.rowAudioCues.rowSubtitle.setText(
                    if (isChecked) R.string.audio_cues_on else R.string.audio_cues_off,
                )
                if (isChecked) {
                    SoundCueHelper.playTick(this@MainActivity)
                }
            }
            paneSwitching.rowAudioCues.root.setOnClickListener {
                paneSwitching.rowAudioCues.rowSwitch.toggle()
            }

            paneApps.rowVisibleApps.root.setOnClickListener {
                AppVisibilityDialog(this@MainActivity) { updateUi() }.show()
            }

            paneApps.rowSortOrder.root.setOnClickListener {
                SortOrderDialog(this@MainActivity, { updateUi() }, { ReorderAppsDialog(this@MainActivity) { updateUi() }.show() }).show()
            }

            paneApps.rowCustomOrder.root.setOnClickListener {
                ReorderAppsDialog(this@MainActivity) { updateUi() }.show()
            }

            paneDockSizing.rowDockSize.rowSeekBar.apply {
                max = 80
                keyProgressIncrement = 5
                setOnSeekBarChangeListener(
                    onProgress = { value ->
                        val percent = 60 + value
                        ConfigManager.setDockScalePercent(this@MainActivity, percent)
                        binding.paneDockSizing.rowDockSize.rowValue.text = getDockSizeLabel(percent)
                    },
                )
            }

            paneDockSizing.rowDockTransparency.rowSeekBar.apply {
                max = 90
                keyProgressIncrement = 5
                setOnSeekBarChangeListener(
                    onProgress = { value ->
                        ConfigManager.setDockTransparencyPercent(this@MainActivity, value)
                        binding.paneDockSizing.rowDockTransparency.rowValue.text = getDockTransparencyLabel(value)
                    },
                )
            }

            paneDockSizing.rowDockMargin.rowSeekBar.apply {
                max = 72
                keyProgressIncrement = 4
                setOnSeekBarChangeListener(
                    onProgress = { value ->
                        ConfigManager.setDockBottomMarginDp(this@MainActivity, value)
                        binding.paneDockSizing.rowDockMargin.rowValue.text = getDockMarginLabel(value)
                    },
                )
            }

            paneDockSizing.rowDockDim.rowSeekBar.apply {
                max = 50
                keyProgressIncrement = 5
                setOnSeekBarChangeListener(
                    onProgress = { value ->
                        ConfigManager.setDockBackgroundDimPercent(this@MainActivity, value)
                        binding.paneDockSizing.rowDockDim.rowValue.text = getDockDimLabel(value)
                    },
                )
            }

            paneDockAppearance.rowAppName.rowSwitch.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setShowAppNameEnabled(this@MainActivity, isChecked)
                updateCountdownRowState(isChecked)
            }
            paneDockAppearance.rowAppName.root.setOnClickListener {
                paneDockAppearance.rowAppName.rowSwitch.toggle()
            }

            paneDockAppearance.rowCountdown.rowSwitch.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setCountdownBarEnabled(this@MainActivity, isChecked)
            }
            paneDockAppearance.rowCountdown.root.setOnClickListener {
                paneDockAppearance.rowCountdown.rowSwitch.toggle()
            }

            paneShortcuts.rowAddShortcut.root.setOnClickListener {
                waitingDialog = KeyCaptureDialog(this@MainActivity, "CUSTOM", getString(R.string.dialog_title_custom_key), { keyCode ->
                    openAppPickerForShortcut(keyCode)
                }) {}.also { it.show() }
            }

            paneShortcuts.rowTestKey.root.setOnClickListener {
                testDialogTextView = KeyTesterDialog(this@MainActivity) {}
                    .also { it.show() }
                    .capturedKeyView
            }
        }
    }

    private fun setupTabRail() {
        val tabs = tabButtons

        tabs.forEachIndexed { index, button ->
            button.setOnClickListener { selectTab(index) }
            button.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (keyCode) {
                    // Vertical rail: up/down walks the tabs
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        moveSelectionTo(index + 1)
                        true
                    }

                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (index == 0) {
                            binding.tvServiceStatus.requestFocus()
                        } else {
                            moveSelectionTo(index - 1)
                        }
                        true
                    }

                    // Right leaves the rail and enters the active pane
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        focusFirstInPane()
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun selectTab(index: Int) {
        activeTab = index.coerceIn(0, paneRoots.lastIndex)
        tabButtons.forEachIndexed { i, button -> button.isSelected = i == activeTab }
        paneRoots.forEachIndexed { i, pane ->
            pane.visibility = if (i == activeTab) View.VISIBLE else View.GONE
        }
    }

    private fun moveSelectionTo(index: Int) {
        val target = ((index % tabButtons.size) + tabButtons.size) % tabButtons.size
        selectTab(target)
        tabs(target)?.requestFocus()
    }

    private fun tabs(index: Int): AppCompatButton? = tabButtons.getOrNull(index)

    private fun focusFirstInPane() {
        val first = paneRoots[activeTab].firstFocusableDescendant()
        if (first != null) {
            first.requestFocus()
        }
    }

    private fun isInActivePane(): Boolean {
        val focused = currentFocus ?: return false
        val pane = paneRoots[activeTab]
        return focused != pane && isDescendantOf(pane, focused)
    }

    /**
     * A focused slider owns DPAD_LEFT/RIGHT for its own value stepping, at any value.
     * Leaving the pane is done with DPAD_UP through the top of the pane instead.
     */
    private fun isOnHorizontalControl(): Boolean = currentFocus is SeekBar

    private fun isDescendantOf(
        ancestor: View,
        view: View,
    ): Boolean {
        var current: View? = view
        while (current != null) {
            if (current === ancestor) return true
            current = current.parent as? View
        }
        return false
    }

    private fun isOnFirstPaneItem(): Boolean {
        val focused = currentFocus ?: return false
        val first = paneRoots[activeTab].firstFocusableDescendant() ?: return false
        return focused === first
    }

    private fun View.firstFocusableDescendant(): View? {
        if (visibility != View.VISIBLE) return null
        if (isFocusable && isEnabled && isShown) return this
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                val found = getChildAt(i).firstFocusableDescendant()
                if (found != null) return found
            }
        }
        return null
    }

    private fun setupFocusEffects() {
        // Only chrome keeps the lift animation. Content rows deliberately do not:
        // their focus state is carried entirely by the card fill, so a focused row
        // never shifts or overlaps its neighbours.
        val chrome = mutableListOf<View>(binding.tvServiceStatus)
        chrome.addAll(tabButtons)

        for (view in chrome) {
            view.setOnFocusChangeListener { v, hasFocus ->
                val scale = if (hasFocus) 1.03f else 1.0f
                val transZ = if (hasFocus) 8f else 0f
                v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .translationZ(transZ)
                    .setDuration(140)
                    .withLayer()
                    .start()
            }
        }

        val seekBars = listOf(
            binding.paneDockSizing.rowDockSize.rowSeekBar,
            binding.paneDockSizing.rowDockTransparency.rowSeekBar,
            binding.paneDockSizing.rowDockMargin.rowSeekBar,
            binding.paneDockSizing.rowDockDim.rowSeekBar,
        )

        // The SeekBar owns focus on slider rows, so the card fill is swapped manually
        val sliderCards = listOf(
            binding.paneDockSizing.rowDockSize.root,
            binding.paneDockSizing.rowDockTransparency.root,
            binding.paneDockSizing.rowDockMargin.root,
            binding.paneDockSizing.rowDockDim.root,
        )

        seekBars.forEachIndexed { index, sb ->
            val card = sliderCards[index]
            sb.setOnFocusChangeListener { _, hasFocus ->
                card.setBackgroundResource(
                    if (hasFocus) R.drawable.bg_settings_row_focused else R.drawable.bg_settings_row,
                )
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
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        if (currentFocus == null) {
            tabs(activeTab)?.post { tabs(activeTab)?.requestFocus() }
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(keyReceiver)
        } catch (e: Exception) {
        }
        ConfigManager.setListening(this, false)
        waitingDialog?.dismiss()
        testKeyDialog?.dismiss()
        testKeyDialog = null
        testDialogTextView = null
    }

    private fun getDockSizeLabel(percent: Int): String {
        val qualifier = when {
            percent < 80 -> getString(R.string.dock_size_small)
            percent < 95 -> getString(R.string.dock_size_compact)
            percent in 95..105 -> getString(R.string.dock_size_medium)
            percent in 106..120 -> getString(R.string.dock_size_large)
            else -> getString(R.string.dock_size_extra_large)
        }
        return getString(R.string.dock_size_fmt, percent, qualifier)
    }

    private fun getDockTransparencyLabel(percent: Int): String {
        val qualifier = when {
            percent == 0 -> getString(R.string.dock_trans_opaque)
            percent <= 25 -> getString(R.string.dock_trans_frosted)
            percent <= 50 -> getString(R.string.dock_trans_subtle)
            percent <= 75 -> getString(R.string.dock_trans_clear)
            else -> getString(R.string.dock_trans_ultra)
        }
        return getString(R.string.dock_trans_fmt, percent, qualifier)
    }

    private fun getDockMarginLabel(marginDp: Int): String {
        val qualifier = when {
            marginDp == 0 -> getString(R.string.dock_margin_flush)
            marginDp in 1..16 -> getString(R.string.dock_margin_low)
            marginDp in 17..32 -> getString(R.string.dock_margin_standard)
            marginDp in 33..48 -> getString(R.string.dock_margin_elevated)
            else -> getString(R.string.dock_margin_high)
        }
        return getString(R.string.dock_margin_fmt, marginDp, qualifier)
    }

    private fun getDockDimLabel(percent: Int): String {
        val qualifier = when {
            percent == 0 -> getString(R.string.dock_dim_none)
            percent <= 15 -> getString(R.string.dock_dim_subtle)
            percent <= 30 -> getString(R.string.dock_dim_medium)
            else -> getString(R.string.dock_dim_cinema)
        }
        return getString(R.string.dock_dim_fmt, percent, qualifier)
    }

    private fun getSortModeLabel(mode: ConfigManager.SortMode): String = when (mode) {
        ConfigManager.SortMode.NAME -> getString(R.string.sort_alphabetical)
        ConfigManager.SortMode.RECENCY -> getString(R.string.sort_recency)
        ConfigManager.SortMode.CUSTOM -> getString(R.string.sort_custom)
    }

    private fun updateUi() {
        updateSwitchingPane()
        updateAppsPane()
        updateDockSizingPane()
        updateDockAppearancePane()
        updateShortcutsPane()
        updateServiceStatus()
    }

    private fun updateSwitchingPane() {
        with(binding.paneSwitching) {
            rowNextKey.rowValue.text = ConfigManager.getKeyLabel(ConfigManager.getNextKey(this@MainActivity))
            rowPrevKey.rowValue.text = ConfigManager.getKeyLabel(ConfigManager.getPrevKey(this@MainActivity))

            val delaySec = ConfigManager.getSwitchDelaySeconds(this@MainActivity)
            rowDelay.rowValue.text = ConfigManager.getSwitchDelayLabel(this@MainActivity, delaySec)

            val singleKey = ConfigManager.isSingleKeyMode(this@MainActivity)
            rowSingleKey.rowSwitch.setChecked(singleKey, false)
            rowSingleKey.rowSubtitle.setText(
                if (singleKey) R.string.single_key_enabled else R.string.single_key_disabled,
            )
            updatePrevKeyAvailability(!singleKey)

            val soundEnabled = ConfigManager.isSoundEffectsEnabled(this@MainActivity)
            rowAudioCues.rowSwitch.setChecked(soundEnabled, false)
            rowAudioCues.rowSubtitle.setText(
                if (soundEnabled) R.string.audio_cues_on else R.string.audio_cues_off,
            )
        }
    }

    private fun updatePrevKeyAvailability(enabled: Boolean) {
        with(binding.paneSwitching.rowPrevKey.root) {
            isEnabled = enabled
            isFocusable = enabled
            alpha = if (enabled) 1.0f else 0.4f
        }
    }

    private fun updateAppsPane() {
        with(binding.paneApps) {
            rowSortOrder.rowValue.text = getSortModeLabel(ConfigManager.getSortMode(this@MainActivity))

            val showCustomOrder = ConfigManager.getSortMode(this@MainActivity) == ConfigManager.SortMode.CUSTOM
            rowCustomOrder.root.visibility = if (showCustomOrder) View.VISIBLE else View.GONE

            val allApps = AppRepository.getAllApps(this@MainActivity)
            val enabledFilter = ConfigManager.getEnabledPackages(this@MainActivity)
            val visibleCount = if (enabledFilter != null && enabledFilter.isNotEmpty()) {
                allApps.count { enabledFilter.contains(it.packageName) }
            } else {
                allApps.size
            }
            rowVisibleApps.rowValue.text = getString(R.string.visible_apps_count_fmt, visibleCount, allApps.size)
        }
    }

    private fun updateDockSizingPane() {
        with(binding.paneDockSizing) {
            val dockScale = ConfigManager.getDockScalePercent(this@MainActivity)
            rowDockSize.rowSeekBar.progress = dockScale - 60
            rowDockSize.rowValue.text = getDockSizeLabel(dockScale)

            val dockTrans = ConfigManager.getDockTransparencyPercent(this@MainActivity)
            rowDockTransparency.rowSeekBar.progress = dockTrans
            rowDockTransparency.rowValue.text = getDockTransparencyLabel(dockTrans)

            val dockMargin = ConfigManager.getDockBottomMarginDp(this@MainActivity)
            rowDockMargin.rowSeekBar.progress = dockMargin
            rowDockMargin.rowValue.text = getDockMarginLabel(dockMargin)

            val dockDim = ConfigManager.getDockBackgroundDimPercent(this@MainActivity)
            rowDockDim.rowSeekBar.progress = dockDim
            rowDockDim.rowValue.text = getDockDimLabel(dockDim)
        }
    }

    private fun updateDockAppearancePane() {
        with(binding.paneDockAppearance) {
            val showAppName = ConfigManager.isShowAppNameEnabled(this@MainActivity)
            rowAppName.rowSwitch.setChecked(showAppName, false)

            val countdownEnabled = ConfigManager.isCountdownBarEnabled(this@MainActivity)
            rowCountdown.rowSwitch.setChecked(countdownEnabled, false)

            updateCountdownRowState(showAppName)
        }
    }

    private fun updateCountdownRowState(appNameEnabled: Boolean) {
        with(binding.paneDockAppearance.rowCountdown.root) {
            isEnabled = appNameEnabled
            isFocusable = appNameEnabled
            alpha = if (appNameEnabled) 1.0f else 0.4f
        }
    }

    private fun updateShortcutsPane() {
        with(binding.paneShortcuts) {
            val mappings = ConfigManager.getCustomMappings(this@MainActivity)
            if (mappings.isEmpty()) {
                tvNoShortcutsHint.visibility = View.VISIBLE
                rvShortcuts.visibility = View.GONE
            } else {
                tvNoShortcutsHint.visibility = View.GONE
                rvShortcuts.visibility = View.VISIBLE
                rvShortcuts.adapter = MappingAdapter(this@MainActivity, mappings) { keyCode ->
                    showRemoveShortcutDialog(keyCode)
                }
            }
        }
    }

    private fun showRemoveShortcutDialog(keyCode: Int) {
        val dialog = AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.dialog_remove_shortcut_title)
            .setMessage(getString(R.string.dialog_remove_shortcut_msg, ConfigManager.getKeyLabel(keyCode)))
            .setPositiveButton(R.string.delete) { _, _ ->
                ConfigManager.removeCustomMapping(this@MainActivity, keyCode)
                updateShortcutsPane()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        // AlertDialog leaves focus on the dialog window rather than a button here,
        // which makes it unusable with a D-pad, so target CANCEL explicitly.
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.requestFocus()
    }

    private fun isServiceCurrentlyEnabled(): Boolean {
        // The service being listed in enabled_accessibility_services is not enough: when the
        // master accessibility switch is off the entry survives but nothing is bound, so no key
        // events are intercepted. Both conditions must hold for switching to actually work.
        val masterEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!masterEnabled) return false

        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        if (!enabledServices.contains(KeyInterceptorService::class.java.name)) return false

        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo?.serviceInfo?.packageName == packageName }
    }

    private fun updateServiceStatus() {
        val isEnabled = isServiceCurrentlyEnabled()
        with(binding) {
            tvServiceStatusLabel.setText(
                if (isEnabled) R.string.service_status_active else R.string.service_status_inactive,
            )
            tvServiceStatusLabel.setTextColor(
                ContextCompat.getColorStateList(this@MainActivity, if (isEnabled) R.color.text_primary else R.color.text_secondary),
            )
            serviceStatusDot.setBackgroundResource(
                if (isEnabled) R.drawable.bg_status_dot_on else R.drawable.bg_status_dot_off,
            )
        }
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
            "CUSTOM" -> openAppPickerForShortcut(keyCode)
        }
    }

    private fun openAppPickerForShortcut(keyCode: Int) {
        AppSelectionDialog(
            this,
            { chosenApp ->
                ConfigManager.saveCustomMapping(this, keyCode, chosenApp.packageName)
                Toast.makeText(this, getString(R.string.key_mapped_toast, ConfigManager.getKeyLabel(keyCode), chosenApp.name), Toast.LENGTH_SHORT).show()
                updateUi()
            },
            {},
        ).show()
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
                AdbSetupDialog(this) {}.show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun tryEnableServiceProgrammatically(): Boolean = try {
        val serviceComponent = "$packageName/${KeyInterceptorService::class.java.name}"
        val currentServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: ""

        val newServices = if (!currentServices.contains(serviceComponent)) {
            if (currentServices.isBlank()) serviceComponent else "$currentServices:$serviceComponent"
        } else {
            currentServices
        }

        val setServices = Settings.Secure.putString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            newServices,
        )
        val setEnabled = Settings.Secure.putInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            1,
        )
        setServices && setEnabled
    } catch (e: Exception) {
        false
    }

    private fun launchTvSettings() {
        val intentsToTry = listOf(
            Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings"),
            Intent(Settings.ACTION_SETTINGS),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.device.apps.AppsActivity"),
        )

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                return
            } catch (e: Exception) {
            }
        }
        Toast.makeText(this, getString(R.string.open_accessibility_manual_toast), Toast.LENGTH_LONG).show()
    }

    private fun ViewSettingsRowActionBinding.configure(
        icon: Int,
        title: Int,
    ) {
        rowIcon.setImageResource(icon)
        rowTitle.setText(title)
    }

    private fun ViewSettingsRowActionBinding.configure(
        icon: Int,
        title: Int,
        subtitle: Int,
    ) {
        configure(icon, title)
        rowSubtitle.setText(subtitle)
        rowSubtitle.visibility = View.VISIBLE
    }

    private fun ViewSettingsRowActionBinding.hideValue() {
        rowValue.visibility = View.GONE
    }

    private fun ViewSettingsRowToggleBinding.configure(
        icon: Int,
        title: Int,
        subtitle: Int,
    ) {
        rowIcon.setImageResource(icon)
        rowTitle.setText(title)
        rowSubtitle.setText(subtitle)
        rowSubtitle.visibility = View.VISIBLE
        rowSwitch.isFocusable = false
    }

    private fun ViewSettingsRowSliderBinding.configure(
        icon: Int,
        title: Int,
    ) {
        rowIcon.setImageResource(icon)
        rowTitle.setText(title)
    }

    private companion object {
        const val KEY_ACTIVE_TAB = "active_tab"
    }
}

private fun SeekBar.setOnSeekBarChangeListener(onProgress: (Int) -> Unit) {
    setOnSeekBarChangeListener(
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean,
            ) {
                onProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        },
    )
}
