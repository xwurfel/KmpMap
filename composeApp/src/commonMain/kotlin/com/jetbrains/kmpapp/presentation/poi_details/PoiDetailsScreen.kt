package com.jetbrains.kmpapp.presentation.poi_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jetbrains.kmpapp.presentation.common.LoadingOverlay
import com.jetbrains.kmpapp.presentation.common.PoiImage
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailsScreen(
    poiId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToMapWithRoute: () -> Unit,
    viewModel: PoiDetailsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(poiId) {
        viewModel.loadPoi(poiId)
    }

    LaunchedEffect(uiState.isDeleted, uiState.isSaved) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val imagePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        title = "Select Image"
    ) { file ->
        viewModel.updateImage(file)
    }

    val cameraLauncher = rememberCameraPickerLauncher { file ->
        viewModel.updateImage(file)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POI Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!uiState.isEditing && uiState.poi != null) {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.poi == null) {
            LoadingOverlay(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.poi?.let { poi ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (uiState.isEditing) {
                                OutlinedTextField(
                                    value = uiState.title,
                                    onValueChange = viewModel::updateTitle,
                                    label = { Text("Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    enabled = !uiState.isLoading
                                )

                                OutlinedTextField(
                                    value = uiState.description,
                                    onValueChange = viewModel::updateDescription,
                                    label = { Text("Description") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5,
                                    enabled = !uiState.isLoading
                                )
                            } else {
                                Text(
                                    text = poi.title,
                                    style = MaterialTheme.typography.headlineSmall
                                )

                                if (poi.description.isNotBlank()) {
                                    Text(
                                        text = poi.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            if (uiState.isEditing) {
                                Text(
                                    text = "Image",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                val imageToShow = uiState.selectedImage ?: poi.imagePath
                                imageToShow?.let {
                                    PoiImage(
                                        imageSource = it,
                                        contentDescription = "POI Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { imagePickerLauncher.launch() },
                                        modifier = Modifier.weight(1f),
                                        enabled = !uiState.isLoading
                                    ) {
                                        Text("Gallery")
                                    }

                                    Button(
                                        onClick = {
                                            cameraLauncher.launch()
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !uiState.isLoading
                                    ) {
                                        Text("Camera")
                                    }
                                }
                            } else {
                                poi.imagePath?.let { imagePath ->
                                    PoiImage(
                                        imageSource = imagePath,
                                        contentDescription = "POI Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.isEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.cancelEditing() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(),
                                enabled = !uiState.isLoading
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = { viewModel.savePoi() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isLoading && uiState.title.isNotBlank()
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save")
                                }
                            }
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.requestRoute()
                                    onNavigateToMapWithRoute()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Show Walking Route")
                            }

                            Button(
                                onClick = { viewModel.deletePoi() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                enabled = !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                } else {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete POI")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}