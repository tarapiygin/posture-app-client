package com.example.postureapp.domain.reports

import com.example.postureapp.data.reports.ReportEntity
import com.example.postureapp.data.reports.ReportsDao
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportRepository @Inject constructor(
    private val dao: ReportsDao
) {

    suspend fun getReports(): List<ReportEntity> = getAll()

    suspend fun getAll(): List<ReportEntity> = withContext(Dispatchers.IO) { dao.getAll() }

    suspend fun getReport(id: String): ReportEntity? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getByServerId(serverId: String): ReportEntity? = withContext(Dispatchers.IO) {
        dao.getByServerId(serverId)
    }

    suspend fun upsert(report: ReportEntity) = withContext(Dispatchers.IO) {
        dao.upsert(report)
    }

    suspend fun markSynced(id: String, serverId: String, version: Int) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.markSynced(id = id, serverId = serverId, version = version, updatedAt = now)
    }

    suspend fun markFailed(id: String, reason: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.markFailed(id = id, reason = reason, updatedAt = now)
    }

    suspend fun updateSession(id: String, sessionId: String) = withContext(Dispatchers.IO) {
        dao.updateSession(id = id, sessionId = sessionId, updatedAt = System.currentTimeMillis())
    }

    suspend fun delete(report: ReportEntity) = withContext(Dispatchers.IO) {
        dao.delete(report)
        report.pdfPath.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).takeIf(File::exists)?.delete() }
        }
        report.thumbnailPath?.takeIf { it.isNotBlank() }?.let { path ->
            runCatching { File(path).takeIf(File::exists)?.delete() }
        }
    }

    suspend fun deleteByServerIds(serverIds: List<String>) = withContext(Dispatchers.IO) {
        dao.deleteByServerIds(serverIds)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clear()
    }
}

