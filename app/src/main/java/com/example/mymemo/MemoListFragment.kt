package com.example.mymemo

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemo.databinding.FragmentMemoListBinding
import com.example.mymemo.recyclerview_memo_list.IListRecyclerVIew
import com.example.mymemo.recyclerview_memo_list.ListAdapter
import com.example.mymemo.room.MemoEntity
import com.example.mymemo.util.ConstData.KEY_MEMO_LABEL_LIST
import com.example.mymemo.util.ConstData.KEY_PREFS
import com.example.mymemo.util.MEMO_TYPE
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class MemoListFragment : Fragment(), IListRecyclerVIew {

    private var _binding: FragmentMemoListBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    // 리사이클러 뷰 아답터
    private val memoAdapter = ListAdapter(this)

    // OnBackPressedCallback (뒤로가기 기능) 객체 선언
    private lateinit var callback: OnBackPressedCallback

    // 양끝에 위치한 메모 지우고 복원시 자동으로 스크롤 되도록 하는 기능을 위한 변수
    private var topPosition = false
    private var bottomPosition = false

    private var filterList = mutableListOf<MemoEntity>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMemoListBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        callback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (memoViewModel.selectedLabel.value != null) {
                    memoViewModel.selectedLabel.value = null
                    findNavController().navigate(R.id.action_memoListFragment_pop)
                } else {
                    if (System.currentTimeMillis() - backWait >= 2000) {
                        backWait = System.currentTimeMillis()
                        Toast.makeText(context, "뒤로가기 버튼을 한번 더 누르면 종료됩니다",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        activity?.finish()
                    }
                }
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setRecyclerView()

        swipeAction()

        loadLabelList()

        memoViewModel.readAllData.observe(viewLifecycleOwner) { memoList ->
            if (memoViewModel.selectedLabel.value != null) {
                binding.collapsingToolbarLayout.title = memoViewModel.selectedLabel.value
                filterList.clear()
                memoList.forEach { memo ->
                    if (memo.label.contains(memoViewModel.selectedLabel.value)) {
                        filterList.add(memo)
                    }
                }
                memoAdapter.submitList(filterList)
            } else {
                filterList = memoList.toMutableList()
                binding.collapsingToolbarLayout.title = "MyMemo"
                memoAdapter.submitList(filterList)
            }

            // 메모가 새로 추가된 경우 메모 리스트 최상단으로 스크롤
            if (memoViewModel.memoType.value == MEMO_TYPE.NEW) setRecyclerView()
        }

        // 최하단 스크롤시 메모 추가버튼 숨기는 기능
        binding.memoRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                hideAddMemoBtn(recyclerView)
            }
        })

        binding.memoAddBtn.setOnClickListener {
            memoViewModel.memoType.value = MEMO_TYPE.NEW
            if (memoViewModel.selectedLabel.value != null) {
                memoViewModel.selectedMemo.value =
                    MemoEntity(null, label = mutableListOf(memoViewModel.selectedLabel.value!!))
            } else {
                memoViewModel.selectedMemo.value = MemoEntity(null)
            }
            moveEditFragment()
        }

        binding.memoSearchBtn.setOnClickListener {
            moveSearchFragment()
        }

        binding.drawerBtn.setOnClickListener {
            moveDrawerFragment()
        }
    }

    // 리스트의 마지막까지 스크롤하면 메모 추가버튼 사라지는 기능
    private fun hideAddMemoBtn(recyclerView: RecyclerView) {
        // 화면에 완전하게 보이는 마지막 아이템의 position
        val lastVisibleItemPosition = (recyclerView.layoutManager as LinearLayoutManager?)!!
            .findLastCompletelyVisibleItemPosition() + 1

        // 화면에 완전하게 보이는 처음 아이템의 position
        val firstVisibleItemPosition =
            (recyclerView.layoutManager as LinearLayoutManager?)!!
                .findFirstCompletelyVisibleItemPosition() + 1

        // 어댑터에 등록된 아이템의 총 개수
        val itemTotalCount = recyclerView.adapter!!.itemCount


        // lastVisibleItemPosition != itemTotalCount 조건으로 스크롤 가능한 상태인지 체크
        // firstVisibleItemPosition == 1 조건으로 맨 아래쪽 아이템까지 스크롤 되었는지 체크
        if (lastVisibleItemPosition != itemTotalCount && firstVisibleItemPosition == 1) {
            binding.memoAddBtn.visibility = View.INVISIBLE
        } else {
            binding.memoAddBtn.visibility = View.VISIBLE
        }
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

            // 삭제하는 메모가 최상단이나 최하단인지 확인
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val memo = filterList[viewHolder.bindingAdapterPosition]
                when (viewHolder.bindingAdapterPosition) {
                    0 -> bottomPosition = true
                    filterList.lastIndex -> topPosition = true
                }
                deleteMemo(memo)
                showSnackBar(memo)
            }
        }
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.memoRecyclerView)
    }

    // 리사이클러뷰 설정
    private fun setRecyclerView() {
        binding.memoRecyclerView.apply {
            adapter = memoAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
                    .apply { stackFromEnd = true }
            doOnPreDraw {
                // 메모를 새로 추가했으면 최상단으로 스크롤
                if (memoViewModel.memoType.value == MEMO_TYPE.NEW) {
                    binding.memoRecyclerView
                        .scrollToPosition(filterList.lastIndex)
                }
            }
        }
    }

    // 메모 지우는 기능
    private fun deleteMemo(memo: MemoEntity) {
        memoViewModel.memoType.value = MEMO_TYPE.NOTHING
        memoViewModel.deleteMemo(memo)
    }

    // 메모 삭제시 복구용 커스텀 스낵바
    private fun showSnackBar(memo: MemoEntity) {
        // 스낵바 인스턴스 생성
        val snackBar = Snackbar.make(binding.coordinatorLayout, "", 2000)

        // 커스텀 스낵바 xml을 inflate 시키는 작업
        val layoutId = R.layout.custom_snackbar
        val customSnackView = layoutInflater.inflate(layoutId, null)

        // 기본적으로 있는 스낵바의 백그라운드 색상을 투명하게 만듬
        snackBar.view.setBackgroundColor(Color.TRANSPARENT)

        // 새로운 커스텀 스낵바 레이아웃 생성
        val snackBarLayout = snackBar.view as SnackbarLayout

        // 스낵바 위치 설정
        snackBarLayout.setPadding(20, 0, 20, 20)

        // 스낵바 내용 설정
        val snackBarText = customSnackView.findViewById<TextView>(R.id.snackbar_text)
        val text = "\"${
            when (memo.title) {
                "" -> "제목 없음"
                else -> {
                    if (memo.title.length > 8) {
                        memo.title.slice(0..7) + "..."
                    } else {
                        memo.title
                    }
                }
            }
        }\" 메모를 삭제하였습니다"
        snackBarText.text = text

        // 스낵바 액션버튼 기능
        val actionBtn = customSnackView.findViewById<TextView>(R.id.snackbar_action_btn)
        actionBtn.setOnClickListener {
            memoViewModel.addMemo(memo)

            // 선택된 메모가 최상단이나 최하단 메모였을 경우 자동 스크롤
            when {
                topPosition -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.memoRecyclerView
                            .scrollToPosition(filterList.lastIndex)
                    }, 35)
                    topPosition = false
                    bottomPosition = false
                }
                bottomPosition -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.memoRecyclerView
                            .scrollToPosition(0)
                    }, 35)
                    topPosition = false
                    bottomPosition = false
                }
            }

            snackBar.dismiss()
        }

        // 커스텀 스낵바 레이아웃과 커스텀 스낵바 xml 연결 (연결 안하면 아무것도 안뜸)
        snackBarLayout.addView(customSnackView, 0)
        snackBar.show()
    }

    // 메모 클릭시
    override fun memoItemClicked(position: Int) {
        val memo = filterList[position]
        memoViewModel.selectedMemo.value = memo
        memoViewModel.memoType.value = MEMO_TYPE.EDIT
        moveEditFragment()
    }


    // 메모 길게 클릭시 삭제 다이얼로그 보여주는 기능
    override fun memoItemLongClicked(position: Int) {
        val memo = filterList[position]

        // 삭제 다이얼로그 보여주기
        AlertDialog.Builder(context)
            .setTitle("MyMemo")
            .setMessage(
                if (filterList[position].title.isNotEmpty()) {
                    "\"${filterList[position].title}\""
                } else {
                    "\"제목 없음\""
                } + " 메모를 삭제하시겠습니까?"
            )
            .setPositiveButton("확인") { _, _ ->
                // 삭제하는 메모가 최상단이나 최하단인지 확인
                when (position) {
                    0 -> bottomPosition = true
                    filterList.lastIndex -> topPosition = true
                }
                deleteMemo(memo)
                showSnackBar(memo)
            }
            .setNegativeButton("취소") { _, _ -> }
            .create()
            .show()
    }


    // 메모 수정 화면 이동 기능
    private fun moveEditFragment() {
        findNavController().navigate(R.id.action_memoListFragment_to_memoEditFragment)
    }

    // 메모 검색 화면 이동 기능
    private fun moveSearchFragment() {
        findNavController().navigate(R.id.action_memoListFragment_to_memoSearchFragment)
    }

    // Drawer(메뉴) 화면 이동 기능
    private fun moveDrawerFragment() {
        findNavController().navigate(R.id.action_memoListFragment_to_drawerFragment)
    }


    // LabelList 불러오기
    private fun loadLabelList() {
        val sharedPreferences =
            requireActivity().getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)

        // Json 파일 변환을 위한 Gson 객체
        val gson = GsonBuilder().create()

        val typeLabelList: TypeToken<MutableList<String>> =
            object : TypeToken<MutableList<String>>() {}

        if (sharedPreferences.contains(KEY_MEMO_LABEL_LIST)) {
            val jsonLabelList = sharedPreferences.getString(KEY_MEMO_LABEL_LIST, "")
            memoViewModel.labelList.value = gson.fromJson(jsonLabelList, typeLabelList.type)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        callback.remove()
    }
}