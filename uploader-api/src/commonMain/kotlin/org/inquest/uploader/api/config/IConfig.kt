package org.inquest.uploader.api.config

import kotlinx.serialization.Serializable
import java.nio.file.Path

/**
 * Contains all app information relevant to the inquest.
 */
interface IConfig {
    val arcdpsPath: Path
    val uploadToDpsReport: Boolean
    val analyzeAutomatically: Boolean
    val showFolderNames: Boolean
    val firstStart: Boolean
    val userToken: String

    /**
     * Stores the config.
     *
     * @param arcdpsPath This shouldnt even need a description
     * @param uploadToDpsReport Should be a no-brainer
     * @param analyzeAutomatically If you say so, we'll analyze happily
     * @param showFolderNames When true, folder rather than encounter names are shown
     * @param userToken DpsReport user token.. Keep it organized
     */
    fun store(
        arcdpsPath: Path = this.arcdpsPath,
        uploadToDpsReport: Boolean = this.uploadToDpsReport,
        analyzeAutomatically: Boolean = this.analyzeAutomatically,
        showFolderNames: Boolean = this.showFolderNames,
        userToken: String = this.userToken
    ): Result<Unit>
}

/**
 * Serializable config entity.
 */
@Serializable
data class ConfigEntity(
    val arcdpsPath: String =
        System.getProperty("user.home") + "\\Documents\\Guild Wars 2\\addons\\arcdps\\arcdps.cbtlogs",
    val uploadToDpsReport: Boolean = true,
    val analyzeAutomatically: Boolean = false,
    val showFolderNames: Boolean = false,
    val userToken: String = ""
)