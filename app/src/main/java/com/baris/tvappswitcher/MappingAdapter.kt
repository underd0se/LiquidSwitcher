package com.baris.tvappswitcher

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MappingAdapter(
    private val context: Context,
    private val mappings: Map<Int, String>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<MappingAdapter.ViewHolder>() {

    private val keys = mappings.keys.toList()
    private val pm = context.packageManager

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKeyCode: TextView = view.findViewById(R.id.tvKeyCode)
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mapping, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val keyCode = keys[position]
        val packageName = mappings[keyCode] ?: return

        holder.tvKeyCode.text = ConfigManager.getKeyLabel(keyCode)

        val cachedApp = AppRepository.getAllApps(context).find { it.packageName == packageName }
        if (cachedApp != null) {
            holder.tvName.text = cachedApp.name
            holder.ivIcon.setImageDrawable(cachedApp.icon)
        } else {
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                holder.tvName.text = pm.getApplicationLabel(appInfo)
                holder.ivIcon.setImageDrawable(pm.getApplicationIcon(appInfo))
            } catch (e: PackageManager.NameNotFoundException) {
                holder.tvName.text = context.getString(R.string.app_uninstalled_fmt, packageName)
                holder.ivIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onClick(keys[pos])
            }
        }
    }

    override fun getItemCount() = keys.size
}
