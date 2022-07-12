package com.pnlkc.mymemo.recyclerview_memo_search

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.mymemo.R
import com.pnlkc.mymemo.databinding.ItemSearchBinding
import com.pnlkc.mymemo.room.MemoEntity
import com.pnlkc.mymemo.util.App

class SearchViewHolder(
    binding: ItemSearchBinding,
    private var recyclerViewInterface: ISearchRecyclerView,
) : RecyclerView.ViewHolder(binding.root),
    View.OnClickListener {

    private val itemCardView = binding.itemCardView
    private val titleTextView = binding.titleTextView
    private val memoTextView = binding.memoTextView

    init {
        itemCardView.setOnClickListener(this)
    }

    fun bind(memo: MemoEntity, searchTerm: String) {
        if (searchTerm.isNotBlank()) {
            // 메모 제목에 검색어가 있을 때
            if (memo.title.contains(searchTerm)) {
                titleTextView.text = highlightString(memo.title, searchTerm)
            } else {
                titleTextView.text = memo.title
            }

            // 메모 내용에 검색어가 있을 때
            if (memo.memo.contains(searchTerm)) {
                memoTextView.text = highlightString(memo.memo, searchTerm)

            } else {
                memoTextView.text = memo.memo
            }
        }

        // 메모가 내용 비었을 때
        if (memo.memo.isEmpty()) {
            memoTextView.visibility = View.GONE
        } else {
            memoTextView.visibility = View.VISIBLE
        }

        // 메모 제목이 비었을 때
        if (memo.title.isEmpty()) {
            titleTextView.hint = "제목 없음"
        }
    }

    // 검색어 강조하는 기능
    private fun highlightString(inputText: String, highlightWord: String): SpannableString {
        val spannableString = SpannableString(inputText)
        val start = inputText.indexOf(highlightWord)
        val end = start + highlightWord.length
        val bgColor = ContextCompat.getColor(App.context(), R.color.searchHighLightTextBg)
        val textColor = ContextCompat.getColor(App.context(), R.color.searchHighLightText)

        spannableString.setSpan(ForegroundColorSpan(textColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(BackgroundColorSpan(bgColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }

    override fun onClick(view: View?) {
        when (view) {
            // 메모 카드뷰 클릭
            itemCardView -> {
                recyclerViewInterface.memoItemClicked(bindingAdapterPosition)
            }
        }
    }
}