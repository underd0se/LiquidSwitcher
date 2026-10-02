package com.baris.tvappswitcher.dialogs

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baris.tvappswitcher.AppInfo
import com.baris.tvappswitcher.AppRepository
import com.baris.tvappswitcher.ConfigManager
import com.baris.tvappswitcher.R
import com.baris.tvappswitcher.ReorderAppAdapter
import kotlin.comparisons.compareBy

class ReorderAppsDialog(
    private val context: Context,
    private val onDismiss: () -> Unit,
) {

    private var appsList: MutableList<AppInfo> = mutableListOf()

    fun show() {
        appsList = getAllInstalledApps().toMutableList()
        if (appsList.isEmpty()) {
            return
        }

        val rv = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            setPadding(20.dp, 16.dp, 20.dp, 16.dp)
            setBackgroundColor(context.getColor(R.color.surface_card))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (context.resources.displayMetrics.heightPixels * 0.55).toInt(),
            )
        }

        var adapter: ReorderAppAdapter? = null

        adapter = ReorderAppAdapter(
            apps = appsList,
            onOrderChanged = { updatedList ->
                val packageOrder = updatedList.map { it.packageName }
                ConfigManager.setCustomAppOrder(context, packageOrder)
                ConfigManager.setSortMode(context, ConfigManager.SortMode.CUSTOM)
            },
            onItemClick = { app, position ->
                showItemPositionMenu(app, position, appsList, adapter!!)
            },
        )

        rv.adapter = adapter

        val previousFocus = (context as? androidx.appcompat.app.AppCompatActivity)?.currentFocus

        AlertDialog.Builder(context)
            .setTitle(R.string.dialog_customize_order_title)
            .setView(rv)
            .setPositiveButton(R.string.done) { _, _ ->
                ConfigManager.setCustomAppOrder(context, appsList.map { it.packageName })
                ConfigManager.setSortMode(context, ConfigManager.SortMode.CUSTOM)
                onDismiss()
            }
            .setNeutralButton(R.string.reset_to_az) { _, _ ->
                val aToZ = appsList.sortedBy { it.name.lowercase() }
                appsList.clear()
                appsList.addAll(aToZ)
                adapter!!.notifyDataSetChanged()
                ConfigManager.setCustomAppOrder(context, aToZ.map { it.packageName })
                ConfigManager.setSortMode(context, ConfigManager.SortMode.CUSTOM)
                onDismiss()
            }
            .setOnDismissListener { onDismiss() }
            .show()
    }

    private fun showItemPositionMenu(
        app: AppInfo,
        position: Int,
        appsList: MutableList<AppInfo>,
        adapter: ReorderAppAdapter,
    ) {
        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (position > 0) {
            options.add(context.getString(R.string.move_to_top))
            actions.add { adapter.moveItem(position, 0) }

            options.add(context.getString(R.string.move_up))
            actions.add { adapter.moveItem(position, position - 1) }
        }

        if (position < appsList.size - 1) {
            options.add(context.getString(R.string.move_down))
            actions.add { adapter.moveItem(position, position + 1) }

            options.add(context.getString(R.string.move_to_bottom))
            actions.add { adapter.moveItem(position, appsList.size - 1) }
        }

        if (options.isEmpty()) return

        val menuAdapter = ArrayAdapter(context, R.layout.item_dialog_simple, options)
        AlertDialog.Builder(context)
            .setTitle("${app.name} (#${position + 1})")
            .setAdapter(menuAdapter) { _, which ->
                actions[which].invoke()
            }
            .show()
            .setupTvFocus()
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

    private fun AlertDialog.setupTvFocus(): AlertDialog {
        val previousFocus = (context as? androidx.appcompat.app.AppCompatActivity)?.currentFocus
        setOnDismissListener {
            previousFocus?.requestFocus()
        }
        return this
    }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}
