package com.jetbrains.kmpapp

import android.app.Application
import com.jetbrains.kmpapp.di.dataModule
import com.jetbrains.kmpapp.di.platformModule
import com.jetbrains.kmpapp.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class KMPMapApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KMPMapApp)
            androidLogger()
            modules(
                dataModule,
                viewModelModule,
                platformModule,
            )
        }
    }
}
