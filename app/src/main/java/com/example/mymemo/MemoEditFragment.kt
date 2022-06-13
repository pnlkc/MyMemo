package com.example.mymemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymemo.databinding.FragmentMemoEditBinding
import com.example.mymemo.recyclerview_edit_memo_label.ILabel
import com.example.mymemo.recyclerview_edit_memo_label.LabelAdapter
import com.example.mymemo.room.MemoEntity
import com.example.mymemo.util.MEMO_TYPE
import com.example.mymemo.util.MEMO_TYPE.NEW
import java.text.SimpleDateFormat

class MemoEditFragment : Fragment(), ILabel {

    private var _binding: FragmentMemoEditBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    // OnBackPressedCallback(뒤로가기 기능) 객체 선언
    private lateinit var callback: OnBackPressedCallback

    private val labelAdapter = LabelAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMemoEditBinding.inflate(inflater,
            container,
            false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        callback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            override fun handleOnBackPressed() {
                memoTypeActon()
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 리사이클러뷰 설정
        binding.labelRecyclerView.adapter = labelAdapter
        binding.labelRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // 메모타입에 따른 뷰 설정
        loadMemo(memoViewModel.memoType.value!!)

        // 라벨 리사이클러뷰 업데이트
        memoViewModel.selectedMemo.observe(viewLifecycleOwner) { memo ->
            labelAdapter.submitList(memo.label)
        }

        // 뒤로가기 버튼을 눌렀을 때 기능
        binding.backButton.setOnClickListener {
            memoTypeActon()
        }

        // 현재 시간 추가 버튼
        binding.timeBtn.setOnClickListener {
            val timeData = SimpleDateFormat("a hh:mm").format(System.currentTimeMillis())
            when {
                binding.memoEditText.hasFocus() -> {
                    binding.memoEditText.text!!.insert(binding.memoEditText.selectionStart,
                        timeData)
                }
                binding.memoEditTitle.hasFocus() -> {
                    binding.memoEditTitle.text!!.insert(binding.memoEditTitle.selectionStart,
                        timeData)
                }
                else -> {
                    binding.memoEditText.append(timeData)
                }
            }
        }

        // 라벨 추가 버튼
        binding.labelBtn.setOnClickListener {
            moveSelectLabelFragment()
        }
    }


    // 메모가 기존의 메모를 수정하는 것이면 데이터 불러오는 기능
    @SuppressLint("SimpleDateFormat")
    private fun loadMemo(it: MEMO_TYPE) {
        if (it == MEMO_TYPE.EDIT) {
            binding.memoEditTitle.setText(memoViewModel.selectedMemo.value!!.title)
            binding.memoEditText.setText(memoViewModel.selectedMemo.value!!.memo)

            // 메모 시간 기록용 변수
            val dateParsing =
                SimpleDateFormat("yy-MM-dd a hh:mm").parse(memoViewModel.selectedMemo.value!!.date)
            val compareDate = SimpleDateFormat("yyMMdd").format(dateParsing!!)
            val currentDate = SimpleDateFormat("yyMMdd").format(System.currentTimeMillis())

            val memoDate: String = if (compareDate == currentDate) {
                SimpleDateFormat("a hh:mm").format(dateParsing)
            } else {
                SimpleDateFormat("yy년 MM월 dd일").format(dateParsing)
            }

            binding.dateTextView.text = memoDate
        }
        // 새 메모이면 새 메모 생성 시간을 하단에 표시해주는 기능
        else if (it == NEW) {
            binding.dateTextView.text =
                SimpleDateFormat("a hh:mm").format(System.currentTimeMillis())
        }
    }

    //
    private fun moveSelectLabelFragment() {
        findNavController().navigate(R.id.action_memoEditFragment_to_selectLabelFragment)
    }

    // 백스택으로 돌아가는 기능
    private fun removeFragment() {
        // requireActivity().supportFragmentManager.popBackStack()과 동일한 기능으로 추정
        findNavController().navigate(R.id.action_memoEditFragment_pop)
    }

    // 메모타입(수정, 새로만들기)에 따른 액션 기능
    private fun memoTypeActon() {
        when (memoViewModel.memoType.value) {
            NEW -> insertMemo()
            else -> editMemo()
        }
        removeFragment()
    }

    // 메모 추가 기능
    @SuppressLint("SimpleDateFormat")
    fun insertMemo() {
        val currentDateSource = System.currentTimeMillis()
        val memoDate = SimpleDateFormat("yy-MM-dd a hh:mm").format(currentDateSource)

        val memo = MemoEntity(null,
            binding.memoEditTitle.text.toString(),
            binding.memoEditText.text.toString(),
            memoDate,
            memoViewModel.selectedMemo.value!!.label
        )

        if (memo.title.isNotBlank()) {
            if (memo.memo.isNotBlank()) {
                memoViewModel.addMemo(memo)
            } else {
                val emptyMemo = MemoEntity(null,
                    memo.title,
                    "",
                    memoDate,
                    memoViewModel.selectedMemo.value!!.label)
                memoViewModel.addMemo(emptyMemo)
                Toast.makeText(context, "내용이 입력되지 않았습니다", Toast.LENGTH_SHORT).show()
            }
        } else if (memo.memo.isBlank()) {
            Toast.makeText(context, "빈 메모입니다", Toast.LENGTH_SHORT).show()
        } else {
            val emptyTitleMemo =
                MemoEntity(null, "", memo.memo, memoDate, memoViewModel.selectedMemo.value!!.label)
            memoViewModel.addMemo(emptyTitleMemo)
            Toast.makeText(context, "제목이 입력되지 않았습니다", Toast.LENGTH_SHORT).show()
        }
    }


    // 메모 수정(업데이트) 기능
    @SuppressLint("SimpleDateFormat")
    fun editMemo() {
        val currentDateSource = System.currentTimeMillis()
        val memoDate = SimpleDateFormat("yy-MM-dd a hh:mm").format(currentDateSource)

        // 메모 수정이 이루어지지 않은 상황 처리
        val memo: MemoEntity =
            if (memoViewModel.selectedMemo.value!!.title == binding.memoEditTitle.text.toString()
                && memoViewModel.selectedMemo.value!!.memo == binding.memoEditText.text.toString()
            ) {
                MemoEntity(memoViewModel.selectedMemo.value!!.id,
                    binding.memoEditTitle.text.toString(),
                    binding.memoEditText.text.toString(),
                    memoViewModel.selectedMemo.value!!.date,
                    memoViewModel.selectedMemo.value!!.label)
            } else {
                MemoEntity(memoViewModel.selectedMemo.value!!.id,
                    binding.memoEditTitle.text.toString(),
                    binding.memoEditText.text.toString(),
                    memoDate,
                    memoViewModel.selectedMemo.value!!.label)
            }

        if (memo.title.isNotBlank()) {
            if (memo.memo.isNotBlank()) {
                memoViewModel.editMemo(memo)
            } else {
                val emptyMemo =
                    MemoEntity(memoViewModel.selectedMemo.value!!.id,
                        memo.title,
                        "",
                        memo.date,
                        memoViewModel.selectedMemo.value!!.label)
                memoViewModel.editMemo(emptyMemo)
                Toast.makeText(context, "내용이 입력되지 않았습니다", Toast.LENGTH_SHORT).show()
            }
        } else if (memo.memo.isBlank()) {
            Toast.makeText(context, "빈 메모입니다", Toast.LENGTH_SHORT).show()
            memoViewModel.deleteMemo(memo)
        } else {
            val emptyTitleMemo =
                MemoEntity(memoViewModel.selectedMemo.value!!.id,
                    "",
                    memo.memo,
                    memo.date,
                    memoViewModel.selectedMemo.value!!.label)
            memoViewModel.editMemo(emptyTitleMemo)
            Toast.makeText(context, "제목이 입력되지 않았습니다", Toast.LENGTH_SHORT).show()
        }
    }

    override fun labelClicked() {
        moveSelectLabelFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        callback.remove()
    }
}