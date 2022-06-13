package com.example.mymemo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymemo.databinding.FragmentSelectLabelBinding
import com.example.mymemo.recyclerview_select_label.ISelectLabel
import com.example.mymemo.recyclerview_select_label.SelectLabelAdapter
import com.example.mymemo.util.ConstData
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SelectLabelFragment : Fragment(), ISelectLabel {

    private var _binding: FragmentSelectLabelBinding? = null
    private val binding: FragmentSelectLabelBinding
        get() = _binding!!

    private lateinit var selectLabelAdapter: SelectLabelAdapter

    private val memoViewModel: MemoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSelectLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectLabelAdapter = SelectLabelAdapter(memoViewModel.selectedMemo.value!!.label, this)

        binding.searchRecyclerView.adapter = selectLabelAdapter
        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        selectLabelAdapter.submitList(memoViewModel.labelList.value!!)

        binding.backButton.setOnClickListener {
            removeFragment()
        }

        binding.addLabelBtn.setOnClickListener {
            binding.addLabelConstraintLayout.visibility = View.VISIBLE
            binding.addLabelEditText.requestFocus()
            val inputManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            inputManager.showSoftInput(binding.addLabelEditText, InputMethodManager.SHOW_IMPLICIT)
            CoroutineScope(Dispatchers.Main).launch {
                delay(35)
                binding.nestedScrollView.scrollTo(0, binding.addLabelBtn.bottom)
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
            memoViewModel.labelList.value!!.add(label)
            memoViewModel.labelList.value = memoViewModel.labelList.value!!.sorted().toMutableList()
            selectLabelAdapter.submitList(memoViewModel.labelList.value!!)
            saveLabelList()
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
    private fun saveLabelList() {
        val sharedPreferences =
            requireActivity().getSharedPreferences(ConstData.KEY_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Json 파일 변환을 위한 Gson 객체
        val gson = GsonBuilder().create()

        val typeLabelList: TypeToken<MutableList<String>> =
            object : TypeToken<MutableList<String>>() {}

        val jsonLabelList = gson.toJson(memoViewModel.labelList.value!!, typeLabelList.type)
        editor.putString(ConstData.KEY_MEMO_LABEL_LIST, jsonLabelList)
        editor.apply()
    }

    private fun removeFragment() {
        findNavController().navigate(R.id.action_selectLabelFragment_pop)
    }

    override fun labelCheckBoxClicked(position: Int) {
        val currentLabel = memoViewModel.labelList.value!![position]
        if (memoViewModel.selectedMemo.value!!.label.contains(currentLabel)) {
            memoViewModel.selectedMemo.value!!.label.remove(currentLabel)
        } else {
            memoViewModel.selectedMemo.value!!.label.add(currentLabel)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}