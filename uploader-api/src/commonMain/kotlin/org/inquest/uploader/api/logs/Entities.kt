package org.inquest.uploader.api.logs

import kotlinx.serialization.Serializable
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import java.nio.file.Path

@Serializable
data class Boss(val readableName: String, val realFile: Path, val name: String)

@Serializable
data class LogFile(val readableName: String, val realFile: Path, val state: LogState)

@Serializable
data class GW2EIConfig(
    val Anonymous: Boolean = false,
    val OutLocation: String? = null,
) {
    val SaveOutJSON: Boolean = true
    val SaveOutHtml: Boolean = false
    val SaveOutTrace: Boolean = false
    val SaveAtOut: Boolean = OutLocation == null
}

@Serializable
data class FolderToBoss(val folderToBoss: Map<String, Long> = emptyMap()) {
    operator fun get(folder: String) = this.folderToBoss[folder]
    operator fun plus(pair: Pair<String, Long>) = FolderToBoss(this.folderToBoss + pair)
}

@Serializable
data class BossToName(val bossToName: Map<Long, String> = emptyMap()) {
    operator fun get(bossId: Long) = this.bossToName[bossId]
    operator fun plus(pair: Pair<Long, String>) = BossToName(this.bossToName + pair)
}

/**
 * Logs that are currently stored by the inquest.
 */
@Serializable
data class StoredLogs(val storedLogs: Map<String, LogState> = emptyMap()) {
    companion object {
        enum class LogState {
            IDLE, UPLOADED, ANALYZED, UPLOADED_ANALZED;

            fun isUploaded() = this == UPLOADED || this == UPLOADED_ANALZED

            fun isAnalyzed() = this == ANALYZED || this == UPLOADED_ANALZED

            fun updateWith(state: LogState): LogState = if(this == IDLE) {
                state
            } else if(state != IDLE && this != state) {
                UPLOADED_ANALZED
            } else {
                this
            }
        }
    }
}