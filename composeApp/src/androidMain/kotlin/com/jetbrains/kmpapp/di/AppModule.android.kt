package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.database.DriverFactory
import com.jetbrains.kmpapp.location.repository.LocationRepository
import dev.icerock.moko.permissions.PermissionsController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DriverFactory(androidContext()) }
    single { LocationRepository(androidContext()) }
    single { PermissionsController(androidContext()) }
}