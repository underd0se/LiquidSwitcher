package com.baris.tvappswitcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AppVisibilityAdapter(
    private val apps: List<AppInfo>,
    private val enabledPackages: MutableSet<String>,
    private val onCountChanged: () -> Unit,
) : RecyclerView.Adapter<AppVisibilityAdapter.ViewHolder>() {
    class ViewHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view) {
        val root: View = view
        val ivIcon: ImageView = view.findViewById(R.id.ivVisIcon)
        val tvName: TextView = view.findViewById(R.id.tvVisName)
        val tvStatus: TextView = view.findViewById(R.id.tvVisStatus)
        val cbCheck: AppCompatCheckBox = view.findViewById(R.id.cbVisCheck)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_app_visibility, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        val app = apps[position]
        val isChecked = enabledPackages.contains(app.packageName)

        holder.ivIcon.setImageDrawable(AppRepository.getAppIcon(holder.itemView.context, app))
        holder.tvName.text = app.name
        holder.cbCheck.isChecked = isChecked

        updateStatusView(holder.tvStatus, isChecked)

        holder.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val currentApp = apps[pos]
            val nextChecked = !enabledPackages.contains(currentApp.packageName)
            if (nextChecked) {
                enabledPackages.add(currentApp.packageName)
            } else {
                enabledPackages.remove(currentApp.packageName)
            }
            holder.cbCheck.isChecked = nextChecked
            updateStatusView(holder.tvStatus, nextChecked)
            onCountChanged()
        }
    }

    private fun updateStatusView(
        tvStatus: TextView,
        isChecked: Boolean,
    ) {
        val context = tvStatus.context
        if (isChecked) {
            tvStatus.text = context.getString(R.string.visibility_status_visible)
            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.accent_primary))
        } else {
            tvStatus.text = context.getString(R.string.visibility_status_hidden)
            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        }
    }

    fun selectAll() {
        enabledPackages.clear()
        enabledPackages.addAll(apps.map { it.packageName })
        notifyDataSetChanged()
        onCountChanged()
    }

    fun deselectAll() {
        enabledPackages.clear()
        notifyDataSetChanged()
        onCountChanged()
    }

    override fun getItemCount(): Int = apps.size
}
