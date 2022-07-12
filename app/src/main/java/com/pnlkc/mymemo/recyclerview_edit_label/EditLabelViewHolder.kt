package com.pnlkc.mymemo.recyclerview_edit_label

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.mymemo.databinding.ItemEditLabelBinding

class EditLabelViewHolder(
    binding: ItemEditLabelBinding,
    private var recyclerViewInterface: IEditLabel
) : RecyclerView.ViewHolder(binding.root), View.OnLongClickListener {

    private val labelTextView = binding.labelTextview
    private val labelConstraintLayout = binding.labelConstraintLayout

    init {
        labelConstraintLayout.setOnLongClickListener(this)
    }

    fun bind(label: String) {
        labelTextView.text = label
    }

    override fun onLongClick(view: View?): Boolean {
        when (view) {
            labelConstraintLayout -> recyclerViewInterface.labelLongClicked(bindingAdapterPosition)
        }
        return true
    }
}