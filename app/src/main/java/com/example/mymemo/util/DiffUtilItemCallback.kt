package com.example.mymemo.util

import androidx.recyclerview.widget.DiffUtil
import com.example.mymemo.room.MemoEntity

// notifyDataSetChanged()를 대신할 DiffUtil
// 리사이클러뷰 Adapter에서 ListAdapter() 상속받아서 사용
object DiffUtilItemCallback {
    val stringDiffUtil = object: DiffUtil.ItemCallback<String>() {
        // 이걸 false 안하면 맨 마지막에 아이템 추가시 리사이클러뷰 업데이트가 안되는 문제 발생
        override fun areContentsTheSame(oldItem: String, newItem: String) = false

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