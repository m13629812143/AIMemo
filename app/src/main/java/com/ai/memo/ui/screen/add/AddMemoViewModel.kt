package com.ai.memo.ui.screen.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.memo.domain.model.Memo
import com.ai.memo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddMemoViewModel(
    private val repository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddMemoUiState>(AddMemoUiState.Idle)
    val uiState: StateFlow<AddMemoUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    /** 调用 AI 解析用户输入 */
    fun parseWithAI() {
        val text = _inputText.value.trim()
        if (text.isBlank()) {
            _uiState.value = AddMemoUiState.Error("请输入内容")
            return
        }

        _uiState.value = AddMemoUiState.Parsing

        viewModelScope.launch {
            try {
                val memo = repository.parseTextWithAI(text)
                _uiState.value = AddMemoUiState.Parsed(memo)
            } catch (e: Exception) {
                _uiState.value = AddMemoUiState.Error(
                    e.message ?: "AI 解析失败，请重试"
                )
            }
        }
    }

    /** 保存解析后的备忘录 */
    fun saveMemo(memo: Memo) {
        viewModelScope.launch {
            try {
                repository.saveMemo(memo)
                _uiState.value = AddMemoUiState.Saved
            } catch (e: Exception) {
                _uiState.value = AddMemoUiState.Error(
                    e.message ?: "保存失败"
                )
            }
        }
    }

    /** 重置状态 */
    fun resetState() {
        _uiState.value = AddMemoUiState.Idle
        _inputText.value = ""
    }
}

sealed interface AddMemoUiState {
    data object Idle : AddMemoUiState
    data object Parsing : AddMemoUiState
    data class Parsed(val memo: Memo) : AddMemoUiState
    data class Error(val message: String) : AddMemoUiState
    data object Saved : AddMemoUiState
}
