package com.jetbrains.kmpapp.presentation.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cValue
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKPolyline
import platform.MapKit.MKUserTrackingModeNone
import platform.MapKit.addOverlay
import platform.MapKit.overlays
import platform.MapKit.removeOverlays
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UILongPressGestureRecognizer
import platform.darwin.NSObject
import platform.objc.sel_registerName

@OptIn(ExperimentalForeignApi::class)
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
    val mapView = remember { MKMapView() }
    val mapDelegate = remember { MapViewDelegate(onPoiClick, onMapLoaded, onMapLongClick) }

    LaunchedEffect(Unit) {
        onMapLoaded()
    }

    // Update user location
    LaunchedEffect(userLocation) {
        userLocation?.let { location ->
            val coordinate = CLLocationCoordinate2DMake(location.latitude, location.longitude)
            val region = MKCoordinateRegionMakeWithDistance(coordinate, 1000.0, 1000.0)
            mapView.setRegion(region, animated = true)
        }
    }

    // Update POI annotations
    LaunchedEffect(pois) {
        mapView.removeAnnotations(mapView.annotations)

        pois.forEach { poi ->
            val annotation = PoiAnnotation(poi)
            mapView.addAnnotation(annotation)
        }
    }

    // Update route
    LaunchedEffect(routePoints) {
        mapView.removeOverlays(mapView.overlays)

        if (routePoints.isNotEmpty()) {
            // Create coordinates array for polyline
            memScoped {
                val coordinatesArray = allocArray<CLLocationCoordinate2D>(routePoints.size)
                routePoints.forEachIndexed { index, location ->
                    coordinatesArray[index].latitude = location.latitude
                    coordinatesArray[index].longitude = location.longitude
                }

                val polyline = MKPolyline.polylineWithCoordinates(
                    coordinatesArray,
                    routePoints.size.toULong()
                )
                mapView.addOverlay(polyline)
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.delegate = mapDelegate
        mapView.showsUserLocation = true
        mapView.userTrackingMode = MKUserTrackingModeNone

        // Add long press gesture
        val longPressGesture = UILongPressGestureRecognizer()
        longPressGesture.minimumPressDuration = 0.5
        longPressGesture.addTarget(
            target = mapDelegate,
            action = sel_registerName("handleLongPress:")
        )
        mapView.addGestureRecognizer(longPressGesture)

        onDispose {
            mapView.delegate = null
        }
    }

    UIKitView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        properties = UIKitInteropProperties(
            isInteractive = true,
            isNativeAccessibilityEnabled = true
        )
    )
}

@OptIn(ExperimentalForeignApi::class)
private class MapViewDelegate(
    private val onPoiClick: (PointOfInterest) -> Unit,
    private val onMapLoaded: () -> Unit,
    private val onMapLongClick: (Location) -> Unit
) : NSObject(), MKMapViewDelegateProtocol {

    override fun mapViewDidFinishLoadingMap(mapView: MKMapView) {
        onMapLoaded()
    }

    // Handle annotation selection
//    override fun mapView(mapView: MKMapView, didSelectAnnotation: MKAnnotationProtocol) {
//        val annotation = didSelectAnnotation as? PoiAnnotation
//        annotation?.poi?.let { poi ->
//            onPoiClick(poi)
//        }
//    }
//
//    // Handle overlay rendering for route display
//    override fun mapView(
//        mapView: MKMapView,
//        rendererForOverlay: MKOverlayProtocol
//    ): MKOverlayRenderer {
//        if (rendererForOverlay is MKPolyline) {
//            val renderer = MKPolylineRenderer(rendererForOverlay)
//            renderer.strokeColor = UIColor.blueColor
//            renderer.lineWidth = 3.0
//            return renderer
//        }
//        return MKOverlayRenderer(rendererForOverlay)
//    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun handleLongPress(gesture: UILongPressGestureRecognizer) {
        if (gesture.state == UIGestureRecognizerStateBegan) {
            val mapView = gesture.view as? MKMapView ?: return
            val point = gesture.locationInView(mapView)
            val coordinate = mapView.convertPoint(point, toCoordinateFromView = mapView)

            val location = Location(
                latitude = coordinate.useContents { latitude },
                longitude = coordinate.useContents { longitude }
            )
            onMapLongClick(location)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PoiAnnotation(val poi: PointOfInterest) : NSObject(), MKAnnotationProtocol {

    override fun coordinate(): CValue<CLLocationCoordinate2D> {
        return cValue {
            latitude = poi.location.latitude
            longitude = poi.location.longitude
        }
    }

    override fun title(): String {
        return poi.title
    }

    override fun subtitle(): String {
        return poi.description
    }
}