package com.jetbrains.kmpapp.point_of_interest.repository

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.createDirectories
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
                // Ensure images directory exists
                val imagesDir = FileKit.filesDir / "images"
                if (!imagesDir.exists()) {
                    imagesDir.createDirectories()
                    println("FileRepository: Created images directory at: ${imagesDir.path}")
                }

                val imageFile = imagesDir / fileName
                imageFile.write(imageBytes)

                // Return relative path for database storage
                val relativePath = "images/$fileName"
                println("FileRepository: Image saved successfully at: ${imageFile.path}")
                println("FileRepository: Returning relative path: $relativePath")

                // Verify the file was actually saved
                if (imageFile.exists()) {
                    val fileSize = imageFile.readBytes().size
                    println("FileRepository: Verification successful - file exists with size: $fileSize bytes")
                    relativePath
                } else {
                    println("FileRepository: ERROR - Image file doesn't exist after saving")
                    null
                }
            } catch (e: Exception) {
                println("FileRepository: ERROR saving image: ${e.message}")
                e.printStackTrace()
                null
            }
        }

    override suspend fun loadImage(path: String): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val file = FileKit.filesDir / path
                if (file.exists()) {
                    val bytes = file.readBytes()
                    println("FileRepository: Image loaded successfully from: $path")
                    bytes
                } else {
                    println("FileRepository: Image file not found at: $path")
                    null
                }
            } catch (e: Exception) {
                println("FileRepository: Error loading image: ${e.message}")
                e.printStackTrace()
                null
            }
        }

    override suspend fun deleteImage(path: String) =
        withContext(Dispatchers.IO) {
            try {
                val file = FileKit.filesDir / path
                if (file.exists()) {
                    val deleted = file.delete()
                    println("FileRepository: Image deleted: $deleted from path: $path")
                } else {
                    println("FileRepository: Image file not found for deletion at: $path")
                }
            } catch (e: Exception) {
                println("FileRepository: Error deleting image: ${e.message}")
                e.printStackTrace()
            }
        }

    override suspend fun getImagePath(fileName: String): String {
        return "images/$fileName"
    }

    override suspend fun getFullImagePath(relativePath: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val file = FileKit.filesDir / relativePath
                if (file.exists()) {
                    val fullPath = file.path
                    println("FileRepository: Full path resolved: $fullPath")
                    fullPath
                } else {
                    println("FileRepository: File doesn't exist for path resolution: $relativePath")
                    null
                }
            } catch (e: Exception) {
                println("FileRepository: Error resolving full path: ${e.message}")
                e.printStackTrace()
                null
            }
        }
}