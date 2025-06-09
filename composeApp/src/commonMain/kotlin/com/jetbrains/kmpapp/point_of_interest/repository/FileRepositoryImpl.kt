package com.jetbrains.kmpapp.point_of_interest.repository

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class FileRepositoryImpl : FileRepository {

    override suspend fun saveImage(imageBytes: ByteArray, fileName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val imagesDir = FileKit.filesDir / "images"
                val imageFile = imagesDir / fileName
                imageFile.write(imageBytes)
                "images/$fileName"
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    override suspend fun loadImage(path: String): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val file = FileKit.filesDir / path
                if (file.exists()) file.readBytes() else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    override suspend fun deleteImage(path: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val file = FileKit.filesDir / path
                if (file.exists()) {
                    file.delete()
                    true
                } else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    override suspend fun getImagePath(fileName: String): String {
        return "images/$fileName"
    }

    override suspend fun getFullImagePath(relativePath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val file = FileKit.filesDir / relativePath
                if (file.exists()) file.path else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}