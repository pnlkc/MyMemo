package com.pnlkc.mymemo

import android.content.Context
import android.os.Bundle
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
import com.pnlkc.mymemo.databinding.FragmentMemoSearchBinding
import com.pnlkc.mymemo.recyclerview_memo_search.ISearchRecyclerView
import com.pnlkc.mymemo.recyclerview_memo_search.SearchAdapter
import com.pnlkc.mymemo.room.MemoEntity
import com.pnlkc.mymemo.util.MEMO_TYPE
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MemoSearchFragment : Fragment(), ISearchRecyclerView {

    private var _binding: FragmentMemoSearchBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    private val searchAdapter = SearchAdapter(this)

    private var filterMemo: MutableList<MemoEntity> = mutableListOf()

    private val myJob = Job()
    private val myContext get() = Dispatchers.Main + myJob

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

        CoroutineScope(myContext).launch {
            launch {
                binding.mainResultLinearLayout.visibility = View.INVISIBLE
                delay(200)
                showKeyboardAndRequestFocus()
                delay(100)
                binding.mainResultLinearLayout.visibility = View.VISIBLE
            }

            // ?????? ????????? ????????? ????????? 0.35??? ?????? ????????? ?????? ??????
            launch {
                val editTextFlow = binding.searchEditText.textChangesToFlow()
                editTextFlow
                    .debounce(350)
                    .onEach { text ->
                        searchMemo(text!!, binding.searchEditTextClearBtn, binding.searchEditText)
                    }
                    .launchIn(this)
            }
        }

        binding.backButton.setOnClickListener {
            removeFragment()
        }

        binding.searchEditText.setOnKeyListener { myView, keyCode, _ ->
            hideKeyboard(myView, keyCode)
        }
    }

    // ?????????????????? ??????
    private fun setRecyclerView() {
        binding.searchRecyclerView.apply {
            adapter = searchAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
                    .apply { stackFromEnd = true }
        }
    }

    // EditText ??? ??????????????? ??? ?????? ??????
    private fun searchMemo(text: CharSequence, btn: ImageView, editText: EditText) {
        // ???????????? ????????? ????????? ?????? ????????????
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

        // ??????????????? ??? ???????????? ?????? ??? visibility ??????
        if (text.isNotEmpty()) {
            // ???????????? ???????????? ??????????????? ?????????
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

    // ????????? ????????? ????????? ?????? ??????
    private fun showKeyboardAndRequestFocus() {
        binding.searchEditText.requestFocus()
        val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        inputManager.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    // ??????????????? ????????? ???????????? ????????? ???????????? ??????
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

    // ??????????????? ???????????? ??????
    private fun removeFragment() {
        findNavController().navigate(R.id.action_memoSearchFragment_pop)
    }

    // ?????? ????????? ????????? ??????
    override fun memoItemClicked(position: Int) {
        val memo = filterMemo[position]
        memoViewModel.selectedMemo.value = memo
        memoViewModel.memoType.value = MEMO_TYPE.EDIT
        // navigation?????? (popUpTo???) MemoSearchFragment??? ??????????????? ?????? ???????????? ??????
        findNavController().navigate(R.id.action_memoSearchFragment_to_memoEditFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        myContext.cancel()
    }
}