package com.ai.memo.data.repository

import com.ai.memo.data.local.AiProvider
import com.ai.memo.data.local.SecureStorage
import com.ai.memo.data.local.dao.MemoDao
import com.ai.memo.data.local.entity.MemoEntity
import com.ai.memo.data.remote.dto.MemoParseResult
import com.ai.memo.data.remote.provider.AiProviderFactory
import com.ai.memo.domain.model.Memo
import com.ai.memo.domain.model.Priority
import com.ai.memo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MemoRepositoryImpl(
    private val memoDao: MemoDao,
    private val secureStorage: SecureStorage,
    private val aiProviderFactory: AiProviderFactory
) : MemoRepository {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        /**
         * 系统 Prompt - 指导 AI 进行结构化信息提取
         *
         * 设计思路：
         * 1. 明确角色定义：日程信息提取助手
         * 2. 严格约束 JSON Schema（time/location/event/priority/remark）
         * 3. 时间智能推算：相对时间 → 绝对日期
         * 4. 五级优先级 + 三维权重计算（时间紧迫度 * 地点远近 * 事件性质）
         * 5. 备注覆盖机制：备注中的"非常重要"可直接覆盖为最高优先级
         */
        fun buildSystemPrompt(currentDate: String): String = """
你是一个专业的日程信息提取助手。你的任务是从用户输入的自然语言文本中，提取结构化的日程信息。

当前日期时间：$currentDate

请严格按照以下 JSON 格式返回结果：
{
  "time": "时间（格式：yyyy.MM.dd HH:mm）",
  "location": "地点",
  "event": "事件名称",
  "priority": "优先级（critical/high/medium/low/minimal）",
  "remark": "备注信息"
}

═══════════════════════════════════════
一、字段提取规则
═══════════════════════════════════════

【time 时间】
- 输出格式固定为：yyyy.MM.dd HH:mm（如 2026.04.07 15:30）
- "明天" → 当前日期 +1 天
- "后天" → 当前日期 +2 天
- "下周二" → 计算出下一个周二的具体日期
- "下个月5号" → 计算出下个月5号
- 只说了时间没说日期 → 默认今天
- 完全没有时间信息 → 填写"待定"

【location 地点】
- 提取具体地址（如"朝阳区苹果派"）
- 未提及地点 → 填写"未指定"

【event 事件】
- 简洁概括事件核心（如"取快递"、"产品会议"）
- 不要照搬原文，要提炼

【remark 备注】
- 提取附加信息：需要携带的物品、注意事项、补充说明
- 无附加信息 → 填写空字符串 ""

═══════════════════════════════════════
二、priority 五级优先级 - 权重计算规则
═══════════════════════════════════════

优先级由三个维度的权重综合决定，并支持备注覆盖：

【维度1 - time 时间紧迫度】权重 40%
- 1小时内 → 权重 5（极紧急）
- 今天内 → 权重 4
- 明天 → 权重 3
- 本周内 → 权重 2
- 更远或待定 → 权重 1

【维度2 - location 地点远近】权重 20%
- 需要跨城/出差 → 权重 5（需大量准备）
- 跨区/较远 → 权重 4
- 同区/附近 → 权重 3
- 本楼/家中 → 权重 2
- 未指定 → 权重 1

【维度3 - event 事件性质】权重 40%
- 面试/考试/就医/合同签约 → 权重 5
- 工作会议/客户拜访/重要汇报 → 权重 4
- 朋友聚餐/日常社交 → 权重 3
- 取快递/购物/日常琐事 → 权重 2
- 纯休闲/可做可不做 → 权重 1

【综合计算】
加权总分 = time*0.4 + location*0.2 + event*0.4
- 总分 >= 4.2 → critical（极重要）
- 总分 >= 3.4 → high（重要）
- 总分 >= 2.6 → medium（一般）
- 总分 >= 1.8 → low（较低）
- 总分 < 1.8 → minimal（最低）

【备注覆盖规则】最高优先
如果 remark 中出现以下关键词，无论计算结果如何，priority 直接设为 critical：
"非常重要"、"特别重要"、"极其重要"、"务必"、"千万别忘"、"生死攸关"、"紧急"

═══════════════════════════════════════
三、示例
═══════════════════════════════════════

示例1：
输入："下周二早上10点在南山区科技园有个关于自驾游的产品会，记得带电脑"
输出：
{
  "time": "2026.04.07 10:00",
  "location": "南山区科技园",
  "event": "自驾游产品会",
  "priority": "high",
  "remark": "记得带电脑"
}

示例2：
输入："明天下午3点去朝阳区苹果派取快递"
输出：
{
  "time": "2026.04.01 15:00",
  "location": "朝阳区苹果派",
  "event": "取快递",
  "priority": "low",
  "remark": ""
}

示例3：
输入："后天上午去医院体检，非常重要，记得带身份证和医保卡"
输出：
{
  "time": "2026.04.02 09:00",
  "location": "医院",
  "event": "体检",
  "priority": "critical",
  "remark": "非常重要，记得带身份证和医保卡"
}

重要：只返回 JSON，不要返回任何其他文字。
        """.trimIndent()
    }

    override fun getAllMemos(): Flow<List<Memo>> {
        return memoDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMemoById(id: Long): Memo? {
        return memoDao.getById(id)?.toDomain()
    }

    override suspend fun parseTextWithAI(rawText: String): Memo {
        // 获取用户选择的 AI 提供商和对应的 API Key
        val providerId = secureStorage.getSelectedProvider()
        val provider = AiProvider.fromId(providerId)
        val apiKey = secureStorage.getApiKey(providerId)

        if (apiKey.isBlank()) {
            throw IllegalStateException("请先在设置中配置 ${provider.displayName} 的 API Key")
        }

        // 动态注入当前时间
        val currentDate = SimpleDateFormat(
            "yyyy.MM.dd HH:mm (EEEE)",
            Locale.CHINESE
        ).format(Date())

        val systemPrompt = buildSystemPrompt(currentDate)

        // 通过工厂调用对应的 AI 提供商
        val response = aiProviderFactory.chat(
            provider = provider,
            apiKey = apiKey,
            systemPrompt = systemPrompt,
            userMessage = rawText
        )

        val content = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("AI 返回结果为空，请重试")

        val parseResult = try {
            json.decodeFromString<MemoParseResult>(content)
        } catch (e: Exception) {
            throw IllegalStateException("AI 返回格式异常: ${e.message}")
        }

        return Memo(
            rawText = rawText,
            event = parseResult.event.ifBlank { "未识别事件" },
            time = parseResult.time.ifBlank { "待定" },
            location = parseResult.location.ifBlank { "未指定" },
            priority = Priority.fromString(parseResult.priority),
            remark = parseResult.remark
        )
    }

    override suspend fun saveMemo(memo: Memo): Long {
        return memoDao.insert(MemoEntity.fromDomain(memo))
    }

    override suspend fun updateMemo(memo: Memo) {
        memoDao.update(MemoEntity.fromDomain(memo))
    }

    override suspend fun deleteMemo(memo: Memo) {
        memoDao.delete(MemoEntity.fromDomain(memo))
    }
}
