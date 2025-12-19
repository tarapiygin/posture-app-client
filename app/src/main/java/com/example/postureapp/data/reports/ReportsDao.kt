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

    @Query(
        "UPDATE reports SET serverId=:serverId, syncStatus='SYNCED', version=:version, updatedAt=:updatedAt, lastSyncError=NULL WHERE id=:id"
    )
    suspend fun markSynced(id: String, serverId: String, version: Int, updatedAt: Long)

    @Query(
        "UPDATE reports SET syncStatus='FAILED', updatedAt=:updatedAt, lastSyncError=:reason WHERE id=:id"
    )
    suspend fun markFailed(id: String, reason: String, updatedAt: Long)

    @Query("UPDATE reports SET sessionId=:sessionId, updatedAt=:updatedAt WHERE id=:id")
    suspend fun updateSession(id: String, sessionId: String, updatedAt: Long)

    @Query("SELECT * FROM reports WHERE serverId=:serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): ReportEntity?

    @Query("DELETE FROM reports WHERE serverId IN (:serverIds)")
    suspend fun deleteByServerIds(serverIds: List<String>)

    @Query("DELETE FROM reports")
    suspend fun clear()
}

