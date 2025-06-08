package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.database.DriverFactory
import com.jetbrains.kmpapp.location.repository.LocationRepository
import dev.icerock.moko.permissions.ios.PermissionsController
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DriverFactory() }
    single { LocationRepository() }
    single { PermissionsController() }
}