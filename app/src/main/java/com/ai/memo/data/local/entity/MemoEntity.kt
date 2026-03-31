package com.ai.memo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ai.memo.domain.model.Memo
import com.ai.memo.domain.model.Priority

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val event: String,
    val time: String,
    val location: String,
    val priority: String,       // 存储为字符串
    val remark: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    /** Entity -> Domain Model */
    fun toDomain(): Memo = Memo(
        id = id,
        rawText = rawText,
        event = event,
        time = time,
        location = location,
        priority = Priority.fromString(priority),
        remark = remark,
        createdAt = createdAt
    )

    companion object {
        /** Domain Model -> Entity */
        fun fromDomain(memo: Memo): MemoEntity = MemoEntity(
            id = memo.id,
            rawText = memo.rawText,
            event = memo.event,
            time = memo.time,
            location = memo.location,
            priority = memo.priority.name,
            remark = memo.remark,
            createdAt = memo.createdAt
        )
    }
}
