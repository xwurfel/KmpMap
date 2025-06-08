package com.jetbrains.kmpapp.location.repository

import com.jetbrains.kmpapp.location.model.Location
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class LocationRepository {
    private val locationManager = CLLocationManager()
    private var locationDelegate: LocationDelegate? = null

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getCurrentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            locationDelegate = LocationDelegate { location, error ->
                if (error != null) {
                    continuation.resume(null)
                } else {
                    location?.let {
                        val coordinate = it.coordinate.useContents {
                            Location(latitude = latitude, longitude = longitude)
                        }
                        continuation.resume(coordinate)
                    } ?: continuation.resume(null)
                }
            }

            locationManager.delegate = locationDelegate
            locationManager.desiredAccuracy = kCLLocationAccuracyBest
            locationManager.requestLocation()
        }
}

private class LocationDelegate(
    private val onLocationUpdate: (CLLocation?, NSError?) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        onLocationUpdate(location, null)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onLocationUpdate(null, didFailWithError)
    }
}