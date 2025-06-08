package com.jetbrains.kmpapp.di

import com.jetbrains.kmpapp.data.database.DriverFactory
import com.jetbrains.kmpapp.point_of_interest.repository.PoiRepository
import com.jetbrains.kmpapp.point_of_interest.repository.PoiRepositoryImpl
import com.jetbrains.kmpapp.point_of_interest.repository.FileRepository
import com.jetbrains.kmpapp.point_of_interest.repository.FileRepositoryImpl
import org.ilnytskyi.mappincmp.data.database.MapPinDatabase
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.Module
import com.jetbrains.kmpapp.presentation.map.MapViewModel
import com.jetbrains.kmpapp.presentation.poi_details.PoiDetailsViewModel
import com.jetbrains.kmpapp.presentation.add_poi.AddPoiViewModel
import org.koin.dsl.module

expect val platformModule: Module

val dataModule = module {
    single { get<DriverFactory>().getDriver() }
    single { MapPinDatabase(get()) }

    single<PoiRepository> { PoiRepositoryImpl(get()) }
    single<FileRepository> { FileRepositoryImpl() }
}

val viewModelModule = module {
    factoryOf(::MapViewModel)
    factoryOf(::PoiDetailsViewModel)
    factoryOf(::AddPoiViewModel)
}

fun initKoin() {
    startKoin {
        modules(
            dataModule,
            viewModelModule,
            platformModule,
        )
    }
}

