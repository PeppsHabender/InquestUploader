package org.inquest.uploader.bl.utils

import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.logs.BossToName
import org.inquest.uploader.api.logs.FolderToBoss
import org.inquest.uploader.api.logs.StoredLogs
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.api.persistence.get
import org.inquest.uploader.api.persistence.set
import org.inquest.uploader.bl.utils.PathExtensions.localLogPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists

/**
 * [String] Extensions.
 */
object StringExtensions {
    /**
     * Converts [this] to a [Path]
     */
    fun String.toPath(): Path = Path.of(this)
}

/**
 * [Any] Extensions.
 */
object AnyExtensions {
    /**
     * Logger.
     */
    val <T: Any> T.LOG: Logger
        get() = LoggerFactory.getLogger(this::class.java)
}

/**
 * [Path] Extensions.
 */
object PathExtensions {
    /**
     * Creates [this.parent] then [this]
     *
     * @return [this]
     */
    fun Path.createDirSafe(): Path = this.apply {
        if(notExists()) {
            createDirectories()
        }
    }

    /**
     * Creates [this.parent] then [this]
     *
     * @return [this]
     */
    fun Path.createFileSafe(): Path = createParentSafe().apply {
        if(notExists()) {
            createFile()
        }
    }

    /**
     * Creates [this.parent]
     *
     * @return [this]
     */
    fun Path.createParentSafe(): Path = absolute().apply {
        if(parent.notExists()) {
            parent.createDirectories()
        }
    }

    /**
     * Fetches a distinct log path for [this].
     * Trims the extension and removes all folders between [this] and
     * the respective boss.
     *
     * @return [this]
     */
    fun Path.localLogPath(config: IConfig): String {
        if(config.arcdpsPath.absolutePathString() in absolutePathString()) {
            return findBoss(config)!!.resolve(nameWithoutExtension).absolutePathString()
        }

        return config.arcdpsPath.resolve(parent.name).resolve(name.takeWhile { it != '_' }).absolutePathString()
    }


    /**
     * Fetches the boss for [this] if present.
     *
     * @return [this]
     */
    fun Path.findBoss(config: IConfig): Path? {
        var curr: Path? = this
        while (curr != null && curr.parent != config.arcdpsPath) {
            curr = curr.parent
        }

        return curr
    }
}

/**
 * [IPersistence] Extensions.
 */
object IPersistenceExtensions {
    private const val FOLDER_TO_BOSS_KEY = "%FolderToBoss%"
    private const val BOSS_TO_NAME_KEY = "%BossToName%"
    private const val STORED_LOGS_KEY = "%StoredLogs%"

    /**
     * Maps folder names to encounter ids.
     */
    var IPersistence.folderToBoss: FolderToBoss
        get() = get<FolderToBoss>(FOLDER_TO_BOSS_KEY).getOrDefault(FolderToBoss())
        @Synchronized internal set(value) {
            set(FOLDER_TO_BOSS_KEY, value).getOrThrow()
        }

    /**
     * Maps encounter ids to folder names
     */
    var IPersistence.bossToName: BossToName
        get() = get<BossToName>(BOSS_TO_NAME_KEY).getOrDefault(BossToName())
        @Synchronized internal set(value) {
           set(BOSS_TO_NAME_KEY, value).getOrThrow()
        }

    /**
     * Contains all logs stored by the application.
     */
    var IPersistence.storedLogs: StoredLogs
        get() = get<StoredLogs>(STORED_LOGS_KEY).getOrDefault(StoredLogs())
        @Synchronized internal set(value) {
            set(STORED_LOGS_KEY, value).getOrThrow()
        }

    /**
     * Checks if [path] was previously stored by the application.
     */
    fun StoredLogs.contains(path: Path, config: IConfig) = path.localLogPath(config) in this.storedLogs

    /**
     * @return Returns the log state for the log stored for [path]
     */
    operator fun StoredLogs.get(path: Path, config: IConfig): LogState? = this.storedLogs[path.localLogPath(config)]

    /**
     * Adds [pair] to the stored logs.
     *
     * @return New [StoredLogs] instance
     */
    fun StoredLogs.plus(pair: Pair<Path, LogState>, config: IConfig): StoredLogs {
        val first = pair.first.localLogPath(config)

        return if(first in this.storedLogs) {
            StoredLogs(
                this.storedLogs + (first to this.storedLogs[first]!!.updateWith(pair.second))
            )
        } else {
            StoredLogs(this.storedLogs + (first to pair.second))
        }
    }
}

/**
 * [HttpClient] Extensions.
 */
internal object HttpClientExtensions {
    inline fun <T> HttpClient.doGetRequest(uri: String, resultConverter: (String) -> T): T = execute(
        HttpGet(uri).apply {
            setHeader("Accept", "application/json")
        }
    ).let {
        resultConverter(EntityUtils.toString(it.entity))
    }
}