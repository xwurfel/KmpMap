package com.jetbrains.kmpapp.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.location.repository.LocationRepository
import com.jetbrains.kmpapp.location.service.DirectionsService
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest
import com.jetbrains.kmpapp.point_of_interest.repository.PoiRepository
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.location.LOCATION
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MapViewModel(
    private val poiRepository: PoiRepository,
    private val locationRepository: LocationRepository,
    private val directionsService: DirectionsService,
    val permissionsController: PermissionsController
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        loadPois()
        checkLocationPermission()
    }

    private fun loadPois() {
        viewModelScope.launch {
            poiRepository.getAllPois()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
                .collect { pois ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        pois = pois,
                        errorMessage = null
                    )
                }
        }
    }

    private fun checkLocationPermission() {
        viewModelScope.launch {
            try {
                val permissionState = permissionsController.getPermissionState(Permission.LOCATION)
                val hasPermission = permissionState == PermissionState.Granted

                _uiState.value = _uiState.value.copy(
                    hasLocationPermission = hasPermission,
                    locationPermissionState = permissionState
                )

                if (hasPermission) {
                    getCurrentLocation()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to check location permission: ${e.message}"
                )
            }
        }
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            try {
                permissionsController.providePermission(Permission.LOCATION)

                _uiState.value = _uiState.value.copy(
                    hasLocationPermission = true,
                    locationPermissionState = PermissionState.Granted,
                    errorMessage = null
                )

                getCurrentLocation()
            } catch (deniedAlways: DeniedAlwaysException) {
                _uiState.value = _uiState.value.copy(
                    hasLocationPermission = false,
                    locationPermissionState = PermissionState.DeniedAlways,
                    errorMessage = "Location permission is permanently denied. Please enable it in settings."
                )
            } catch (denied: DeniedException) {
                _uiState.value = _uiState.value.copy(
                    hasLocationPermission = false,
                    locationPermissionState = PermissionState.Denied,
                    errorMessage = "Location permission was denied."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to request location permission: ${e.message}"
                )
            }
        }
    }

    fun openAppSettings() {
        permissionsController.openAppSettings()
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                val permissionState = permissionsController.getPermissionState(Permission.LOCATION)
                if (permissionState != PermissionState.Granted) {
                    requestLocationPermission()
                    return@launch
                }

                val location = locationRepository.getCurrentLocation()
                _uiState.value = _uiState.value.copy(currentLocation = location)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to get current location: ${e.message}"
                )
            }
        }
    }

    fun showRouteTo(destination: Location) {
        val currentLocation = _uiState.value.currentLocation
        if (currentLocation != null) {
            _uiState.value = _uiState.value.copy(
                isLoadingRoute = true,
                errorMessage = null
            )

            viewModelScope.launch {
                try {
                    val routePoints = directionsService.getRoute(currentLocation, destination)

                    _uiState.value = _uiState.value.copy(
                        showRouteToLocation = destination,
                        routePoints = routePoints,
                        isLoadingRoute = false
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingRoute = false,
                        errorMessage = "Failed to calculate route: ${e.message}"
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Current location not available for route calculation"
            )
        }
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            showRouteToLocation = null,
            routePoints = emptyList()
        )
    }

    fun onMapLoaded() {
        _uiState.value = _uiState.value.copy(isMapLoaded = true)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    data class MapUiState(
        val isLoading: Boolean = true,
        val isMapLoaded: Boolean = false,
        val isLoadingRoute: Boolean = false,
        val hasLocationPermission: Boolean = false,
        val locationPermissionState: PermissionState = PermissionState.NotDetermined,
        val currentLocation: Location? = null,
        val pois: List<PointOfInterest> = emptyList(),
        val showRouteToLocation: Location? = null,
        val routePoints: List<Location> = emptyList(),
        val errorMessage: String? = null
    )
}