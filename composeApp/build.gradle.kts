import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.secrets)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true

            // Link iOS frameworks
            linkerOpts.add("-framework")
            linkerOpts.add("CoreLocation")
            linkerOpts.add("-framework")
            linkerOpts.add("MapKit")
            linkerOpts.add("-framework")
            linkerOpts.add("UIKit")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.material.icons.extended)

            // Navigation
            implementation(libs.navigation.compose)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Koin DI
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // SQLDelight
            implementation(libs.sqldelight.coroutines)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // DateTime and UUID
            implementation(libs.kotlinx.datetime)

            // FileKit
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)

            // Moko permissions
            implementation(libs.moko.permissions)
            implementation(libs.moko.permissions.compose)
            implementation(libs.permissions.camera)
            implementation(libs.permissions.gallery)
            implementation(libs.permissions.location)
            implementation(libs.permissions.storage)

            // Ktor for HTTP requests
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Kotlinx Serialization
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            // Android Compose
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Koin Android
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // SQLDelight Android
            implementation(libs.sqldelight.driver.android)

            // Android Coroutines
            implementation(libs.kotlinx.coroutines.android)

            // Location Services
            implementation(libs.play.services.location)

            // Maps
            implementation(libs.play.services.maps)
            implementation(libs.maps.compose)
            implementation(libs.maps.compose.utils)

            // Permissions
            implementation(libs.accompanist.permissions)

            // Coil
            implementation(libs.coil.compose)

            // Ktor Android
            implementation(libs.ktor.client.android)
        }

        nativeMain.dependencies {
            // SQLDelight iOS
            implementation(libs.sqldelight.driver.native)

            // Ktor iOS
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "org.ilnytskyi.mappincmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.ilnytskyi.mappincmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
    ignoreList.add("keyToIgnore")
    ignoreList.add("sdk.*")
}

buildkonfig {
    packageName = "com.jetbrains.kmpapp"

    defaultConfigs {
        buildConfigField(
            type = com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            name = "MAPS_API_KEY",
            value = project.findProperty("MAPS_API_KEY")?.toString() ?: "DEFAULT_KEY"
        )
    }
}

sqldelight {
    databases {
        create("MapPinDatabase") {
            packageName.set("org.ilnytskyi.mappincmp.data.database")
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}