# AI Memo - Prompt 逻辑文档

## 一、概述

本文档记录了 AI Memo 应用中，如何设计 Prompt 来驱动 DeepSeek API 完成**自然语言 -> 结构化日程**的信息提取任务，以及五级优先级权重计算体系的设计。

---

## 二、AI 接口选型

| 项目 | 选择 | 理由 |
|------|------|------|
| 模型 | `deepseek-chat` (DeepSeek-V3) | 中文理解能力强，价格低（$0.28/M tokens），支持 JSON mode |
| 输出模式 | `response_format: {"type": "json_object"}` | 强制返回合法 JSON，避免 Markdown 包裹 |
| 温度 | `0.1` | 信息提取任务需要确定性输出，不需要创造性 |
| max_tokens | `1024` | 防止 JSON 被截断 |

---

## 三、JSON Schema 定义

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["time", "location", "event", "priority", "remark"],
  "properties": {
    "time": {
      "type": "string",
      "description": "时间，格式 yyyy.MM.dd HH:mm",
      "example": "2026.04.07 15:30"
    },
    "location": {
      "type": "string",
      "description": "地点",
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
      "description": "五级优先级，由权重公式计算"
    },
    "remark": {
      "type": "string",
      "description": "备注信息，附加说明",
      "example": "记得带手机"
    }
  }
}
```

---

## 四、五级优先级权重计算体系

这是本 App 的核心设计亮点。优先级不是简单的关键词匹配，而是由三个维度的权重综合计算得出。

### 4.1 三维权重模型

```
加权总分 = time × 0.4 + location × 0.2 + event × 0.4
```

#### 维度 1 - time 时间紧迫度（权重 40%）

| 时间距离 | 权重 |
|----------|------|
| 1 小时内 | 5（极紧急） |
| 今天内 | 4 |
| 明天 | 3 |
| 本周内 | 2 |
| 更远或待定 | 1 |

#### 维度 2 - location 地点远近（权重 20%）

| 地点距离 | 权重 |
|----------|------|
| 跨城/出差 | 5（需大量准备） |
| 跨区/较远 | 4 |
| 同区/附近 | 3 |
| 本楼/家中 | 2 |
| 未指定 | 1 |

#### 维度 3 - event 事件性质（权重 40%）

| 事件类型 | 权重 |
|----------|------|
| 面试/考试/就医/签约 | 5 |
| 工作会议/客户拜访 | 4 |
| 朋友聚餐/社交 | 3 |
| 取快递/购物/琐事 | 2 |
| 纯休闲/可选 | 1 |

### 4.2 总分映射

| 加权总分 | 优先级 | 标签 |
|----------|--------|------|
| >= 4.2 | critical | 极重要 |
| >= 3.4 | high | 重要 |
| >= 2.6 | medium | 一般 |
| >= 1.8 | low | 较低 |
| < 1.8 | minimal | 最低 |

### 4.3 备注覆盖规则（最高优先）

如果 remark 中出现以下关键词，**无论计算结果如何**，priority 直接设为 `critical`：
- "非常重要"、"特别重要"、"极其重要"
- "务必"、"千万别忘"、"紧急"、"生死攸关"

**设计意图**：用户在备注中明确强调重要性时，应尊重用户的主观判断，覆盖 AI 的客观计算。

---

## 五、Prompt 设计

### 5.1 关键设计决策

1. **动态注入当前时间**：每次调用时将 `当前日期时间` 注入 System Prompt，让 AI 能准确计算"明天"、"下周二"等相对时间。

2. **权重计算指令化**：在 Prompt 中明确写出权重公式和阈值，让 AI 按公式执行而非猜测。

3. **Few-shot 示例覆盖三种场景**：
   - 示例 1：标准场景（工作会议 -> high）
   - 示例 2：低优先级场景（取快递 -> low）
   - 示例 3：备注覆盖场景（"非常重要" -> critical）

### 5.2 System Prompt 核心结构

```
[角色定义] → 日程信息提取助手
[当前时间注入] → 动态生成
[JSON Schema] → 五字段定义
[字段提取规则] → time/location/event/remark 各自的处理逻辑
[权重计算规则] → 三维权重 + 阈值映射
[备注覆盖规则] → 关键词列表
[Few-shot 示例] → 三个典型场景
[输出约束] → "只返回 JSON"
```

完整 Prompt 见 `data/repository/MemoRepositoryImpl.kt` -> `buildSystemPrompt()` 方法。

---

## 六、API 调用流程

```
1. 用户输入原始文本
     |
