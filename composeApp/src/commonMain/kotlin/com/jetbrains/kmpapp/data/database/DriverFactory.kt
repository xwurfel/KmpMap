package com.jetbrains.kmpapp.data.database

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun getDriver(): SqlDriver
}