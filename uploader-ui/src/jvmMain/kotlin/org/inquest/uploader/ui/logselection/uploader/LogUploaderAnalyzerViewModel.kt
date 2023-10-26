package org.inquest.uploader.ui.logselection.uploader

import cafe.adriel.voyager.core.model.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.api.logs.ILogUploader
import org.inquest.uploader.ui.commons.utils.ViewModel
import org.inquest.uploader.ui.commons.view.GlobalViewModel
import java.nio.file.Path

/**
 * Contains methods for uploading/analyzing a given log.
 *
 * @param logUploader Used to upload
 * @param gW2EIParser Used to analyze
 * @param globalViewModel Used to update the ui
 */
data class LogUploaderAnalyzerViewModel(
    private val logUploader: ILogUploader,
    private val gW2EIParser: IGW2EIParser,
    private val globalViewModel: GlobalViewModel
): ViewModel {
    /**
     * Uploads the given [path] to dps.report.
     *
     * @param path Log to upload
     * @param onSuccess Run when a log was successfully uploaded
     * @param onError Run when uploading failed
     */
    fun upload(
        path: Path,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) = this.coroutineScope.launch(Dispatchers.IO) {
        this@LogUploaderAnalyzerViewModel.logUploader.upload(
            path,
            this@LogUploaderAnalyzerViewModel.globalViewModel::updateBossList,
            this@LogUploaderAnalyzerViewModel.globalViewModel::updateStoredLogs,
        ).onFailure(onError).onSuccess {
            onSuccess()
        }
    }

    /**
     * Analyzes the given [path] using elite insights.
     *
     * @param path Log to analyze
     * @param onSuccess Run when a log was successfully analyzed
     * @param onError Run when analysis failed
     */
    fun parse(
        path: Path,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) = this.coroutineScope.launch(Dispatchers.IO) {
        this@LogUploaderAnalyzerViewModel.gW2EIParser.parse(path).onFailure(onError).onSuccess { onSuccess() }
    }
}