2. 获取当前日期时间 → 注入 System Prompt
     |
3. 构造 ChatRequest
   - model: "deepseek-chat"
   - messages: [system_prompt(含当前时间), user_input]
   - response_format: {"type": "json_object"}
   - temperature: 0.1
   - max_tokens: 1024
     |
4. POST https://api.deepseek.com/chat/completions
     |
5. 校验 finish_reason == "stop"
     |
6. 解析 JSON → MemoParseResult
     |
7. 转换为 Memo 领域模型（Priority.fromString 五级映射）
     |
8. 展示预览卡片 → 用户确认 → 存入 Room
```

---

## 七、容错处理

| 场景 | 处理方式 |
|------|----------|
| AI 返回空内容 | 抛出异常，UI 提示"AI 返回结果为空，请重试" |
| JSON 解析失败 | 捕获异常，UI 提示"AI 返回格式异常" |
| 网络超时 | OkHttp 设置 120s 读超时 |
| 字段缺失 | Kotlinx Serialization 默认值兜底 |
| 未知优先级字符串 | `Priority.fromString()` 默认返回 MEDIUM |
| finish_reason = "length" | JSON 可能被截断，当作解析失败 |

---

## 八、环境隔离策略（类容器化）

通过 Gradle Build Variants 实现多环境隔离：

| Flavor | Build Type | 用途 | 日志 | API 地址 |
|--------|-----------|------|------|----------|
| dev + debug | 开发调试 | 本地开发 | 开启 | BuildConfig 注入 |
| dev + release | 开发发布 | 内测 | 关闭 | BuildConfig 注入 |
| prod + debug | 生产调试 | 线上问题排查 | 开启 | BuildConfig 注入 |
| prod + release | 正式发布 | 用户使用 | 关闭 | BuildConfig 注入 |

API Key 通过 `local.properties`（本地）或 `GitHub Secrets`（CI）注入，不写死在代码中。

---

## 九、Prompt 迭代历程

### v1（初版）
```
请从以下文本中提取日程信息，返回 JSON。
```
**问题**：格式不稳定，优先级判断随意。

### v2（加入 Schema + 三级优先级）
```
请按以下格式返回：{"event":"","time":"","location":"","priority":"high/medium/low","remark":""}
```
**问题**：优先级粒度不够，无法区分"紧急会议"和"普通会议"。

### v3（最终版 - 五级优先级 + 权重体系）
- 五级优先级：critical / high / medium / low / minimal
- 三维权重公式：time×0.4 + location×0.2 + event×0.4
- 备注覆盖机制
- 动态时间注入
- 三个 Few-shot 示例覆盖不同场景

---

## 十、关键代码位置

| 功能 | 文件 |
|------|------|
| System Prompt + 权重逻辑 | `data/repository/MemoRepositoryImpl.kt` -> `buildSystemPrompt()` |
| 五级 Priority 枚举 | `domain/model/Memo.kt` -> `enum class Priority` |
| API 请求/响应模型 | `data/remote/dto/DeepSeekModels.kt` |
| Retrofit 接口 | `data/remote/DeepSeekApi.kt` |
| AI 解析调用 | `data/repository/MemoRepositoryImpl.kt` -> `parseTextWithAI()` |
| 优先级 UI 展示 | `ui/component/MemoCard.kt` -> `PriorityChip()` / `priorityDisplay()` |
| 环境隔离配置 | `app/build.gradle.kts` -> `productFlavors` |
