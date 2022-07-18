package com.pnlkc.mymemo

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.pnlkc.mymemo.databinding.FragmentSettingBinding
import com.pnlkc.mymemo.room.MemoEntity
import com.pnlkc.mymemo.util.App
import com.pnlkc.mymemo.util.ConstData.KEY_ADD_TIME_BTN_SETTING
import com.pnlkc.mymemo.util.ConstData.KEY_PREFS
import com.pnlkc.mymemo.util.ConstData.KEY_VIBRATION_SETTING
import com.pnlkc.mymemo.util.DialogCreator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val memoViewModel: MemoViewModel by activityViewModels()

    // OnBackPressedCallback (뒤로가기 기능) 객체 선언
    private lateinit var callback: OnBackPressedCallback

    // 파이어베이스에 업로드하거나 다운로드 받을 때 모든 파일이 정상적으로 처리되었는지 확인용 변수
    private val uploadResultList = mutableListOf<Boolean>()
    private val downloadResultList = mutableListOf<Boolean>()

    // 파이어베이스에서 다운로드 파일을 임시저장할 변수
    private var downloadDB: File? = null
    private var downloadSHM: File? = null
    private var downloadWAL: File? = null

    // 백업이나 복원 중에 다른 프래그먼트로 이동하지 못하게 하는 기능 구현용 변수
    private var isWorking = false

    // 백업이나 복원 작업 완료 시 진동 유무 설정용 변수
    private var isVibrate = true

    // API 30 이상 퍼미션 런처
    private lateinit var highAPIPermissionLauncher: ActivityResultLauncher<Intent>
    // API 30 미만 퍼미션 런처
    private lateinit var lowAPIPermissionLauncher: ActivityResultLauncher<Array<String>>
    // 백업 및 복원시 최초 권한 획득 후, 기존의 작업을 자동으로 실행하기 위한 변수
    private lateinit var currentAction: () -> Unit

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
                backAction()
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setVibrationSetting("load")
        setAddTimeBtnSetting("load")

        // 다크모드 감지 코드 -> 다크모드에 따라 로딩 애니메이션 변경
        when (requireContext().resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                binding.memoBackupLoadingLottie.setAnimation(R.raw.loading_lottie_animation_night)
                binding.memoRestoreLoadingLottie.setAnimation(R.raw.loading_lottie_animation_night)
            }
            else -> {
                binding.memoBackupLoadingLottie.setAnimation(R.raw.loading_lottie_animation)
                binding.memoRestoreLoadingLottie.setAnimation(R.raw.loading_lottie_animation)
            }
        }

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

        highAPIPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (isPermissionGranted()) {
                    requestPermission { currentAction() }
                } else {
                    Toast.makeText(requireContext(),
                        "이 기능을 실행하려면 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
            }

        lowAPIPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                if (it.values.contains(false)) {
                    Toast.makeText(requireContext(),
                        "이 기능을 실행하려면 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                } else {
                    currentAction()
                }
            }


        // 상단의 뒤로가기 화살표 버튼 설정
        binding.backButton.setOnClickListener { backAction() }

        // 백업 완료 시 진동 설정
        binding.vibrationSetting.setOnClickListener {
            binding.vibrationSettingSwitch.isChecked = !binding.vibrationSettingSwitch.isChecked
        }

        binding.vibrationSettingSwitch.setOnCheckedChangeListener { _, isChecked ->
            isVibrate = when (isChecked) {
                true -> true
                false -> false
            }
            setVibrationSetting("save")
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
            currentAction = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                exportLauncher.launch(intent)
            }
            requestPermission { currentAction() }
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
            currentAction = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                importLauncher.launch(intent)
            }
            requestPermission { currentAction() }
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
                    DialogCreator().showDialog(
                        requireContext(),
                        getString(R.string.app_name),
                        "이 앱과 연결된 계정을 로그아웃하시겠습니까?"
                    ) {
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
                }
                // 로그인 되어있지 않으면
                false -> {
                    val gso = GoogleSignInOptions
                        .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        //R.string.default_web_client_id 에러시
                        // project 수준의 classpath ...google-services:4.3.8 로 변경
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val signInIntent = GoogleSignIn.getClient(requireContext(), gso).signInIntent
                    gsoLauncher.launch(signInIntent)
                }
            }
        }


        // 메모 백업(클라우드) 기능
        binding.memoBackup.setOnClickListener {
            DialogCreator().showDialog(
                requireContext(),
                getString(R.string.app_name),
                "메모를 백업하시겠습니까?"
            ) {
                currentAction = {
                    binding.memoBackupLoading.visibility = View.VISIBLE
                    binding.touchBlocker.visibility = View.VISIBLE
                    isWorking = true
                    when (App.checkAuth()) {
                        true -> {
                            uploadMemoData()
                        }
                        false -> {
                            Toast.makeText(requireContext(),
                                "계정 연동이 되어있지 않습니다", Toast.LENGTH_SHORT).show()
                            binding.memoBackupLoading.visibility = View.INVISIBLE
                            binding.touchBlocker.visibility = View.INVISIBLE
                            isWorking = false
                        }
                    }
                }

                requestPermission {
                    currentAction()
                }
            }
        }

        // 메모 복원(클라우드) 기능
        binding.memoRestore.setOnClickListener {
            DialogCreator().showDialog(
                requireContext(),
                getString(R.string.app_name),
                "메모를 복원하시겠습니까?"
            ) {
                currentAction = {
                    binding.memoRestoreLoading.visibility = View.VISIBLE
                    binding.touchBlocker.visibility = View.VISIBLE
                    isWorking = true
                    when (App.checkAuth()) {
                        true -> {
                            downloadMemoData()
                        }
                        false -> {
                            Toast.makeText(requireContext(),
                                "계정 연동이 되어있지 않습니다", Toast.LENGTH_SHORT).show()
                            binding.memoRestoreLoading.visibility = View.INVISIBLE
                            binding.touchBlocker.visibility = View.INVISIBLE
                            isWorking = false
                        }
                    }
                }

                requestPermission {
                    currentAction()
                }
            }
        }

        binding.touchBlocker.setOnClickListener {
            Toast.makeText(requireContext(),
                "백업이나 복원이 완료된 다음 다시 시도해주세요", Toast.LENGTH_SHORT).show()
        }

        // 시간 추가 버튼 라디오 그룹 리스너
        binding.addTimeBtnRadioGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.year_radio_btn -> memoViewModel.addTimeBtnType = "year"
                R.id.month_radio_btn -> memoViewModel.addTimeBtnType = "month"
                R.id.time_radio_btn -> memoViewModel.addTimeBtnType = "time"
            }
            setAddTimeBtnSetting("save")
        }
    }

    private fun backAction() {
        when (isWorking) {
            true -> Toast.makeText(requireContext(),
                "백업이나 복원이 완료된 다음 화면을 이동해주세요", Toast.LENGTH_SHORT).show()
            false -> removeFragment()
        }
    }

    // 메모 데이터 파일 내보내기 기능
    private fun exportDatabase(path: String) {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()

            if (sd!!.canWrite()) {
                val currentDB = File(data, "/data/com.pnlkc.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.pnlkc.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.pnlkc.mymemo/databases/memo.db-wal")
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

                makeVibration()
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
                val currentDB = File(data, "/data/com.pnlkc.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.pnlkc.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.pnlkc.mymemo/databases/memo.db-wal")
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

                makeVibration()
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
    private fun requestPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!isPermissionGranted()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                highAPIPermissionLauncher.launch(intent)
            } else {
                action()
            }
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
                lowAPIPermissionLauncher.launch(permission)
            } else {
                action()
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
        val currentDB = Uri.fromFile(File(data, "/data/com.pnlkc.mymemo/databases/memo.db"))
        val currentSHM = Uri.fromFile(File(data, "/data/com.pnlkc.mymemo/databases/memo.db-shm"))
        val currentWAl = Uri.fromFile(File(data, "/data/com.pnlkc.mymemo/databases/memo.db-wal"))

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

        checkUploadStatus()
    }

    // 3개 파일 모두 백업 성공했는지 확인하고 토스트 메시지 보여주는 기능
    private fun checkUploadStatus() {
        binding.memoBackupLoadingTextview.text = "메모를 백업하고 있습니다 (${uploadResultList.size}/3)"
        if (uploadResultList.size != 3) {
            Handler(Looper.getMainLooper()).postDelayed({
                checkUploadStatus()
            }, 35)
        } else {
            if (uploadResultList.contains(false)) {
                Toast.makeText(requireContext(), "백업 실패", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "백업 성공", Toast.LENGTH_SHORT).show()
            }
            uploadResultList.clear()

            // 백업 완료 후
            binding.memoBackupLoading.visibility = View.INVISIBLE
            binding.touchBlocker.visibility = View.INVISIBLE
            isWorking = false
            makeVibration()
        }
    }

    // 3개 파일 모두 다운로드 성공했는지 확인하고 토스트 메시지 보여주는 기능
    private fun checkDownloadStatus() {
        binding.memoRestoreLoadingTextview.text = "메모를 복원하고 있습니다 (${downloadResultList.size}/3)"
        if (downloadResultList.size != 3) {
            Handler(Looper.getMainLooper()).postDelayed({
                checkDownloadStatus()
            }, 35)
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
        }

        checkDownloadStatus()
    }

    // 복원 파일이 다운로드가 완료되면 메모 복원하는 기능
    private fun restoreMemoData(importDB: File, importSHM: File, importWAL: File) {
        try {
            val sd = Environment.getExternalStorageDirectory()
            val data = Environment.getDataDirectory()

            if (sd!!.canWrite()) {
                val currentDB = File(data, "/data/com.pnlkc.mymemo/databases/memo.db")
                val currentSHM = File(data, "/data/com.pnlkc.mymemo/databases/memo.db-shm")
                val currentWAl = File(data, "/data/com.pnlkc.mymemo/databases/memo.db-wal")

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
                Log.d("로그", "SettingFragment - restoreMemoData() 권한 오류")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "복원 실패", Toast.LENGTH_SHORT).show()
        }

        // 복원 완료 후
        binding.memoRestoreLoading.visibility = View.INVISIBLE
        binding.touchBlocker.visibility = View.INVISIBLE
        isWorking = false
        makeVibration()
    }

    // 진동 1회 발생
    private fun makeVibration() {
        if (isVibrate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibrator =
                    requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrationEffect =
                    VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE)
                val combinedVibration = CombinedVibration.createParallel(vibrationEffect)
                vibrator.vibrate(combinedVibration)
            } else {
                @Suppress("DEPRECATION")
                val vibrator =
                    requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val vibrationEffect =
                    VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            }
        }
    }

    // SharedPreferences 사용해서 진동 설정 저장
    private fun setVibrationSetting(mode: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)
        when (mode) {
            "save" -> {
                val editor = sharedPreferences.edit()
                editor.putBoolean(KEY_VIBRATION_SETTING, isVibrate)
                editor.apply()
            }
            "load" -> {
                if (sharedPreferences.contains(KEY_VIBRATION_SETTING)) {
                    isVibrate = sharedPreferences.getBoolean(KEY_VIBRATION_SETTING, true)
                    binding.vibrationSettingSwitch.isChecked = isVibrate
                }
            }
        }
    }

    // SharedPreferences 사용해서 시간 추가 버튼 설정 저장
    private fun setAddTimeBtnSetting(mode: String) {
        val sharedPreferences =
            requireActivity().getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE)
        when (mode) {
            "save" -> {
                val editor = sharedPreferences.edit()
                editor.putString(KEY_ADD_TIME_BTN_SETTING, memoViewModel.addTimeBtnType)
                editor.apply()
            }
            "load" -> {
                if (sharedPreferences.contains(KEY_ADD_TIME_BTN_SETTING)) {
                    memoViewModel.addTimeBtnType =
                        sharedPreferences.getString(KEY_ADD_TIME_BTN_SETTING, "time") ?: "time"
                    when (memoViewModel.addTimeBtnType) {
                        "year" -> binding.addTimeBtnRadioGroup.check(R.id.year_radio_btn)
                        "month" -> binding.addTimeBtnRadioGroup.check(R.id.month_radio_btn)
                        else -> binding.addTimeBtnRadioGroup.check(R.id.time_radio_btn)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        callback.remove()
    }
}