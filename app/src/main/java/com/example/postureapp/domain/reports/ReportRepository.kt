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

    suspend fun getReports(): List<ReportEntity> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun getReport(id: String): ReportEntity? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun upsert(report: ReportEntity) = withContext(Dispatchers.IO) {
        dao.upsert(report)
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
}

