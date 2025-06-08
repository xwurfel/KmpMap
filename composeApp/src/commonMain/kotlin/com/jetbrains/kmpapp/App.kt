package com.jetbrains.kmpapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.presentation.add_poi.AddPoiScreen
import com.jetbrains.kmpapp.presentation.map.MapScreen
import com.jetbrains.kmpapp.presentation.navigation.Destinations
import com.jetbrains.kmpapp.presentation.poi_details.PoiDetailsScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = Destinations.Map
        ) {
            composable<Destinations.Map> {
                MapScreen(
                    onNavigateToAddPoi = { location ->
                        navController.navigate(
                            Destinations.AddPoi(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    },
                    onNavigateToPoiDetails = { poiId ->
                        navController.navigate(Destinations.PoiDetails(poiId))
                    }
                )
            }

            composable<Destinations.MapWithRoute> { backStackEntry ->
                val args = backStackEntry.toRoute<Destinations.MapWithRoute>()
                MapScreen(
                    onNavigateToAddPoi = { location ->
                        navController.navigate(
                            Destinations.AddPoi(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    },
                    onNavigateToPoiDetails = { poiId ->
                        navController.navigate(Destinations.PoiDetails(poiId))
                    },
                    routeToLocation = Location(
                        latitude = args.destinationLatitude,
                        longitude = args.destinationLongitude
                    )
                )
            }

            composable<Destinations.AddPoi> { backStackEntry ->
                val args = backStackEntry.toRoute<Destinations.AddPoi>()
                AddPoiScreen(
                    latitude = args.latitude,
                    longitude = args.longitude,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<Destinations.PoiDetails> { backStackEntry ->
                val args = backStackEntry.toRoute<Destinations.PoiDetails>()
                PoiDetailsScreen(
                    poiId = args.poiId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToMap = { poi ->
                        navController.navigate(
                            Destinations.MapWithRoute(
                                destinationLatitude = poi.location.latitude,
                                destinationLongitude = poi.location.longitude
                            )
                        ) {
                            popUpTo(Destinations.Map) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}