package com.pnlkc.mymemo.room

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE

// DAO = Data Access Object
// 데이터에 접근할 수 있는 메서드를 정의해놓은 인터페이스
@Dao
interface MemoDAO {

    // 값 입력
    // onConflict = REPLACE => 만약에 PrimaryKey가 동일하면 덮어씌운다
    @Insert(onConflict = REPLACE)
    suspend fun insert(memo: MemoEntity)

    // tableName이 memo인 Entity의 값을 전부(*) 가져옴
    @Query("SELECT * FROM memo")
    fun getAll() : LiveData<List<MemoEntity>>

    // 값 업데이트
    @Update
    suspend fun update(memo: MemoEntity)

    // 값 삭제
    @Delete
    suspend fun delete(memo: MemoEntity)

    // id(PrimaryKey) 값 교체
    @Query("UPDATE memo SET id=:updateId WHERE id=:id")
    suspend fun updateID(id: Long?, updateId: Long?)
}