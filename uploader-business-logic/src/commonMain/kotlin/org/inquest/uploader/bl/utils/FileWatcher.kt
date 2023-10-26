package org.inquest.uploader.bl.utils

import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.Watchable
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.visitFileTree

/**
 * Semi-robust implementation of a kotlin file watcher using the [WatchService] api.
 *
 * @param root Root folder to watch
 */
class FileWatcher(private val root: Path) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private var onNewFile: suspend (Path) -> Unit = {}
    private var onModifiedFile: suspend (Path) -> Unit = {}

    // All locations currently watched
    private var watching: Set<Path> = emptySet()

    init {
        // Register root and sub dirs for watching
        this.watchService.register(this.root)
        this.root.registerSubDirs()
    }

    /**
     * @param block Action to be performed when a new file was created
     * @return this
     */
    fun onFileCreated(block: suspend (Path) -> Unit): FileWatcher {
        this.onNewFile = block
        return this
    }

    /**
     * @param block Action to be performed when a new file was modified
     * @return this
     */
    fun onFileModified(block: suspend (Path) -> Unit): FileWatcher {
        this.onModifiedFile = block
        return this
    }

    /**
     * Starts this watcher.
     */
    suspend fun startWatching() {
        var key: WatchKey? = this.watchService.take()
        while(key != null) {
            analyzeEvents(key)
            key.reset()
            key = this.watchService.take()
        }
    }

    private suspend fun analyzeEvents(key: WatchKey) = key.pollEvents().forEach {
        val context: Any = it.context()
        val watchable: Watchable = key.watchable()
        if(context !is Path || watchable !is Path) {
            return@forEach
        }

        val thePath: Path = watchable.resolve(context)
        if(StandardWatchEventKinds.ENTRY_CREATE == it.kind()) {
            handleEvent(thePath, this.onNewFile)
        } else if(StandardWatchEventKinds.ENTRY_MODIFY == it.kind()) {
            handleEvent(thePath, this.onModifiedFile)
        }
    }

    private suspend inline fun handleEvent(eventPath: Path, crossinline block: suspend (Path) -> Unit) {
        if(Files.isDirectory(eventPath)) {
            // Perform action for all files in directory
            this.watchService.register(eventPath, true)
            for(path in eventPath.listDirectoryEntries().filterNot(Path::isDirectory)) {
                suspend { block(path) }.result()
            }
        } else {
            suspend { block(eventPath) }.result()
        }
    }

    private fun WatchService.register(path: Path, registerSubDirs: Boolean = false) {
        if (path in this@FileWatcher.watching) {
            return
        }

        this@FileWatcher.watching += path

        path.register(
            this,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        if(registerSubDirs) {
            path.registerSubDirs()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun Path.registerSubDirs(): Path = this.apply {
        if(!isDirectory()) {
            return@apply
        }

        visitFileTree(object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(subDir: Path, attrs: BasicFileAttributes?): FileVisitResult {
                this@FileWatcher.watchService.register(subDir)
                return FileVisitResult.CONTINUE
            }
        })
    }
}