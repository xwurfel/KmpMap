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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
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
        initializeScreen()
    }

    private fun initializeScreen() {
        loadPois()
        checkLocationPermission()
    }

    fun handleLocationAction() {
        when (_uiState.value.locationPermissionState) {
            PermissionState.Granted -> getCurrentLocation()
            PermissionState.NotGranted, PermissionState.NotDetermined, PermissionState.Denied -> requestLocationPermission()

            PermissionState.DeniedAlways -> openAppSettings()
        }
    }

    fun requestLocationPermission() {
        viewModelScope.launch {
            try {
                permissionsController.providePermission(Permission.LOCATION)
                updatePermissionState(PermissionState.Granted, hasPermission = true)
                clearError()
                getCurrentLocation()
            } catch (e: Exception) {
                handlePermissionException(e)
            }
        }
    }

    fun openAppSettings() {
        permissionsController.openAppSettings()
    }

    private fun getCurrentLocation() {
        viewModelScope.launch {
            if (!verifyLocationPermission()) return@launch

            try {
                val location = locationRepository.getCurrentLocation()
                _uiState.update { it.copy(currentLocation = location) }
            } catch (e: Exception) {
                showError("Failed to get current location: ${e.message}")
            }
        }
    }

    fun setRouteDestination(location: Location?) {
        if (location != null) {
            showRouteTo(location)
        } else {
            clearRoute()
        }
    }

    private fun showRouteTo(destination: Location) {

        val currentLocation = _uiState.value.currentLocation
        if (currentLocation == null) {
            showError("Current location not available for route calculation")
            return
        }
        _uiState.update {
            it.copy(
                isLoadingRoute = true, errorMessage = null
            )
        }

        calculateRoute(currentLocation, destination)
    }

    fun clearRoute() {
        _uiState.update {
            it.copy(
                showRouteToLocation = null, routePoints = emptyList()
            )
        }
    }

    fun onMapLoaded() {
        _uiState.update { it.copy(isMapLoaded = true) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadPois() {
        viewModelScope.launch {
            poiRepository.getAllPois().catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false, errorMessage = error.message
                    )
                }
            }.collect { pois ->
                _uiState.update {
                    it.copy(
                        isLoading = false, pois = pois, errorMessage = null
                    )
                }
            }
        }
    }

    private fun checkLocationPermission() {
        viewModelScope.launch {
            try {
                val permissionState = permissionsController.getPermissionState(Permission.LOCATION)
                val hasPermission = permissionState == PermissionState.Granted

                updatePermissionState(permissionState, hasPermission)

                if (hasPermission) {
                    getCurrentLocation()
                }
            } catch (e: Exception) {
                showError("Failed to check location permission: ${e.message}")
            }
        }
    }

    private suspend fun verifyLocationPermission(): Boolean {
        val permissionState = runCatching {
            permissionsController.getPermissionState(Permission.LOCATION)
        }.getOrElse {
            showError("Failed to verify location permission")
            return false
        }

        if (permissionState != PermissionState.Granted) {
            requestLocationPermission()
            return false
        }
        return true
    }

    private fun calculateRoute(from: Location, to: Location) {
        CoroutineScope(SupervisorJob()).launch {
            try {
                val routePoints = directionsService.getRoute(from, to)
                _uiState.update {
                    it.copy(
                        showRouteToLocation = to, routePoints = routePoints, isLoadingRoute = false
                    )
                }
            } catch (e: Exception) {
                ensureActive()
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoadingRoute = false,
                        errorMessage = "Failed to calculate route: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handlePermissionException(exception: Exception) {
        when (exception) {
            is DeniedAlwaysException -> {
                updatePermissionState(PermissionState.DeniedAlways, hasPermission = false)
                showError("Location permission is permanently denied. Please enable it in settings.")
            }

            is DeniedException -> {
                updatePermissionState(PermissionState.Denied, hasPermission = false)
                showError("Location permission was denied.")
            }

            else -> {
                showError("Failed to request location permission: ${exception.message}")
            }
        }
    }

    private fun updatePermissionState(state: PermissionState, hasPermission: Boolean) {
        _uiState.update {
            it.copy(
                hasLocationPermission = hasPermission, locationPermissionState = state
            )
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
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