package com.ai.memo.ui.screen.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.memo.domain.model.Memo
import com.ai.memo.domain.model.Priority
import com.ai.memo.domain.repository.MemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemoDetailViewModel(
    private val memoId: Long,
    private val repository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemoDetailUiState>(MemoDetailUiState.Loading)
    val uiState: StateFlow<MemoDetailUiState> = _uiState.asStateFlow()

    // 编辑状态
    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing.asStateFlow()

    private val _editEvent = MutableStateFlow("")
    val editEvent: StateFlow<String> = _editEvent.asStateFlow()

    private val _editTime = MutableStateFlow("")
    val editTime: StateFlow<String> = _editTime.asStateFlow()

    private val _editLocation = MutableStateFlow("")
    val editLocation: StateFlow<String> = _editLocation.asStateFlow()

    private val _editRemark = MutableStateFlow("")
    val editRemark: StateFlow<String> = _editRemark.asStateFlow()

    private val _editPriority = MutableStateFlow(Priority.MEDIUM)
    val editPriority: StateFlow<Priority> = _editPriority.asStateFlow()

    init {
        loadMemo()
    }

    private fun loadMemo() {
        viewModelScope.launch {
            try {
                val memo = repository.getMemoById(memoId)
                if (memo != null) {
                    _uiState.value = MemoDetailUiState.Success(memo)
                    // 初始化编辑字段
                    _editEvent.value = memo.event
                    _editTime.value = memo.time
                    _editLocation.value = memo.location
                    _editRemark.value = memo.remark
                    _editPriority.value = memo.priority
                } else {
                    _uiState.value = MemoDetailUiState.Error("备忘录不存在")
                }
            } catch (e: Exception) {
                _uiState.value = MemoDetailUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
        // 恢复原始值
        val state = _uiState.value
        if (state is MemoDetailUiState.Success) {
            _editEvent.value = state.memo.event
            _editTime.value = state.memo.time
            _editLocation.value = state.memo.location
            _editRemark.value = state.memo.remark
            _editPriority.value = state.memo.priority
        }
    }

    fun updateEditEvent(value: String) { _editEvent.value = value }
    fun updateEditTime(value: String) { _editTime.value = value }
    fun updateEditLocation(value: String) { _editLocation.value = value }
    fun updateEditRemark(value: String) { _editRemark.value = value }
    fun updateEditPriority(value: Priority) { _editPriority.value = value }

    fun saveEdit() {
        val state = _uiState.value
        if (state !is MemoDetailUiState.Success) return

        val updatedMemo = state.memo.copy(
            event = _editEvent.value,
            time = _editTime.value,
            location = _editLocation.value,
            remark = _editRemark.value,
            priority = _editPriority.value
        )

        viewModelScope.launch {
            try {
                repository.updateMemo(updatedMemo)
                _uiState.value = MemoDetailUiState.Success(updatedMemo)
                _isEditing.value = false
            } catch (e: Exception) {
                _uiState.value = MemoDetailUiState.Error(e.message ?: "保存失败")
            }
        }
    }

    fun deleteMemo(onDeleted: () -> Unit) {
        val state = _uiState.value
        if (state !is MemoDetailUiState.Success) return

        viewModelScope.launch {
            try {
                repository.deleteMemo(state.memo)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = MemoDetailUiState.Error(e.message ?: "删除失败")
            }
        }
    }
}

sealed interface MemoDetailUiState {
    data object Loading : MemoDetailUiState
    data class Success(val memo: Memo) : MemoDetailUiState
    data class Error(val message: String) : MemoDetailUiState
}
