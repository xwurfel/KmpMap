package com.jetbrains.kmpapp.presentation.map

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest

@OptIn(ExperimentalPermissionsApi::class, MapsComposeExperimentalApi::class)
@Composable
actual fun MapComponent(
    modifier: Modifier,
    userLocation: Location?,
    pois: List<PointOfInterest>,
    onMapLongClick: (Location) -> Unit,
    onPoiClick: (PointOfInterest) -> Unit,
    onMapLoaded: () -> Unit,
    showRouteToLocation: Location?,
    routePoints: List<Location>
) {
    val multiplePermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val cameraPositionState = rememberCameraPositionState()

    val mapProperties = remember(multiplePermissionState.allPermissionsGranted) {
        MapProperties(
            isMyLocationEnabled = multiplePermissionState.allPermissionsGranted,
            mapType = MapType.HYBRID
        )
    }

    LaunchedEffect(Unit) {
        multiplePermissionState.launchMultiplePermissionRequest()
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude), 15f
                )
            )
        }
    }

    LaunchedEffect(showRouteToLocation, userLocation) {
        if (showRouteToLocation != null && userLocation != null) {
            val bounds = LatLngBounds.builder()
                .include(LatLng(userLocation.latitude, userLocation.longitude))
                .include(LatLng(showRouteToLocation.latitude, showRouteToLocation.longitude))
                .build()

            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 100)
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        onMapLoaded = onMapLoaded,
        onMapLongClick = { latLng ->
            onMapLongClick(Location(latLng.latitude, latLng.longitude))
        },
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
    ) {
        Clustering(
            items = pois.map { poi -> PoiClusterItem(poi) },
            onClusterItemClick = { clusterItem ->
                onPoiClick(clusterItem.poi)
                false
            },
        )

        if (routePoints.isNotEmpty()) {
            Polyline(
                points = routePoints.map { LatLng(it.latitude, it.longitude) },
                color = Color.Blue,
                width = 8f
            )
        }

        showRouteToLocation?.let { destination ->
            Marker(
                state = rememberMarkerState(
                    position = LatLng(destination.latitude, destination.longitude)
                ),
                title = "Destination"
            )
        }
    }
}

private class PoiClusterItem(
    val poi: PointOfInterest
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(poi.location.latitude, poi.location.longitude)
    override fun getTitle(): String = poi.title
    override fun getSnippet(): String = poi.description
    override fun getZIndex(): Float? = null
}