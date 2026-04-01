package com.ai.memo.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 安全存储 - 使用 EncryptedSharedPreferences
 *
 * API Key 使用 AES-256-GCM 加密存储在本地：
 * - 密钥由 Android Keystore 管理，无法导出
 * - 数据仅在当前设备可解密
 * - 其他 App 无法访问（Android 沙箱机制）
 * - 卸载 App 后数据自动销毁
 */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "ai_memo_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_SELECTED_PROVIDER = "selected_provider"
    }

    /** 存储指定 AI 提供商的 API Key */
    fun saveApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString("$KEY_API_KEY_PREFIX$providerId", apiKey).apply()
    }

    /** 读取指定 AI 提供商的 API Key */
    fun getApiKey(providerId: String): String {
        return prefs.getString("$KEY_API_KEY_PREFIX$providerId", "") ?: ""
    }

    /** 删除指定 AI 提供商的 API Key */
    fun removeApiKey(providerId: String) {
        prefs.edit().remove("$KEY_API_KEY_PREFIX$providerId").apply()
    }

    /** 检查指定 AI 提供商是否已配置 API Key */
    fun hasApiKey(providerId: String): Boolean {
        return getApiKey(providerId).isNotBlank()
    }

    /** 保存当前选择的 AI 提供商 */
    fun saveSelectedProvider(providerId: String) {
        prefs.edit().putString(KEY_SELECTED_PROVIDER, providerId).apply()
    }

    /** 获取当前选择的 AI 提供商 */
    fun getSelectedProvider(): String {
        return prefs.getString(KEY_SELECTED_PROVIDER, AiProvider.DEEP_SEEK.id) ?: AiProvider.DEEP_SEEK.id
    }
}

/**
 * 支持的 AI 提供商枚举
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val model: String,
    val description: String
) {
    DEEP_SEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com/",
        model = "deepseek-chat",
        description = "DeepSeek-V3，中文能力强，性价比高"
    ),
    CLAUDE(
        id = "claude",
        displayName = "Claude",
        baseUrl = "https://api.anthropic.com/",
        model = "claude-sonnet-4-20250514",
        description = "Anthropic Claude Sonnet 4，推理能力强"
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/",
        model = "gpt-4o-mini",
        description = "OpenAI GPT-4o-mini，通用能力强"
    );

    companion object {
        fun fromId(id: String): AiProvider {
            return entries.find { it.id == id } ?: DEEP_SEEK
        }
    }
}
