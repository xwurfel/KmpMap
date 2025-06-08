package com.jetbrains.kmpapp.point_of_interest.model

import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import com.jetbrains.kmpapp.location.model.Location

data class PointOfInterest(
    val id: Long = 0,
    val title: String,
    val description: String,
    val imagePath: String?,
    val location: Location,
    val createdAt: Instant = System.now()
)