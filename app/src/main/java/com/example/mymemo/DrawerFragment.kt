package com.example.mymemo

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymemo.databinding.FragmentDrawerBinding
import com.example.mymemo.recyclerview_drawer.DrawerAdapter
import com.example.mymemo.recyclerview_drawer.IDrawerRecyclerView
import com.example.mymemo.room.MemoEntity
import com.example.mymemo.util.ConstData
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken


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
            moveEditLabelFragment()
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

    private fun moveMemoListFragment() {
        findNavController().navigate(R.id.action_drawerFragment_to_memoListFragment)
    }

    private fun moveEditLabelFragment() {
        findNavController().navigate(R.id.action_drawerFragment_to_editLabelFragment)
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

                // 현재 선택된 라벨을 삭제하는 경우
                if (label == memoViewModel.selectedLabel.value) {
                    memoViewModel.selectedLabel.value = null
                    // MemoListFragment 리사이클러뷰 업데이트용 덤프 메모 추가 후 삭제
                    memoViewModel.addMemo(MemoEntity(-1L))
                    memoViewModel.deleteMemo(MemoEntity(-1L))
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