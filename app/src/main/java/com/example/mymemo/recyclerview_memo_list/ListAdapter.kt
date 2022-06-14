package com.example.mymemo.recyclerview_memo_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.example.mymemo.databinding.ItemListBinding
import com.example.mymemo.room.MemoEntity
import com.example.mymemo.util.CustomDiffUtil

class ListAdapter(private var recyclerViewInterface: IListRecyclerVIew) :
    ListAdapter<MemoEntity, ListViewHolder>(CustomDiffUtil.memoDiffUtilItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding =
            ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

