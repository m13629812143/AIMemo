package com.ai.memo.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.memo.data.local.AiProvider
import com.ai.memo.data.local.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _selectedProviderId = MutableStateFlow(secureStorage.getSelectedProvider())
    val selectedProviderId: StateFlow<String> = _selectedProviderId.asStateFlow()

    // 每个 provider 的 API Key 缓存（避免在 Composable 中直接读加密存储）
    private val _apiKeys = MutableStateFlow<Map<String, String>>(emptyMap())
    val apiKeys: StateFlow<Map<String, String>> = _apiKeys.asStateFlow()

    // 一次性事件
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        loadApiKeys()
    }

    private fun loadApiKeys() {
        viewModelScope.launch(Dispatchers.IO) {
            val keys = AiProvider.entries.associate { provider ->
                provider.id to secureStorage.getApiKey(provider.id)
            }
            _apiKeys.value = keys
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            secureStorage.saveSelectedProvider(providerId)
            _selectedProviderId.value = providerId
        }
    }

    fun saveApiKey(providerId: String, apiKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            secureStorage.saveApiKey(providerId, apiKey)
            _apiKeys.value = _apiKeys.value.toMutableMap().apply {
                put(providerId, apiKey)
            }
            val displayName = AiProvider.fromId(providerId).displayName
            _snackbarMessage.value = "$displayName API Key 已保存"
        }
    }

    fun getApiKey(providerId: String): String {
        return _apiKeys.value[providerId] ?: ""
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
