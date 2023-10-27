package org.inquest.uploader.bl.logs

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.logs.GW2EIConfig
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.api.persistence.get
import org.inquest.uploader.api.persistence.set
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.bl.utils.InquestUploaderConstants
import org.inquest.uploader.bl.utils.PathExtensions.createParentSafe
import org.inquest.uploader.bl.utils.PathExtensions.findBoss
import org.inquest.uploader.bl.utils.makeResult
import org.inquest.uploader.bl.utils.result
import java.io.BufferedInputStream
import java.net.URI
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Implementation of [IGW2EIParser].
 *
 * @param appConfig Used to load a files boss
 * @param persistence Used to store elite insights meta information
 */
internal class GW2EIParser(
    private val appConfig: IConfig,
    private val persistence: IPersistence
): IGW2EIParser {

    override val config: GW2EIConfig by lazy {
        GW2EIConfigParser.fetchConfig()
    }

    override suspend fun needsUpdate(): Result<Boolean> = suspend {
        this.persistence.get<Long>(ID_KEY).getOrNull().let { id ->
            id == null || downLoadImpl {
                id != it.id
            }.getOrThrow()
        }
    }.result()

    override suspend fun downloadLatestRelease(): Result<Unit> {
        LOG.info("Downloading GW2 Elite Insights...")
        return downLoadImpl(::downLoadAndUnzip).onSuccess {
            LOG.info("Successfully downloaded GW2 Elite Insights.")
        }.onFailure {
            LOG.error("Failed to download GW2 Elite Insights!", it)
        }
    }

    private fun <T> downLoadImpl(block: (Response) -> T): Result<T> {
        LOG.debug("Opening connection to {}...", GITHUB_RELEASE)
        val conn: URLConnection = URI.create(GITHUB_RELEASE).toURL().openConnection()

        return conn.getInputStream().use { ins ->
            LOG.debug("Reading response...")
            val json: String = ins.readAllBytes().decodeToString()
            LOG.debug("Reading response...")
            OBJECT_MAPPER.readValue<Response>(json).let(block).makeResult().onSuccess {
                LOG.debug("Successfully read response.")
            }.onFailure {
                LOG.debug("Failed to read response!", it)
            }
        }
    }

    private fun downLoadAndUnzip(response: Response) {
        // Create parent dir
        GW2EI_PARENT.createDirectories()

        // Clear it for newer versions if needed
        clearDirectory()

        LOG.debug("Opening connection to {}...", response.assets[0].browser_download_url)
        // Download the asset (zip)
        val conn: URLConnection = URI.create(response.assets[0].browser_download_url).toURL().openConnection()
        // Move asset to right directory
        BufferedInputStream(conn.getInputStream()).use {
            LOG.debug("Copying zip...")
            Files.copy(it, GW2EI_ZIP_PATH, StandardCopyOption.REPLACE_EXISTING)
        }

        // Unzip
        ZipInputStream(GW2EI_ZIP_PATH.inputStream()).use(::unzip)

        // Delete the sample config (who'd even need that?)
        GW2EI_PARENT.resolve("Settings").resolve("sample.conf").deleteIfExists()
        // Aaaand remove the zip again
        GW2EI_ZIP_PATH.deleteExisting()

        this.persistence[ID_KEY] = response.id
    }

    @OptIn(ExperimentalPathApi::class)
    private fun clearDirectory() {
        LOG.debug("Clearing previous GW2 Elite Insights installation...")
        // Only list entries to keep parent directory
        GW2EI_PARENT.listDirectoryEntries().filter { it.name != "Settings" }.forEach { p ->
            if(p.isDirectory()) {
                p.deleteRecursively()
            } else {
                p.deleteExisting()
            }
        }
    }

    private fun unzip(zis: ZipInputStream) {
        LOG.debug("Unzipping...")
        var curr: ZipEntry? = zis.nextEntry
        while (curr != null) {
            // Create path in parent
            val newPath: Path = GW2EI_PARENT.resolve(curr.name)
            if (curr.isDirectory) {
                Files.createDirectories(newPath)
            } else {
                newPath.createParentSafe()

                // Move file
                Files.copy(zis, newPath, StandardCopyOption.REPLACE_EXISTING)
            }

            // Aaaand next
            curr = zis.nextEntry
        }

        zis.closeEntry()
        LOG.debug("Unzipped!")
    }

    override suspend fun parse(path: Path): Result<Unit> = {
        LOG.info("Parsing {}...", path)

        val confName: String = path.findBoss(this@GW2EIParser.appConfig)?.name.toString()
        if(confName !in GW2EIConfigParser) {
            // We dont have a config for that boss yet -> Create it
            GW2EIConfigParser.saveConfig(
                GW2EIConfigParser.fetchConfig().copy(
                    OutLocation = InquestUploaderConstants.ANALYZED_DIR_PATH.resolve(confName).absolutePathString()
                ), confName
            )
        }

        // Create the dir where the analyzed json will be stored to
        InquestUploaderConstants.ANALYZED_DIR_PATH.resolve(confName).createDirectories()
        ProcessBuilder(
            GW2EI_PATH.absolutePathString().replace("\"", "\\\""), // EI exe
            "-c", "\"${GW2EIConfigParser.configPath(confName)}\"", // Config to use
            "-p", "\"${path.absolutePathString().replace("\"", "\\\"")}\"" // Log to analyze
        ).start().apply {
            // Read stdout for parsing
            val output: String = inputReader().use {
                it.readText()
            }

            LOG.debug(output)

            if(PARSING_FAILURE_PATTERN.containsMatchIn(output)) {
                // Failed to parse log -> throw
                PARSING_FAILURE_PATTERN.find(output)?.also {
                    throw RuntimeException(it.groupValues[1])
                }

                throw RuntimeException("Unknown Cause")
            }
        }

        Unit
    }.result().onSuccess {
        LOG.info("Successfully parsed {}.", path)
    }.onFailure {
        LOG.error("Failed to parse {}!", path)
    }

    override fun storeConfig(config: GW2EIConfig): Unit = GW2EIConfigParser.saveConfig(config)

    companion object {
        private const val ID_KEY = "%GW2EI_VERSION%"

        private const val GITHUB_RELEASE: String = "https://api.github.com/repos/baaron4/GW2-Elite-Insights-Parser/releases/latest"
        private const val GW2_EI_STR: String = "gw2ei"
        private const val GW2_EI_EXE: String = "GuildWars2EliteInsights.exe"

        private val PARSING_FAILURE_PATTERN = Regex("Parsing Failure .*?: (.*)\r?\n")
        private val GW2EI_PARENT: Path = Path("").resolve(GW2_EI_STR).absolute()
        private val GW2EI_ZIP_PATH: Path = GW2EI_PARENT.resolve("$GW2_EI_STR.zip")
        private val GW2EI_PATH: Path = GW2EI_PARENT.resolve(GW2_EI_EXE)

        private val OBJECT_MAPPER = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}

private data class Response(val id: Long, val assets: List<Asset>)

private data class Asset(val browser_download_url: String)