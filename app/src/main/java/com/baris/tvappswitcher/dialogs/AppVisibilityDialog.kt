package com.baris.tvappswitcher.dialogs

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baris.tvappswitcher.AppRepository
import com.baris.tvappswitcher.AppVisibilityAdapter
import com.baris.tvappswitcher.ConfigManager
import com.baris.tvappswitcher.R

class AppVisibilityDialog(
    private val context: Context,
    private val onDismiss: () -> Unit,
) {

    private var dialog: AlertDialog? = null

    fun show() {
        val allApps = AppRepository.getAllApps(context).sortedBy { it.name.lowercase() }
        if (allApps.isEmpty()) {
            return
        }

        val currentEnabled = ConfigManager.getEnabledPackages(context)?.toMutableSet()
            ?: allApps.map { it.packageName }.toMutableSet()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(context.getColor(R.color.surface_card))
            setPadding(16.dp, 12.dp, 16.dp, 8.dp)
        }

        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8.dp, 4.dp, 8.dp, 12.dp)
        }

        val tvSummary = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            textSize = 14f
            setTextColor(context.getColor(R.color.accent_primary))
            text = context.getString(R.string.apps_saved_visible_toast, currentEnabled.size)
        }

        val btnSelectAll = AppCompatButton(context).apply {
            text = context.getString(R.string.select_all)
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.selector_tv_btn_bg)
            setPadding(20.dp, 8.dp, 20.dp, 8.dp)
            isFocusable = true
            isClickable = true
        }

        val btnDeselectAll = AppCompatButton(context).apply {
            text = context.getString(R.string.deselect_all)
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.selector_tv_btn_bg)
            setPadding(20.dp, 8.dp, 20.dp, 8.dp)
            isFocusable = true
            isClickable = true
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginStart = 12.dp
            layoutParams = lp
        }

        headerLayout.addView(tvSummary)
        headerLayout.addView(btnSelectAll)
        headerLayout.addView(btnDeselectAll)
        container.addView(headerLayout)

        val rv = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            setPadding(4.dp, 4.dp, 4.dp, 4.dp)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.heightPixels * 0.55).toInt(),
            )
            layoutParams = lp
        }

        lateinit var adapter: AppVisibilityAdapter
        fun updateSummary() {
            tvSummary.text = context.getString(R.string.apps_saved_visible_toast, currentEnabled.size)
        }

        adapter = AppVisibilityAdapter(
            apps = allApps,
            enabledPackages = currentEnabled,
            onCountChanged = { updateSummary() },
        )
        rv.adapter = adapter
        container.addView(rv)

        btnSelectAll.setOnClickListener { adapter.selectAll() }
        btnDeselectAll.setOnClickListener { adapter.deselectAll() }

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.dialog_manage_visibility_title)
            .setView(container)
            .setPositiveButton(R.string.save) { _, _ ->
                ConfigManager.setEnabledPackages(context, currentEnabled)
                AppRepository.refreshAppsAsync(context)
                onDismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { onDismiss() }
            .show()

        dialog?.setupTvFocus()
    }

    private fun AlertDialog.setupTvFocus(): AlertDialog {
        val previousFocus = (context as? androidx.appcompat.app.AppCompatActivity)?.currentFocus
        listView?.let { lv ->
            lv.setOnFocusChangeListener { _, hasFocus ->
                for (i in 0 until lv.childCount) {
                    val child = lv.getChildAt(i)
                    if (!hasFocus) {
                        child.isSelected = false
                    } else if (i == lv.selectedItemPosition - lv.firstVisiblePosition) {
                        child.isSelected = true
                    }
                }
            }
        }
        setOnDismissListener {
            previousFocus?.requestFocus()
        }
        return this
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}
