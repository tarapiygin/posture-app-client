package com.example.postureapp.domain.cleanup

import android.content.Context
import com.example.postureapp.domain.reports.ReportAssetsRepository
import com.example.postureapp.domain.reports.ReportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearLocalDataUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
    private val assetsRepository: ReportAssetsRepository,
    @ApplicationContext private val context: Context
) {

    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        val reports = reportRepository.getAll()
        reports.forEach { report ->
            runCatching { File(report.pdfPath).takeIf { it.exists() }?.delete() }
            report.thumbnailPath?.takeIf { it.isNotBlank() }?.let { path ->
                runCatching { File(path).takeIf(File::exists)?.delete() }
            }
            listOf(report.frontImagePath, report.rightImagePath).forEach { path ->
                path?.takeIf { it.isNotBlank() }?.let {
                    runCatching { File(it).takeIf(File::exists)?.delete() }
                }
            }
            val assets = assetsRepository.getByReport(report.id)
            assets.forEach { asset ->
                runCatching { File(asset.localPath).takeIf(File::exists)?.delete() }
            }
        }
        assetsRepository.clearAll()
        reportRepository.clearAll()

        // clean cached report directories
        val reportsDir = File(context.filesDir, "reports")
        runCatching { reportsDir.deleteRecursively() }
    }
}

