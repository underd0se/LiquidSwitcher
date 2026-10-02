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
import android.view.accessibility.AccessibilityManager
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.baris.tvappswitcher.databinding.ActivityMainBinding
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

        initViews()
        setupListeners()
        setupFocusEffects()
        updateUi()

        ConfigManager.setListening(this, false)
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
        return super.dispatchKeyEvent(event)
    }

    private fun initViews() {
        with(binding) {
            sbDockSize.keyProgressIncrement = 5
            sbDockTransparency.keyProgressIncrement = 5
            sbDockMargin.keyProgressIncrement = 4
            sbDockDim.keyProgressIncrement = 5
            rvShortcuts.layoutManager = LinearLayoutManager(this@MainActivity)
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

            btnChangeNext.setOnClickListener {
                KeyCaptureDialog(this@MainActivity, "NEXT", getString(R.string.dialog_title_next_key), { keyCode ->
                    ConfigManager.setNextKey(this@MainActivity, keyCode)
                    Toast.makeText(this@MainActivity, getString(R.string.key_next_set_toast, ConfigManager.getKeyLabel(keyCode)), Toast.LENGTH_SHORT).show()
                    updateUi()
                }) {}.show()
            }

            btnChangePrev.setOnClickListener {
                KeyCaptureDialog(this@MainActivity, "PREV", getString(R.string.dialog_title_prev_key), { keyCode ->
                    ConfigManager.setPrevKey(this@MainActivity, keyCode)
                    Toast.makeText(this@MainActivity, getString(R.string.key_prev_set_toast, ConfigManager.getKeyLabel(keyCode)), Toast.LENGTH_SHORT).show()
                    updateUi()
                }) {}.show()
            }

            btnChangeDelay.setOnClickListener {
                DelaySelectionDialog(this@MainActivity) { updateUi() }.show()
            }

            btnChangeSortOrder.setOnClickListener {
                SortOrderDialog(this@MainActivity, { updateUi() }, { ReorderAppsDialog(this@MainActivity) { updateUi() }.show() }).show()
            }

            btnCustomizeOrder.setOnClickListener {
                ReorderAppsDialog(this@MainActivity) { updateUi() }.show()
            }

            btnManageVisibility.setOnClickListener {
                AppVisibilityDialog(this@MainActivity) { updateUi() }.show()
            }

            switchSingleKey.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setSingleKeyMode(this@MainActivity, isChecked)
                btnChangePrev.isEnabled = !isChecked
                btnChangePrev.alpha = if (isChecked) 0.4f else 1.0f
            }

            switchSound.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setSoundEffectsEnabled(this@MainActivity, isChecked)
                if (isChecked) {
                    SoundCueHelper.playTick(this@MainActivity)
                }
            }

            switchShowAppName.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setShowAppNameEnabled(this@MainActivity, isChecked)
                updateCountdownRowState(isChecked)
            }

            switchCountdown.setOnCheckedChangeListener { _, isChecked ->
                ConfigManager.setCountdownBarEnabled(this@MainActivity, isChecked)
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
                KeyCaptureDialog(this@MainActivity, "CUSTOM", getString(R.string.dialog_title_custom_key), { keyCode ->
                    AppSelectionDialog(this@MainActivity, { chosenApp ->
                        ConfigManager.saveCustomMapping(this@MainActivity, keyCode, chosenApp.packageName)
                        Toast.makeText(this@MainActivity, getString(R.string.key_mapped_toast, ConfigManager.getKeyLabel(keyCode), chosenApp.name), Toast.LENGTH_SHORT).show()
                        updateUi()
                    }) {}.show()
                }) {}.show()
            }

            btnTestKey.setOnClickListener {
                KeyTesterDialog(this@MainActivity) {}.show()
            }
        }
    }

    private fun setupFocusEffects() {
        with(binding) {
            val buttons = listOf(
                tvServiceStatus,
                btnChangeNext,
                btnChangePrev,
                btnChangeDelay,
                btnChangeSortOrder,
                btnCustomizeOrder,
                btnManageVisibility,
                btnAddDirectShortcut,
                btnTestKey,
            )

            for (btn in buttons) {
                btn.setOnFocusChangeListener { v, hasFocus ->
                    val scale = if (hasFocus) 1.05f else 1.0f
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

            val seekBars = listOf(sbDockSize, sbDockTransparency, sbDockMargin, sbDockDim)
            for (sb in seekBars) {
                sb.setOnFocusChangeListener { v, hasFocus ->
                    val scaleX = if (hasFocus) 1.03f else 1.0f
                    val scaleY = if (hasFocus) 1.15f else 1.0f
                    val transZ = if (hasFocus) 8f else 0f
                    v.animate()
                        .scaleX(scaleX)
                        .scaleY(scaleY)
                        .translationZ(transZ)
                        .setDuration(150)
                        .withLayer()
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
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        binding.btnChangeNext.post {
            binding.btnChangeNext.requestFocus()
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
        with(binding) {
            val nextKey = ConfigManager.getNextKey(this@MainActivity)
            val prevKey = ConfigManager.getPrevKey(this@MainActivity)
            val delaySec = ConfigManager.getSwitchDelaySeconds(this@MainActivity)
            val sortMode = ConfigManager.getSortMode(this@MainActivity)

            tvNextKey.text = ConfigManager.getKeyLabel(nextKey)
            tvPrevKey.text = ConfigManager.getKeyLabel(prevKey)

            tvSwitchDelay.text = ConfigManager.getSwitchDelayLabel(this@MainActivity, delaySec)
            btnChangeDelay.text = getString(R.string.btn_change_delay_fmt, ConfigManager.getSwitchDelayShortLabel(this@MainActivity, delaySec))

            tvSortOrder.text = getSortModeLabel(sortMode)

            if (sortMode == ConfigManager.SortMode.CUSTOM) {
                rowCustomOrder.visibility = View.VISIBLE
                btnChangeSortOrder.nextFocusDownId = R.id.btnCustomizeOrder
                switchSingleKey.nextFocusUpId = R.id.btnCustomizeOrder
            } else {
                rowCustomOrder.visibility = View.GONE
                btnChangeSortOrder.nextFocusDownId = R.id.switchSingleKey
                switchSingleKey.nextFocusUpId = R.id.btnChangeSortOrder
            }

            val allApps = AppRepository.getAllApps(this@MainActivity)
            val enabledFilter = ConfigManager.getEnabledPackages(this@MainActivity)
            val visibleCount = if (enabledFilter != null && enabledFilter.isNotEmpty()) {
                allApps.count { enabledFilter.contains(it.packageName) }
            } else {
                allApps.size
            }
            tvVisibleAppsCount.text = getString(R.string.visible_apps_count_fmt, visibleCount, allApps.size)
            btnManageVisibility.text = getString(R.string.btn_manage)

            val singleKey = ConfigManager.isSingleKeyMode(this@MainActivity)
            switchSingleKey.setChecked(singleKey, false)
            btnChangePrev.isEnabled = !singleKey
            btnChangePrev.alpha = if (singleKey) 0.4f else 1.0f

            val soundEnabled = ConfigManager.isSoundEffectsEnabled(this@MainActivity)
            switchSound.setChecked(soundEnabled, false)

            val showAppName = ConfigManager.isShowAppNameEnabled(this@MainActivity)
            switchShowAppName.setChecked(showAppName, false)
            updateCountdownRowState(showAppName)

            val countdownEnabled = ConfigManager.isCountdownBarEnabled(this@MainActivity)
            switchCountdown.setChecked(countdownEnabled, false)

            val dockScale = ConfigManager.getDockScalePercent(this@MainActivity)
            sbDockSize.progress = dockScale - 60
            tvDockSizeLabel.text = getDockSizeLabel(dockScale)

            val dockTrans = ConfigManager.getDockTransparencyPercent(this@MainActivity)
            sbDockTransparency.progress = dockTrans
            tvDockTransparencyLabel.text = getDockTransparencyLabel(dockTrans)

            val dockMargin = ConfigManager.getDockBottomMarginDp(this@MainActivity)
            sbDockMargin.progress = dockMargin
            tvDockMarginLabel.text = getDockMarginLabel(dockMargin)

            val dockDim = ConfigManager.getDockBackgroundDimPercent(this@MainActivity)
            sbDockDim.progress = dockDim
            tvDockDimLabel.text = getDockDimLabel(dockDim)

            updateShortcutsList()
            updateServiceStatus()
        }
    }

    private fun updateCountdownRowState(appNameEnabled: Boolean) {
        with(binding) {
            switchCountdown.isEnabled = appNameEnabled
            switchCountdown.isFocusable = appNameEnabled
            switchCountdown.alpha = if (appNameEnabled) 1.0f else 0.4f
            rowCountdown.alpha = if (appNameEnabled) 1.0f else 0.4f

            if (appNameEnabled) {
                switchShowAppName.nextFocusDownId = R.id.switchCountdown
                btnAddDirectShortcut.nextFocusUpId = R.id.switchCountdown
                btnTestKey.nextFocusUpId = R.id.switchCountdown
            } else {
                switchShowAppName.nextFocusDownId = R.id.btnAddDirectShortcut
                btnAddDirectShortcut.nextFocusUpId = R.id.switchShowAppName
                btnTestKey.nextFocusUpId = R.id.switchShowAppName
            }
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun updateShortcutsList() {
        with(binding) {
            val mappings = ConfigManager.getCustomMappings(this@MainActivity)
            if (mappings.isEmpty()) {
                tvNoShortcutsHint.visibility = View.VISIBLE
                rvShortcuts.visibility = View.GONE
            } else {
                tvNoShortcutsHint.visibility = View.GONE
                rvShortcuts.visibility = View.VISIBLE
                rvShortcuts.adapter = MappingAdapter(this@MainActivity, mappings) { keyCode ->
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.dialog_remove_shortcut_title)
                        .setMessage(getString(R.string.dialog_remove_shortcut_msg, ConfigManager.getKeyLabel(keyCode)))
                        .setPositiveButton(R.string.delete) { _, _ ->
                            ConfigManager.removeCustomMapping(this@MainActivity, keyCode)
                            updateShortcutsList()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
        }
    }

    private fun isServiceCurrentlyEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        if (enabledServices != null && enabledServices.contains("KeyInterceptorService")) {
            return true
        }

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
        with(binding) {
            val isEnabled = isServiceCurrentlyEnabled()
            if (isEnabled) {
                tvServiceStatus.text = getString(R.string.service_status_active)
            } else {
                tvServiceStatus.text = getString(R.string.service_status_inactive)
            }
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
        }
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
}
