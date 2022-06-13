package com.example.mymemo.room

import androidx.lifecycle.LiveData

// Single source of truth for all app data, clean API for UI to communicate with
// DAO나 네트워크를 통해 데이터를 요청하고 가져오는 기능
// Repository는 기본 생성자로 DAO를 가짐
class MemoRepository(private val memoDAO: MemoDAO) {

    // MemoDAO의 getAll() 메소드를 통해 데이터 값 전부 가져오기
    val readAllData : LiveData<List<MemoEntity>> = memoDAO.getAll()

    // MemoDAO의 insert() 메소드를 통해 데이터 추가하기
    suspend fun addMemo(memo: MemoEntity) {
        memoDAO.insert(memo)
    }

    // MemoDAO의 delete() 메소드를 통해 데이터 삭제하기
    suspend fun deleteMemo(memo: MemoEntity) {
        memoDAO.delete(memo)
    }

    // MemoDAO의 update() 메소드를 통해 데이터 수정(update)하기
    suspend fun editMemo(memo: MemoEntity) {
        memoDAO.update(memo)
    }

    // MemoDAO의 updateID()를 통해 위치이동한 데이터 id 스왑하기
    suspend fun updateID(id: Long?, updateId: Long?) {
        memoDAO.updateID(id, updateId)
    }
}