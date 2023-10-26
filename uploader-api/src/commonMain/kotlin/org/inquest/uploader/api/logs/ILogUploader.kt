package org.inquest.uploader.api.logs

import org.inquest.uploader.api.entities.ILog
import java.nio.file.Path

/**
 * Responsible for uploading logs to dps.report.
 */
interface ILogUploader {
    /**
     * Uploads the given log to dps.report.
     *
     * @param log Log to upload
     * @param onNewMeta Executed when new meta information is encountered
     * @param onNewLog Executed when a new log was found
     */
    suspend fun upload(
        log: Path,
        onNewMeta: (suspend () -> Unit)? = null,
        onNewLog: (suspend () -> Unit)?
    ): Result<ILog.DpsLog>
}