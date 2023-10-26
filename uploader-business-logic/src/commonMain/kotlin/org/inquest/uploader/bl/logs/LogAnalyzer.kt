package org.inquest.uploader.bl.logs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.api.logs.FolderToBoss
import org.inquest.uploader.api.logs.ILogAnalyzer
import org.inquest.uploader.api.logs.StoredLogs
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.bl.utils.FileWatcher
import org.inquest.uploader.bl.utils.IPersistenceExtensions.bossToName
import org.inquest.uploader.bl.utils.IPersistenceExtensions.folderToBoss
import org.inquest.uploader.bl.utils.IPersistenceExtensions.plus
import org.inquest.uploader.bl.utils.IPersistenceExtensions.storedLogs
import org.inquest.uploader.bl.utils.InquestUploaderConstants
import org.inquest.uploader.bl.utils.result
import java.nio.file.FileVisitResult
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.io.path.visitFileTree

/**
 * Implementation of [ILogAnalyzer].
 *
 * @param config Used to update [IPersistence.storedLogs]
 * @param persistence Used to update meta information
 */
internal class LogAnalyzer(
    private val config: IConfig,
    private val persistence: IPersistence,
): ILogAnalyzer {
    private var visited: Set<Path> = emptySet()

    override suspend fun startAnalyzing(onNewMeta: () -> Unit, onNewLog: () -> Unit) {
        LOG.info("Starting to watch for logs to analyze...")
        FileWatcher(InquestUploaderConstants.ANALYZED_DIR_PATH)
            .onFileCreated {
                LOG.info("Found new file {} to analyze...", it)
                // Wait a second until the file is written to (hopefully xd)
                // I'm pretty much too lazy to implement a mechanism for automatic detection
                runBlocking { delay(1000) }
                it.analyse(onNewMeta, onNewLog)
            }.startWatching()
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun triggerAnalysis() {
        LOG.info("Analysis triggered manually...")
        InquestUploaderConstants.ANALYZED_DIR_PATH.visitFileTree(object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                file.analyse({}, {})
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun Path.analyse(onNewMeta: () -> Unit, onNewLog: () -> Unit) {
        if(this in this@LogAnalyzer.visited) {
            // Visited already -> No business here
            return
        }

        fetchLog().onSuccess { log ->
            // Visit
            this@LogAnalyzer.visited += this

            // Encounter id to load boss images
            val triggerId: Long = log.triggerID?:-1
            // Store meta information if necessary
            storeMeta(this, triggerId, log.fightName, onNewMeta)
            // And store the log finally
            this@LogAnalyzer.persistence.storeEiLog(this, log)

            // Update the log cache
            val storedLogs: StoredLogs = this@LogAnalyzer.persistence.storedLogs
            this@LogAnalyzer.persistence.storedLogs =
                storedLogs.plus(this to LogState.ANALYZED, this@LogAnalyzer.config)
            onNewLog()

            ::deleteIfExists.result().onFailure { _ ->
                toFile().deleteOnExit()
            }
        }.onFailure {
            this@LogAnalyzer.LOG.error("Failed to analyze {}!", this, it)
        }
    }

    private fun storeMeta(path: Path, triggerID: Long, fightName: String?, onNewMeta: () -> Unit) {
        val folderToBoss: FolderToBoss = this@LogAnalyzer.persistence.folderToBoss
        if(path.parent.name in folderToBoss.folderToBoss) {
            // The inquest already knows everything
            return
        }

        this@LogAnalyzer.persistence.folderToBoss += (path.parent.name to triggerID)

        fightName?.let { name ->
            this@LogAnalyzer.persistence.bossToName += triggerID to name
        }

        onNewMeta()
    }

    private fun Path.fetchLog(): Result<ILog.JsonLog> = {
        OBJECT_MAPPER.readValue<ILog.JsonLog>(this.toFile())
    }.result()

    companion object {
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper().registerKotlinModule()
    }
}