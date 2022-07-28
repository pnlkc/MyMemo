package com.pnlkc.mymemo.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

// Entitiy는 개체, 독립체 이런 뜻으로
// 여기서는 "id"와 "memo" 속성을 가지는 "memo"라는 이름의 개체이다
// 이중 "id" 값은 각각의 데이터의 고유 값인 PrimaryKey로 지정되어 있다
@Entity(tableName = "memo")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long?,
    var title: String = "",
    var memo: String = "",
    var date: String = "",
    var language: String = Locale.getDefault().language,
    var label: MutableList<String> = mutableListOf()
)