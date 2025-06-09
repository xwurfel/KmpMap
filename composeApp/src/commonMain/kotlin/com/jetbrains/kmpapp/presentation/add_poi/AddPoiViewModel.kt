package com.jetbrains.kmpapp.presentation.add_poi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.location.model.Location
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest
import com.jetbrains.kmpapp.point_of_interest.repository.FileRepository
import com.jetbrains.kmpapp.point_of_interest.repository.PoiRepository
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.storage.STORAGE
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class AddPoiViewModel(
    private val poiRepository: PoiRepository,
    private val fileRepository: FileRepository,
    val permissionsController: PermissionsController
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPoiUiState())
    val uiState: StateFlow<AddPoiUiState> = _uiState.asStateFlow()

    init {
        checkPermissions()
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            try {
                val cameraState = permissionsController.getPermissionState(Permission.CAMERA)
                val storageState = permissionsController.getPermissionState(Permission.STORAGE)

                _uiState.update {
                    it.copy(
                        cameraPermissionState = cameraState,
                        storagePermissionState = storageState,
                        hasCameraPermission = cameraState == PermissionState.Granted,
                        hasStoragePermission = storageState == PermissionState.Granted
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to check permissions: ${e.message}")
                }
            }
        }
    }

    fun requestCameraPermission() {
        viewModelScope.launch {
            try {
                permissionsController.providePermission(Permission.CAMERA)
                _uiState.update {
                    it.copy(
                        hasCameraPermission = true,
                        cameraPermissionState = PermissionState.Granted,
                        errorMessage = null
                    )
                }
            } catch (deniedAlways: DeniedAlwaysException) {
                _uiState.update {
                    it.copy(
                        hasCameraPermission = false,
                        cameraPermissionState = PermissionState.DeniedAlways,
                        errorMessage = "Camera permission is permanently denied. Please enable it in settings."
                    )
                }
            } catch (denied: DeniedException) {
                _uiState.update {
                    it.copy(
                        hasCameraPermission = false,
                        cameraPermissionState = PermissionState.Denied,
                        errorMessage = "Camera permission was denied."
                    )
                }
            }
        }
    }

    fun requestStoragePermission() {
        viewModelScope.launch {
            try {
                permissionsController.providePermission(Permission.STORAGE)
                _uiState.update {
                    it.copy(
                        hasStoragePermission = true,
                        storagePermissionState = PermissionState.Granted,
                        errorMessage = null
                    )
                }
            } catch (deniedAlways: DeniedAlwaysException) {
                _uiState.update {
                    it.copy(
                        hasStoragePermission = false,
                        storagePermissionState = PermissionState.DeniedAlways,
                        errorMessage = "Storage permission is permanently denied. Please enable it in settings."
                    )
                }
            } catch (denied: DeniedException) {
                _uiState.update {
                    it.copy(
                        hasStoragePermission = false,
                        storagePermissionState = PermissionState.Denied,
                        errorMessage = "Storage permission was denied."
                    )
                }
            }
        }
    }

    fun openAppSettings() {
        permissionsController.openAppSettings()
    }

    fun setLocation(latitude: Double, longitude: Double) {
        _uiState.update {
            it.copy(location = Location(latitude, longitude))
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateImage(file: PlatformFile?) {
        _uiState.update { it.copy(selectedImage = file) }
    }

    fun savePoi() {
        val state = _uiState.value

        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required") }
            return
        }

        if (state.location == null) {
            _uiState.update { it.copy(errorMessage = "Location is required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                var imagePath: String? = null

                state.selectedImage?.let { imageFile ->
                    val imageBytes = imageFile.readBytes()
                    val fileName = "poi_${Clock.System.now().toEpochMilliseconds()}.jpg"
                    imagePath = fileRepository.saveImage(imageBytes, fileName)
                }

                val poi = PointOfInterest(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    imagePath = imagePath,
                    location = state.location,
                    createdAt = Clock.System.now()
                )

                poiRepository.savePoi(poi)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save POI: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    data class AddPoiUiState(
        val isLoading: Boolean = false,
        val isSaved: Boolean = false,
        val location: Location? = null,
        val title: String = "",
        val description: String = "",
        val selectedImage: PlatformFile? = null,
        val hasCameraPermission: Boolean = false,
        val hasStoragePermission: Boolean = false,
        val cameraPermissionState: PermissionState = PermissionState.NotDetermined,
        val storagePermissionState: PermissionState = PermissionState.NotDetermined,
        val errorMessage: String? = null
    )
}