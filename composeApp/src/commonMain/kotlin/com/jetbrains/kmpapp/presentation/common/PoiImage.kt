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
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir

@Composable
fun PoiImage(
    imageSource: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var imageModel by remember(imageSource) { mutableStateOf<Any?>(null) }

    LaunchedEffect(imageSource) {
        imageModel = when (imageSource) {
            is PlatformFile -> imageSource
            is String -> {
                try {
                    val fullFile = FileKit.filesDir / imageSource
                    fullFile
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            else -> null
        }
    }

    imageModel?.let { model ->
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}