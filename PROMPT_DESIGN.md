# AI Memo - Prompt 逻辑文档

## 一、项目概述

AI Memo 是一款 Android 原生 App，核心功能是将用户输入的**非结构化自然语言文本**，通过 AI 自动提取为**结构化日程卡片**。

用户输入示例：
> "下周二早上10点在南山区科技园有个关于自驾游的产品会，记得带电脑"

AI 自动提取为：
```json
{
  "time": "2026.04.07 10:00",
  "location": "南山区科技园",
  "event": "自驾游产品会",
  "priority": "high",
  "remark": "记得带电脑"
}
```

---

## 二、开发过程中使用的关键 Prompt

### 2.1 用于「开发 App 本身」的 Prompt

以下是我在使用 AI 辅助开发时的关键 Prompt 策略：

#### Prompt 1：项目架构设计
```
Android 开发实战：智能语音/文本记事本。
用户输入一段凌乱文字，App 通过 AI 自动提取关键信息并生成结构化日程卡片。
技术栈：Kotlin + Jetpack Compose + Material 3 + Room + Retrofit + MVVM + Koin。
请设计完整的项目目录结构和架构分层。
```
**目的**：让 AI 先输出整体架构蓝图，再逐层实现，避免边写边改。

#### Prompt 2：数据层建模
```
根据以下需求设计 Room Entity 和 DAO：
- 备忘录字段：event, time, location, priority(五级), remark, rawText, createdAt
- DAO 需要支持：Flow 实时查询、按 ID 查询、插入、更新、删除
同时设计对应的 Domain Model，分离数据层和领域层。
```
**目的**：明确数据结构约束，让 AI 生成符合 Room 规范的代码。

#### Prompt 3：多模型 API 适配
```
需要支持 DeepSeek、Claude、OpenAI 三个 AI 提供商。
三者的 API 格式不同：
- DeepSeek/OpenAI：Bearer token + response_format json_object
- Claude：x-api-key header + system 是顶层字段 + 无 json_object 模式
请设计一个 AiProviderFactory，统一输出为 ChatResponse 格式。
```
**目的**：让 AI 理解不同 API 的差异，生成统一的适配层。

#### Prompt 4：安全存储设计
```
API Key 需要安全存储在 Android 本地，要求：
- 使用 EncryptedSharedPreferences + AES-256-GCM 加密
- 密钥由 Android Keystore 管理
- 不同 AI 提供商的 Key 独立存储
- 提供设置页面让用户自行配置
```
**目的**：确保 API Key 安全性，不硬编码在代码中。

#### Prompt 5：CI/CD 配置
```
配置 GitHub Actions 自动编译 Android APK：
- 使用 ubuntu-latest + JDK 17 + Gradle
- 支持 productFlavors (dev/prod) 
- 编译 prodDebug 变体
- 上传 APK 为 artifact
```
**目的**：实现自动化构建，每次 push 自动产出可安装的 APK。

### 2.2 开发过程中的调试 Prompt

#### 遇到 Claude 400 错误时的排查 Prompt
```
Claude API 返回 HTTP 400: "max_tokens: Field required"。
请检查 ClaudeRequest 的 kotlinx.serialization 配置，
maxTokens 字段有 @SerialName("max_tokens") 和默认值 1024，
为什么序列化时会丢失这个字段？
```
**定位结果**：`kotlinx.serialization` 默认 `encodeDefaults = false`，有默认值的字段不会被序列化。修复：`Json { encodeDefaults = true }`。

---

## 三、App 内 AI 解析的 System Prompt 设计

这是 App 的核心——发送给 AI 的 System Prompt，用于指导 AI 从用户输入中提取结构化信息。

### 3.1 Prompt 设计原则

| 原则 | 说明 |
|------|------|
| **角色定义** | 明确告诉 AI "你是日程信息提取助手"，限定行为边界 |
| **动态时间注入** | 每次调用注入当前日期时间，让 AI 能准确计算"明天""下周二" |
| **JSON Schema 约束** | 在 Prompt 中给出完整的 JSON 结构和字段说明 |
| **量化优先级** | 用三维权重公式替代主观判断，确保优先级一致性 |
| **Few-shot 示例** | 提供 3 个覆盖不同场景的完整输入→输出示例 |
| **边界处理** | 明确信息缺失时的默认值策略（"未指定"、"待定"） |
| **备注覆盖** | 尊重用户主观判断："非常重要"直接覆盖为最高优先级 |

### 3.2 完整 System Prompt

```text
你是一个专业的日程信息提取助手。你的任务是从用户输入的自然语言文本中，提取结构化的日程信息。

当前日期时间：{动态注入，如 2026.04.01 09:05 (星期三)}

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

示例1（工作会议 → high）：
输入："下周二早上10点在南山区科技园有个关于自驾游的产品会，记得带电脑"
输出：
{"time":"2026.04.07 10:00","location":"南山区科技园","event":"自驾游产品会","priority":"high","remark":"记得带电脑"}

示例2（日常琐事 → low）：
输入："明天下午3点去朝阳区苹果派取快递"
输出：
{"time":"2026.04.01 15:00","location":"朝阳区苹果派","event":"取快递","priority":"low","remark":""}

示例3（备注覆盖 → critical）：
输入："后天上午去医院体检，非常重要，记得带身份证和医保卡"
输出：
{"time":"2026.04.02 09:00","location":"医院","event":"体检","priority":"critical","remark":"非常重要，记得带身份证和医保卡"}

重要：只返回 JSON，不要返回任何其他文字。
```

