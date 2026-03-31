package com.ai.memo.di

import androidx.room.Room
import com.ai.memo.BuildConfig
import com.ai.memo.data.local.MemoDatabase
import com.ai.memo.data.remote.DeepSeekApi
import com.ai.memo.data.repository.MemoRepositoryImpl
import com.ai.memo.domain.repository.MemoRepository
import com.ai.memo.ui.screen.add.AddMemoViewModel
import com.ai.memo.ui.screen.detail.MemoDetailViewModel
import com.ai.memo.ui.screen.list.MemoListViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

val appModule = module {

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
            // 根据 Build Variant 控制日志级别（环境隔离）
            level = if (BuildConfig.ENABLE_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)    // DeepSeek 可能较慢
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    single {
        val json = Json { ignoreUnknownKeys = true }
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(DeepSeekApi.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(DeepSeekApi::class.java)
    }

    // ==================== Repository ====================
    single<MemoRepository> {
        MemoRepositoryImpl(
            memoDao = get(),
            deepSeekApi = get()
        )
    }

    // ==================== ViewModels ====================
    viewModel { MemoListViewModel(get()) }
    viewModel { AddMemoViewModel(get()) }
    viewModel { params -> MemoDetailViewModel(params.get(), get()) }
}
