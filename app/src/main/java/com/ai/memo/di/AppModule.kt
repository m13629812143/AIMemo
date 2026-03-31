package com.ai.memo.di

import androidx.room.Room
import com.ai.memo.BuildConfig
import com.ai.memo.data.local.MemoDatabase
import com.ai.memo.data.local.SecureStorage
import com.ai.memo.data.remote.provider.AiProviderFactory
import com.ai.memo.data.repository.MemoRepositoryImpl
import com.ai.memo.domain.repository.MemoRepository
import com.ai.memo.ui.screen.add.AddMemoViewModel
import com.ai.memo.ui.screen.detail.MemoDetailViewModel
import com.ai.memo.ui.screen.list.MemoListViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {

    // ==================== Secure Storage ====================
    single { SecureStorage(androidContext()) }

    // ==================== Database ====================
    single {
        Room.databaseBuilder(
            androidContext(),
            MemoDatabase::class.java,
            "memo_database"
        ).build()
    }

    single { get<MemoDatabase>().memoDao() }

    // ==================== Network ====================
    single {
        val logging = HttpLoggingInterceptor().apply {
            // 根据 Build Variant 控制日志级别
            level = if (BuildConfig.ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // AI Provider Factory - 支持多模型切换
    single { AiProviderFactory(get()) }

    // ==================== Repository ====================
    single<MemoRepository> {
        MemoRepositoryImpl(
            memoDao = get(),
            secureStorage = get(),
            aiProviderFactory = get()
        )
    }

    // ==================== ViewModels ====================
    viewModel { MemoListViewModel(get()) }
    viewModel { AddMemoViewModel(get()) }
    viewModel { params -> MemoDetailViewModel(params.get(), get()) }
}
