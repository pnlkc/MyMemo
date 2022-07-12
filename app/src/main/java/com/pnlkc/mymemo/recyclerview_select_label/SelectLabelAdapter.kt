package com.pnlkc.mymemo.recyclerview_select_label

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.mymemo.databinding.ItemSelectLabelBinding
import com.pnlkc.mymemo.util.CustomDiffUtil

class SelectLabelAdapter(
    private val memoLabelList: MutableList<String>,
    private val recyclerViewInterface: ISelectLabel
) : RecyclerView.Adapter<SelectLabelViewHolder>() {

    private val labelList = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectLabelViewHolder {
        val binding =
            ItemSelectLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectLabelViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: SelectLabelViewHolder, position: Int) {
        holder.bind(labelList[position], memoLabelList)
    }

    override fun getItemCount(): Int = labelList.size

    fun setData(label: MutableList<String>) {
        label.let {
            val diffCallback = CustomDiffUtil.DiffUtilCallback(labelList, label)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            labelList.run {
                clear()
                addAll(label)
                diffResult.dispatchUpdatesTo(this@SelectLabelAdapter)
            }
        }
    }
}