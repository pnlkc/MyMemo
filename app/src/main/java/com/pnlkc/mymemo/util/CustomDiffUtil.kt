package com.pnlkc.mymemo.util

import androidx.recyclerview.widget.DiffUtil
import com.pnlkc.mymemo.room.MemoEntity

// notifyDataSetChanged()를 대신할 DiffUtil
// itemCallback은 리사이클러뷰 Adapter에서 ListAdapter() 상속받아서 사용
object CustomDiffUtil {

    // DiffUtilCallback은 두 항목의 사이즈까지 비교하는 듯 보임
    class DiffUtilCallback(
        private val oldList: List<Any>,
        private val newList: List<Any>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    val stringDiffUtilItemCallback = object: DiffUtil.ItemCallback<String>() {
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem

        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }

    val memoDiffUtilItemCallback = object : DiffUtil.ItemCallback<MemoEntity>() {
        override fun areContentsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
            return oldItem.id == newItem.id
        }
    }
}