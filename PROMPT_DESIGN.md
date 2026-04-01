# AI Memo - Prompt 逻辑文档

## 一、前期调研

项目启动前，通过 AI 并行调研了两个方向：一是 DeepSeek API 的能力边界，确认其支持 `response_format: json_object` 强制 JSON 输出、低温度（0.1）确定性提取、以及 `$0.28/M tokens` 的成本优势；二是 Jetpack Compose + Material 3 的最新依赖版本与 MVVM 架构最佳实践，确定了 Kotlin 2.1 + Compose BOM 2024.12 + Room 2.6 + Koin 3.5 的技术栈组合。调研结果直接决定了项目的技术选型和架构分层。

## 二、架构设计

基于调研结果，要求 AI 按 `data / domain / ui` 三层设计完整目录结构：data 层包含 Room 本地持久化和 Retrofit 远程调用；domain 层定义 Memo 领域模型和 Repository 接口；ui 层采用 Compose + ViewModel + StateFlow 的响应式架构。同时要求 AI 设计了 Koin 依赖注入模块，将 Database、API、Repository、ViewModel 的创建和生命周期统一管理。

## 三、JSON Schema 设计

要求 AI 返回固定五字段 JSON：`time`（格式 yyyy.MM.dd HH:mm，相对时间基于动态注入的当前日期计算）、`location`、`event`、`priority`（五级：critical/high/medium/low/minimal）、`remark`。优先级通过三维权重公式计算：`time紧迫度×0.4 + location远近×0.2 + event性质×0.4`，总分映射到五个等级。若备注含"非常重要""紧急"等关键词，直接覆盖为 critical。Prompt 中提供 3 个 Few-shot 示例覆盖高/低/覆盖场景。

## 四、Bug 修复过程

开发中遇到两个关键问题：一是 Claude API 返回 HTTP 400 `"max_tokens: Field required"`，根因是 `kotlinx.serialization` 默认不序列化有默认值的字段，修复为 `Json { encodeDefaults = true }`；二是 Claude 不支持 `response_format: json_object`，可能返回 markdown 包裹的 JSON，通过 `extractJson()` 方法实现三级容错提取（纯 JSON → 代码块提取 → 花括号定位）。此外还修复了启动器图标资源缺失导致的 AAPT 编译失败。
