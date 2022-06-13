package com.example.mymemo.recyclerview_drawer

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemo.R
import com.example.mymemo.databinding.ItemDrawerBinding

class DrawerViewHolder(
    binding: ItemDrawerBinding,
    private var recyclerViewInterface: IDrawerRecyclerView,
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {

    private val labelTextView = binding.labelTextview
    private val labelLinearLayout = binding.labelLinearLayout

    init {
        labelLinearLayout.setOnClickListener(this)
        labelLinearLayout.setOnLongClickListener(this)
    }

    fun bind(label: String, selectedLabel: String?) {
        labelTextView.text = label
        if (label == selectedLabel) {
            labelLinearLayout.setBackgroundResource(R.drawable.drawer_selected_bg)
        } else {
            labelLinearLayout.setBackgroundResource(R.drawable.drawer_unselected_bg)
        }
    }

    override fun onClick(view: View?) {
        when (view) {
            // 라벨 클릭
            labelLinearLayout -> {
                recyclerViewInterface.memoItemClicked(bindingAdapterPosition)
            }
        }
    }

    override fun onLongClick(view: View?): Boolean {
        when (view) {
            // 라벨 클릭
            labelLinearLayout -> {
                recyclerViewInterface.memoItemLongClicked(bindingAdapterPosition)
            }
        }
        return true
    }
}