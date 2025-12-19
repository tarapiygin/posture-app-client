package com.example.postureapp.domain.reports

import android.content.Context
import com.example.postureapp.core.network.api.PhotosApi
import com.example.postureapp.core.network.api.ReportsApi
import com.example.postureapp.core.network.api.SessionsApi
import com.example.postureapp.data.reports.ReportAssetEntity
import com.example.postureapp.data.reports.ReportEntity
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toImagePart
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toPdfPart
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toReportEntity
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toRequestBody
import com.example.postureapp.data.reports.sync.ReportSyncMappers.toUploadDto
import com.example.postureapp.data.reports.sync.dto.ReportsDeltaRequest
import com.example.postureapp.data.reports.sync.dto.SessionResolveRequest
import com.example.postureapp.data.system.HealthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink

class SyncAllReportsUseCase @Inject constructor(
    private val healthRepository: HealthRepository,
    private val reportRepository: ReportRepository,
    private val assetsRepository: ReportAssetsRepository,
    private val sessionsApi: SessionsApi,
    private val photosApi: PhotosApi,
    private val reportsApi: ReportsApi,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {

    sealed interface Result {
        data class Success(val uploaded: Int, val pulled: Int, val failed: Int) : Result
        data class NetworkUnavailable(val message: String) : Result
        data class Error(val message: String, val throwable: Throwable? = null) : Result
    }

    suspend operator fun invoke(): Result = withContext(Dispatchers.IO) {
        try {
            val healthy = healthRepository.ping()
            if (!healthy) return@withContext Result.NetworkUnavailable("Server not available or no internet")

            val pendingReports = reportRepository.getAll().filter { it.syncStatus != "SYNCED" }
            var uploaded = 0
            var failed = 0

            for (report in pendingReports) {
                try {
                    processReportUpload(report)
                    uploaded++
                } catch (t: Throwable) {
                    failed++
                    reportRepository.markFailed(report.id, t.message ?: "Sync failed")
                }
            }

            val pulled = pullMissingReports()
            Result.Success(uploaded = uploaded, pulled = pulled, failed = failed)
        } catch (t: Throwable) {
            Result.Error(t.message ?: "Sync failed", t)
        }
    }

    private suspend fun processReportUpload(report: ReportEntity) {
        val sessionClientId = report.sessionClientId.ifBlank { report.id }
        val session = sessionsApi.resolve(
            SessionResolveRequest(
                sessionId = report.sessionId,
                sessionClientId = sessionClientId
            )
        )
        reportRepository.updateSession(report.id, session.sessionId)

        val assets = assetsRepository.getByReport(report.id)
        for (asset in assets.filter { it.syncStatus != "SYNCED" }) {
            uploadAsset(session.sessionId, asset)
        }

        val refreshedAssets = assetsRepository.getByReport(report.id)
        val pdfFile = File(report.pdfPath)
        if (!pdfFile.exists()) throw IllegalStateException("PDF missing")
        val dto = report.toUploadDto(refreshedAssets)
        val response = reportsApi.uploadReport(pdfFile.toPdfPart(), dto.toRequestBody())
        reportRepository.markSynced(
            id = report.id,
            serverId = response.id,
            version = response.version ?: (report.version + 1)
        )
    }

    private suspend fun uploadAsset(sessionId: String, asset: ReportAssetEntity) {
        val file = File(asset.localPath)
        if (!file.exists()) {
            assetsRepository.markFailed(asset.id)
            return
        }
        val view = when (asset.side.uppercase()) {
            "FRONT" -> "front"
            "RIGHT" -> "side"
            else -> "front"
        }
        val resp = photosApi.uploadToSession(
            sessionId = sessionId,
            file = file.toImagePart(),
            view = view.toRequestBody("text/plain".toMediaType())
        )
        val serverId = resp.id
        if (serverId.isNullOrBlank()) {
            assetsRepository.markFailed(asset.id)
        } else {
            assetsRepository.markSynced(asset.id, serverId)
        }
    }

    private suspend fun pullMissingReports(): Int {
        val local = reportRepository.getAll()
        val knownServerIds = local.mapNotNull { it.serverId }
        val delta = reportsApi.delta(
            ReportsDeltaRequest(
                knownServerIds = knownServerIds,
                since = 0L
            )
        )
        var pulled = 0
        delta.deletedServerIds.forEach { serverId ->
            reportRepository.getByServerId(serverId)?.let { reportRepository.delete(it) }
        }
        for (remote in delta.addedOrUpdated) {
            val existing = remote.id.let { id -> local.firstOrNull { it.serverId == id } }
            if (existing != null && existing.version >= remote.version) continue
            val pdfPath = downloadPdf(remote.pdfUrl, remote.id, remote.version) ?: continue
            val thumbPath = generateThumbnailFromPdf(pdfPath, remote.id, remote.version)
            val entity = remote.toReportEntity(pdfPath, thumbPath)
            reportRepository.upsert(entity)
            pulled++
        }
        return pulled
    }

    private fun downloadPdf(url: String, serverId: String, version: Int): String? {
        val reportsDir = File(context.filesDir, "reports/remote").apply { mkdirs() }
        val target = File(reportsDir, "report_${serverId}_v$version.pdf")
        val request = Request.Builder().url(url).get().build()
        return runCatching {
            okHttpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                resp.body?.source()?.use { source ->
                    target.sink().buffer().use { sink -> sink.writeAll(source) }
                }
            }
            target.absolutePath
        }.getOrNull()
    }

    private fun generateThumbnailFromPdf(pdfPath: String, serverId: String, version: Int): String? {
        return runCatching {
            val file = File(pdfPath)
            if (!file.exists()) return@runCatching null
            val thumbnailsDir = File(context.filesDir, "reports/thumbs/remote").apply { mkdirs() }
            val thumbFile = File(thumbnailsDir, "thumb_${serverId}_v$version.jpg")
            PdfThumbnailGenerator.renderFirstPage(file, thumbFile)
            thumbFile.takeIf { it.exists() }?.absolutePath
        }.getOrNull()
    }
}

