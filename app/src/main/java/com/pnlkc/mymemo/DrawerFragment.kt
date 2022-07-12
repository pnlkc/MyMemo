package com.pnlkc.mymemo

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pnlkc.mymemo.databinding.FragmentDrawerBinding
import com.pnlkc.mymemo.recyclerview_drawer.DrawerAdapter
import com.pnlkc.mymemo.recyclerview_drawer.IDrawerRecyclerView


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

        binding.settingBtn.setOnClickListener {
            moveSettingFragment()
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

    private fun moveSettingFragment() {
        findNavController().navigate(R.id.action_drawerFragment_to_settingFragment)
    }

    private fun removeFragment() {
        findNavController().navigate(R.id.action_drawerFragment_pop)
    }

    override fun memoItemClicked(position: Int) {
        memoViewModel.selectedLabel.value = memoViewModel.labelList.value!![position]
        moveMemoListFragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}