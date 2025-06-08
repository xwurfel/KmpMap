package com.jetbrains.kmpapp.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.presentation.common.LoadingOverlay
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapScreen(
    onNavigateToAddPoi: (Location) -> Unit,
    onNavigateToPoiDetails: (Long) -> Unit,
    routeToLocation: Location? = null,
    viewModel: MapViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BindEffect(viewModel.permissionsController)

    LaunchedEffect(routeToLocation) {
        routeToLocation?.let { destination ->
            viewModel.showRouteTo(destination)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (uiState.locationPermissionState == PermissionState.DeniedAlways) "Settings" else null,
                duration = SnackbarDuration.Long
            )

            if (result == SnackbarResult.ActionPerformed &&
                uiState.locationPermissionState == PermissionState.DeniedAlways
            ) {
                viewModel.openAppSettings()
            }

            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (uiState.showRouteToLocation != null) {
                    SmallFloatingActionButton(
                        onClick = { viewModel.clearRoute() },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear Route",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        when (uiState.locationPermissionState) {
                            PermissionState.Granted -> viewModel.getCurrentLocation()

                            PermissionState.NotGranted,
                            PermissionState.NotDetermined,
                            PermissionState.Denied -> viewModel.requestLocationPermission()

                            PermissionState.DeniedAlways -> viewModel.openAppSettings()
                        }
                    },
                    containerColor = when (uiState.locationPermissionState) {
                        PermissionState.Granted -> MaterialTheme.colorScheme.primary
                        PermissionState.DeniedAlways -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                ) {
                    when (uiState.locationPermissionState) {
                        PermissionState.DeniedAlways -> Icon(
                            Icons.Default.Settings,
                            contentDescription = "Open Settings",
                            modifier = Modifier.size(24.dp)
                        )

                        else -> Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Current Location",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MapComponent(
                modifier = Modifier.fillMaxSize(),
                userLocation = uiState.currentLocation,
                pois = uiState.pois,
                onMapLongClick = onNavigateToAddPoi,
                onPoiClick = { poi -> onNavigateToPoiDetails(poi.id) },
                onMapLoaded = { viewModel.onMapLoaded() },
                showRouteToLocation = uiState.showRouteToLocation,
                routePoints = uiState.routePoints
            )

            if (!uiState.hasLocationPermission && uiState.locationPermissionState == PermissionState.NotDetermined) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Location access is needed to show your current position and provide location-based features.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.requestLocationPermission() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                LoadingOverlay(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}