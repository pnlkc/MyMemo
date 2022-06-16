package com.example.mymemo

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymemo.databinding.FragmentMemoSearchBinding
import com.example.mymemo.recyclerview_memo_search.ISearchRecyclerView
import com.example.mymemo.recyclerview_memo_search.SearchAdapter
import com.example.mymemo.room.MemoEntity
import com.example.mymemo.util.MEMO_TYPE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MemoSearchFragment : Fragment(), ISearchRecyclerView {

    private var _binding: FragmentMemoSearchBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    private val searchAdapter = SearchAdapter(this)

    private var filterMemo: MutableList<MemoEntity> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMemoSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()

        detectInputTextChanging(binding.searchEditTextClearBtn, binding.searchEditText)

        CoroutineScope(Dispatchers.Main).launch {
            binding.mainResultLinearLayout.visibility = View.INVISIBLE
            delay(200)
            showKeyboardAndRequestFocus()
            delay(100)
            binding.mainResultLinearLayout.visibility = View.VISIBLE
        }

        binding.backButton.setOnClickListener {
            removeFragment()
        }

        binding.searchEditText.setOnKeyListener { myView, keyCode, _ ->
            hideKeyboard(myView, keyCode)
        }
    }

    // 리사이클러뷰 설정
    private fun setRecyclerView() {
        binding.searchRecyclerView.apply {
            adapter = searchAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
                    .apply { stackFromEnd = true }
        }
    }

    // EditText 값 변경되었을 때 기능
    private fun detectInputTextChanging(btn: ImageView, editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(text: CharSequence, p1: Int, p2: Int, p3: Int) {
                // 텍스트가 공백이 아니면 메모 검색하기
                if (text.isNotBlank()) {
                    filterMemo.clear()
                    val allMemo = memoViewModel.readAllData.value!!.toMutableList()
                    allMemo.removeFirst()
                    allMemo.forEach { memoEntity ->
                        if (memoEntity.memo.contains(text) || memoEntity.title.contains(text)) {
                            filterMemo.add(memoEntity)
                        }
                    }

                    searchAdapter.setDataNotify(filterMemo, text.toString())
                } else {
                    filterMemo.clear()
                    searchAdapter.setDataNotify(filterMemo, text.toString())
                }

                // 클리어버튼 및 검색결과 없음 뷰 visibility 설정
                if (text.isNotEmpty()) {
                    // 텍스트가 입력되면 클리어버튼 보이기
                    btn.visibility = View.VISIBLE
                    btn.setOnClickListener {
                        editText.text.clear()
                    }
                    binding.mainResultLinearLayout.visibility = View.INVISIBLE

                    if (filterMemo.isEmpty()) {
                        binding.noResultLinearLayout.visibility = View.VISIBLE
                    } else {
                        binding.noResultLinearLayout.visibility = View.INVISIBLE
                    }
                } else {
                    btn.visibility = View.INVISIBLE
                    binding.noResultLinearLayout.visibility = View.INVISIBLE
                    binding.mainResultLinearLayout.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(p0: Editable?) {}

        })
    }

    // 키보드 올리고 포커스 주는 기능
    private fun showKeyboardAndRequestFocus() {
        binding.searchEditText.requestFocus()
        val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        inputManager.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    // 엔터누르면 키보드 내려가고 포커스 사라지는 기능
    private fun hideKeyboard(view: View, keyCode: Int): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            inputManager.hideSoftInputFromWindow(view.windowToken, 0)
            binding.searchEditText.clearFocus()
            return true
        }
        return false
    }

    // 백스택으로 돌아가는 기능
    private fun removeFragment() {
        findNavController().navigate(R.id.action_memoSearchFragment_pop)
    }

    // 메모 아이템 클릭시 기능
    override fun memoItemClicked(position: Int) {
        val memo = filterMemo[position]
        memoViewModel.selectedMemo.value = memo
        memoViewModel.memoType.value = MEMO_TYPE.EDIT
        // navigation에서 (popUpTo로) MemoSearchFragment는 백스택에서 바로 제거되게 설정
        findNavController().navigate(R.id.action_memoSearchFragment_to_memoEditFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}