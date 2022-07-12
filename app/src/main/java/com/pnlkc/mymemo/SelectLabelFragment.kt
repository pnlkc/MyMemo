package com.pnlkc.mymemo

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pnlkc.mymemo.databinding.FragmentSelectLabelBinding
import com.pnlkc.mymemo.recyclerview_select_label.ISelectLabel
import com.pnlkc.mymemo.recyclerview_select_label.SelectLabelAdapter
import com.pnlkc.mymemo.room.MemoEntity

class SelectLabelFragment : Fragment(), ISelectLabel {

    private var _binding: FragmentSelectLabelBinding? = null
    private val binding: FragmentSelectLabelBinding
        get() = _binding!!

    private lateinit var selectLabelAdapter: SelectLabelAdapter

    private val memoViewModel: MemoViewModel by activityViewModels()

    // OnBackPressedCallback (뒤로가기 기능) 객체 선언
    private lateinit var callback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSelectLabelBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        callback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            override fun handleOnBackPressed() {
                if (binding.addLabelConstraintLayout.visibility == View.VISIBLE) {
                    binding.addLabelConstraintLayout.visibility = View.GONE
                } else {
                    removeFragment()
                }
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectLabelAdapter =
            SelectLabelAdapter(memoViewModel.selectedMemo.value!!.label, this)

        binding.selectLabelRecyclerView.adapter = selectLabelAdapter
        binding.selectLabelRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        selectLabelAdapter.setData(memoViewModel.labelList.value!!)

        binding.backButton.setOnClickListener {
            removeFragment()
        }

        binding.addLabelBtn.setOnClickListener {
            binding.addLabelConstraintLayout.visibility = View.VISIBLE
            binding.addLabelEditText.requestFocus()
            val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            inputManager.showSoftInput(binding.addLabelEditText, InputMethodManager.SHOW_IMPLICIT)
            Handler(Looper.getMainLooper()).postDelayed({
                binding.nestedScrollView.smoothScrollTo(0, binding.addLabelBtn.bottom)
            }, 35)
        }

        binding.addLabelEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.nestedScrollView.smoothScrollTo(0, binding.addLabelBtn.bottom)
                }, 100)
            }
        }

        binding.addLabelEditText.setOnKeyListener { _, keyCode, _ ->
            enterKeyboard(keyCode)
        }

        binding.addLabelConfirmBtn.setOnClickListener {
            addLabel()
        }
    }

    // 엔터누르면 실행되는 기능
    private fun enterKeyboard(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> {
                addLabel()
                return true
            }
        }
        return false
    }

    // 라벨을 추가하는 기능
    private fun addLabel() {
        val label = binding.addLabelEditText.text.toString()
        if (label.isNotBlank() && !memoViewModel.labelList.value!!.contains(label)) {
            memoViewModel.selectedMemo.value!!.label.add(label)
            memoViewModel.labelList.value!!.add(label)
            memoViewModel.labelList.value = memoViewModel.labelList.value!!.sorted().toMutableList()
            selectLabelAdapter.setData(memoViewModel.labelList.value!!)
            saveLabelList(memoViewModel.labelList.value!!)
            Toast.makeText(requireContext(),
                "\"$label\" 라벨이 추가되었습니다", Toast.LENGTH_SHORT).show()
        } else if (label.isBlank()) {
            Toast.makeText(requireContext(),
                "라벨 이름은 비어있을 수 없습니다", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(),
                "같은 이름의 라벨이 이미 있습니다", Toast.LENGTH_SHORT).show()
        }

        controlAddLabelLayout()
    }

    private fun controlAddLabelLayout() {
        // 키보드 내리는 기능
        val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        inputManager.hideSoftInputFromWindow(view?.windowToken, 0)

        // 포커스 사라지게 하는 기능
        binding.addLabelEditText.clearFocus()
        binding.addLabelEditText.text!!.clear()

        // 라벨 추가용 레이아웃 사라지게하는 코드
        binding.addLabelConstraintLayout.visibility = View.GONE
    }

    // LabelList 저장
    private fun saveLabelList(labelList: MutableList<String>) {
        val memo = memoViewModel.readAllData.value!!.first()
        memoViewModel.editMemo(
            MemoEntity(
                memo.id,
                memo.title,
                memo.memo,
                memo.date,
                labelList
            )
        )
    }

    private fun removeFragment() {
        findNavController().navigate(R.id.action_selectLabelFragment_pop)
    }

    override fun labelCheckBoxClicked(position: Int) {
        val currentLabel = memoViewModel.labelList.value!![position]

        if (!memoViewModel.selectedMemo.value!!.label.contains(currentLabel)) {
            memoViewModel.selectedMemo.value!!.label.add(currentLabel)
            memoViewModel.selectedMemo.value!!.label =
                memoViewModel.selectedMemo.value!!.label.sorted().toMutableList()
        } else {
            memoViewModel.selectedMemo.value!!.label.remove(currentLabel)
            memoViewModel.selectedMemo.value!!.label =
                memoViewModel.selectedMemo.value!!.label.sorted().toMutableList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        callback.remove()
    }
}