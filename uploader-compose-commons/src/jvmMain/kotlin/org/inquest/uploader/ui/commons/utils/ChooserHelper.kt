package org.inquest.uploader.ui.commons.utils

import java.io.File
import java.nio.file.Path
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Result of the file dialog.
 */
sealed class PathResult {
    /**
     * Single file [path] was selected.
     */
    data class Single(val path: Path) : PathResult()

    /**
     * Multiple files [paths] were selected.
     */
    data class Multiple(val paths: Set<Path>) : PathResult()

    /**
     * No files were selected.
     */
    data object None : PathResult()
}

/**
 * Opens a [JFileChooser] with the given configuration.
 *
 * @param title Title of the chooser
 * @param initDir Initial directory of the chooser
 * @param allowedExtensions Allowed extensions
 * @param allowMultiSelection Wether mutliple files may be selected
 */
fun FileDialog(
    title: String = "Choose a File",
    initDir: Path = Path.of(""),
    allowedExtensions: List<String> = listOf(),
    allowMultiSelection: Boolean = false,
): PathResult = JFileChooser(initDir.toFile()).apply {
    dialogTitle = title
    acceptAllFileFilter
    isMultiSelectionEnabled = allowMultiSelection
    fileSelectionMode = JFileChooser.FILES_ONLY
    isVisible = true

    allowedExtensions.forEach {
        addChoosableFileFilter(FileNameExtensionFilter(it, it))
    }
}.convertResult()

/**
 * Analogous to [FileDialog], but with directories.
 *
 * @param title Title of the chooser
 * @param initDir Initial directory of the chooser
 * @param allowMultiSelection Wether mutliple files may be selected
 */
fun DirectoryDialog(
    title: String = "Choose a Directory",
    initDir: Path = Path.of(""),
    allowMultiSelection: Boolean = false,
): PathResult = JFileChooser(initDir.toFile()).apply {
    dialogTitle = title
    isMultiSelectionEnabled = allowMultiSelection
    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    isVisible = true
}.convertResult()

private fun JFileChooser.convertResult(): PathResult {
    return when (showOpenDialog(null)) {
        JFileChooser.APPROVE_OPTION -> {
            val files: MutableList<Path> = selectedFiles?.map(File::toPath)?.toMutableList() ?: return PathResult.None

            if(selectedFile != null) {
                files.add(selectedFile.toPath())
            }

            if(files.size == 1) {
                return PathResult.Single(files[0])
            }

            return PathResult.Multiple(files.toSet())
        }
        else -> PathResult.None
    }
}