### 3.3 Prompt 中的关键技术决策

| 决策 | 为什么这样做 |
|------|-------------|
| 动态注入当前时间 | AI 无法获取系统时间，必须显式告知才能计算"明天""下周二"等相对时间 |
| 三维权重公式而非关键词匹配 | 关键词匹配容易遗漏（如"给客户做汇报"没有"重要"关键词但实际很重要），权重公式更稳定 |
| 时间权重 40% + 事件权重 40% + 地点权重 20% | 时间和事件性质对优先级影响最大，地点主要影响准备时间 |
| 备注覆盖机制 | 用户明确说"非常重要"时，应尊重主观判断，覆盖客观计算结果 |
| 三个 Few-shot 示例 | 分别覆盖高/低/覆盖三种场景，让 AI 理解不同优先级的输出预期 |
| "只返回 JSON" | 配合 DeepSeek/OpenAI 的 `response_format: json_object`，双重保障输出格式 |

---

## 四、JSON Schema 定义

### 4.1 AI 返回的 JSON Schema

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["time", "location", "event", "priority", "remark"],
  "properties": {
    "time": {
      "type": "string",
      "description": "时间，格式 yyyy.MM.dd HH:mm，无法确定时填'待定'",
      "example": "2026.04.07 15:30"
    },
    "location": {
      "type": "string",
      "description": "地点，未提及时填'未指定'",
      "example": "朝阳区苹果派"
    },
    "event": {
      "type": "string",
      "description": "事件名称，简洁概括",
      "example": "取快递"
    },
    "priority": {
      "type": "string",
      "enum": ["critical", "high", "medium", "low", "minimal"],
      "description": "五级优先级，由三维权重公式计算"
    },
    "remark": {
      "type": "string",
      "description": "备注信息，附加说明，无则为空字符串",
      "example": "记得带手机"
    }
  }
}
```

### 4.2 五级优先级定义

| 等级 | 枚举值 | 分值 | 中文标签 | 颜色 | 触发条件 |
|------|--------|------|----------|------|----------|
| 5 | `critical` | >= 4.2 | 极重要 | 深红 | 高分计算 或 备注含"非常重要/紧急" |
| 4 | `high` | >= 3.4 | 重要 | 红 | 工作会议 + 时间较近 |
| 3 | `medium` | >= 2.6 | 一般 | 琥珀 | 普通社交、日常安排 |
| 2 | `low` | >= 1.8 | 较低 | 绿 | 取快递、购物等琐事 |
| 1 | `minimal` | < 1.8 | 最低 | 灰 | 无时间压力的可选活动 |

### 4.3 Kotlin 侧的数据映射

AI 返回的 JSON 通过 `kotlinx.serialization` 反序列化为 `MemoParseResult`：

```kotlin
@Serializable
data class MemoParseResult(
    val time: String = "",
    val location: String = "",
    val event: String = "",
    val priority: String = "medium",
    val remark: String = ""
)
```

然后映射为领域模型 `Memo`，其中 `priority` 字符串通过 `Priority.fromString()` 转换为枚举：

```kotlin
enum class Priority(val level: Int, val label: String) {
    CRITICAL(5, "极重要"),
    HIGH(4, "重要"),
    MEDIUM(3, "一般"),
    LOW(2, "较低"),
    MINIMAL(1, "最低");

    companion object {
        fun fromString(value: String): Priority = when (value.lowercase().trim()) {
            "critical", "5", "极重要", "最高" -> CRITICAL
            "high", "4", "重要", "高"        -> HIGH
            "medium", "3", "一般", "中"      -> MEDIUM
            "low", "2", "较低", "低"         -> LOW
            "minimal", "1", "最低"           -> MINIMAL
            else -> MEDIUM  // 兜底默认值
        }
    }
}
```

---

## 五、多模型适配策略

App 支持三个 AI 提供商，Prompt 完全相同，差异在 API 调用层：

| 差异点 | DeepSeek | Claude | OpenAI |
|--------|----------|--------|--------|
| 认证方式 | `Authorization: Bearer sk-xxx` | `x-api-key: sk-ant-xxx` | `Authorization: Bearer sk-xxx` |
| System Prompt 位置 | `messages[0].role = "system"` | 顶层 `system` 字段 | `messages[0].role = "system"` |
| JSON 强制模式 | `response_format: {"type":"json_object"}` | 不支持，靠 Prompt 约束 | `response_format: {"type":"json_object"}` |
| API 端点 | `POST /chat/completions` | `POST /v1/messages` | `POST /v1/chat/completions` |
| 额外 Header | 无 | `anthropic-version: 2023-06-01` | 无 |

### Claude 的 JSON 输出兼容处理

由于 Claude 不支持 `response_format: json_object`，AI 可能返回 markdown 包裹的 JSON。App 通过 `extractJson()` 方法容错处理：

```kotlin
private fun extractJson(raw: String): String {
    val trimmed = raw.trim()
    // 1. 已经是纯 JSON
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
    // 2. 提取 ```json ... ``` 代码块
    val codeBlockRegex = Regex("```(?:json)?\\s*\\n?(\\{[\\s\\S]*?\\})\\s*\\n?```")
    codeBlockRegex.find(trimmed)?.let { return it.groupValues[1].trim() }
    // 3. 兜底：找第一个 { 到最后一个 }
    val first = trimmed.indexOf('{')
    val last = trimmed.lastIndexOf('}')
    if (first != -1 && last > first) return trimmed.substring(first, last + 1)
    return trimmed
}
```

---

## 六、API 调用完整流程

```
用户输入文本："后天上午去医院体检，非常重要"
        │
        ▼
