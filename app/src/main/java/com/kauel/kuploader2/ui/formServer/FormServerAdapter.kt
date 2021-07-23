package com.kauel.kuploader2.ui.formServer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kauel.kuploader2.api.server.Server
import com.kauel.kuploader2.databinding.ItemListServerBinding

class FormServerAdapter(private val listener: OnItemClickListener) :
    ListAdapter<Server, FormServerAdapter.UploadFileViewHolder>(UploadFileComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadFileViewHolder {
        val binding =
            ItemListServerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UploadFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UploadFileViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) {
            holder.bind(currentItem, listener)
        }
    }

    class UploadFileViewHolder(private val binding: ItemListServerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(server: Server, listener: OnItemClickListener) {
            binding.apply {
                tvNameServer.text = server.name
                tvUrlServer.text = server.url
                btnDeleteServer.setOnClickListener {
                    listener.onItemClick(server)
                }
            }
        }
    }

    class UploadFileComparator : DiffUtil.ItemCallback<Server>() {
        override fun areItemsTheSame(oldItem: Server, newItem: Server): Boolean =
            oldItem.id == newItem.id


        override fun areContentsTheSame(oldItem: Server, newItem: Server): Boolean =
            oldItem == newItem

    }

    interface OnItemClickListener {
        fun onItemClick(server: Server)
    }
}