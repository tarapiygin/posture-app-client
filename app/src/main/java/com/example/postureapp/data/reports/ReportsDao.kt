package com.example.postureapp.data.reports

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReportsDao {

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    suspend fun getAll(): List<ReportEntity>

    @Query("SELECT * FROM reports WHERE id=:id LIMIT 1")
    suspend fun getById(id: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: ReportEntity)

    @Delete
    suspend fun delete(report: ReportEntity)
}

