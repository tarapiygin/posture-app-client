package com.example.postureapp.data.reports

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReportAssetsDao {

    @Query("SELECT * FROM report_assets WHERE reportId=:reportId")
    suspend fun getByReportId(reportId: String): List<ReportAssetEntity>

    @Query("SELECT * FROM report_assets WHERE syncStatus != 'SYNCED'")
    suspend fun getPending(): List<ReportAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: ReportAssetEntity)

    @Query("UPDATE report_assets SET serverId=:serverId, syncStatus='SYNCED', updatedAt=:updatedAt WHERE id=:id")
    suspend fun markSynced(id: String, serverId: String, updatedAt: Long)

    @Query("UPDATE report_assets SET syncStatus='FAILED', updatedAt=:updatedAt WHERE id=:id")
    suspend fun markFailed(id: String, updatedAt: Long)

    @Query("DELETE FROM report_assets WHERE reportId=:reportId")
    suspend fun deleteByReportId(reportId: String)

    @Query("DELETE FROM report_assets")
    suspend fun clear()
}

