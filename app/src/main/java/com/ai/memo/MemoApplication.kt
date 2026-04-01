package com.ai.memo

import android.app.Application
import com.ai.memo.BuildConfig
import com.ai.memo.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Release 构建不输出 Koin 日志
            androidLogger(if (BuildConfig.ENABLE_LOGGING) Level.DEBUG else Level.NONE)
            androidContext(this@MemoApplication)
            modules(appModule)
        }
    }
}
