package com.jetbrains.kmpapp.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.ilnytskyi.mappincmp.data.database.MapPinDatabase

actual class DriverFactory {
    actual fun getDriver(): SqlDriver {
        return NativeSqliteDriver(MapPinDatabase.Schema, "MapPinCMP.db")
    }
}