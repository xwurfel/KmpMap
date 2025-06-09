package com.jetbrains.kmpapp.presentation.navigation

import com.jetbrains.kmpapp.location.model.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RouteManager {
    private val _pendingRouteDestination = MutableStateFlow<Location?>(null)
    val pendingRouteDestination: StateFlow<Location?> = _pendingRouteDestination.asStateFlow()

    fun requestRoute(destination: Location) {
        println("RouteManager: Route requested to $destination")
        _pendingRouteDestination.value = destination
    }

    fun consumePendingRoute(): Location? {
        val destination = _pendingRouteDestination.value
        _pendingRouteDestination.value = null
        println("RouteManager: Consumed pending route: $destination")
        return destination
    }

    fun clearPendingRoute() {
        println("RouteManager: Clearing pending route")
        _pendingRouteDestination.value = null
    }
}