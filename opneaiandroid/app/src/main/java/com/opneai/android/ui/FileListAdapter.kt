package com.opneai.android.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opneai.android.R
import com.opneai.android.data.FileItem

class FileListAdapter(
    private val onItemClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    private val items = mutableListOf<FileItem>()

    fun updateItems(newItems: List<FileItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.item_icon)
        private val name: TextView = itemView.findViewById(R.id.item_name)
        private val type: TextView = itemView.findViewById(R.id.item_type)

        fun bind(item: FileItem, onItemClick: (FileItem) -> Unit) {
            name.text = item.name
            type.text = if (item.type == "folder") "Folder" else "File"
            val iconRes = if (item.type == "folder") {
                android.R.drawable.ic_menu_manage
            } else {
                android.R.drawable.ic_menu_agenda
            }
            icon.setImageResource(iconRes)
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
