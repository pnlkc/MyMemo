package com.example.mymemo.recyclerview_memo_list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemo.databinding.ItemListBinding
import com.example.mymemo.room.MemoEntity

class ListViewHolder(
    binding: ItemListBinding,
    private var recyclerViewInterface: IListRecyclerVIew,
) : RecyclerView.ViewHolder(binding.root),
    View.OnClickListener, View.OnLongClickListener {

    private val memoItemTextView = binding.memoItemTextView
    private val itemCardView = binding.itemCardView

    init {
        itemCardView.setOnClickListener(this)
        itemCardView.setOnLongClickListener(this)
    }


    fun bind(memo: MemoEntity) {
        memoItemTextView.text = memo.title
    }

    override fun onClick(view: View?) {
        when (view) {
            // 메모 카드뷰 클릭
            itemCardView -> {
                recyclerViewInterface.memoItemClicked(bindingAdapterPosition)
            }
        }
    }

    override fun onLongClick(view: View?): Boolean {
        when (view) {
            // 메모 카드뷰 클릭
            itemCardView -> {
                recyclerViewInterface.memoItemLongClicked(bindingAdapterPosition)
            }
        }
        return true
    }
}