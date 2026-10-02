package com.baris.tvappswitcher.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baris.tvappswitcher.AppInfo
import com.baris.tvappswitcher.AppListAdapter
import com.baris.tvappswitcher.AppRepository
import com.baris.tvappswitcher.ConfigManager
import com.baris.tvappswitcher.R
import kotlin.comparisons.compareBy

class AppSelectionDialog(
    private val context: Context,
    private val onChosen: (AppInfo) -> Unit,
    private val onDismiss: () -> Unit,
) {

    private var dialog: AlertDialog? = null

    fun show() {
        val apps = getAllInstalledApps().sortedBy { it.name.lowercase() }

        val rv = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            setPadding(16.dp, 16.dp, 16.dp, 16.dp)
            setBackgroundColor(context.getColor(R.color.surface_card))
        }

        val previousFocus = (context as? androidx.appcompat.app.AppCompatActivity)?.currentFocus

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.dialog_select_app_title)
            .setView(rv)
            .setOnDismissListener {
                previousFocus?.requestFocus()
                onDismiss()
            }
            .show()

        rv.adapter = AppListAdapter(apps) { app ->
            dialog?.dismiss()
            onChosen(app)
        }
    }

    private fun getAllInstalledApps(): List<AppInfo> {
        val all = AppRepository.getAllApps(context)
        val customOrder = ConfigManager.getCustomAppOrder(context)
        return all.sortedWith(
            compareBy<AppInfo> {
                val idx = customOrder.indexOf(it.packageName)
                if (idx >= 0) idx else Int.MAX_VALUE
            }.thenBy { it.name.lowercase() },
        )
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}
