package com.baris.tvappswitcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit,
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {
    class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val app = apps[position]
        holder.ivIcon.setImageDrawable(AppRepository.getAppIcon(holder.itemView.context, app))
        holder.tvName.text = app.name
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onClick(apps[pos])
            }
        }
    }

    override fun getItemCount() = apps.size
}
