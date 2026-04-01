package com.ai.memo.ui.screen.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.memo.domain.model.Memo
import com.ai.memo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MemoListViewModel(
    private val repository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemoListUiState>(MemoListUiState.Loading)
    val uiState: StateFlow<MemoListUiState> = _uiState.asStateFlow()

    // 一次性事件通知（删除失败等）
    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    init {
        loadMemos()
    }

    private fun loadMemos() {
        viewModelScope.launch {
            repository.getAllMemos()
                .catch { e ->
                    _uiState.value = MemoListUiState.Error(e.message ?: "未知错误")
                }
                .collect { memos ->
                    _uiState.value = if (memos.isEmpty()) {
                        MemoListUiState.Empty
                    } else {
                        MemoListUiState.Success(memos)
                    }
                }
        }
    }

    fun deleteMemo(memo: Memo) {
        viewModelScope.launch {
            try {
                repository.deleteMemo(memo)
            } catch (e: Exception) {
                _errorEvent.value = e.message ?: "删除失败"
            }
        }
    }

    fun clearError() {
        _errorEvent.value = null
    }
}

sealed interface MemoListUiState {
    data object Loading : MemoListUiState
    data object Empty : MemoListUiState
    data class Success(val memos: List<Memo>) : MemoListUiState
    data class Error(val message: String) : MemoListUiState
}
