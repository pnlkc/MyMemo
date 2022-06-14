package com.example.mymemo.recyclerview_edit_memo_label

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.example.mymemo.databinding.ItemLabelBinding
import com.example.mymemo.util.CustomDiffUtil

class LabelAdapter(private var recyclerViewInterface: ILabel) :
    ListAdapter<String, LabelViewHolder>(CustomDiffUtil.stringDiffUtilItemCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val binding = ItemLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LabelViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}