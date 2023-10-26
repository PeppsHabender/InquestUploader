package org.inquest.uploader.api.logs

/**
 * Responsible to watch for logs to upload.
 */
interface ILogLoader {
    /**
     * Starts to watch for any logs to upload.
     *
     * @param onNewMeta Executed when new meta information is encountered
     * @param onNewLog Executed when a new log was found
     */
    suspend fun startWatching(onNewMeta: () -> Unit, onNewLog: () -> Unit)
}