# 代码审查 Prompt

## 使用的核心提示词

```
你的任务是作为一个监督者，监督验证代码的完整性，找出其逻辑bug，为我记录项目遇到的问题。
```

## 审查过程

通过该 Prompt 指导 AI 对已构建完成的 Android 项目进行全量代码审查，覆盖以下维度：

- **项目结构**：Gradle 配置、AndroidManifest、依赖版本兼容性
- **架构规范**：MVVM 数据流方向、ViewModel/Repository 职责划分
- **数据层**：Room Entity/DAO 定义、数据库迁移策略
- **网络层**：Retrofit 配置、AI API 集成、JSON 解析容错
- **UI 层**：Compose 状态管理、生命周期安全、重组优化
- **逻辑 Bug**：空指针、并发、内存泄漏、Android 平台兼容性

最终输出按 P0-P3 分级的问题清单，共发现 10 个问题并逐一修复。
