package com.jetbrains.kmpapp.point_of_interest.repository

import kotlinx.coroutines.flow.Flow
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest

interface PoiRepository {
    suspend fun savePoi(poi: PointOfInterest): Long
    fun getAllPois(): Flow<List<PointOfInterest>>
    suspend fun getPoiById(id: Long): PointOfInterest?
    suspend fun deletePoi(id: Long)
    suspend fun updatePoi(poi: PointOfInterest)
    fun getPoiByLocation(latitude: Double, longitude: Double): Flow<PointOfInterest?>
}