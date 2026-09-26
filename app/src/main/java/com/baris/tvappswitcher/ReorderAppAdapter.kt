package com.baris.tvappswitcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView

class ReorderAppAdapter(
    private val apps: MutableList<AppInfo>,
    private val onOrderChanged: (List<AppInfo>) -> Unit,
    private val onItemClick: (AppInfo, Int) -> Unit
) : RecyclerView.Adapter<ReorderAppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvOrderRank)
        val ivIcon: ImageView = view.findViewById(R.id.ivReorderAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvReorderAppName)
        val btnUp: AppCompatButton = view.findViewById(R.id.btnMoveUp)
        val btnDown: AppCompatButton = view.findViewById(R.id.btnMoveDown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reorder_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.tvRank.text = "#${position + 1}"
        holder.ivIcon.setImageDrawable(AppRepository.getAppIcon(holder.itemView.context, app))
        holder.tvName.text = app.name

        holder.btnUp.isEnabled = position > 0
        holder.btnUp.alpha = if (position > 0) 1.0f else 0.3f

        holder.btnDown.isEnabled = position < apps.size - 1
        holder.btnDown.alpha = if (position < apps.size - 1) 1.0f else 0.3f

        holder.btnUp.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos > 0) {
                moveItem(currentPos, currentPos - 1)
            }
        }

        holder.btnDown.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos in 0 until apps.size - 1) {
                moveItem(currentPos, currentPos + 1)
            }
        }

        holder.itemView.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos in 0 until apps.size) {
                onItemClick(apps[currentPos], currentPos)
            }
        }
    }

    fun moveItem(fromPos: Int, toPos: Int) {
        if (fromPos == toPos || fromPos !in 0 until apps.size || toPos !in 0 until apps.size) return
        val item = apps.removeAt(fromPos)
        apps.add(toPos, item)
        notifyItemMoved(fromPos, toPos)
        notifyItemRangeChanged(minOf(fromPos, toPos), maxOf(fromPos, toPos) - minOf(fromPos, toPos) + 1)
        onOrderChanged(apps)
    }

    override fun getItemCount() = apps.size
}
