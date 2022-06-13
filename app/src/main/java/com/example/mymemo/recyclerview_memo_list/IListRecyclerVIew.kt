package com.example.mymemo.recyclerview_memo_list

interface IListRecyclerVIew {
    // 리사이클러뷰 아이템 클릭시
    fun memoItemClicked(position: Int)

    fun memoItemLongClicked(position: Int)
}