┌─ MemoRepositoryImpl.parseTextWithAI() ─────────────────────┐
│                                                             │
│  1. 读取 SecureStorage → 获取选中的 AI 提供商 + API Key     │
│  2. 获取当前时间 → "2026.04.01 09:05 (星期三)"              │
│  3. 注入时间到 System Prompt                                │
│                                                             │
│  4. 调用 AiProviderFactory.chat()                           │
│     ├── DeepSeek → Bearer token + json_object 模式          │
│     ├── Claude   → x-api-key + system 顶层字段              │
│     └── OpenAI   → Bearer token + json_object 模式          │
│                                                             │
│  5. 收到响应 → extractJson() 提取纯 JSON                    │
│  6. kotlinx.serialization 反序列化 → MemoParseResult        │
│  7. 映射为 Memo 领域模型（Priority.fromString 转换优先级）   │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
   展示预览卡片 → 用户确认 → 存入 Room 数据库
```

---

## 七、容错与异常处理

| 异常场景 | 处理方式 | 代码位置 |
|----------|----------|----------|
| API Key 未配置 | 抛出提示"请先在设置中配置 xxx 的 API Key" | `MemoRepositoryImpl.kt` |
| AI 返回空内容 | 抛出"AI 返回结果为空，请重试" | `MemoRepositoryImpl.kt` |
| HTTP 错误 (400/401/500) | 捕获 HttpException，展示具体状态码和错误体 | `AiProviderFactory.kt` |
| JSON 解析失败 | 捕获异常，展示原始响应内容辅助排查 | `MemoRepositoryImpl.kt` |
| 网络超时 | OkHttp 读超时 120s，连接超时 60s | `AppModule.kt` |
| 未知优先级字符串 | `Priority.fromString()` 兜底返回 MEDIUM | `Memo.kt` |
| Claude 返回 markdown 包裹 JSON | `extractJson()` 三级容错提取 | `MemoRepositoryImpl.kt` |
| `max_tokens` 未序列化 | `Json { encodeDefaults = true }` 强制序列化默认值 | `AiProviderFactory.kt` |

---

## 八、Prompt 迭代历程

### v1 — 基础版
```
请从以下文本中提取日程信息，返回 JSON。
```
**问题**：输出格式不稳定，有时返回 markdown，优先级随意。

### v2 — 加入 Schema + 三级优先级
```
请按以下格式返回：{"event":"","time":"","location":"","priority":"high/medium/low","remark":""}
优先级：high=重要，medium=普通，low=低。
```
**问题**：三级优先级粒度不够；时间是相对的（"明天"）AI 不知道今天是哪天。

### v3 — 最终版
- 五级优先级 + 三维权重公式（时间 40% + 地点 20% + 事件 40%）
- 动态注入当前日期时间
- 备注覆盖机制（"非常重要" → 直接 critical）
- 3 个 Few-shot 示例覆盖 high / low / critical 三种场景
- 明确的时间推算规则（明天=+1天、下周二=下一个周二）
- `response_format: json_object` + Prompt 双重保障输出格式

---

## 九、关键代码文件索引

| 功能 | 文件路径 |
|------|----------|
| System Prompt + 权重逻辑 | `data/repository/MemoRepositoryImpl.kt` → `buildSystemPrompt()` |
| JSON 提取容错 | `data/repository/MemoRepositoryImpl.kt` → `extractJson()` |
| 五级 Priority 枚举 | `domain/model/Memo.kt` |
| API 请求/响应数据模型 | `data/remote/dto/DeepSeekModels.kt` |
| 多模型适配工厂 | `data/remote/provider/AiProviderFactory.kt` |
| API Key 加密存储 | `data/local/SecureStorage.kt` |
| AI 提供商配置 | `data/local/SecureStorage.kt` → `AiProvider` 枚举 |
| 设置页面 UI | `ui/screen/settings/SettingsScreen.kt` |
| 环境隔离配置 | `app/build.gradle.kts` → `productFlavors` |
| CI 自动构建 | `.github/workflows/build.yml` |
