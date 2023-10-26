package org.inquest.uploader.api.logs

import java.nio.file.Path

/**
 * Responsible for everything related to GW2 Elite Insights.
 */
interface IGW2EIParser {
    val config: GW2EIConfig

    /**
     * Checks if Elite Insights needs an update.
     */
    suspend fun needsUpdate(): Result<Boolean>

    /**
     * Downloads the latest release of Elite Insights.
     */
    suspend fun downloadLatestRelease(): Result<Unit>

    /**
     * Analyzes the given [path].
     */
    suspend fun parse(path: Path): Result<Unit>

    /**
     * Stores a general Elite Insights [config].
     */
    fun storeConfig(config: GW2EIConfig)
}