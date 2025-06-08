package com.jetbrains.kmpapp.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest

@Composable
expect fun MapComponent(
    modifier: Modifier,
    userLocation: Location?,
    pois: List<PointOfInterest>,
    onMapLongClick: (Location) -> Unit,
    onPoiClick: (PointOfInterest) -> Unit,
    onMapLoaded: () -> Unit,
    showRouteToLocation: Location?,
    routePoints: List<Location>
)