package com.ai.memo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ai.memo.ui.screen.add.AddMemoScreen
import com.ai.memo.ui.screen.detail.MemoDetailScreen
import com.ai.memo.ui.screen.list.MemoListScreen

object Routes {
    const val MEMO_LIST = "memo_list"
    const val ADD_MEMO = "add_memo"
    const val MEMO_DETAIL = "memo_detail/{memoId}"

    fun memoDetail(memoId: Long) = "memo_detail/$memoId"
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MEMO_LIST
    ) {
        // 首页 - 备忘录列表
        composable(Routes.MEMO_LIST) {
            MemoListScreen(
                onNavigateToAdd = {
                    navController.navigate(Routes.ADD_MEMO)
                },
                onNavigateToDetail = { memoId ->
                    navController.navigate(Routes.memoDetail(memoId))
                }
            )
        }

        // 新建备忘
        composable(Routes.ADD_MEMO) {
            AddMemoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 备忘详情 / 编辑
        composable(
            route = Routes.MEMO_DETAIL,
            arguments = listOf(
                navArgument("memoId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val memoId = backStackEntry.arguments?.getLong("memoId") ?: return@composable
            MemoDetailScreen(
                memoId = memoId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
