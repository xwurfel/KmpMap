package com.jetbrains.kmpapp.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.coil.securelyAccessFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir

@Composable
fun PoiImage(
    imageSource: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageModel by remember(imageSource) { mutableStateOf<Any?>(null) }
    var imageBitmap by remember(imageSource) { mutableStateOf<ImageBitmap?>(null) }
    var platformFile by remember(imageSource) { mutableStateOf<PlatformFile?>(null) }

    LaunchedEffect(imageSource) {
        when (imageSource) {
            is PlatformFile -> {
                imageModel = imageSource
                platformFile = imageSource
                imageBitmap = imageSource.toImageBitmap()
            }

            is String -> {
                try {
                    val fullFile = FileKit.filesDir / imageSource

                    if (fullFile.exists()) {
                        imageModel = fullFile
                        platformFile = fullFile
                    } else {
                        if (!imageSource.contains("/")) {
                            val imageFile = FileKit.filesDir / "images" / imageSource
                            if (imageFile.exists()) {
                                imageModel = imageFile
                                platformFile = imageFile
                            } else {
                                imageModel = null
                                platformFile = null
                            }
                        } else {
                            imageModel = null
                            platformFile = null
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageModel = null
                    platformFile = null
                }
                imageBitmap = null
            }

            else -> {
                imageModel = null
                platformFile = null
                imageBitmap = null
            }
        }
    }

    if (imageBitmap != null) {
        Image(
            imageBitmap!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        imageModel?.let { model ->
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
                onState = { state: AsyncImagePainter.State ->
                    platformFile?.let { file ->
                        state.securelyAccessFile(file)
                    }
                }
            )
        }
    }
}