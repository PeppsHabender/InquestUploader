package org.inquest.uploader.ui.logselection.loglist

import cafe.adriel.voyager.core.model.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.logs.Boss
import org.inquest.uploader.api.logs.FolderToBoss
import org.inquest.uploader.api.logs.LogFile
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import org.inquest.uploader.bl.utils.DateFormatters
import org.inquest.uploader.bl.utils.FileWatcher
import org.inquest.uploader.bl.utils.IPersistenceExtensions.get
import org.inquest.uploader.bl.utils.InquestUploaderConstants
import org.inquest.uploader.bl.utils.PathExtensions.findBoss
import org.inquest.uploader.bl.utils.result
import org.inquest.uploader.ui.commons.utils.ViewModel
import org.inquest.uploader.ui.commons.view.GlobalViewModel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.walk

/**
 * Simple model for [LogSelection].
 */
internal class LogSelectionViewModel(
    private val config: IConfig,
    private val globalViewModel: GlobalViewModel
) : ViewModel {
    private var bossesValid = false
    private var logFilesValid: Set<Boss> = emptySet()

    private var bossCache: List<Boss> = emptyList()
    private var logFileCache: Map<Boss, List<LogFile>> = emptyMap()

    init {
        this.coroutineScope.launch(context = Dispatchers.IO) {
            FileWatcher(config.arcdpsPath).onFileCreated {
                this@LogSelectionViewModel.bossesValid = false
                this@LogSelectionViewModel.logFilesValid = emptySet()
            }
        }
    }

    /**
     * @return List of all boss names.
     */
    fun fetchBosses(): List<Boss> = fetchCurrentBossesImpl()

    /**
     * Loads all logs for the given [boss].
     */
    fun loadLogs(boss: Boss): List<LogFile> = loadLogsImpl(boss)

    @OptIn(ExperimentalPathApi::class)
    private fun fetchCurrentBossesImpl(): List<Boss> = if(this.bossesValid) this.bossCache else
        this.config.arcdpsPath.walk()
            // Filter out any non-logs
            .filter { it.extension in InquestUploaderConstants.LOG_FILE_EXTENSIONS_STR }
            // Find all available bosses
            .map { it.findBoss(this@LogSelectionViewModel.config) }.filterNotNull()
            // Filter out duplicates
            .distinctBy(Path::name)
            // Map to the real boss name
            .map {
                val folderToBoss: FolderToBoss = this@LogSelectionViewModel.globalViewModel.folderToBoss
                val readableName: String = if(this.config.showFolderNames) it.name else it.fetchEncounterName(folderToBoss)

                Boss(readableName, it, it.name)
            }.toList().also {
                this.bossCache = it
                this.bossesValid = true
            }

    private fun Path.fetchEncounterName(folderToBoss: FolderToBoss): String {
        if(this.name !in folderToBoss.folderToBoss) {
            return this.name
        }

        return this@LogSelectionViewModel.globalViewModel.bossToName[folderToBoss[this.name]!!]!!.replace(" CM", "")
    }

    private fun loadLogsImpl(boss: Boss): List<LogFile> {
        if(!boss.realFile.isDirectory() || boss.realFile.notExists()) {
            // This ain't a real boss
            return emptyList()
        } else if(boss in this.logFilesValid) {
            // We already have logs cached for that
            return this.logFileCache[boss] ?: emptyList()
        }

        return Files.walk(boss.realFile).sorted(
            // Newest logs first
            Comparator.comparing<Path?, Long?> {
                it.toFile().lastModified()
            }.reversed()
        )
        // Filter out real logs
        .filter { it.extension in InquestUploaderConstants.LOG_FILE_EXTENSIONS_STR }
        // Fetch log files
        .map {
            { toLogFile(it) }.result()
        }
        // Map to successful conversions
        .filter(Result<LogFile>::isSuccess).map(Result<LogFile>::getOrThrow).map {
            val stored: Map<String, LogState> = this@LogSelectionViewModel.globalViewModel.storedLogs.storedLogs
            if(it.realFile.absolutePathString() in stored) {
                // Update the log state
                it.copy(state = stored[it.realFile.absolutePathString()]!!)
            } else {
                it
            }
        }.toList().also {
            this.logFileCache += boss to it
            this.logFilesValid += boss
        }
    }

    private fun toLogFile(path: Path): LogFile = if(!path.isRegularFile()) {
        throw IllegalArgumentException()
    } else {
        val date = DateFormatters.LOG_FORMATTER.parse(path.name)
        LogFile(DateFormatters.READABLE_FORMATTER.format(date), path, this.globalViewModel.storedLogs[path, this.config] ?: LogState.IDLE)
    }
}
