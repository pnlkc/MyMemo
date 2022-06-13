package com.example.mymemo.recyclerview_memo_search

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemo.databinding.ItemSearchBinding
import com.example.mymemo.room.MemoEntity

class SearchAdapter(private var recyclerViewInterface: ISearchRecyclerView) :
    RecyclerView.Adapter<SearchViewHolder>() {

    private var memoList = mutableListOf<MemoEntity>()
    private var searchTerm = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding =
            ItemSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding, recyclerViewInterface)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(memoList[position], searchTerm)
    }

    override fun getItemCount(): Int {
        return memoList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setDataNotify(memo: MutableList<MemoEntity>, string: String) {
        searchTerm = string
        memoList = memo

        // 검색어 변경시에는 diffUtil 사용하는 것보다 notifyDataSetChanged()을 사용하는게 더 자연스러운듯 보임
        notifyDataSetChanged()
    }
}