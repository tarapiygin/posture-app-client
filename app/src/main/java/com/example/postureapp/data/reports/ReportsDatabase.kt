package com.example.postureapp.data.reports

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ReportEntity::class, ReportAssetEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(ReportConverters::class)
abstract class ReportsDatabase : RoomDatabase() {
    abstract fun reportsDao(): ReportsDao
    abstract fun reportAssetsDao(): ReportAssetsDao
}

