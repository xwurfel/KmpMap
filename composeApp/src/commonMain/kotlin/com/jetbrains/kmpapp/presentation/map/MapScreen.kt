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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.presentation.common.LoadingOverlay
import com.jetbrains.kmpapp.presentation.navigation.RouteManager
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MapScreen(
    onNavigateToAddPoi: (Location) -> Unit,
    onNavigateToPoiDetails: (Long) -> Unit,
    viewModel: MapViewModel = koinViewModel(),
    routeManager: RouteManager = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingRouteDestination by routeManager.pendingRouteDestination.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BindEffect(viewModel.permissionsController)

    HandlePendingRoute(
        pendingDestination = pendingRouteDestination,
        onRouteSet = viewModel::setRouteDestination,
        onRouteClear = routeManager::clearPendingRoute
    )

    HandleErrorMessages(
        errorMessage = uiState.errorMessage,
        permissionState = uiState.locationPermissionState,
        snackbarHostState = snackbarHostState,
        onOpenSettings = viewModel::openAppSettings,
        onErrorCleared = viewModel::clearError
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            MapFloatingActions(
                uiState = uiState,
                onClearRoute = viewModel::clearRoute,
                onLocationAction = viewModel::handleLocationAction
            )
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
                onMapLoaded = viewModel::onMapLoaded,
                showRouteToLocation = uiState.showRouteToLocation,
                routePoints = uiState.routePoints
            )

            PermissionRequestOverlay(
                showOverlay = !uiState.hasLocationPermission &&
                        uiState.locationPermissionState == PermissionState.NotDetermined,
                onGrantPermission = viewModel::requestLocationPermission
            )

            LoadingStates(
                uiState = uiState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HandlePendingRoute(
    pendingDestination: Location?,
    onRouteSet: (Location?) -> Unit,
    onRouteClear: () -> Unit
) {
    LaunchedEffect(pendingDestination) {
        pendingDestination?.let { destination ->
            println("MapScreen: Detected pending route destination: $destination")
            onRouteSet(destination)
            onRouteClear()
        }
    }
}

@Composable
private fun HandleErrorMessages(
    errorMessage: String?,
    permissionState: PermissionState,
    snackbarHostState: SnackbarHostState,
    onOpenSettings: () -> Unit,
    onErrorCleared: () -> Unit
) {
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (permissionState == PermissionState.DeniedAlways) "Settings" else null,
                duration = SnackbarDuration.Long
            )

            if (result == SnackbarResult.ActionPerformed &&
                permissionState == PermissionState.DeniedAlways
            ) {
                onOpenSettings()
            }

            onErrorCleared()
        }
    }
}

@Composable
private fun MapFloatingActions(
    uiState: MapViewModel.MapUiState,
    onClearRoute: () -> Unit,
    onLocationAction: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        RouteActionButton(
            showButton = uiState.showRouteToLocation != null,
            onClearRoute = onClearRoute
        )

        LocationActionButton(
            permissionState = uiState.locationPermissionState,
            onLocationAction = onLocationAction
        )
    }
}

@Composable
private fun RouteActionButton(
    showButton: Boolean,
    onClearRoute: () -> Unit
) {
    if (showButton) {
        SmallFloatingActionButton(
            onClick = onClearRoute,
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Clear Route",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LocationActionButton(
    permissionState: PermissionState,
    onLocationAction: () -> Unit
) {
    FloatingActionButton(
        onClick = onLocationAction,
        containerColor = when (permissionState) {
            PermissionState.Granted -> MaterialTheme.colorScheme.primary
            PermissionState.DeniedAlways -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.secondary
        }
    ) {
        val (icon, contentDescription) = when (permissionState) {
            PermissionState.DeniedAlways -> Icons.Default.Settings to "Open Settings"
            else -> Icons.Default.MyLocation to "Current Location"
        }

        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PermissionRequestOverlay(
    showOverlay: Boolean,
    onGrantPermission: () -> Unit
) {
    if (showOverlay) {
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

                Button(
                    onClick = onGrantPermission,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun LoadingStates(
    uiState: MapViewModel.MapUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (uiState.isLoading) {
            LoadingOverlay(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (uiState.isLoadingRoute) {
            RouteLoadingIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun RouteLoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Calculating walking route...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}