package com.baris.tvappswitcher.dialogs

import android.content.Context
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import com.baris.tvappswitcher.ConfigManager
import com.baris.tvappswitcher.R

class KeyCaptureDialog(
    private val context: Context,
    private val target: String,
    private val title: String,
    private val onKeyCaptured: (Int) -> Unit,
    private val onDismiss: () -> Unit,
) {

    private var dialog: AlertDialog? = null

    /** Stops listening and closes the dialog, e.g. once a key arrives via broadcast. */
    fun dismiss() {
        ConfigManager.setListening(context, false)
        dialog?.dismiss()
    }

    fun show() {
        ConfigManager.setListening(context, true, target)

        dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(R.string.dialog_press_remote_key)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel) { _, _ ->
                ConfigManager.setListening(context, false)
            }
            .setOnCancelListener {
                ConfigManager.setListening(context, false)
            }
            .setOnKeyListener { _, keyCode, event ->
                if (isStandardNavKey(keyCode)) {
                    false
                } else if (event.action == KeyEvent.ACTION_DOWN) {
                    ConfigManager.setListening(context, false)
                    dialog?.dismiss()
                    onKeyCaptured(keyCode)
                    true
                } else {
                    false
                }
            }
            .show()

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
}
