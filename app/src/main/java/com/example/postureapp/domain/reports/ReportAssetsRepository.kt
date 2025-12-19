package com.example.postureapp.domain.reports

import com.example.postureapp.data.reports.ReportAssetEntity
import com.example.postureapp.data.reports.ReportAssetsDao
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportAssetsRepository @Inject constructor(
    private val dao: ReportAssetsDao
) {

    suspend fun upsert(asset: ReportAssetEntity) = withContext(Dispatchers.IO) {
        dao.upsert(asset)
    }

    suspend fun getByReport(reportId: String): List<ReportAssetEntity> = withContext(Dispatchers.IO) {
        dao.getByReportId(reportId)
    }

    suspend fun getPending(): List<ReportAssetEntity> = withContext(Dispatchers.IO) {
        dao.getPending()
    }

    suspend fun markSynced(id: String, serverId: String) = withContext(Dispatchers.IO) {
        dao.markSynced(id, serverId, System.currentTimeMillis())
    }

    suspend fun markFailed(id: String) = withContext(Dispatchers.IO) {
        dao.markFailed(id, System.currentTimeMillis())
    }

    suspend fun deleteByReport(reportId: String) = withContext(Dispatchers.IO) {
        dao.deleteByReportId(reportId)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clear()
    }
}

