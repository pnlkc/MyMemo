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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pnlkc.mymemo.databinding.FragmentEditLabelBinding
import com.pnlkc.mymemo.recyclerview_edit_label.EditLabelAdapter
import com.pnlkc.mymemo.recyclerview_edit_label.IEditLabel
import com.pnlkc.mymemo.room.MemoEntity
import com.pnlkc.mymemo.util.DialogCreator

class EditLabelFragment : Fragment(), IEditLabel {

    private var _binding: FragmentEditLabelBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    private lateinit var editLabelAdapter: EditLabelAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditLabelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()

        swipeAction()

        binding.addLabelEditText.setOnKeyListener { _, keyCode, _ ->
            enterKeyboard(keyCode)
        }

        binding.addLabelConfirmBtn.setOnClickListener {
            addLabel()
        }

        binding.backButton.setOnClickListener {
            removeFragment()
        }
    }

    // 리사이클러뷰 설정
    private fun setRecyclerView() {
        editLabelAdapter = EditLabelAdapter(this)
        binding.editLabelRecyclerView.adapter = editLabelAdapter
        binding.editLabelRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        editLabelAdapter.setData(memoViewModel.labelList.value!!)
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
            editLabelAdapter.setData(memoViewModel.labelList.value!!)
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
    }

    // 리사이클러뷰 아이템 스와이프 기능
    private fun swipeAction() {
        val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean = false

            // 삭제하는 라벨이 최상단이나 최하단인지 확인
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                showDeleteDialog(memoViewModel.labelList.value!![viewHolder.bindingAdapterPosition])
            }
        }
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.editLabelRecyclerView)
    }

    // 라벨 롱클릭 이벤트
    override fun labelLongClicked(position: Int) {
        val label = memoViewModel.labelList.value!![position]
        showDeleteDialog(label)
    }

    // 라벨 삭제 다이얼로그 보여주는 기능
    private fun showDeleteDialog(label: String) {
        DialogCreator().showDialog(
            requireContext(),
            "라벨을 삭제하시겠습니까?",
            "\"${label}\"" +
                    " 라벨을 모든 메모에서 삭제합니다.\n메모는 삭제되지 않습니다.",
            {
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
                    // MemoListFragment 리사이클러뷰 리프레시 (딜레이가 없으면 에러 발생)
                    memoViewModel.addMemo(MemoEntity(-1))
                    Handler(Looper.getMainLooper()).postDelayed({
                        memoViewModel.deleteMemo(MemoEntity(-1))
                    }, 35)
                }

                saveLabelList(memoViewModel.labelList.value!!)
                setRecyclerView()
            },
            { setRecyclerView() }
        )
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
                memo.language,
                labelList
            )
        )
    }

    private fun removeFragment() {
        findNavController().navigate(R.id.action_editLabelFragment_pop)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}