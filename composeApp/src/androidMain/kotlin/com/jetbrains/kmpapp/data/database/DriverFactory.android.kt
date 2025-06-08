package com.jetbrains.kmpapp.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.ilnytskyi.mappincmp.data.database.MapPinDatabase

actual class DriverFactory(private val context: Context) {
    actual fun getDriver(): SqlDriver {
        return AndroidSqliteDriver(MapPinDatabase.Schema, context, "MapPinCMP.db")
    }
}