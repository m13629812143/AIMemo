package com.ai.memo.data.remote

import com.ai.memo.BuildConfig
import com.ai.memo.data.remote.dto.ChatRequest
import com.ai.memo.data.remote.dto.ChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * DeepSeek API 接口定义
 * BASE_URL 通过 BuildConfig 注入，支持多环境隔离
 */
interface DeepSeekApi {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequest
    ): ChatResponse

    companion object {
        val BASE_URL: String get() = BuildConfig.API_BASE_URL
    }
}
