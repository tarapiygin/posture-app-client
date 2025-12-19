package com.example.postureapp.domain.reports

import com.example.postureapp.core.network.api.ReportsSyncApi
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toPdfPart
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toRequestBody
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toUploadDto
import com.example.postureapp.data.system.HealthRepository
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncReportsUseCase @Inject constructor(
    private val healthRepository: HealthRepository,
    private val reportRepository: ReportRepository,
    private val reportsSyncApi: ReportsSyncApi
) {

    sealed interface Result {
        data class Success(val synced: Int, val skipped: Int, val failed: Int) : Result
        data class NetworkUnavailable(val message: String) : Result
        data class Error(val message: String, val throwable: Throwable? = null) : Result
    }

    suspend operator fun invoke(): Result = withContext(Dispatchers.IO) {
        try {
            val healthy = healthRepository.ping()
            if (!healthy) return@withContext Result.NetworkUnavailable("Server not available or no internet")

            val pending = reportRepository.getAll()
                .filter { it.syncStatus != "SYNCED" }

            var synced = 0
            var skipped = 0
            var failed = 0

            for (report in pending) {
                val pdf = File(report.pdfPath)
                if (!pdf.exists()) {
                    reportRepository.markFailed(report.id, "PDF missing")
                    failed++
                    continue
                }
                try {
                    val dto = report.toUploadDto()
                    val response = if (report.serverId.isNullOrBlank()) {
                        reportsSyncApi.uploadReport(pdf.toPdfPart(), dto.toRequestBody())
                    } else {
                        reportsSyncApi.updateReport(report.serverId, pdf.toPdfPart(), dto.toRequestBody())
                    }

                    reportRepository.markSynced(
                        id = report.id,
                        serverId = response.id,
                        version = response.version ?: (report.version + 1)
                    )
                    synced++
                } catch (t: Throwable) {
                    reportRepository.markFailed(report.id, t.message ?: "Sync failed")
                    failed++
                }
            }

            Result.Success(synced = synced, skipped = skipped, failed = failed)
        } catch (t: Throwable) {
            Result.Error(message = t.message ?: "Sync failed", throwable = t)
        }
    }
}

