package com.example.postureapp.domain.reports

import com.example.postureapp.core.network.api.ReportsApi
import com.example.postureapp.data.reports.ReportEntity
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeleteReportUseCase @Inject constructor(
    private val reportsApi: ReportsApi,
    private val reportRepository: ReportRepository,
    private val assetsRepository: ReportAssetsRepository
) {

    suspend operator fun invoke(report: ReportEntity) = withContext(Dispatchers.IO) {
        report.serverId?.takeIf { it.isNotBlank() }?.let { serverId ->
            runCatching { reportsApi.delete(serverId) }
        }

        assetsRepository.getByReport(report.id).forEach { asset ->
            runCatching { File(asset.localPath).takeIf(File::exists)?.delete() }
        }
        assetsRepository.deleteByReport(report.id)
        reportRepository.delete(report)
    }
}

