package com.jetbrains.kmpapp.point_of_interest.repository

interface FileRepository {
    suspend fun saveImage(imageBytes: ByteArray, fileName: String): String?
    suspend fun loadImage(path: String): ByteArray?
    suspend fun deleteImage(path: String): Boolean
    suspend fun getImagePath(fileName: String): String
    suspend fun getFullImagePath(relativePath: String): String?
}