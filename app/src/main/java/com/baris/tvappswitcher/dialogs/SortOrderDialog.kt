package com.baris.tvappswitcher.dialogs

import android.content.Context
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.baris.tvappswitcher.ConfigManager
import com.baris.tvappswitcher.R

class SortOrderDialog(
    private val context: Context,
    private val onDismiss: () -> Unit,
    private val onReorderRequested: () -> Unit,
) {

    fun show() {
        val modes = ConfigManager.SortMode.values()
        val labels = modes.map { getSortModeLabel(it) }.toTypedArray()
        val currentMode = ConfigManager.getSortMode(context)
        val currentIndex = modes.indexOf(currentMode).coerceAtLeast(0)

        val adapter = ArrayAdapter(context, R.layout.item_dialog_singlechoice, labels)

        AlertDialog.Builder(context)
            .setTitle(R.string.dialog_select_sort_order)
            .setSingleChoiceItems(adapter, currentIndex) { dialog, which ->
                val selectedMode = modes[which]
                ConfigManager.setSortMode(context, selectedMode)
                onDismiss()

                if (selectedMode == ConfigManager.SortMode.CUSTOM) {
                    onReorderRequested()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { onDismiss() }
            .show()
            .setupTvFocus()
    }

    private fun getSortModeLabel(mode: ConfigManager.SortMode): String = when (mode) {
        ConfigManager.SortMode.NAME -> context.getString(R.string.sort_alphabetical)
        ConfigManager.SortMode.RECENCY -> context.getString(R.string.sort_recency)
        ConfigManager.SortMode.CUSTOM -> context.getString(R.string.sort_custom)
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
}
