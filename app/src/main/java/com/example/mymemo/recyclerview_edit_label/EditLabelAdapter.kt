package com.example.mymemo.recyclerview_edit_label

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.example.mymemo.databinding.ItemEditLabelBinding
import com.example.mymemo.util.DiffUtilItemCallback

class EditLabelAdapter(private var recyclerViewInterface: IEditLabel) :
    ListAdapter<String, EditLabelViewHolder>(DiffUtilItemCallback.stringDiffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditLabelViewHolder {
        val binding =
            ItemEditLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EditLabelViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: EditLabelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}