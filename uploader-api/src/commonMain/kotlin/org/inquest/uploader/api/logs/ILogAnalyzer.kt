package org.inquest.uploader.api.logs

/**
 * Responsible for watching logs to analyze.
 */
interface ILogAnalyzer {
    /**
     * Starts to watch for any logs to analyze.
     *
     * @param onNewMeta Executed when new meta information is encountered
     * @param onNewLog Executed when a new log was found
     */
    suspend fun startAnalyzing(onNewMeta: () -> Unit, onNewLog: () -> Unit)

    /**
     * Manually triggers analysis.
     */
    suspend fun triggerAnalysis()
}