package com.example.mymemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mymemo.databinding.FragmentSettingBinding
import com.example.mymemo.room.MemoEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    private var isImport = false

    // OnBackPressedCallback (뒤로가기 기능) 객체 선언
    private lateinit var callback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        callback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            override fun handleOnBackPressed() {
                if (isImport) {
                    memoViewModel.addMemo(MemoEntity(-1L))
                    memoViewModel.deleteMemo(MemoEntity(-1L))
                }
                removeFragment()
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.memoExport.setOnClickListener {
            exportDatabase()
        }

        binding.memoImport.setOnClickListener {
            importDatabase()
        }

        binding.backButton.setOnClickListener {
            if (isImport) {
                memoViewModel.addMemo(MemoEntity(-1L))
                memoViewModel.deleteMemo(MemoEntity(-1L))
            }
            removeFragment()
        }
    }

    // 메모 데이터 파일 내보내기 기능
    private fun exportDatabase() {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()

            if (sd!!.canWrite()) {
                Log.d("로그", "SettingFragment - exportDatabase() 호출됨")
                val currentDB = File(data, "/data/com.example.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.example.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.example.mymemo/databases/memo.db-wal")
                val exportDB = File(sd, "/Download/memo.db")
                val exportSHM = File(sd, "/Download/memo.db-shm")
                val exportWAL = File(sd, "/Download/memo.db-wal")

                Log.d("로그", "SettingFragment - exportDatabase2() 호출됨")
                val dbInputStream = FileInputStream(currentDB).channel
                val dbOutputStream = FileOutputStream(exportDB).channel
                dbOutputStream.transferFrom(dbInputStream, 0, dbInputStream.size())
                dbInputStream.close()
                dbOutputStream.close()

                val shmInputStream = FileInputStream(currentSHM).channel
                val shmOutputStream = FileOutputStream(exportSHM).channel
                shmOutputStream.transferFrom(shmInputStream, 0, shmInputStream.size())
                shmInputStream.close()
                shmOutputStream.close()

                val walInputStream = FileInputStream(currentWAl).channel
                val walOutputStream = FileOutputStream(exportWAL).channel
                walOutputStream.transferFrom(walInputStream, 0, walInputStream.size())
                walInputStream.close()
                walOutputStream.close()

                Toast.makeText(requireContext(), "내보내기 성공", Toast.LENGTH_SHORT).show()
            } else {
                requestPermission()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "내보내기 실패", Toast.LENGTH_SHORT).show()
        }

    }

    // 메모 데이터 파일 가져오기 기능
    private fun importDatabase() {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()

            if (sd!!.canWrite()) {
                val currentDB = File(data, "/data/com.example.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.example.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.example.mymemo/databases/memo.db-wal")
                val importDB = File(sd, "/Download/memo.db")
                val importSHM = File(sd, "/Download/memo.db-shm")
                val importWAL = File(sd, "/Download/memo.db-wal")

                val dataStream = { original: File, overwrite: File ->
                    val inputStream = FileInputStream(original).channel
                    val outputStream = FileOutputStream(overwrite).channel
                    outputStream.transferFrom(inputStream, 0, inputStream.size())
                    inputStream.close()
                    outputStream.close()
                }

                dataStream(importDB, currentDB)
                dataStream(importSHM, currentSHM)
                dataStream(importWAL, currentWAl)

                isImport = true
                memoViewModel.selectedLabel.value = null

                Toast.makeText(requireContext(), "가져오기 성공", Toast.LENGTH_SHORT).show()
            } else {
                requestPermission()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "가져오기 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 외부 저장소 쓰기권한 요청 기능
    // 안드로이드 API 30 이상과 미만에 따른 권한 요청 방법 구분
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !isPermissionGranted()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            val uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            requireContext().startActivity(intent)
        } else {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                val permission = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                ActivityCompat.requestPermissions(requireActivity(), permission, 99)
            }
        }
    }

    // MANAGE_EXTERNAL_STORAGE 권한 확인
    private fun isPermissionGranted(): Boolean {
        var granted = false
        try {
            granted = Environment.isExternalStorageManager()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return granted
    }

    private fun removeFragment() {
        findNavController().navigate(R.id.action_settingFragment_pop)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        callback.remove()
    }
}