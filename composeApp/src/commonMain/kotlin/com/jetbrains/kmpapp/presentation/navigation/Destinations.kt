package com.jetbrains.kmpapp.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destinations {
    @Serializable
    data object Map : Destinations

    @Serializable
    data class AddPoi(
        val latitude: Double,
        val longitude: Double
    ) : Destinations

    @Serializable
    data class PoiDetails(
        val poiId: Long
    ) : Destinations
}