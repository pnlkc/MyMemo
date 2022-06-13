package com.example.mymemo.util

import androidx.recyclerview.widget.DiffUtil
import com.example.mymemo.room.MemoEntity

// notifyDataSetChanged()를 대신할 DiffUtil
// 리사이클러뷰 Adapter에서 ListAdapter() 상속받아서 사용
object DiffUtilItemCallback {
    val stringDiffUtil = object: DiffUtil.ItemCallback<String>() {
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem

        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }

    val memoDiffUtil = object: DiffUtil.ItemCallback<MemoEntity>() {
        override fun areContentsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: MemoEntity, newItem: MemoEntity): Boolean {
            return oldItem.id == newItem.id
        }
    }
}