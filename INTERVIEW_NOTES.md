# AI Memo - 面试问答笔记

## 一、开发过程中使用的关键 Prompt

### 1.1 项目初始化阶段

**Prompt 1 - 项目架构搭建**
```
创建一个 Android 项目 AI Memo，技术栈：
- Kotlin + Jetpack Compose + Material 3
- MVVM 架构（ViewModel + Repository）
- Room Database 持久化
- Retrofit + kotlinx-serialization 网络请求
- Koin 依赖注入
- Kotlin Coroutines 异步处理

项目结构按 Clean Architecture 分层：data / domain / ui / di
包名 com.ai.memo，compileSdk 35，minSdk 26
```

**为什么这样写**：一次性把技术栈和架构约束全部明确，避免 AI 自由发挥导致后续返工。明确 Clean Architecture 分层，让生成的代码具备可维护性。

---

**Prompt 2 - 数据模型设计**
```
设计 Room Entity 和 Domain Model：
- MemoEntity（数据库层）：id, rawText, event, time, location, priority(String), remark, createdAt
- Memo（领域层）：同上但 priority 为 Priority 枚举
- Priority 枚举五级：CRITICAL/HIGH/MEDIUM/LOW/MINIMAL，带 fromString() 兼容中英文

需要 Entity <-> Domain 双向转换方法
```

**为什么这样写**：明确两层模型的差异（Entity 存 String，Domain 用枚举），要求双向转换，这是 Clean Architecture 的核心 — 数据层和领域层解耦。

---

### 1.2 AI 集成阶段

**Prompt 3 - 多 AI 提供商工厂**
```
实现 AiProviderFactory，支持 DeepSeek / Claude / OpenAI 三个提供商：
- 用枚举定义每个提供商的 baseUrl、model、displayName
- 统一输出为 ChatResponse 格式，屏蔽 API 差异
- Claude 的 system prompt 是顶层字段，不在 messages 里
- Claude 认证用 x-api-key，不是 Bearer token
- DeepSeek/OpenAI 支持 response_format: json_object，Claude 不支持

Retrofit 实例需要缓存，不要每次调用都创建
```

**为什么这样写**：把三个 API 的关键差异点列出来，让 AI 生成的代码一次性处理正确。特别是 Claude 的差异（认证方式、prompt 位置、无 JSON mode），这些坑不提前说明会反复出错。

---

**Prompt 4 - System Prompt 设计（App 内调用 AI 的 Prompt）**
```
设计 AI 日程提取的 System Prompt，要求：
1. 动态注入当前日期时间，让 AI 能计算"明天""下周二"等相对时间
2. 严格约束 JSON 输出格式：time/location/event/priority/remark
3. 时间格式固定 yyyy.MM.dd HH:mm
4. 五级优先级用三维权重公式计算：
   加权总分 = time紧迫度*0.4 + location远近*0.2 + event性质*0.4
5. 备注中出现"非常重要""紧急"等关键词时，直接覆盖为 critical
6. 提供3个 Few-shot 示例覆盖：标准场景、低优先级、备注覆盖
7. 末尾强调"只返回JSON，不要其他文字"
```

**为什么这样写**：这是整个 App 的核心 Prompt。关键设计决策：
- **动态时间注入**解决了相对时间计算问题
- **权重公式**让优先级判断可解释、可预测，而不是 AI 随意猜测
- **备注覆盖**尊重用户主观判断
- **Few-shot 示例**通过实际输入输出对，约束 AI 行为

---

### 1.3 UI 阶段

**Prompt 5 - 主界面与交互**
```
实现以下 Compose 页面：
1. MemoListScreen：LargeTopAppBar + LazyColumn + FAB，点击卡片弹出 BottomSheet（添加到日历/编辑/分享/删除）
2. AddMemoScreen：输入框 + 语音输入按钮 + AI解析按钮 + 解析结果预览卡片 + 确认保存
3. MemoDetailScreen：查看详情 + 编辑功能（五级优先级 FilterChip 选择器）+ 删除确认弹窗
4. SettingsScreen：AI 模型选择（RadioButton）+ API Key 加密存储（PasswordVisualTransformation）

每个页面使用独立的 ViewModel，状态用 sealed interface 建模
```

