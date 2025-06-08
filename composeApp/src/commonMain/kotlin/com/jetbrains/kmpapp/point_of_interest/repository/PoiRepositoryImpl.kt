package com.jetbrains.kmpapp.point_of_interest.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest
import org.ilnytskyi.mappincmp.data.database.MapPinDatabase

class PoiRepositoryImpl(
    private val database: MapPinDatabase
) : PoiRepository {

    override suspend fun savePoi(poi: PointOfInterest): Long = withContext(Dispatchers.IO) {
        database.pointOfInterestQueries.insertPoi(
            title = poi.title,
            description = poi.description,
            imagePath = poi.imagePath,
            latitude = poi.location.latitude,
            longitude = poi.location.longitude,
            createdAt = poi.createdAt.toEpochMilliseconds()
        )
        database.pointOfInterestQueries.getLastInsertRowId().executeAsOne()
    }

    override fun getAllPois(): Flow<List<PointOfInterest>> {
        return database.pointOfInterestQueries.selectAllPois()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entities ->
                entities.map { entity ->
                    PointOfInterest(
                        id = entity.id,
                        title = entity.title,
                        description = entity.description,
                        imagePath = entity.imagePath,
                        location = Location(entity.latitude, entity.longitude),
                        createdAt = Instant.fromEpochMilliseconds(entity.createdAt)
                    )
                }
            }
    }

    override suspend fun getPoiById(id: Long): PointOfInterest? = withContext(Dispatchers.IO) {
        database.pointOfInterestQueries.selectPoiById(id).executeAsOneOrNull()?.let { entity ->
            PointOfInterest(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                imagePath = entity.imagePath,
                location = Location(entity.latitude, entity.longitude),
                createdAt = Instant.fromEpochMilliseconds(entity.createdAt)
            )
        }
    }

    override suspend fun deletePoi(id: Long) = withContext(Dispatchers.IO) {
        database.pointOfInterestQueries.deletePoi(id)
    }

    override suspend fun updatePoi(poi: PointOfInterest) = withContext(Dispatchers.IO) {
        database.pointOfInterestQueries.updatePoi(
            title = poi.title,
            description = poi.description,
            imagePath = poi.imagePath,
            latitude = poi.location.latitude,
            longitude = poi.location.longitude,
            id = poi.id
        )
    }

    override fun getPoiByLocation(latitude: Double, longitude: Double): Flow<PointOfInterest?> {
        return database.pointOfInterestQueries.selectPoiByLocation(latitude, longitude)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { entity ->
                entity?.let {
                    PointOfInterest(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        imagePath = it.imagePath,
                        location = Location(it.latitude, it.longitude),
                        createdAt = Instant.fromEpochMilliseconds(it.createdAt)
                    )
                }
            }
    }
}