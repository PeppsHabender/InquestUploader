package org.inquest.uploader.ui.logselection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.inquest.uploader.api.logs.LogFile
import org.inquest.uploader.spi.ICurrentLog

/**
 * View model for the currently selected single log.
 */
internal class CurrentLogViewModel: ICurrentLog {
    /**
     * Currently selected single log.
     */
    override var currentLog: LogFile? by mutableStateOf(null)
        internal set
}