**Prompt 6 - 日历 Intent 集成**
```
点击"添加到日历"时，用 Intent.ACTION_INSERT 打开系统日历：
- 将 memo.time 解析为毫秒时间戳
- 传入 TITLE/EVENT_LOCATION/DESCRIPTION/BEGIN_TIME/END_TIME
- 默认时长1小时
- 注意 Android 11+ 的包可见性限制，需要在 Manifest 加 <queries>
```

---

### 1.4 安全与工程化

**Prompt 7 - API Key 安全存储**
```
使用 EncryptedSharedPreferences 存储 API Key：
- MasterKey 使用 AES256_GCM
- 支持按 providerId 分别存储不同 AI 提供商的 Key
- 提供 save/get/remove/has 方法
- Key 加密方案 AES256_SIV，Value 加密方案 AES256_GCM
```

**Prompt 8 - CI/CD**
```
配置 GitHub Actions：
- 触发条件：push 到 main/master，PR 到 main/master
- JDK 17 + Gradle wrapper
- 构建 prod debug APK
- 上传 artifact 保留 30 天
- API Key 不需要在 CI 中注入（用户在 App 内设置）
```

---

## 二、JSON Schema 设计思路

### 2.1 最终 Schema

```json
{
  "time": "yyyy.MM.dd HH:mm 格式的时间字符串",
  "location": "地点字符串",
  "event": "事件名称",
  "priority": "critical | high | medium | low | minimal",
  "remark": "备注信息"
}
```

### 2.2 为什么选这五个字段

| 字段 | 设计考量 |
|------|---------|
| **event** | 核心字段，用户最关心"什么事" |
| **time** | 日程的基本要素，固定格式方便后续解析为时间戳（日历 Intent、排序） |
| **location** | 日程的基本要素，同时作为优先级权重维度之一（距离越远越需要提前准备） |
| **priority** | **五级**而非三级，增加区分度。用权重公式而非让 AI 随意判断，保证可解释性 |
| **remark** | 兜底字段，捕获 AI 无法归类到其他字段的附加信息（"带电脑""穿正装"等） |

### 2.3 关键设计决策

**决策 1：时间用字符串而非时间戳**

用 `"2026.04.07 10:00"` 而非 Unix 时间戳。原因：
- AI 生成时间戳容易出错（位数、精度问题）
- 字符串格式人类可读，方便 UI 直接展示
- 需要时间戳时在客户端用 `SimpleDateFormat` 解析即可

**决策 2：priority 用枚举字符串而非数字**

用 `"critical"` 而非 `5`。原因：
- 语义明确，AI 理解门槛更低
- 客户端用 `Priority.fromString()` 转换，支持中英文和数字多种格式兼容
- 即使 AI 返回意外值，默认 fallback 为 MEDIUM，不会崩溃

**决策 3：五级优先级 + 权重公式**

从三级升级为五级，并引入三维权重公式：

```
加权总分 = time紧迫度 × 0.4 + location远近 × 0.2 + event性质 × 0.4
```

原因：
- 三级（high/medium/low）无法区分"30分钟后的面试"和"下周的例会"，两者都会被判为 high
- 权重公式让优先级**可解释** — 面试时可以向面试官解释为什么一条记录是 critical 而不是 high
- 公式写在 Prompt 中让 AI **按规则执行**，而非依赖 AI 的"直觉"

**决策 4：备注覆盖机制**

当用户在文本中写了"非常重要""紧急"等词时，无论权重公式计算结果如何，priority 直接设为 critical。

原因：
- 用户的主观判断应优先于算法的客观计算
- 这是一个常见的产品设计模式（类似"置顶"功能）

### 2.4 容错设计

