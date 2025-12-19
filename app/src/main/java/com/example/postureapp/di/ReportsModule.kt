package com.example.postureapp.di

import android.content.Context
import androidx.room.Room
import com.example.postureapp.data.reports.ReportConverters
import com.example.postureapp.data.reports.ReportAssetsDao
import com.example.postureapp.data.reports.ReportsDao
import com.example.postureapp.data.reports.ReportsDatabase
import com.example.postureapp.domain.reports.ReportRepository
import com.example.postureapp.domain.reports.ReportAssetsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object ReportsModule {

    @Provides
    @Singleton
    fun provideReportsDatabase(
        @ApplicationContext context: Context,
        json: Json
    ): ReportsDatabase {
        return Room.databaseBuilder(
            context,
            ReportsDatabase::class.java,
            "reports.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideReportsDao(db: ReportsDatabase): ReportsDao = db.reportsDao()

    @Provides
    fun provideReportAssetsDao(db: ReportsDatabase): ReportAssetsDao = db.reportAssetsDao()

    @Provides
    @Singleton
    fun provideReportRepository(dao: ReportsDao): ReportRepository = ReportRepository(dao)

    @Provides
    @Singleton
    fun provideReportAssetsRepository(dao: ReportAssetsDao): ReportAssetsRepository =
        ReportAssetsRepository(dao)
}

