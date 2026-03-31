package com.ai.memo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== Request ====================

@Serializable
data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<ChatMessage>,
    @SerialName("response_format")
    val responseFormat: ResponseFormat = ResponseFormat(),
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
    val temperature: Double = 0.1,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

// ==================== Response ====================

@Serializable
data class ChatResponse(
    val id: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    @SerialName("finish_reason")
    val finishReason: String? = null,
    val message: ChatMessage? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

// ==================== Claude Request/Response ====================

@Serializable
data class ClaudeRequest(
    val model: String,
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
    val system: String = "",
    val messages: List<Message>,
    val temperature: Double = 0.1
) {
    @Serializable
    data class Message(
        val role: String,
        val content: String
    )
}

@Serializable
data class ClaudeResponse(
    val id: String = "",
    val type: String = "",
    val role: String = "",
    val content: List<ContentBlock> = emptyList(),
    @SerialName("stop_reason")
    val stopReason: String? = null
) {
    @Serializable
    data class ContentBlock(
        val type: String = "text",
        val text: String = ""
    )
}

// ==================== AI 解析结果 ====================

@Serializable
data class MemoParseResult(
    val time: String = "",
    val location: String = "",
    val event: String = "",
    val priority: String = "medium",
    val remark: String = ""
)
