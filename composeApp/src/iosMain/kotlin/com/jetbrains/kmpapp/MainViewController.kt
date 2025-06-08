package com.jetbrains.kmpapp

import androidx.compose.ui.window.ComposeUIViewController
import com.jetbrains.kmpapp.di.initKoin

fun MainViewController() = ComposeUIViewController {
    var wasInitialized = false
    if (wasInitialized.not()) {
        try {
            initKoin()
            wasInitialized = true
        } catch (e: Exception) {
            println("Koin initialization failed: ${e.message}")
        }
    }
    App()
}