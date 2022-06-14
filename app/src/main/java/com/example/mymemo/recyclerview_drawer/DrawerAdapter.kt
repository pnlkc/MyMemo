package com.example.mymemo.recyclerview_drawer

import android.text.method.TextKeyListener.clear
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemo.databinding.ItemDrawerBinding
import com.example.mymemo.recyclerview_edit_memo_label.LabelViewHolder
import com.example.mymemo.util.CustomDiffUtil
import java.util.Collections.addAll

class DrawerAdapter(private var recyclerViewInterface: IDrawerRecyclerView) :
    ListAdapter<String, DrawerViewHolder>(CustomDiffUtil.stringDiffUtilItemCallback) {

    private var selectedLabel: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrawerViewHolder {
        val binding =
            ItemDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DrawerViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: DrawerViewHolder, position: Int) {
        holder.bind(getItem(position), selectedLabel)
    }

    fun updateList(labelList: MutableList<String>, selectedLabel: String?) {
        this.selectedLabel = selectedLabel
        submitList(labelList)
    }
}