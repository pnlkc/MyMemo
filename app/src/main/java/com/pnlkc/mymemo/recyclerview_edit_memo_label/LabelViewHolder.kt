package com.pnlkc.mymemo.recyclerview_edit_memo_label

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.mymemo.databinding.ItemLabelBinding

class LabelViewHolder(binding: ItemLabelBinding, private val recyclerViewInterface: ILabel) :
    RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    private val labelTextView = binding.labelTextview

    init {
        labelTextView.setOnClickListener(this)
    }

    fun bind(label: String) {
        labelTextView.text = label
    }

    override fun onClick(view: View?) {
        when (view) {
            labelTextView -> recyclerViewInterface.labelClicked()
        }
    }
}