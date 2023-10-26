package org.inquest.uploader.bl.logs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.api.logs.BossToName
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.api.logs.ILogLoader
import org.inquest.uploader.api.logs.ILogUploader
import org.inquest.uploader.api.logs.StoredLogs
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState.UPLOADED
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.bl.utils.FileWatcher
import org.inquest.uploader.bl.utils.IPersistenceExtensions.bossToName
import org.inquest.uploader.bl.utils.IPersistenceExtensions.folderToBoss
import org.inquest.uploader.bl.utils.IPersistenceExtensions.plus
import org.inquest.uploader.bl.utils.IPersistenceExtensions.storedLogs
import org.inquest.uploader.bl.utils.InquestUploaderConstants
import org.inquest.uploader.bl.utils.result
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

/**
 * Implementation of [ILogLoader]
 *
 * @param config Used for what its supposed for
 * @param uploader Obvious as well
 * @param gW2EIParser Bruh
 */
internal class LogLoader(
    private val config: IConfig,
    private val uploader: ILogUploader,
    private val gW2EIParser: IGW2EIParser
): ILogLoader {
    override suspend fun startWatching(onNewMeta: () -> Unit, onNewLog: () -> Unit) {
        LOG.info("Starting to watch for logs to upload...")
        FileWatcher(this.config.arcdpsPath)
            .onFileCreated {
                LOG.info("Found new file {} to upload...", it)
                if(it.extension !in InquestUploaderConstants.LOG_FILE_EXTENSIONS_STR) {
                    // No log -> No business
                    return@onFileCreated
                }

                if(this@LogLoader.config.uploadToDpsReport) {
                    this@LogLoader.uploader.upload(it, onNewMeta, onNewLog)
                }

                if(this@LogLoader.config.analyzeAutomatically) {
                    this@LogLoader.gW2EIParser.parse(it)
                }
            }.startWatching()
    }
}

/**
 * Implementation of [ILogUploader].
 *
 * @param config Used for config reasons
 * @param persistence Used to store uploaded logs
 * @param gw2EIParser Determines if logs are uploaded anonymously
 */
internal class LogUploader(
    private val config: IConfig,
    private val persistence: IPersistence,
    private val gw2EIParser: IGW2EIParser,
): ILogUploader {
    override suspend fun upload(
        log: Path,
        onNewMeta: (suspend () -> Unit)?,
        onNewLog: (suspend () -> Unit)?
    ): Result<ILog.DpsLog> = suspend {
        uploadImpl(log, onNewMeta, onNewLog)
    }.result().onFailure {
        LOG.error("Failed to upload {} to dps.report!", log, it)
    }

    private suspend fun uploadImpl(
        log: Path,
        onNewMeta: (suspend () -> Unit)?,
        onNewLog: (suspend () -> Unit)?
    ): ILog.DpsLog {
        if(log.extension !in InquestUploaderConstants.LOG_FILE_EXTENSIONS_STR) {
            throw IllegalArgumentException()
        }

        LOG.info("Uploading log {} to dps.report...", log)
        val dpsLog: ILog.DpsLog = post(log, onNewMeta)
        if (StringUtils.isEmpty(this.config.userToken)) {
            // Dataaaa
            dpsLog.userToken?.let { this.config.store(userToken = it).getOrThrow() }
        }
        LOG.info("Successfully uploaded {} to dps.report.", log)

        if(onNewLog != null) {
            onNewLog()
        }

        return dpsLog
    }

    private suspend fun post(path: Path, onNewMeta: (suspend () -> Unit)?): ILog.DpsLog {
        var userTokenParam = if (StringUtils.isEmpty(this.config.userToken)) "" else "&userToken=" + this.config.userToken
        userTokenParam += "&anonymous=${gw2EIParser.config.Anonymous}"

        val post: HttpPost = HttpPost(DPS_REPORT_ENDPOINT + userTokenParam).apply {
            setHeader("Accept", "application/json")
            entity = MultipartEntityBuilder.create().addPart("file", FileBody(path.toFile(), ContentType.DEFAULT_BINARY)).build()
        }
        val response: HttpResponse = CLIENT.execute(post)

        return OBJECT_MAPPER.readValue<ILog.DpsLog>(EntityUtils.toString(response.entity)).also {
            this.persistence.storeDpsReportLog(path, it).getOrThrow()

            val storedLogs: StoredLogs = this.persistence.storedLogs
            this.persistence.storedLogs = storedLogs.plus(path to UPLOADED, this@LogUploader.config)

            if(it.encounter.boss != null && it.encounter.boss != null) {
                storeMeta(path, it.encounter.bossId!!, it.encounter.boss!!, onNewMeta)
            }
        }
    }

    private suspend fun storeMeta(path: Path, triggerID: Long, bossName: String, onNewMeta: (suspend () -> Unit)?) {
        val bossToName: BossToName = this.persistence.bossToName
        if(triggerID in bossToName.bossToName) {
            return
        }

        this.persistence.bossToName = bossToName + (triggerID to bossName)
        this.persistence.folderToBoss += (path.parent.name to triggerID)
        if (onNewMeta != null) {
            onNewMeta()
        }
    }

    companion object {
        private const val DPS_REPORT_ENDPOINT = "https://dps.report/uploadContent?json=1&generator=ei"
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper().registerKotlinModule()
        private val REQUEST_CONFIG: RequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
        private val CLIENT: HttpClient = HttpClientBuilder.create().setDefaultRequestConfig(REQUEST_CONFIG).build();
    }
}