package com.baris.tvappswitcher.dialogs

import android.content.Context
import android.view.KeyEvent
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.baris.tvappswitcher.ConfigManager
import com.baris.tvappswitcher.R

class KeyTesterDialog(
    private val context: Context,
    private val onDismiss: () -> Unit,
) {

    private var dialog: AlertDialog? = null
    private var testDialogTextView: TextView? = null

    fun show() {
        ConfigManager.setListening(context, true, "TEST")

        val tv = TextView(context).apply {
            setPadding(48.dp, 36.dp, 48.dp, 28.dp)
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16f
            text = context.getString(R.string.dialog_test_key_msg)
            setLineSpacing(0f, 1.3f)
        }
        testDialogTextView = tv

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.dialog_test_key_title)
            .setView(tv)
            .setCancelable(true)
            .setPositiveButton(R.string.done) { _, _ ->
                ConfigManager.setListening(context, false)
            }
            .setOnDismissListener {
                ConfigManager.setListening(context, false)
                onDismiss()
            }
            .setOnKeyListener { _, keyCode, event ->
                if (isStandardNavKey(keyCode)) {
                    false
                } else if (event.action == KeyEvent.ACTION_DOWN) {
                    testDialogTextView?.text = context.getString(
                        R.string.dialog_test_key_detected,
                        ConfigManager.getKeyLabel(keyCode),
                        keyCode,
                    )
                    true
                } else {
                    false
                }
            }
            .create()

        dialog?.show()
        dialog?.setupTvFocus()
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

    private fun AlertDialog.setupTvFocus(): AlertDialog {
        val previousFocus = (context as? androidx.appcompat.app.AppCompatActivity)?.currentFocus
        setOnDismissListener {
            ConfigManager.setListening(context, false)
            previousFocus?.requestFocus()
            onDismiss()
        }
        return this
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}
