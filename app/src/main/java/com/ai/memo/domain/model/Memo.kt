package com.ai.memo.domain.model

/**
 * 领域模型 - 备忘录
 */
data class Memo(
    val id: Long = 0,
    val rawText: String,           // 用户原始输入
    val event: String,             // AI 提取的事件
    val time: String,              // AI 提取的时间（格式：yyyy.MM.dd HH:mm）
    val location: String,          // AI 提取的地点
    val priority: Priority,        // AI 综合权重判断的优先级（五级）
    val remark: String = "",       // AI 提取的备注
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 五级优先级
 *
 * 权重计算逻辑（由 AI 综合判断）：
 * - time 权重：时间越近，紧急度越高
 * - location 权重：距离越远，重要性越高（需要提前准备）
 * - event 权重：AI 根据事件性质智能分析（会议 > 社交 > 休闲）
 * - remark 覆盖：若备注中明确标注"非常重要/紧急"等，可直接提升至最高级
 */
enum class Priority(val level: Int, val label: String) {
    CRITICAL(5, "极重要"),   // 紧急且重要，或备注强调"非常重要"
    HIGH(4, "重要"),         // 工作会议、面试、就医等
    MEDIUM(3, "一般"),       // 普通社交、日常安排
    LOW(2, "较低"),          // 休闲娱乐、可选活动
    MINIMAL(1, "最低");      // 无时间压力的琐事

    companion object {
        fun fromString(value: String): Priority {
            return when (value.lowercase().trim()) {
                "critical", "5", "极重要", "最高" -> CRITICAL
                "high", "4", "重要", "高" -> HIGH
                "medium", "3", "一般", "中" -> MEDIUM
                "low", "2", "较低", "低" -> LOW
                "minimal", "1", "最低" -> MINIMAL
                else -> MEDIUM
            }
        }
    }
}
