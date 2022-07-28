package com.pnlkc.mymemo.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pnlkc.mymemo.util.GsonConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

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

        fun getInstance(context: Context): MemoDatabase? {
            if (INSTANCE == null) {
                synchronized(MemoDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        MemoDatabase::class.java,
                        "memo.db"
                    ).addCallback(object : Callback() {
                        // 데이터베이스 생성시 최초 데이터 등록
                        // 라벨 리스트 저장용 메모 생성
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context)!!.memoDAO().insert(
                                    MemoEntity(
                                        -2,
                                        "LabelList",
                                        "",
                                        "00-00-00 오전 00:00",
                                        Locale.getDefault().language,
                                        mutableListOf())
                                )
                            }
                        }

                    }).build()
                }
            }

            return INSTANCE
        }
    }
}