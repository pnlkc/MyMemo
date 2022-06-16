package com.example.mymemo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide.init
import com.example.mymemo.room.MemoDatabase
import com.example.mymemo.room.MemoEntity
import com.example.mymemo.room.MemoRepository
import com.example.mymemo.util.App
import com.example.mymemo.util.MEMO_TYPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ViewModel이 기본생성자로 Context를 가지기 위해서는
// ViewModel이 아닌 AndroidViewModel을 상속받아야 한다
// ViewModel은 Database에 직접 접근하지 않아야 됨, Repository 로 통신해서 DB에 접근함
class MemoViewModel : ViewModel() {

    // 변수 선언
    private val _readAllData: LiveData<List<MemoEntity>>
    val readAllData: LiveData<List<MemoEntity>>
        get() = _readAllData

    private val repository: MemoRepository

    private val _selectedMemo = MutableLiveData(MemoEntity(null, "", ""))
    val selectedMemo: MutableLiveData<MemoEntity> = _selectedMemo

    private val _memoType = MutableLiveData(MEMO_TYPE.EDIT)
    val memoType: MutableLiveData<MEMO_TYPE>
        get() = _memoType

    private val _labelList = MutableLiveData(mutableListOf<String>())
    val labelList: MutableLiveData<MutableList<String>>
        get() = _labelList

    private val _selectedLabel: MutableLiveData<String?> = MutableLiveData(null)
    val selectedLabel: MutableLiveData<String?> = _selectedLabel

    // 위에서 선언한 비어있는 변수를 실제 값들과 연결
    init {
        val memoDAO = MemoDatabase.getInstance(App.context())!!.memoDAO()
        repository = MemoRepository(memoDAO)
        _readAllData = repository.readAllData
    }

    // 코루틴을 통해 Repository에 있는 addMemo() 메서드 실행
    fun addMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addMemo(memo)
        }
    }

    // 코루틴을 통해 Repository에 있는 deleteMemo() 메서드 실행
    fun deleteMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMemo(memo)
        }
    }

    fun editMemo(memo: MemoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.editMemo(memo)
        }
    }

    fun updateId(id: Long?, updateId: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateID(id, updateId)
        }
    }
}