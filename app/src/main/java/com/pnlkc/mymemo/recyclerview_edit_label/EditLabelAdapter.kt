package com.pnlkc.mymemo.recyclerview_edit_label

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.mymemo.databinding.ItemEditLabelBinding
import com.pnlkc.mymemo.util.CustomDiffUtil

class EditLabelAdapter(private var recyclerViewInterface: IEditLabel) :
    RecyclerView.Adapter<EditLabelViewHolder>() {

    private val labelList = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditLabelViewHolder {
        val binding =
            ItemEditLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EditLabelViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: EditLabelViewHolder, position: Int) {
        holder.bind(labelList[position])
    }

    override fun getItemCount(): Int = labelList.size

    fun setData(label: MutableList<String>) {
        label.let {
            val diffCallback = CustomDiffUtil.DiffUtilCallback(labelList, label)
            val diffResult = DiffUtil.calculateDiff(diffCallback)

            labelList.run {
                clear()
                addAll(label)
                diffResult.dispatchUpdatesTo(this@EditLabelAdapter)
            }
        }
    }
}