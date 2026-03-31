package com.ai.memo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ai.memo.data.local.dao.MemoDao
import com.ai.memo.data.local.entity.MemoEntity

@Database(
    entities = [MemoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MemoDatabase : RoomDatabase() {
    abstract fun memoDao(): MemoDao
}
