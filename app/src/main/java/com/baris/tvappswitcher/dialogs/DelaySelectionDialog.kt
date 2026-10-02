package com.baris.tvappswitcher.dialogs

import android.content.Context
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.baris.tvappswitcher.ConfigManager
import com.baris.tvappswitcher.R

class DelaySelectionDialog(
    private val context: Context,
    private val onDismiss: () -> Unit,
) {

    fun show() {
        val options = arrayOf(
            context.getString(R.string.delay_1_fast),
            context.getString(R.string.delay_2_recommended),
            context.getString(R.string.delay_3),
            context.getString(R.string.delay_4),
            context.getString(R.string.delay_5_slow),
            context.getString(R.string.delay_manual),
        )
        val values = intArrayOf(1, 2, 3, 4, 5, 0)
        val currentDelay = ConfigManager.getSwitchDelaySeconds(context)
        val checkedIndex = values.indexOf(currentDelay).coerceAtLeast(0)

        val adapter = ArrayAdapter(context, R.layout.item_dialog_singlechoice, options)

        AlertDialog.Builder(context)
            .setTitle(R.string.dialog_select_countdown_title)
            .setSingleChoiceItems(adapter, checkedIndex) { dialog, which ->
                val selectedSeconds = values[which]
                ConfigManager.setSwitchDelaySeconds(context, selectedSeconds)
                onDismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener { onDismiss() }
            .show()
            .setupTvFocus()
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
