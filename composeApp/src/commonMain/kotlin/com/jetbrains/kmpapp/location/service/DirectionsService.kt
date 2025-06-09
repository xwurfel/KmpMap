package com.jetbrains.kmpapp.location.service

import com.jetbrains.kmpapp.location.model.Location
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface DirectionsService {
    suspend fun getRoute(origin: Location, destination: Location): List<Location>
}

class GoogleDirectionsService(
    private val apiKey: String
) : DirectionsService {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    override suspend fun getRoute(origin: Location, destination: Location): List<Location> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    httpClient.get("https://maps.googleapis.com/maps/api/directions/json") {
                        parameter("origin", "${origin.latitude},${origin.longitude}")
                        parameter("destination", "${destination.latitude},${destination.longitude}")
                        parameter("key", apiKey)
                        parameter("mode", "driving")
                    }

                val directionsResponse = response.body<DirectionsResponse>()

                if (directionsResponse.status == "OK" && directionsResponse.routes.isNotEmpty()) {
                    val route = directionsResponse.routes.first()
                    return@withContext decodePolyline(route.overviewPolyline.points)
                } else {
                    // Fallback to straight line if API fails
                    return@withContext createStraightLineRoute(origin, destination)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to straight line if API fails
                return@withContext createStraightLineRoute(origin, destination)
            }
        }

    private fun createStraightLineRoute(start: Location, end: Location): List<Location> {
        val points = mutableListOf<Location>()
        val steps = 10

        for (i in 0..steps) {
            val fraction = i.toDouble() / steps
            val lat = start.latitude + (end.latitude - start.latitude) * fraction
            val lng = start.longitude + (end.longitude - start.longitude) * fraction
            points.add(Location(lat, lng))
        }

        return points
    }

    private fun decodePolyline(encoded: String): List<Location> {
        val poly = mutableListOf<Location>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = Location(
                latitude = lat.toDouble() / 1E5,
                longitude = lng.toDouble() / 1E5
            )
            poly.add(p)
        }

        return poly
    }
}

@Serializable
data class DirectionsResponse(
    val status: String,
    val routes: List<Route>
)

@Serializable
data class Route(
    val overviewPolyline: OverviewPolyline
)

@Serializable
data class OverviewPolyline(
    val points: String
)