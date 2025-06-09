package com.jetbrains.kmpapp.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.coil.securelyAccessFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path

@Composable
fun PoiImage(
    imageSource: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageModel by remember(imageSource) { mutableStateOf<Any?>(null) }
    var platformFile by remember(imageSource) { mutableStateOf<PlatformFile?>(null) }

    LaunchedEffect(imageSource) {
        when (imageSource) {
            is PlatformFile -> {
                if (imageSource.exists()) {
                    imageModel = imageSource
                    platformFile = imageSource
                } else {
                    println("PoiImage: PlatformFile does not exist: ${imageSource.path}")
                    imageModel = null
                    platformFile = null
                }
            }

            is String -> {
                try {
                    val resolvedFile = when {
                        imageSource.contains("/") -> FileKit.filesDir / imageSource
                        else -> FileKit.filesDir / "images" / imageSource
                    }

                    if (resolvedFile.exists()) {
                        imageModel = resolvedFile
                        platformFile = resolvedFile
                    } else {
                        imageModel = null
                        platformFile = null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    imageModel = null
                    platformFile = null
                }
            }

            else -> {
                imageModel = null
                platformFile = null
            }
        }
    }

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