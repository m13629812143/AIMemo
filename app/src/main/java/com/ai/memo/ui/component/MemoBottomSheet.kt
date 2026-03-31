package com.ai.memo.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ai.memo.domain.model.Memo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoBottomSheet(
    memo: Memo,
    onDismiss: () -> Unit,
    onAddToCalendar: (Memo) -> Unit,
    onEdit: (Memo) -> Unit,
    onShare: (Memo) -> Unit,
    onDelete: (Memo) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = memo.event,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 操作选项
            BottomSheetItem(
                icon = Icons.Outlined.CalendarMonth,
                label = "添加到日历提醒",
                onClick = { onAddToCalendar(memo); onDismiss() }
            )
            BottomSheetItem(
                icon = Icons.Outlined.Edit,
                label = "编辑",
                onClick = { onEdit(memo); onDismiss() }
            )
            BottomSheetItem(
                icon = Icons.Outlined.Share,
                label = "分享",
                onClick = { onShare(memo); onDismiss() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            BottomSheetItem(
                icon = Icons.Outlined.Delete,
                label = "删除",
                tint = MaterialTheme.colorScheme.error,
                onClick = { onDelete(memo); onDismiss() }
            )
        }
    }
}

@Composable
private fun BottomSheetItem(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}
