package org.inquest.uploader.spi

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import org.inquest.uploader.api.logs.LogFile
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState

/**
 * Entry point for adapters which attach to the single log view.
 */
interface ILogViewerAdapter: Screen {
    /**
     * When true, an upload/analysis panel is shown for logs in need.
     */
    val showUploadPanel: Boolean
        get() = false

    /**
     * Id to be shown as the tab accessor, if null the class name is used.
     */
    val id: String?
        get() = null

    /**
     * Defines if a log in the state [logState] can be handled by this adapter.
     */
    fun canHandle(logState: LogState?): Boolean = LogState.UPLOADED_ANALZED == logState
}

/**
 * View model which contains the currently opened log.
 */
interface ICurrentLog: ScreenModel {
    val currentLog: LogFile?
}