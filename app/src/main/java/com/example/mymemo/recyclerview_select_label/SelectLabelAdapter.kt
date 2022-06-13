package com.example.mymemo.recyclerview_select_label

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.example.mymemo.databinding.ItemSelectLabelBinding
import com.example.mymemo.util.DiffUtilItemCallback

class SelectLabelAdapter(
    private val memoLabelList: MutableList<String>, private val recyclerViewInterface: ISelectLabel,
) : ListAdapter<String, SelectLabelViewHolder>(DiffUtilItemCallback.stringDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectLabelViewHolder {
        val binding =
            ItemSelectLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectLabelViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: SelectLabelViewHolder, position: Int) {
        holder.bind(getItem(position), memoLabelList)
    }
}