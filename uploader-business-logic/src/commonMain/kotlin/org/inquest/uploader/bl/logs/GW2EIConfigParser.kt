package org.inquest.uploader.bl.logs

import org.inquest.uploader.api.logs.GW2EIConfig
import org.inquest.uploader.bl.utils.PathExtensions.createDirSafe
import org.inquest.uploader.bl.utils.SerializationUtils
import org.inquest.uploader.bl.utils.SerializationUtils.serializeToProps
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists

/**
 * Utils class handling everything around the elite insights config files.
 */
internal object GW2EIConfigParser {
    private const val GENERAL = "general"
    private val GW2EI_CONFIG_PATH: Path = Path("gw2ei", "Settings").createDirSafe()

    /**
     * @return General config upon which all child configurations are based.
     */
    internal fun fetchConfig(): GW2EIConfig = fetchConfig(GW2EI_CONFIG_PATH.resolve("$GENERAL.conf"))

    /**
     * @param name Name of the configuration to resolve path for
     * @return Path where a config of [name] should be stored to
     */
    internal fun configPath(name: String): Path = GW2EI_CONFIG_PATH.resolve(name.conf())

    private fun fetchConfig(path: Path): GW2EIConfig {
        if(path.notExists()) {
            // Create new config shouldnt it exist yet
            return GW2EIConfig().also(::saveConfig)
        }

        return SerializationUtils.deserializeFromProps(path)
    }

    /**
     * Saves the given [config] under the given [name].
     *
     * @param config Config to save
     * @param name Name of the config
     */
    fun saveConfig(
        config: GW2EIConfig,
        name: String = GENERAL
    ) {
        if(name == GENERAL) {
            // Propagate general config change to sub-configs
            GW2EI_CONFIG_PATH.listDirectoryEntries().filter { it.nameWithoutExtension != GENERAL }.forEach {
                saveConfig(config.copy(OutLocation = fetchConfig(it).OutLocation), it.nameWithoutExtension)
            }
        }

        config.serializeToProps(GW2EI_CONFIG_PATH.resolve(name.conf()))
    }

    /**
     * Checks wether a config with [name] already exists.
     *
     * @param name Name of the config
     * @return true when it exists, false otherwise
     */
    operator fun contains(name: String): Boolean = GW2EI_CONFIG_PATH.resolve(name.conf()).exists()

    private fun String.conf(): String = "${lowercase()}.conf"
}