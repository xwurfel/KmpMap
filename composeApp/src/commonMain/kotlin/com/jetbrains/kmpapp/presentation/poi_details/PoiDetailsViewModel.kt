package com.jetbrains.kmpapp.presentation.poi_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jetbrains.kmpapp.point_of_interest.model.PointOfInterest
import com.jetbrains.kmpapp.point_of_interest.repository.FileRepository
import com.jetbrains.kmpapp.point_of_interest.repository.PoiRepository
import com.jetbrains.kmpapp.presentation.navigation.RouteManager
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class PoiDetailsViewModel(
    private val poiRepository: PoiRepository,
    private val fileRepository: FileRepository,
    private val routeManager: RouteManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoiDetailsUiState())
    val uiState: StateFlow<PoiDetailsUiState> = _uiState.asStateFlow()

    fun loadPoi(poiId: Long) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val poi = poiRepository.getPoiById(poiId)
                if (poi != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            poi = poi,
                            title = poi.title,
                            description = poi.description,
                            isEditing = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "POI not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load POI: ${e.message}"
                    )
                }
            }
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun cancelEditing() {
        val poi = _uiState.value.poi
        _uiState.update {
            it.copy(
                isEditing = false,
                title = poi?.title ?: "",
                description = poi?.description ?: "",
                selectedImage = null
            )
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
        val currentPoi = state.poi ?: return

        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title is required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                var imagePath = currentPoi.imagePath

                state.selectedImage?.let { imageFile ->
                    currentPoi.imagePath?.let { oldPath ->
                        fileRepository.deleteImage(oldPath)
                    }

                    val imageBytes = imageFile.readBytes()
                    val fileName =
                        "poi_${currentPoi.id}_${Clock.System.now().toEpochMilliseconds()}.jpg"
                    imagePath = fileRepository.saveImage(imageBytes, fileName)
                }

                val updatedPoi = currentPoi.copy(
                    title = state.title.trim(),
                    description = state.description.trim(),
                    imagePath = imagePath
                )

                poiRepository.updatePoi(updatedPoi)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEditing = false,
                        poi = updatedPoi,
                        selectedImage = null,
                        isSaved = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to update POI: ${e.message}"
                    )
                }
            }
        }
    }

    fun deletePoi() {
        val currentPoi = _uiState.value.poi ?: return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                currentPoi.imagePath?.let { imagePath ->
                    fileRepository.deleteImage(imagePath)
                }

                poiRepository.deletePoi(currentPoi.id)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isDeleted = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete POI: ${e.message}"
                    )
                }
            }
        }
    }

    fun requestRoute() {
        val poi = _uiState.value.poi
        if (poi != null) {
            routeManager.requestRoute(poi.location)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    data class PoiDetailsUiState(
        val isLoading: Boolean = false,
        val isEditing: Boolean = false,
        val isSaved: Boolean = false,
        val isDeleted: Boolean = false,
        val poi: PointOfInterest? = null,
        val title: String = "",
        val description: String = "",
        val selectedImage: PlatformFile? = null,
        val errorMessage: String? = null
    )
}