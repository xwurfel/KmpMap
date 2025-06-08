package com.jetbrains.kmpapp.presentation.add_poi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import coil3.compose.AsyncImage
import dev.icerock.moko.permissions.PermissionState
import dev.icerock.moko.permissions.compose.BindEffect
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class)
@Composable
fun AddPoiScreen(
    latitude: Double,
    longitude: Double,
    onNavigateBack: () -> Unit,
    viewModel: AddPoiViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    BindEffect(viewModel.permissionsController)

    LaunchedEffect(latitude, longitude) {
        viewModel.setLocation(latitude, longitude)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (uiState.cameraPermissionState == PermissionState.DeniedAlways || uiState.storagePermissionState == PermissionState.DeniedAlways) "Settings" else null,
                duration = SnackbarDuration.Long
            )

            if (result == SnackbarResult.ActionPerformed) {
                viewModel.openAppSettings()
            }

            viewModel.clearError()
        }
    }

    val imagePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image, title = "Select Image"
    ) { file ->
        viewModel.updateImage(file)
    }

    val cameraLauncher = rememberCameraPickerLauncher { file ->
        viewModel.updateImage(file)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add Point of Interest") }, navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                    Text(
                        text = "Image", style = MaterialTheme.typography.titleMedium
                    )

                    uiState.selectedImage?.let { imageFile ->
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                when (uiState.storagePermissionState) {
                                    PermissionState.Granted -> imagePickerLauncher.launch()

                                    PermissionState.NotGranted,
                                    PermissionState.NotDetermined,
                                    PermissionState.Denied -> viewModel.requestStoragePermission()

                                    PermissionState.DeniedAlways -> viewModel.openAppSettings()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading,
                            colors = if (uiState.storagePermissionState == PermissionState.DeniedAlways) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            } else ButtonDefaults.buttonColors()
                        ) {
                            Row {
                                if (uiState.storagePermissionState == PermissionState.DeniedAlways) {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                }
                                Text(
                                    when (uiState.storagePermissionState) {
                                        PermissionState.Granted -> "Select from Gallery"
                                        PermissionState.DeniedAlways -> "Enable Storage in Settings"
                                        else -> "Grant Storage Permission"
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                when (uiState.cameraPermissionState) {
                                    PermissionState.Granted -> cameraLauncher.launch()

                                    PermissionState.NotGranted,
                                    PermissionState.NotDetermined,
                                    PermissionState.Denied -> viewModel.requestCameraPermission()

                                    PermissionState.DeniedAlways -> viewModel.openAppSettings()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading,
                            colors = if (uiState.cameraPermissionState == PermissionState.DeniedAlways) {
                                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            } else ButtonDefaults.buttonColors()
                        ) {
                            Row {
                                if (uiState.cameraPermissionState == PermissionState.DeniedAlways) {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                    Spacer(modifier = Modifier.size(8.dp))
                                }
                                Text(
                                    when (uiState.cameraPermissionState) {
                                        PermissionState.Granted -> "Take Photo"
                                        PermissionState.DeniedAlways -> "Enable Camera in Settings"
                                        else -> "Grant Camera Permission"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.savePoi() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && uiState.title.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save POI")
                }
            }
        }
    }
}