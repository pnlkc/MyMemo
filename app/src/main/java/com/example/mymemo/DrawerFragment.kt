package com.example.mymemo

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymemo.databinding.FragmentDrawerBinding
import com.example.mymemo.recyclerview_drawer.DrawerAdapter
import com.example.mymemo.recyclerview_drawer.IDrawerRecyclerView
import com.example.mymemo.util.ConstData
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class DrawerFragment : DialogFragment(), IDrawerRecyclerView {

    private var _binding: FragmentDrawerBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    private val drawerAdapter = DrawerAdapter(this)

    // 풀스크린 및 뒤로가기 기능 설정
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.fullscreen_dialog)
        isCancelable = true

    }

    // 들어오고 나갈때 애니메이션 설정
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window!!.attributes.windowAnimations = R.style.dialog_animation
        dialog.window!!.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDrawerBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()

        memoViewModel.labelList.observe(this) {
            updateRecyclerViewList()
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

        binding.drawerEmptySpace.setOnClickListener {
            removeFragment()
        }

        binding.allMemoBtn.setOnClickListener {
            if (memoViewModel.selectedLabel.value == null) {
                removeFragment()
            } else {
                memoViewModel.selectedLabel.value = null
                moveMemoListFragment()
            }
        }
    }

    private fun setRecyclerView() {
        binding.drawerRecyclerView.adapter = drawerAdapter
        binding.drawerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        updateRecyclerViewList()
    }

    private fun updateRecyclerViewList() {
        if (memoViewModel.labelList.value != null) {
            if (memoViewModel.selectedLabel.value != null) {
                binding.allMemoBtn.setBackgroundResource(R.drawable.drawer_unselected_bg)
            } else {
                binding.allMemoBtn.setBackgroundResource(R.drawable.drawer_selected_bg)
            }
            drawerAdapter.updateList(memoViewModel.labelList.value!!,
                memoViewModel.selectedLabel.value)
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

    private fun moveMemoListFragment() {
        findNavController().navigate(R.id.action_drawerFragment_to_memoListFragment)
    }

    private fun removeFragment() {
        findNavController().navigate(R.id.action_drawerFragment_pop)
    }

    override fun memoItemClicked(position: Int) {
        memoViewModel.selectedLabel.value = memoViewModel.labelList.value!![position]
        moveMemoListFragment()
    }

    override fun memoItemLongClicked(position: Int) {
        val label = memoViewModel.labelList.value!![position]

        // 삭제 다이얼로그 보여주기
        AlertDialog.Builder(context)
            .setTitle("라벨을 삭제하시겠습니까?")
            .setMessage("\"${label}\"" +
                    " 라벨을 모든 메모에서 삭제합니다.\n메모는 삭제되지 않습니다.")
            .setPositiveButton("확인") { _, _ ->
                memoViewModel.labelList.value!!.remove(label)

                memoViewModel.readAllData.value!!.forEach { memo ->
                    if (memo.label.contains(label)) {
                        memo.label.remove(label)
                        memoViewModel.editMemo(memo)
                    }
                }
                saveLabelList()
                setRecyclerView()
            }
            .setNegativeButton("취소") { _, _ -> }
            .create()
            .show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}