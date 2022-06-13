package com.example.mymemo.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mymemo.util.GsonConverter

// entities는 사용할 Entitiy를 선언 해주면 됨 []는 배열을 의미
// version은 Entity 구조 변경시 구분해주는 역할
@Database(entities = [MemoEntity::class], version = 1)
@TypeConverters(GsonConverter::class)
abstract class MemoDatabase : RoomDatabase() {

    abstract fun memoDAO(): MemoDAO

    // 싱글톤으로 데이터 베이스 만들기
    companion object {
        @Volatile
        var INSTANCE: MemoDatabase? = null

        fun getInstance(context: Context) : MemoDatabase? {
            if (INSTANCE == null) {
                synchronized(MemoDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        MemoDatabase::class.java,
                        "memo.db"
                    ).build()
                }
            }

            return INSTANCE
        }
    }
}