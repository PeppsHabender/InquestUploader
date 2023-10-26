package org.inquest.uploader.bl.config

import org.inquest.uploader.api.config.ConfigEntity
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.bl.utils.PathExtensions.createFileSafe
import org.inquest.uploader.bl.utils.SerializationUtils
import org.inquest.uploader.bl.utils.SerializationUtils.serializeToProps
import org.inquest.uploader.bl.utils.StringExtensions.toPath
import org.inquest.uploader.bl.utils.result
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.notExists

/**
 * Everything the inquest needs to know.
 */
internal class Config:  IConfig {
    private var configEntity: ConfigEntity = ConfigEntity()

    override val firstStart: Boolean = CONFIG_PATH.notExists()
    override val arcdpsPath: Path
        get() = this.configEntity.arcdpsPath.toPath()
    override val uploadToDpsReport: Boolean
        get() = this.configEntity.uploadToDpsReport
    override val analyzeAutomatically: Boolean
        get() = this.configEntity.analyzeAutomatically
    override val showFolderNames: Boolean
        get() = this.configEntity.showFolderNames
    override val userToken: String
        get() = this.configEntity.userToken

    init {
        if(CONFIG_PATH.exists()) {
            this.configEntity = SerializationUtils.deserializeFromProps(CONFIG_PATH)
        }
    }

    override fun store(
        arcdpsPath: Path,
        uploadToDpsReport: Boolean,
        analyzeAutomatically: Boolean,
        showFolderNames: Boolean,
        userToken: String
    ): Result<Unit> = this.configEntity.copy(
        arcdpsPath = arcdpsPath.absolutePathString(),
        uploadToDpsReport = uploadToDpsReport,
        analyzeAutomatically = analyzeAutomatically,
        showFolderNames = showFolderNames,
        userToken = userToken
    ).let {
        LOG.info("Storing configuration...")
        return@let { it.serializeToProps(CONFIG_PATH.createFileSafe()) }.result().onSuccess {
            LOG.info("Successfully stored configuration.")
        }.onFailure {
            LOG.error("Failed to store configuration!", it)
        }
    }

    /**
     * Factory and companion for [Config].
     */
    companion object {
        private val CONFIG_PATH: Path = Path.of("").resolve("inquest.config")
    }
}