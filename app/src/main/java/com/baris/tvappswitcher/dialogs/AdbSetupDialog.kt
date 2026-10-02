package com.baris.tvappswitcher.dialogs

import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.baris.tvappswitcher.R

class AdbSetupDialog(
    private val context: Context,
    private val onDismiss: () -> Unit,
) {

    fun show() {
        val cmd1 = "adb shell settings put secure enabled_accessibility_services com.baris.tvappswitcher/com.baris.tvappswitcher.KeyInterceptorService"
        val cmd2 = "adb shell settings put secure accessibility_enabled 1"
        val message = context.getString(R.string.adb_setup_msg, cmd1, cmd2)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.adb_setup_title)
            .setMessage(message)
            .setPositiveButton(R.string.close, null)
            .setOnDismissListener { onDismiss() }
            .show()

        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
    }
}
