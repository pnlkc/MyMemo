package com.example.mymemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.mymemo.databinding.FragmentSettingBinding
import com.example.mymemo.room.MemoEntity
import com.example.mymemo.util.App
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.*
import java.io.*

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    // OnBackPressedCallback (뒤로가기 기능) 객체 선언
    private lateinit var callback: OnBackPressedCallback

    private val uploadResultList = mutableListOf<Boolean>()
    private val downloadResultList = mutableListOf<Boolean>()

    var downloadDB: File? = null
    var downloadSHM: File? = null
    var downloadWAL: File? = null

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
                removeFragment()
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 메모 클라우드 백업 연동 버튼 초기 설정
        when (App.checkAuth()) {
            // 로그인 되어 있으면
            true -> {
                binding.loginGoogleIdTitle.text = getText(R.string.logout_google_id)
                binding.loginGoogleIdDescription.text =
                    getText(R.string.logout_google_id_description)
            }
            // 로그인 되어있지 않으면
            false -> {
                binding.loginGoogleIdTitle.text = getText(R.string.login_google_id)
                binding.loginGoogleIdDescription.text =
                    getText(R.string.login_google_id_description)
            }
        }

        binding.backButton.setOnClickListener {
            removeFragment()
        }

        // 메모 내보내기(로컬) 기능
        val exportLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    val resultData = it.data?.data?.path.toString()
                    val index = resultData.indexOf(":")
                    val path = resultData.removeRange(0..index)
                    exportDatabase(path)
                }
            }

        binding.memoExport.setOnClickListener {
            requestPermission()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            exportLauncher.launch(intent)
        }


        // 메모 가져오기(로컬) 기능
        val importLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    val resultData = it.data?.data?.path.toString()
                    val index = resultData.indexOf(":")
                    val path = resultData.removeRange(0..index)
                    importDatabase(path)
                }
            }

        binding.memoImport.setOnClickListener {
            requestPermission()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            importLauncher.launch(intent)
        }


        // 파이어베이스 구글 로그인 기능
        val gsoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // 구글 로그인 결과 처리
                if (it.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                    try {
                        val account = task.result
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        App.auth.signInWithCredential(credential)
                            .addOnCompleteListener(requireActivity()) { signInTask ->
                                if (signInTask.isSuccessful) {
                                    // 구글 로그인 성공
                                    App.email = account.email

                                    Toast.makeText(requireContext(),
                                        "연동에 성공하였습니다",
                                        Toast.LENGTH_SHORT)
                                        .show()

                                    binding.loginGoogleIdTitle.text =
                                        getText(R.string.logout_google_id)
                                    binding.loginGoogleIdDescription.text =
                                        getText(R.string.logout_google_id_description)
                                } else {
                                    // 구글 로그인 실패
                                    Toast.makeText(requireContext(),
                                        "연동에 실패하였습니다",
                                        Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                    } catch (e: ApiException) {
                        Toast.makeText(requireContext(), "연동에 실패하였습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        binding.loginGoogleId.setOnClickListener {
            when (App.checkAuth()) {
                // 로그인 되어 있으면
                true -> {
                    // 파이어베이스 로그아아웃
                    App.auth.signOut()

                    // 구글 계정 로그아웃
                    // 이 코드가 없으면 재로그인시 초기 로그인시 나오는 인텐트 팝업이 뜨지 않음
                    GoogleSignIn.getClient(
                        requireContext(),
                        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                    ).signOut()

                    Toast.makeText(requireContext(),
                        "연동이 해제 되었습니다", Toast.LENGTH_SHORT).show()

                    binding.loginGoogleIdTitle.text = getText(R.string.login_google_id)
                    binding.loginGoogleIdDescription.text =
                        getText(R.string.login_google_id_description)

                }
                // 로그인 되어있지 않으면
                false -> {
                    val gso = GoogleSignInOptions
                        .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        //R.string.default_web_client_id 에러시 project 수준의 classpath ...google-services:4.3.8 로 변경
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val signInIntent = GoogleSignIn.getClient(requireContext(), gso).signInIntent
                    gsoLauncher.launch(signInIntent)

                    binding.loginGoogleIdTitle.text = getText(R.string.logout_google_id)
                    binding.loginGoogleIdDescription.text =
                        getText(R.string.logout_google_id_description)
                }
            }
        }

        // 메모 백업(클라우드) 기능
        binding.memoBackup.setOnClickListener {
            when (App.checkAuth()) {
                true -> {
                    uploadMemoData()
                    checkUploadStatus()
                }
                false -> {
                    Toast.makeText(requireContext(),
                        "계정 연동이 되어있지 않습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 메모 복원(클라우드) 기능
        binding.memoRestore.setOnClickListener {
            when (App.checkAuth()) {
                true -> {
                    downloadMemoData()
                }
                false -> {
                    Toast.makeText(requireContext(),
                        "계정 연동이 되어있지 않습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 메모 데이터 파일 내보내기 기능
    private fun exportDatabase(path: String) {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()

            if (sd!!.canWrite()) {
                val currentDB = File(data, "/data/com.example.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.example.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.example.mymemo/databases/memo.db-wal")
                val exportDB = File(sd, "/$path/memo.db")
                val exportSHM = File(sd, "/$path/memo.db-shm")
                val exportWAL = File(sd, "/$path/memo.db-wal")

                // original 파일을 overwrite 파일에 덮어씌우는 기능
                val dataStream = { original: File, overwrite: File ->
                    val inputStream = FileInputStream(original).channel
                    val outputStream = FileOutputStream(overwrite).channel
                    outputStream.transferFrom(inputStream, 0, inputStream.size())
                    inputStream.close()
                    outputStream.close()
                }

                dataStream(currentDB, exportDB)
                dataStream(currentSHM, exportSHM)
                dataStream(currentWAl, exportWAL)

                Toast.makeText(requireContext(), "내보내기 성공", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("로그", "SettingFragment - exportDatabase() 권한 오류")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "내보내기 실패", Toast.LENGTH_SHORT).show()
        }

    }

    // 메모 데이터 파일 가져오기 기능
    private fun importDatabase(path: String) {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()

            if (sd!!.canWrite()) {
                val currentDB = File(data, "/data/com.example.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.example.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.example.mymemo/databases/memo.db-wal")
                val importDB = File(sd, "/$path/memo.db")
                val importSHM = File(sd, "/$path/memo.db-shm")
                val importWAL = File(sd, "/$path/memo.db-wal")

                // original 파일을 overwrite 파일에 덮어씌우는 기능
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

                // MemoListFragment 리사이클러뷰 리프레시 (딜레이가 없으면 에러 발생)
                memoViewModel.addMemo(MemoEntity(-1))
                Handler(Looper.getMainLooper()).postDelayed({
                    memoViewModel.deleteMemo(MemoEntity(-1))
                }, 35)
                memoViewModel.selectedLabel.value = null

                Toast.makeText(requireContext(), "가져오기 성공", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("로그", "SettingFragment - importDatabase() 권한 오류")
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

    // 메모 데이터를 클라우드에 업로드하는 기능
    private fun uploadMemoData() {
        val storage = App.storage
        val storageRef = storage.reference
        val memoDataRef = storageRef.child("${App.auth.uid}").child("memoData")

        val data = Environment.getDataDirectory()
        val currentDB = Uri.fromFile(File(data, "/data/com.example.mymemo/databases/memo.db"))
        val currentSHM = Uri.fromFile(File(data, "/data/com.example.mymemo/databases/memo.db-shm"))
        val currentWAl = Uri.fromFile(File(data, "/data/com.example.mymemo/databases/memo.db-wal"))

        CoroutineScope(Dispatchers.IO).launch {
            launch {
                val uploadTaskDB = memoDataRef.child("memo.db").putFile(currentDB)
                uploadTaskDB.addOnSuccessListener {
                    uploadResultList.add(true)
                    Log.d("로그", "uploadTaskDB 업로드 성공")
                }.addOnFailureListener {
                    uploadResultList.add(false)
                    Log.d("로그", "uploadTaskDB 업로드 실패")
                }
            }

            launch {
                val uploadTaskSHM = memoDataRef.child("memo.db-shm").putFile(currentSHM)
                uploadTaskSHM.addOnSuccessListener {
                    uploadResultList.add(true)
                    Log.d("로그", "uploadTaskSHM 업로드 성공")
                }.addOnFailureListener {
                    uploadResultList.add(false)
                    Log.d("로그", "uploadTaskSHM 업로드 실패")
                }
            }

            launch {
                val uploadTaskWAl = memoDataRef.child("memo.db-wal").putFile(currentWAl)
                uploadTaskWAl.addOnSuccessListener {
                    uploadResultList.add(true)
                    Log.d("로그", "uploadTaskWAl 업로드 성공")
                }.addOnFailureListener {
                    uploadResultList.add(false)
                    Log.d("로그", "uploadTaskWAl 업로드 실패")
                }
            }
        }
    }

    // 3개 파일 모두 백업 성공했는지 확인하고 토스트 메시지 보여주는 기능
    private fun checkUploadStatus() {
        if (uploadResultList.size != 3) {
            Handler(Looper.getMainLooper()).postDelayed({
                checkUploadStatus()
            }, 500)
        } else {
            if (uploadResultList.contains(false)) {
                Toast.makeText(requireContext(), "백업 실패", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "백업 성공", Toast.LENGTH_SHORT).show()
            }
            uploadResultList.clear()
        }
    }

    // 3개 파일 모두 다운로드 성공했는지 확인하고 토스트 메시지 보여주는 기능
    private fun checkDownloadStatus() {
        if (downloadResultList.size != 3) {
            Handler(Looper.getMainLooper()).postDelayed({
                checkDownloadStatus()
            }, 500)
        } else {
            if (downloadResultList.contains(false)) {
                Toast.makeText(requireContext(), "복원 실패", Toast.LENGTH_SHORT).show()
            } else {
                if (downloadDB != null && downloadSHM != null && downloadWAL != null) {
                    restoreMemoData(downloadDB!!, downloadSHM!!, downloadWAL!!)
                } else {
                    Toast.makeText(requireContext(), "복원 실패", Toast.LENGTH_SHORT).show()
                }
            }
            downloadResultList.clear()
        }
    }

    // 메모 데이터를 클라우드에서 다운로드 하는 기능
    private fun downloadMemoData() {
        val storage = App.storage
        val storageRef = storage.reference
        val memoDataRef = storageRef.child("${App.auth.uid}").child("memoData")

        CoroutineScope(Dispatchers.IO).launch {
            launch(Dispatchers.IO) {
                val downloadTaskDB = File.createTempFile("memoData", "memo.db")
                memoDataRef.child("memo.db").getFile(downloadTaskDB)
                    .addOnSuccessListener {
                        Log.d("로그", "downloadTaskDB 다운로드 성공")
                        downloadDB = File(downloadTaskDB.absolutePath)
                        downloadResultList.add(true)
                    }
                    .addOnFailureListener {
                        Log.d("로그", "downloadTaskDB 다운로드 실패")
                        downloadResultList.add(false)
                    }
                downloadTaskDB.deleteOnExit()
            }

            launch(Dispatchers.IO) {
                val downloadTaskSHM = File.createTempFile("memoData", "memo.db-shm")
                memoDataRef.child("memo.db-shm").getFile(downloadTaskSHM)
                    .addOnSuccessListener {
                        Log.d("로그", "downloadTaskSHM 다운로드 성공")
                        downloadSHM = File(downloadTaskSHM.absolutePath)
                        downloadResultList.add(true)
                    }
                    .addOnFailureListener {
                        Log.d("로그", "downloadTaskSHM 다운로드 실패")
                        downloadResultList.add(false)
                    }
                downloadTaskSHM.deleteOnExit()
            }

            launch(Dispatchers.IO) {
                val downloadTaskWAL = File.createTempFile("memoData", "memo.db-wal")
                memoDataRef.child("memo.db-wal").getFile(downloadTaskWAL)
                    .addOnSuccessListener {
                        Log.d("로그", "downloadTaskWAL 다운로드 성공")
                        downloadWAL = File(downloadTaskWAL.absolutePath)
                        downloadResultList.add(true)
                    }
                    .addOnFailureListener {
                        Log.d("로그", "downloadTaskWAL 다운로드 실패")
                        downloadResultList.add(false)
                    }
                downloadTaskWAL.deleteOnExit()
            }

            checkDownloadStatus()
        }
    }

    // 복원 파일이 다운로드가 완료되면 메모 복원하는 기능
    private fun restoreMemoData(importDB: File, importSHM: File, importWAL: File) {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()

            if (sd!!.canWrite()) {
                val currentDB = File(data, "/data/com.example.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.example.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.example.mymemo/databases/memo.db-wal")

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

                // MemoListFragment 리사이클러뷰 리프레시 (딜레이가 없으면 에러 발생)
                memoViewModel.addMemo(MemoEntity(-1))
                Handler(Looper.getMainLooper()).postDelayed({
                    memoViewModel.deleteMemo(MemoEntity(-1))
                }, 35)

                memoViewModel.selectedLabel.value = null

                Toast.makeText(requireContext(), "복원 성공", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("로그", "SettingFragment - importDatabase() 권한 오류")
                requestPermission()
                restoreMemoData(importDB, importSHM, importWAL)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "복원 실패", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        callback.remove()
    }
}