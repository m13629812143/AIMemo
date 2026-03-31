package com.ai.memo.domain.repository

import com.ai.memo.domain.model.Memo
import kotlinx.coroutines.flow.Flow

/**
 * 备忘录仓库接口 - 定义数据操作契约
 */
interface MemoRepository {

    /** 获取所有备忘录（实时流） */
    fun getAllMemos(): Flow<List<Memo>>

    /** 根据 ID 获取单条备忘录 */
    suspend fun getMemoById(id: Long): Memo?

    /** AI 解析文本并返回结构化 Memo（不保存） */
    suspend fun parseTextWithAI(rawText: String): Memo

    /** 保存备忘录到本地数据库 */
    suspend fun saveMemo(memo: Memo): Long

    /** 更新备忘录 */
    suspend fun updateMemo(memo: Memo)

    /** 删除备忘录 */
    suspend fun deleteMemo(memo: Memo)
}
