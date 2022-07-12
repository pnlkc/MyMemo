package com.pnlkc.mymemo.recyclerview_select_label

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.mymemo.databinding.ItemSelectLabelBinding

class SelectLabelViewHolder(
    binding: ItemSelectLabelBinding,
    private var recyclerViewInterface: ISelectLabel
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    private val labelTextView = binding.labelTextview
    private val labelCheckBox = binding.labelCheckbox
    private val labelConstraintLayout = binding.labelConstraintLayout

    init {
        labelCheckBox.setOnClickListener(this)
        labelConstraintLayout.setOnClickListener(this)
    }

    fun bind(label: String, memoLabelList: MutableList<String>) {
        labelTextView.text = label
        labelCheckBox.isChecked = memoLabelList.contains(label)
    }

    override fun onClick(view: View?) {
        when (view) {
            // 라벨 체크박스 클릭
            labelCheckBox -> {
                recyclerViewInterface.labelCheckBoxClicked(bindingAdapterPosition)
            }
            labelConstraintLayout -> {
                recyclerViewInterface.labelCheckBoxClicked(bindingAdapterPosition)
                labelCheckBox.isChecked = !labelCheckBox.isChecked
            }
        }
    }


}