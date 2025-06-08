package com.jetbrains.kmpapp.location.repository

import com.jetbrains.kmpapp.location.model.Location

expect class LocationRepository {
    suspend fun getCurrentLocation(): Location?
}