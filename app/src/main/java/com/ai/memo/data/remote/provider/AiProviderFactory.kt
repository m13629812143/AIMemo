package com.ai.memo.data.remote.provider

import com.ai.memo.data.local.AiProvider
import com.ai.memo.data.remote.dto.ChatMessage
import com.ai.memo.data.remote.dto.ChatRequest
import com.ai.memo.data.remote.dto.ChatResponse
import com.ai.memo.data.remote.dto.Choice
import com.ai.memo.data.remote.dto.ClaudeRequest
import com.ai.memo.data.remote.dto.ClaudeResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * AI 提供商工厂 - 根据用户选择动态创建对应的 API 客户端
 *
 * 统一输出为 ChatResponse 格式，屏蔽不同 API 的差异
 */
class AiProviderFactory(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true   // 确保 max_tokens 等带默认值的字段也会被序列化
    }

    /**
     * 调用 AI 接口，统一返回 ChatResponse
     */
    suspend fun chat(
        provider: AiProvider,
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): ChatResponse {
        return try {
            when (provider) {
                AiProvider.DEEP_SEEK -> callDeepSeek(apiKey, systemPrompt, userMessage)
                AiProvider.CLAUDE -> callClaude(provider, apiKey, systemPrompt, userMessage)
                AiProvider.OPENAI -> callOpenAI(apiKey, systemPrompt, userMessage)
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string() ?: "无响应体"
            throw IllegalStateException(
                "${provider.displayName} API 错误 (HTTP ${e.code()}): $errorBody"
            )
        }
    }

    // ==================== DeepSeek ====================
    private suspend fun callDeepSeek(
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): ChatResponse {
        val api = createRetrofit(AiProvider.DEEP_SEEK.baseUrl).create(DeepSeekEndpoint::class.java)
        return api.chatCompletion(
            authorization = "Bearer $apiKey",
            request = ChatRequest(
                model = AiProvider.DEEP_SEEK.model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userMessage)
                )
            )
        )
    }

    // ==================== Claude ====================
    // Claude API 格式与 OpenAI 完全不同：
    // - system prompt 是顶层字段，不在 messages 里
    // - 没有 response_format 参数
    // - 认证用 x-api-key header，不是 Bearer token
    private suspend fun callClaude(
        provider: AiProvider,
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): ChatResponse {
        val api = createRetrofit(provider.baseUrl).create(ClaudeEndpoint::class.java)
        val response = api.createMessage(
            apiKey = apiKey,
            request = ClaudeRequest(
                model = provider.model,
                maxTokens = 1024,
                system = systemPrompt,
                messages = listOf(
                    ClaudeRequest.Message(role = "user", content = userMessage)
                )
            )
        )
        // 将 Claude 响应转换为统一的 ChatResponse 格式
        return ChatResponse(
            id = response.id,
            choices = listOf(
                Choice(
                    index = 0,
                    finishReason = response.stopReason ?: "stop",
                    message = ChatMessage(
                        role = "assistant",
                        content = response.content.firstOrNull()?.text ?: ""
                    )
                )
            )
        )
    }

    // ==================== OpenAI ====================
    private suspend fun callOpenAI(
        apiKey: String,
        systemPrompt: String,
        userMessage: String
    ): ChatResponse {
        val api = createRetrofit(AiProvider.OPENAI.baseUrl).create(OpenAIEndpoint::class.java)
        return api.chatCompletion(
            authorization = "Bearer $apiKey",
            request = ChatRequest(
                model = AiProvider.OPENAI.model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userMessage)
                )
            )
        )
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}

// ==================== API 端点定义 ====================

interface DeepSeekEndpoint {
    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}

interface OpenAIEndpoint {
    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse
}

interface ClaudeEndpoint {
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: ClaudeRequest
    ): ClaudeResponse
}
