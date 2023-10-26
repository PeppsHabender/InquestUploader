package org.inquest.uploader.bl.utils

import org.inquest.uploader.bl.utils.PathExtensions.createDirSafe
import org.inquest.uploader.bl.utils.StringExtensions.toPath
import java.nio.file.Path
import kotlin.io.path.absolute

/**
 * Inquest relevant constants.
 */
object InquestUploaderConstants {
    /**
     * All known log file extensions.
     */
    val LOG_FILE_EXTENSIONS_STR: Set<String> = setOf("evtc", "evtc.zip", "zevtc")

    /**
     * Space for local storage.
     */
    val LOCAL_STORAGE: Path = System.getProperty("user.home").toPath().resolve(".inquest").createDirSafe()

    /**
     * Directory where analysis stores the respective json.
     */
    val ANALYZED_DIR_PATH: Path = LOCAL_STORAGE.resolve("analyzer_cache").absolute().createDirSafe()
}