| 异常场景 | 处理方式 | 代码位置 |
|---------|---------|---------|
| AI 返回空内容 | 抛出异常，UI 显示提示 | `MemoRepositoryImpl.kt:193` |
| JSON 被 markdown 包裹 | `extractJson()` 用正则提取纯 JSON | `MemoRepositoryImpl.kt:237` |
| JSON 解析失败 | 捕获异常，显示原始响应便于调试 | `MemoRepositoryImpl.kt:201` |
| 字段缺失 | `MemoParseResult` 所有字段有默认值 | `DeepSeekModels.kt:95` |
| priority 值异常 | `Priority.fromString()` 默认返回 MEDIUM | `Memo.kt:41` |
| event 为空 | 兜底显示"未识别事件" | `MemoRepositoryImpl.kt:209` |
| time 为空 | 兜底显示"待定" | `MemoRepositoryImpl.kt:210` |
| location 为空 | 兜底显示"未指定" | `MemoRepositoryImpl.kt:211` |

### 2.5 Prompt 迭代过程

| 版本 | 内容 | 问题 |
|------|------|------|
| v1 | `"请提取日程信息，返回JSON"` | 格式不稳定，有时返回 markdown，优先级随意 |
| v2 | 加入 JSON Schema + 三级优先级 | 格式稳定了，但三级优先级区分度不够 |
| v3（最终版） | 五级优先级 + 三维权重公式 + 动态时间注入 + 3个 Few-shot + 备注覆盖 | 稳定可靠 |

核心教训：**Prompt 越具体，AI 输出越稳定**。把规则公式化、把格式模板化、把边界情况用示例覆盖，是让 AI 可靠工作的关键。

---

## 三、项目中遇到的问题与解决

| # | 问题 | 原因 | 解决方案 |
|---|------|------|---------|
| 1 | Android 11+ 点击"添加到日历"无反应 | 包可见性限制，`resolveActivity()` 返回 null | Manifest 添加 `<queries>` + 改用 try-catch |
| 2 | Claude API 返回 400 | 请求体缺少 `max_tokens`（Claude 必填） | JSON 序列化开启 `encodeDefaults = true` |
| 3 | Claude 返回 JSON 被 markdown 包裹 | Claude 不支持 `response_format: json_object` | 实现 `extractJson()` 用正则提取纯 JSON |
| 4 | 保存后偶发页面白屏 | `onNavigateBack()` 后 Composable 已移除，`resetState()` 时序错误 | 先 reset 再 navigate |
| 5 | Release APK 日志泄漏 | Koin 日志级别硬编码 DEBUG | 根据 `BuildConfig.ENABLE_LOGGING` 动态控制 |
| 6 | 每次 AI 调用网络延迟高 | Retrofit 实例每次重新创建 | 缓存 Retrofit 实例 |
| 7 | 数据库升级崩溃风险 | Room 无迁移策略 | 添加 `fallbackToDestructiveMigration()` |
| 8 | Settings 页面加密读写可能卡主线程 | 直接在 Composable 中调用 EncryptedSharedPreferences | 抽取 SettingsViewModel，操作移至 IO 调度器 |

---

## 四、技术亮点总结

1. **五级优先级权重体系** — 不是让 AI 随意判断，而是给出明确的三维权重公式，让结果可解释、可预测
2. **多 AI 提供商支持** — 工厂模式统一 DeepSeek/Claude/OpenAI 三种 API 差异，用户可在设置中切换
3. **API Key 安全** — EncryptedSharedPreferences (AES-256-GCM) 加密存储，不硬编码、不上传
4. **动态时间注入** — 每次调用将当前时间注入 System Prompt，解决"明天""下周二"等相对时间计算
5. **JSON 容错链** — 纯 JSON → markdown 代码块提取 → 首尾花括号提取，三级 fallback
6. **完整 MVVM** — 所有页面使用独立 ViewModel，状态用 sealed interface 建模，数据流单向
